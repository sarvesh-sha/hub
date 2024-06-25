import {Injectable} from "@angular/core";
import {ReportError} from "app/app.service";

import {ApiService} from "app/services/domain/api.service";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";

@Injectable()
export class DataImportsService extends SharedSvc.BaseService<Models.ImportedMetadata, ImportedMetadataExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.ImportedMetadata, ImportedMetadataExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.ImportedMetadata.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.ImportedMetadata>
    {
        return this.api.dataImports.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.ImportedMetadata[]>
    {
        return this.api.dataImports.getBatch(ids);
    }

    //--//

    @ReportError
    public getList(): Promise<Models.RecordIdentity[]>
    {
        return this.api.dataImports.getAll();
    }

    //--//

    async getExtendedAll(): Promise<ImportedMetadataExtended[]>
    {
        let ids = await this.getList();
        return this.getExtendedBatch(ids);
    }
}

export class ImportedMetadataExtended extends SharedSvc.ExtendedModel<Models.ImportedMetadata>
{
    static newInstance(svc: DataImportsService,
                       model: Models.ImportedMetadata): ImportedMetadataExtended
    {
        return new ImportedMetadataExtended(svc, model, Models.ImportedMetadata.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.ImportedMetadata.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    public async loadForExport()
    {
        if (!this.model.metadata || !this.model.metadata.length)
        {
            this.model = await this.domain.apis.dataImports.get(this.model.sysId, true);
        }
    }
}

export type ImportedMetadataChangeSubscription = SharedSvc.DbChangeSubscription<Models.ImportedMetadata>;
