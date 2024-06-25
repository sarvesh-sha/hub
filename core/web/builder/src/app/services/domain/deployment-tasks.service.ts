import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";
import {JobExtended} from "app/services/domain/jobs.service";
import {RegistryImageExtended} from "app/services/domain/registry-images.service";
import {RegistryTaggedImageExtended} from "app/services/domain/registry-tagged-images.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class DeploymentTasksService extends SharedSvc.BaseService<Models.DeploymentTask, DeploymentTaskExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.DeploymentTask, DeploymentTaskExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.DeploymentTask.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.DeploymentTask>
    {
        return this.api.deploymentTasks.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.DeploymentTask[]>
    {
        return this.api.deploymentTasks.getBatch(ids);
    }

    //--//

    @ReportError
    public getList(dep: Models.DeploymentHost): Promise<Models.RecordIdentity[]>
    {
        return this.api.deploymentTasks.getAll(dep.sysId);
    }

    //--//

    async getExtendedAll(dep: Models.DeploymentHost): Promise<DeploymentTaskExtended[]>
    {
        let ids = await this.getList(dep);
        return this.getExtendedBatch(ids);
    }
}

export class DeploymentTaskExtended extends SharedSvc.ExtendedModel<Models.DeploymentTask>
{
    static newInstance(svc: DeploymentTasksService,
                       model: Models.DeploymentTask): DeploymentTaskExtended
    {
        return new DeploymentTaskExtended(svc, model, Models.DeploymentTask.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.DeploymentTask.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getOwningDeployment(): Promise<DeploymentHostExtended>
    {
        return this.domain.deploymentHosts.getExtendedByIdentity(this.model.deployment);
    }

    filterLog(filters: Models.LogEntryFilterRequest): Promise<Models.LogRange[]>
    {
        return this.domain.apis.deploymentTasks.filterLog(this.model.sysId, filters);
    }

    getLog(fromOffset?: number,
           toOffset?: number,
           limit?: number): Promise<Models.LogLine[]>
    {
        return this.domain.apis.deploymentTasks.getLog(this.model.sysId, fromOffset, toOffset, limit);
    }

    //--//

    async checkRestart(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.deploymentTasks.restart(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async restart(): Promise<Models.ValidationResults>
    {
        return await this.domain.apis.deploymentTasks.restart(this.model.sysId, false);
    }

    async checkTerminate(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.deploymentTasks.terminate(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async terminate(): Promise<Models.ValidationResults>
    {
        return await this.domain.apis.deploymentTasks.terminate(this.model.sysId, false);
    }

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.deploymentTasks.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.deploymentTasks.remove(this.model.sysId, false);
    }

    //--//

    @Memoizer
    async getImage(): Promise<RegistryImageExtended>
    {
        return await this.domain.registryImages.getExtendedByIdentity(this.model.imageReference);
    }

    async sameImage(tag: RegistryTaggedImageExtended): Promise<boolean>
    {
        let img1 = await tag.getImage();
        let img2 = await this.getImage();

        return img1 && img2 && img1.model.imageSha == img2.model.imageSha;
    }

    @Memoizer
    async getFirstImageTag(): Promise<string>
    {
        let image = await this.getImage();
        if (image)
        {
            for (let tag of await image.getReferencingTags())
            {
                return tag.model.tag;
            }
        }

        return this.model.image || "<unknown>";
    }

    @Memoizer
    async listAssociatedJobs(): Promise<JobExtended[]>
    {
        let res = [];

        let image = await this.getImage();
        if (image)
        {
            for (let tag of await image.getReferencingTags())
            {
                let job = await tag.getOwingJob();
                if (job) res.push(job);
            }
        }

        return res;
    }

    async isInstanceOfJob(job: JobExtended): Promise<boolean>
    {
        for (let job2 of await this.listAssociatedJobs())
        {
            if (job2.sameIdentity(job)) return true;
        }

        return false;
    }

    @Memoizer
    async getRole(): Promise<Models.DeploymentRole>
    {
        let image = await this.getImage();
        if (image) return image.getTargetService();

        if (this.model.labels) return <Models.DeploymentRole>this.model.labels["Optio3_TargetService"];

        return null;
    }
}

export type DeploymentTaskChangeSubscription = SharedSvc.DbChangeSubscription<Models.DeploymentTask>;
