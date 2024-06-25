import {Injectable} from "@angular/core";

import {ChannelSubscription, MessageBusService} from "app/services/domain/message-bus.service";

import * as Models from "app/services/proxy/model/models";

@Injectable()
export class DatabaseActivityService
{
    private tableSubscriptions = new Map<string, Set<DatabaseActivitySubscriber>>();

    private db: DatabaseActivity;

    constructor(private mb: MessageBusService)
    {
        this.db = new DatabaseActivity("SYS.DB-ACTIVITY", this);
        this.mb.register(this.db);
    }

    //--//

    register(sub: DatabaseActivitySubscriber,
             table: string)
    {
        let subs = this.tableSubscriptions.get(table);
        if (!subs)
        {
            subs = new Set<DatabaseActivitySubscriber>();
            this.tableSubscriptions.set(table, subs);

            if (this.db.isConnected())
            {
                this.db.addTable(table);
            }
        }

        subs.add(sub);
    }

    registerSimpleNotification(table: string,
                               onMessageCallback: (value: Models.DbEvent) => void): DatabaseActivitySubscriber
    {
        let sub = new class extends DatabaseActivitySubscriber
        {
            public async onConnect(lastUpdate: Date): Promise<void>
            {
            }

            public async onDisconnect(): Promise<void>
            {
            }

            public onMessage(value: Models.DbEvent): void
            {
                onMessageCallback(value);
            }
        };

        this.register(sub, table);

        return sub;
    }

    //--//

    async onConnect(): Promise<void>
    {
        this.tableSubscriptions.forEach((subs: Set<DatabaseActivitySubscriber>,
                                         table: string) =>
                                        {
                                            this.connectToTable(subs, table);
                                        });
    }

    async onDisconnect(): Promise<void>
    {
        this.tableSubscriptions.forEach((subs: Set<DatabaseActivitySubscriber>,
                                         table: string) =>
                                        {
                                            this.disconnectFromTable(subs, table);
                                        });
    }

    onMessage(value: Models.DbEvent)
    {
        let subs = this.tableSubscriptions.get(value.context.table);
        if (subs)
        {
            subs.forEach((sub: DatabaseActivitySubscriber) =>
                         {
                             sub.onMessage(value);
                         });
        }
    }

    //--//

    private async connectToTable(subs: Set<DatabaseActivitySubscriber>,
                                 table: string)
    {
        let lastUpdate = await this.db.addTable(table);

        subs.forEach((sub: DatabaseActivitySubscriber) =>
                     {
                         sub.onConnect(lastUpdate);
                     });
    }

    private async disconnectFromTable(subs: Set<DatabaseActivitySubscriber>,
                                      table: string)
    {
        subs.forEach((sub: DatabaseActivitySubscriber) =>
                     {
                         sub.onDisconnect();
                     });
    }
}

class DatabaseActivity extends ChannelSubscription<Models.DbMessage>
{
    constructor(channel: string,
                private mbs: DatabaseActivityService)
    {
        super(channel);
    }

    async onConnect(): Promise<void>
    {
        await this.mbs.onConnect();
    }

    async onDisconnect(): Promise<void>
    {
        await this.mbs.onDisconnect();
    }

    onMessage(value: Models.DbMessage)
    {
        Models.DbMessage.fixupPrototype(value);

        if (value instanceof Models.DbMessageEvent && value.events)
        {
            for (let event of value.events)
            {
                this.mbs.onMessage(event);
            }
        }
    }

    async addTable(table: string): Promise<Date>
    {
        let cfg = Models.DbMessageConfig.newInstance({
                                                         table : table,
                                                         active: true
                                                     });

        let reply = await this.send<Models.DbMessageConfigReply>(cfg);
        return reply?.lastUpdate || new Date();
    }

    async removeTable(table: string): Promise<void>
    {
        let cfg = Models.DbMessageConfig.newInstance({
                                                         table : table,
                                                         active: false
                                                     });

        await this.send(cfg);
    }
}

export abstract class DatabaseActivitySubscriber
{
    abstract onConnect(lastUpdate: Date): Promise<void>;

    abstract onDisconnect(): Promise<void>;

    abstract onMessage(value: Models.DbEvent): void;
}
