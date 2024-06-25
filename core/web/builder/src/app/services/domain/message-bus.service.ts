import {DOCUMENT} from "@angular/common";
import {Inject, Injectable} from "@angular/core";
import {UUID} from "angular2-uuid";
import {ApiService} from "app/services/domain/api.service";

import * as Models from "app/services/proxy/model/models";
import {Logger, LoggingService, LoggingSeverity} from "framework/services/logging.service";
import {UtilsService} from "framework/services/utils.service";
import {callSafely, Future} from "framework/utils/concurrency";
import {Subject} from "rxjs";

@Injectable()
export class MessageBusService
{
    readonly logger: Logger;

    maxReconnectExceeded: Subject<void> = new Subject<void>();

    private websocketConnecting: boolean;
    private websocket: WebSocket;

    private url: string;

    private connected: boolean;
    private waitForConnected       = new Future<void>();
    private reconnectCount: number = 0;
    private maxReconnectSent       = false;
    private reconnectTimer: any;

    private pendingSystemMessages = new Map<String, Future<Models.MessageBusPayload>>();
    private pendingDataMessages   = new Map<String, Future<Object>>();
    private endpointId: string;

    private channelSubscriptions = new Map<String, ChannelSubscriptionHandle<any>>();

    private ping: Ping;

    constructor(private apiService: ApiService,
                private logService: LoggingService,
                private utilService: UtilsService,
                @Inject(DOCUMENT) document: any)
    {
        this.logger = logService.getLogger(MessageBusService);

        let origin = this.apiService.basePath;

        this.url = "ws" + origin.substring(4) + "/message-bus";

        this.ping = new Ping("SYS.PING", this);

        // Ping every minute, to keep the channel open. Run outside angular so we don't block e2e tests.

        this.utilService.setIntervalOutsideAngular(() => this.executePing(), 60 * 1000);
    }

    onLogin()
    {
        this.logger.info("Logged in");
        this.reconnectCount = 0;
        this.connect();
    }

    onLogout()
    {
        this.logger.info("Logged out");

        // on log out, disconnect from server.
        this.closeSocket();
    }

    async register<T>(subscription: ChannelSubscription<T>): Promise<void>
    {
        if (this.channelSubscriptions.has(subscription.channel))
        {
            throw new Error(`Channel ${subscription.channel} already subscribed`);
        }

        let handle          = new ChannelSubscriptionHandle<T>(this, subscription);
        subscription.handle = handle;

        this.channelSubscriptions.set(subscription.channel, handle);

        if (this.connected)
        {
            await this.tryAndJoin(handle);
        }
    }

    async unregister<T>(handle: ChannelSubscriptionHandle<T>): Promise<void>
    {
        let sub = handle.sub;
        if (sub)
        {
            sub.handle = null;

            if (this.channelSubscriptions.has(sub.channel))
            {
                this.channelSubscriptions.delete(sub.channel);

                if (this.connected)
                {
                    await this.tryAndLeave(handle);
                }
            }
        }
    }

    private async tryAndJoin<T>(handle: ChannelSubscriptionHandle<T>): Promise<void>
    {
        let result = await this.joinChannel(handle.sub.channel);
        if (result == true)
        {
            handle.onConnect();
        }
    }

    private async tryAndLeave<T>(handle: ChannelSubscriptionHandle<T>): Promise<void>
    {
        handle.onDisconnect();

        await this.leaveChannel(handle.sub.channel);
    }

    private async connect()
    {
        if (this.websocketConnecting) return;

        try
        {
            this.logger.info(`Try to connect to ${this.url}`);
            this.websocketConnecting = true;
            this.websocket           = new WebSocket(this.url);
            this.websocket.onopen    = (ev) =>
            {
                if (this.isDebugEnabled())
                {
                    this.logger.debug(`onOpen: ${JSON.stringify(ev)}`);
                }

                this.retrieveIdentity();
            };

            this.websocket.onclose = (ev) =>
            {
                if (this.isDebugEnabled())
                {
                    this.logger.debug(`onClose: ${JSON.stringify(ev)}`);
                }

                this.closeSocketAndRetryToConnect();
            };

            this.websocket.onerror = (ev) =>
            {
                if (this.isDebugEnabled())
                {
                    this.logger.debug(`onError: ${JSON.stringify(ev)}`);
                }

                this.closeSocketAndRetryToConnect();
            };

            this.websocket.onmessage = (ev) =>
            {
                this.receivedMessage(ev);
            };

            this.logger.debug("Waiting for connection...");
            this.waitForConnected = new Future<void>();
            this.waitForConnected.setCancellationTimeout(10000);

            await this.waitForConnected;

            this.reconnectSubscribers();
        }
        catch (error)
        {
            this.logger.error(`Got an error while connecting to server: ${JSON.stringify(error)}`);

            this.closeSocketAndRetryToConnect();
        }

        this.websocketConnecting = false;
    }

    private reconnectSubscribers()
    {
        this.channelSubscriptions.forEach(
            handle => this.tryAndJoin(handle));
    }

    private disconnectSubscribers()
    {
        this.channelSubscriptions.forEach(
            handle => handle.onDisconnect());
    }

    private closeSocket()
    {
        this.connected = false;
        this.waitForConnected.reject("disconnected");
        this.waitForConnected = new Future<void>();

        this.pendingSystemMessages.forEach(
            promise => promise.reject("disconnected"));
        this.pendingDataMessages.forEach(
            promise => promise.reject("disconnected"));

        this.pendingSystemMessages.clear();
        this.pendingDataMessages.clear();

        callSafely(() =>
                   {
                       if (this.websocket)
                       {
                           this.websocket.onclose = null;
                           this.websocket.onerror = null;
                           this.websocket.onopen  = null;
                           this.websocket.close();
                           this.websocket = null;
                       }
                   });

        this.disconnectSubscribers();
    }

    private closeSocketAndRetryToConnect()
    {
        this.closeSocket();

        // Exponential backoff.
        if (this.reconnectCount++ > 4)
        {
            if (!this.maxReconnectSent)
            {
                this.maxReconnectExceeded.next();
                this.maxReconnectSent = true;
            }
            return;
        }

        if (this.reconnectTimer)
        {
            clearTimeout(this.reconnectTimer);
        }

        this.reconnectTimer = this.utilService.setTimeoutOutsideAngular(() => this.connect(), 10000);
    }

    private receivedMessage(ev: MessageEvent)
    {
        let debugSpew = this.isDebugEnabled();
        if (debugSpew)
        {
            this.logger.debug(`receivedMessage: ${ev.data}`);
        }

        let req = <Models.MessageBusPayload>JSON.parse(ev.data);
        Models.MessageBusPayload.fixupPrototype(req);

        if (this.pendingSystemMessages.has(req.messageId))
        {
            if (debugSpew)
            {
                this.logger.debug(`receivedMessage: reply for sys message ${req.messageId}`);
            }

            let handler = this.pendingSystemMessages.get(req.messageId);
            this.pendingSystemMessages.delete(req.messageId);

            handler.resolve(req);
            return;
        }

        if (req instanceof Models.MbDataMessageReply)
        {
            let msg = req.payload;

            if (this.pendingDataMessages.has(req.messageId))
            {
                if (debugSpew)
                {
                    this.logger.debug(`receivedMessage: reply for data message ${msg.messageId}`);
                }

                let handler = this.pendingDataMessages.get(msg.messageId);
                this.pendingDataMessages.delete(msg.messageId);

                handler.resolve(msg);
                return;
            }
        }

        if (req instanceof Models.MbDataMessage)
        {
            let payload = req.payload;

            let sub = this.channelSubscriptions.get(req.channel);
            if (sub)
            {
                sub.onMessage(payload);
            }
        }
    }

    private sendMessage(msg: Models.MessageBusPayload)
    {
        if (this.isDebugEnabled())
        {
            this.logger.debug(`sendMessage: ${JSON.stringify(msg)}`);
        }

        this.websocket.send(JSON.stringify(msg));
    }

    private async sendSystemMessageWithNoReply(msg: Models.MessageBusPayload): Promise<void>
    {
        msg.messageId = MessageBusService.createMessageId();

        if (this.isDebugEnabled())
        {
            this.logger.debug(`sendSystemMessageWithNoReply: ${msg.messageId} => ${JSON.stringify(msg)}`);
        }

        this.sendMessage(msg);
    }

    private async sendSystemMessageWithReply<T extends Models.MessageBusPayload>(msg: Models.MessageBusPayload): Promise<T>
    {
        msg.messageId = MessageBusService.createMessageId();

        if (this.isDebugEnabled())
        {
            this.logger.debug(`sendSystemMessageWithReply: ${msg.messageId} => ${JSON.stringify(msg)}`);
        }

        let replyFuture = new Future<Models.MessageBusPayload>();
        this.pendingSystemMessages.set(msg.messageId, replyFuture);

        this.sendMessage(msg);

        let reply = await replyFuture;

        if (this.isDebugEnabled())
        {
            this.logger.debug(`sendSystemMessageWithReply: got reply for ${msg.messageId}`);
        }

        return <T>reply;
    }

    private close()
    {
        if (this.websocket)
        {
            this.websocket.close();
            this.websocket = null;
        }
    }

    private executePing()
    {
        if (this.ping.isConnected())
        {
            this.ping.send("Keep-Alive");
        }
    }

    static createMessageId(): string
    {
        return UUID.UUID();
    }

    //--//

    private async retrieveIdentity()
    {
        try
        {
            let req = new Models.MbControlGetIdentity();

            let reply = await this.sendSystemMessageWithReply<Models.MbControlGetIdentityReply>(req);

            this.endpointId = reply.endpointIdentity;
            this.logger.info(`MessageBus identity is ${this.endpointId}`);

            this.waitForConnected.resolve();
            this.reconnectCount   = 0;
            this.maxReconnectSent = false;
            this.connected        = true;
        }
        catch (error)
        {
            this.closeSocketAndRetryToConnect();
        }
    }

    public async listChannels(): Promise<string[]>
    {
        try
        {
            let req = new Models.MbControlListChannels();

            let reply = await this.sendSystemMessageWithReply<Models.MbControlListChannelsReply>(req);

            return reply.availableChannels;
        }
        catch (error)
        {
            this.closeSocketAndRetryToConnect();
        }

        return undefined;
    }

    private async joinChannel(name: string): Promise<boolean>
    {
        try
        {
            let req = Models.MbControlJoinChannel.newInstance({channel: name});

            let reply = await this.sendSystemMessageWithReply<Models.MbControlJoinChannelReply>(req);

            return reply.success;
        }
        catch (error)
        {
            this.closeSocketAndRetryToConnect();
        }

        return false;
    }

    private async leaveChannel(name: string): Promise<boolean>
    {
        try
        {
            let req = Models.MbControlLeaveChannel.newInstance({channel: name});

            let reply = await this.sendSystemMessageWithReply<Models.MbControlLeaveChannelReply>(req);

            return reply.success;
        }
        catch (error)
        {
            this.closeSocketAndRetryToConnect();
        }

        return false;
    }

    public async postToChannel<T>(channelName: string,
                                  destination: string,
                                  data: any): Promise<void>
    {
        try
        {
            let req = Models.MbDataMessage.newInstance({
                                                           channel    : channelName,
                                                           destination: destination,
                                                           payload    : data
                                                       });

            await this.sendSystemMessageWithNoReply(req);
        }
        catch (error)
        {
            this.closeSocketAndRetryToConnect();
        }
    }

    public async sendToChannel<T>(channelName: string,
                                  destination: string,
                                  data: any): Promise<T>
    {
        try
        {
            let req = Models.MbDataMessage.newInstance({
                                                           channel    : channelName,
                                                           destination: destination,
                                                           payload    : data
                                                       });

            let reply = await this.sendSystemMessageWithReply<Models.MbDataMessageReply>(req);

            return reply.payload;
        }
        catch (error)
        {
            this.closeSocketAndRetryToConnect();
        }

        return null;
    }

    private isDebugEnabled(): boolean
    {
        return this.logger.isEnabled(LoggingSeverity.Debug);
    }
}

class ChannelSubscriptionHandle<T>
{
    constructor(private mb: MessageBusService,
                readonly sub: ChannelSubscription<T>)
    {
    }

    private joined: boolean = false;
    private joinedWait: Future<void>;

    unregister()
    {
        this.mb.unregister(this);
    }

    onConnect()
    {
        if (!this.joined)
        {
            this.joined = true;

            if (this.joinedWait)
            {
                this.joinedWait.resolve();
                this.joinedWait = null;
            }
        }

        callSafely(() => this.sub.onConnect());
    }

    onDisconnect()
    {
        if (this.joined)
        {
            this.joined = false;

            if (this.joinedWait)
            {
                this.joinedWait.reject("disconnected");
                this.joinedWait = null;
            }
        }

        callSafely(() => this.sub.onDisconnect());
    }

    onMessage(value: T)
    {
        callSafely(() => this.sub.onMessage(value));
    }

    isConnected()
    {
        return this.joined;
    }

    async waitForConnection(timeout?: number): Promise<void>
    {
        if (!this.joined)
        {
            let waitOn: Promise<void>[] = [];
            let timer;

            if (!this.joinedWait)
            {
                this.joinedWait = new Future<void>();
            }

            waitOn.push(this.joinedWait);

            if (timeout)
            {
                let cancel = new Future<void>();
                timer      = setTimeout(() => cancel.reject("timeout"), timeout);
                waitOn.push(cancel);
            }

            await Promise.race(waitOn);

            if (timer)
            {
                clearTimeout(timer);
            }
        }
    }

    async broadcast(value: T): Promise<void>
    {
        await this.waitForConnection();

        return this.mb.postToChannel(this.sub.channel, "<<BROADCAST>>", value);
    }

    async post(value: T,
               destination: string): Promise<void>
    {
        await this.waitForConnection();

        return this.mb.postToChannel(this.sub.channel, destination, value);
    }

    async send<R>(value: T,
                  destination: string): Promise<R>
    {
        await this.waitForConnection();

        return this.mb.sendToChannel<R>(this.sub.channel, destination, value);
    }
}

export abstract class ChannelSubscription<T>
{
    handle: ChannelSubscriptionHandle<T>;

    constructor(public readonly channel: string)
    {
    }

    abstract onConnect(): Promise<void>;

    abstract onDisconnect(): Promise<void>;

    abstract onMessage(value: T): void

    isConnected()
    {
        return this.handle && this.handle.isConnected();
    }

    waitForConnection(timeout?: number): Promise<void>
    {
        return this.handle.waitForConnection(timeout);
    }

    broadcast(value: T): Promise<void>
    {
        return this.handle.broadcast(value);
    }

    post(value: T,
         destination: string = "<<SERVICE>>"): Promise<void>
    {
        return this.handle.post(value, destination);
    }

    send<R>(value: T,
            destination: string = "<<SERVICE>>"): Promise<R>
    {
        return this.handle.send<R>(value, destination);
    }

    unregister()
    {
        this.handle.unregister();
    }
}

class Ping extends ChannelSubscription<string>
{
    constructor(channel: string,
                private messageBusService: MessageBusService)
    {
        super(channel);

        messageBusService.register(this);
    }

    async onConnect(): Promise<void>
    {
    }

    async onDisconnect(): Promise<void>
    {
    }

    onMessage(value: string)
    {
        this.messageBusService.logger.info(`Got message on the ping channel: ${value}`);
    }
}
