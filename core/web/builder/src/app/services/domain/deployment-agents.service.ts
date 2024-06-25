import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";
import {DeploymentTaskExtended} from "app/services/domain/deployment-tasks.service";
import {JobExtended} from "app/services/domain/jobs.service";
import {RegistryImageExtended} from "app/services/domain/registry-images.service";
import {DeploymentStatus} from "app/services/proxy/model/DeploymentStatus";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {UtilsService} from "framework/services/utils.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class DeploymentAgentsService extends SharedSvc.BaseService<Models.DeploymentAgent, DeploymentAgentExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.DeploymentAgent, DeploymentAgentExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.DeploymentAgent.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.DeploymentAgent>
    {
        return this.api.deploymentAgents.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.DeploymentAgent[]>
    {
        return this.api.deploymentAgents.getBatch(ids);
    }

    //--//

    @ReportError
    public getList(host: Models.DeploymentHost): Promise<Models.RecordIdentity[]>
    {
        return this.api.deploymentAgents.getAll(host.sysId);
    }

    //--//

    async getExtendedAll(host: Models.DeploymentHost): Promise<DeploymentAgentExtended[]>
    {
        let ids = await this.getList(host);
        return this.getExtendedBatch(ids);
    }
}

export class DeploymentAgentExtended extends SharedSvc.ExtendedModel<Models.DeploymentAgent>
{
    static newInstance(svc: DeploymentAgentsService,
                       model: Models.DeploymentAgent): DeploymentAgentExtended
    {
        return new DeploymentAgentExtended(svc, model, Models.DeploymentAgent.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.DeploymentAgent.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getOwningDeployment(): Promise<DeploymentHostExtended>
    {
        return this.domain.deploymentHosts.getExtendedByIdentity(this.model.deployment);
    }

    @Memoizer
    public getSupportedFeaturesDesc(): string
    {
        let features = this.model?.details?.supportedFeatures || [];

        features.sort((a,
                       b) => UtilsService.compareStrings(a, b, true));

        return features.join(", ");
    }

    get diskSize(): number
    {
        let totalDisk = this.model.details?.diskTotal;
        return totalDisk > 0 ? totalDisk : NaN;
    }

    get diskFree(): number
    {
        let diskTotal = this.model.details?.diskTotal;
        let diskFree  = this.model.details?.diskFree;
        if (diskTotal > 0 && diskFree > 0)
        {
            return diskFree / diskTotal;
        }

        return NaN;
    }

    get cpuLoad(): number
    {
        return this.cpuUser + this.cpuSystem;
    }

    get cpuUser(): number
    {
        return this.model.details?.cpuUsageUser / 100;
    }

    get cpuSystem(): number
    {
        return this.model.details?.cpuUsageSystem / 100;
    }

    get batteryVoltage(): string
    {
        return formatVoltage(this.model.details?.batteryVoltage);
    }

    get cpuTemperature(): string
    {
        return formatTemperature(this.model.details?.cpuTemperature);
    }

    @Memoizer
    async findTask(): Promise<DeploymentTaskExtended>
    {
        let host = await this.getOwningDeployment();
        if (host != null)
        {
            for (let task of await host.getTasks())
            {
                if (task.model.dockerId == this.model.dockerId)
                {
                    return task;
                }
            }
        }

        return null;
    }

    @Memoizer
    async findImage(): Promise<RegistryImageExtended>
    {
        let task = await this.findTask();
        if (task)
        {
            return task.getImage();
        }

        return null;
    }

    @Memoizer isUsingReleaseCandidate(): Promise<boolean>
    {
        return this.checkReleaseStatus(Models.RegistryImageReleaseStatus.ReleaseCandidate);
    }

    @Memoizer isUsingRelease(): Promise<boolean>
    {
        return this.checkReleaseStatus(Models.RegistryImageReleaseStatus.Release);
    }

    private async checkReleaseStatus(target: Models.RegistryImageReleaseStatus): Promise<boolean>
    {
        let image = await this.findImage();
        if (image)
        {
            for (let tag of await image.getReferencingTags())
            {
                if (tag.model.releaseStatus == target)
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Memoizer
    async listAssociatedJobs(): Promise<JobExtended[]>
    {
        let task = await this.findTask();
        return task ? await task.listAssociatedJobs() : [];
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
    async getStatusDesc(): Promise<string>
    {
        if (!this.model)
        {
            return "<unavailable>";
        }

        let text = `${this.model.status}`;

        if (await this.isUsingRelease())
        {
            text = `${text} - RTM`;
        }
        else if (await this.isUsingReleaseCandidate())
        {
            text = `${text} - RC`;
        }

        text = `${text} - ${this.model.active ? "Active" : "Inactive"}`;

        if (this.model.status != DeploymentStatus.Ready)
        {
            return text;
        }

        let status = await this.getStalenessStatus();
        switch (status)
        {
            case Models.DeploymentOperationalResponsiveness.Responsive:
                return text;

            default:
                return text + ` and no heartbeat in ${this.getStalenessAsText()}`;
        }
    }

    getStaleness(): number
    {
        if (!this.model)
        {
            return 365 * 3600 * 1000; // Assume one year...
        }

        let now        = new Date().getTime();
        let lastUpdate = new Date(this.model.lastHeartbeat).getTime();
        return now - lastUpdate;
    }

    getStalenessAsText(): string
    {
        return DeploymentHostExtended.computeStalenessAsText(this.model ? this.model.lastHeartbeat : null);
    }

    async getStalenessStatus(): Promise<Models.DeploymentOperationalResponsiveness>
    {
        let host = await this.getOwningDeployment();

        return DeploymentHostExtended.computeStaleness(this.getStaleness(), host.model.warningThreshold);
    }

    //--//

    async isOnline(): Promise<boolean>
    {
        try
        {
            return await this.domain.apis.deploymentAgents.checkOnline(this.model.sysId);
        }
        catch (e)
        {
            return false;
        }
    }

    openShell(cmd: string): Promise<Models.ShellToken>
    {
        return this.domain.apis.deploymentAgents.openShell(this.model.sysId, cmd);
    }

    closeShell(token: Models.ShellToken): Promise<boolean>
    {
        return this.domain.apis.deploymentAgents.closeShell(this.model.sysId, token.id);
    }

    readFromShell(token: Models.ShellToken): Promise<Models.ShellOutput[]>
    {
        return this.domain.apis.deploymentAgents.readFromShell(this.model.sysId, token.id);
    }

    writeToShell(token: Models.ShellToken,
                 line: string): Promise<boolean>
    {
        let input = Models.ShellInput.newInstance({
                                                      fd  : 1,
                                                      text: line
                                                  });

        return this.domain.apis.deploymentAgents.writeToShell(this.model.sysId, token.id, input);
    }

    //--//

    canActivate(): boolean
    {
        return this.model.status == Models.DeploymentStatus.Ready && this.model.active == false;
    }

    //--//

    async makeActive(): Promise<boolean>
    {
        return this.domain.apis.deploymentAgents.makeActive(this.model.sysId);
    }

    //--//

    async checkTerminate(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.deploymentAgents.terminate(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async terminate(): Promise<Models.ValidationResults>
    {
        let res = await this.domain.apis.deploymentAgents.terminate(this.model.sysId);
        if (res.entries.length == 0)
        {
            this.model.status = DeploymentStatus.Terminating;
        }
        return res;
    }

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.deploymentAgents.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.deploymentAgents.remove(this.model.sysId);
    }
}

export type DeploymentAgentChangeSubscription = SharedSvc.DbChangeSubscription<Models.DeploymentAgent>;

//--//

export function formatVoltage(val: number): string
{
    return isFinite(val) ? `${val.toFixed(1)}V` : "N/A";
}

export function formatTemperature(val: number): string
{
    return isFinite(val) ? `${val.toFixed(1)}°C` : "N/A";
}
