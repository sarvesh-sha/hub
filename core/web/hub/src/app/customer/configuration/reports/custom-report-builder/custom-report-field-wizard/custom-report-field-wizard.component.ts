import {Component, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import {AppContext} from "app/app.service";
import {CustomReportBuilderComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-builder.component";
import {TimeSeriesSourceHost} from "app/customer/visualization/time-series-utils";
import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import * as SharedSvc from "app/services/domain/base.service";
import {EngineeringUnitsDescriptorExtended} from "app/services/domain/units.service";
import {ControlPointsGroupExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {AlertTableComponent} from "app/shared/alerts/alert-table/alert-table.component";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {RecurringWeeklyScheduleExtended} from "app/shared/forms/time-range/range-selection-extended";

import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {ControlOption} from "framework/ui/control-option";
import {WizardComponent} from "framework/ui/wizards/wizard.component";
import {Subject} from "rxjs";

@Component({
               selector   : "o3-custom-report-field-wizard",
               templateUrl: "./custom-report-field-wizard.component.html"
           })
export class CustomReportFieldWizardComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() public model: Models.ReportLayoutItem;

    @Input() public units: ControlOption<EngineeringUnitsDescriptorExtended>[] = [];

    @Input() public range: Models.RangeSelection;

    private m_graphs: Map<string, SharedAssetGraphExtended>;
    @Input()
    public set graphs(graphs: Map<string, SharedAssetGraphExtended>)
    {
        this.m_graphs = graphs;
        if (this.data) this.data.graphs = this.m_graphs;
    }

    @Input() withPageBreak: boolean;

    @Output() public submitted = new EventEmitter<Models.ReportLayoutItem>();
    @Output() public cancelled = new EventEmitter<void>();

    data: CustomFieldData;

    @ViewChild(WizardComponent, {static: false}) wizard: WizardComponent<CustomFieldData>;

    public viewWindow: VerticalViewWindow;

    constructor(inj: Injector,
                private readonly m_host: CustomReportBuilderComponent)
    {
        super(inj);

        this.subscribeToObservable(this.m_host.graphsChanged, async () =>
        {
            this.data.graphs = await this.m_host.resolveGraphs();
            this.markForCheck();
        });
    }

    public async ngOnInit()
    {
        super.ngOnInit();


        let clone               = Models.ReportLayoutItem.deepClone(this.model);
        this.data               = new CustomFieldData(this.m_host, this.m_graphs, this.units, clone, this.range);
        this.data.withPageBreak = this.withPageBreak;

        this.data.rollupOptions = await this.data.app.bindings.getUsedLocationTypeOptions();
        this.data.rollupOptions.unshift(new ControlOption("", "None"));
    }

    wizardCancel()
    {
        this.cancelled.emit();
    }

    async wizardCommit()
    {
        this.submitted.emit(this.data.model);
    }
}

export type ReportItem =
    "CustomReportElementAggregatedValue" |
    "CustomReportElementAggregationTable" |
    "CustomReportElementAggregationTrend" |
    "CustomReportElementAlertFeed" |
    "CustomReportElementAlertTable" |
    "CustomReportElementChartSet" |
    "CustomReportElementDeviceElementList" |
    "CustomReportElementPageBreak" |
    "CustomReportElementRichText";

export class CustomFieldData
{
    readonly newItem: boolean = true;

    get elementType(): ReportItem
    {
        if (this.element instanceof Models.CustomReportElement)
        {
            return <ReportItem>this.element.__type;
        }

        return null;
    }

    set elementType(type: ReportItem)
    {
        let newField: Models.CustomReportElement;
        switch (type)
        {
            case "CustomReportElementAggregatedValue":
                newField = Models.CustomReportElementAggregatedValue.newInstance({controlPointGroup: ControlPointsGroupExtended.newModel(null)});
                break;

            case "CustomReportElementAggregationTable":
                newField = Models.CustomReportElementAggregationTable.newInstance({
                                                                                      groups                 : [ControlPointsGroupExtended.newModel(null)],
                                                                                      columns                : [],
                                                                                      visualizationMode      : Models.HierarchicalVisualizationType.TABLE,
                                                                                      controlPointDisplayType: Models.ControlPointDisplayType.NameOnly
                                                                                  });
                break;

            case "CustomReportElementAggregationTrend":
                newField = Models.CustomReportElementAggregationTrend.newInstance({
                                                                                      groups           : [ControlPointsGroupExtended.newModel(null)],
                                                                                      visualizationMode: Models.AggregationTrendVisualizationMode.Line,
                                                                                      granularity      : Models.AggregationGranularity.Day,
                                                                                      showY            : true,
                                                                                      showLegend       : true
                                                                                  });
                break;

            case "CustomReportElementAlertFeed":
                newField = Models.CustomReportElementAlertFeed.newInstance({
                                                                               label     : "",
                                                                               alertTypes: [],
                                                                               locations : []
                                                                           });
                break;


            case "CustomReportElementAlertTable":
                newField = Models.CustomReportElementAlertTable.newInstance({
                                                                                label           : "",
                                                                                locations       : [],
                                                                                alertTypeIDs    : [],
                                                                                alertRules      : [],
                                                                                alertStatusIDs  : [Models.AlertStatus.active],
                                                                                alertSeverityIDs: [],
                                                                                severityColors  : AlertTableComponent.defaultSeverityColors()
                                                                            });
                break;

            case "CustomReportElementChartSet":
                newField = Models.CustomReportElementChartSet.newInstance({charts: []});
                break;

            case "CustomReportElementDeviceElementList":
                newField = new Models.CustomReportElementDeviceElementList();
                break;

            case "CustomReportElementPageBreak":
                newField = Models.CustomReportElementPageBreak.newInstance({});
                break;

            case "CustomReportElementRichText":
                newField = Models.CustomReportElementRichText.newInstance({
                                                                              data           : [],
                                                                              backgroundColor: "#FFFFFF"
                                                                          });
                break;
        }

        this.model    = Models.ReportLayoutItem.newInstance({element: newField});
        this.element  = newField;
        this.m_groups = null;
    }

    withPageBreak: boolean = true;

    element: Models.CustomReportElement;

    get graphsHost(): GraphConfigurationHost
    {
        return this.m_comp;
    }

    graphOptions: ControlOption<string>[] = [];

    alertTypeOptions: ControlOption<string>[]   = [];
    alertRuleOptions: ControlOption<string>[]   = [];
    alertStatusOptions: ControlOption<string>[] = [];

    rollupOptions: ControlOption<string>[]                = [];
    groupByOptions: ControlOption<Models.SummaryFlavor>[] = [
        new ControlOption(Models.SummaryFlavor.rule, "Rule"),
        new ControlOption(Models.SummaryFlavor.severity, "Severity"),
        new ControlOption(Models.SummaryFlavor.status, "Status"),
        new ControlOption(Models.SummaryFlavor.type, "Type"),
        new ControlOption(Models.SummaryFlavor.location, "Location")
    ];

    timeSeriesHost: TimeSeriesSourceHost;

    app: AppContext;

    loading: boolean = false;

    private m_groups: Models.ControlPointsGroup[];

    graphsUpdated = new Subject<void>();

    get graphs(): Map<string, SharedAssetGraphExtended>
    {
        return this.m_graphs;
    }

    set graphs(graphs: Map<string, SharedAssetGraphExtended>)
    {
        this.m_graphs = graphs;
        this.updateGraphOptions();
        this.graphsUpdated.next();
    }

    constructor(private m_comp: CustomReportBuilderComponent,
                private m_graphs: Map<string, SharedAssetGraphExtended>,
                public units: ControlOption<EngineeringUnitsDescriptorExtended>[],
                public model?: Models.ReportLayoutItem,
                public range: Models.RangeSelection = Models.RangeSelection.newInstance({range: Models.TimeRangeId.Last30Days}))
    {
        this.timeSeriesHost = new TimeSeriesSourceHost(m_comp);
        this.app            = m_comp.app;

        if (this.model)
        {
            this.newItem = false;
            this.element = this.model.element;
        }
        else
        {
            this.elementType = "CustomReportElementAggregatedValue";
        }

        this.updateGraphOptions();
    }

    private updateGraphOptions()
    {
        this.graphOptions = [];
        for (let graphId of this.m_graphs?.keys() || [])
        {
            this.graphOptions.push(new ControlOption(graphId, this.m_graphs.get(graphId).name));
        }
    }

    public async ensureData()
    {
        switch (this.elementType)
        {
            case "CustomReportElementAlertFeed":
                await this.ensureAlertTypes();
                break;

            case "CustomReportElementAlertTable":
                await this.ensureAlertRules();
                await this.ensureAlertTypes();
                await this.ensureAlertStatuses();
                break;
        }
    }

    private async ensureAlertTypes()
    {
        if (!this.alertTypeOptions.length)
        {
            let alertTypeEnums    = await this.app.domain.alerts.describeTypes();
            this.alertTypeOptions = SharedSvc.BaseService.mapEnumOptions<string>(alertTypeEnums);
        }
    }

    private async ensureAlertRules()
    {
        if (!this.alertRuleOptions.length)
        {
            let rules             = await this.app.domain.alertDefinitions.getExtendedList();
            this.alertRuleOptions = rules.map((rule) => new ControlOption<string>(rule.model.sysId, rule.model.description));
        }
    }

    private async ensureAlertStatuses()
    {
        if (!this.alertStatusOptions.length)
        {
            let alertStatusEnums    = await this.app.domain.alerts.describeStates();
            this.alertStatusOptions = SharedSvc.BaseService.mapEnumOptions<string>(alertStatusEnums);
        }
    }

    public requiresAssetStructure(): boolean
    {
        switch (this.elementType)
        {
            case "CustomReportElementDeviceElementList":
                return true;

            default:
                return false;
        }
    }

    private ensureGroups()
    {
        if (!this.m_groups)
        {
            if (this.element instanceof Models.CustomReportElementAggregatedValue)
            {
                this.m_groups = [this.element.controlPointGroup];
            }
            else if (this.element instanceof Models.CustomReportElementAggregationTable || this.element instanceof Models.CustomReportElementAggregationTrend)
            {
                this.m_groups = this.element.groups;
            }
        }
    }

    get controlPointGroups(): Models.ControlPointsGroup[]
    {
        this.ensureGroups();
        return this.m_groups;
    }

    private m_timeRangeFilter: Models.RecurringWeeklySchedule;
    get timeRangeFilter(): Models.RecurringWeeklySchedule
    {
        this.ensureSchedule();
        return this.m_timeRangeFilter;
    }

    set timeRangeFilter(filter: Models.RecurringWeeklySchedule)
    {
        if (this.element instanceof Models.CustomReportElementAggregatedValue)
        {
            this.element.filter    = filter;
            this.m_timeRangeFilter = filter;
        }
    }

    private ensureSchedule()
    {
        if (!this.m_timeRangeFilter)
        {
            if (this.element instanceof Models.CustomReportElementAggregatedValue)
            {
                if (!this.element.filter)
                {
                    this.element.filter = RecurringWeeklyScheduleExtended.generateFullWeekSchedule();
                }
                this.m_timeRangeFilter = this.element.filter;
            }
        }
    }
}
