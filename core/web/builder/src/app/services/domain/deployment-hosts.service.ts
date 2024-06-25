import {Injectable} from "@angular/core";

import {AppContext, ReportError} from "app/app.service";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {DeploymentAgentExtended} from "app/services/domain/deployment-agents.service";
import {DeploymentHostFileExtended} from "app/services/domain/deployment-host-files.service";
import {DeploymentTaskExtended} from "app/services/domain/deployment-tasks.service";
import {EnumsService} from "app/services/domain/enums.service";
import {RegistryTaggedImageExtended} from "app/services/domain/registry-tagged-images.service";
import * as Models from "app/services/proxy/model/models";
import {DeploymentHostProvisioningInfo, RecordIdentity} from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {ColumnConfiguration, DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {Memoizer, ResetMemoizers} from "framework/utils/memoizers";

@Injectable()
export class DeploymentHostsService extends SharedSvc.BaseService<Models.DeploymentHost, DeploymentHostExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService,
                private enums: EnumsService)
    {
        super(api, errors, cache, Models.DeploymentHost, DeploymentHostExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.DeploymentHost.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.DeploymentHost>
    {
        return this.api.deploymentHosts.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.DeploymentHost[]>
    {
        return this.api.deploymentHosts.getBatch(ids);
    }

    //--//

    @ReportError
    public getList(filter?: Models.DeploymentHostFilterRequest): Promise<Models.DeploymentHostStatusDescriptor[]>
    {
        return this.api.deploymentHosts.describeFiltered(filter);
    }

    public listImages(sysId: string): Promise<Models.DeploymentHostImage[]>
    {
        return this.api.deploymentHosts.listImages(sysId);
    }

    public pruneImages(sysId: string,
                       daysToKeep: number): Promise<boolean>
    {
        return this.api.deploymentHosts.pruneImages(sysId, daysToKeep);
    }

    public refreshImages(sysId: string): Promise<boolean>
    {
        return this.api.deploymentHosts.refreshImages(sysId);
    }

    //--//

    describeStates(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("DeploymentStatus", false);
    }

    async describeState(mode: Models.DeploymentStatus): Promise<Models.EnumDescriptor>
    {
        return EnumsService.find(await this.describeStates(), mode);
    }

    @Memoizer
    async getStates(): Promise<ControlOption<Models.DeploymentStatus>[]>
    {
        let types = await this.describeStates();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    describeOperationalStates(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("DeploymentOperationalStatus", false);
    }

    async describeOperationalState(mode: Models.DeploymentOperationalStatus): Promise<Models.EnumDescriptor>
    {
        return EnumsService.find(await this.describeOperationalStates(), mode);
    }

    @Memoizer
    async getOperationalStates(): Promise<ControlOption<Models.DeploymentOperationalStatus>[]>
    {
        let types = await this.describeOperationalStates();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    describeOperationalFilters(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("DeploymentOperationalFilter", false);
    }

    async describeOperationalFilter(mode: Models.DeploymentOperationalFilter): Promise<Models.EnumDescriptor>
    {
        return EnumsService.find(await this.describeOperationalFilters(), mode);
    }

    @Memoizer
    async getOperationalFilters(): Promise<ControlOption<Models.DeploymentOperationalFilter>[]>
    {
        let types = await this.describeOperationalFilters();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    describeBootConfigOptions(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("BootConfig$Options", false);
    }

    async describeBootConfigOption(mode: Models.BootConfigOptions): Promise<Models.EnumDescriptor>
    {
        return EnumsService.find(await this.describeBootConfigOptions(), mode);
    }

    @Memoizer
    async getBootConfigOptions(): Promise<ControlOption<Models.BootConfigOptions>[]>
    {
        let types = await this.describeBootConfigOptions();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    /**
     * Save the entry.
     */
    @ReportError
    async save(model: Models.DeploymentHost): Promise<Models.DeploymentHost>
    {
        if (model.sysId)
        {
            this.logger.debug(`Updating entry ${model.sysId}`);
            await this.flushModel(model);
            await this.api.deploymentHosts.update(model.sysId, undefined, model);
            return this.get(model.sysId, null);
        }

        return model;
    }

    //--//

    @Memoizer
    public async prepareFilterControlOptions(): Promise<ControlOption<string>[]>
    {
        let opFilters = await this.getOperationalFilters();
        let opStates  = await this.getOperationalStates();

        let states = [];
        for (let opFilter of opFilters)
        {
            if (opFilter.id != Models.DeploymentOperationalFilter.matchingStatus)
            {
                let statusFilter    = new Models.DeploymentHostFilterStatusPair();
                statusFilter.filter = <Models.DeploymentOperationalFilter>opFilter.id;

                let state   = new ControlOption<string>();
                state.id    = JSON.stringify(statusFilter);
                state.label = opFilter.label;

                states.push(state);
            }
        }

        for (let opState of opStates)
        {
            let statusFilter    = new Models.DeploymentHostFilterStatusPair();
            statusFilter.filter = Models.DeploymentOperationalFilter.matchingStatus;
            statusFilter.target = <Models.DeploymentOperationalStatus>opState.id;

            let state   = new ControlOption<string>();
            state.id    = JSON.stringify(statusFilter);
            state.label = `Matching State: ${opState.label}`;

            states.push(state);
        }

        return states;
    }

    async startNewAgents(comp: BaseApplicationComponent,
                         upgrade: Models.DeploymentAgentUpgrade): Promise<AgentsUpgradeSummary>
    {
        return new AgentsUpgradeSummary(comp, await this.api.deploymentHosts.upgradeAgents(upgrade));
    }

    async activateNewAgents(app: AppContext,
                            useRC: boolean,
                            upgrade: Models.DeploymentAgentUpgrade = new Models.DeploymentAgentUpgrade())
    {
        upgrade.action = useRC ? Models.DeploymentAgentUpgradeAction.ActivateAgentsWithReleaseCandidate : Models.DeploymentAgentUpgradeAction.ActivateAgentsWithRelease;

        let activated = 0;

        let results = await this.api.deploymentHosts.upgradeAgents(upgrade);
        for (let host of results)
        {
            activated += host.processed;

            app.logger.info(`${host.displayName} : ${host.processed} : ${host.positiveReasonForSkipping} ${host.negativeReasonForSkipping}`);
        }

        if (activated > 0)
        {
            app.framework.errors.success(`Activated ${activated} agents.`, -1);
        }
        else
        {
            app.framework.errors.success(`No agents to activate.`, -1);
        }
    }

    async stopOldAgents(app: AppContext,
                        upgrade: Models.DeploymentAgentUpgrade = new Models.DeploymentAgentUpgrade())
    {
        upgrade.action = Models.DeploymentAgentUpgradeAction.TerminateNonActiveAgents;

        let stopped = 0;

        let results = await this.api.deploymentHosts.upgradeAgents(upgrade);
        for (let host of results)
        {
            stopped += host.processed;

            app.logger.info(`${host.displayName} : ${host.processed} : ${host.positiveReasonForSkipping} ${host.negativeReasonForSkipping}`);
        }

        if (stopped > 0)
        {
            app.framework.errors.success(`Stopping ${stopped} old agents....`, -1);
        }
        else
        {
            app.framework.errors.success(`No old agents to stop.`, -1);
        }
    }

    async removeOldAgents(app: AppContext,
                          upgrade: Models.DeploymentAgentUpgrade = new Models.DeploymentAgentUpgrade())
    {
        upgrade.action = Models.DeploymentAgentUpgradeAction.DeleteTerminatedAgents;
        let removed    = 0;

        let results = await this.api.deploymentHosts.upgradeAgents(upgrade);
        for (let host of results)
        {
            removed += host.processed;

            app.logger.info(`${host.displayName} : ${host.processed} : ${host.positiveReasonForSkipping} ${host.negativeReasonForSkipping}`);
        }

        if (removed > 0)
        {
            app.framework.errors.success(`Deleted ${removed} terminated agents.`, -1);
        }
        else
        {
            app.framework.errors.success(`No terminated agents to delete.`, -1);
        }
    }

    private static joinIfShort(lst: string[],
                               max: number): string
    {
        let text = lst.join(", ");
        if (text.length > max)
        {
            text = text.substr(0, max) + "...";
        }

        return text;
    }
}

export class DeploymentHostExtended extends SharedSvc.ExtendedModel<Models.DeploymentHost>
{
    static newInstance(svc: DeploymentHostsService,
                       model: Models.DeploymentHost): DeploymentHostExtended
    {
        return new DeploymentHostExtended(svc, model, Models.DeploymentHost.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.DeploymentHost.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    async getCustomerService(): Promise<CustomerServiceExtended>
    {
        return this.domain.customerServices.getExtendedByIdentity(this.model.customerService);
    }

    @Memoizer
    public async getAgents(): Promise<DeploymentAgentExtended[]>
    {
        if (!this.model || !this.model.sysId) return [];

        let agents = await this.domain.apis.deploymentHosts.getAgents(this.model.sysId);
        return this.domain.deploymentAgents.getExtendedBatch(agents);
    }

    public async getImagePulls(filters: Models.DeploymentHostImagePullFilterRequest = new Models.DeploymentHostImagePullFilterRequest()): Promise<RecordIdentity[]>
    {
        filters.hostSysId = this.model?.sysId;

        return this.domain.apis.deploymentHostImagePulls.getFiltered(filters);
    }

    public async getFiles(): Promise<DeploymentHostFileExtended[]>
    {
        if (!this.model || !this.model.sysId) return [];

        let files = await this.domain.apis.deploymentHosts.getFiles(this.model.sysId);
        return this.domain.deploymentHostFiles.getExtendedBatch(files);
    }

    @Memoizer
    public async getImages(): Promise<Models.DeploymentHostImage[]>
    {
        if (!this.model || !this.model.sysId) return [];

        return this.domain.deploymentHosts.listImages(this.model.sysId);
    }

    public async pruneImages(daysToKeep: number): Promise<boolean>
    {
        if (!this.model?.sysId) return false;

        return this.domain.deploymentHosts.pruneImages(this.model.sysId, daysToKeep);
    }

    public async refreshImages(): Promise<boolean>
    {
        if (!this.model || !this.model.sysId) return false;

        return this.domain.deploymentHosts.refreshImages(this.model.sysId);
    }

    @Memoizer
    public async getTasks(): Promise<DeploymentTaskExtended[]>
    {
        if (!this.model || !this.model.sysId) return [];

        let tasks = await this.domain.apis.deploymentHosts.getTasks(this.model.sysId);
        return this.domain.deploymentTasks.getExtendedBatch(tasks);
    }

    @Memoizer
    public async getProvisioningInfo(): Promise<Models.DeploymentHostProvisioningInfo>
    {
        if (!this.model || !this.model.sysId) return null;

        return await this.domain.apis.deploymentHosts.getProvisioningInfo(this.model.sysId);
    }

    public async getManufacturingLocation(): Promise<DeploymentHostExtended>
    {
        let provisioning = await this.getProvisioningInfo();
        if (provisioning)
        {
            let manufacturingInfo = provisioning.manufacturingInfo;
            if (manufacturingInfo)
            {
                try
                {
                    return await this.domain.deploymentHosts.getExtendedById(manufacturingInfo.manufacturingLocation);
                }
                catch (e)
                {
                    // Ignore failure
                }
            }
        }

        return null;
    }

    //--//

    filterLog(filters: Models.LogEntryFilterRequest): Promise<Models.LogRange[]>
    {
        return this.domain.apis.deploymentHosts.filterLog(this.model.sysId, filters);
    }

    getLog(fromOffset?: number,
           toOffset?: number,
           limit?: number): Promise<Models.LogLine[]>
    {
        return this.domain.apis.deploymentHosts.getLog(this.model.sysId, fromOffset, toOffset, limit);
    }

    deleteLog(olderThan?: number): Promise<number>
    {
        return this.domain.apis.deploymentHosts.deleteLog(this.model.sysId, olderThan);
    }

    //--//

    @ReportError
    async updateAlertThreshold(role: Models.DeploymentRole,
                               warningThreshold: number,
                               alertThreshold: number)
    {
        await this.domain.apis.deploymentHosts.setAlertThresholds(this.model.sysId, role, warningThreshold, alertThreshold);
    }

    @ReportError
    async updateBatteryThreshold(cfg: Models.DeployerShutdownConfiguration)
    {
        await this.domain.apis.deploymentHosts.setBatteryThresholds(this.model.sysId, cfg);
    }

    //--//

    @ResetMemoizers
    public async addProvisioningNote(customerInfo: string,
                                     text: string): Promise<boolean>
    {
        if (!this.model || !this.model.sysId) return null;

        let notes          = new Models.DeploymentHostProvisioningNotes();
        notes.customerInfo = customerInfo;
        notes.text         = text;

        return await this.domain.apis.deploymentHostProvisioning.addNotes(this.model.sysId, notes);
    }

    @ResetMemoizers
    public async removeProvisioningNote(noteSysId: string): Promise<DeploymentHostProvisioningInfo>
    {
        if (!this.model || !this.model.sysId) return null;

        return await this.domain.apis.deploymentHosts.removeProvisioningInfo(this.model.sysId, noteSysId);
    }

    @Memoizer
    public async getRemoteInfo(): Promise<Models.DeploymentHostServiceDetails>
    {
        if (!this.model || !this.model.sysId) return null;

        let info = await this.domain.apis.deploymentHosts.getRemote(this.model.sysId);
        return info;
    }

    //--//

    async save(): Promise<DeploymentHostExtended>
    {
        this.model = await this.domain.deploymentHosts.save(this.model);

        return this;
    }

    //--//

    get displayName(): string
    {
        let res = this.model.hostName || this.model.hostId || "<unknown>";

        if (this.model.remoteName) res += ` [${this.model.remoteName}]`;

        return res;
    }

    getStaleness(): number
    {
        return DeploymentHostExtended.computeStalenessFromLastHeartbeat(this.model ? this.model.lastHeartbeat : null);
    }

    static computeStalenessFromLastHeartbeat(lastHeartbeat: Date): number
    {
        if (!lastHeartbeat)
        {
            return 365 * 3600 * 1000; // Assume one year...
        }

        let now        = new Date().getTime();
        let lastUpdate = new Date(lastHeartbeat).getTime();
        return now - lastUpdate;
    }

    getStalenessAsText(): string
    {
        return DeploymentHostExtended.computeStalenessAsText(this.model ? this.model.lastHeartbeat : null);
    }

    getStalenessStatus(): Models.DeploymentOperationalResponsiveness
    {
        return DeploymentHostExtended.computeStaleness(this.getStaleness(), this.model.warningThreshold);
    }

    static computeStalenessAsText(lastHeartbeat: Date): string
    {
        if (!lastHeartbeat)
        {
            return "forever";
        }

        let now       = MomentHelper.now();
        let heartbeat = MomentHelper.parse(lastHeartbeat);

        return now.from(heartbeat, true);
    }

    static computeStaleness(inactive: number,
                            warningThreshold: number): Models.DeploymentOperationalResponsiveness
    {
        if (inactive == null)
        {
            return null;
        }

        let inactiveMinutes = inactive / (1000 * 60);

        if (inactiveMinutes >= warningThreshold * 2)
        {
            return Models.DeploymentOperationalResponsiveness.Unresponsive;
        }

        if (inactiveMinutes >= warningThreshold)
        {
            return Models.DeploymentOperationalResponsiveness.UnresponsiveFullThreshold;
        }

        if (inactiveMinutes >= Math.max(40, warningThreshold / 2))
        {
            return Models.DeploymentOperationalResponsiveness.UnresponsiveHalfThreshold;
        }

        return Models.DeploymentOperationalResponsiveness.Responsive;
    }

    @Memoizer
    async getComplexStatus(): Promise<string>
    {
        if (this.model.status != Models.DeploymentStatus.Ready)
        {
            let state = await this.domain.deploymentHosts.describeState(this.model.status);
            return state != null ? `Not Ready - ${state.displayName}` : "Not Ready";
        }
        else
        {
            let state = await this.domain.deploymentHosts.describeOperationalState(this.model.operationalStatus);
            let text  = state != null ? `Ready - ${state.displayName}` : "Ready";

            switch (this.getStalenessStatus())
            {
                case Models.DeploymentOperationalResponsiveness.Responsive:
                    return text;

                default:
                    return `${text} - no heartbeat in ${this.getStalenessAsText()}`;
            }
        }
    }

    @Memoizer
    public async isBound(): Promise<boolean>
    {
        let roles = this.model.roles;
        return roles && roles.length > 0;
    }

    @Memoizer
    async roleInfo(): Promise<string>
    {
        let roleInfos = [];

        for (let role of this.model.roles)
        {
            roleInfos.push(role.toString());
        }

        roleInfos.sort((a,
                        b) => UtilsService.compareStrings(a, b, true));

        return roleInfos.length > 0 ? roleInfos.join(", ") : "<none>";
    }

    @Memoizer
    async serviceInfo(): Promise<string>
    {
        let svc = await this.getCustomerService();
        if (svc)
        {
            return svc.model.name;
        }

        return "<none>";
    }

    @Memoizer
    async customerInfo(): Promise<string>
    {
        let svc = await this.getCustomerService();
        if (svc)
        {
            let cust = await svc.getOwningCustomer();
            if (cust) return cust.model.name;
        }

        return "<none>";
    }

    @Memoizer
    async boundInfo(): Promise<string>
    {
        let svc = await this.getCustomerService();
        if (svc)
        {
            let cust = await svc.getOwningCustomer();
            if (cust)
            {
                let roleInfos = await this.roleInfo();

                return `'${svc.model.name}' of customer '${cust.model.name}' as '${roleInfos}'`;
            }
        }

        return "<not bound>";
    }

    @Memoizer
    async boundTarget(): Promise<string>
    {
        let svc = await this.getCustomerService();
        return svc ? svc.model.url : null;
    }

    @Memoizer
    async activeTasks(): Promise<number>
    {
        let res = 0;

        let svc = await this.getCustomerService();
        if (svc)
        {
            for (let role of this.model.roles)
            {
                let tag = await svc.getImage(role, this.model.architecture);
                if (tag)
                {
                    for (let task of await this.getTasks())
                    {
                        if (task.model.status == Models.DeploymentStatus.Ready && await task.sameImage(tag))
                        {
                            res++;
                        }
                    }
                }
            }
        }

        return res;
    }

    //--//

    @Memoizer
    public describeDelayedOperations(): Promise<Models.DelayedOperations>
    {
        return this.domain.apis.deploymentHosts.getDelayedOps(this.model.sysId);
    }

    @ResetMemoizers
    public cancelDelayedOperation(op: Models.DelayedOperation): Promise<Models.DelayedOperations>
    {
        return this.domain.apis.deploymentHosts.removeDelayedOp(this.model.sysId, op);
    }

    getCharges(refresh?: boolean): Promise<Models.DeploymentCellularChargesSummary>
    {
        return this.domain.apis.deploymentHosts.getCharges(this.model.sysId, refresh);
    }

    getDataConnectionStatus(): Promise<Models.DeploymentCellularStatus>
    {
        return this.domain.apis.deploymentHosts.getDataConnectionStatus(this.model.sysId);
    }

    getDataSessions(): Promise<Models.DeploymentCellularSession[]>
    {
        return this.domain.apis.deploymentHosts.getDataSessions(this.model.sysId);
    }

    getDataExchanges(days: number): Promise<Models.DeploymentCellularCommunications>
    {
        return this.domain.apis.deploymentHosts.getDataExchanges(this.model.sysId, days);
    }

    //--//

    startAgent(image: RegistryTaggedImageExtended): Promise<boolean>
    {
        return this.domain.apis.deploymentHosts.startAgent(this.model.sysId, image.model.sysId);
    }

    async startTask(image: RegistryTaggedImageExtended,
                    cfg?: Models.DeploymentTaskConfiguration): Promise<DeploymentHostExtended>
    {
        let host = await this.domain.apis.deploymentHosts.startTask(this.model.sysId, image.model.sysId, cfg);
        return this.domain.deploymentHosts.wrapModel(host);
    }

    async updateWaypoint(image: RegistryTaggedImageExtended): Promise<boolean>
    {
        return this.domain.apis.deploymentHosts.updateWaypoint(this.model.sysId, image.model.sysId);
    }

    //--//

    async checkTerminate(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.deploymentHosts.terminate(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async terminate(): Promise<Models.ValidationResults>
    {
        return await this.domain.apis.deploymentHosts.terminate(this.model.sysId);
    }

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.deploymentHosts.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.deploymentHosts.remove(this.model.sysId);
    }

    //--//

    async bindToService(customerSvc: CustomerServiceExtended,
                        role: Models.DeploymentRole): Promise<boolean>
    {
        this.flush();
        return await this.domain.apis.deploymentHosts.bindToService(this.model.sysId, customerSvc.model.sysId, role.toString());
    }

    async unbindFromService(role: Models.DeploymentRole): Promise<boolean>
    {
        this.flush();
        return await this.domain.apis.deploymentHosts.unbindFromService(this.model.sysId, role.toString());
    }

    async prepareForOfflineDeployment(): Promise<Models.DeploymentHostOffline[]>
    {
        this.flush();
        return await this.domain.apis.deploymentHosts.prepareForOfflineDeployment(this.model.sysId);
    }

    //--//

    async addFile(path: string,
                  task: DeploymentTaskExtended): Promise<DeploymentHostFileExtended>
    {
        let req  = new Models.DeploymentHostFile();
        req.path = path;

        if (task)
        {
            req.task = task.getIdentity();
        }

        let model = await this.domain.apis.deploymentHostFiles.create(this.model.sysId, req);
        return this.domain.deploymentHostFiles.wrapModel(model);
    }

    notifyMe(): Promise<boolean>
    {
        return this.domain.apis.deploymentHosts.notifyMe(this.model.sysId);
    }

    @Memoizer getLogRpc(): Promise<boolean>
    {
        return this.domain.apis.deploymentHosts.logRpc(this.model.sysId);
    }

    @ResetMemoizers setLogRpc(state: boolean): Promise<boolean>
    {
        return this.domain.apis.deploymentHosts.logRpc(this.model.sysId, state);
    }

    //--//

    getBootOptions(): Promise<Models.BootOptions>
    {
        return this.domain.apis.deploymentHosts.getBootOptions(this.model.sysId);
    }

    fetchBootOptions(): Promise<Models.BootOptions>
    {
        return this.domain.apis.deploymentHosts.fetchBootOptions(this.model.sysId);
    }

    setBootOption(key: Models.BootConfigOptions,
                  keyRaw: string,
                  value: string): Promise<Models.BootOptions>
    {
        let req    = new Models.BootConfigOptionAndValue();
        req.key    = key;
        req.keyRaw = keyRaw;
        req.value  = value;

        return this.domain.apis.deploymentHosts.setBootOption(this.model.sysId, req);
    }
}

export type DeploymentHostChangeSubscription = SharedSvc.DbChangeSubscription<Models.DeploymentHost>;

export class AgentsUpgradeSummary
{
    startedCount = 0;

    readonly started: AgentsUpgradeSummaryPerReason;
    readonly notStartedNeg: AgentsUpgradeSummaryPerReason[];
    readonly notStartedPos: AgentsUpgradeSummaryPerReason[];

    constructor(private comp: BaseApplicationComponent,
                results: Models.DeploymentAgentUpgradeDescriptor[])
    {
        this.started = new AgentsUpgradeSummaryPerReason(comp, "New Agents");

        let notStartedNeg: { [key: string]: AgentsUpgradeSummaryPerReason } = {};
        let notStartedPos: { [key: string]: AgentsUpgradeSummaryPerReason } = {};

        for (let host of results)
        {
            comp.app.logger.debug(`${host.displayName} : ${host.processed} : ${host.positiveReasonForSkipping} ${host.negativeReasonForSkipping}`);

            if (host.processed)
            {
                this.started.entries.push(host);
                this.startedCount += host.processed;
            }
            else
            {
                if (host.positiveReasonForSkipping)
                {
                    this.updateScoreboard(notStartedPos, host.positiveReasonForSkipping, host);
                }

                if (host.negativeReasonForSkipping)
                {
                    this.updateScoreboard(notStartedNeg, host.negativeReasonForSkipping, host);
                }
            }
        }

        this.started.sort();
        this.notStartedNeg = this.convert(notStartedNeg);
        this.notStartedPos = this.convert(notStartedPos);

        if (this.startedCount == 0)
        {
            comp.app.framework.errors.success("All hosts are already running the selected builds.", -1);
        }
        else
        {
            comp.app.framework.errors.success(`Starting ${this.startedCount} new agent(s)...`, -1);
        }
    }

    private updateScoreboard(lookup: { [key: string]: AgentsUpgradeSummaryPerReason },
                             key: string,
                             value: Models.DeploymentAgentUpgradeDescriptor)
    {
        let obj = lookup[key];
        if (!obj)
        {
            obj         = new AgentsUpgradeSummaryPerReason(this.comp, key);
            lookup[key] = obj;
        }

        obj.entries.push(value);
    }

    private convert(lst: { [p: string]: AgentsUpgradeSummaryPerReason }): AgentsUpgradeSummaryPerReason[]
    {
        let res: AgentsUpgradeSummaryPerReason[] = [];

        for (let key in lst)
        {
            let obj = lst[key];
            obj.sort();
            res.push(obj);
        }

        return res;
    }
}

class AgentsUpgradeSummaryPerReason implements IDatatableDataProvider<Models.DeploymentAgentUpgradeDescriptor, Models.DeploymentAgentUpgradeDescriptor, Models.DeploymentAgentUpgradeDescriptor>
{
    entries: Models.DeploymentAgentUpgradeDescriptor[] = [];

    table: DatatableManager<Models.DeploymentAgentUpgradeDescriptor, Models.DeploymentAgentUpgradeDescriptor, Models.DeploymentAgentUpgradeDescriptor>;
    private m_tableBound = false;

    constructor(private component: BaseApplicationComponent,
                public title: string)
    {
        this.table = new DatatableManager<Models.DeploymentAgentUpgradeDescriptor, Models.DeploymentAgentUpgradeDescriptor, Models.DeploymentAgentUpgradeDescriptor>(this, () => null);
    }

    bindTable()
    {
        if (!this.m_tableBound)
        {
            this.m_tableBound = true;

            this.table.refreshData();
        }
    }

    sort()
    {
        this.entries.sort((a,
                           b) => UtilsService.compareStrings(a.displayName, b.displayName, true));
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
        return "Upgrades";
    }

    public async getList(): Promise<Models.DeploymentAgentUpgradeDescriptor[]>
    {
        let rows = this.entries;

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
                              case "displayName":
                                  res = UtilsService.compareStrings(valueA.displayName, valueB.displayName, true);
                                  break;

                              case "lastHeartbeat":
                                  res = MomentHelper.compareDates(valueA.lastHeartbeat, valueB.lastHeartbeat);
                                  break;
                          }

                          return sort.dir == "asc" ? res : -res;
                      });
        }

        return rows;
    }

    public async getPage(offset: number,
                         limit: number): Promise<Models.DeploymentAgentUpgradeDescriptor[]>
    {
        return this.table.slicePage(offset, limit);
    }

    public itemClicked(columnId: string,
                       item: Models.DeploymentAgentUpgradeDescriptor)
    {
        this.component.app.ui.navigation.go("/deployments/item", [item.host.sysId]);
    }

    public async transform(rows: Models.DeploymentAgentUpgradeDescriptor[]): Promise<Models.DeploymentAgentUpgradeDescriptor[]>
    {
        return rows;
    }
}
