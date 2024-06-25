import {Component, ElementRef, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ReportError} from "app/app.service";
import {LoggersComponent} from "app/customer/maintenance/loggers/loggers.component";
import {TimeSeriesChartConfigurationExtended, TimeSeriesSourceConfigurationExtended, TimeSeriesSourceHost} from "app/customer/visualization/time-series-utils";
import {DeviceElementExtended, GatewayExtended, LocationExtended, NetworkExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {SortCriteria} from "app/services/proxy/model/models";
import {TimeSeriesChartComponent} from "app/shared/charting/time-series-chart/time-series-chart.component";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

import {UtilsService} from "framework/services/utils.service";
import {ChartRangeSelectionHandler} from "framework/ui/charting/chart.component";
import {ChartPointSource} from "framework/ui/charting/core/data-sources";
import {ControlOption} from "framework/ui/control-option";
import {NumberWithSeparatorsPipe} from "framework/ui/formatting/string-format.pipe";
import {inParallel} from "framework/utils/concurrency";
import moment from "framework/utils/moment";

const c_wellKnown_global  = "Global";
const c_wellKnown_builder = "Builder";
const c_wellKnown_hub     = "Hub";

const c_wellKnown_packetRx         = "packetRx";
const c_wellKnown_packetTx         = "packetTx";
const c_wellKnown_packetRxBytes    = "packetRxBytes";
const c_wellKnown_packetTxBytes    = "packetTxBytes";
const c_wellKnown_packetRxBytesUDP = "packetRxBytesUDP";
const c_wellKnown_packetTxBytesUDP = "packetTxBytesUDP";

const c_wellKnown_mbPacketRx            = "mbPacketRx";
const c_wellKnown_mbPacketTx            = "mbPacketTx";
const c_wellKnown_mbPacketRxBytes       = "mbPacketRxBytes";
const c_wellKnown_mbPacketTxBytes       = "mbPacketTxBytes";
const c_wellKnown_mbPacketRxBytesResent = "mbPacketRxBytesResent";
const c_wellKnown_mbPacketTxBytesResent = "mbPacketTxBytesResent";

const c_wellKnown_pendingQueueLength  = "pendingQueueLength";
const c_wellKnown_numberOfConnections = "numberOfConnections";

const c_wellKnown_entitiesUploaded        = "entitiesUploaded";
const c_wellKnown_entitiesUploadedRetries = "entitiesUploadedRetries";

@Component({
               selector   : "o3-gateways-detail-page",
               templateUrl: "./gateways-detail-page.component.html",
               styleUrls  : ["./gateways-detail-page.component.scss"]
           })
export class GatewaysDetailPageComponent extends SharedSvc.BaseComponentWithRouter implements ChartRangeSelectionHandler
{
    gatewayId: string;
    gatewayLocationId: string;
    gatewayLocationReady = false;

    gatewayExtended: GatewayExtended;
    gatewayRemoveChecks: Models.ValidationResult[];
    gatewayNoDeleteReason: string;
    location: LocationExtended;

    boundNetworks: NetworkExtended[];
    boundNetworkIds: string[];

    get queueStatus(): Models.GatewayQueueStatus
    {
        return this.gatewayExtended.typedModel.details?.queueStatus;
    }

    //--//

    @ViewChild("gatewayForm", {static: true}) gatewayForm: NgForm;

    //--//

    loggers: Array<Models.LoggerConfiguration>;
    loggersUpdating: boolean;

    @ViewChild("loggersComp") loggersComp: LoggersComponent;

    //--//

    threads: string = "<not fetched yet>";
    threadsFetching = false;

    //--//

    operationalStates: ControlOption<Models.AssetState>[];

    readonly enabledOptions: Models.TimeRangeId[] = [
        Models.TimeRangeId.Last3Hours,
        Models.TimeRangeId.Last24Hours,
        Models.TimeRangeId.Last2Days,
        Models.TimeRangeId.Last3Days,
        Models.TimeRangeId.Last7Days,
        Models.TimeRangeId.Last30Days,
        Models.TimeRangeId.Last365Days
    ];

    @ViewChild(TimeSeriesChartComponent,
               {
                   static: true,
                   read  : ElementRef
               }) elementRef: ElementRef;

    private chartInitialized: boolean = false;

    range: Models.RangeSelection                 = new Models.RangeSelection();
    config: TimeSeriesChartConfigurationExtended = TimeSeriesChartConfigurationExtended.emptyInstance(this.app);

    rangeComputing = false;
    rangeSum: string;
    rangeStart: moment.Moment;
    rangeEnd: moment.Moment;

    points: DeviceElementExtended[]  = [];
    sources: ControlOption<string>[] = [];
    sourcesVersion                   = 0;

    mainSource: string                        = "";
    mainSourcePoint: string                   = "";
    mainSourcePoints: ControlOption<string>[] = [];

    secondarySource: string                        = "";
    secondarySourcePoint: string                   = "";
    secondarySourcePoints: ControlOption<string>[] = [];

    sameSourcesSelected: boolean;

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        // Select default time range
        this.range.range = Models.TimeRangeId.Last7Days;

        this.gatewayExtended = this.app.domain.assets.wrapTypedModel(GatewayExtended, new Models.GatewayAsset());
    }

    protected onNavigationComplete()
    {
        this.gatewayId             = this.getPathParameter("id");
        this.gatewayNoDeleteReason = "<checking...>";
        this.detectChanges();
        this.loadGateway();
    }

    public ngOnDestroy(): void
    {
        super.ngOnDestroy();

        this.sourcesVersion++;
    }

    protected shouldDelayNotifications(): boolean
    {
        return !this.gatewayForm.pristine;
    }

    async loadGateway()
    {
        // load gateway info
        let gateway = await this.app.domain.assets.getTypedExtendedById(GatewayExtended, this.gatewayId);
        if (!gateway)
        {
            this.exit();
            return;
        }

        this.gatewayExtended = gateway;

        this.location = await gateway.getLocation();
        if (this.location)
        {
            this.gatewayLocationId = this.location.model.sysId;
        }
        this.gatewayLocationReady = true;

        this.gatewayRemoveChecks   = await gateway.checkRemove();
        this.gatewayNoDeleteReason = this.fromValidationToReason("Remove is disabled because:", this.gatewayRemoveChecks);

        this.boundNetworks = await gateway.getBoundNetworks();
        if (this.boundNetworks.length == 0)
        {
            this.boundNetworkIds = null;
        }
        else
        {
            this.boundNetworkIds = this.boundNetworks.map((n) => n.model.sysId);
        }

        this.operationalStates = await this.app.domain.assets.getOperationalStates();

        //--//

        let sortBy       = new SortCriteria();
        sortBy.column    = "identifier";
        sortBy.ascending = true;

        let filters = Models.DeviceElementFilterRequest.newInstance({
                                                                        parentIDs: [this.gatewayId],
                                                                        sortBy   : [sortBy]
                                                                    });

        let response = await this.app.domain.assets.getList(filters);
        this.points  = await this.app.domain.assets.getTypedExtendedBatch(DeviceElementExtended, response.results);

        //--//

        // set breadcrumbs
        let model                                     = gateway.typedModel;
        this.app.ui.navigation.breadcrumbCurrentLabel = model.name || model.instanceId;

        this.subscribeOneShot(gateway, () => this.loadGateway());

        if (this.chartInitialized)
        {
            this.computeSources();
        }

        this.gatewayForm.form.markAsPristine();
        this.detectChanges();
    }

    async initializeTrendChart()
    {
        await this.waitUntilTrue(10, () => !!this.points && !!this.elementRef?.nativeElement);

        if (!this.chartInitialized)
        {
            let rect = this.elementRef.nativeElement.getBoundingClientRect();
            if (rect.top > 0)
            {
                const padding = 20;
                this.updateConfig(window.innerHeight - rect.top - padding);
                this.chartInitialized = true;
            }

            this.computeSources();
        }
    }

    async computeSources()
    {
        let version = ++this.sourcesVersion;

        let sources1Ext: HostNetworkStats   = null;
        let sources2Ext: HostNetworkStats   = null;
        let sources3Ext: HostNetworkStats   = null;
        let sources4Ext: HostNetworkStats[] = [];

        for (let point of this.points)
        {
            switch (point.typedModel.physicalName)
            {
                case c_wellKnown_global:
                    sources1Ext = new HostNetworkStats(point, new ControlOption(point.model.sysId, "Global Traffic"));
                    break;

                case c_wellKnown_hub:
                    sources2Ext = new HostNetworkStats(point, new ControlOption(point.model.sysId, "Hub Traffic"));
                    break;

                case c_wellKnown_builder:
                    sources3Ext = new HostNetworkStats(point, new ControlOption(point.model.sysId, "Builder Traffic"));
                    break;

                default:
                    let stats = new HostNetworkStats(point, new ControlOption(point.model.sysId));
                    sources4Ext.push(stats);

                    let id = point.typedModel.identifier;

                    if (point.model.name)
                    {
                        stats.option.label = `Host ${point.model.name} [${id}]`;
                    }
                    else
                    {
                        stats.option.label = `Host ${id}`;
                    }
                    break;
            }
        }

        this.updateSources(sources1Ext, sources2Ext, sources3Ext, sources4Ext);

        //--//

        let rangeExtended = new RangeSelectionExtended(this.range);
        let min           = rangeExtended.getMin();
        let max           = rangeExtended.getMax();

        if (sources1Ext)
        {
            let sum      = 0;
            let valuesRx = await sources1Ext.point.getValues(c_wellKnown_packetRxBytes, min, max);
            for (let value of valuesRx.results.values)
            {
                sum += value;
            }

            let valuesTx = await sources1Ext.point.getValues(c_wellKnown_packetTxBytes, min, max);
            for (let value of valuesTx.results.values)
            {
                sum += value;
            }

            sources1Ext.option.label += ` (${NumberWithSeparatorsPipe.format(sum)} bytes)`;

            this.updateSources(sources1Ext, sources2Ext, sources3Ext, sources4Ext);
        }

        await inParallel([
                             sources2Ext,
                             sources3Ext,
                             ...sources4Ext
                         ], async (stats) =>
                         {
                             if (!stats || this.sourcesVersion != version)
                             {
                                 return;
                             }

                             let sum      = 0;
                             let valuesRx = await stats.point.getValues(c_wellKnown_packetRxBytes, min, max);
                             for (let value of valuesRx.results.values)
                             {
                                 sum += value;
                             }

                             let valuesTx = await stats.point.getValues(c_wellKnown_packetTxBytes, min, max);
                             for (let value of valuesTx.results.values)
                             {
                                 sum += value;
                             }

                             stats.total = sum;
                             stats.option.label += ` (${NumberWithSeparatorsPipe.format(sum)} bytes)`;

                             this.updateSources(sources1Ext, sources2Ext, sources3Ext, sources4Ext);
                         });
    }

    private updateSources(sources1Ext: HostNetworkStats,
                          sources2Ext: HostNetworkStats,
                          sources3Ext: HostNetworkStats,
                          sources4Ext: HostNetworkStats[])
    {
        sources4Ext.sort((a,
                          b) =>
                         {
                             if (a.total > b.total)
                             {
                                 return -1;
                             }

                             if (a.total < b.total)
                             {
                                 return 1;
                             }

                             return UtilsService.compareStrings(a.option.label, b.option.label, true);
                         });

        let sources: ControlOption<string>[] = [];
        this.addSourceIfNotNull(sources, sources1Ext);
        this.addSourceIfNotNull(sources, sources2Ext);
        this.addSourceIfNotNull(sources, sources3Ext);

        for (let source of sources4Ext)
        {
            this.addSourceIfNotNull(sources, source);
        }

        this.sources = sources;

        if (!this.mainSource && sources.length > 0)
        {
            this.mainSource = this.sources[0].id;

            this.updateMainSource();
        }
    }

    private addSourceIfNotNull(sources: ControlOption<string>[],
                               source: HostNetworkStats)
    {
        if (source?.option)
        {
            sources.push(source.option);
        }
    }

    async locationChanged(selectedLocationID: string)
    {
        this.gatewayExtended.setLocation(selectedLocationID);

        this.location = await this.gatewayExtended.getLocation();
        this.gatewayForm.form.markAsDirty();
    }

    //--//

    @ReportError
    async save()
    {
        this.gatewayExtended = await this.gatewayExtended.save();

        await this.cancel();
    }

    async cancel()
    {
        await this.gatewayExtended.refresh();

        await this.loadGateway();
    }

    @ReportError
    async remove()
    {
        if (this.gatewayExtended)
        {
            if (await this.confirmOperation("Click Yes to confirm deletion of this Gateway."))
            {
                let name = this.gatewayExtended.model.name || this.gatewayExtended.model.sysId;
                let msg  = this.app.framework.errors.success(`Deleting Gateway '${name}'...`, -1);

                let promise = this.gatewayExtended.remove();

                // Navigate away without waiting for deletion, since it can take a long time.
                this.exit();

                if (await promise)
                {
                    this.app.framework.errors.dismiss(msg);
                    this.app.framework.errors.success(`Gateway '${name}' deleted`, -1);
                }
            }
        }
    }

    async updateMainSource()
    {
        this.mainSourcePoints = await this.updateSource(this.mainSource);

        if (this.mainSourcePoint && !this.mainSourcePoints.find((p) => p.id == this.mainSourcePoint))
        {
            this.mainSourcePoint = undefined;
        }

        if (!this.mainSourcePoint)
        {
            let point = this.points.find((p) => p.typedModel.sysId == this.mainSource);
            if (point && point.typedModel.physicalName == c_wellKnown_global)
            {
                this.mainSourcePoint = c_wellKnown_pendingQueueLength;

                if (!this.secondarySource)
                {
                    this.secondarySourcePoint = c_wellKnown_numberOfConnections;
                }
            }
            else
            {
                this.mainSourcePoint = c_wellKnown_packetRxBytes;
            }
        }

        if (this.sameSourcesSelected || !this.secondarySource)
        {
            this.secondarySource = this.mainSource;
            this.updateSecondarySource();
        }

        this.updateMainPoint();
    }

    async updateSecondarySource()
    {
        this.sameSourcesSelected   = this.mainSource == this.secondarySource;
        this.secondarySourcePoints = await this.updateSource(this.secondarySource);

        if (this.secondarySourcePoint && !this.secondarySourcePoints.find((p) => p.id == this.secondarySourcePoint))
        {
            this.secondarySourcePoint = undefined;
        }

        this.updateConfig();
    }

    async updateSource(sysId: string): Promise<ControlOption<string>[]>
    {
        let options = [];

        let point = this.points.find((p) => p.typedModel.sysId == sysId);
        if (point)
        {
            let pointSchema = await point.fetchSchema();
            let keys        = UtilsService.extractKeysFromMap(pointSchema);

            keys.sort((a,
                       b) => UtilsService.compareStrings(a, b, true));

            for (let prop of keys)
            {
                let schema = pointSchema[prop];

                if (point.typedModel.physicalName != c_wellKnown_global)
                {
                    switch (schema.name)
                    {
                        // These are the only values that work for non-global points.
                        case c_wellKnown_packetTx:
                        case c_wellKnown_packetTxBytes:
                        case c_wellKnown_packetTxBytesUDP:
                        case c_wellKnown_packetRx:
                        case c_wellKnown_packetRxBytes:
                        case c_wellKnown_packetRxBytesUDP:
                            break;

                        default:
                            continue;
                    }
                }

                options.push(new ControlOption<string>(schema.name, schema.name));
            }
        }

        return options;
    }

    async updateMainPoint()
    {
        if (this.sameSourcesSelected)
        {
            let pairs = [
                [
                    c_wellKnown_packetRx,
                    c_wellKnown_packetTx
                ],
                [
                    c_wellKnown_packetRxBytes,
                    c_wellKnown_packetTxBytes
                ],
                [
                    c_wellKnown_packetRxBytesUDP,
                    c_wellKnown_packetTxBytesUDP
                ],
                [
                    c_wellKnown_mbPacketRx,
                    c_wellKnown_mbPacketTx
                ],
                [
                    c_wellKnown_mbPacketRxBytes,
                    c_wellKnown_mbPacketTxBytes
                ],
                [
                    c_wellKnown_mbPacketRxBytesResent,
                    c_wellKnown_mbPacketTxBytesResent
                ],
                [
                    c_wellKnown_pendingQueueLength,
                    c_wellKnown_numberOfConnections
                ],
                [
                    c_wellKnown_entitiesUploaded,
                    c_wellKnown_entitiesUploadedRetries
                ]
            ];

            for (let pair of pairs)
            {
                if (this.mainSourcePoint == pair[0])
                {
                    this.secondarySourcePoint = pair[1];
                }
                else if (this.mainSourcePoint == pair[1])
                {
                    this.secondarySourcePoint = pair[0];
                }
            }
        }

        this.updateConfig();
    }

    async updateSecondaryPoint()
    {
        this.updateConfig();
    }

    async updateConfig(chartHeight?: number)
    {
        chartHeight = Math.max(300, chartHeight ?? this.config.model.display.size);

        let host         = new TimeSeriesSourceHost(this);
        let mainExt      = await TimeSeriesSourceConfigurationExtended.resolveFromIdAndDimension(host, this.mainSource, this.mainSourcePoint);
        let secondaryExt = await TimeSeriesSourceConfigurationExtended.resolveFromIdAndDimension(host, this.secondarySource, this.secondarySourcePoint);

        this.config                    = await TimeSeriesChartConfigurationExtended.generateNewInstanceFromSources(this.app, mainExt, secondaryExt);
        this.config.model.display.size = chartHeight;
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    viewLocation(): void
    {
        if (this.location)
        {
            this.app.ui.navigation.go("/configuration/locations/location", [this.location.model.sysId]);
        }
    }

    //--//

    async flushEntities()
    {
        this.gatewayExtended.flushEntities();
    }

    async flushHeartbeat()
    {
        this.gatewayExtended.flushHeartbeat();
    }

    //--//

    @ReportError
    async loadLoggers()
    {
        try
        {
            this.loggersUpdating = true;
            this.detectChanges();

            this.loggers = await this.app.domain.apis.adminTasks.getLoggersForGateway(this.gatewayId);
        }
        finally
        {
            this.loggersUpdating = false;
        }

        this.detectChanges();
    }

    @ReportError
    async saveLoggers()
    {
        try
        {
            this.loggersUpdating = true;

            for (let logger of this.loggers)
            {
                if (this.loggersComp.wasUpdated(logger))
                {
                    await this.app.domain.apis.adminTasks.configLoggerForGateway(this.gatewayId, logger);
                }
            }
        }
        finally
        {
            this.loggersUpdating = false;
        }

        this.resetLoggers();
    }

    resetLoggers()
    {
        this.loadLoggers();
    }

    //--//

    public acceptRange(dataSource: ChartPointSource<any>,
                       start: moment.Moment,
                       end: moment.Moment): boolean
    {
        this.updateSum(dataSource, start, end);
        return true;
    }

    public clearRange()
    {
        this.rangeSum = undefined;
    }

    private async updateSum(dataSource: ChartPointSource<any>,
                            start: moment.Moment,
                            end: moment.Moment)
    {
        this.rangeStart = start;
        this.rangeEnd   = end;

        if (this.rangeComputing) return;

        while (true)
        {
            this.rangeComputing = true;

            let sum            = await dataSource.provider.computeSum(start, end);
            let rangeInSeconds = Math.max(1, (end.valueOf() - start.valueOf()) / 1000);

            this.rangeComputing = false;

            let numberFormat = new Intl.NumberFormat();
            this.rangeSum    = `${numberFormat.format(sum)} (${numberFormat.format(Math.round(sum * 86400 / rangeInSeconds))} / day)`;

            if (this.rangeStart == start && this.rangeEnd == end) break;

            // Range changed, recompute.
            start = this.rangeStart;
            end   = this.rangeEnd;
        }
    }

    //--//

    async fetchThreads()
    {
        try
        {
            this.threadsFetching = true;
            this.threads         = await this.app.domain.apis.gateways.dumpThreads(this.gatewayId, false);
            if (!this.threads)
            {
                this.threads = "<unable to fetch threads...>";
            }
        }
        catch (e)
        {
            this.threads = "<failed to fetch threads>";
        }
        finally
        {
            this.threadsFetching = false;
        }
    }

    //--//

    viewNetwork(networkId: string): void
    {
        this.app.ui.navigation.go("/networks/network", [networkId]);
    }

    showLog()
    {
        this.app.ui.navigation.go("/gateways/gateway", [
            this.gatewayId,
            "log"
        ]);
    }

    showProber()
    {
        this.app.ui.navigation.go("/gateways/gateway", [
            this.gatewayId,
            "prober"
        ]);
    }

    public goToBuilder()
    {
        window.open("https://builder.dev.optio3.io/#/deployments/item/" + this.gatewayExtended.typedModel.instanceId, "_blank");
    }
}

class HostNetworkStats
{
    total: number = 0;

    constructor(public point: DeviceElementExtended,
                public option: ControlOption<string>)
    {
    }
}
