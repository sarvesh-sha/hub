import {animate, state, style, transition, trigger} from "@angular/animations";
import {CDK_DRAG_CONFIG, CdkDragEnd} from "@angular/cdk/drag-drop";
import {Point} from "@angular/cdk/drag-drop/drag-ref";
import {Component, ContentChild, ContentChildren, Directive, ElementRef, EventEmitter, HostListener, Injector, Input, Optional, Output, QueryList, TemplateRef, ViewChild, ViewChildren} from "@angular/core";
import {MatPaginator, PageEvent} from "@angular/material/paginator";
import {MatSort, Sort, SortDirection} from "@angular/material/sort";
import {MatCell, MatHeaderCell, MatTable, MatTableDataSource} from "@angular/material/table";
import {UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";

import {ContextMenu, ContextMenuItemComponent} from "framework/ui/context-menu/context-menu.component";
import {DatatableColumnManagerComponent} from "framework/ui/datatables/datatable-column-manager.component";
import {ColumnConfiguration, DatatableManager, DatatableSort} from "framework/ui/datatables/datatable-manager";
import {Future} from "framework/utils/concurrency";
import {Subject, Subscription} from "rxjs";
import {debounceTime} from "rxjs/operators";

@Directive({
               selector: "[o3-datatable-cell-template],[o3DatatableCellTemplate]"
           })
export class DatatableCellTemplateDirective
{
    constructor(public templateRef: TemplateRef<any>) { }
}

@Directive({
               selector: "[o3-datatable-header-cell-template]"
           })
export class DatatableHeaderCellTemplateDirective
{
    constructor(public templateRef: TemplateRef<any>) { }
}

@Directive({
               selector: "[o3DatatableCellSuffix]"
           })
export class DatatableCellSuffixTemplateDirective
{
    constructor(public templateRef: TemplateRef<any>) { }

    @Input("o3DatatableCellSuffixHoverOnly") hoverOnly: boolean;
    @Input("o3DatatableCellSuffixHide") hide: boolean;
}

@Directive({
               selector: "[o3-datatable-details-template], [o3DatatableDetailsTemplate]",
               exportAs: "o3DatatableDetails"
           })
export class DatatableDetailsTemplateDirective
{
    constructor(public templateRef: TemplateRef<{ row: any; $implicit: any; }>,
                @Optional() public column: DatatableColumnDirective)
    { }
}

@Directive({
               selector: "[o3DatatableContextMenuTrigger]"
           })
export class DatatableContextMenuTriggerDirective<T>
{
    constructor(public column: DatatableColumnDirective,
                public table: DatatableComponent<any, any, T>)
    {

    }

    @Input("o3DatatableContextMenuTrigger") row: T;

    @HostListener("click", ["$event"])
    public onClick(event: MouseEvent)
    {
        if (this.row) this.table.onTableContextMenu(event, this.row, this.column.id);
    }
}


@Directive({
               selector: "o3-datatable-column[id]"
           })
export class DatatableColumnDirective
{
    @ContentChild(DatatableCellTemplateDirective, {static: true}) template: DatatableCellTemplateDirective;

    @ContentChild(DatatableHeaderCellTemplateDirective, {static: true}) headerTemplate: DatatableCellTemplateDirective;

    @ContentChildren(DatatableCellSuffixTemplateDirective) suffixes: QueryList<DatatableCellSuffixTemplateDirective>;

    @Input() id: string;

    @Input() enabled: boolean = true;

    @Input() name: string;

    @Input() prop: string;

    @Input() secondaryProp: string;

    @Input() resizable: boolean = true;

    @Input() width: number;

    @Input() hideTooltip: boolean = false;

    @Input() tooltip: string;

    private m_minWidth: number = 100;
    @Input() set minWidth(minWidth: number)
    {
        this.m_minWidth = minWidth;
    }

    get minWidth(): number
    {
        return !this.maxWidth || this.maxWidth >= this.m_minWidth ? this.m_minWidth : this.maxWidth;
    }

    @Input() maxWidth: number;

    @Input() grow: number = 1;

    @Input() shrink: number = 1;

    @Input() sortId: string;

    @Input() sortStart: "asc" | "desc" = "asc";

    get sortable(): boolean
    {
        return !!this.sortId;
    }

    @Input() formatString: string = null;

    @Input() formatCallback: (val: string) => string = null;

    @Input() format: ColumnFormat = ColumnFormat.VALUE;

    get isValue(): boolean { return !this.isFormat && !this.isTemplate && this.format == ColumnFormat.VALUE; }

    get isNumber(): boolean { return !this.isFormat && this.format == ColumnFormat.NUMBER; }

    get isPercent(): boolean { return !this.isFormat && this.format == ColumnFormat.PERCENT; }

    get isDate(): boolean { return !this.isFormat && this.format == ColumnFormat.DATE; }

    get isDateLong(): boolean { return !this.isFormat && this.format == ColumnFormat.DATELONG; }

    get isYesNo(): boolean { return !this.isFormat && this.format == ColumnFormat.YESNO; }

    get isTemplate(): boolean { return !this.isFormat && !!this.template; }

    get isFormat(): boolean { return this.formatString != null || this.formatCallback != null; }

    getFormattedValue(value: string): string
    {
        if (this.formatCallback != null)
        {
            return this.formatCallback(value);
        }
        else if (this.formatString != null && this.formatString.indexOf("{value}") > -1)
        {
            return this.formatString.replace("{value}", value);
        }

        return value;
    }

    get widthCss(): string
    {
        return UtilsService.formatCssValue(this.width);
    }

    get maxWidthCss(): string
    {
        return UtilsService.formatCssValue(this.maxWidth);
    }

    get minWidthCss(): string
    {
        return UtilsService.formatCssValue(this.minWidth);
    }
}

@Component({
               selector   : "o3-datatable",
               templateUrl: "./datatable.component.html",
               animations : [
                   trigger("detailExpand", [
                       state("*", style({
                                            height   : "0",
                                            minHeight: "0"
                                        })),
                       state("expanded", style({height: "*"})),
                       transition("* <=> *", animate("225ms cubic-bezier(0.4, 0.0, 0.2, 1)"))
                   ])
               ],
               providers  : [
                   {
                       provide : CDK_DRAG_CONFIG,
                       useValue: {
                           dragStartThreshold             : 0,
                           pointerDirectionChangeThreshold: 5
                       }
                   }
               ]
           })
export class DatatableComponent<K, E, T> extends BaseComponent
{
    public get resizing(): boolean
    {
        return this.datatableConfiguration && this.datatableConfiguration.resizing;
    }

    public get hasResized(): boolean
    {
        return this.datatableConfiguration && this.datatableConfiguration.hasResized;
    }

    public columns: DatatableColumnDirective[];
    public sortSettings: { [columnId: string]: DatatableSort[] };
    public defaultSort: string                 = "";
    public defaultSortDirection: SortDirection = "";
    public activeSort: string                  = "";
    public activeSortDirection: SortDirection  = "";

    private resizingReady: boolean                         = false;
    private datatableConfiguration: DatatableConfiguration = new DatatableConfiguration([], []);

    private detailsFromContent: DatatableDetailsTemplateDirective[] = [];
    private detailsFromInput: DatatableDetailsTemplateDirective;

    public mouseOverHeader: boolean;
    private mouseOverTimer: number;

    @Input() constantRowLayout: boolean = false;

    @Input() showRowNumbers: boolean = true;

    @Input() showViewIcons: boolean = true;

    @Input() showHeader: boolean = true;

    @Input() showFooter: boolean = true;

    @Input() useExpandToggle: boolean = true;

    @Input() showConfig: boolean = true;

    @Input() enableLimit: boolean = true;

    @Input() clickableRows: boolean = false;

    @Input() resizable: boolean = true;

    @Input() count: number = 0;

    @Input() offset: number = 0;

    @Input() limit: number;

    @Input() loadingIndicator: boolean = false;

    @Input() messages: { [key: string]: string } = {
        emptyMessage  : "No data",
        totalMessage  : "total",
        warningMessage: null
    };

    @Input() warningColor: string = null;

    @Input() sorts: DatatableSort[];

    @Input() tableClass: string;

    @Input() rowComparator: (a: T,
                             b: T) => boolean = this.defaultComparator;

    @Output() activate: EventEmitter<DatatableRowActivateEvent<T>> = new EventEmitter<DatatableRowActivateEvent<T>>();

    @Output() page: EventEmitter<PageEvent> = new EventEmitter<PageEvent>();

    @Output() sort: EventEmitter<DatatableSort[]> = new EventEmitter<DatatableSort[]>();

    @Output() onLimitSet: EventEmitter<number> = new EventEmitter<number>();

    private m_changeDetectSub: Subscription;
    private m_table: DatatableManager<K, E, T>;

    @Input()
    public set table(table: DatatableManager<K, E, T>)
    {
        this.m_table = table;

        if (this.m_changeDetectSub)
        {
            this.m_changeDetectSub.unsubscribe();
            this.m_changeDetectSub = null;
        }

        if (table)
        {
            this.m_changeDetectSub = table.detectChanges.subscribe(() =>
                                                                   {
                                                                       this.detectChanges();
                                                                   });

            let oldSort = table.sort;
            if (!oldSort || oldSort.length == 0)
            {
                this.onSort({
                                active   : this.defaultSort,
                                direction: this.defaultSortDirection
                            });
            }
            else
            {
                this.activeSort = oldSort[0].prop;
                this.activeSortDirection = oldSort[0].dir;
            }
        }
    }

    public get table(): DatatableManager<K, E, T>
    {
        return this.m_table;
    }

    @Output() onShowingContextMenu: EventEmitter<DatatableContextMenuEvent<T>> = new EventEmitter<DatatableContextMenuEvent<T>>();

    @Output() onShowingHeaderContextMenu: EventEmitter<DatatableContextMenuEvent<T>> = new EventEmitter<DatatableContextMenuEvent<T>>();

    @ContentChildren(DatatableColumnDirective, {
        descendants: false,
        read       : DatatableColumnDirective
    })
    public set columnTemplates(columnTemplates: QueryList<DatatableColumnDirective>)
    {
        this.generateColumnConfigs(columnTemplates.toArray());
    }

    private gripperUpdateDebouncer = new Subject<void>();

    private m_matTable: ElementRef;

    @ViewChild(MatTable, {read: ElementRef})
    private set tableElem(table: ElementRef)
    {
        if (table)
        {
            this.m_matTable = table;
            this.gripperUpdateDebouncer.next();
        }
    }

    private m_headers: QueryList<ElementRef>;
    @ViewChildren(MatHeaderCell, {read: ElementRef})
    private set headers(headers: QueryList<ElementRef>)
    {
        if (headers)
        {
            this.m_headers = headers;
            this.gripperUpdateDebouncer.next();
        }
    }

    private m_cells: QueryList<ElementRef>;
    @ViewChildren(MatCell, {read: ElementRef})
    private set cells(cells: QueryList<ElementRef>)
    {
        if (cells)
        {
            this.m_cells = cells;
            this.gripperUpdateDebouncer.next();
        }
    }

    get hasScrollbar(): boolean
    {
        if (!this.m_matTable) return false;

        return this.m_matTable.nativeElement.scrollHeight > this.m_matTable.nativeElement.clientHeight;
    }

    get showGrippers(): boolean
    {
        return this.mouseOverHeader || this.resizing;
    }

    private generateResizeInfo()
    {
        if (!this.datatableConfiguration.hasColumnIds) return;

        this.datatableConfiguration.headers = this.m_headers;
        this.datatableConfiguration.table   = this.m_matTable?.nativeElement;

        this.resizingReady = this.datatableConfiguration.setUpResizeInfo(this.m_cells);
        this.detectChanges();
    }

    public setCursorDragStart(event: MouseEvent)
    {
        this.datatableConfiguration.setupForDrag(event);
        event.preventDefault();
    }

    public resizePrep(column: DatatableColumnDirective): void
    {
        this.datatableConfiguration.resizePrep(column.id);
        this.detectChanges();
    }

    public resizeComplete(event: CdkDragEnd)
    {
        event.source.reset(); // remove transform style on element being dragged
        this.datatableConfiguration.completeResize();

        this.generateResizeInfo();
    }

    public showExpandToggle(): boolean
    {
        return this.hasDetails && this.useExpandToggle;
    }

    public hasGripper(column: DatatableColumnDirective): boolean
    {
        return column.resizable && this.toConfig(column).hasGripper;
    }

    public getFlexBasis(column: DatatableColumnDirective): string
    {
        return UtilsService.formatCssValue(this.toConfig(column).flexBasis);
    }

    public get resizeFn(): (point: Point) => Point
    {
        return this.datatableConfiguration.resizeConstrain.bind(this.datatableConfiguration);
    }

    public setMouseoverHeader(mouseover: boolean)
    {
        // necessary to avoid flickering
        if (mouseover)
        {
            if (this.mouseOverTimer)
            {
                clearTimeout(this.mouseOverTimer);
                this.mouseOverTimer = null;
            }

            if (!this.resizingReady) this.generateResizeInfo();

            this.mouseOverHeader = true;
        }
        else
        {
            this.mouseOverTimer = setTimeout(() =>
                                             {
                                                 this.mouseOverHeader = false;
                                                 this.datatableConfiguration.clearStaleInfo();
                                                 this.mouseOverTimer = null;
                                             }, 0);
        }
    }


    private toConfig(column: DatatableColumnDirective): ColumnConfiguration
    {
        return this.datatableConfiguration?.getColumnConfig(column.id);
    }

    private async generateColumnConfigs(columnTemplates: DatatableColumnDirective[])
    {
        let columnConfigs: ColumnConfiguration[];
        if (this.m_table) columnConfigs = await this.m_table.getConfigs();

        this.datatableConfiguration = new DatatableConfiguration(columnTemplates, columnConfigs);
        this.updateColumns();
        this.generateResizeInfo();
    }

    public processColumnChanges(updatedConfigs: ColumnConfiguration[])
    {
        let updated = this.table.setConfigs(updatedConfigs); // currently no indication in case of failure exists
        this.datatableConfiguration.setColumnConfigs(updatedConfigs);
        this.columnEditComponent.isDefault = this.datatableConfiguration.isDefault;
        this.updateColumns();
        setTimeout(() => this.generateResizeInfo(), 0); // let view changes render
    }

    private updateColumns(): void
    {
        this.columns = this.datatableConfiguration.getColumnTemplates();

        let sortSettings: { [key: string]: DatatableSort[] } = {};

        for (let column of this.columns || [])
        {
            if (column.sortable)
            {
                let parts = column.sortId.split(",");

                let primary = false;

                let sorts = parts.map((part) =>
                                      {
                                          let res = new DatatableSort();
                                          if (part.startsWith("!"))
                                          {
                                              primary = true;
                                              part    = part.substring(1);
                                          }

                                          if (part.startsWith("-"))
                                          {
                                              res.dir = "desc";
                                              part    = part.substring(1);
                                          }
                                          else
                                          {
                                              res.dir = "asc";
                                          }

                                          res.prop = part;
                                          return res;
                                      });

                sortSettings[column.id] = sorts;

                if (primary)
                {
                    this.defaultSort          = column.id;
                    this.defaultSortDirection = column.sortStart;
                }
            }
        }

        this.sortSettings = sortSettings;

        if (this.m_table && !this.m_table.sort && this.defaultSort)
        {
            this.onSort({
                            active   : this.defaultSort,
                            direction: this.defaultSortDirection
                        });
        }

        this.datatableConfiguration.columnIds = this.columnIds;

        this.detectChanges();
    }

    @ContentChildren(DatatableDetailsTemplateDirective, {descendants: true})
    public set detailsContent(details: QueryList<DatatableDetailsTemplateDirective>)
    {
        this.detailsFromContent = details.toArray();
    }

    @Input("details")
    public set detailsInput(details: DatatableDetailsTemplateDirective)
    {
        this.detailsFromInput = details;
    }

    public get details(): DatatableDetailsTemplateDirective
    {
        let details = this.tableLevelDetails;
        if (this.lastClickedColumnId)
        {
            for (let columnDetails of this.detailsFromContent)
            {
                if (columnDetails.column && columnDetails.column.id === this.lastClickedColumnId)
                {
                    details = columnDetails;
                    break;
                }
            }
        }

        return details;
    }

    private get tableLevelDetails()
    {
        return this.detailsFromInput || this.detailsFromContent.filter((details) => !details.column)[0];
    }

    tableDataSource: MatTableDataSource<T>;

    @Input()
    public set rows(rows: T[])
    {
        if (!this.tableDataSource)
        {
            this.tableDataSource = new MatTableDataSource<T>(rows || []);
        }
        else
        {
            this.tableDataSource.data = rows || [];
        }
    }

    public get columnIds(): string[]
    {
        let columns = this.columns.filter((column: DatatableColumnDirective) => this.toConfig(column).enabled)
                          .map((column) => column.id);
        if (this.showViewIcons)
        {
            columns.push("viewIcon");
        }
        if (this.table?.selectionManager)
        {
            columns.unshift("selection");
        }
        if (this.showRowNumbers)
        {
            columns.unshift("rowNumber");
        }
        if (this.showExpandToggle() && this.table?.expansionManager)
        {
            columns.unshift("expandToggle");
        }
        return columns;
    }

    public tableConfigureToggle()
    {
        if (this.columnEditComponent.isOpen)
        {
            this.columnEditComponent.close();
        }
        else
        {
            this.columnEditComponent.open(this.datatableConfiguration);
        }
    }

    public getValue(row: any           = {},
                    column: DatatableColumnDirective,
                    secondary: boolean = false): any
    {
        let prop = secondary ? column.secondaryProp : column.prop;
        if (!prop) return undefined;

        return this.getValueInner(row, prop);
    }

    private sortDataAccessor(row: any = {},
                             id: string)
    {
        let template = this.datatableConfiguration.getColumnTemplate(id);
        if (!template || !template.prop) return undefined;

        let prop = template.prop;
        return this.getValueInner(row, prop);
    }

    private getValueInner(row: any = {},
                          prop: string)
    {

        let path = prop.split(".")
                       .filter((part) => !!part);
        if (path.length == 0)
        {
            return undefined;
        }

        let part: string;

        while (path.length > 0)
        {
            if (!row || typeof row !== "object")
            {
                return undefined;
            }

            part = path.shift();
            row  = row[part];
        }

        return row;
    }

    @ViewChild("contextMenu", {static: true}) contextMenu: ContextMenu;

    @ViewChild("contextMenuTriggerWrapper", {
        read  : ElementRef,
        static: true
    }) contextMenuTriggerWrapper: ElementRef;

    contextMenuVisibleItems: ContextMenuItemComponent[] = [];

    @ViewChild(MatPaginator) set paginator(paginator: MatPaginator)
    {
        if (!this.externalPaging && this.tableDataSource) this.tableDataSource.paginator = paginator;
    }

    @ViewChild(MatSort) set sorter(sorter: MatSort)
    {
        if (!this.externalPaging && this.tableDataSource) this.tableDataSource.sort = sorter;
    }

    @ViewChild(DatatableColumnManagerComponent, {static: true}) columnEditComponent: DatatableColumnManagerComponent;

    lastClickedColumnId: string;

    public getDataSource(): T[] | MatTableDataSource<T>
    {
        if (this.table) return this.table.rows;

        return this.tableDataSource;
    }

    public getRows(): T[]
    {
        if (this.table) return this.table.rows;

        return this.tableDataSource.data;
    }

    public getCount(): number
    {
        if (this.table) return this.table.count;
        if (this.tableDataSource) return Math.max(this.tableDataSource.data.length, this.count);

        return this.count;
    }

    public getOffset(): number
    {
        if (this.table) return this.table.offset;

        return this.offset;
    }

    public getLimit(): number
    {
        if (this.table) return this.table.limit;

        return this.limit;
    }

    public getSorts(): DatatableSort[]
    {
        if (this.table) return this.table.sort;

        return this.sorts;
    }

    public getLoadingIndicator(): boolean
    {
        if (this.loadingIndicator) return this.loadingIndicator;
        if (this.table) return this.table.loading;

        return false;
    }

    public getMessages(): { [key: string]: string }
    {
        if (this.table) return this.table.config.messages;

        return this.messages ? this.messages : {};
    }

    hasMessage(message: "emptyMessage" | "totalMessage" | "warningMessage"): boolean
    {
        if (this.table && this.table.config && this.table.config.messages) return !!this.table.config.messages[message];
        if (this.messages) return !!this.messages[message];
        return false;
    }


    private m_externalPaging: boolean = false;
    @Input()
    public set externalPaging(externalPaging: boolean)
    {
        this.m_externalPaging = externalPaging;
    }

    public get externalPaging(): boolean
    {
        return this.m_externalPaging || this.table != null;
    }

    public get externalSorting(): boolean
    {
        return this.table != null;
    }

    public get hasDetails(): boolean
    {
        return this.detailsFromContent.length > 0 || !!this.detailsFromInput;
    }

    public get hasTableDetailsOnly(): boolean
    {
        return !!this.detailsFromInput || this.detailsFromContent.length === 1 && !this.detailsFromContent[0].column;
    }

    constructor(inj: Injector,
                private element: ElementRef)
    {
        super(inj);

        this.subscribeToObservable(this.gripperUpdateDebouncer.pipe(debounceTime(250)), () =>
        {
            if (this.m_matTable && this.m_headers && this.m_cells) this.generateResizeInfo();
        });
    }

    ngOnInit()
    {
        super.ngOnInit();

        if (this.tableDataSource)
        {
            if (!this.externalSorting)
            {
                this.tableDataSource.sortingDataAccessor = this.sortDataAccessor.bind(this);
            }
        }
    }

    ngOnDestroy()
    {
        super.ngOnDestroy();

        if (this.m_changeDetectSub)
        {
            this.m_changeDetectSub.unsubscribe();
            this.m_changeDetectSub = null;
        }

        if (this.datatableConfiguration) this.datatableConfiguration.clearListeners();
    }

    protected afterLayoutChange(): void
    {
        this.datatableConfiguration.triggerRecalculation(false);
    }

    public async onPage(event: PageEvent): Promise<void>
    {
        if (this.table)
        {
            await this.table.onPage(event);
        }
        else
        {
            this.page.emit(event);
        }
    }

    public async onSort(event: Sort): Promise<void>
    {
        if (!this.externalSorting)
        {
            return;
        }

        let sort: DatatableSort[];

        if (event.active)
        {
            sort = this.sortSettings[event.active];
            if (sort && event.direction === "desc")
            {
                sort = sort.map((s,
                                 i) =>
                                {
                                    let s2: DatatableSort = {
                                        prop: s.prop,
                                        dir : s.dir === "desc" ? "asc" : "desc"
                                    };
                                    return s2;
                                });
            }

            this.activeSort          = event.active;
            this.activeSortDirection = event.direction;
        }
        else
        {
            this.activeSort          = "";
            this.activeSortDirection = "";
        }

        if (this.table)
        {
            await this.table.onSort(sort);
        }
        else
        {
            this.sort.emit(sort);
        }
    }

    public get anyExpanded(): boolean
    {
        return this.table?.expansionManager?.anyExpanded;
    }

    public isExpanded(row: T): boolean
    {
        return this.table?.expansionManager?.isExpanded(row);
    }

    public expandRow(row: T)
    {
        // Skip to handler if known interactive column
        let column = this.lastClickedColumnId;
        if (column === "selection" || column === "viewIcon")
        {
            this.handleRowClick(row, null, column);
            return;
        }

        // Do not expand if there are no table-level or column-level details
        if (!this.hasDetails) return;

        // Get any active expansion record
        let expanded               = this.table?.expansionManager.isExpanded(row);
        let trigger                = this.hasTableDetailsOnly ? null : this.lastClickedColumnId;
        let columnChanged: boolean = this.table?.expansionManager.getTrigger(row) !== trigger;

        // If nothing is expanded, expand it
        if (!expanded)
        {
            if (this.hasTableDetailsOnly && this.useExpandToggle)
            {
                if (this.lastClickedColumnId === "expandToggle")
                {
                    // Do nothing, we toggle in the template
                }
                else
                {
                    // Pass through click for any other column
                    this.handleRowClick(row, null, this.lastClickedColumnId);
                }
            }
            else
            {
                // Toggle the row
                this.table?.expansionManager.itemExpanded(row, trigger);
            }
        }
        // If there was an expansion, toggle or change it
        else
        {
            // If the column changed, modify the expansion
            if (columnChanged)
            {
                this.table?.expansionManager.itemExpanded(row, trigger);
            }
            // If column was not changed, toggle expansion
            else
            {
                if (this.hasTableDetailsOnly && this.useExpandToggle)
                {
                    if (this.lastClickedColumnId === "expandToggle")
                    {
                        // We handle this toggle in the template
                    }
                    else
                    {
                        // Pass through click if using expand column to toggle and not clicking expand toggle column
                        this.handleRowClick(row, null, this.lastClickedColumnId);
                    }
                }
                else
                {
                    this.table?.expansionManager.itemExpanded(row, trigger);
                }
            }

        }
    }

    public rowClicked(row: T,
                      columnName: string,
                      columnId: string)
    {
        // close the menu if its open
        if (this.contextMenu.isShown)
        {
            this.contextMenu.reset();
            return;
        }

        this.lastClickedColumnId = columnId;

        // If clicking inside expanded detail column let the details area handle it
        if (this.lastClickedColumnId === "expandedDetail") return;

        // If there is a detail template associated with the click, handle it
        if (!!this.details)
        {
            this.expandRow(row);
            return;
        }

        // handle activate event
        this.handleRowClick(row, columnName, columnId);
    }

    private handleRowClick(row: T,
                           columnName: string,
                           columnId: string)
    {
        // handle activate event
        if (this.table)
        {
            this.table.rowClicked(columnId, row);
        }
        else
        {
            this.activate.emit(new DatatableRowActivateEvent(row, columnId));
        }
    }

    public async onTableContextMenu(event: MouseEvent,
                                    row: T,
                                    columnProp: string)
    {
        // cancel the original event so we don't show native context menu
        event.preventDefault();
        event.stopPropagation();

        // close the menu if its open
        if (this.contextMenu.isShown)
        {
            this.contextMenu.reset();

            await Future.delayed(250);
        }

        // position the context menu
        let bounds                                              = this.element.nativeElement.getBoundingClientRect();
        let x                                                   = event.clientX - bounds.left;
        let y                                                   = event.clientY - bounds.top;
        this.contextMenuTriggerWrapper.nativeElement.style.left = `${x}px`;
        this.contextMenuTriggerWrapper.nativeElement.style.top  = `${y}px`;

        // populate declared items
        // ** to do: find those added declaratively **

        // raise the context menu showing event
        let emittedEvent            = new DatatableContextMenuEvent<T>();
        emittedEvent.columnProperty = columnProp;
        emittedEvent.row            = row;
        this.onShowingContextMenu.emit(emittedEvent);

        // show the context menu
        if (emittedEvent.root.subMenuItems.length)
        {
            this.contextMenu.show(emittedEvent.root.subMenuItems);
        }
    }

    private defaultComparator(a: T,
                              b: T): boolean
    {
        return a === b;
    }
}

export class DatatableConfiguration
{
    private m_table: HTMLElement;
    set table(table: HTMLElement)
    {
        if (table) this.m_table = table;
    }

    private m_headers: QueryList<ElementRef>;
    set headers(headers: QueryList<ElementRef>)
    {
        if (headers && this.m_headers !== headers)
        {
            this.m_headers = headers;
            this.recomputeHeaders();
        }
    }

    private headerArray: ElementRef[];
    private recalcWidthTimer: number = null;

    hasResized: boolean;

    private columnConfigLookup: { [id: string]: ColumnConfiguration };
    private columnTemplateLookup: { [id: string]: DatatableColumnDirective };

    private dragging: boolean;
    private prevClientX: number;
    private resizingColumn: ColumnConfiguration;

    private columnIdToIndex: { [colId: string]: number };

    public get hasColumnIds(): boolean
    {
        return !!this.columnIdToIndex;
    }

    public set columnIds(columnIds: string[])
    {
        if (!columnIds || columnIds.length === 0) return;
        this.columnIdToIndex = {};
        for (let i = 0; i < columnIds.length; i++) this.columnIdToIndex[columnIds[i]] = i;
    }

    public get resizing(): boolean
    {
        return this.dragging;
    }

    public get columnConfigurations(): ColumnConfiguration[]
    {
        return this.m_columnConfigurations
                   .filter((column) => !!this.columnConfigLookup[column.id])
                   .map((columnConfig) => columnConfig.clone());
    }

    public get defaultConfigs(): ColumnConfiguration[]
    {
        return this.m_columnTemplates.map((template) => new ColumnConfiguration(template.id, template.enabled, template.name));
    }

    public get isDefault(): boolean
    {
        if (!this.m_columnConfigurations || !this.m_columnTemplates) return true;
        if (this.m_columnConfigurations.length !== this.m_columnTemplates.length) return false;

        for (let i = 0; i < this.m_columnConfigurations.length; i++)
        {
            let columnConfig   = this.m_columnConfigurations[i];
            let columnTemplate = this.m_columnTemplates[i];

            if (columnConfig.id !== columnTemplate.id || columnConfig.enabled !== columnTemplate.enabled || columnConfig.name !== columnTemplate.name)
            {
                return false;
            }
        }

        return true;
    }

    private static readonly manualResizeRowCount = 16;

    constructor(private m_columnTemplates: DatatableColumnDirective[],
                private m_columnConfigurations: ColumnConfiguration[])
    {
        this.columnTemplateLookup = {};
        for (let columnTemplate of m_columnTemplates) this.columnTemplateLookup[columnTemplate.id] = columnTemplate;

        this.updateColumnConfigs();
    }

    private updateColumnConfigs()
    {
        this.columnConfigLookup = {};
        this.hasResized         = false;

        let defaultConfigs = this.defaultConfigs;
        if (!this.m_columnConfigurations)
        {
            this.m_columnConfigurations = defaultConfigs;
        }

        for (let column of this.m_columnConfigurations)
        {
            // Add any columns from config still in the template
            if (this.columnTemplateLookup[column.id])
            {
                this.columnConfigLookup[column.id] = column;
            }
        }

        // Add in any new columns from the template, not in the config
        for (let column of defaultConfigs)
        {
            if (!this.columnConfigLookup[column.id])
            {
                this.columnConfigLookup[column.id] = column;

                let templateIndex = this.m_columnTemplates.indexOf(this.columnTemplateLookup[column.id]);
                this.m_columnConfigurations.splice(templateIndex, 0, column);
            }
        }
    }

    public setColumnConfigs(updatedConfigs: ColumnConfiguration[])
    {
        this.m_columnConfigurations = updatedConfigs;
        this.updateColumnConfigs();
    }

    //--//

    public setUpResizeInfo(cells: QueryList<ElementRef>): boolean
    {
        if (this.setUpGrippers(false))
        {
            this.setUpCells(cells.toArray());
            this.triggerRecalculation(true);
            return true;
        }
        return false;
    }

    private getIdFromHeaderElement(headerElement: ElementRef): string
    {
        const classIdPrefix = "mat-column-";
        let className       = headerElement.nativeElement.className;
        let columnClass     = className.split(" ")
                                       .find((c: string) => c.startsWith(classIdPrefix));
        return columnClass ? columnClass.substring(classIdPrefix.length) : null;
    }

    private recomputeHeaders()
    {
        if (!this.m_headers) return;
        this.headerArray = this.m_headers.toArray()
                               .sort((elRefA,
                                      elRefB) => UtilsService.compareNumbers(this.columnIdToIndex[this.getIdFromHeaderElement(elRefA)], this.columnIdToIndex[this.getIdFromHeaderElement(elRefB)]));
    }

    private setUpGrippers(fromSelf: boolean): boolean
    {
        if (this.headerArray.some((header) => header.nativeElement.clientWidth === 0))
        {
            if (fromSelf) return false;

            this.recomputeHeaders();
            return this.setUpGrippers(true);
        }

        for (let config of this.m_columnConfigurations)
        {
            config.prevColConfig = undefined;
            config.nextColConfig = undefined;
        }

        let gripperBounds                         = this.getGripperBounds();
        let offset                                = 0;
        let prevColumnConfig: ColumnConfiguration = null;
        for (let i = 0; i < this.headerArray.length; i++)
        {
            let header: HTMLElement = this.headerArray[i].nativeElement;
            let headerWidth         = header.clientWidth;
            let headerId: string    = DatatableConfiguration.getColumnId(header);

            let columnConfig: ColumnConfiguration = this.getColumnConfig(headerId);
            if (columnConfig)
            {
                columnConfig.headerElement = header;
                columnConfig.flexBasis     = headerWidth;
                columnConfig.leftOffset    = offset;
                columnConfig.cellElements  = [];

                columnConfig.hasGripper     = i >= gripperBounds.low && i < gripperBounds.high;
                columnConfig.gripperElement = <HTMLElement>header.getElementsByClassName("gripper")[0];
                columnConfig.updateGripperPosition(this.columnTemplateLookup[columnConfig.id]?.sortable);

                if (prevColumnConfig)
                {
                    columnConfig.prevColConfig     = prevColumnConfig;
                    prevColumnConfig.nextColConfig = columnConfig;
                }
            }

            offset += headerWidth;

            prevColumnConfig = columnConfig;
        }

        return true;
    }

    private getGripperBounds(): { low: number, high: number }
    {
        let bounds = {
            low : 0,
            high: this.headerArray.length - 1
        };

        const isResizable: (header: ElementRef) => boolean = (header) =>
        {
            let template = this.getColumnTemplate(DatatableConfiguration.getColumnId(header.nativeElement));
            return template && template.resizable && (isNaN(template.maxWidth) || template.minWidth < template.maxWidth);
        };

        while (bounds.low < this.headerArray.length && !isResizable(this.headerArray[bounds.low])) bounds.low++;
        while (bounds.high > bounds.low && !isResizable(this.headerArray[bounds.high])) bounds.high--;

        return bounds;
    }

    private setUpCells(cells: ElementRef[])
    {
        for (let cell of cells)
        {
            let columnConfig = this.getColumnConfig(DatatableConfiguration.getColumnId(cell.nativeElement));
            // ~20 rows is more than will generally ever be in view if resizing; if sticky headers, will have to introduce more complex logic or change up
            if (columnConfig?.cellElements?.length < DatatableConfiguration.manualResizeRowCount) columnConfig.cellElements.push(cell.nativeElement);
        }
    }

    //--//

    public triggerRecalculation(fromCreation: boolean)
    {
        if (!this.recalcWidthTimer && this.m_table)
        {
            let prevTableWidth: number = this.m_table.clientWidth;
            let hasChanged: boolean    = !fromCreation;
            this.recalcWidthTimer      = setInterval(() =>
                                                     {
                                                         let currTableWidth = this.m_table.clientWidth;
                                                         if (currTableWidth !== prevTableWidth)
                                                         {
                                                             prevTableWidth = currTableWidth;
                                                             hasChanged     = true;
                                                         }
                                                         else
                                                         {
                                                             clearInterval(this.recalcWidthTimer);
                                                             this.recalcWidthTimer = null;
                                                             if (hasChanged) this.setUpGrippers(false);
                                                         }
                                                     }, 100);
        }
    }

    public clearListeners()
    {
        if (this.recalcWidthTimer)
        {
            clearInterval(this.recalcWidthTimer);
            this.recalcWidthTimer = null;
        }
    }

    //--//

    public resizePrep(columnId: string): void
    {
        let column           = this.getColumnConfig(columnId);
        column.overrideBasis = column.flexBasis;
        column.overrideLeft  = column.leftOffset;

        this.resizingColumn = column;
        this.hasResized     = true;
        this.dragging       = true;
    }

    public setupForDrag(event: MouseEvent)
    {
        this.prevClientX = event.clientX;
    }

    public resizeConstrain(point: Point): Point
    {
        let targetPx = point.x - this.prevClientX;
        if (Math.abs(targetPx) > 1) this.prevClientX += this.doResize(targetPx);

        point.x = this.prevClientX;
        return point;
    }

    private doResize(targetPx: number): number
    {
        let availableLeftPx  = this.generateResizeRecipe(this.resizingColumn, targetPx, false);
        let availableRightPx = -this.generateResizeRecipe(this.resizingColumn.nextColConfig, -targetPx, true);

        let resizePx = targetPx > 0 ? Math.min(availableLeftPx, availableRightPx) : Math.max(availableLeftPx, availableRightPx);

        this.followResizeRecipe(this.resizingColumn, resizePx, false);
        this.followResizeRecipe(this.resizingColumn.nextColConfig, -resizePx, true);

        return resizePx;
    }

    private generateResizeRecipe(config: ColumnConfiguration,
                                 targetPx: number,
                                 toRight: boolean): number
    {
        let resizablePx: number = 0;
        while (config)
        {
            let localPx = 0;

            if (Math.abs(resizablePx) < Math.abs(targetPx))
            {
                let template = this.getColumnTemplate(config.id);

                if (template.resizable)
                {
                    let lowBound  = template.minWidth - config.flexBasis;
                    let highBound = template.maxWidth ? template.maxWidth - config.flexBasis : Number.MAX_VALUE;
                    localPx       = UtilsService.clamp(lowBound, highBound, targetPx - resizablePx);
                    resizablePx += localPx;
                }
            }

            config.scratchPadForResize = localPx;

            config = config.moveCursor(toRight);
        }

        return resizablePx;
    }

    private followResizeRecipe(config: ColumnConfiguration,
                               totalPx: number,
                               toRight: boolean)
    {
        if (totalPx === 0)
        {
            return;
        }

        let compareFn = totalPx > 0 ? Math.min : Math.max;

        let configStart = config;

        while (true)
        {
            config.scratchPadForResize = compareFn(config.scratchPadForResize, totalPx);

            totalPx -= config.scratchPadForResize;

            let configNext = config.moveCursor(toRight);
            if (!configNext)
            {
                break;
            }

            config = configNext;
        }

        while (true)
        {
            let adjustedPx = config.scratchPadForResize;

            config.leftOffset -= toRight ? totalPx + adjustedPx : -totalPx;
            config.flexBasis += adjustedPx;
            totalPx += adjustedPx;

            config.updateColumnStyles(this.columnTemplateLookup[config.id]?.sortable);

            if (config == configStart)
            {
                break;
            }

            config = config.moveCursor(!toRight);
        }
    }

    public clearStaleInfo()
    {
        if (!this.dragging) this.prevClientX = undefined;
    }

    public completeResize(): void
    {
        let column           = this.resizingColumn;
        column.overrideBasis = undefined;
        column.overrideLeft  = undefined;

        this.resizingColumn = null;
        this.dragging       = false;
    }

    //--//

    private static getColumnId(columnElement: HTMLElement): string
    {
        let classes  = columnElement.className;
        let idPrefix = "mat-column-";
        let idx      = classes.indexOf(idPrefix);
        if (idx !== -1)
        {
            let endIdx = classes.indexOf(" ", idx);
            if (endIdx === -1) endIdx = classes.length;
            return classes.substring(idx + idPrefix.length, endIdx);
        }

        return null;
    }

    //--//

    public getColumnTemplates(): DatatableColumnDirective[]
    {
        return this.m_columnConfigurations.map((columnConfig) => this.columnTemplateLookup[columnConfig.id])
                   .filter((columnTemplate) => !!columnTemplate);
    }

    public getColumnConfig(headerId: string): ColumnConfiguration
    {
        return this.columnConfigLookup[headerId] || null;
    }

    public getColumnTemplate(headerId: string): DatatableColumnDirective
    {
        return this.columnTemplateLookup[headerId] || null;
    }
}

const ColumnFormatPrivate = {
    VALUE   : "VALUE",
    NUMBER  : "NUMBER",
    PERCENT : "PERCENT",
    DATE    : "DATE",
    DATELONG: "DATELONG",
    YESNO   : "YESNO",
    TEMPLATE: "TEMPLATE"
};

export type ColumnFormat = keyof typeof ColumnFormatPrivate;

export const ColumnFormat: { [P in ColumnFormat]: P } = <any>ColumnFormatPrivate;

export class DatatableContextMenuEvent<T>
{
    public root: ContextMenuItemComponent = new ContextMenuItemComponent();

    public row: T;

    public columnProperty: string;
}

export class DatatableRowActivateEvent<T>
{
    constructor(public row: T,
                public columnId: string)
    {
    }
}

export class DatatableExpandedRow<T>
{
    constructor(public row: T,
                public columnId: string)
    {
    }
}

