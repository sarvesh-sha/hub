import {CDK_DRAG_CONFIG, CdkDragDrop, moveItemInArray, transferArrayItem} from "@angular/cdk/drag-drop";
import {Component, Injector, Input, ViewChild} from "@angular/core";
import {UUID} from "angular2-uuid";

import {CustomReportBuilderRowConfigurerComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-builder-row-configurer.component";
import {REPORT_METADATA, ReportMetadata} from "app/reports/base-report.component";
import {DynamicReport} from "app/reports/dynamic/dynamic-report";
import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {ClipboardEntryData, ClipboardService} from "app/services/domain/clipboard.service";
import {ReportDefinitionExtended} from "app/services/domain/report-definitions.service";
import {ReportConfigurationExtended} from "app/services/domain/reporting.service";
import {EngineeringUnitsDescriptorExtended} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {DataAggregationType} from "app/shared/aggregation/data-aggregation.component";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {FilterableTimeRangeExtended, RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";
import {StateHistory, StateRestorable} from "app/shared/undo/undo-redo-state";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ChartTimeRange} from "framework/ui/charting/core/time";
import {ControlOption} from "framework/ui/control-option";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {inParallel} from "framework/utils/concurrency";
import moment from "framework/utils/moment";

import {Subject} from "rxjs";

@Component({
               selector   : "o3-custom-report-builder",
               styleUrls  : ["./custom-report-builder.component.scss"],
               providers  : [
                   {
                       provide    : REPORT_METADATA,
                       useExisting: CustomReportBuilderComponent
                   },
                   {
                       provide : CDK_DRAG_CONFIG,
                       useValue: {
                           dragStartThreshold             : 2,
                           pointerDirectionChangeThreshold: 5
                       }
                   }
               ],
               templateUrl: "./custom-report-builder.component.html"
           })
export class CustomReportBuilderComponent extends BaseApplicationComponent implements ReportMetadata,
                                                                                      StateRestorable<ReportContainer>,
                                                                                      GraphConfigurationHost
{
    public readonly hostContext = "Report";

    private m_dropListModelToId         = new Map<ReportContainer, string>();
    private m_allDropListIds: string[]  = [];
    private m_rootDropListIds: string[] = [];

    public get connectedDropLists(): string[]
    {
        return this.draggingPageBreak ? this.m_rootDropListIds : this.m_allDropListIds;
    }

    @Input() public report: ReportDefinitionExtended;
    @Input() public data: ReportConfigurationExtended;

    private m_rangeId: Models.TimeRangeId = Models.TimeRangeId.Last30Days;
    @Input()
    public set rangeId(id: Models.TimeRangeId)
    {
        if (id && this.m_rangeId != id)
        {
            this.m_rangeId = id;
            this.rangeExt  = RangeSelectionExtended.fromTimeRangeId(this.m_rangeId);
            this.m_range   = null;
            if (this.data?.container) this.initContent();
        }
    }

    public get rangeStart(): moment.Moment
    {
        if (!this.m_range) this.m_range = this.rangeExt.getChartRange();
        return this.m_range.minAsMoment;
    }

    public get rangeEnd(): moment.Moment
    {
        if (!this.m_range) this.m_range = this.rangeExt.getChartRange();
        return this.m_range.maxAsMoment;
    }

    public addOrigin: Models.ReportLayoutBase;
    private addParent: ReportContainer; // if present, then adding after a widget and addOrigin represents an item
    private addIndex: number;

    public get canAddRow(): boolean
    {
        if (this.addParent) return !!this.asRow(this.addParent);
        return !!this.asColumn(this.addOrigin);
    }

    public get canAddColumn(): boolean
    {
        if (this.addParent) return !!this.asColumn(this.addParent);
        return !!this.asRow(this.addOrigin);
    }

    public graphsChanged = new Subject<void>();

    public graphs: Map<string, SharedAssetGraphExtended>;
    public units: ControlOption<EngineeringUnitsDescriptorExtended>[] = [];

    public models   = new Map<Models.CustomReportElement, ReportElementModel>();
    public rangeExt = RangeSelectionExtended.fromTimeRangeId(this.m_rangeId);
    private m_range: ChartTimeRange;

    public container: ReportContainer;
    public editItem: Models.ReportLayoutItem;
    public editParent: Models.ReportLayoutBase;

    public dragging: boolean           = false;
    private draggingPageBreak: boolean = false;

    public editItemConfig = OverlayConfig.newInstance({containerClasses: ["dialog-xl"]});

    public stateHistory: StateHistory<ReportContainer>;

    public get canPaste()
    {
        for (let data of this.clipboard.getAll())
        {
            if (data.getReportItem(null)) return true;
        }

        return false;
    }

    @ViewChild(OverlayComponent, {static: true}) itemConfigOverlay: OverlayComponent;
    @ViewChild(CustomReportBuilderRowConfigurerComponent, {static: true}) rowConfigurer: CustomReportBuilderRowConfigurerComponent;

    constructor(inj: Injector,
                private clipboard: ClipboardService)
    {
        super(inj);
    }

    public async ngOnInit()
    {
        super.ngOnInit();

        this.container = this.data.container;
        this.computeDropListIds();

        let descriptors = await this.app.domain.units.describeEngineeringUnits();
        this.units      = descriptors.map((descExt) => descExt.controlPointWithDescription);
        await this.initContent();

        this.stateHistory = new StateHistory(this);
    }

    public async restoreToState(state: ReportContainer)
    {
        this.container.children = state.children;
        await this.initReportLayouts(this.container);
        this.layoutChange();
    }

    public async cloneState(state: ReportContainer): Promise<ReportContainer>
    {
        if (this.asRow(state))
        {
            return Models.ReportLayoutRow.deepClone(state);
        }
        else if (this.asColumn(state))
        {
            return Models.ReportLayoutColumn.deepClone(state);
        }

        return null;
    }

    public async readState(): Promise<ReportContainer>
    {
        return this.container;
    }

    public asRow(layout: Models.ReportLayoutBase): Models.ReportLayoutRow
    {
        if (layout instanceof Models.ReportLayoutRow) return layout;
        return null;
    }

    public asColumn(layout: Models.ReportLayoutBase): Models.ReportLayoutColumn
    {
        if (layout instanceof Models.ReportLayoutColumn) return layout;
        return null;
    }

    public asItem(layout: Models.ReportLayoutBase): Models.ReportLayoutItem
    {
        if (layout instanceof Models.ReportLayoutItem) return layout;
        return null;
    }

    public addLayoutPrep(origin: ReportContainer | Models.ReportLayoutItem,
                         parent?: ReportContainer,
                         index?: number)
    {
        if (!this.asItem(origin)) parent = null;

        this.addOrigin = origin;
        this.addParent = parent;
        this.addIndex  = index;
    }

    public beginWidgetAdd()
    {
        this.editItem = null;
        this.itemConfigOverlay.toggleOverlay();
    }

    public async updateItem(item: Models.ReportLayoutItem)
    {
        if (item instanceof Models.ReportLayoutItem)
        {
            await this.initItemModel(item);

            let message = " report widget";
            if (this.editItem)
            {
                message = "edit" + message;
                this.editHelper(item);
            }
            else
            {
                message = "add" + message;
                this.addHelper(item);
            }

            this.layoutChange();

            // Save state
            await this.stateHistory.record(message);

            this.itemConfigOverlay.closeOverlay();
        }
    }

    public async addRow()
    {
        let newRow = Models.ReportLayoutRow.newInstance({children: []});
        this.addHelper(newRow);
        this.layoutChange();

        // Save state
        await this.stateHistory.record("add row");
    }

    public async addColumn()
    {
        let newCol = Models.ReportLayoutColumn.newInstance({children: []});
        this.addHelper(newCol);
        this.layoutChange();

        // Save state
        await this.stateHistory.record("add column");
    }

    private addHelper(layout: Models.ReportLayoutBase)
    {
        if (this.addParent)
        {
            let originIdx            = this.addParent.children.indexOf(this.addOrigin);
            let wrappingInstantiator = this.asColumn(this.addParent) ? Models.ReportLayoutRow.newInstance : Models.ReportLayoutColumn.newInstance;
            let wrapperContainer     = wrappingInstantiator({
                                                                children  : [this.addOrigin],
                                                                widthRatio: 1
                                                            });
            this.addParent.children.splice(originIdx, 1, wrapperContainer);

            wrapperContainer.children.push(layout);
        }
        else if (this.addOrigin)
        {
            if (this.addIndex == null)
            {
                this.addOrigin.children.push(layout);
            }
            else
            {
                this.addOrigin.children.splice(this.addIndex, 0, layout);
            }
        }
    }

    private editHelper(layout: Models.ReportLayoutItem)
    {
        if (this.editParent && this.editItem)
        {
            let idx = this.editParent.children.indexOf(this.editItem);
            if (idx >= 0)
            {
                this.editParent.children.splice(idx, 1, layout);
            }
        }

        this.editParent = null;
        this.editItem   = null;
    }

    public copy(layout: Models.ReportLayoutBase)
    {
        this.clipboard.copy(...this.gatherCopyable(layout));
    }

    public async paste()
    {
        let items = this.clipboard.getAll();

        let containerEntry: ClipboardEntryData<any, any>;
        let pasteItem: Models.ReportLayoutBase;

        for (let item of items)
        {
            let data = item.getReportItem(null);
            if (data instanceof Models.ReportLayoutColumn || data instanceof Models.ReportLayoutRow)
            {
                containerEntry = item;
                break;
            }
        }

        if (containerEntry)
        {
            let oldToNewGraphId = await this.ensureGraphs(containerEntry);

            pasteItem            = containerEntry.getReportItem(oldToNewGraphId);
            pasteItem.widthRatio = 1;

            let needsInvert: boolean;
            let pastingRow = !!this.asRow(pasteItem);
            if (this.asItem(this.addOrigin))
            {
                if (pastingRow)
                {
                    needsInvert = !!this.asColumn(this.addParent);
                }
                else
                {
                    needsInvert = !!this.asRow(this.addParent);
                }
            }
            else
            {
                if (pastingRow)
                {
                    needsInvert = !!this.asRow(this.addOrigin);
                }
                else
                {
                    needsInvert = !!this.asColumn(this.addOrigin);
                }
            }

            if (needsInvert) pasteItem = this.invertContainers(pasteItem);
        }
        else
        {
            let pasteData = items[0];
            if (pasteData)
            {
                let sharedGraphs    = UtilsService.arrayCopy(this.data.model.sharedGraphs);
                let oldToNewGraphId = await this.ensureGraphs(pasteData);
                pasteItem           = pasteData.getReportItem(oldToNewGraphId);

                if (!pasteItem)
                {
                    if (this.data.model.sharedGraphs.length != sharedGraphs.length)
                    {
                        this.data.model.sharedGraphs = sharedGraphs;
                        await this.resolveGraphs();
                    }
                    return;
                }
            }
        }

        // Initialize new models
        await this.initReportLayouts(pasteItem);

        this.addHelper(pasteItem);

        // Signal insertion to component
        this.layoutChange();

        // Save state
        await this.stateHistory.record("paste report widget");
    }

    private async ensureGraphs(entry: ClipboardEntryData<any, any>): Promise<Lookup<string>>
    {
        let someAdded                       = false;
        let oldToNewGraphId: Lookup<string> = {};
        let sharedGraphs                    = this.data.model.sharedGraphs;
        for (let entryGraph of entry.getReportGraphs())
        {
            let graph       = entryGraph.graph;
            let sharedGraph = sharedGraphs.find((sharedGraph) => UtilsService.compareJson(sharedGraph.graph, graph));
            if (!sharedGraph)
            {
                sharedGraph = SharedAssetGraphExtended.newModel(Models.AssetGraph.deepClone(graph), UUID.UUID(), "Asset Graph " + (sharedGraphs.length + 1));
                sharedGraphs.push(sharedGraph);
                someAdded = true;
            }

            oldToNewGraphId[entryGraph.id] = sharedGraph.id;
        }

        if (someAdded) await this.resolveGraphs();

        return oldToNewGraphId;
    }

    public dragStarted(layout: Models.ReportLayoutBase)
    {
        this.dragging          = true;
        this.draggingPageBreak = this.isLineBreak(layout);

        this.detectChanges();
    }

    public dragEnded()
    {
        this.dragging          = false;
        this.draggingPageBreak = false;
    }

    public isLineBreak(layout: Models.ReportLayoutBase): boolean
    {
        let item = this.asItem(layout);
        return item && this.models.get(item.element)?.type === ReportElementType.PageBreak;
    }

    public isEditable(layout: Models.ReportLayoutBase): boolean
    {
        let item = this.asItem(layout);
        if (item) return !this.isLineBreak(item);

        return this.asRow(layout)?.children.length > 1;
    }

    public editableTooltip(layout: Models.ReportLayoutBase): string
    {
        if (this.asItem(layout)) return "Configure widget";
        if (this.asRow(layout)?.children.length > 1) return "Configure column weights";

        return null;
    }

    public editLayout(parent: Models.ReportLayoutBase,
                      layout: Models.ReportLayoutBase)
    {
        let item = this.asItem(layout);
        if (item)
        {
            this.editParent = parent;
            this.editItem   = item;
            this.itemConfigOverlay.toggleOverlay();
        }

        let row = this.asRow(layout);
        if (row)
        {
            this.rowConfigurer.configureRow(row);
        }
    }

    async rowUpdated()
    {
        await this.stateHistory.record("column weights configured");
    }

    public getDropListId(container: ReportContainer): string
    {
        return this.m_dropListModelToId.get(container) || "0";
    }

    public async dropBefore(event: CdkDragDrop<Models.ReportLayoutBase>,
                            beforeItem: Models.ReportLayoutBase)
    {
        let itemDropped: Models.ReportLayoutBase       = event.item.data;
        let container: Models.ReportLayoutBase         = event.container.data;
        let previousContainer: Models.ReportLayoutBase = event.previousContainer.data;
        let currentIndex                               = beforeItem ? container.children.indexOf(beforeItem) : container.children.length;
        let previousIndex                              = previousContainer.children.indexOf(itemDropped);

        if (previousContainer !== container)
        {
            // Invert containers if needed
            if ((container instanceof Models.ReportLayoutColumn && itemDropped instanceof Models.ReportLayoutColumn) ||
                (container instanceof Models.ReportLayoutRow && itemDropped instanceof Models.ReportLayoutRow))
            {
                previousContainer.children[previousIndex] = this.invertContainers(itemDropped);
            }

            // Transfer the item
            transferArrayItem(previousContainer.children, container.children, previousIndex, currentIndex);
        }
        else
        {
            if (previousIndex < currentIndex)
            {
                currentIndex--;
            }

            moveItemInArray(container.children, previousIndex, currentIndex);
        }

        this.layoutChange();

        // Save state
        await this.stateHistory.record("move report widget");
    }

    public async removeLayout(parent: Models.ReportLayoutBase,
                              layout: Models.ReportLayoutBase)
    {
        let layoutString = this.getLayoutString(layout);
        if (layoutString != "widget" && layout.children.length === 0 ||
            await this.confirmOperation(`Are you sure you want to remove this ${layoutString}?`))
        {
            let layoutIdx = parent.children.indexOf(layout);
            parent.children.splice(layoutIdx, 1);
            this.layoutChange();

            // Save state
            await this.stateHistory.record("delete report widget");
        }
    }

    public getAddTooltip(layout: ReportContainer): string
    {
        if (this.asItem(layout)) return "Add item after this widget";

        return `Add to this ${this.getLayoutString(layout)}`;
    }

    public getCopyTooltip(layout: Models.ReportLayoutBase): string
    {
        return `Copy ${this.getLayoutString(layout)} to clipboard`;
    }

    public getRemoveTooltip(layout: Models.ReportLayoutBase): string
    {
        return `Remove ${this.getLayoutString(layout)}`;
    }

    public getPreviewText(layout: Models.ReportLayoutBase): string
    {
        let text = `1 ${this.getLayoutString(layout)}`;
        if (layout.children)
        {
            let numChildren = layout.children.length;
            text += ` with ${numChildren} ${UtilsService.pluralize("item", numChildren)}`;
        }

        return text;
    }

    public getLayoutString(layout: Models.ReportLayoutBase): "row" | "column" | "widget"
    {
        if (this.asRow(layout))
        {
            return "row";
        }
        else if (this.asColumn(layout))
        {
            return "column";
        }
        else
        {
            return "widget";
        }
    }

    private async initContent()
    {
        await this.resolveGraphs();
        await this.initReportLayouts(this.data.container);
    }

    private async initReportLayouts(layout: Models.ReportLayoutBase)
    {
        if (layout instanceof Models.ReportLayoutItem)
        {
            await this.initItemModel(layout);
        }

        await inParallel(layout.children || [], (child) => this.initReportLayouts(child));
    }

    public async resolveGraphs(): Promise<Map<string, SharedAssetGraphExtended>>
    {
        this.graphs = await SharedAssetGraphExtended.loadGraphs(this.app.domain, this.data.model.sharedGraphs);
        return this.graphs;
    }

    private async initItemModel(item: Models.ReportLayoutItem)
    {
        let reportModel = await DynamicReport.getModel(this.app, item, this.graphs, this.rangeStart, this.rangeEnd);
        if (reportModel)
        {
            reportModel.configuration.forPreview = true;
            this.models.set(item.element, reportModel);
        }
    }

    private layoutChange()
    {
        this.computeDropListIds();
        setTimeout(() => this.reportLayoutChange(), 200);
    }

    private computeDropListIds()
    {
        this.m_dropListModelToId.clear();

        this.m_allDropListIds = [];
        this.computeDropListsIdsInternal(this.container, 1);
        this.m_allDropListIds.reverse();

        this.m_rootDropListIds = [];
        this.m_rootDropListIds.push(this.getDropListId(this.container) + "-inner");
        for (let child of this.container.children || []) this.m_rootDropListIds.push(this.getDropListId(child));
    }

    private computeDropListsIdsInternal(layout: Models.ReportLayoutBase,
                                        id: number): number
    {
        let idString = `${id++}`;
        this.m_dropListModelToId.set(layout, idString);
        this.m_allDropListIds.push(idString);
        this.m_allDropListIds.push(idString + "-inner");

        for (let child of layout.children || [])
        {
            id = this.computeDropListsIdsInternal(child, id);
        }

        return id;
    }

    private invertContainers(layout: Models.ReportLayoutBase,
                             seen?: Set<Models.ReportLayoutBase>): Models.ReportLayoutBase
    {
        if (!seen)
        {
            seen = new Set([layout]);
        }
        else if (seen.has(layout))
        {
            return layout;
        }

        if (this.asItem(layout)) return layout;

        let inverter = this.asColumn(layout) ? Models.ReportLayoutRow.newInstance : Models.ReportLayoutColumn.newInstance;
        return inverter({children: layout.children.map((child) => this.invertContainers(child, seen))});
    }

    private gatherCopyable(layout: Models.ReportLayoutBase,
                           entries?: ClipboardEntryData<any, any>[]): ClipboardEntryData<any, any>[]
    {
        if (!entries)
        {
            entries = [];
            if (layout instanceof Models.ReportLayoutRow || layout instanceof Models.ReportLayoutColumn)
            {
                let layoutCopy = Models.ReportLayoutBase.deepClone(layout);

                entries.push(new class extends ClipboardEntryData<null, Models.ReportLayoutBase>
                {
                    constructor()
                    {
                        super("report container");
                    }

                    public getDashboardWidget(): null
                    {
                        return null;
                    }

                    public getReportItem(): Models.ReportLayoutBase
                    {
                        return Models.ReportLayoutBase.deepClone(layoutCopy);
                    }
                }());
            }
        }

        if (layout instanceof Models.ReportLayoutItem)
        {
            let entry = this.makeClipboardElementEntry(layout.element);
            entries.push(entry);
        }

        if (layout.children)
        {
            for (let child of layout.children)
            {
                this.gatherCopyable(child, entries);
            }
        }

        // Return all recursively gathered clipboard entries
        return entries;
    }

    private makeClipboardElementEntry(element: Models.CustomReportElement): ClipboardEntryData<any, any>
    {
        element = Models.CustomReportElement.deepClone(element);

        let range = FilterableTimeRangeExtended.newInstance(Models.RangeSelection.deepClone(this.rangeExt.model));

        if (element instanceof Models.CustomReportElementAggregatedValue)
        {
            let graphExt = this.graphs.get(element.controlPointGroup.pointInput?.graphId);
            let sharedGraph: Models.SharedAssetGraph;
            if (graphExt) sharedGraph = SharedAssetGraphExtended.newModel(graphExt.model, graphExt.id, null);

            let elementCopy = Models.CustomReportElementAggregatedValue.deepClone(element);

            return new class extends ClipboardEntryData<Models.AggregationWidgetConfiguration, Models.ReportLayoutItem>
            {
                constructor()
                {
                    super("aggregation summary");
                }

                public getDashboardWidget(): Models.AggregationWidgetConfiguration
                {
                    let cpGroup = Models.ControlPointsGroup.deepClone(elementCopy.controlPointGroup);
                    if (!cpGroup) return null;

                    if (sharedGraph && cpGroup.pointInput)
                    {
                        cpGroup.graph      = Models.AssetGraph.deepClone(sharedGraph.graph);
                        cpGroup.pointInput = Models.AssetGraphBinding.newInstance({nodeId: cpGroup.pointInput.nodeId});
                    }

                    return Models.AggregationWidgetConfiguration.newInstance(
                        {
                            name             : elementCopy.label,
                            controlPointGroup: cpGroup,
                            filterableRange  : Models.FilterableTimeRange.deepClone(range),
                            hideRange        : true
                        });
                }

                public getReportItem(oldToNewGraphId: Lookup<string>): Models.ReportLayoutItem
                {
                    let element = Models.CustomReportElementAggregatedValue.deepClone(elementCopy);

                    return Models.ReportLayoutItem.newInstance({element: element});
                }

                public getReportGraphs(): Models.SharedAssetGraph[]
                {
                    return sharedGraph ? [sharedGraph] : [];
                }
            }();
        }

        if (element instanceof Models.CustomReportElementAggregationTable)
        {
            let type: DataAggregationType;

            let graphIds = new Set<string>();
            if (element.groups?.length)
            {
                for (let group of element.groups)
                {
                    let graphId = group.pointInput?.graphId;
                    if (graphId) graphIds.add(graphId);
                }

                type = DataAggregationType.Groups;
            }
            else if (element.columns?.length)
            {
                if (!this.graphs.get(element.graphId)) return null;
                graphIds.add(element.graphId);

                type = DataAggregationType.Bindings;
            }

            let sharedGraphs: Models.SharedAssetGraph[] = [];
            for (let graphId of graphIds)
            {
                let graphExt = this.graphs.get(graphId);
                if (graphExt) sharedGraphs.push(graphExt.modelClone());
            }

            let elementCopy = Models.CustomReportElementAggregationTable.deepClone(element);

            return new class extends ClipboardEntryData<Models.AggregationTableWidgetConfiguration, Models.ReportLayoutItem>
            {
                constructor()
                {
                    super("aggregation table");
                }

                public getDashboardWidget(): Models.AggregationTableWidgetConfiguration
                {
                    let graph: Models.AssetGraph;
                    let groups: Models.ControlPointsGroup[] = [];
                    switch (type)
                    {
                        case DataAggregationType.Bindings:
                            if (sharedGraphs?.length !== 1) return null;
                            graph = Models.AssetGraph.deepClone(sharedGraphs[0].graph);
                            break;

                        case DataAggregationType.Groups:
                            if (!elementCopy.groups) return null;
                            groups = ClipboardEntryData.getDashboardGroups(elementCopy.groups, sharedGraphs);
                            if (groups.length !== elementCopy.groups.length) return null;
                            break;
                    }

                    return Models.AggregationTableWidgetConfiguration.newInstance({
                                                                                      name                   : elementCopy.label,
                                                                                      groups                 : groups,
                                                                                      columns                : elementCopy.columns?.map((col) => Models.AggregationNodeBinding.deepClone(col)) || [],
                                                                                      graph                  : graph,
                                                                                      filterableRanges       : [Models.FilterableTimeRange.deepClone(range)],
                                                                                      controlPointDisplayType: elementCopy.controlPointDisplayType,
                                                                                      visualizationMode      : elementCopy.visualizationMode
                                                                                  });
                }

                public getReportItem(oldToNewGraphId: Lookup<string>): Models.ReportLayoutItem
                {
                    let element = Models.CustomReportElementAggregationTable.deepClone(elementCopy);

                    if (oldToNewGraphId) element.graphId = oldToNewGraphId[element.graphId];

                    return Models.ReportLayoutItem.newInstance({element: element});
                }

                public getReportGraphs(): Models.SharedAssetGraph[]
                {
                    return UtilsService.arrayCopy(sharedGraphs);
                }
            }();
        }

        if (element instanceof Models.CustomReportElementAggregationTrend && element.groups?.length > 0)
        {
            let elementCopy = Models.CustomReportElementAggregationTrend.deepClone(element);

            let graphIds                                = new Set<string>();
            let sharedGraphs: Models.SharedAssetGraph[] = [];
            for (let group of elementCopy.groups)
            {
                let graphId = group.pointInput?.graphId;
                if (graphId && !graphIds.has(graphId))
                {
                    graphIds.add(graphId);
                    let graphExt = this.graphs.get(graphId);
                    if (graphExt) sharedGraphs.push(graphExt.modelClone());
                }
            }

            return new class extends ClipboardEntryData<Models.AggregationTrendWidgetConfiguration, Models.ReportLayoutItem>
            {
                constructor()
                {
                    super("aggregation trend");
                }

                public getDashboardWidget(): Models.AggregationTrendWidgetConfiguration
                {
                    let groups = ClipboardEntryData.getDashboardGroups(elementCopy.groups, sharedGraphs);
                    if (elementCopy.groups.length !== groups.length) return null;

                    return Models.AggregationTrendWidgetConfiguration.deepClone({
                                                                                    name             : elementCopy.label,
                                                                                    groups           : groups,
                                                                                    filterableRange  : range,
                                                                                    granularity      : elementCopy.granularity,
                                                                                    visualizationMode: elementCopy.visualizationMode,
                                                                                    showY            : elementCopy.showY,
                                                                                    showLegend       : elementCopy.showLegend
                                                                                });
                }

                public getReportItem(oldToNewGraphId: Lookup<string>): Models.ReportLayoutItem
                {
                    let element = Models.CustomReportElementAggregationTrend.deepClone(elementCopy);

                    return Models.ReportLayoutItem.newInstance({element: element});
                }

                public getReportGraphs(): Models.SharedAssetGraph[]
                {
                    return UtilsService.arrayCopy(sharedGraphs);
                }
            }();
        }

        if (element instanceof Models.CustomReportElementAlertFeed)
        {
            let elementCopy = Models.CustomReportElementAlertFeed.deepClone(element);

            return new class extends ClipboardEntryData<Models.AlertFeedWidgetConfiguration, Models.ReportLayoutItem>
            {
                constructor()
                {
                    super("alert feed");
                }

                public getDashboardWidget(): Models.AlertFeedWidgetConfiguration
                {
                    return Models.AlertFeedWidgetConfiguration.newInstance(
                        {
                            name      : elementCopy.label,
                            locations : UtilsService.arrayCopy(elementCopy.locations),
                            alertTypes: UtilsService.arrayCopy(elementCopy.alertTypes),
                            timeRange : Models.RangeSelection.deepClone(range?.range)
                        });
                }

                public getReportItem(): Models.ReportLayoutItem
                {
                    let element = Models.CustomReportElementAlertFeed.deepClone(elementCopy);
                    return Models.ReportLayoutItem.newInstance({element: element});
                }
            }();
        }

        if (element instanceof Models.CustomReportElementAlertTable)
        {
            let elementCopy = Models.CustomReportElementAlertTable.deepClone(element);

            return new class extends ClipboardEntryData<Models.AlertTableWidgetConfiguration, Models.ReportLayoutItem>
            {
                constructor()
                {
                    super("alert table");
                }

                public getDashboardWidget(): Models.AlertTableWidgetConfiguration
                {
                    return Models.AlertTableWidgetConfiguration.deepClone({
                                                                              name            : elementCopy.label || this.description,
                                                                              groupBy         : elementCopy.groupBy,
                                                                              locations       : elementCopy.locations,
                                                                              rollupType      : elementCopy.rollupType,
                                                                              alertStatusIDs  : elementCopy.alertStatusIDs,
                                                                              alertTypeIDs    : elementCopy.alertTypeIDs,
                                                                              alertSeverityIDs: elementCopy.alertSeverityIDs,
                                                                              severityColors  : elementCopy.severityColors,
                                                                              alertRules      : elementCopy.alertRules,
                                                                              filterableRanges: [range]
                                                                          });
                }

                public getReportItem(): Models.ReportLayoutItem
                {
                    let element = Models.CustomReportElementAlertTable.deepClone(elementCopy);
                    return Models.ReportLayoutItem.newInstance({element: element});
                }
            }();
        }

        if (element instanceof Models.CustomReportElementChartSet && element.charts)
        {
            let elementCopy = Models.CustomReportElementChartSet.deepClone(element);

            return new class extends ClipboardEntryData<Models.TimeSeriesWidgetConfiguration, Models.ReportLayoutItem>
            {
                constructor()
                {
                    super("chart");
                }

                public getDashboardWidget(): Models.TimeSeriesWidgetConfiguration
                {
                    return Models.TimeSeriesWidgetConfiguration.newInstance({
                                                                                range : Models.RangeSelection.deepClone(range?.range),
                                                                                charts: elementCopy.charts.map((chart) => Models.TimeSeriesChartConfiguration.deepClone(chart))
                                                                            });
                }

                public getReportItem(): Models.ReportLayoutItem
                {
                    return Models.ReportLayoutItem.newInstance({element: Models.CustomReportElementChartSet.deepClone(elementCopy)});
                }
            }();
        }

        return new class extends ClipboardEntryData<null, Models.ReportLayoutItem>
        {
            constructor()
            {
                super("report widget");
            }

            public getDashboardWidget(): null
            {
                return null;
            }

            public getReportItem(): Models.ReportLayoutItem
            {
                return Models.ReportLayoutItem.newInstance({element: Models.CustomReportElement.deepClone(element)});
            }
        }();
    }

    public getGraphs(): Models.SharedAssetGraph[]
    {
        return this.data.sharedGraphs;
    }

    public canRemove(graphId: string)
    {
        return !this.hasReference(this.data.model.container, graphId);
    }

    public canRemoveNode(graphId: string,
                         nodeId: string): boolean
    {
        return true;
    }

    private hasReference(container: Models.ReportLayoutBase,
                         graphId: string): boolean
    {
        if (!container) return false;
        if (container instanceof Models.ReportLayoutItem)
        {
            let widget = container.element;
            if (widget instanceof Models.CustomReportElementAggregatedValue)
            {
                return widget.controlPointGroup?.pointInput?.graphId === graphId;
            }
            if (widget instanceof Models.CustomReportElementAggregationTable || widget instanceof Models.CustomReportElementAggregationTrend)
            {
                if (widget.groups?.length) return widget.groups.some((gr) => gr.pointInput?.graphId === graphId);
            }
            if (widget instanceof Models.CustomReportElementAggregationTable && widget.columns.length)
            {
                return widget.graphId === graphId;
            }
            if (widget instanceof Models.CustomReportElementDeviceElementList)
            {
                return widget.pointInput?.graphId === graphId;
            }

            return false;
        }

        return container.children.some((child) => this.hasReference(child, graphId));
    }
}

type ReportContainer = Models.ReportLayoutColumn | Models.ReportLayoutRow;

