import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {formatTemperature, formatVoltage} from "app/services/domain/deployment-agents.service";
import {DeploymentHostExtended, DeploymentHostsService} from "app/services/domain/deployment-hosts.service";
import {ExportsApi} from "app/services/proxy/api/ExportsApi";
import * as Models from "app/services/proxy/model/models";
import {ExcelExporter} from "app/shared/utils/excel-exporter";
import {PersistViewState} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";

import {DatatableManager, IDatatableDataProvider, SimpleSelectionManager} from "framework/ui/datatables/datatable-manager";
import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {mapInParallel} from "framework/utils/concurrency";
import {Debouncer} from "framework/utils/debouncers";
import {Memoizer} from "framework/utils/memoizers";

@Component({
               selector   : "o3-deployment-hosts-list",
               templateUrl: "./deployment-hosts-list.component.html"
           })
export class DeploymentHostsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, DeploymentHostExtended, HostDecoded>
{
    table: DatatableManager<Models.RecordIdentity, DeploymentHostExtended, HostDecoded>;
    private tableSelectionManager: SimpleSelectionManager<Models.RecordIdentity, HostDecoded, string>;

    private m_allHostsValid: boolean                                = false;
    private m_allHostsDecoded: HostDecoded[]                        = [];
    private m_allHostsDecodedLookup: { [key: string]: HostDecoded } = {};
    private m_allHostsLastFilter: string;

    private m_notifierHosts: SharedSvc.DbChangeNotifier<Models.DeploymentHost, DeploymentHostExtended>;

    private m_invalidationDebouncer: Debouncer;

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

        if (this.m_filterStatus)
        {
            switch (this.m_filterStatus.filter)
            {
                case Models.DeploymentOperationalFilter.recentlyCreated:
                    this.table.sort = [
                        {
                            prop: "createdOn",
                            dir : "desc"
                        }
                    ];
                    break;

                case Models.DeploymentOperationalFilter.delayedOps:
                case Models.DeploymentOperationalFilter.needAttention:
                case Models.DeploymentOperationalFilter.onlyBroken:
                    this.table.sort = [
                        {
                            prop: "lastHeartbeat",
                            dir : "desc"
                        }
                    ];
                    break;

            }
        }

        this.invalidateData();
    }

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        this.persistViewStateReady(); // Needed to properly enable @PersistViewState

        if (!this.m_filterStatus)
        {
            this.m_filterStatus        = new Models.DeploymentHostFilterStatusPair();
            this.m_filterStatus.filter = Models.DeploymentOperationalFilter.needAttention;
        }

        this.table                 = this.newTableWithAutoRefresh(this.app.domain.deploymentHosts, this, 100, 1000, 100);
        this.tableSelectionManager = this.table.enableSimpleSelection((k) => k.sysId, (v) => v.desc.ri.sysId, false, false);
        this.tableSelectionManager.selectionChangeSummary.subscribe(() => this.hostsSelected());

        this.m_notifierHosts = this.listenToDatabase(this.app.domain.deploymentHosts, () => this.invalidateData(), () => this.invalidateData(), 100, 1000, 100);
        this.m_notifierHosts.subscribe(null, true, true, true);

        this.m_invalidationDebouncer = new Debouncer(500, async () =>
        {
            this.m_allHostsValid = false;
            this.table.refreshData();
        });
    }

    async ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.states = await this.app.domain.deploymentHosts.prepareFilterControlOptions();
    }

    protected shouldDelayNotifications(): boolean
    {
        return this.tableSelectionManager.selection.size > 0;
    }

    //--//

    async invalidateData()
    {
        this.m_invalidationDebouncer.invoke();
    }

    getItemName(): string
    {
        return "Deployment Hosts";
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        let filter                = new Models.DeploymentHostFilterRequest();
        filter.sortBy             = this.mapSortBindings(this.table.sort);
        filter.likeFilter         = this.m_filterText;
        filter.statusFilter       = this.m_filterStatus;
        filter.includeFullDetails = true;

        if (this.m_allHostsValid)
        {
            if (this.m_allHostsLastFilter != JSON.stringify(filter))
            {
                this.m_allHostsValid = false;
            }
        }

        if (!this.m_allHostsValid)
        {
            let hostsDecodedLookup: { [key: string]: HostDecoded } = {};
            let hostsDecoded: HostDecoded[]                        = [];

            let descriptors = await this.app.domain.deploymentHosts.getList(filter);

            for (let desc of descriptors)
            {
                let sysId = desc.ri.sysId;

                let host = new HostDecoded(this.app.domain.deploymentHosts, desc);

                hostsDecoded.push(host);
                hostsDecodedLookup[sysId] = host;
            }

            this.m_allHostsValid         = true;
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
        return mapInParallel(rows, async (row) =>
        {
            let decoded = this.m_allHostsDecodedLookup[row.model.sysId];
            await decoded.getProvisioningInfo();
            return decoded;
        });
    }

    itemClicked(columnId: string,
                item: HostDecoded)
    {
        if (columnId == "selected")
        {
            return;
        }

        this.app.ui.navigation.go("/deployments/item", [item.desc.ri.sysId]);
    }

    getTableConfigId(): string
    {
        return "DeploymentHosts";
    }

    hostsSelected()
    {
        this.detectChanges();
    }

    getAll(): Models.DeploymentHostStatusDescriptor[]
    {
        return this.m_allHostsDecoded.map((r) => r.desc);
    }

    getAllSelected(): Models.DeploymentHostStatusDescriptor[]
    {
        let res = [];

        for (let sysId of this.tableSelectionManager.selection)
        {
            let r = this.m_allHostsDecodedLookup[sysId];
            if (r) res.push(r.desc);
        }

        return res.sort((a,
                         b) => MomentHelper.compareDates(a.createdOn, b.createdOn));
    }

    changeSelection(sysId: string,
                    on: boolean)
    {
        for (let host of this.m_allHostsDecoded)
        {
            if (host.desc.ri.sysId == sysId)
            {
                this.tableSelectionManager.setChecked(host, false);
                this.hostsSelected();
            }
        }
    }

    async exportToExcel()
    {
        let exts = [];
        for (let ri of await this.getList())
        {
            exts.push(this.m_allHostsDecodedLookup[ri.sysId]);
        }

        let fileName      = DownloadDialogComponent.fileName("hosts", ".xlsx");
        const sheetName   = "Host List";
        let dataGenerator = new HostsListDownloader(this.app.domain.apis.exports, exts, fileName, sheetName);

        return DownloadDialogComponent.openWithGenerator(this, sheetName, fileName, dataGenerator);
    }
}

export class HostDecoded
{
    customerName: string;
    serviceName: string;

    constructor(public deploymentHosts: DeploymentHostsService,
                public desc: Models.DeploymentHostStatusDescriptor)
    {
        this.serviceName  = this.desc.serviceName || (this.desc.preparedForService ? `<Prepared for ${this.desc.preparedForService}>` : null);
        this.customerName = this.desc.customerName || (this.desc.preparedForCustomer ? `<Prepared for ${this.desc.preparedForCustomer}>` : null);
    }

    get diskTotal(): number
    {
        let totalDisk = this.desc.diskTotal;
        return totalDisk > 0 ? totalDisk : NaN;
    }

    get diskFree(): number
    {
        let diskFree = this.desc.diskFree;
        return diskFree > 0 ? diskFree : NaN;
    }

    get diskFreePercent(): number
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
    async getHostIdAndProvisioningInfo(): Promise<string>
    {
        let info = await this.getProvisioningInfo();

        return `${this.desc.hostId} # ${info}`;
    }

    @Memoizer
    async getProvisioningInfo(): Promise<string>
    {
        let lines = [];

        let loc = this.desc?.provisioningInfo?.manufacturingInfo?.manufacturingLocation;
        if (loc)
        {
            let host = await this.deploymentHosts.getExtendedById(loc);
            if (host)
            {
                lines.push(`Production: ${host.model.hostName}`);
            }
        }

        let notes = this.desc?.provisioningInfo?.notes;
        if (notes)
        {
            for (let note of notes)
            {
                switch ((note.customerInfo ? 1 : 0) + (note.text ? 2 : 0))
                {
                    case 1:
                        lines.push(note.customerInfo);
                        break;

                    case 2:
                        lines.push(note.text);
                        break;

                    case 3:
                        lines.push(`${note.customerInfo}: ${note.text}`);
                        break;
                }
            }
        }

        return lines.length > 0 ? lines.join("\n") : null;
    }
}

export class HostsListDownloader implements DownloadGenerator
{
    constructor(private m_exportsApi: ExportsApi,
                private m_exts: HostDecoded[],
                private m_fileName: string,
                private m_sheetName: string)
    {
    }

    public getProgressPercent(): number
    {
        return NaN;
    }

    public getProgressMessage(): string
    {
        return null;
    }

    public async makeProgress(dialog: DownloadDialogComponent): Promise<boolean>
    {
        return true;
    }

    public async sleepForProgress(): Promise<void>
    {
        // We don't need to sleep.
    }

    public isDeterminate(): boolean
    {
        return false;
    }

    public async getResults(fileName: string): Promise<DownloadResults>
    {
        let exporter = new ExcelExporter(this.m_exportsApi, this.m_sheetName);

        let dateFormatter = "m/d/yy h:mm:ss";
        exporter.addColumnHeader("Display Name");
        exporter.addColumnHeader("Host Id");
        exporter.addColumnHeader("Last Heartbeat", dateFormatter);
        exporter.addColumnHeader("Status");
        exporter.addColumnHeader("Remote Name");
        exporter.addColumnHeader("Instance Type");
        exporter.addColumnHeader("Role");
        exporter.addColumnHeader("Service");
        exporter.addColumnHeader("Customer");
        exporter.addColumnHeader("Cellular Provider");
        exporter.addColumnHeader("ICCID");
        exporter.addColumnHeader("Input Voltage");
        exporter.addColumnHeader("Disk Size");
        exporter.addColumnHeader("Disk Free");
        exporter.addColumnHeader("Disk Free %");
        exporter.addColumnHeader("Agent Built On", dateFormatter);
        exporter.addColumnHeader("Created On", dateFormatter);

        for (let ext of this.m_exts)
        {
            let row = await exporter.addRow();
            row.push(ext.desc.hostName,
                     ext.desc.hostId,
                     ext.desc.lastHeartbeat,
                     await ext.getComplexStatus(),
                     ext.desc.remoteName,
                     ext.desc.instanceType,
                     ext.desc.rolesSummary,
                     ext.serviceName,
                     ext.customerName,
                     ext.desc.hostDetails?.provider,
                     ext.desc.hostDetails?.cellular?.modemICCID,
                     ext.batteryVoltage,
                     ext.diskTotal,
                     ext.diskFree,
                     ext.diskFreePercent,
                     ext.desc.agentBuildTime,
                     ext.desc.createdOn);
        }

        return exporter.getResults(this.m_fileName);
    }
}
