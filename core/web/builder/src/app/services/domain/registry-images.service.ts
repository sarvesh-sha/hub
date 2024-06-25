import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentTaskExtended} from "app/services/domain/deployment-tasks.service";
import {JobExtended} from "app/services/domain/jobs.service";
import {RegistryTaggedImageExtended} from "app/services/domain/registry-tagged-images.service";
import * as Models from "app/services/proxy/model/models";
import {BackgroundActivityStatus, DockerImageArchitecture, RegistryImageReleaseStatus} from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {Future} from "framework/utils/concurrency";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class RegistryImagesService extends SharedSvc.BaseService<Models.RegistryImage, RegistryImageExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.RegistryImage, RegistryImageExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.RegistryImage.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.RegistryImage>
    {
        return this.api.registryImages.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.RegistryImage[]>
    {
        return this.api.registryImages.getBatch(ids);
    }

    //--//

    public async getExtendedAll(): Promise<RegistryImageExtended[]>
    {
        let ids = await this.api.registryImages.getAll();
        return this.getExtendedBatch(ids);
    }

    public async findBySha(imageSha: string): Promise<RegistryImageExtended>
    {
        let image = await this.api.registryImages.findBySha(imageSha);
        return this.wrapModel(image);
    }

    async refresh(): Promise<Models.RegistryRefresh>
    {
        let sysId = await this.api.registryImages.startRefresh();
        while (true)
        {
            let res = await this.api.registryImages.checkRefresh(sysId, true);
            if (res.status == BackgroundActivityStatus.COMPLETED)
            {
                return res;
            }

            await Future.delayed(100);
        }
    }
}

export class RegistryImageExtended extends SharedSvc.ExtendedModel<Models.RegistryImage>
{
    static newInstance(svc: RegistryImagesService,
                       model: Models.RegistryImage): RegistryImageExtended
    {
        return new RegistryImageExtended(svc, model, Models.RegistryImage.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.RegistryImage.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public async getReferencingTags(): Promise<RegistryTaggedImageExtended[]>
    {
        return this.domain.registryTaggedImages.getExtendedBatch(this.model.referencingTags);
    }

    @Memoizer
    public async getReferencingTasks(): Promise<DeploymentTaskExtended[]>
    {
        return this.domain.deploymentTasks.getExtendedBatch(this.model.referencingTasks);
    }

    @Memoizer
    public async getRunningReferencingTasks(): Promise<DeploymentTaskExtended[]>
    {
        let res = [];

        for (let task of await this.getReferencingTasks())
        {
            if (task.model.status == Models.DeploymentStatus.Ready)
            {
                res.push(task);
            }
        }

        return res;
    }

    @Memoizer
    public async isInUse(): Promise<boolean>
    {
        let running = await this.getRunningReferencingTasks();
        return running.length > 0;
    }

    public getTargetService(): Models.DeploymentRole
    {
        return <any>this.model.labels["Optio3_TargetService"];
    }

    public isDeployerForArm(): boolean
    {
        switch (this.model.architecture)
        {
            case DockerImageArchitecture.ARM:
            case DockerImageArchitecture.ARMv6:
            case DockerImageArchitecture.ARMv7:
                return this.getTargetService() == Models.DeploymentRole.deployer;

            default:
                return false;
        }
    }

    public isCompatibleWithTarget(target: Models.DockerImageArchitecture)
    {
        if (this.model.architecture == target)
        {
            return true;
        }

        switch (this.model.architecture)
        {
            case DockerImageArchitecture.ARM:
            case DockerImageArchitecture.ARMv7:
                switch (target)
                {
                    case DockerImageArchitecture.ARM:
                    case DockerImageArchitecture.ARMv7:
                        // ARM and ARMv7 are synonyms, due to legacy reasons.
                        return true;
                }
                break;
        }

        return false;
    }

    public async selectTag(): Promise<{ taggedImage: RegistryTaggedImageExtended, desc: string }>
    {
        let tagRegular: RegistryTaggedImageExtended;

        for (let tag of await this.getReferencingTags())
        {
            return {
                taggedImage: tag,
                desc       : tag.model.tag
            };
        }

        return null;
    }
}

export type RegistryImageChangeSubscription = SharedSvc.DbChangeSubscription<Models.RegistryImage>;

export class RegistryImageDecoded
{
    readonly fullName: string;

    registryHost: string;
    registryPort: number;

    account: string;
    name: string;
    tag: string;

    constructor(image: string)
    {
        this.fullName = image;

        let regHost: string;
        let regPort: number;

        let hostPos = image.indexOf("/");
        if (hostPos >= 0)
        {
            let hostPart = image.substring(0, hostPos);

            let hasDomainName = hostPart.indexOf(".") >= 0;
            let portPos       = hostPart.indexOf(":");

            // Either it's "registry.domain.com" or "somelocalhost:port".
            if (hasDomainName || portPos > 0)
            {
                if (portPos > 0)
                {
                    regPort = Number(hostPart.substring(portPos + 1));
                    regHost = hostPart.substring(0, portPos);
                }
                else
                {
                    regHost = hostPart;
                }

                image = image.substring(hostPos + 1);
            }
        }

        this.registryHost = regHost;
        this.registryPort = regPort;

        let accountPos = image.indexOf("/");
        if (accountPos >= 0)
        {
            this.account = image.substring(0, accountPos);

            image = image.substring(accountPos + 1);
        }
        else
        {
            this.account = null;
        }

        let tagPos = image.indexOf(":");
        if (tagPos >= 0)
        {
            this.name = image.substring(0, tagPos);
            this.tag  = image.substring(tagPos + 1);
        }
        else
        {
            this.name = image;
            this.tag  = null;
        }
    }


    //--//

    getFullName(): string
    {
        return this.emitRegistryAddress() + this.emitAccountAndName() + this.emitTag();
    }

    getLocalName(): string
    {
        return this.emitAccountAndName() + this.emitTag();
    }

    getRepositoryName(): string
    {
        return this.emitRegistryAddress() + this.emitAccountAndName();
    }

    getRegistryAddress(): string
    {
        return !this.registryHost ? null : this.emitRegistryAddress();
    }

    getAccountAndName(): string
    {
        return this.emitAccountAndName();
    }

    //--//

    private emitRegistryAddress(): string
    {
        if (!this.registryHost)
        {
            return "";
        }

        let text = this.registryHost;

        if (this.registryPort)
        {
            text += `:${this.registryPort}`;
        }

        text += "/";

        return text;
    }

    private emitAccountAndName(): string
    {
        let text = "";

        if (this.account)
        {
            text += `${this.account}/`;
        }

        text += this.name;

        return text;
    }

    private emitTag(): string
    {
        return this.tag ? `:${this.tag}` : "";
    }
}

export class RegistryImageDependencies
{
    image: RegistryImageExtended;
    tagged: RegistryTaggedImageExtended;

    job: JobExtended;

    usage: Models.RegistryTaggedImageUsage;

    readonly usages = new ImageUsageDetails();

    accountAndName: string;
    tag: string;
    releaseStatus: Models.RegistryImageReleaseStatus;

    //--//

    async init(ext: RegistryTaggedImageExtended)
    {
        this.tagged = ext;
        this.image  = await ext.getImage();

        this.usage = await ext.getUsage();

        if (this.tagged)
        {
            this.job           = await this.tagged.getOwingJob();
            this.releaseStatus = this.tagged.model.releaseStatus;

            let parsedTag = this.tagged.getParsedImage();
            if (parsedTag)
            {
                this.accountAndName = parsedTag.getAccountAndName();
                this.tag            = parsedTag.tag;
            }

            if (this.releaseStatus != RegistryImageReleaseStatus.None)
            {
                this.usages.addEntry(`Tagged as ${this.releaseStatus}`, this.tagged);
            }
        }

        if (this.job)
        {
            this.usages.addEntry(`Job '${this.job.model.name}' built on ${MomentHelper.parse(this.job.model.createdOn)
                                                                                      .format("ll")}`, this.job);
        }

        for (let taskSysId of this.usage.tasks)
        {
            let task = this.usage.lookupTask[taskSysId];
            let host = this.usage.lookupHost[task.deployment.sysId];
            let id   = host.hostName || host.hostId;

            this.usages.addEntry(`Task for ${task.name} on host ${id}`, task);
        }

        for (let serviceSysId of this.usage.services)
        {
            let service = this.usage.lookupService[serviceSysId];

            this.usages.addEntry(`Service for '${service.name} - ${service.url}'`, service);
        }

        for (let backupSysId of this.usage.backups)
        {
            let backup  = this.usage.lookupBackup[backupSysId];
            let service = this.usage.lookupService[backup.customerService.sysId];

            this.usages.addEntry(`Backup '${backup.fileId}' for service '${service.name} - ${service.url}'`, backup);
        }
    }

    //--//

    get service(): string
    {
        return <any>this.image.getTargetService() || "<No associated service>";
    }
}

export class ImageUsageDetails
{
    entries: ImageUsageDetail[] = [];

    isInUse(): boolean
    {
        return this.entries.length > 0;
    }

    inUseText(summary: boolean): string
    {
        if (summary)
        {
            switch (this.entries.length)
            {
                case 0:
                    return "No";
                case 1:
                    return "Yes";
                default:
                    return `Yes (${this.entries.length})`;
            }
        }

        return this.entries.map(detail => detail.reason)
                   .join("\n");
    }

    addEntry(text: string,
             context: any)
    {
        let obj     = new ImageUsageDetail();
        obj.reason  = text;
        obj.context = context;

        this.entries.push(obj);
    }

    compare(other: ImageUsageDetails): number
    {
        return this.entries.length - other.entries.length;
    }
}

export class ImageUsageDetail
{
    reason: string;
    context: any;
}
