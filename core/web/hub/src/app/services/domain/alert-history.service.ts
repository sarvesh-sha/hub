import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";

import * as SharedSvc from "app/services/domain/base.service";
import {AlertDetails, AlertExtended} from "app/services/domain/events.service";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class AlertsHistoryService extends SharedSvc.BaseService<Models.AlertHistory, AlertHistoryExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.AlertHistory, AlertHistoryExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string { return Models.AlertHistory.RECORD_IDENTITY; }

    protected getRaw(id: string): Promise<Models.AlertHistory>
    {
        return this.api.alerts.getAlertHistoryByID(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.AlertHistory[]>
    {
        return this.api.alerts.getAlertHistoryBatch(ids);
    }

    protected getBatchRawSize(): number
    {
        return 500;
    }
}

export class AlertHistoryExtended extends SharedSvc.ExtendedModel<Models.AlertHistory>
{
    static newInstance(svc: AlertsHistoryService,
                       model: Models.AlertHistory): AlertHistoryExtended
    {
        return new AlertHistoryExtended(svc, model, Models.AlertHistory.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.AlertHistory.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    async getAll()
    {
        let alert = await this.getAlert();
        await alert.getAll();
    }

    //--//

    @Memoizer
    public async getAlert(): Promise<AlertExtended>
    {
        return this.domain.events.getTypedExtendedByIdentity(AlertExtended, this.model.alert);
    }

    public async extractDetails(): Promise<AlertDetails>
    {
        let alert   = await this.getAlert();
        let details = await alert.extractDetails();

        details.historySysId   = this.model.sysId;
        details.description    = alert.model.description;
        details.eventTimestamp = MomentHelper.parse(this.model.createdOn);
        details.eventType      = this.model.type;
        details.eventLevel     = this.model.level;
        details.eventText      = this.model.text;

        return details;
    }
}
