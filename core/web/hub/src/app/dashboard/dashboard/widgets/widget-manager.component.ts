import {CdkDrag, CdkDragEnd, CdkDropList} from "@angular/cdk/drag-drop";
import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, QueryList, ViewChildren} from "@angular/core";
import {MatTooltip} from "@angular/material/tooltip/tooltip";

import {WidgetContainerComponent} from "app/dashboard/dashboard/widgets/widget-container.component";
import {ResizePoint, WidgetManipulator, WidgetOutlineExtended} from "app/dashboard/dashboard/widgets/widget-manipulator";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WidgetGraph} from "app/services/domain/dashboard-management.service";
import * as Models from "app/services/proxy/model/models";

import {Vector2} from "framework/ui/charting/charting-math";

@Component({
               selector       : "o3-widget-manager[manipulator]",
               templateUrl    : "./widget-manager.component.html",
               styleUrls      : ["./widget-manager.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class WidgetManagerComponent extends BaseApplicationComponent
{
    private m_manipulator: WidgetManipulator;
    @Input() set manipulator(manipulator: WidgetManipulator)
    {
        if (this.m_manipulator !== manipulator)
        {
            this.m_manipulator = manipulator;
            this.setUpManipulator();
        }
    }

    get manipulator(): WidgetManipulator
    {
        return this.m_manipulator;
    }

    private m_widgetCursorOffset: Vector2;
    private m_widgetPreviewDimensions: Vector2;

    private m_widgets: Models.WidgetComposition[];
    @Input() set widgets(widgets: Models.WidgetComposition[])
    {
        if (widgets)
        {
            this.m_widgets = widgets;
            this.resetManipulator();
        }
    }

    @Input() disableEditing   = false;
    @Input() preview          = false;
    @Input() singularWidget   = false;
    @Input() useWidgetPadding = true;
    @Input() alwaysShowGrid   = false;
    @Input() alwaysShowShadow = true;
    @Input() nested           = false;
    @Input() widgetPrimaryColor: string;
    @Input() widgetSecondaryColor: string;

    get showGrid(): boolean
    {
        if (this.alwaysShowGrid) return true;
        if (!this.editing) return false;
        if (this.columnarLayout) return false;
        if (this.cellDimensionSmallerThan(WidgetManipulator.MIN_CELL_DIMENSION)) return false;

        return true;
    }

    get disableWidgetTooltips(): boolean
    {
        if (!this.editing) return true;
        if (!this.cellDimensionSmallerThan(WidgetManipulator.CELL_DIMENSION_TOOLTIP_THRESHOLD)) return true;

        return false;
    }

    private cellDimensionSmallerThan(dimension: number)
    {
        const elem = this.m_element.nativeElement;
        return elem.clientHeight < this.m_manipulator.numRows * dimension ||
               elem.clientWidth < this.m_manipulator.numCols * dimension;
    }

    get canManipulate(): boolean
    {
        return !this.columnarLayout && !this.disableEditing;
    }

    private m_queriedWidgets: QueryList<WidgetContainerComponent>;

    @ViewChildren(WidgetContainerComponent) set widgetComponents(widgetComponents: QueryList<WidgetContainerComponent>)
    {
        if (widgetComponents)
        {
            this.m_queriedWidgets = widgetComponents;
            if (this.m_manipulator) this.m_manipulator.widgetContainers = this.m_queriedWidgets.toArray();
            this.widgetsLoaded.emit();
        }
    }

    @Output() gridUpdated       = new EventEmitter<void>();
    @Output() configChange      = new EventEmitter<string>();
    @Output() widgetsLoaded     = new EventEmitter<void>();
    @Output() widgetDelete      = new EventEmitter<WidgetGraph>();
    @Output() widgetEdit        = new EventEmitter<EditWidgetRequest>();
    @Output() cursorClassChange = new EventEmitter<string[]>();

    get columnarLayout(): boolean
    {
        return this.m_manipulator?.columnar;
    }

    get editing(): boolean
    {
        return this.m_manipulator?.editing;
    }

    get resizing(): boolean
    {
        return this.m_manipulator?.resizing;
    }

    get dragging(): boolean
    {
        return this.m_manipulator?.dragging;
    }

    private static readonly WIDGET_ENTERABLE_DELAY: number                 = 500;
    private static readonly WIDGET_CAN_ENTER_FN: CdkDropListEnterPredicate = (drag: CdkDrag,
                                                                              drop: CdkDropList<WidgetManipulator>) =>
    {
        // no drag data means it's a resize: don't allow moving between manipulators
        if (!drag.data) return false;

        // don't allow nested grouping widgets
        if (WidgetManipulator.getInnerManipulator(drop.data.rootManipulator.getWidgetContainer(drag.data))) return false;

        // at onset of a cdkDrag, the connected lists are checked to see if enterPredicate evaluates to true
        // if not, they are never considered again during that drag even if predicate outcome changes (not in docs so subject to change)
        if (drop.data.isSetUpForDrag)
        {
            return drop.data.dragEnterableDuration > WidgetManagerComponent.WIDGET_ENTERABLE_DELAY;
        }
        else
        {
            drop.data.setUpForDrag();
            return true;
        }
    };

    get widgetCanEnter(): CdkDropListEnterPredicate
    {
        return WidgetManagerComponent.WIDGET_CAN_ENTER_FN;
    }

    get gridHeightCss(): string
    {
        let gridHeight = this.m_manipulator?.gridHeightPx;
        return isNaN(gridHeight) ? undefined : gridHeight + "px";
    }

    private m_resizeClasses: string[];
    set resizeClasses(classes: string[])
    {
        this.m_resizeClasses = classes;
        this.cursorClassChange.emit(classes);
    }

    get resizeClasses(): string[]
    {
        return this.m_resizeClasses;
    }

    private m_resizeCompleting = false;

    widgetIdentifierFn: (idx: number,
                         item: WidgetOutlineExtended) => string;

    private m_focusClick: MouseEvent;
    hoveringWidget: string;
    listeningForFocusClear = false;

    get element(): ElementRef<HTMLElement>
    {
        return this.m_element;
    }

    constructor(inj: Injector,
                private m_element: ElementRef<HTMLElement>)
    {
        super(inj);
    }

    private setUpManipulator()
    {
        if (this.m_manipulator)
        {
            this.m_manipulator.setup(this);
            this.resetManipulator();
            this.m_manipulator.updateViewport(true);
            this.widgetIdentifierFn = this.m_manipulator.identifyWidget.bind(this.m_manipulator);
        }
    }

    private resetManipulator()
    {
        if (this.m_manipulator?.initialized && this.m_widgets)
        {
            this.m_manipulator.reset(this.m_widgets);
            this.gridUpdated.emit();
            this.markForCheck();
        }
    }

    //--//

    public handleWidgetHovered(hoveringWidget: string,
                               tooltip: MatTooltip,
                               hovering: boolean)
    {
        if (hovering)
        {
            this.hoveringWidget = hoveringWidget;
        }
        else
        {
            if (hoveringWidget && tooltip) tooltip.hide();
            if (!hoveringWidget || this.hoveringWidget === hoveringWidget) this.hoveringWidget = null;
        }
    }

    public isDragging(widgetOutline: WidgetOutlineExtended): boolean
    {
        if (!this.dragging) return false;
        return this.m_manipulator.manipulationId === widgetOutline.id;
    }

    public isResizing(widgetOutline: WidgetOutlineExtended): boolean
    {
        if (!this.resizing) return false;
        return this.m_manipulator.manipulationId === widgetOutline.id;
    }

    public setDragStart(widgetContainer: WidgetContainerComponent,
                        event: MouseEvent)
    {
        this.m_manipulator.rootManipulator.clearDomRects();

        const widgetRect               = widgetContainer.element.nativeElement.getBoundingClientRect();
        this.m_widgetCursorOffset      = new Vector2(event.clientX - widgetRect.left, event.clientY - widgetRect.top);
        this.m_widgetPreviewDimensions = new Vector2(widgetRect.width, widgetRect.height);

        this.m_manipulator.dragStarting();

        event.stopPropagation();
    }

    public manipulationPrep(widgetId: string,
                            resize?: ResizePoint)
    {
        this.handleWidgetHovered(null, null, null);

        if (this.m_manipulator?.initializeNewWidgetManipulation(widgetId, resize, this.m_widgetCursorOffset, this.m_widgetPreviewDimensions) && resize)
        {
            let resizeClasses = resize.split(" ")
                                      .map((resizePoint) => "widget-resize-" + resizePoint);
            resizeClasses.push("resizing");
            this.resizeClasses = resizeClasses;
        }

        this.m_widgetCursorOffset      = null;
        this.m_widgetPreviewDimensions = null;
    }

    public completeDrag(widgetId: string)
    {
        const widgetOutlineExt = this.manipulator.widgetOutlineExtLookup[widgetId];
        const idx              = this.m_widgets.findIndex((widget) => widget.config.id === widgetId);
        if (widgetOutlineExt?.isPresent)
        {
            if (idx === -1)
            {
                // transferred in
                this.m_widgets.push(Models.WidgetComposition.newInstance({
                                                                             config : widgetOutlineExt.config,
                                                                             outline: widgetOutlineExt.effectiveModel
                                                                         }));
            }
        }
        else if (idx >= 0)
        {
            this.m_widgets.splice(idx, 1);
        }

        this.manipulator.reset(this.m_widgets);
    }

    public completeManipulation(dragEnd: CdkDragEnd)
    {
        let wasResize = this.m_manipulator.resizing;
        if (this.m_manipulator.completeManipulation(dragEnd))
        {
            this.configChange.emit(`${wasResize ? "resize" : "move"} widget`);
            this.gridUpdated.emit();
        }

        if (wasResize)
        {
            this.m_resizeCompleting = true;
            setTimeout(() => this.m_resizeCompleting = false);
        }

        this.resizeClasses = [];
    }

    //--//

    public clearFocusWidget(event: MouseEvent): void
    {
        if (this.m_manipulator && !this.m_resizeCompleting && this.m_focusClick !== event && this.listeningForFocusClear)
        {
            if (!this.m_manipulator.resizing && !this.m_manipulator.dragging && this.m_manipulator.focusId != undefined)
            {
                this.updateFocusWidget(undefined);
            }
            this.listeningForFocusClear = false;
        }
    }

    public updateFocusWidget(widgetId: string,
                             event?: MouseEvent): void
    {
        this.m_focusClick          = event;
        this.m_manipulator.focusId = widgetId;
    }

    public relayWidgetEdit(editRequest: EditWidgetRequest,
                           widgetId: string)
    {
        this.widgetEdit.emit(editRequest);
        this.updateFocusWidget(widgetId);
    }
}

export class EditWidgetRequest
{
    constructor(public readonly editConfig?: Models.WidgetComposition,
                public readonly parentConfig?: Models.GroupingWidgetConfiguration)
    {}
}

type CdkDropListEnterPredicate = (drag: CdkDrag,
                                  drop: CdkDropList) => boolean
