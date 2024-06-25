import {CdkDragEnd, CdkDragMove} from "@angular/cdk/drag-drop";
import {Point} from "@angular/cdk/drag-drop/drag-ref";
import {Injector, Renderer2} from "@angular/core";

import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {WidgetContainerComponent} from "app/dashboard/dashboard/widgets/widget-container.component";
import {WidgetGridManipulator} from "app/dashboard/dashboard/widgets/widget-grid-manipulator";
import {WidgetManagerScroller} from "app/dashboard/dashboard/widgets/widget-manager-scroller";
import {WidgetManagerComponent} from "app/dashboard/dashboard/widgets/widget-manager.component";
import {WidgetManipulation} from "app/dashboard/dashboard/widgets/widget-manipulation";
import {DashboardConfigurationExtended, WidgetGraph} from "app/services/domain/dashboard-management.service";
import * as Models from "app/services/proxy/model/models";

import {Lookup, PxPerCharacter, UtilsService} from "framework/services/utils.service";
import {Vector2} from "framework/ui/charting/charting-math";
import {ScrollerConfig, ScrollState} from "framework/ui/utils/drag-scroller";

export class WidgetManipulatorHierarchy
{
    activeManipulator: WidgetManipulator;
    activeManipulation: WidgetManipulation;
    baseStates: Lookup<WidgetManipulationBaseState>;

    private m_ids: string[];
    get ids(): string[]
    {
        return this.m_ids;
    }

    private m_setUpManipulators = new Set<string>();
    private m_settingUpForDrag  = false;
    set settingUpForDrag(settingUp: boolean)
    {
        this.m_settingUpForDrag = settingUp;
        if (settingUp) this.m_setUpManipulators = new Set();
    }

    constructor(public readonly rootManipulator: WidgetManipulator)
    {
    }

    public setUpForDrag(manipulatorId: string)
    {
        if (this.m_settingUpForDrag)
        {
            this.m_setUpManipulators.add(manipulatorId);
        }
    }

    public isSetUpForDrag(manipulatorId: string): boolean
    {
        return !this.m_settingUpForDrag || this.m_setUpManipulators.has(manipulatorId);
    }

    public updateManipulatorIds()
    {
        const ids      = new Set<string>();
        const workList = [this.rootManipulator];
        while (workList.length)
        {
            const curr = workList.pop();

            ids.add(curr.id);
            for (let child of curr.childManipulators) workList.push(child);
        }

        this.m_ids = Array.from(ids);
    }
}

export class WidgetManipulator
{
    public static readonly MIN_WIDGET_TOOLBAR_WIDTH         = 42;
    public static readonly MIN_TOP_LEVEL_ROW_HEIGHT         = 60;
    public static readonly CELL_DIMENSION_TOOLTIP_THRESHOLD = 10;
    public static readonly MIN_CELL_DIMENSION               = 5;
    public static readonly BASE_SUBWIDGET_WIDTH             = 200;
    public static readonly BASE_SUBWIDGET_HEIGHT            = 100;

    private static readonly CHARACTERS_PER_COL = 15;

    public readonly resizePoints = ResizePoints;

    private m_editing = false;
    get editing(): boolean
    {
        return this.m_editing;
    }

    set editing(editing: boolean)
    {
        if (this.m_editing !== editing)
        {
            this.m_editing = editing;

            if (editing)
            {
                this.ensureGridRows();
                this.resetWidgetSizes();
            }
            else
            {
                setTimeout(() => this.updateWidgetSizes(), 0);
            }

            this.markForCheck();

            this.refreshWidgets(false);
        }
    }

    get isReady(): boolean
    {
        return !!this.m_component;
    }

    private readonly m_hierarchy: WidgetManipulatorHierarchy;

    get manipulatorIds(): string[]
    {
        return this.m_hierarchy.ids;
    }

    get activeManipulation(): WidgetManipulation
    {
        return this.m_hierarchy.activeManipulation;
    }

    get manipulationBaseState(): WidgetManipulationBaseState
    {
        return this.m_hierarchy.baseStates[this.id];
    }

    set manipulationBaseState(state: WidgetManipulationBaseState)
    {
        this.m_hierarchy.baseStates[this.id] = state;
    }

    get manipulationId(): string
    {
        return this.activeManipulation?.widgetId;
    }

    get isSetUpForDrag(): boolean
    {
        return this.m_hierarchy.isSetUpForDrag(this.id);
    }

    get dragging(): boolean
    {
        return this.activeManipulation?.forDrag;
    }

    get resizing(): boolean
    {
        return this.activeManipulation && !this.activeManipulation.forDrag;
    }

    private m_dragEnterableStart: number;

    get dragEnterableDuration(): number
    {
        if (!this.m_dragEnterableStart) return -1;
        return performance.now() - this.m_dragEnterableStart;
    }

    set dragEnterable(enterable: boolean)
    {
        if (enterable)
        {
            if (!this.m_dragEnterableStart) this.m_dragEnterableStart = performance.now();
        }
        else
        {
            this.m_dragEnterableStart = null;
        }
    }

    private m_columnar: boolean = false;
    get columnar(): boolean
    {
        return this.m_columnar;
    }

    set columnar(columnar: boolean)
    {
        if (columnar !== this.m_columnar)
        {
            this.m_columnar = columnar;

            this.updateNumRows();

            // let redraw take place first
            setTimeout(() => this.refresh());
        }
    }

    inView = true;

    private m_scroller: WidgetManagerScroller;

    private m_childManipulators: WidgetManipulator[] = [];
    get childManipulators(): WidgetManipulator[]
    {
        return this.m_childManipulators;
    }

    get synchronized(): boolean
    {
        if (this.m_widgetContainers.length !== this.widgetOutlineExts.length) return false;
        return this.m_childManipulators.every((manipulator) => manipulator.synchronized);
    }

    private m_widgetContainerLookup: Lookup<WidgetContainerComponent> = {}; // all WidgetContainer descendants
    private m_widgetContainers: WidgetContainerComponent[]            = []; // only WidgetContainer children
    set widgetContainers(widgets: WidgetContainerComponent[])
    {
        this.m_widgetContainers = widgets || [];

        this.m_childManipulators = [];
        for (let widgetContainer of this.m_widgetContainers) widgetContainer.widget.collectWidgetManipulators(this.m_childManipulators);

        this.updateWidgetContainerLookup();
        this.updateRelatedToFocus();
        this.updateNumRows();
        this.updateWidgetSizes();

        if (this.m_parentManipulator) this.m_parentManipulator.updateWidgetContainerLookup();
        this.m_hierarchy.updateManipulatorIds();
    }

    get widgetContainers(): WidgetContainerComponent[]
    {
        return this.m_widgetContainers;
    }

    widgetOutlineExts: WidgetOutlineExtended[]            = [];
    widgetOutlineExtLookup: Lookup<WidgetOutlineExtended> = {};
    gridManipulator: WidgetGridManipulator<string>;

    private m_baseFontSize: number;
    get baseFontSize(): number
    {
        return this.m_baseFontSize * this.widgetGridConfig.baseFontScalar;
    }

    manipulationMinOutline: Models.WidgetOutline;

    get hasParentManipulator(): boolean
    {
        return !!this.m_parentManipulator;
    }

    get rootManipulator(): WidgetManipulator
    {
        return this.m_hierarchy.rootManipulator;
    }

    private m_focusId: string;

    get focusId(): string
    {
        return this.m_focusId;
    }

    set focusId(widgetId: string)
    {
        if (!widgetId || !this.widgetOutlineExtLookup[widgetId]) widgetId = undefined;

        if (this.m_focusId != widgetId)
        {
            let changedIdx = widgetId === undefined ? this.m_focusId : widgetId;
            this.m_focusId = widgetId;

            this.updateRelatedToFocus();

            this.refreshWidget(changedIdx);
        }
    }

    private get focusWidget(): WidgetBaseComponent<any, any>
    {
        let widget: WidgetBaseComponent<any, any>;

        let manipulator: WidgetManipulator = this;
        while (manipulator?.focusId)
        {
            let nextWidget = manipulator.m_widgetContainerLookup[manipulator.focusId]?.widget;
            if (nextWidget)
            {
                widget      = nextWidget;
                manipulator = manipulator.m_childLookup.get(manipulator.focusId);
            }
            else
            {
                return widget;
            }
        }

        return widget;
    }

    private m_destroyed = false;
    get destroyed(): boolean
    {
        return this.m_destroyed;
    }

    private m_numRows: number;
    private m_numColumnarRows: number;

    get numRows(): number
    {
        if (this.columnar) return this.m_numColumnarRows;
        if (this.widgetGridConfig.numRows) return this.widgetGridConfig.numRows;
        return this.m_numRows;
    }

    get numCols(): number
    {
        let numCols = this.widgetGridConfig.numCols;
        if (!numCols)
        {
            numCols = this.gridManipulator.numCols;
        }

        return numCols;
    }

    colWidth: number;

    private m_aspectRatio: number = null;
    set aspectRatio(ratio: number)
    {
        this.m_aspectRatio = ratio;
        this.updateGridDimensions();
    }

    private m_rowHeight: number;
    get rowHeight(): number
    {
        if (this.m_aspectRatio)
        {
            return Math.max(this.colWidth / this.m_aspectRatio, WidgetManipulator.MIN_TOP_LEVEL_ROW_HEIGHT);
        }

        return this.m_rowHeight;
    }

    set rowHeight(height: number)
    {
        if (!this.widgetGridConfig.rowHeight) this.m_rowHeight = height;
    }

    get gridHeightPx(): number
    {
        if (this.widgetGridConfig.rowHeight)
        {
            return this.numRows * this.widgetGridConfig.rowHeight + (this.columnar ? 0 : 2);
        }
        else if (this.m_aspectRatio)
        {
            return this.rowHeight * this.numRows;
        }

        // fills container
        return undefined;
    }

    private m_widgetIdentifier: WidgetIdentifier;
    private m_component: WidgetManagerComponent;

    get initialized(): boolean
    {
        if (!this.m_component) return false;
        if (!this.m_widgetIdentifier) return false;

        return true;
    }

    get layoutContainer(): HTMLElement
    {
        return this.m_component?.element.nativeElement;
    }

    private m_domRect: DOMRect;
    get domRect(): DOMRect
    {
        if (!this.m_domRect) this.m_domRect = this.layoutContainer?.getBoundingClientRect();
        return this.m_domRect;
    }

    private m_childLookup = new Map<string, WidgetManipulator>();

    public readonly renderer: Renderer2;

    constructor(inj: Injector,
                public readonly widgetGridConfig: WidgetLayoutConfig,
                private readonly m_scrollerConfig: ScrollerConfig,
                public readonly id: string,
                private readonly m_parentManipulator: WidgetManipulator)
    {
        this.renderer = inj.get(Renderer2);

        this.gridManipulator = new WidgetGridManipulator<string>(this.widgetGridConfig.numRows || -1, this.widgetGridConfig.numCols);
        this.m_rowHeight     = this.widgetGridConfig.rowHeight;

        if (this.m_parentManipulator)
        {
            this.m_parentManipulator.addChild(this);
        }

        if (this.m_scrollerConfig)
        {
            this.m_scroller = new WidgetManagerScroller(this.m_scrollerConfig, this);
        }

        if (!this.m_parentManipulator)
        {
            this.m_hierarchy = new WidgetManipulatorHierarchy(this);
        }
        else
        {
            let parent = this.m_parentManipulator;
            while (parent.m_parentManipulator) parent = parent.m_parentManipulator;

            this.m_hierarchy = parent.m_hierarchy;
        }
    }

    private updateWidgetContainerLookup()
    {
        this.m_widgetContainerLookup = {};
        this.collectWidgetContainers(this.m_widgetContainerLookup);
        for (let subManipulator of this.m_childManipulators)
        {
            subManipulator.collectWidgetContainers(this.m_widgetContainerLookup);
        }
    }

    private collectWidgetContainers(widgetContainerLookup: Lookup<WidgetContainerComponent>)
    {
        for (let widgetContainer of this.m_widgetContainers)
        {
            widgetContainerLookup[widgetContainer.widget.id] = widgetContainer;
        }
    }

    private addChild(child: WidgetManipulator)
    {
        if (this.id !== child.id)
        {
            this.m_childLookup.set(child.id, child);
        }
    }

    public setup(component: WidgetManagerComponent)
    {
        this.m_component        = component;
        this.m_widgetIdentifier = new WidgetIdentifier(this.id);

        this.generateGridLookup();
    }

    public reset(widgets: Models.WidgetComposition[])
    {
        widgets = widgets.sort((widgetA,
                                widgetB) =>
                               {
                                   const vertComparison = UtilsService.compareNumbers(widgetA.outline.top, widgetB.outline.top, true);
                                   return vertComparison || UtilsService.compareNumbers(widgetA.outline.left, widgetB.outline.left, true);
                               });
        this.m_widgetIdentifier.updateIdentifiers(widgets);

        this.widgetOutlineExts      = widgets.map((composition) => new WidgetOutlineExtended(this, composition));
        this.widgetOutlineExtLookup = UtilsService.extractLookup(this.widgetOutlineExts);

        this.updateNumRows();
        this.generateGridLookup();
    }

    public dragStarting()
    {
        this.m_hierarchy.settingUpForDrag = true;
    }

    public setUpForDrag()
    {
        this.m_hierarchy.setUpForDrag(this.id);
    }

    public clearDomRects()
    {
        this.m_domRect = null;
        for (let manipulator of this.m_childManipulators) manipulator.clearDomRects();
    }

    public getWidgetContainer(widgetId: string): WidgetContainerComponent
    {
        if (!widgetId || !this.m_widgetContainers.length) return null;

        return this.m_widgetContainerLookup[widgetId];
    }

    private updateRelatedToFocus()
    {
        let rootManipulator = this.rootManipulator;
        let widgetGraph     = new WidgetGraph(rootManipulator);
        let focusWidget     = this.m_widgetContainerLookup[this.m_focusId]?.widget;
        if (focusWidget && !widgetGraph.getNode(focusWidget).children.length)
        {
            widgetGraph.forEachNode((node) => node.widget.relatedToFocus = !node.children.length && focusWidget.isAssetGraphRelated(node.widget));
        }
        else
        {
            let overallFocusWidget = rootManipulator.focusWidget;
            if (!overallFocusWidget || widgetGraph.getNode(overallFocusWidget).children.length)
            {
                widgetGraph.forEachNode((node) => node.widget.relatedToFocus = false);
            }
        }
        rootManipulator.markForCheck(true);
    }

    //--//

    public markForCheck(alsoDescendants?: boolean)
    {
        this.m_component?.markForCheck();
        if (alsoDescendants)
        {
            for (let childManipulator of this.m_childLookup.values())
            {
                childManipulator.markForCheck(alsoDescendants);
            }
        }
    }

    public detectChanges()
    {
        this.m_component?.detectChanges();
    }

    //--//

    public scrollToTop()
    {
        if (this.m_scroller)
        {
            this.m_scroller.scrollToTop();
            this.updateViewport(true);
        }
    }

    public updateViewport(detectChanges: boolean)
    {
        if (this.m_scroller)
        {
            this.m_scroller.updateViewport(detectChanges);
        }
    }

    public identifyWidget(idx: number,
                          widgetOutlineExt: WidgetOutlineExtended): string
    {
        return this.m_widgetIdentifier.getIdentifier(widgetOutlineExt?.id);
    };

    /**
     * by default, returns from widgets from top to bottom
     *
     * @param sortFn by which you want the widgets returned
     */
    private orderedWidgetOutlines(sortFn?: (a: WidgetOutlineExtended,
                                            b: WidgetOutlineExtended) => number): WidgetOutlineExtended[]
    {
        if (!sortFn)
        {
            sortFn = (a,
                      b) => a.top - b.top;
        }

        return this.widgetOutlineExts
                   .sort(sortFn);
    }

    private updateNumRows()
    {
        if (!this.widgetOutlineExts?.length)
        {
            this.m_numRows = this.m_numColumnarRows = 0;
        }
        else
        {
            let numColumnarRows = 0;
            this.m_numRows      = -1;

            for (let outline of this.widgetOutlineExts)
            {
                let widgetContainer = this.m_widgetContainerLookup[outline.id];
                if (!isNaN(numColumnarRows) && widgetContainer)
                {
                    outline.columnarTop    = numColumnarRows;
                    outline.columnarHeight = widgetContainer.widget.getUpdatedColumnarHeight();
                    numColumnarRows += outline.columnarHeight;
                }
                else
                {
                    numColumnarRows = NaN;
                }

                this.m_numRows = Math.max(this.m_numRows, outline.top + outline.height);
            }

            if (!isNaN(numColumnarRows)) this.m_numColumnarRows = numColumnarRows;

            this.ensureGridRows();
        }
    }

    public bannerToggled(bannerShowing: boolean)
    {
        if (this.m_scroller)
        {
            this.m_scroller.bannerShowing = bannerShowing;
        }
    }

    public refresh(resetLoaded: boolean = true): Promise<boolean>
    {
        this.updateGridDimensions();
        this.updateWidgetSizes();
        return this.refreshWidgets(resetLoaded);
    }

    public recordWidgetDeletion(widgetId: string)
    {
        let outlineExt = this.widgetOutlineExtLookup[widgetId];
        this.widgetOutlineExts.splice(this.widgetOutlineExts.indexOf(outlineExt), 1);
        delete this.widgetOutlineExtLookup[widgetId];

        if (this.m_focusId === widgetId) this.m_focusId = undefined;
    }

    /**
     * returns true if widget was modified
     *
     * @param widgetId that may have been modified
     */
    public updateGrid(widgetId: string): boolean
    {
        let widgetOutlineExt = this.widgetOutlineExtLookup[widgetId];
        let prevOutline      = widgetOutlineExt.model;
        let newOutline       = widgetOutlineExt.effectiveModel;
        if (widgetOutlineExt.updateModel())
        {
            if (!this.columnar)
            {
                this.updateGridHelper(prevOutline, (row,
                                                    col) =>
                {
                    if (this.gridManipulator.getEntry(row, col) === widgetId)
                    {
                        this.gridManipulator.setEntry(row, col, undefined);
                    }
                });

                this.updateGridHelper(newOutline, (row,
                                                   col) => this.gridManipulator.setEntry(row, col, widgetId));
            }

            return true;
        }

        return false;
    }

    public updateGridHelper(model: Models.WidgetOutline,
                            updateCell: (row: number,
                                         col: number) => void)
    {
        let endRow = model.top + model.height;
        let endCol = model.left + model.width;

        for (let row = model.top; row < endRow; row++)
        {
            for (let col = model.left; col < endCol; col++)
            {
                updateCell(row, col);
            }
        }
    }

    public isWidgetViewable(outlineExt: WidgetOutlineExtended): boolean
    {
        if (!this.m_scroller) return this.inView;

        let renderBounds = this.m_scroller.renderBounds;
        if (!renderBounds) return undefined;

        let widgetTop: number;
        let widgetBottom: number;
        if (this.columnar)
        {
            if (isNaN(outlineExt.columnarTop)) return undefined;

            widgetTop    = outlineExt.columnarTop;
            widgetBottom = widgetTop + outlineExt.columnarHeight;
        }
        else
        {
            widgetTop    = outlineExt.activeTop;
            widgetBottom = widgetTop + outlineExt.activeHeight;
        }

        const rowHeight = this.rowHeight;
        widgetTop *= rowHeight;
        widgetBottom *= rowHeight;

        return !(widgetBottom < renderBounds.low || widgetTop > renderBounds.high);
    }

    private generateGridLookup(): void
    {
        this.gridManipulator = WidgetManipulator.generateWidgetRepresentation(this.widgetOutlineExts,
                                                                              this.widgetGridConfig.numRows,
                                                                              this.widgetGridConfig.numCols);

        this.updateGridDimensions();
        this.ensureGridRows();
    }

    private ensureGridRows()
    {
        if (!this.m_columnar && this.widgetGridConfig.numRows == null)
        {
            let minRows = Math.ceil(window.innerHeight / this.rowHeight);
            if (this.numRows < minRows || this.gridManipulator.numRows < minRows)
            {
                this.updateLayoutHeight(minRows);
            }
        }
    }

    public updateLayoutHeight(newGridHeight: number): void
    {
        if (this.columnar)
        {
            this.m_numColumnarRows = newGridHeight;
        }
        else
        {
            this.gridManipulator.expandRows(newGridHeight);

            let heightMultiplier = this.m_numRows / newGridHeight;
            this.m_numRows       = newGridHeight;

            let widgetContainerElements = this.widgetContainers.map((widget) => widget.element.nativeElement);
            for (let i = 0; i < widgetContainerElements.length; i++)
            {
                let widgetContainerElement       = <HTMLElement>widgetContainerElements[i];
                widgetContainerElement.style.top = UtilsService.parseComputedStyle(widgetContainerElement.style.top, "%") * heightMultiplier + "%";
            }
        }

        let gridHeightPx = this.gridHeightPx;
        if (this.layoutContainer && !isNaN(gridHeightPx)) this.layoutContainer.style.minHeight = gridHeightPx + "px";

        if (!this.columnar) this.detectChanges(); // necessary for ngForOf of background grid to add in new rows while scrolling
    }

    public collapseVertically(): boolean
    {
        if (this.columnar) return false;

        let widgetsMoved = false;
        let outlineExts  = this.orderedWidgetOutlines();
        let currHeights  = new Array(this.widgetGridConfig.numCols).fill(0);
        for (let outlineExt of outlineExts)
        {
            let furthestTopPoint = 0;
            let right            = outlineExt.left + outlineExt.width;
            for (let col = outlineExt.left; col < right; col++) furthestTopPoint = Math.max(currHeights[col], furthestTopPoint);
            for (let col = outlineExt.left; col < right; col++) currHeights[col] = furthestTopPoint + outlineExt.height;

            if (outlineExt.top !== furthestTopPoint)
            {
                let outline = outlineExt.modelCopy(true);
                outline.top = furthestTopPoint;
                outlineExt.updateOverride(outline);
                this.updateGrid(outlineExt.id);
                widgetsMoved = true;
            }
        }

        return widgetsMoved;
    }

    public updateWidgetSizes(): void
    {
        if (this.m_editing) return;

        this.resetWidgetSizes();
        if (this.rowHeight <= 0) return;

        let widgetSizeOverrides = new Map<string, number>();
        let outlineExts         = this.orderedWidgetOutlines();

        for (let outlineExt of outlineExts)
        {
            let container = this.getWidgetContainer(outlineExt.widgetId);
            if (!container) return;

            let desiredSize = container.getDesiredHeight();
            if (desiredSize != null)
            {
                widgetSizeOverrides.set(outlineExt.id, desiredSize);
            }
        }

        for (let [id, size] of widgetSizeOverrides.entries())
        {
            let outlineExt            = this.widgetOutlineExtLookup[id];
            // divide size by rowHeight to get decimal number of rows for desired height
            outlineExt.heightOverride = Math.min(outlineExt.height, size / this.rowHeight);
        }

        if (this.columnar)
        {
            // Column mode, updating heights is good enough
            return;
        }

        let heights = WidgetManipulator.generateWidgetOutlineGrid(outlineExts,
                                                                  (ext) => ext.activeHeight,
                                                                  this.numRows,
                                                                  this.widgetGridConfig.numCols);

        let top = 0;

        for (let r = 0; r < heights.numRows; r++)
        {
            // Check if widgets are placed in this row
            if (!heights.hasAnyEntriesOnRow(r))
            {
                top++;
                continue;
            }

            let height = 0;
            for (let c = 0; c < this.widgetGridConfig.numCols; c++)
            {
                let possibleHeight = heights.getEntry(r, c);
                if (possibleHeight != null)
                {
                    height = Math.max(height, possibleHeight);
                }
            }

            height = Math.min(height, 1);

            for (let outlineExt of outlineExts)
            {
                if (outlineExt.top === r)
                {
                    outlineExt.topOverride = top;
                }

                let bottom = outlineExt.top + outlineExt.height;
                if (outlineExt.model.top <= r && bottom > r)
                {
                    for (let wr = outlineExt.model.top; wr < bottom; wr++)
                    {
                        for (let c = outlineExt.left; c < outlineExt.left + outlineExt.width; c++)
                        {
                            heights.setEntry(wr, c, Math.max(0, heights.getEntry(wr, c) - height));
                        }
                    }
                }
            }

            top += height;
        }
    }

    private resetWidgetSizes()
    {
        for (let outlineExt of this.orderedWidgetOutlines())
        {
            outlineExt.topOverride    = undefined;
            outlineExt.heightOverride = undefined;
        }
    }

    public initializeNewWidgetManipulation(widgetId: string,
                                           resize: ResizePoint,
                                           widgetCursorOffset: Vector2,
                                           widgetPreviewDiagonal: Vector2): boolean
    {
        if (this.columnar) return false;
        if (!resize && !widgetCursorOffset) return false;

        const widgetContainer = this.getWidgetContainer(widgetId);
        if (!widgetContainer.widget) return false;

        this.m_hierarchy.activeManipulation = new WidgetManipulation(this, widgetContainer, resize, widgetCursorOffset, widgetPreviewDiagonal);
        this.m_hierarchy.baseStates         = {};
        this.initializeWidgetManipulation();
        this.manipulationBaseState = new WidgetManipulationBaseState(this.widgetOutlineExtLookup[widgetId].model,
                                                                     new Vector2(0, 0),
                                                                     new Vector2(0, 0));

        return true;
    }

    public initializeWidgetManipulation()
    {
        this.m_hierarchy.activeManipulator = this;
        this.manipulationMinOutline        = this !== this.rootManipulator ? this.getMinOutline() :
            Models.WidgetOutline.newInstance({
                                                 width : 1,
                                                 height: 1
                                             });
        this.focusId                       = this.manipulationId;

        if (this.m_scroller) this.m_scroller.setUpForScroll();
    }

    public ensureBaseState(widgetOutlineExt: WidgetOutlineExtended,
                           manipulationEvent: CdkDragMove<string>)
    {
        if (!this.manipulationBaseState && this.dragging)
        {
            const baseOutline          = widgetOutlineExt.getTransferOutline(this);
            const widthPx              = baseOutline.width * this.colWidth;
            const heightPx             = baseOutline.height * this.rowHeight;
            const previewCenter        = this.activeManipulation.previewCenter;
            const shadowPreviewOffset  = new Vector2(previewCenter.x - this.domRect.left - widthPx / 2,
                                                     previewCenter.y - this.domRect.top - heightPx / 2);
            baseOutline.left           = Math.round(shadowPreviewOffset.x / this.colWidth);
            baseOutline.top            = Math.round(shadowPreviewOffset.y / this.rowHeight);
            const targetOffset         = new Vector2(baseOutline.left * this.colWidth,
                                                     baseOutline.top * this.rowHeight);
            this.manipulationBaseState = new WidgetManipulationBaseState(baseOutline,
                                                                         shadowPreviewOffset.differenceVector(targetOffset),
                                                                         new Vector2(manipulationEvent.distance.x, manipulationEvent.distance.y));
        }
    }

    public getManipulationVector(manipulationEvent: CdkDragMove): Vector2
    {
        const totalDist = manipulationEvent.distance;
        const startDist = this.manipulationBaseState.startDist;
        const localDist = new Vector2(totalDist.x - startDist.x, totalDist.y - startDist.y);
        const shifts    = localDist.add(this.manipulationBaseState.baseOutlineDist);

        return new Vector2(Math.round(shifts.x / this.colWidth), Math.round(shifts.y / this.rowHeight));
    }

    public handleDrag(manipulationEvent: CdkDragMove)
    {
        this.m_hierarchy.settingUpForDrag = false;
        const activeManipulator           = this.m_hierarchy.activeManipulator || this;
        activeManipulator.performWidgetManipulation(manipulationEvent);
    }

    public updateEnterable()
    {
        const rootManipulator = this.rootManipulator;
        if (this === rootManipulator)
        {
            const previewCenter = this.activeManipulation.previewCenter;
            let rootEnterable   = this.enterableHelper(previewCenter);

            for (let child of this.m_childManipulators)
            {
                const enterable     = child.enterableHelper(previewCenter);
                child.dragEnterable = enterable;
                if (enterable) rootEnterable = false;
            }

            rootManipulator.dragEnterable = rootEnterable;
        }
        else
        {
            rootManipulator.updateEnterable();
        }
    }

    private enterableHelper(previewCenter: Point): boolean
    {
        const domRect = this.domRect;
        return !!previewCenter &&
               previewCenter.x >= domRect.left && previewCenter.x < domRect.right &&
               previewCenter.y >= domRect.top && previewCenter.y < domRect.bottom;
    }

    public adoptManipulation()
    {
        this.activeManipulation.transferManipulator(this);
        this.initializeWidgetManipulation();
    }

    public transferWidget(): WidgetOutlineExtended
    {
        const widgetId        = this.activeManipulation.widgetId;
        const prevManipulator = this.activeManipulation.activeManipulator;
        const prevOutlineExt  = prevManipulator.widgetOutlineExtLookup[widgetId];
        let outlineExt        = this.widgetOutlineExtLookup[widgetId];

        if (!outlineExt)
        {
            outlineExt = new WidgetOutlineExtended(this, Models.WidgetComposition.newInstance({
                                                                                                  config : prevOutlineExt.config,
                                                                                                  outline: prevManipulator.manipulationBaseState.baseOutline
                                                                                              }));
            this.widgetOutlineExts.push(outlineExt);
            this.widgetOutlineExtLookup[widgetId] = outlineExt;
            this.detectChanges();
        }
        else
        {
            outlineExt.isPresent = true;
        }

        if (this.activeManipulation.originManipulator === prevManipulator)
        {
            // can't remove original manipulator's outlineExt because it will end the cdkDrag
            prevOutlineExt.isPresent = false;

            // move to invalid position so dragging back into previous position of previous drop list is not blocked
            const prevOutline = prevOutlineExt.effectiveModel;
            prevOutline.left  = -prevOutline.width;
            prevOutline.top   = -prevOutline.height;
            prevOutlineExt.updateOverride(prevOutline);
        }
        else
        {
            prevManipulator.widgetOutlineExts.pop();
            prevManipulator.widgetOutlineExtLookup[widgetId] = null;
            prevManipulator.detectChanges();
        }
        prevManipulator.updateNumRows();
        prevManipulator.generateGridLookup();

        return outlineExt;
    }

    public performWidgetManipulation(manipulationEvent: CdkDragMove)
    {
        if (this.activeManipulation)
        {
            this.m_scroller?.processState(manipulationEvent.pointerPosition.y);
            this.activeManipulation.processState(manipulationEvent);
        }
    }

    public completeManipulation(dragEnd: CdkDragEnd): boolean
    {
        this.refresh(this.resizing);

        let changed = this.rootManipulator.completeManipulationLocal(dragEnd);
        for (const manipulator of this.rootManipulator.childManipulators)
        {
            if (manipulator.completeManipulationLocal(dragEnd)) changed = true;
        }

        this.m_hierarchy.activeManipulator  = null;
        this.m_hierarchy.activeManipulation = null;

        return changed;
    }

    private completeManipulationLocal(dragEnd: CdkDragEnd): boolean
    {
        let changed = false;
        if (this.activeManipulation)
        {
            this.stopScrolling();

            if (this.activeManipulation.activeManipulator === this)
            {
                changed = this.activeManipulation.completeManipulation(dragEnd);

                if (!this.activeManipulation.forDrag && this.layoutContainer.parentElement)
                {
                    for (let resize of this.activeManipulation.resizeInfo.split(" "))
                    {
                        this.renderer.removeClass(this.layoutContainer.parentElement, resize);
                    }
                }
            }

            if (this.activeManipulation.manipulatorParticipated(this))
            {
                this.m_component.completeDrag(this.manipulationId);
            }

            this.manipulationMinOutline = null;
            this.manipulationBaseState  = null;
        }

        return changed;
    }

    public stopScrolling()
    {
        if (this.m_scroller) this.m_scroller.updateState(ScrollState.Stationary);
    }

    public destroy()
    {
        if (this.m_scroller) this.m_scroller.destroy();
        this.m_destroyed = true;
    }

    private updateGridDimensions(): void
    {
        if (this.layoutContainer)
        {
            this.colWidth = this.layoutContainer.clientWidth / this.widgetGridConfig.numCols;
            if (this.m_aspectRatio)
            {
                this.rowHeight = this.colWidth / this.m_aspectRatio;
            }
            else
            {
                this.rowHeight = this.layoutContainer.clientHeight / this.numRows;
            }
        }
        else
        {
            this.colWidth  = 0;
            this.rowHeight = 0;
        }

        this.m_baseFontSize = this.colWidth / (PxPerCharacter * WidgetManipulator.CHARACTERS_PER_COL);
    }

    private async refreshWidgets(resetLoaded: boolean): Promise<boolean>
    {
        let widgetContainers = this.widgetContainers;
        if (widgetContainers.length > 0)
        {
            let promises = [];
            for (let widget of widgetContainers)
            {
                if (resetLoaded && widget.widget) widget.widget.loaded = false;
                promises.push(widget.refreshWidget());
            }

            this.markForCheck();
            let results = await Promise.all(promises);

            return results.every((result) => result);
        }

        return true;
    }

    private async refreshWidget(widgetId: string)
    {
        await this.m_widgetContainerLookup[widgetId]?.refreshWidget();
        this.markForCheck();
    }

    public getMinOutline(): Models.WidgetOutline
    {
        const minWidth = this.numCols / DashboardConfigurationExtended.numDashboardColumns;
        return Models.WidgetOutline.newInstance({
                                                    width : Math.max(1, Math.floor(minWidth)),
                                                    height: Math.max(1, Math.round(minWidth * this.colWidth / this.rowHeight))
                                                });
    }

    public getBaseOutline(): Models.WidgetOutline
    {
        return Models.WidgetOutline.newInstance({
                                                    width : Math.max(1, Math.round(WidgetManipulator.BASE_SUBWIDGET_WIDTH / this.colWidth)),
                                                    height: Math.max(1, Math.round(WidgetManipulator.BASE_SUBWIDGET_HEIGHT / this.rowHeight))
                                                });
    }

    //--//

    public static getInnerManipulator(widgetContainer: WidgetContainerComponent): WidgetManipulator
    {
        if (!widgetContainer) return null;

        const manipulators: WidgetManipulator[] = [];
        widgetContainer.widget.collectWidgetManipulators(manipulators);
        return manipulators[0];
    }

    public static getPositionedOutline(outline: Models.WidgetOutline,
                                       grid: WidgetGridManipulator<any>): Models.WidgetOutline
    {
        let res = outline && grid?.findInsertPosition(outline.width, outline.height);
        if (res)
        {
            return Models.WidgetOutline.newInstance({
                                                        left  : res.left,
                                                        top   : res.top,
                                                        width : outline.width,
                                                        height: outline.height
                                                    });
        }

        return null;
    }

    public static generateMinimalWidgetRepresentation(widgetOutlines: Models.WidgetOutline[],
                                                      numRows: number,
                                                      numCols: number): WidgetGridManipulator<boolean>
    {
        return WidgetManipulator.generateWidgetOutlineGrid(widgetOutlines, (outline) => !!outline, numRows, numCols);
    }

    public static generateWidgetRepresentation(widgetOutlineExts: WidgetOutlineExtended[],
                                               numRows: number,
                                               numCols: number): WidgetGridManipulator<string>
    {
        return WidgetManipulator.generateWidgetOutlineGrid(widgetOutlineExts, (outlineExt) => outlineExt.id, numRows, numCols);
    }

    public static generateWidgetOutlineGrid<T extends WidgetOutline, U>(widgetOutlines: T[],
                                                                        mapFn: (outline: T) => U,
                                                                        maxNumRows: number,
                                                                        maxNumCols: number): WidgetGridManipulator<U>
    {
        let grid = new WidgetGridManipulator<U>(maxNumRows || -1, maxNumCols);

        for (let outline of widgetOutlines)
        {
            let maxRow = outline.top + outline.height;
            let maxCol = outline.left + outline.width;
            for (let row = outline.top; row < maxRow; row++)
            {
                for (let col = outline.left; col < maxCol; col++)
                {
                    grid.setEntry(row, col, mapFn(outline));
                }
            }
        }

        return grid;
    }
}

interface WidgetOutline
{
    top: number;
    left: number;
    width: number;
    height: number;
}

class WidgetManipulationBaseState
{
    constructor(public readonly baseOutline: Models.WidgetOutline,
                public readonly baseOutlineDist: Vector2,
                public readonly startDist: Vector2)
    {}
}

export class WidgetOutlineExtended
{
    public static readonly BASE_COLUMNAR_HEIGHT = 5;

    private m_overrideModel: Models.WidgetOutline;
    private m_viewable: boolean;

    public readonly widgetId: string;

    public isPresent = true;
    public hover     = false;

    public get columnar(): boolean
    {
        return this.m_manipulator.columnar;
    }

    public get top(): number
    {
        return this.effectiveModel.top;
    }

    public get left(): number
    {
        return this.effectiveModel.left;
    }

    public get height(): number
    {
        return this.effectiveModel.height;
    }

    public get width(): number
    {
        return this.effectiveModel.width;
    }

    private m_topOverride: number;
    public set topOverride(topOverride: number)
    {
        this.m_topOverride = topOverride;
    }

    private m_heightOverride: number;
    public set heightOverride(heightOverride: number)
    {
        this.m_heightOverride = heightOverride;
    }

    public get activeTop(): number
    {
        if (this.m_topOverride != null) return this.m_topOverride;
        return this.top;
    }

    public columnarHeight: number;
    public columnarTop: number;

    public get activeHeight(): number
    {
        if (this.m_heightOverride != null) return this.m_heightOverride;
        return this.columnar ? this.columnarHeight : this.height;
    }

    public get heightPx(): number
    {
        return this.activeHeight * this.m_manipulator.rowHeight;
    }

    public get widthPx(): number
    {
        return this.computeWidthPx(this.columnar, null);
    }

    public computeWidthPx(columnar: boolean,
                          overrideColWidth: number): number
    {
        const numCols  = columnar ? this.m_manipulator.widgetGridConfig.numCols : this.width;
        const colWidth = overrideColWidth || this.m_manipulator.colWidth;
        return numCols * colWidth;
    }

    public get widthCssPercent(): number
    {
        let decimal = this.columnar ? 1 : this.width / this.m_manipulator.widgetGridConfig.numCols;
        return 100 * decimal;
    }

    public get leftCssPercent(): number
    {
        return 100 * this.left / this.m_manipulator.widgetGridConfig.numCols;
    }

    public get topCssPercent(): number
    {
        return 100 * this.activeTop / this.m_manipulator.numRows;
    }

    public get templateStyles(): Lookup<string>
    {
        return {
            "left"  : this.leftCssPercent + "%",
            "top"   : this.topCssPercent + "%",
            "width" : this.widthCssPercent + "%",
            "height": this.heightPx + "px"
        };
    }

    public get outlineTooltip(): string
    {
        return `${this.width}x${this.height} at (${this.left + 1},${this.top + 1})`;
    }

    public get inView(): boolean
    {
        if (this.m_viewable === undefined) this.updateViewable();
        return this.m_viewable;
    }

    public get id(): string
    {
        return this.config.id;
    }

    public get config(): Models.WidgetConfiguration
    {
        return this.m_composition.config;
    }

    public get effectiveModel(): Models.WidgetOutline
    {
        return this.m_overrideModel || this.m_model;
    }

    private m_model: Models.WidgetOutline;
    public get model(): Models.WidgetOutline
    {
        return this.m_model;
    }

    constructor(private readonly m_manipulator: WidgetManipulator,
                private readonly m_composition: Models.WidgetComposition)
    {
        if (!this.m_composition.outline) this.m_composition.outline = new Models.WidgetOutline();

        this.m_model  = this.m_composition.outline;
        this.widgetId = this.m_composition.config.id;

        this.updateViewable();
    }

    public updateViewable(): boolean
    {
        let prev        = this.m_viewable;
        this.m_viewable = this.m_manipulator.isWidgetViewable(this);
        return prev !== this.m_viewable;
    }

    public updateOverride(model: Models.WidgetOutline = this.m_model): void
    {
        this.m_overrideModel = Models.WidgetOutline.newInstance(model);
    }

    public updateModel(): boolean
    {
        if (!this.m_overrideModel) return false;

        let changed = !UtilsService.compareJson(this.m_model, this.m_overrideModel);
        if (changed)
        {
            this.m_model               = this.m_overrideModel;
            this.m_composition.outline = this.m_model;
            changed                    = true;
        }
        this.m_overrideModel = undefined;

        return changed;
    }

    public modelCopy(active: boolean): Models.WidgetOutline
    {
        let model = active ? this.effectiveModel : this.m_model;
        return Models.WidgetOutline.deepClone(model);
    }

    public getTransferOutline(manipulator: WidgetManipulator): Models.WidgetOutline
    {
        let targetWidth         = this.width / this.m_manipulator.numCols * manipulator.numCols;
        const targetScaleFactor = (targetWidth * manipulator.colWidth) / (this.width * this.m_manipulator.colWidth);
        const maxScaleFactor    = 3;
        if (targetScaleFactor > maxScaleFactor) targetWidth *= maxScaleFactor / targetScaleFactor;

        const width   = UtilsService.clamp(manipulator.manipulationMinOutline.width, manipulator.numCols, Math.floor(targetWidth));
        const widthPx = width * manipulator.colWidth;
        const height  = UtilsService.clamp(manipulator.manipulationMinOutline.height,
                                           manipulator.widgetGridConfig.numRows || Number.MAX_SAFE_INTEGER,
                                           Math.round(widthPx * manipulator.activeManipulation.baseAspectRatio / manipulator.rowHeight));
        return Models.WidgetOutline.newInstance({
                                                    width : width,
                                                    height: height
                                                });
    }

    public static getManipulationOutline(baseOutline: Models.WidgetOutline,
                                         manipulationVector: Vector2,
                                         resize: ResizePoint): Models.WidgetOutline
    {
        let outline = Models.WidgetOutline.newInstance(baseOutline);

        // default minimum widget dimensions
        let minWidth  = 1;
        let minHeight = 1;

        if (resize)
        {
            if (resize.indexOf("left") >= 0)
            {
                if (manipulationVector.x > 0) manipulationVector.x = Math.min(manipulationVector.x, outline.width - minWidth);
                outline.left += manipulationVector.x;
                outline.width -= manipulationVector.x;
            }
            else if (resize.indexOf("right") >= 0)
            {
                if (manipulationVector.x < 0) manipulationVector.x = Math.max(manipulationVector.x, minWidth - outline.width);
                outline.width += manipulationVector.x;
            }

            if (resize.indexOf("top") >= 0)
            {
                if (manipulationVector.y > 0) manipulationVector.y = Math.min(manipulationVector.y, outline.height - minHeight);
                outline.top += manipulationVector.y;
                outline.height -= manipulationVector.y;
            }
            else if (resize.indexOf("bot") >= 0)
            {
                if (manipulationVector.y < 0) manipulationVector.y = Math.max(manipulationVector.y, minHeight - outline.height);
                outline.height += manipulationVector.y;
            }
        }
        else
        {
            outline.left += manipulationVector.x;
            outline.top += manipulationVector.y;
        }

        return outline;
    }

    public static isIntersection(model: Models.WidgetOutline,
                                 row: number,
                                 col: number): boolean
    {
        return model.top <= row && model.top + model.height > row &&
               model.left <= col && model.left + model.width > col;
    }

    public static isValid(model: Models.WidgetOutline,
                          minModel: Models.WidgetOutline,
                          maxNumRows: number,
                          maxNumCols: number): boolean
    {
        if (!model) return false;
        if (model.top < 0 || model.left < 0) return false;

        if (minModel)
        {
            if (model.width < minModel.width || model.height < minModel.height) return false;
        }
        else if (model.width < 1 || model.height < 1) return false;

        if (maxNumCols && model.left + model.width > maxNumCols) return false;
        if (maxNumRows && model.top + model.height > maxNumRows) return false;

        return true;
    }
}

export class WidgetLayoutConfig
{
    constructor(public readonly numCols: number,
                public readonly numRows?: number,
                public readonly rowHeight?: number,
                public readonly baseFontScalar: number = 1)
    {}
}

class WidgetIdentifier
{
    private m_changeId: number                           = 0;
    private readonly m_widgetToChangeId?: Lookup<number> = {};

    private m_currConfigs: Models.WidgetConfiguration[] = [];

    constructor(public readonly layoutId: string)
    {}

    getIdentifier(widgetId: string): string
    {
        return this.layoutId + widgetId + this.m_widgetToChangeId[widgetId];
    }

    updateIdentifiers(widgets: Models.WidgetComposition[])
    {
        let prevConfigs: Lookup<Models.WidgetConfiguration> = {};
        for (let config of this.m_currConfigs) prevConfigs[config.id] = config;

        // only care about configs that are in both layouts because these are the ones with the same trackBy id -> won't respect config changes
        let changedWidgetIds: string[] = [];
        for (let widget of widgets)
        {
            let widgetId   = widget.config.id;
            let prevConfig = prevConfigs[widgetId];
            if (prevConfig)
            {
                let nextConfig  = Models.WidgetConfiguration.deepClone(widget.config);
                prevConfig.name = nextConfig.name = null;

                if (!UtilsService.compareJson(prevConfig, nextConfig)) changedWidgetIds.push(widgetId);
            }
        }

        for (let changedWidgetId of changedWidgetIds)
        {
            // changes identifier (and trackBy id) which triggers widget rebuild
            this.m_widgetToChangeId[changedWidgetId] = this.m_changeId;
        }

        this.m_currConfigs = widgets.map((widget) => Models.WidgetConfiguration.deepClone(widget.config));
        this.m_changeId++;
    }
}

export enum ResizePoint
{
    Left     = "left",
    BotLeft  = "bot left",
    Bot      = "bot",
    BotRight = "bot right",
    Right    = "right",
    TopRight = "top right",
    Top      = "top",
    TopLeft  = "top left"
}

const ResizePoints = [
    ResizePoint.Left,
    ResizePoint.BotLeft,
    ResizePoint.Bot,
    ResizePoint.BotRight,
    ResizePoint.Right,
    ResizePoint.TopRight,
    ResizePoint.Top,
    ResizePoint.TopLeft
] as const;
