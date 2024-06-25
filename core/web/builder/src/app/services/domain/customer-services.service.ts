import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";

import {ApiService} from "app/services/domain/api.service";
import {BackgroundActivityExtended} from "app/services/domain/background-activities.service";
import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceBackupExtended} from "app/services/domain/customer-service-backups.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";
import {EnumsService} from "app/services/domain/enums.service";
import {JobExtended} from "app/services/domain/jobs.service";
import {RegistryTaggedImageExtended, ReleaseStatusDetail, ReleaseStatusDetails} from "app/services/domain/registry-tagged-images.service";

import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {ControlOption} from "framework/ui/control-option";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {Future} from "framework/utils/concurrency";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class CustomerServicesService extends SharedSvc.BaseService<Models.CustomerService, CustomerServiceExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService,
                private enums: EnumsService)
    {
        super(api, errors, cache, Models.CustomerService, CustomerServiceExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.CustomerService.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.CustomerService>
    {
        return this.api.customerServices.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.CustomerService[]>
    {
        return this.api.customerServices.getBatch(ids);
    }

    //--//

    describeInstanceTypes(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("DeploymentInstance", false);
    }

    async describeInstanceType(mode: Models.DeploymentInstance): Promise<Models.EnumDescriptor>
    {
        return EnumsService.find(await this.describeInstanceTypes(), mode);
    }

    @Memoizer
    async getInstanceTypes(): Promise<ControlOption<Models.DeploymentInstance>[]>
    {
        let types = await this.describeInstanceTypes();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    describeCustomerVerticals(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("CustomerVertical", false);
    }

    async describeCustomerVertical(mode: Models.CustomerVertical): Promise<Models.EnumDescriptor>
    {
        return EnumsService.find(await this.describeCustomerVerticals(), mode);
    }

    @Memoizer
    async getCustomerVerticals(): Promise<ControlOption<Models.CustomerVertical>[]>
    {
        let types = await this.describeCustomerVerticals();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    @ReportError
    async upgradeBulk(targets: CustomerServiceExtended[],
                      useRC: boolean,
                      justHub: boolean,
                      justGateway: boolean)
    {
        let candidate = 0;
        let busy      = 0;
        let upgrades  = 0;
        let noChanges = 0;

        let imagesForReleaseCandidate = await this.domain.registryTaggedImages.reportByReleaseStatus(Models.RegistryImageReleaseStatus.ReleaseCandidate);
        let imagesForRelease          = await this.domain.registryTaggedImages.reportByReleaseStatus(Models.RegistryImageReleaseStatus.Release);

        for (let svc of targets)
        {
            if (svc.model.operationalStatus != Models.DeploymentOperationalStatus.operational)
            {
                this.logger.info(`Skipped ${svc.model.name} because service is not operational...`);
                continue;
            }

            if (!(await svc.isReadyForChange()))
            {
                this.logger.info(`Skipped ${svc.model.name} because service is busy...`);
                busy++;
                continue;
            }

            let state = await svc.getState();
            if (!state.hasTasks() || !state.hasHosts())
            {
                this.logger.info(`Skipped ${svc.model.name} because service is not running...`);
                continue;
            }

            let upgradeStatus = state.checkUpgrade(imagesForReleaseCandidate, imagesForRelease);

            if (useRC)
            {
                if (!(upgradeStatus.hasAnyImagesForReleaseCandidate && upgradeStatus.hasAllImagesForReleaseCandidate))
                {
                    continue;
                }
            }
            else
            {
                if (!upgradeStatus.hasAllImagesForRelease)
                {
                    continue;
                }
            }

            candidate++;

            let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                  roles       : [],
                                                                                  createBackup: svc.model.relaunchAlways ? undefined : Models.BackupKind.Upgrade
                                                                              });

            if (!await state.enumerateHostsAsync(false,
                                                 async (role,
                                                        host) =>
                                                 {
                                                     let arch = host.architecture;
                                                     if (!svc.findDesiredState(desiredState, role, arch))
                                                     {
                                                         let targetRelease: ReleaseStatusDetail;

                                                         if (useRC)
                                                         {
                                                             targetRelease = upgradeStatus.imagesForReleaseCandidate.findMatch(role, arch);
                                                             if (!targetRelease)
                                                             {
                                                                 targetRelease = upgradeStatus.imagesForRelease.findMatch(role, arch);
                                                             }
                                                         }
                                                         else
                                                         {
                                                             targetRelease = upgradeStatus.imagesForRelease.findMatch(role, arch);
                                                         }

                                                         if (targetRelease == null)
                                                         {
                                                             this.logger.error(`Skipped upgrade to ${svc.model.name} because no image for role ${role}...`);
                                                             return false;
                                                         }

                                                         let image = targetRelease.image;
                                                         if (!image)
                                                         {
                                                             this.logger.error(`Skipped upgrade to ${svc.model.name} because no image for role ${role}...`);
                                                             return false;
                                                         }

                                                         let shouldUpgrade: boolean;

                                                         if (justHub && role != Models.DeploymentRole.hub)
                                                         {
                                                             shouldUpgrade = false;
                                                         }
                                                         else if (justGateway && role != Models.DeploymentRole.gateway)
                                                         {
                                                             shouldUpgrade = false;
                                                         }
                                                         else
                                                         {
                                                             shouldUpgrade = true;
                                                         }

                                                         if (!shouldUpgrade)
                                                         {
                                                             image = await svc.getImage(role, arch);
                                                             if (image == null)
                                                             {
                                                                 this.logger.error(`Skipped upgrade to ${svc.model.name} because no image for role ${role}...`);
                                                                 return false;
                                                             }

                                                             let roleSpec = svc.addImageToDesiredState(desiredState, role, arch, image);
                                                             // Don't start or stop, just create the role.
                                                             return true;
                                                         }

                                                         let roleSpec    = svc.addImageToDesiredState(desiredState, role, arch, image);
                                                         roleSpec.launch = true;

                                                         if (justGateway)
                                                         {
                                                             roleSpec.shutdownIfDifferent = true;
                                                         }
                                                         else
                                                         {
                                                             roleSpec.shutdown = true;
                                                         }
                                                     }

                                                     return true;
                                                 }))
            {
                continue;
            }

            let sameImages = true;

            for (let role of desiredState.roles)
            {
                let existingImage = svc.findImageSpec(role.role, role.architecture);
                let desiredImage  = role.image;

                if (existingImage && desiredImage && existingImage.image.sysId != desiredImage.sysId)
                {
                    sameImages = false;
                    break;
                }
            }

            if (sameImages)
            {
                noChanges++;
            }
            else
            {
                await svc.applyDesiredState(desiredState);
                upgrades++;
            }
        }

        if (busy == 0 && candidate == 0)
        {
            this.errors.success(`No matching services for ${useRC ? "RC" : "RTM"} build!`, -1);
            return;
        }

        if (upgrades == 0)
        {
            if (busy > 0)
            {
                this.errors.success(`${busy} upgrade(s) already active...`, -1);
                return;
            }

            this.errors.success(`Nothing to do, all services up-to-date.`, -1);
            return;
        }

        let msg = `Started ${upgrades} upgrade(s)`;

        if (busy > 0)
        {
            msg = `${msg}, ${busy} upgrade(s) already active`;
        }

        if (noChanges > 0)
        {
            msg = `${msg}, ${noChanges} service(s) already running target build`;
        }

        this.errors.success(`${msg}...`, -1);
    }

    //--//

    describeDbModes(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("DatabaseMode", false);
    }

    async describeDbMode(mode: Models.DatabaseMode): Promise<Models.EnumDescriptor>
    {
        return EnumsService.find(await this.describeDbModes(), mode);
    }

    @Memoizer
    async getDbModes(): Promise<ControlOption<Models.DatabaseMode>[]>
    {
        let types = await this.describeDbModes();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    getAllCharges(maxTopHosts: number): Promise<Models.DeploymentCellularChargesSummary>
    {
        return this.domain.apis.customers.getAllCharges(maxTopHosts);
    }

    //--//

    async save(extended: CustomerServiceExtended): Promise<CustomerServiceExtended>
    {
        if (extended.model.sysId)
        {
            this.flush(extended);

            await this.api.customerServices.update(extended.model.sysId, undefined, extended.model);

            return this.refreshExtended(extended);
        }
        else
        {
            let customer = await extended.getOwningCustomer();

            let newModel = await this.api.customerServices.create(customer.model.sysId, extended.model);
            return this.wrapModel(newModel);
        }
    }
}

export class CustomerServiceExtended extends SharedSvc.ExtendedModel<Models.CustomerService>
{
    static newInstance(svc: CustomerServicesService,
                       model: Models.CustomerService): CustomerServiceExtended
    {
        return new CustomerServiceExtended(svc, model, Models.CustomerService.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.CustomerService.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    get instanceTypeDisplay(): string
    {
        let res = "<unknown>";

        if (this.model.instanceType)
        {
            res = this.model.instanceType;

            if (this.model.instanceRegion)
            {
                res = `${res} / ${this.model.instanceRegion}`;
            }

            if (this.model.instanceAccount)
            {
                res = `${res} / ${this.model.instanceAccount}`;
            }
        }

        return res;
    }

    @Memoizer
    public getOwningCustomer(): Promise<CustomerExtended>
    {
        return this.domain.customers.getExtendedByIdentity(this.model.customer);
    }

    @Memoizer
    public getCurrentActivity(): Promise<BackgroundActivityExtended>
    {
        return this.domain.backgroundActivities.getExtendedByIdentity(this.model.currentActivity);
    }

    @Memoizer
    public async getCurrentRunningActivity(): Promise<BackgroundActivityExtended>
    {
        let pendingActivity = await this.getCurrentActivity();
        if (pendingActivity)
        {
            switch (pendingActivity.model.status)
            {
                case Models.BackgroundActivityStatus.COMPLETED:
                case Models.BackgroundActivityStatus.FAILED:
                    break;

                default:
                    return pendingActivity;
            }
        }

        return null;
    }

    @Memoizer
    async getImages(): Promise<{ [key: string]: RegistryTaggedImageExtended }>
    {
        let images: { [key: string]: RegistryTaggedImageExtended } = {};
        for (let imageSpec of this.model.roleImages)
        {
            images[imageSpec.image.sysId] = await this.getImage(imageSpec.role, imageSpec.architecture);
        }
        return images;
    }

    findExistingImage(images: { [key: string]: RegistryTaggedImageExtended },
                      role: Models.DeploymentRole,
                      arch: Models.DockerImageArchitecture)
    {
        let imageSpec = this.findImageSpec(role, arch);
        if (imageSpec == null) return null;

        return images[imageSpec.image.sysId];
    }

    addImage(role: Models.DeploymentRole,
             architecture: Models.DockerImageArchitecture,
             image: RegistryTaggedImageExtended): Promise<boolean>
    {
        let cfg = Models.RoleAndArchitectureWithImage.newInstance({
                                                                      role        : role,
                                                                      architecture: architecture,
                                                                      image       : image.getIdentity()
                                                                  });

        return this.domain.apis.customerServices.addImage(this.model.sysId, cfg);
    }

    //--//

    filterLog(filters: Models.LogEntryFilterRequest): Promise<Models.LogRange[]>
    {
        return this.domain.apis.customerServices.filterLog(this.model.sysId, filters);
    }

    getLog(fromOffset?: number,
           toOffset?: number,
           limit?: number): Promise<Models.LogLine[]>
    {
        return this.domain.apis.customerServices.getLog(this.model.sysId, fromOffset, toOffset, limit);
    }

    deleteLog(olderThan?: number): Promise<number>
    {
        return this.domain.apis.customerServices.deleteLog(this.model.sysId, olderThan);
    }

    //--//

    @ReportError
    async save(): Promise<CustomerServiceExtended>
    {
        return this.domain.customerServices.save(this);
    }

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.customerServices.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.customerServices.remove(this.model.sysId);
    }

    //--//

    @ReportError
    async createSecret(model: Models.CustomerServiceSecret): Promise<Models.CustomerServiceSecret>
    {
        return this.domain.apis.customerServiceSecrets.create(this.model.sysId, model);
    }

    //--//

    getCharges(maxTopHosts: number): Promise<Models.DeploymentCellularChargesSummary>
    {
        return this.domain.apis.customerServices.getCharges(this.model.sysId, maxTopHosts);
    }

    async getHeapStatusHistory(): Promise<Models.HubHeapAndThreads[]>
    {
        let entries = await this.domain.apis.customerServices.getHeapStatusHistory(this.model.sysId);

        entries.sort((a,
                      b) => MomentHelper.compareDates(b.timestamp, a.timestamp));

        return entries;
    }

    //--//

    async getLatestBackup(): Promise<CustomerServiceBackupExtended>
    {
        let lastBackup = null;

        for (let backup of await this.domain.customerServiceBackups.getExtendedBatch(this.model.backups))
        {
            if (!lastBackup || MomentHelper.compareDates(lastBackup.model.createdOn, backup.model.createdOn) < 0)
            {
                lastBackup = backup;
            }
        }

        return lastBackup;
    }

    async migrate()
    {
        await this.domain.apis.customerServices.migrate(this.model.sysId);
    }

    //--//

    async extractJobs(): Promise<JobExtended[]>
    {
        let jobs = [];

        for (let imageSpec of this.model.roleImages || [])
        {
            let image = await this.domain.registryTaggedImages.getExtendedByIdentity(imageSpec.image);
            if (image)
            {
                let job = await image.getOwingJob();
                for (let job2 of jobs)
                {
                    if (job.sameIdentity(job2))
                    {
                        job = null;
                        break;
                    }
                }

                if (job)
                {
                    jobs.push(job);
                }
            }
        }

        return jobs;
    }

    //--//

    findImageSpec(role: Models.DeploymentRole,
                  architecture: Models.DockerImageArchitecture): Models.RoleAndArchitectureWithImage
    {
        for (let imageSpec of this.model.roleImages || [])
        {
            if (imageSpec.role == role && imageSpec.architecture == architecture)
            {
                return imageSpec;
            }
        }

        return null;
    }

    async getImage(role: Models.DeploymentRole,
                   architecture: Models.DockerImageArchitecture): Promise<RegistryTaggedImageExtended>
    {
        let imageSpec = this.findImageSpec(role, architecture);
        if (!imageSpec)
        {
            return null;
        }

        return await this.domain.registryTaggedImages.getExtendedByIdentity(imageSpec.image);
    }

    @Memoizer
    async isReadyForChange(): Promise<boolean>
    {
        let activity = await this.getCurrentActivity();
        if (activity)
        {
            switch (activity.model.status)
            {
                case Models.BackgroundActivityStatus.COMPLETED:
                case Models.BackgroundActivityStatus.FAILED:
                    return true;

                default:
                    return false;
            }
        }

        return true;
    }

    async deploy(cfg: Models.DeploymentHostConfig): Promise<DeploymentHostExtended>
    {
        let host = await this.domain.apis.customerServices.deploy(this.model.sysId, cfg);
        return this.domain.deploymentHosts.wrapModel(host);
    }

    findDesiredState(state: Models.CustomerServiceDesiredState,
                     role: Models.DeploymentRole,
                     architecture: Models.DockerImageArchitecture): Models.CustomerServiceDesiredStateRole
    {
        for (let imageSpec of state.roles || [])
        {
            if (imageSpec.role == role && imageSpec.architecture == architecture)
            {
                return imageSpec;
            }
        }

        return null;
    }

    addImageToDesiredState(state: Models.CustomerServiceDesiredState,
                           role: Models.DeploymentRole,
                           architecture: Models.DockerImageArchitecture,
                           image: RegistryTaggedImageExtended): Models.CustomerServiceDesiredStateRole
    {
        let roleSpec = Models.CustomerServiceDesiredStateRole.newInstance({
                                                                              role                  : role,
                                                                              architecture          : architecture,
                                                                              shutdown              : false,
                                                                              shutdownIfDifferent   : false,
                                                                              launch                : false,
                                                                              launchIfMissing       : false,
                                                                              launchIfMissingAndIdle: false,
                                                                              image                 : image ? image.getIdentity() : null
                                                                          });

        state.roles.push(roleSpec);

        return roleSpec;
    }

    //--//

    @ReportError
    async compactTimeSeries()
    {
        await this.domain.apis.customerServices.compactTimeSeries(this.model.sysId);
    }

    async checkUsages(filters: Models.UsageFilterRequest): Promise<Models.UsageFilterResponse>
    {
        let sysId = await this.domain.apis.customerServices.checkUsages(this.model.sysId, filters);
        if (!sysId) return null;

        while (true)
        {
            let res = await this.domain.apis.customerServices.checkUsagesProgress(sysId, true);
            if (res.status == Models.BackgroundActivityStatus.COMPLETED)
            {
                return res.results;
            }

            await Future.delayed(100);
        }
    }

    formatUsages(response: Models.UsageFilterResponse,
                 ignoreNoHits: boolean): string[]
    {
        let lines: string[] = [];

        if (!response)
        {
            if (ignoreNoHits) return null;
            lines.push("No response, not supported yet...");
        }
        else
        {
            if (response.userPreferenceHits > 0)
            {
                lines.push(`${response.userPreferenceHits} hit(s) in User Preferences`);
            }

            if (response.systemPreferenceHits > 0)
            {
                lines.push(`${response.systemPreferenceHits} hit(s) in System Preferences`);
            }

            if (response.dashboardHits > 0)
            {
                lines.push(`${response.dashboardHits} hit(s) in Dashboards`);
            }

            if (response.alertDefinitionVersionHits > 0)
            {
                lines.push(`${response.alertDefinitionVersionHits} hit(s) in Alert Definitions`);
            }

            if (response.metricsDefinitionVersionHits > 0)
            {
                lines.push(`${response.metricsDefinitionVersionHits} hit(s) in Metrics Definitions`);
            }

            if (response.normalizationVersionHits > 0)
            {
                lines.push(`${response.normalizationVersionHits} hit(s) in Normalization Rules`);
            }

            if (response.reportDefinitionVersionHits > 0)
            {
                lines.push(`${response.reportDefinitionVersionHits} hit(s) in Report Definitions`);
            }

            if (response.workflowHits > 0)
            {
                lines.push(`${response.workflowHits} hit(s) in Workflows`);
            }

            if (lines.length == 0)
            {
                if (ignoreNoHits) return null;

                lines.push("No hits!");
            }
        }

        return lines;
    }

    //--//

    @ReportError
    async updateUpgradeBlocker(blockUntil?: Date)
    {
        await this.domain.apis.customerServices.manageUpgradeBlocker(this.model.sysId, blockUntil);
    }

    @ReportError
    async updateAlertThreshold(role: Models.DeploymentRole,
                               warningThreshold: number,
                               alertThreshold: number)
    {
        await this.domain.apis.customerServices.setAlertThresholds(this.model.sysId, role, warningThreshold, alertThreshold);
    }

    @ReportError
    async updateBatteryThreshold(cfg: Models.DeployerShutdownConfiguration)
    {
        await this.domain.apis.customerServices.setBatteryThresholds(this.model.sysId, cfg);
    }

    @ReportError
    async refreshCertificate(): Promise<BackgroundActivityExtended>
    {
        let activity = await this.domain.apis.customerServices.refreshCertificate(this.model.sysId);
        return this.domain.backgroundActivities.wrapModel(activity);
    }

    @ReportError
    async applyDesiredState(state: Models.CustomerServiceDesiredState): Promise<BackgroundActivityExtended>
    {
        let activity = await this.domain.apis.customerServices.desiredState(this.model.sysId, state);
        return this.domain.backgroundActivities.wrapModel(activity);
    }

    async startBackup(): Promise<BackgroundActivityExtended>
    {
        let activity = await this.domain.apis.customerServices.backup(this.model.sysId);
        return this.domain.backgroundActivities.wrapModel(activity);
    }

    async refreshUsers(): Promise<BackgroundActivityExtended>
    {
        let activity = await this.domain.apis.customerServices.refreshAccounts(this.model.sysId);
        return this.domain.backgroundActivities.wrapModel(activity);
    }

    async refreshSecrets(): Promise<BackgroundActivityExtended>
    {
        let activity = await this.domain.apis.customerServices.refreshSecrets(this.model.sysId);
        return this.domain.backgroundActivities.wrapModel(activity);
    }

    hasPurpose(purpose: Models.DeploymentRole): boolean
    {
        return this.model.purposes && this.model.purposes.indexOf(purpose) >= 0;
    }

    async getState(): Promise<CustomerServiceState>
    {
        let res = new CustomerServiceState();

        let filter                = new Models.DeploymentHostFilterRequest();
        filter.serviceSysid       = this.model.sysId;
        filter.includeFullDetails = true;
        let descriptors           = await this.domain.apis.deploymentHosts.describeFiltered(filter);

        for (let host of descriptors)
        {
            for (let role of host.roles)
            {
                if (role == Models.DeploymentRole.tracker)
                {
                    // Not part of service state, since it's not deployable.
                    continue;
                }

                addToMultimap(res.hostsPerRole, role, host);

                let tag = await this.getImage(role, host.architecture);
                if (tag)
                {
                    let image = await tag.getImage();

                    for (let taskSysid in host.tasks)
                    {
                        let task = host.tasks[taskSysid];

                        if (task.status == Models.DeploymentStatus.Ready && task.imageReference && task.imageReference.sysId == image.model.sysId)
                        {
                            addToMultimap(res.tasksPerRole, role, task);
                        }
                    }
                }
            }
        }

        return res;
    }
}

function addToMultimap<K, V>(map: Map<K, V[]>,
                             key: K,
                             value: V)
{
    let lst = map.get(key);
    if (!lst)
    {
        lst = [];
        map.set(key, lst);
    }

    lst.push(value);
}

export class CustomerServiceState
{
    public hostsPerRole = new Map<Models.DeploymentRole, Models.DeploymentHostStatusDescriptor[]>();

    public tasksPerRole = new Map<Models.DeploymentRole, Models.DeploymentTask[]>();

    public hasHosts(): boolean
    {
        return this.hostsPerRole.size > 0;
    }

    public hasTasks(): boolean
    {
        return this.tasksPerRole.size > 0;
    }

    public getRolesWithNoTasksOnHost(): Map<Models.DeploymentRole, Models.DeploymentHostStatusDescriptor[]>
    {
        let rolesWithNoTasksOnHost = new Map<Models.DeploymentRole, Models.DeploymentHostStatusDescriptor[]>();

        for (let role of this.hostsPerRole.keys())
        {
            let hosts = this.hostsPerRole.get(role) || [];
            for (let host of hosts)
            {
                let hasTask = false;

                for (let task of this.tasksPerRole.get(role) || [])
                {
                    if (task.deployment.sysId == host.ri.sysId)
                    {
                        hasTask = true;
                        break;
                    }
                }

                if (!hasTask)
                {
                    addToMultimap(rolesWithNoTasksOnHost, role, host);
                }
            }
        }

        return rolesWithNoTasksOnHost;
    }

    public async enumerateHostsAsync(includeMissing: boolean,
                                     callback: (role: Models.DeploymentRole,
                                                host: Models.DeploymentHostStatusDescriptor) => Promise<boolean>): Promise<boolean>
    {
        for (let role of this.hostsPerRole.keys())
        {
            let hosts = this.hostsPerRole.get(role);
            if (hosts)
            {
                for (let host of hosts)
                {
                    if (!await callback(role, host)) return false;
                }
            }
            else if (includeMissing)
            {
                if (!await callback(role, null)) return false;
            }
        }

        return true;
    }

    public enumerateHostsSync(includeMissing: boolean,
                              callback: (role: Models.DeploymentRole,
                                         host: Models.DeploymentHostStatusDescriptor) => boolean): boolean
    {
        for (let role of this.hostsPerRole.keys())
        {
            let hosts = this.hostsPerRole.get(role);
            if (hosts)
            {
                for (let host of hosts)
                {
                    if (!callback(role, host)) return false;
                }
            }
            else if (includeMissing)
            {
                if (!callback(role, null)) return false;
            }
        }

        return true;
    }

    public async enumerateTasksAsync(includeMissing: boolean,
                                     callback: (role: Models.DeploymentRole,
                                                task: Models.DeploymentTask) => Promise<boolean>): Promise<boolean>
    {
        for (let role of this.hostsPerRole.keys())
        {
            let tasks = this.tasksPerRole.get(role);
            if (tasks)
            {
                for (let task of tasks)
                {
                    if (!await callback(role, task)) return false;
                }
            }
            else if (includeMissing)
            {
                if (!await callback(role, null)) return false;
            }
        }

        return true;
    }

    public enumerateTasksSync(includeMissing: boolean,
                              callback: (role: Models.DeploymentRole,
                                         task: Models.DeploymentTask) => boolean): boolean
    {
        for (let role of this.hostsPerRole.keys())
        {
            let tasks = this.tasksPerRole.get(role);
            if (tasks)
            {
                for (let task of tasks)
                {
                    if (!callback(role, task)) return false;
                }
            }
            else if (includeMissing)
            {
                if (!callback(role, null)) return false;
            }
        }

        return true;
    }

    checkUpgrade(imagesForReleaseCandidate: ReleaseStatusDetails,
                 imagesForRelease: ReleaseStatusDetails): UpgradeStatus
    {
        let res                       = new UpgradeStatus();
        res.imagesForReleaseCandidate = imagesForReleaseCandidate;
        res.imagesForRelease          = imagesForRelease;

        res.hasAllImagesForRelease          = imagesForRelease.entries.length > 0;
        res.hasAllImagesForReleaseCandidate = imagesForReleaseCandidate.entries.length > 0;
        res.hasAnyImagesForReleaseCandidate = false;

        for (let role of this.hostsPerRole.keys())
        {
            let hosts = this.hostsPerRole.get(role);
            for (let host of hosts || [])
            {
                let imageForReleaseCandidate = res.imagesForReleaseCandidate.findMatch(role, host.architecture);
                let imageForRelease          = res.imagesForRelease.findMatch(role, host.architecture);

                if (!imageForRelease)
                {
                    res.hasAllImagesForRelease = false;
                }

                if (imageForReleaseCandidate)
                {
                    res.hasAnyImagesForReleaseCandidate = true;
                }

                if (!imageForReleaseCandidate && !imageForRelease)
                {
                    res.hasAllImagesForReleaseCandidate = false;
                }
            }
        }

        return res;
    }
}

export type CustomerServiceChangeSubscription = SharedSvc.DbChangeSubscription<Models.CustomerService>;

export class UpgradeStatus
{
    imagesForReleaseCandidate: ReleaseStatusDetails;
    imagesForRelease: ReleaseStatusDetails;

    hasAllImagesForRelease: boolean;
    hasAllImagesForReleaseCandidate: boolean;
    hasAnyImagesForReleaseCandidate: boolean;
}
