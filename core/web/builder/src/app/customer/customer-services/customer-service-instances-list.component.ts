import {Component, Injector, Input, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {formatTemperature, formatVoltage} from "app/services/domain/deployment-agents.service";
import {DeploymentHostExtended, DeploymentHostsService} from "app/services/domain/deployment-hosts.service";
import * as Models from "app/services/proxy/model/models";
import {PersistViewState} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {DatatableComponent} from "framework/ui/datatables/datatable.component";
import {mapInParallel} from "framework/utils/concurrency";
import {Memoizer} from "framework/utils/memoizers";

@Component({
               selector   : "o3-customer-service-instances-list",
               templateUrl: "./customer-service-instances-list.component.html"
           })
export class CustomerServiceInstancesListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, DeploymentHostExtended, HostDecoded>
{
    @ViewChild("hostsList", {static: true}) hostsList: DatatableComponent<Models.RecordIdentity, DeploymentHostExtended, HostDecoded>;

    table: DatatableManager<Models.RecordIdentity, DeploymentHostExtended, HostDecoded>;
    private m_allHostsDecoded: HostDecoded[];
    private m_allHostsDecodedLookup: { [key: string]: HostDecoded } = {};
    private m_allHostsLastFilter: string;

    private m_extended: CustomerServiceExtended;

    public get extended(): CustomerServiceExtended
    {
        return this.m_extended;
    }

    @Input()
    public set extended(value: CustomerServiceExtended)
    {
        this.m_extended = value;

        this.invalidateData();
    }

    //--//

    @PersistViewState() private m_filterText: string = null;

    public get filterText(): string
    {
        return this.m_filterText;
    }

    public set filterText(value: string)
    {
        this.m_filterText = value;

        this.invalidateData();
    }

    //--//

    @PersistViewState() private m_filterStatus: Models.DeploymentHostFilterStatusPair;
    public states: ControlOption<string>[];

    public get filterStatus(): string
    {
        return JSON.stringify(this.m_filterStatus);
    }

    public set filterStatus(value: string)
    {
        if (value)
        {
            this.m_filterStatus = Models.DeploymentHostFilterStatusPair.newInstance(JSON.parse(value));
        }
        else
        {
            this.m_filterStatus = undefined;
        }

        this.invalidateData();
    }

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        this.persistViewStateReady(); // Needed to properly enable @PersistViewState

        this.table = new DatatableManager<Models.RecordIdentity, DeploymentHostExtended, HostDecoded>(this, () => this.getViewState());
    }

    async ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.states = await this.app.domain.deploymentHosts.prepareFilterControlOptions();
    }

    protected shouldDelayNotifications(): boolean
    {
        if (this.hostsList?.anyExpanded) return true;

        return false;
    }

    async invalidateData()
    {
        this.m_allHostsDecoded = null;
        this.table.refreshData();
    }

    getItemName(): string
    {
        return "Customer Service Instances";
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        let filter                = new Models.DeploymentHostFilterRequest();
        filter.sortBy             = this.mapSortBindings(this.table.sort);
        filter.likeFilter         = this.m_filterText;
        filter.statusFilter       = this.m_filterStatus;
        filter.includeFullDetails = true;

        if (this.extended && this.extended.hasIdentity())
        {
            filter.serviceSysid = this.extended.model.sysId;
        }

        if (this.m_allHostsDecoded)
        {
            if (this.m_allHostsLastFilter != JSON.stringify(filter))
            {
                this.m_allHostsDecoded = null;
            }
        }

        if (!this.m_allHostsDecoded)
        {
            let hostsDecoded: HostDecoded[];
            let hostsDecodedLookup: { [key: string]: HostDecoded } = {};

            if (this.extended && this.extended.hasIdentity())
            {
                let images      = await this.extended.getImages();
                let descriptors = await this.app.domain.deploymentHosts.getList(filter);

                hostsDecoded = await mapInParallel(descriptors, async (desc) =>
                {
                    let hostDecoded = new HostDecoded(this.app.domain.deploymentHosts, desc);
                    hostDecoded.ext = await this.app.domain.deploymentHosts.getExtendedById(desc.ri.sysId);

                    let activeTasks    = 0;
                    let duplicateTasks = 0;
                    let staleTasks     = 0;

                    let tasksPerRole: { [key: string]: number } = {};

                    for (let roleSvc of desc.roles)
                    {
                        let imageSpec = this.extended.findImageSpec(roleSvc, desc.architecture);
                        if (!imageSpec) continue;

                        let taggedImage = images[imageSpec.image.sysId];
                        if (!taggedImage) continue;
                        let image = await taggedImage.getImage();

                        for (let taskId in desc.tasks)
                        {
                            let task = desc.tasks[taskId];
                            if (task.status == Models.DeploymentStatus.Ready && task.purpose == roleSvc)
                            {
                                activeTasks++;

                                if (task.imageReference?.sysId == image.model.sysId)
                                {
                                    let count = tasksPerRole[roleSvc] || 0;
                                    if (count > 0)
                                    {
                                        duplicateTasks++;
                                    }

                                    tasksPerRole[roleSvc] = count + 1;
                                }
                                else
                                {
                                    staleTasks++;
                                }
                            }
                        }
                    }

                    hostDecoded.activeTasks        = activeTasks;
                    hostDecoded.activeTasksSummary = `${activeTasks}`;
                    if (duplicateTasks > 0)
                    {
                        hostDecoded.activeTasksSummary += ` - ${duplicateTasks} duplicates`;
                    }
                    if (staleTasks > 0)
                    {
                        hostDecoded.activeTasksSummary += ` - ${staleTasks} stale`;
                    }

                    hostsDecodedLookup[desc.ri.sysId] = hostDecoded;
                    return hostDecoded;
                });

                if (filter.sortBy?.length == 1 && filter.sortBy[0].column == "tasks")
                {
                    let dir = filter.sortBy[0].ascending ? 1 : -1;
                    hostsDecoded.sort((a,
                                       b) => dir * (a.activeTasks - b.activeTasks));
                }

                this.subscribeOneShot(this.extended, () => this.invalidateData());

                for (let hostDecoded of hostsDecoded)
                {
                    this.subscribeOneShot(hostDecoded.ext, () => this.invalidateData());
                }
            }
            else
            {
                hostsDecoded = [];
            }

            this.m_allHostsDecoded       = hostsDecoded;
            this.m_allHostsDecodedLookup = hostsDecodedLookup;
            this.m_allHostsLastFilter    = JSON.stringify(filter);
        }

        return this.m_allHostsDecoded.map((host) => host.desc.ri);
    }

    async getPage(offset: number,
                  limit: number): Promise<DeploymentHostExtended[]>
    {
        return this.app.domain.deploymentHosts.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: DeploymentHostExtended[]): Promise<HostDecoded[]>
    {
        return rows.map((row) => this.m_allHostsDecodedLookup[row.model.sysId]);
    }

    getTableConfigId(): string
    {
        return "DeploymentHostsInService";
    }

    itemClicked(columnId: string,
                item: HostDecoded)
    {
        this.app.ui.navigation.go("/deployments/item", [item.ext.model.sysId]);
    }
}

class HostDecoded
{
    ext: DeploymentHostExtended;
    activeTasks: number;
    activeTasksSummary: string;

    constructor(public deploymentHosts: DeploymentHostsService,
                public desc: Models.DeploymentHostStatusDescriptor)
    {
    }

    get diskSize(): number
    {
        let totalDisk = this.desc.diskTotal;
        return totalDisk > 0 ? totalDisk : NaN;
    }

    get diskFree(): number
    {
        let diskTotal = this.desc.diskTotal;
        let diskFree  = this.desc.diskFree;
        if (diskTotal > 0 && diskFree > 0)
        {
            return diskFree / diskTotal;
        }

        return NaN;
    }

    get batteryVoltage(): string
    {
        return formatVoltage(this.desc?.batteryVoltage);
    }

    get cpuTemperature(): string
    {
        return formatTemperature(this.desc?.cpuTemperature);
    }

    get cellProvider(): string
    {
        return this.desc?.hostDetails?.provider;
    }

    @Memoizer
    async getComplexStatus(): Promise<string>
    {
        if (this.desc.status != Models.DeploymentStatus.Ready)
        {
            let state = await this.deploymentHosts.describeState(this.desc.status);
            return state != null ? `Not Ready - ${state.displayName}` : "Not Ready";
        }
        else
        {
            let state = await this.deploymentHosts.describeOperationalState(this.desc.operationalStatus);
            let text  = state != null ? `Ready - ${state.displayName}` : "Ready";

            switch (this.desc.responsiveness)
            {
                case Models.DeploymentOperationalResponsiveness.Responsive:
                    return text;

                default:
                    return `${text} - no heartbeat in ${DeploymentHostExtended.computeStalenessAsText(this.desc.lastHeartbeat)}`;
            }
        }
    }

    @Memoizer
    async getDelayedOps(): Promise<string[]>
    {
        let delayedText: string[] = [];

        let delayedOps = await this.ext.describeDelayedOperations();
        if (delayedOps)
        {
            for (let op of delayedOps.ops || [])
            {
                delayedText.push(op.description);
            }
        }

        return delayedText;
    }
}
