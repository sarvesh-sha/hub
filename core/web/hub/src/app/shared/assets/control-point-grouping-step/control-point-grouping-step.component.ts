import {CdkDragDrop, moveItemInArray} from "@angular/cdk/drag-drop";
import {Component, ElementRef, EventEmitter, Input, Output, QueryList, ViewChild, ViewChildren} from "@angular/core";

import {AggregationHelper} from "app/services/domain/aggregation.helper";
import {AssetGraphExtended, AssetGraphTreeNode, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {UnitsService} from "app/services/domain/units.service";
import {ControlPointsGroupExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {ControlPointGroupType, ControlPointsGroupConfigurerComponent} from "app/shared/assets/control-point-grouping-step/control-points-group-configurer.component";
import {DataSourceWizardDialogComponent, DataSourceWizardPurpose} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {SelectComponent} from "framework/ui/forms/select.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {inParallel, mapInParallel} from "framework/utils/concurrency";
import {Memoizer} from "framework/utils/memoizers";

@Component({
               selector   : "o3-control-point-grouping-step",
               templateUrl: "./control-point-grouping-step.component.html",
               styleUrls  : ["./control-point-grouping-step.component.scss"]
           })
export class ControlPointGroupingStepComponent extends BaseApplicationComponent
{
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_isAggregationSummary: boolean | "";
    static ngAcceptInputType_isAggregationTable: boolean | "";
    static ngAcceptInputType_isAggregationTrend: boolean | "";
    static ngAcceptInputType_forDashboard: boolean | "";
    static ngAcceptInputType_forPane: boolean | "";
    static ngAcceptInputType_forReport: boolean | "";

    @ViewChild(ControlPointsGroupConfigurerComponent, {static: true}) groupConfigurer: ControlPointsGroupConfigurerComponent;
    @ViewChild(StandardFormOverlayComponent) graphEditorOverlay: StandardFormOverlayComponent;

    @ViewChildren("test_groupName", {read: ElementRef}) test_groupNames: QueryList<ElementRef>;
    @ViewChildren("test_configureSources", {read: ElementRef}) test_configureSourceTriggers: QueryList<ElementRef>;
    @ViewChildren("test_configureGroup", {read: ElementRef}) test_configureGroupTriggers: QueryList<ElementRef>;
    @ViewChildren("test_copyGroup", {read: ElementRef}) test_copyGroupTriggers: QueryList<ElementRef>;
    @ViewChild("test_trendType") test_trendType: SelectComponent<Models.AggregationTrendVisualizationMode>;

    sourcesUpdating = false;

    displayOptions: ControlOption<Models.ControlPointDisplayType>[];
    visualizationOptions: ControlOption<Models.HierarchicalVisualizationType>[];

    @Input() @CoerceBoolean() public forDashboard: boolean;
    @Input() @CoerceBoolean() public forReport: boolean;
    @Input() @CoerceBoolean() public forPane: boolean;
    @Input() @CoerceBoolean() public isAggregationSummary: boolean;
    @Input() @CoerceBoolean() public isAggregationTable: boolean;
    @Input() @CoerceBoolean() public isAggregationTrend: boolean;

    private get dataSourceWizardPurpose(): DataSourceWizardPurpose
    {
        if (this.forDashboard) return DataSourceWizardPurpose.dashboard;
        if (this.forPane) return DataSourceWizardPurpose.pane;
        if (this.forReport) return DataSourceWizardPurpose.report;

        return null;
    }

    //--//

    private m_configuredGroups = new Set<ControlPointsGroupExtended>();
    private m_groupToConfigure: ControlPointsGroupExtended;
    set groupToConfigure(groupExt: ControlPointsGroupExtended)
    {
        this.m_groupToConfigure = groupExt;

        if (groupExt) this.m_configuredGroups.add(groupExt);

    }

    get groupToConfigure(): ControlPointsGroupExtended
    {
        return this.m_groupToConfigure;
    }

    ControlPointsGroupExtended = ControlPointsGroupExtended;
    controlPointGroupExts: ControlPointsGroupExtended[];
    private m_controlPointGroups: Models.ControlPointsGroup[];

    @Input()
    public set controlPointGroups(value: Models.ControlPointsGroup[])
    {
        this.m_controlPointGroups = value ?? [];

        this.controlPointGroupExts = this.m_controlPointGroups.map((group) => new ControlPointsGroupExtended(this.app.domain, group));
        this.updateAllUnitsIdentical();
        this.ensureColorConfigs();
    }

    public get controlPointGroups(): Models.ControlPointsGroup[]
    {
        return this.m_controlPointGroups;
    }

    //--//

    @Input() public graph: AssetGraphExtended;
    @Input() public graphContexts: Models.AssetGraphContext[];
    @Input() public graphsHost: GraphConfigurationHost;
    public graphs: Map<string, SharedAssetGraphExtended>;

    @Input() public aggregationTrendShowYAxis: boolean;
    @Output() public aggregationTrendShowYAxisChange = new EventEmitter<boolean>();

    @Input() public aggregationTrendShowLegend: boolean;
    @Output() public aggregationTrendShowLegendChange = new EventEmitter<boolean>();

    @Input() public aggregationTrendVisualizationMode: Models.AggregationTrendVisualizationMode;
    @Output() public aggregationTrendVisualizationModeChange = new EventEmitter<Models.AggregationTrendVisualizationMode>();

    visualizationModeOptions = [
        new ControlOption(Models.AggregationTrendVisualizationMode.Bar, "Bar"),
        new ControlOption(Models.AggregationTrendVisualizationMode.Line, "Line")
    ];

    private m_visualizationMode: Models.HierarchicalVisualizationType;
    @Input()
    public set visualizationMode(mode: Models.HierarchicalVisualizationType)
    {
        this.m_visualizationMode = mode;
        this.ensureColorConfigs();
    }

    public get visualizationMode(): Models.HierarchicalVisualizationType
    {
        return this.m_visualizationMode;
    }

    @Output() public visualizationModeChange = new EventEmitter<Models.HierarchicalVisualizationType>();

    private m_compareBetweenGroups: boolean = true;
    @Input()
    public set compareBetweenGroups(compare: boolean)
    {
        this.m_compareBetweenGroups = compare;
    }

    public get compareBetweenGroups(): boolean
    {
        return this.allUnitsIdentical && this.m_compareBetweenGroups;
    }

    @Output() public compareBetweenGroupsChange = new EventEmitter<boolean>();

    allUnitsIdentical: boolean = true;

    get compareBetweenGroupsTooltip(): string
    {
        if (!this.allUnitsIdentical) return "Cannot compare between groups of different units";
        return "Only relevant to table modes";
    }

    get showCompareBetweenGroupsTooltip(): boolean
    {
        switch (this.m_visualizationMode)
        {
            case Models.HierarchicalVisualizationType.TABLE:
            case Models.HierarchicalVisualizationType.TABLE_WITH_BAR:
                return !this.allUnitsIdentical;

            default:
                return true;
        }
    }

    @Input() public visualizationLegend: boolean = false;
    @Output() public visualizationLegendChange   = new EventEmitter<boolean>();

    @Input() public selectorNameLookup: Lookup<Models.SharedAssetSelector>;

    get groupType(): ControlPointGroupType
    {
        if (this.isAggregationTable) return ControlPointGroupingStepComponent.convertGroupType(this.m_visualizationMode);
        if (this.isAggregationSummary) return ControlPointGroupType.SUMMARY;
        if (this.isAggregationTrend) return ControlPointGroupType.TREND;

        return null;
    }

    @Input() public controlPointDisplayType: Models.ControlPointDisplayType;
    @Output() public controlPointDisplayTypeChange = new EventEmitter<Models.ControlPointDisplayType>();

    @Output() public unitsUpdated = new EventEmitter<void>();

    @Input() public timeRangeFilter: Models.RecurringWeeklySchedule;
    @Output() public timeRangeFilterChange = new EventEmitter<Models.RecurringWeeklySchedule>();

    @Input() public timeRangeFilterApplied: boolean;
    @Output() public timeRangeFilterAppliedChange = new EventEmitter<boolean>();

    private m_timeRange: Models.FilterableTimeRange;
    get timeRange(): Models.FilterableTimeRange
    {
        if (!this.timeRangeFilter)
        {
            return null;
        }

        if (!this.m_timeRange)
        {
            this.m_timeRange = Models.FilterableTimeRange.newInstance({
                                                                          name           : "Aggregation Filter",
                                                                          filter         : this.timeRangeFilter,
                                                                          isFilterApplied: this.timeRangeFilterApplied
                                                                      });
        }

        return this.m_timeRange;
    }

    get singleGroupName(): string
    {
        if (this.isAggregationSummary) return "Control Point Group";
        return null;
    }

    public updateTimeRange(timeRange: Models.FilterableTimeRange)
    {
        if (timeRange.isFilterApplied !== this.timeRangeFilterApplied)
        {
            this.timeRangeFilterApplied = timeRange.isFilterApplied;
            this.timeRangeFilterAppliedChange.emit(this.timeRangeFilterApplied);
        }

        this.timeRangeFilter = timeRange.filter;
        this.timeRangeFilterChange.emit(this.timeRangeFilter);

        this.m_timeRange = timeRange;
    }

    private static convertGroupType(mode: Models.HierarchicalVisualizationType): ControlPointGroupType
    {
        switch (mode)
        {
            case Models.HierarchicalVisualizationType.BUBBLEMAP:
                return ControlPointGroupType.BUBBLEMAP;

            case Models.HierarchicalVisualizationType.DONUT:
                return ControlPointGroupType.DONUT;

            case Models.HierarchicalVisualizationType.SUNBURST:
                return ControlPointGroupType.SUNBURST;

            case Models.HierarchicalVisualizationType.PIE:
                return ControlPointGroupType.PIE;

            case Models.HierarchicalVisualizationType.PIEBURST:
                return ControlPointGroupType.PIEBURST;

            case Models.HierarchicalVisualizationType.TABLE:
                return ControlPointGroupType.TABLE;

            case Models.HierarchicalVisualizationType.TABLE_WITH_BAR:
                return ControlPointGroupType.TABLE_WITH_BAR;

            case Models.HierarchicalVisualizationType.TREEMAP:
                return ControlPointGroupType.TREEMAP;
        }

        return null;
    }

    public getAggregationMessage(groupExt: ControlPointsGroupExtended): string
    {
        let group = groupExt.model;
        return AggregationHelper.aggregationCategorizationDisplay(group.groupAggregationType, group.aggregationType, group.granularity, group.limitMode, group.limitValue);
    }

    public unconfiguredGroup(groupExt: ControlPointsGroupExtended): boolean
    {
        return !this.m_configuredGroups.has(groupExt);
    }

    public getId(binding: Models.AssetGraphBinding): string
    {
        return AssetGraphTreeNode.getIdFromBinding(binding);
    }

    public getInput(id: string): Models.AssetGraphBinding
    {
        return AssetGraphTreeNode.getBinding(id);
    }

    async ngOnInit()
    {
        this.graphs = await this.graphsHost?.resolveGraphs();

        await inParallel(this.controlPointGroupExts, (container) => this.initializeSources(container, true));

        if (!this.displayOptions)
        {
            let displayTypes    = await this.getControlPointDisplayTypes();
            this.displayOptions = displayTypes.map((descriptor) => new ControlOption(<Models.ControlPointDisplayType>descriptor.id, descriptor.displayName));
        }

        if (!this.visualizationOptions)
        {
            this.visualizationOptions = this.app.domain.widgetData.getVisualizationModeOptions(false);
        }

        if (this.isAggregationSummary)
        {
            AggregationHelper.fixupGroup(this.controlPointGroupExts[0].model);
            this.markForCheck();
        }
        else
        {
            this.controlPointGroupExts.forEach((group) =>
                                               {
                                                   if (group.model)
                                                   {
                                                       if (!group.model.groupAggregationType) group.model.groupAggregationType = Models.AggregationTypeId.MAX;
                                                       if (!group.model.aggregationType) group.model.aggregationType = Models.AggregationTypeId.MAX;
                                                   }
                                               });
        }
    }

    public async initializeGroup(groupExt: ControlPointsGroupExtended)
    {
        await this.initializeSources(groupExt);
        this.markForCheck();
    }

    private async initializeSources(groupExt: ControlPointsGroupExtended,
                                    skipUnitUpdate?: boolean)
    {
        this.sourcesUpdating = true;

        let group                                     = groupExt.model;
        let state                                     = groupExt.getDataSourceState(this.dataSourceWizardPurpose, this.graphsHost, false);
        let localGraphResults: Models.DeviceElement[] = undefined;
        if (state.hasSelection())
        {
            switch (state.type)
            {
                case Models.TimeSeriesChartType.STANDARD:
                    group.selections.identities = state.ids;
                    group.graph                 = null;
                    break;

                case Models.TimeSeriesChartType.GRAPH:
                    if (!state.locallyBound)
                    {
                        group.graph      = null;
                        group.selections = Models.ControlPointsSelection.newInstance({identities: []});
                        group.pointInput = state.graphBinding;
                        if (this.selectorNameLookup && group.pointInput && state.newSelectorName)
                        {
                            this.selectorNameLookup[group.pointInput.selectorId] = Models.SharedAssetSelector.newInstance(
                                {
                                    id     : group.pointInput.selectorId,
                                    graphId: group.pointInput.graphId,
                                    name   : state.newSelectorName
                                }
                            );
                        }

                        if (this.graphsHost)
                        {
                            this.graphs = await this.graphsHost.resolveGraphs();
                        }

                        let graph = this.graphs?.get(group.pointInput?.graphId) || this.graph;
                        if (graph)
                        {
                            await this.resolveGraphSources(group, graph);
                        }
                        break;
                    }
                    else
                    {
                        group.graph      = state.localGraph;
                        group.pointInput = Models.AssetGraphBinding.newInstance({nodeId: state.hierarchy.bindings[0]?.leafNodeId});

                        const graphExt    = new AssetGraphExtended(this.app.domain, group.graph);
                        const response    = await graphExt.resolve();
                        localGraphResults = await response.resolveControlPoints(this.app.domain, [group.pointInput]);
                    }

                    break;
            }
        }
        else if (this.graph)
        {
            group.selections = Models.ControlPointsSelection.newInstance({identities: []});
            await this.resolveGraphSources(group, this.graph);
        }

        await this.initializeUnits(groupExt, localGraphResults, !skipUnitUpdate);
        this.updateAllUnitsIdentical();

        this.sourcesUpdating = false;
    }

    private async resolveGraphSources(group: Models.ControlPointsGroup,
                                      graph: AssetGraphExtended)
    {
        let contexts                            = this.graphContexts;
        let identities: Models.RecordIdentity[] = [];
        if (this.graphsHost && !contexts)
        {
            let resolved = await graph.resolve();

            if (this.forDashboard)
            {
                // Find first response with matching asset (accounts for optional node in asset structure)
                for (let response of resolved.responses)
                {
                    identities = response.resolveInputIdentities(group.pointInput);
                    if (identities.length) break;
                }
            }
            else
            {
                identities = resolved.resolveIdentities(group.pointInput);
            }
        }
        else
        {
            let resolved = await graph.resolveWithContext(contexts);
            identities   = resolved.resolveIdentities(group.pointInput);
        }

        group.selections.identities.push(...identities);
    }

    private async initializeUnits(groupExt: ControlPointsGroupExtended,
                                  localGraphResults: Models.DeviceElement[],
                                  force: boolean)
    {
        await groupExt.initialized;

        let selections: string[];
        if (localGraphResults)
        {
            selections                = localGraphResults.map((result) => result.sysId);
            groupExt.numControlPoints = selections.length;
        }
        else
        {
            selections                = groupExt.model.selections.identities.map((id) => id?.sysId);
            groupExt.numControlPoints = null;
        }

        if (!groupExt.desc || force)
        {
            for (let identity of selections)
            {
                let element = await this.app.domain.assets.getTypedExtendedById(DeviceElementExtended, identity);
                if (element)
                {
                    await groupExt.bindToElement(element);
                    if (groupExt.desc) break;
                }
            }
        }

        if (groupExt.desc?.noDimensions && groupExt.model.unitsDisplay == undefined)
        {
            await groupExt.updateUnitsDisplay(this.app.domain.units);
        }
    }

    public async unitsChanged(groupExt: ControlPointsGroupExtended)
    {
        await groupExt.updateFactors();

        let idx                    = this.controlPointGroupExts.indexOf(this.groupToConfigure);
        this.controlPointGroupExts = await mapInParallel(this.controlPointGroupExts, (controlPointGroupExt) => controlPointGroupExt.clone());
        if (idx >= 0) this.groupToConfigure = this.controlPointGroupExts[idx];

        this.updateAllUnitsIdentical();

        this.markForCheck();
    }

    private updateAllUnitsIdentical()
    {
        if (this.isAggregationTable)
        {
            let firstUnits         = this.controlPointGroupExts[0]?.model.unitsFactors;
            this.allUnitsIdentical = this.controlPointGroupExts.every((groupExt) => UnitsService.areIdentical(firstUnits, groupExt.model.unitsFactors));
        }
    }

    public getNamePlaceholder(groupIndex: number): string
    {
        if (this.isAggregationSummary)
        {
            return "";
        }
        else
        {
            let groupContainer = this.controlPointGroupExts[groupIndex];
            return groupContainer.model.name ? "Control Point Group Name" : "Insert group's name here";
        }
    }

    public configureGroup(groupExt: ControlPointsGroupExtended)
    {
        this.groupToConfigure = groupExt;
        this.groupConfigurer.toggleOverlay();
    }

    public visualizationModeChanged(mode: Models.HierarchicalVisualizationType)
    {
        switch (mode)
        {
            case Models.HierarchicalVisualizationType.TABLE:
            case Models.HierarchicalVisualizationType.TABLE_WITH_BAR:
                break;

            default:
                for (let group of this.controlPointGroupExts)
                {
                    if (group.model.groupAggregationType === Models.AggregationTypeId.NONE)
                    {
                        group.model.groupAggregationType = Models.AggregationTypeId.SUM;
                    }
                }
                break;
        }

        this.visualizationModeChange.emit(mode);
    }

    public async configureGroupSources(groupExt: ControlPointsGroupExtended)
    {
        this.groupToConfigure = groupExt;
        this.detectChanges();

        if (this.graph)
        {
            this.graphEditorOverlay.toggleOverlay();
        }
        else
        {
            let dataSourceWizardState = this.groupToConfigure.getDataSourceState(this.dataSourceWizardPurpose, this.graphsHost, true);
            if (await DataSourceWizardDialogComponent.open(dataSourceWizardState, this))
            {
                await this.initializeSources(this.groupToConfigure);
            }
        }
    }

    private ensureColorConfigs()
    {
        let colors: string[] = [];
        for (let groupExt of this.controlPointGroupExts)
        {
            groupExt.ensureColorConfig(this.m_visualizationMode, colors);

            let newColor = groupExt.colorConfig.segments[0]?.color;
            if (this.m_visualizationMode !== Models.HierarchicalVisualizationType.TABLE && newColor) colors.push(newColor);
        }
    }

    private groupExtsChanged()
    {
        this.controlPointGroupExts = UtilsService.arrayCopy(this.controlPointGroupExts);
        this.ensureColorConfigs();

        this.detectChanges();
    }

    public removeGroup(groupIndex: number)
    {
        this.m_controlPointGroups.splice(groupIndex, 1);
        this.groupExtsChanged();
        this.updateAllUnitsIdentical();
    }

    public groupsReordered(event: CdkDragDrop<ControlPointsGroupExtended>)
    {
        moveItemInArray(this.m_controlPointGroups, event.previousIndex, event.currentIndex);

        this.groupExtsChanged();
    }

    public addGroup(newGroup?: Models.ControlPointsGroup)
    {
        if (this.isAggregationSummary) return;

        if (!newGroup) newGroup = ControlPointsGroupExtended.newModel(null);

        this.m_controlPointGroups.push(newGroup);
        this.controlPointGroupExts.push(new ControlPointsGroupExtended(this.app.domain, newGroup));

        this.groupExtsChanged();
    }

    public async addCopyGroup(groupIndex: number)
    {
        let copyGroup  = Models.ControlPointsGroup.deepClone(this.controlPointGroupExts[groupIndex].model);
        copyGroup.name = copyGroup.name && UtilsService.getUniqueTitle(copyGroup.name, this.controlPointGroupExts.map((groupExt) => groupExt.model.name)) || "";
        this.addGroup(copyGroup);
        this.initializeSources(this.controlPointGroupExts[this.controlPointGroupExts.length - 1]);
    }

    @Memoizer
    async getControlPointDisplayTypes(): Promise<Models.EnumDescriptor[]>
    {
        return await this.app.domain.enums.getInfos("ControlPointDisplayType", false);
    }
}
