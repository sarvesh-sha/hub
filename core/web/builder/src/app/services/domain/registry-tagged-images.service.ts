import {Injectable} from "@angular/core";
import {ReportError} from "app/app.service";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {JobExtended} from "app/services/domain/jobs.service";
import {RegistryImageDecoded, RegistryImageExtended} from "app/services/domain/registry-images.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {inParallel} from "framework/utils/concurrency";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class RegistryTaggedImagesService extends SharedSvc.BaseService<Models.RegistryTaggedImage, RegistryTaggedImageExtended>
{
    private m_detailsByReleaseStatus: { [key: string]: ReleaseStatusDetails } = {};

    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.RegistryTaggedImage, RegistryTaggedImageExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.RegistryTaggedImage.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.RegistryTaggedImage>
    {
        return this.api.registryTaggedImages.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.RegistryTaggedImage[]>
    {
        return this.api.registryTaggedImages.getBatch(ids);
    }

    //--//

    public flushReleaseStatus()
    {
        this.m_detailsByReleaseStatus = {};
    }

    public async reportByReleaseStatus(status: Models.RegistryImageReleaseStatus,
                                       role?: Models.DeploymentRole): Promise<ReleaseStatusDetails>
    {
        let key     = `${status} ${role}`;
        let details = this.m_detailsByReleaseStatus[key];
        if (!details || !await details.isUpToDate())
        {
            let report = await this.api.registryTaggedImages.report(status);

            details = new ReleaseStatusDetails(status);
            await inParallel(report, async (entry) =>
            {
                if (role && entry.role != role) return;

                let image = await this.getExtendedByIdentity(entry.image);

                details.entries.push(new ReleaseStatusDetail(entry.role, entry.architecture, image));
            });

            this.m_detailsByReleaseStatus[key] = details;
        }

        return details;
    }
}

export class RegistryTaggedImageExtended extends SharedSvc.ExtendedModel<Models.RegistryTaggedImage>
{
    static newInstance(svc: RegistryTaggedImagesService,
                       model: Models.RegistryTaggedImage): RegistryTaggedImageExtended
    {
        return new RegistryTaggedImageExtended(svc, model, Models.RegistryTaggedImage.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.RegistryTaggedImage.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public async getOwingJob(): Promise<JobExtended>
    {
        return this.domain.jobs.getExtendedByIdentity(this.model.owningJob);
    }

    @Memoizer
    public async getImage(): Promise<RegistryImageExtended>
    {
        return await this.domain.registryImages.getExtendedByIdentity(this.model.image);
    }

    @Memoizer
    public getParsedImage(): RegistryImageDecoded
    {
        return this.model.tag ? new RegistryImageDecoded(this.model.tag) : null;
    }

    @Memoizer
    public async getUsage(): Promise<Models.RegistryTaggedImageUsage>
    {
        return this.domain.apis.registryTaggedImages.getUsage(this.model.sysId);
    }

    @Memoizer
    public async isInUse(): Promise<boolean>
    {
        let usage = await this.getUsage();

        return usage.isRC || usage.isRTM || (usage.services || []).length > 0 || (usage.backups || []).length > 0 || (usage.tasks || []).length > 0;
    }

    public get releaseOrder(): number
    {
        switch (this.model.releaseStatus)
        {
            case Models.RegistryImageReleaseStatus.Release:
                return 0;

            case Models.RegistryImageReleaseStatus.ReleaseCandidate:
                return 1;

            default:
                return 2;
        }
    }

    //--//

    public async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.registryTaggedImages.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    public async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.registryTaggedImages.remove(this.model.sysId);
    }

    //--//

    public markForRelease(status: Models.RegistryImageReleaseStatus): Promise<Models.RegistryTaggedImage>
    {
        this.domain.registryTaggedImages.flushReleaseStatus();

        return this.domain.apis.registryTaggedImages.mark(this.model.sysId, status);
    }

    public distribute(status?: Models.DeploymentOperationalStatus,
                      hostId?: string): Promise<number>
    {
        return this.domain.apis.registryTaggedImages.distribute(this.model.sysId, status, hostId);
    }
}

export type RegistryTaggedImageChangeSubscription = SharedSvc.DbChangeSubscription<Models.RegistryTaggedImage>;

export class ReleaseStatusDetails
{
    entries: ReleaseStatusDetail[] = [];

    constructor(public readonly status: Models.RegistryImageReleaseStatus)
    {
    }

    findMatch(role: Models.DeploymentRole,
              architecture: Models.DockerImageArchitecture): ReleaseStatusDetail
    {
        for (let image of this.entries)
        {
            if (image.role == role && image.architecture == architecture)
            {
                return image;
            }
        }

        return null;
    }

    async isUpToDate(): Promise<boolean>
    {
        for (let entry of this.entries)
        {
            let image = await entry.image.refresh();
            if (image.model.releaseStatus != this.status)
            {
                return false;
            }
        }

        return true;
    }
}

export class ReleaseStatusDetail
{
    constructor(public readonly role: Models.DeploymentRole,
                public readonly architecture: Models.DockerImageArchitecture,
                public readonly image: RegistryTaggedImageExtended)
    {
    }
}
