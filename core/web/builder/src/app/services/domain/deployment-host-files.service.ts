import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {UtilsService} from "framework/services/utils.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class DeploymentHostFilesService extends SharedSvc.BaseService<Models.DeploymentHostFile, DeploymentHostFileExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.DeploymentHostFile, DeploymentHostFileExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.DeploymentHostFile.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.DeploymentHostFile>
    {
        return this.api.deploymentHostFiles.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.DeploymentHostFile[]>
    {
        return this.api.deploymentHostFiles.getBatch(ids);
    }

    //--//

    @ReportError
    public getList(dep: Models.DeploymentHost): Promise<Models.RecordIdentity[]>
    {
        return this.api.deploymentHostFiles.getAll(dep.sysId);
    }

    //--//

    async getExtendedAll(dep: Models.DeploymentHost): Promise<DeploymentHostFileExtended[]>
    {
        let ids = await this.getList(dep);
        return this.getExtendedBatch(ids);
    }
}

export class DeploymentHostFileExtended extends SharedSvc.ExtendedModel<Models.DeploymentHostFile>
{
    static newInstance(svc: DeploymentHostFilesService,
                       model: Models.DeploymentHostFile): DeploymentHostFileExtended
    {
        return new DeploymentHostFileExtended(svc, model, Models.DeploymentHostFile.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.DeploymentHostFile.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getOwningDeployment(): Promise<DeploymentHostExtended>
    {
        return this.domain.deploymentHosts.getExtendedByIdentity(this.model.deployment);
    }

    get available(): boolean
    {
        return this.model.length > 0;
    }

    public transferDisplay: string;

    get lengthDisplay(): string
    {
        return this.available ? `${this.model.length}` : "<not initialized>";
    }

    get mayBeBinary(): boolean
    {
        let name = this.model.path;
        if (name.endsWith(".tar")) return true;
        if (name.endsWith(".gz")) return true;
        if (name.endsWith(".tgz")) return true;

        return false;
    }

    //--//

    async forget(): Promise<void>
    {
        await this.domain.apis.deploymentHostFiles.remove(this.model.sysId);
    }

    async getContents(): Promise<string>
    {
        let res = await this.domain.apis.deploymentHostFiles.getAsText(this.model.sysId);
        return res.text;
    }

    async setContents(contents: Models.DeploymentHostFileContents): Promise<DeploymentHostFileExtended>
    {
        let newModel = await this.domain.apis.deploymentHostFiles.setContents(this.model.sysId, contents);
        return this.domain.deploymentHostFiles.wrapModel(newModel);
    }

    async startDownload(): Promise<DeploymentHostFileExtended>
    {
        let newModel = await this.domain.apis.deploymentHostFiles.startDownload(this.model.sysId);
        return this.domain.deploymentHostFiles.wrapModel(newModel || this.model);
    }

    async startUpload(): Promise<DeploymentHostFileExtended>
    {
        let newModel = await this.domain.apis.deploymentHostFiles.startUpload(this.model.sysId);
        return this.domain.deploymentHostFiles.wrapModel(newModel || this.model);
    }

    //--//

    async getUrlForDownload(): Promise<string>
    {
        let path = this.model.path;
        let lastPart = path.lastIndexOf("/");
        let fileName = lastPart >= 0 ? path.substring(lastPart + 1) : path;

        let host = await this.getOwningDeployment();
        let hostName = host.model.hostName || host.model.hostId;

        hostName = UtilsService.replaceAll(hostName, "/", "_");
        hostName = UtilsService.replaceAll(hostName, "\\", "_");
        hostName = UtilsService.replaceAll(hostName, ":", "_");

        return this.domain.apis.deploymentHostFiles.getStream__generateUrl(this.model.sysId, `${hostName}__${fileName}`);
    }

    async uploadContents(file: File): Promise<DeploymentHostFileExtended>
    {
        let newModel = await this.domain.apis.deploymentHostFiles.setStream(this.model.sysId, file);
        return this.domain.deploymentHostFiles.wrapModel(newModel || this.model);
    }
}

export type DeploymentHostFileChangeSubscription = SharedSvc.DbChangeSubscription<Models.DeploymentHostFile>;
