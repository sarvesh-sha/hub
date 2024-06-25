import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import {GatewayExtended} from "app/services/domain/assets.service";
import {BackgroundActivityExtended} from "app/services/domain/background-activities.service";
import * as SharedSvc from "app/services/domain/base.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class GatewayProberOperationsService extends SharedSvc.BaseService<Models.GatewayProberOperation, GatewayProberOperationExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.GatewayProberOperation, GatewayProberOperationExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.GatewayProberOperation.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.GatewayProberOperation>
    {
        return this.api.gatewayProberOperations.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.GatewayProberOperation[]>
    {
        return this.api.gatewayProberOperations.getBatch(ids);
    }

    //--//

    @ReportError
    public getList(dep: Models.GatewayAsset): Promise<Models.RecordIdentity[]>
    {
        return this.api.gatewayProberOperations.getAll(dep.sysId);
    }

    //--//

    async getExtendedAll(dep: Models.GatewayAsset): Promise<GatewayProberOperationExtended[]>
    {
        let ids = await this.getList(dep);
        return this.getExtendedBatch(ids);
    }
}

export class GatewayProberOperationExtended extends SharedSvc.ExtendedModel<Models.GatewayProberOperation>
{
    static newInstance(svc: GatewayProberOperationsService,
                       model: Models.GatewayProberOperation): GatewayProberOperationExtended
    {
        return new GatewayProberOperationExtended(svc, model, Models.GatewayProberOperation.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.GatewayProberOperation.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getOwningGateway(): Promise<GatewayExtended>
    {
        return this.domain.assets.getTypedExtendedByIdentity(GatewayExtended, this.model.gateway);
    }

    @Memoizer
    public getCurrentActivity(): Promise<BackgroundActivityExtended>
    {
        return this.domain.backgroundActivities.getExtendedByIdentity(this.model.currentActivity);
    }

    filterLog(filters: Models.LogEntryFilterRequest): Promise<Models.LogRange[]>
    {
        return this.domain.apis.gatewayProberOperations.filterLog(this.model.sysId, filters);
    }

    getLog(fromOffset?: number,
           toOffset?: number,
           limit?: number): Promise<Models.LogLine[]>
    {
        return this.domain.apis.gatewayProberOperations.getLog(this.model.sysId, fromOffset, toOffset, limit);
    }

    //--//

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.gatewayProberOperations.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.gatewayProberOperations.remove(this.model.sysId);
    }
}

export type GatewayProberOperationChangeSubscription = SharedSvc.DbChangeSubscription<Models.GatewayProberOperation>;
