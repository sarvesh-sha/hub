import {Component, Injector, ViewChild} from "@angular/core";
import {DeploymentAgentUpgradeOverlay} from "app/customer/deployment-agents/deployment-agent-upgrade-overlay.component";
import {ImageDescriptor, RegistryImageSelectionDialogComponent} from "app/customer/registry-images/registry-image-selection-dialog.component";

import {AppDomainContext} from "app/services/domain";
import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceBackupExtended} from "app/services/domain/customer-service-backups.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {formatVoltage} from "app/services/domain/deployment-agents.service";
import {AgentsUpgradeSummary, DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";
import {DeploymentTaskExtended} from "app/services/domain/deployment-tasks.service";
import {JobExtended} from "app/services/domain/jobs.service";
import * as Models from "app/services/proxy/model/models";

import {LoggingSeverity} from "framework/services/logging.service";
import {UtilsService} from "framework/services/utils.service";

import {ColumnConfiguration, DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {DatatableContextMenuEvent, DatatableRowActivateEvent} from "framework/ui/datatables/datatable.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

import {inParallel, mapInParallel} from "framework/utils/concurrency";
import {Memoizer} from "framework/utils/memoizers";
import moment from "framework/utils/moment";

@Component({
               selector   : "o3-jobs-list",
               templateUrl: "./jobs-list.component.html"
           })
export class JobsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, JobExtended, JobDetail>
{
    table: DatatableManager<Models.RecordIdentity, JobExtended, JobDetail>;

    private readonly m_lookup: Map<string, JobDetail> = new Map<string, JobDetail>();

    private upgradeResults: AgentsUpgradeSummary;
    @ViewChild(DeploymentAgentUpgradeOverlay, {static: true}) upgradeOverlay: DeploymentAgentUpgradeOverlay;

    @ViewChild("servicesOverlay", {static: true}) servicesOverlay: OverlayComponent;
    selectedServices: ServiceDetailsProvider;

    @ViewChild("backupsOverlay", {static: true}) backupsOverlay: OverlayComponent;
    selectedBackups: BackupDetailsProvider;

    @ViewChild("tasksOverlay", {static: true}) tasksOverlay: OverlayComponent;
    selectedTasks: TaskDetailsProvider;

    constructor(inj: Injector)
    {
        super(inj);

        this.table              = this.newTableWithAutoRefresh(this.app.domain.jobs, this);
        this.table.defaultLimit = 25;

        // Pre-cache all the hosts and images.
        this.app.domain.registryImages.getExtendedAll();

        this.selectedServices = new ServiceDetailsProvider(this, []);
        this.selectedBackups  = new BackupDetailsProvider(this, []);
        this.selectedTasks    = new TaskDetailsProvider(this, []);
    }

    getItemName(): string
    {
        return "Jobs";
    }

    getList(): Promise<Models.RecordIdentity[]>
    {
        return this.app.domain.jobs.getList();
    }

    getPage(offset: number,
            limit: number): Promise<JobExtended[]>
    {
        return this.app.domain.jobs.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: JobExtended[]): Promise<JobDetail[]>
    {
        let details = [];

        for (let row of rows)
        {
            let detail = this.m_lookup.get(row.model.sysId);
            if (!detail)
            {
                detail = new JobDetail(this.app.domain);
                this.m_lookup.set(row.model.sysId, detail);
            }

            detail.extended = row;

            let momentStart = MomentHelper.parse(row.model.createdOn);
            let momentEnd: moment.Moment;

            if (row.model.status == Models.JobStatus.EXECUTING)
            {
                momentEnd = MomentHelper.now();
            }
            else
            {
                momentEnd = MomentHelper.parse(row.model.updatedOn);

            }

            detail.duration = momentEnd.from(momentStart, true);

            detail.computeInUse(this);

            details.push(detail);
        }

        return details;
    }

    itemClicked(columnId: string,
                item: JobDetail)
    {
        switch (columnId)
        {
            case "inServices":
                this.openServicesOverlay(item.getServices());
                break;

            case "inBackups":
                this.openBackupsOverlay(item.getBackups());
                break;

            case "inTasks":
                this.openTasksOverlay(item.getTasks());
                break;

            default:
                this.app.ui.navigation.push([
                                                "..",
                                                "item",
                                                item.extended.model.sysId
                                            ]);
        }
    }

    viewService(event: DatatableRowActivateEvent<ServiceDetails>)
    {
        this.app.ui.navigation.go("/customers/item", [
            event.row.customer.sysId,
            "service",
            event.row.service.sysId
        ]);
    }

    viewBackup(event: DatatableRowActivateEvent<BackupDetails>)
    {
        switch (event.columnId)
        {
            case "fileId":
                this.app.ui.navigation.go("/customers/item", [
                    event.row.customer.sysId,
                    "service",
                    event.row.service.sysId,
                    "backup",
                    event.row.backup.sysId
                ]);
                break;

            default:
                this.app.ui.navigation.go("/customers/item", [
                    event.row.customer.sysId,
                    "service",
                    event.row.service.sysId
                ]);
                break;
        }
    }

    viewTask(event: DatatableRowActivateEvent<TaskDetails>)
    {
        switch (event.columnId)
        {
            case "taskName":
                this.app.ui.navigation.go("/deployments/item", [
                    event.row.host.ri.sysId,
                    "task",
                    event.row.task.sysId
                ]);
                break;

            default:
                this.app.ui.navigation.go("/deployments/item", [
                    event.row.host.ri.sysId
                ]);
                break;
        }
    }

    async purgeOldJobs()
    {
        this.table.disableRefreshWhileProcessing(() => this.purgeOldJobsInner());
    }

    private async purgeOldJobsInner()
    {
        let jobs = this.app.domain.jobs;

        let targetJobs: JobExtended[] = [];

        await inParallel(await jobs.getExtendedAll(), async (job) =>
        {
            let usage = await job.getUsage();

            let inUse = false;

            for (let imgUsage of usage.imagesInUse || [])
            {
                if (imgUsage.safeToDelete)
                {
                    continue;
                }

                if (!inUse)
                {
                    inUse = true;

                    if (jobs.logger.isEnabled(LoggingSeverity.Debug))
                    {
                        jobs.logger.debug(`Could NOT delete job ${job.model.name}`);
                    }
                }

                let tag = imgUsage.tag;

                jobs.logger.info(`   Tagged Image ${tag} in use`);

                if (imgUsage.isRC)
                {
                    jobs.logger.info(`     Marked as Release Candidate with '${tag}'`);
                }

                if (imgUsage.isRTM)
                {
                    jobs.logger.info(`     Marked as Release with '${tag}'`);
                }

                if (jobs.logger.isEnabled(LoggingSeverity.Debug))
                {
                    for (let serviceSysId of imgUsage.services)
                    {
                        let service = imgUsage.lookupService[serviceSysId];
                        jobs.logger.debug(`     Used by service '${service.name}'`);
                    }

                    for (let backupSysId of imgUsage.backups)
                    {
                        let backup  = imgUsage.lookupBackup[backupSysId];
                        let service = imgUsage.lookupService[backup.customerService.sysId];

                        jobs.logger.debug(`     Used by backup '${backup.fileId}' for service '${service.name} - ${service.url}'`);
                    }

                    for (let taskSysId of imgUsage.tasks)
                    {
                        let task = imgUsage.lookupTask[taskSysId];
                        let host = imgUsage.lookupHost[task.deployment.sysId];

                        jobs.logger.debug(`     Used by task '${task.name}' on host '${host.hostId}'`);
                    }
                }
            }

            if (inUse) return;

            let now        = new Date().getTime();
            let lastUpdate = new Date(job.model.updatedOn).getTime();
            let inactive   = now - lastUpdate;

            // Older than N days?
            const days = 1;
            if (inactive < days * (24 * 3600 * 1000))
            {
                jobs.logger.info(`Skipping recent job ${job.model.name}: ${job.model.updatedOn}`);
                return;
            }

            jobs.logger.info(`Delete job ${job.model.name} / ${job.model.idPrefix}: ${job.model.updatedOn}`);
            targetJobs.push(job);
        });

        if (targetJobs.length > 0 && await this.confirmOperation(`Check Yes to delete ${targetJobs.length} job(s).`))
        {
            for (let targetJob of targetJobs)
            {
                await targetJob.remove();
            }
        }
    }

    public handleContextMenu(event: DatatableContextMenuEvent<JobDetail>)
    {
        let details = event.row;
        if (!details) return;

        switch (event.columnProperty)
        {
            case "inServices":
                if (details.usedInServices.values.length > 0)
                {
                    if (!details.isRC)
                    {
                        event.root.addItem("Upgrade to Release Candidate", async () =>
                        {
                            if (await this.confirmOperation("Click Yes to schedule upgrade to Release Candidate build"))
                            {
                                await this.app.domain.customerServices.upgradeBulk(await details.getServicesExt(), true, false, false);
                            }
                        });

                        event.root.addItem("Upgrade Hub to Release Candidate", async () =>
                        {
                            if (await this.confirmOperation("Click Yes to schedule upgrade Hub to Release Candidate build"))
                            {
                                await this.app.domain.customerServices.upgradeBulk(await details.getServicesExt(), true, true, false);
                            }
                        });

                        event.root.addItem("Upgrade Gateways to Release Candidate", async () =>
                        {
                            if (await this.confirmOperation("Click Yes to schedule upgrade gateways to Release Candidate build"))
                            {
                                await this.app.domain.customerServices.upgradeBulk(await details.getServicesExt(), true, false, true);
                            }
                        });
                    }

                    if (!details.isRTM)
                    {
                        event.root.addItem("Upgrade to Release", async () =>
                        {
                            if (await this.confirmOperation("Click Yes to schedule upgrade to Release build"))
                            {
                                await this.app.domain.customerServices.upgradeBulk(await details.getServicesExt(), false, false, false);
                            }
                        });

                        event.root.addItem("Upgrade Hub to Release", async () =>
                        {
                            if (await this.confirmOperation("Click Yes to schedule upgrade Hub to Release build"))
                            {
                                await this.app.domain.customerServices.upgradeBulk(await details.getServicesExt(), false, true, false);
                            }
                        });

                        event.root.addItem("Upgrade Gateways to Release", async () =>
                        {
                            if (await this.confirmOperation("Click Yes to schedule upgrade gateways to Release build"))
                            {
                                await this.app.domain.customerServices.upgradeBulk(await details.getServicesExt(), false, false, true);
                            }
                        });
                    }
                }
                break;

            case "inTasks":
                if (details.usedInHosts.values.length > 0)
                {
                    if (details.isDeployer)
                    {
                        if (!details.isRC)
                        {
                            event.root.addItem("Upgrade Agents to Release Candidate", async () =>
                            {
                                if (await this.confirmOperation("Click Yes to schedule upgrade agents to Release Candidate build"))
                                {
                                    let upgrade   = Models.DeploymentAgentUpgrade.newInstance({action: Models.DeploymentAgentUpgradeAction.StartAgentsWithReleaseCandidate});
                                    upgrade.hosts = [];

                                    for (let host of details.usedInHosts.values)
                                    {
                                        upgrade.hosts.push(DeploymentHostExtended.newIdentity(host.ri.sysId));
                                    }

                                    this.upgradeResults = await this.app.domain.deploymentHosts.startNewAgents(this, upgrade);
                                    this.upgradeOverlay.open(this.upgradeResults);
                                }
                            });
                        }

                        if (!details.isRTM)
                        {
                            event.root.addItem("Upgrade Agents to Release", async () =>
                            {
                                if (await this.confirmOperation("Click Yes to schedule upgrade agent to Release build"))
                                {
                                    let upgrade   = Models.DeploymentAgentUpgrade.newInstance({action: Models.DeploymentAgentUpgradeAction.StartAgentsWithRelease});
                                    upgrade.hosts = [];

                                    for (let host of details.usedInHosts.values)
                                    {
                                        upgrade.hosts.push(DeploymentHostExtended.newIdentity(host.ri.sysId));
                                    }

                                    this.upgradeResults = await this.app.domain.deploymentHosts.startNewAgents(this, upgrade);
                                    this.upgradeOverlay.open(this.upgradeResults);
                                }
                            });
                        }
                    }

                    if (details.isWaypoint)
                    {
                        if (!details.isRTM)
                        {
                            event.root.addItem("Upgrade Waypoint to Release", async () =>
                            {
                                if (await this.confirmOperation("Click Yes to schedule upgrade waypoint to Release build"))
                                {
                                    let imagesForRelease = await this.app.domain.registryTaggedImages.reportByReleaseStatus(Models.RegistryImageReleaseStatus.Release, Models.DeploymentRole.waypoint);

                                    for (let host of details.usedInHosts.values)
                                    {
                                        let rec_host = await this.app.domain.deploymentHosts.getExtendedById(host.ri.sysId);

                                        let arch  = rec_host.model.architecture;
                                        let target = imagesForRelease.findMatch(Models.DeploymentRole.waypoint, arch);
                                        if (target)
                                        {
                                            await rec_host.updateWaypoint(target.image);
                                        }
                                    }
                                }
                            });
                        }
                    }

                    if (details.isProvisioner)
                    {
                        if (!details.isRTM)
                        {
                            event.root.addItem("Upgrade Provisioner to Release", async () =>
                            {
                                if (await this.confirmOperation("Click Yes to schedule upgrade provisioner to Release build"))
                                {
                                    let imagesForRelease = await this.app.domain.registryTaggedImages.reportByReleaseStatus(Models.RegistryImageReleaseStatus.Release, Models.DeploymentRole.provisioner);

                                    for (let host of details.usedInHosts.values)
                                    {
                                        let rec_host = await this.app.domain.deploymentHosts.getExtendedById(host.ri.sysId);

                                        let arch  = rec_host.model.architecture;
                                        let target = imagesForRelease.findMatch(Models.DeploymentRole.provisioner, arch);
                                        if (target)
                                        {
                                            await rec_host.updateWaypoint(target.image);
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
                break;
        }
    }

    public openServicesOverlay(services: ServiceDetails[])
    {
        if (services?.length > 0)
        {
            this.selectedServices = new ServiceDetailsProvider(this, services);

            this.servicesOverlay.config = OverlayConfig.newInstance({
                                                                        showCloseButton: true,
                                                                        width          : "80%"
                                                                    });
            this.servicesOverlay.toggleOverlay();
        }
    }

    public openBackupsOverlay(backups: BackupDetails[])
    {
        if (backups?.length > 0)
        {
            this.selectedBackups = new BackupDetailsProvider(this, backups);

            this.backupsOverlay.config = OverlayConfig.newInstance({
                                                                       showCloseButton: true,
                                                                       width          : "80%"
                                                                   });
            this.backupsOverlay.toggleOverlay();
        }
    }

    public openTasksOverlay(tasks: TaskDetails[])
    {
        if (tasks?.length > 0)
        {
            this.selectedTasks = new TaskDetailsProvider(this, tasks);

            this.tasksOverlay.config = OverlayConfig.newInstance({
                                                                     showCloseButton: true,
                                                                     width          : "80%"
                                                                 });
            this.tasksOverlay.toggleOverlay();
        }
    }
}

class JobDetail
{
    constructor(private domain: AppDomainContext)
    {
    }

    extended: JobExtended;

    duration: string;

    usage: Models.JobUsage;

    isRC: boolean;
    isRTM: boolean;
    isDeployer: boolean;
    isWaypoint: boolean;
    isProvisioner: boolean;

    marked        = "<checking...>";
    markedDetails = "<checking...>";

    usedInServices = new UniqueArray<Models.CustomerService>();
    usedInBackups  = new UniqueArray<Models.CustomerServiceBackup>();
    usedInHosts    = new UniqueArray<Models.DeploymentHostStatusDescriptor>();
    usedInTasks    = new UniqueArray<Models.DeploymentTask>();

    lookupCustomer: { [key: string]: Models.Customer; }                   = {};
    lookupService: { [key: string]: Models.CustomerService; }             = {};
    lookupBackup: { [key: string]: Models.CustomerServiceBackup; }        = {};
    lookupTask: { [key: string]: Models.DeploymentTask; }                 = {};
    lookupHost: { [key: string]: Models.DeploymentHostStatusDescriptor; } = {};

    async computeInUse(comp: JobsListComponent)
    {
        this.usage = await this.extended.getUsage();

        let markedDetails = [];
        let used          = false;

        for (let imgUsage of this.usage.imagesInUse)
        {
            let tag = imgUsage.tag;

            switch (imgUsage.image?.targetService)
            {
                case Models.DeploymentRole.deployer:
                    this.isDeployer = true;
                    break;

                case Models.DeploymentRole.waypoint:
                    this.isWaypoint = true;
                    break;

                case Models.DeploymentRole.provisioner:
                    this.isProvisioner = true;
                    break;
            }

            if (imgUsage.isRC)
            {
                this.isRC = true;
                markedDetails.push(`Marked as RC for ${tag}`);
            }

            if (imgUsage.isRTM)
            {
                this.isRTM = true;
                markedDetails.push(`Marked as RTM for ${tag}`);
            }

            for (let serviceSysId of imgUsage.services)
            {
                this.usedInServices.put(serviceSysId, this.addService(imgUsage, serviceSysId));
            }

            for (let backupSysId of imgUsage.backups)
            {
                this.usedInBackups.put(backupSysId, this.addBackup(imgUsage, backupSysId));
            }

            for (let taskSysId of imgUsage.tasks)
            {
                let task = this.addTask(imgUsage, taskSysId);
                let host = this.addHost(imgUsage, task.deployment.sysId);

                this.usedInTasks.put(taskSysId, task);
                this.usedInHosts.put(taskSysId, host);
            }

            used ||= !imgUsage.safeToDelete;
        }

        this.marked        = used ? `Yes${this.isRC ? " - RC" : ""}${this.isRTM ? " - RTM" : ""}` : "No";
        this.markedDetails = markedDetails.join("\n");

        comp.detectChanges();
    }

    @Memoizer
    public getServicesExt(): Promise<CustomerServiceExtended[]>
    {
        return mapInParallel(this.usedInServices.values, (service) => this.domain.customerServices.getExtendedById(service.sysId));
    }

    @Memoizer
    public getServices(): ServiceDetails[]
    {
        let details = this.usedInServices.values.map((service) =>
                                                     {
                                                         let customer = this.lookupCustomer[service.customer.sysId];

                                                         return {
                                                             customer: customer,
                                                             service : service
                                                         };
                                                     });

        details.sort((a,
                      b) =>
                     {
                         let diff = UtilsService.compareStrings(a.customer.name, b.customer.name, true);
                         if (!diff)
                         {
                             let diff = UtilsService.compareStrings(a.service.name, b.service.name, true);
                             if (!diff)
                             {
                                 diff = b.service.createdOn.valueOf() - a.service.createdOn.valueOf();
                             }
                         }

                         return diff;
                     });

        return details;
    }

    @Memoizer
    public getBackupsExt(): Promise<CustomerServiceBackupExtended[]>
    {
        return mapInParallel(this.usedInBackups.values, (backup) => this.domain.customerServiceBackups.getExtendedById(backup.sysId));
    }

    @Memoizer
    public getBackups(): BackupDetails[]
    {
        let details = this.usedInBackups.values.map((backup) =>
                                                    {
                                                        let service  = this.lookupService[backup.customerService.sysId];
                                                        let customer = this.lookupCustomer[service.customer.sysId];

                                                        return {
                                                            customer: customer,
                                                            service : service,
                                                            backup  : backup
                                                        };
                                                    });

        details.sort((a,
                      b) => b.backup.createdOn.valueOf() - a.backup.createdOn.valueOf());

        return details;
    }

    @Memoizer
    public getTasksExt(): Promise<DeploymentTaskExtended[]>
    {
        return mapInParallel(this.usedInTasks.values, (task) => this.domain.deploymentTasks.getExtendedById(task.sysId));
    }

    @Memoizer
    public getTasks(): TaskDetails[]
    {
        let details = this.usedInTasks.values.map((task) =>
                                                  {
                                                      let host = this.lookupHost[task.deployment.sysId];

                                                      let obj  = new TaskDetails();
                                                      obj.host = host;
                                                      obj.task = task;
                                                      return obj;
                                                  });

        details.sort((a,
                      b) => b.task.createdOn.valueOf() - a.task.createdOn.valueOf());

        return details;
    }

    //--//

    private addCustomer(imgUsage: Models.RegistryTaggedImageUsage,
                        sysId: string): Models.Customer
    {
        let customerService = imgUsage.lookupCustomer[sysId];
        if (customerService)
        {
            this.lookupCustomer[sysId] = customerService;
        }

        return customerService;
    }

    private addService(imgUsage: Models.RegistryTaggedImageUsage,
                       sysId: string): Models.CustomerService
    {
        let service = imgUsage.lookupService[sysId];
        if (service)
        {
            this.lookupService[sysId] = service;

            this.addCustomer(imgUsage, service.customer.sysId);
        }

        return service;
    }

    private addBackup(imgUsage: Models.RegistryTaggedImageUsage,
                      sysId: string): Models.CustomerServiceBackup
    {
        let backup = imgUsage.lookupBackup[sysId];
        if (backup)
        {
            this.lookupBackup[sysId] = backup;

            this.addService(imgUsage, backup.customerService.sysId);
        }

        return backup;
    }

    private addHost(imgUsage: Models.RegistryTaggedImageUsage,
                    sysId: string): Models.DeploymentHostStatusDescriptor
    {
        let host = imgUsage.lookupHost[sysId];
        if (host)
        {
            this.lookupHost[sysId] = host;
        }

        return host;
    }

    private addTask(imgUsage: Models.RegistryTaggedImageUsage,
                    sysId: string): Models.DeploymentTask
    {
        let task = imgUsage.lookupTask[sysId];
        if (task)
        {
            this.lookupTask[sysId] = task;

            this.addHost(imgUsage, task.deployment.sysId);
        }

        return task;
    }
}

//--//

abstract class BaseDetailsProvider<T> implements IDatatableDataProvider<T, T, T>
{
    private m_filter: string;
    private m_filterParts: string[];

    get filter(): string
    {
        return this.m_filter;
    }

    set filter(value: string)
    {
        this.m_filter      = value;
        this.m_filterParts = value.split(" ")
                                  .map(a => a.toLowerCase());

        this.table.refreshData();
    }

    table: DatatableManager<T, T, T>;

    constructor(protected component: JobsListComponent,
                protected rows: T[],
                private prefix: string)
    {
        this.table = new DatatableManager<T, T, T>(this, () =>
        {
            let view = component.getViewState();
            return view.getSubView(prefix, true);
        });
    }

    //--//

    public wasDestroyed(): boolean
    {
        return this.component.wasDestroyed();
    }

    public detectChanges()
    {
        this.component.detectChanges();
    }

    //--//

    public getTableConfigId(): string
    {
        return null;
    }

    public async setColumnConfigs(configs: ColumnConfiguration[]): Promise<boolean>
    {
        return true;
    }

    public async getColumnConfigs(): Promise<ColumnConfiguration[]>
    {
        return null;
    }

    public getItemName(): string
    {
        return this.prefix;
    }

    public async getPage(offset: number,
                         limit: number): Promise<T[]>
    {
        return this.table.slicePage(offset, limit);
    }

    public abstract getList(): Promise<T[]>;

    public async transform(rows: T[]): Promise<T[]>
    {
        return rows;
    }

    public abstract itemClicked(columnId: string,
                                item: T): void;

    protected contains(...vals: string[])
    {
        if (this.m_filterParts == null)
        {
            return true;
        }

        for (let filterPart of this.m_filterParts)
        {
            let got = false;

            for (let val of vals)
            {
                if (val)
                {
                    val = val.toLowerCase();

                    if (val.indexOf(filterPart) >= 0)
                    {
                        got = true;
                        break;
                    }
                }
            }

            if (!got)
            {
                return false;
            }
        }

        return true;
    }
}

//--//

class ServiceDetails
{
    customer: Models.Customer;
    service: Models.CustomerService;
}

class ServiceDetailsProvider extends BaseDetailsProvider<ServiceDetails>
{
    constructor(component: JobsListComponent,
                rows: ServiceDetails[])
    {
        super(component, rows, "Services");
    }

    //--//

    public async getList(): Promise<ServiceDetails[]>
    {
        let rows = this.rows.filter((serviceDetail) => this.contains(serviceDetail.customer.name, serviceDetail.service.name, serviceDetail.service.url));

        let sortBindings = this.table.sort;
        if (sortBindings?.length > 0)
        {
            let sort = sortBindings[0];

            rows.sort((valueA,
                       valueB) =>
                      {
                          let res: number;

                          switch (sort.prop)
                          {
                              case "customerName":
                                  res = UtilsService.compareStrings(valueA.customer.name, valueB.customer.name, true);
                                  break;

                              case "serviceName":
                                  res = UtilsService.compareStrings(valueA.service.name, valueB.service.name, true);
                                  break;

                              case "serviceUrl":
                                  res = UtilsService.compareStrings(valueA.service.url, valueB.service.url, true);
                                  break;
                          }

                          return sort.dir == "asc" ? res : -res;
                      });
        }

        return rows;
    }

    public itemClicked(columnId: string,
                       item: ServiceDetails)
    {
        switch (columnId)
        {
            case "customerName":
                if (item.service)
                {
                    this.component.app.ui.navigation.go("/customers/item", [
                        item.customer.sysId
                    ]);
                }
                break;

            case "serviceName":
            case "serviceUrl":
                if (item.service)
                {
                    this.component.app.ui.navigation.go("/customers/item", [
                        item.customer.sysId,
                        "service",
                        item.service.sysId
                    ]);
                }
                break;
        }
    }
}

//--//

class BackupDetails
{
    customer: Models.Customer;
    service: Models.CustomerService;
    backup: Models.CustomerServiceBackup;
}

class BackupDetailsProvider extends BaseDetailsProvider<BackupDetails>
{
    constructor(component: JobsListComponent,
                rows: BackupDetails[])
    {
        super(component, rows, "Backups");
    }

    //--//

    public async getList(): Promise<BackupDetails[]>
    {
        let rows = this.rows.filter((backupDetail) => this.contains(backupDetail.service.name, backupDetail.backup.fileId));

        let sortBindings = this.table.sort;
        if (sortBindings?.length > 0)
        {
            let sort = sortBindings[0];

            rows.sort((valueA,
                       valueB) =>
                      {
                          let res: number;

                          switch (sort.prop)
                          {
                              case "serviceName":
                                  res = UtilsService.compareStrings(valueA.service.name, valueB.service.name, true);
                                  break;

                              case "fileId":
                                  res = UtilsService.compareStrings(valueA.backup.fileId, valueB.backup.fileId, true);
                                  break;
                          }

                          return sort.dir == "asc" ? res : -res;
                      });
        }

        return rows;
    }

    public itemClicked(columnId: string,
                       item: BackupDetails)
    {
        switch (columnId)
        {
            case "serviceUrl":
            case "serviceName":
                if (item.service)
                {
                    this.component.app.ui.navigation.go("/customers/item", [
                        item.customer.sysId,
                        "service",
                        item.service.sysId
                    ]);
                }
                break;

            case "fileId":
                if (item.customer)
                {
                    this.component.app.ui.navigation.go("/customers/item", [
                        item.customer.sysId,
                        "service",
                        item.service.sysId,
                        "backup",
                        item.backup.sysId
                    ]);
                }
                break;
        }
    }
}

//--//

class TaskDetails
{
    host: Models.DeploymentHostStatusDescriptor;
    task: Models.DeploymentTask;

    get batteryVoltage(): string
    {
        return formatVoltage(this.host.batteryVoltage);
    }
}

class TaskDetailsProvider extends BaseDetailsProvider<TaskDetails>
{
    constructor(component: JobsListComponent,
                rows: TaskDetails[])
    {
        super(component, rows, "Tasks");
    }

    //--//

    public async getList(): Promise<TaskDetails[]>
    {
        let rows = this.rows.filter((taskDetail) => this.contains(taskDetail.host.hostName, taskDetail.host.hostId, taskDetail.task.name, taskDetail.host.operationalStatus));

        let sortBindings = this.table.sort;
        if (sortBindings?.length > 0)
        {
            let sort = sortBindings[0];

            rows.sort((valueA,
                       valueB) =>
                      {
                          let res: number;

                          switch (sort.prop)
                          {
                              case "hostName":
                                  res = UtilsService.compareStrings(valueA.host.hostName, valueB.host.hostName, true);
                                  break;

                              case "hostId":
                                  res = UtilsService.compareStrings(valueA.host.hostId, valueB.host.hostId, true);
                                  break;

                              case "heartbeat":
                                  res = MomentHelper.compareDates(valueA.host.lastHeartbeat, valueB.host.lastHeartbeat);
                                  break;

                              case "operationalStatus":
                                  res = UtilsService.compareStrings(valueA.host.operationalStatus, valueB.host.operationalStatus, true);
                                  break;

                              case "taskName":
                                  res = UtilsService.compareStrings(valueA.task.name, valueB.task.name, true);
                                  break;
                          }

                          return sort.dir == "asc" ? res : -res;
                      });
        }

        return rows;
    }

    public itemClicked(columnId: string,
                       item: TaskDetails)
    {
        switch (columnId)
        {
            case "hostName":
            case "hostId":
                if (item.host)
                {
                    this.component.app.ui.navigation.go("/deployments/item", [
                        item.host.ri.sysId
                    ]);
                }
                break;

            case "taskName":
                if (item.task)
                {
                    this.component.app.ui.navigation.go("/deployments/item", [
                        item.host.ri.sysId,
                        "task",
                        item.task.sysId
                    ]);
                }
                break;
        }
    }
}

class UniqueArray<T>
{
    private m_seen: { [key: string]: T } = {};

    values: T[] = [];

    put(key: string,
        value: T)
    {
        if (key && !this.m_seen[key])
        {
            this.m_seen[key] = value;

            this.values.push(value);
        }
    }
}
