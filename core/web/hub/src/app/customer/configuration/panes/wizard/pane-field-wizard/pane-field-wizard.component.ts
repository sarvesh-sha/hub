import {Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";
import {UUID} from "angular2-uuid";

import {AppContext} from "app/app.service";
import {TimeSeriesChartConfigurationExtended} from "app/customer/visualization/time-series-utils";
import {AssetGraphExtended, AssetGraphResponseExtended, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {EngineeringUnitsDescriptorExtended} from "app/services/domain/units.service";
import {ControlPointsGroupExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

import {ControlOption} from "framework/ui/control-option";
import {WizardComponent} from "framework/ui/wizards/wizard.component";

@Component({
               selector   : "o3-pane-field-wizard",
               templateUrl: "./pane-field-wizard.component.html"
           })
export class PaneFieldWizardComponent extends SharedSvc.BaseApplicationComponent
{
    private m_model: Models.PaneFieldConfiguration;
    @Input() set model(model: Models.PaneFieldConfiguration)
    {
        this.m_model = Models.PaneFieldConfiguration.deepClone(model);
        this.label   = "Field Settings" + this.labelSuffix;
    }

    @Input() public units: ControlOption<EngineeringUnitsDescriptorExtended>[];
    @Input() public graph: AssetGraphExtended;
    @Input() public graphContexts: Models.AssetGraphContextAsset[];
    @Input() public graphResponse: AssetGraphResponseExtended;

    @Output() public created   = new EventEmitter<Models.PaneFieldConfiguration>();
    @Output() public updated   = new EventEmitter<Models.PaneFieldConfiguration>();
    @Output() public cancelled = new EventEmitter<void>();

    label: string;
    data: CustomFieldData;

    @ViewChild(WizardComponent, {static: false}) wizard: WizardComponent<CustomFieldData>;

    private get labelSuffix(): string
    {
        if (this.m_model instanceof Models.PaneFieldConfigurationAggregatedValue) return ": Aggregation";
        if (this.m_model instanceof Models.PaneFieldConfigurationAlertCount) return ": Alert Count";
        if (this.m_model instanceof Models.PaneFieldConfigurationAlertFeed) return ": Alert Feed";
        if (this.m_model instanceof Models.PaneFieldConfigurationChart) return ": Chart";
        if (this.m_model instanceof Models.PaneFieldConfigurationCurrentValue) return ": Current Value";
        if (this.m_model instanceof Models.PaneFieldConfigurationPathMap) return ": Path Map";

        return "";
    }

    public ngOnInit(): void
    {
        super.ngOnInit();
        this.data = new CustomFieldData(this, this.graph, this.units, this.m_model, this.graphContexts, this.graphResponse);
        this.data.initialize();
    }

    wizardCancel()
    {
        this.cancelled.emit();
    }

    async wizardCommit()
    {
        if (this.data.isNew)
        {
            this.created.emit(this.data.element);
        }
        else
        {
            this.updated.emit(this.data.element);
        }
    }
}

export class CustomFieldData
{
    fieldType: string;
    unitsFactors: Models.EngineeringUnitsFactors;

    app: AppContext;

    types = [
        new ControlOption<string>("aggregatedValue", "Aggregation"),
        new ControlOption<string>("alertCount", "Alert Count"),
        new ControlOption<string>("alertFeed", "Alert Feed"),
        new ControlOption<string>("chart", "Chart"),
        new ControlOption<string>("currentValue", "Current Value"),
        new ControlOption<string>("pathMap", "Map")
    ];

    range = RangeSelectionExtended.newModel();

    loading: boolean = false;

    isNew = false;

    private m_groups: Models.ControlPointsGroup[];

    get defaultLabel(): string
    {
        switch (this.fieldType)
        {
            case "aggregatedValue":
                return "Aggregation";

            case "alertCount":
                return (<Models.PaneFieldConfigurationAlertCount>this.element).onlyActive ? "Active Alerts" : "Total Alerts";

            case "alertFeed":
                return "Alert Feed";

            case "chart":
                return "Chart";

            case "currentValue":
                return "Current Value";

            case "pathMap":
                return "GPS Path Map";
        }

        return null;
    }

    constructor(private comp: BaseApplicationComponent,
                public graph: AssetGraphExtended,
                public units: ControlOption<EngineeringUnitsDescriptorExtended>[],
                public element: Models.PaneFieldConfiguration,
                public graphContexts: Models.AssetGraphContextAsset[],
                public graphResponse: AssetGraphResponseExtended)
    {
        this.app = comp.app;

        if (!this.element)
        {
            this.fieldType = "aggregatedValue";
            this.typeChanged();
            this.isNew = true;
        }

        this.initChart();
    }

    typeChanged()
    {
        let newField: Models.PaneFieldConfiguration;
        switch (this.fieldType)
        {
            case "aggregatedValue":
                newField = Models.PaneFieldConfigurationAggregatedValue.newInstance({
                                                                                        controlPointGroup: ControlPointsGroupExtended.newModel({
                                                                                                                                                   groupAggregationType: Models.AggregationTypeId.SUM,
                                                                                                                                                   aggregationType     : Models.AggregationTypeId.MEAN
                                                                                                                                               })
                                                                                    });
                break;

            case "alertCount":
                newField = new Models.PaneFieldConfigurationAlertCount();
                break;

            case "alertFeed":
                newField = new Models.PaneFieldConfigurationAlertFeed();
                break;

            case "chart":
                let chart = Models.PaneFieldConfigurationChart.newInstance({
                                                                               config: TimeSeriesChartConfigurationExtended.newModel()
                                                                           });

                chart.config.display.size = 200;

                newField = chart;
                break;

            case "currentValue":
                newField = new Models.PaneFieldConfigurationCurrentValue();
                break;

            case "pathMap":
                newField = new Models.PaneFieldConfigurationPathMap();
                break;
        }

        newField.label = this.defaultLabel;

        this.element          = newField;
        this.m_groups         = null;
        this.m_selectedPoints = null;
        this.chartExt         = null;
    }

    private ensureGroups()
    {
        if (!this.m_groups)
        {
            if (this.element instanceof Models.PaneFieldConfigurationAggregatedValue)
            {
                this.m_groups = [this.element.controlPointGroup];
            }
            else if (this.element instanceof Models.CustomReportElementAggregationTable || this.element instanceof Models.CustomReportElementAggregationTrend)
            {
                this.m_groups = this.element.groups;
            }
        }
    }

    private m_selectedPoints: string[];

    chartExt: TimeSeriesChartConfigurationExtended;

    private initChart()
    {
        if (this.element instanceof Models.PaneFieldConfigurationChart)
        {
            let points = new Set<string>();
            for (let source of this.element.config.dataSources || [])
            {
                points.add(source.pointBinding.nodeId);
            }
            this.m_selectedPoints = [...points];
            this.updateSelectedPoints(this.m_selectedPoints);
        }
    }

    get selectedPoints(): string[]
    {
        return this.m_selectedPoints;
    }

    async updateSelectedPoints(points: string[])
    {
        if (this.element instanceof Models.PaneFieldConfigurationChart)
        {
            const graphId = this.graphContexts.find((context) => context.graphId)?.graphId || UUID.UUID();
            const rootId  = this.graph.getRootNodes()[0].id;
            for (const context of this.graphContexts)
            {
                context.graphId = graphId;
                context.nodeId  = rootId;
            }

            const graphConfig = this.element.config.graph;
            const sharedGraph = graphConfig?.sharedGraphs?.[0];
            if (sharedGraph && sharedGraph.id !== graphId)
            {
                sharedGraph.id = graphId;

                for (let context of graphConfig.contexts || [])
                {
                    context.graphId = graphId;
                }

                for (let dataSource of this.element.config.dataSources || [])
                {
                    dataSource.pointBinding.graphId = graphId;
                }
            }

            this.chartExt             = null;
            this.m_selectedPoints     = points;
            this.element.config.type  = Models.TimeSeriesChartType.GRAPH;
            this.element.config.graph = Models.TimeSeriesGraphConfiguration.newInstance({
                                                                                            sharedGraphs: [SharedAssetGraphExtended.newModel(this.graph.model, graphId, null)],
                                                                                            contexts    : this.graphContexts
                                                                                        });

            let chartExt = await TimeSeriesChartConfigurationExtended.newInstance(this.app, this.element.config);
            await chartExt.loadGraphs();

            let bindings = points.map((point) => Models.AssetGraphBinding.newInstance({
                                                                                          graphId: graphId,
                                                                                          nodeId : point
                                                                                      }));
            await chartExt.applyStandardGraphSourceChanges(bindings);

            this.chartExt = chartExt;
        }
    }

    get controlPointGroups(): Models.ControlPointsGroup[]
    {
        this.ensureGroups();
        return this.m_groups;
    }

    public async initialize()
    {
        if (this.element instanceof Models.PaneFieldConfigurationCurrentValue)
        {
            await this.updatePropSchema();
            this.m_unitsSelected = await this.app.domain.units.resolveDescriptor(this.element.unitsFactors, false);
        }
    }

    public async updatePropSchema()
    {
        let field = <Models.PaneFieldConfigurationCurrentValue>this.element;
        if (field.pointInput)
        {
            let recordId      = await this.graphResponse.resolveInputIdentity(field.pointInput);
            let deviceElem    = await this.app.domain.assets.getTypedExtendedByIdentity(DeviceElementExtended, recordId);
            let propSchema    = deviceElem instanceof DeviceElementExtended ? await deviceElem.getSchemaProperty(DeviceElementExtended.PRESENT_VALUE) : null;
            this.unitsFactors = propSchema?.unitsFactors;
        }
    }

    private m_unitsSelected: EngineeringUnitsDescriptorExtended;
    get unitsSelected(): EngineeringUnitsDescriptorExtended
    {
        return this.m_unitsSelected;
    }

    set unitsSelected(value: EngineeringUnitsDescriptorExtended)
    {
        this.m_unitsSelected = value;

        if (this.element instanceof Models.PaneFieldConfigurationCurrentValue)
        {
            this.element.unitsFactors = EngineeringUnitsDescriptorExtended.extractFactors(value);
        }
    }

}
