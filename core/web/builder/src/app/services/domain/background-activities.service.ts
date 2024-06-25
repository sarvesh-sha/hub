import {Injectable} from "@angular/core";
import {ReportError} from "app/app.service";

import {ApiService} from "app/services/domain/api.service";

import * as SharedSvc from "app/services/domain/base.service";
import {EnumsService} from "app/services/domain/enums.service";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class BackgroundActivitiesService extends SharedSvc.BaseService<Models.BackgroundActivity, BackgroundActivityExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService,
                private enums: EnumsService)
    {
        super(api, errors, cache, Models.BackgroundActivity, BackgroundActivityExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.BackgroundActivity.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.BackgroundActivity>
    {
        return this.api.backgroundActivities.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.BackgroundActivity[]>
    {
        return this.api.backgroundActivities.getBatch(ids);
    }

    //--//

    @ReportError
    public getList(filters?: Models.BackgroundActivityFilterRequest): Promise<Models.RecordIdentity[]>
    {
        return this.api.backgroundActivities.getFiltered(filters);
    }

    describeStates(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("BackgroundActivityStatus", false);
    }

    async describeState(mode: Models.BackgroundActivityStatus): Promise<Models.EnumDescriptor>
    {
        return EnumsService.find(await this.describeStates(), mode);
    }

    @Memoizer
    async getStates(): Promise<ControlOption<Models.BackgroundActivityStatus>[]>
    {
        let types = await this.describeStates();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    describeFilters(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("BackgroundActivityFilter", false);
    }

    async describeFilter(mode: Models.BackgroundActivityFilter): Promise<Models.EnumDescriptor>
    {
        return EnumsService.find(await this.describeFilters(), mode);
    }

    @Memoizer
    async getFilters(): Promise<ControlOption<Models.BackgroundActivityFilter>[]>
    {
        let types = await this.describeFilters();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    async getExtendedAll(): Promise<BackgroundActivityExtended[]>
    {
        let ids = await this.getList();
        return this.getExtendedBatch(ids);
    }

    //--//

    @Memoizer
    public async prepareFilterControlOptions(): Promise<ControlOption<string>[]>
    {
        let opFilters = await this.getFilters();
        let opStates  = await this.getStates();

        let states = [];
        for (let opFilter of opFilters)
        {
            if (opFilter.id != Models.BackgroundActivityFilter.matchingStatus)
            {
                let statusFilter    = new Models.BackgroundActivityFilterPair();
                statusFilter.filter = <Models.BackgroundActivityFilter>opFilter.id;

                let state   = new ControlOption<string>();
                state.id    = JSON.stringify(statusFilter);
                state.label = opFilter.label;

                states.push(state);
            }
        }

        for (let opState of opStates)
        {
            let statusFilter     = new Models.BackgroundActivityFilterPair();
            statusFilter.filter  = Models.BackgroundActivityFilter.matchingStatus;
            statusFilter.targets = [<Models.BackgroundActivityStatus>opState.id];

            let state   = new ControlOption<string>();
            state.id    = JSON.stringify(statusFilter);
            state.label = `Matching State: ${opState.label}`;

            states.push(state);
        }

        return states;
    }
}

export class BackgroundActivityExtended extends SharedSvc.ExtendedModel<Models.BackgroundActivity>
{
    static newInstance(svc: BackgroundActivitiesService,
                       model: Models.BackgroundActivity): BackgroundActivityExtended
    {
        return new BackgroundActivityExtended(svc, model, Models.BackgroundActivity.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.BackgroundActivity.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    public async flush(): Promise<void>
    {
        for (let sub of await this.getSubActivities())
        {
            await sub.flush();
        }

        return super.flush();
    }

    @Memoizer
    public getWaitingActivities(): Promise<BackgroundActivityExtended[]>
    {
        return this.domain.backgroundActivities.getExtendedBatch(this.model.waitingActivities);
    }

    @Memoizer
    public getSubActivities(): Promise<BackgroundActivityExtended[]>
    {
        return this.domain.backgroundActivities.getExtendedBatch(this.model.subActivities);
    }

    @Memoizer
    public async activityDetails(): Promise<any>
    {
        let details = await this.domain.apis.backgroundActivities.details(this.model.sysId);
        return details.handlerState;
    }

    @Memoizer
    public async activityInfo(): Promise<string[]>
    {
        let res = [];

        if (this.model.status == Models.BackgroundActivityStatus.WAITING)
        {
            let subs = await this.getSubActivities();
            if (subs.length > 0)
            {
                for (let sub of subs)
                {
                    let title = sub.model.title;

                    if (sub.isDone())
                    {
                        title = `${title} (Done)`;
                    }

                    res.push(title);
                }

                res.sort((a,
                          b) => UtilsService.compareStrings(a, b, true));
            }
        }
        else
        {
            res.push(this.model.title);
        }

        return res;
    }

    public isDone(): boolean
    {
        switch (this.model.status)
        {
            case Models.BackgroundActivityStatus.CANCELLED:
            case Models.BackgroundActivityStatus.COMPLETED:
            case Models.BackgroundActivityStatus.FAILED:
                return true;

            default:
                return false;
        }
    }

    public async cancel(): Promise<BackgroundActivityExtended>
    {
        await this.domain.apis.backgroundActivities.cancel(this.model.sysId);
        return await this.refresh<BackgroundActivityExtended>();
    }

    public async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.backgroundActivities.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    public async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.backgroundActivities.remove(this.model.sysId);
    }
}

export type BackgroundActivityChangeSubscription = SharedSvc.DbChangeSubscription<Models.BackgroundActivity>;
