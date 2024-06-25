import {Component, ContentChildren, Directive, ElementRef, EventEmitter, HostListener, Injector, Input, Optional, Output, QueryList, Renderer2, ViewChild, ViewChildren} from "@angular/core";

import {ExpandDirective} from "framework/directives/expand.directive";
import {Logger, LoggingService} from "framework/services/logging.service";
import {BaseComponent} from "framework/ui/components";
import {ApplicationLogEntry, ApplicationLogFilter, ColumnConfig, DefaultColumns, IApplicationLog, IApplicationLogRange, IConsoleLogEntry, IConsoleLogProvider, LogColumn} from "framework/ui/consoles/console-log";
import {ConsoleLogFilterComponent} from "framework/ui/consoles/console-log-filter.component";
import {ContextMenuComponent, ContextMenuItemComponent} from "framework/ui/context-menu/context-menu.component";
import {Future} from "framework/utils/concurrency";

import {Subject} from "rxjs";
import {debounceTime} from "rxjs/operators";

@Component({
               selector: "o3-console-log-column",
               template: ""
           })
export class ConsoleLogColumnComponent
{
    @Input() public type: LogColumn;
    @Input() public name: string;
    @Input() public enabled: boolean;
}

@Component({
               selector   : "o3-console-log",
               templateUrl: "./console-log.component.html"
           })
export class ConsoleLogComponent extends BaseComponent
{
    private readonly logger: Logger;

    private m_handleKeyDown: boolean;

    @ViewChild("scrollbar", {static: true}) scrollbar: ConsoleLogVirtualScrollComponent;

    @ViewChildren("resizer", {read: ElementRef})
    public set resizers(resizers: QueryList<ElementRef>)
    {
        this.resizeObserver.disconnect();
        let elements = resizers.toArray()
                               .map((ref) => ref.nativeElement);

        for (let el of elements)
        {
            this.resizeObserver.observe(el);
        }
    }

    @ContentChildren(ConsoleLogColumnComponent) configuredColumns: QueryList<ConsoleLogColumnComponent>;

    log: ConsoleLog;

    @Input() interactive: boolean = false;

    @Input() configurable: boolean = true;

    @Input() filterable: boolean = true;

    inputText: string;

    inputHistory: string[] = [];

    inputHistoryOffset: number = 0;

    @Output() commandSubmitted = new EventEmitter<ConsoleLogCommandEventArgs>();

    @Input() scrollLockEnabled: boolean = false;

    @Input() scrollLocked: boolean = false;

    @Output() scrollLockedChange: EventEmitter<boolean> = new EventEmitter<boolean>();

    resizeObserver: ResizeObserver;

    private resizeSubject = new Subject<ResizeObserverEntry[]>();

    public columns: ColumnConfig[];
    public messageColumn: ColumnConfig;

    public get enabledColumns(): ColumnConfig[]
    {
        return this.columns.filter((c) => c.enabled) || [];
    }

    constructor(inj: Injector,
                private element: ElementRef,
                private renderer: Renderer2,
                logService: LoggingService,
                @Optional() private expand: ExpandDirective)
    {
        super(inj);

        this.initLog();

        this.logger = logService.getLogger(ConsoleLogComponent);

        this.subscribeToObservable(this.resizeSubject.pipe(debounceTime(250)), (entries) => this.handleResize(entries));
        this.resizeObserver = new window.ResizeObserver((entries: ResizeObserverEntry[]) => this.resizeSubject.next(entries));
        if (this.expand)
        {
            this.subscribeToObservable(this.expand.expansionCompleted, () => this.refresh(false));
        }
    }

    private initLog(provider?: IConsoleLogProvider)
    {
        if (!provider)
        {
            provider = {
                getLogCount(): number
                {
                    return 0;
                },
                async getLogPage(start: number,
                                 end: number): Promise<IConsoleLogEntry[]>
                {
                    return [];
                },
                async performFilter(filter: ApplicationLogFilter): Promise<IApplicationLogRange[]>
                {
                    return [];
                }
            };
        }

        this.log = new ConsoleLog(provider);
    }

    public ngOnInit()
    {
        super.ngOnInit();

        this.messageColumn = new ColumnConfig("line", "Message", true, 0);
    }

    public ngOnDestroy()
    {
        super.ngOnDestroy();
        this.resizeObserver.disconnect();
        this.resizeObserver = null;
    }

    protected afterLayoutChange(): void
    {
        if (this.calculateVisibleLines())
        {
            this.log.refresh(false);
        }
    }

    bind(provider: IConsoleLogProvider)
    {
        if (!this.columns)
        {
            let configuredColumns = this.configuredColumns?.toArray();
            if (configuredColumns?.length)
            {
                this.columns = configuredColumns.map((c) =>
                                                     {
                                                         let defaultColumn = DefaultColumns.find((c2) => c2.type == c.type);

                                                         return new ColumnConfig(c.type, c.name, c.enabled, defaultColumn?.width || 0);
                                                     });
            }
            else
            {
                this.columns = DefaultColumns;
            }
        }
        if (this.log.provider !== provider)
        {
            this.initLog(provider);
            this.detectChanges();

            this.scrollbar.bind(this.log);

            this.calculateVisibleLines();
        }
    }

    private calculateVisibleLines(): boolean
    {
        try
        {
            this.logger.debug("Calculating lines based on height...");

            // calculate # visible based on size of container
            let container = this.element.nativeElement.querySelector(".console-log-container");
            if (!container) return false;

            let containerHeight = container.clientHeight;

            let list = this.element.nativeElement.querySelector(".console-log table");
            if (!list) return false;

            let testLine       = this.renderer.createElement("tr");
            testLine.innerText = "_ _ _";
            this.renderer.appendChild(list, testLine);

            let lineHeight = testLine.clientHeight;

            this.renderer.removeChild(list, testLine);
            if (lineHeight > 0)
            {
                let linesVisible = Math.floor((containerHeight - 7.5 /* horiz scroll height */) / lineHeight);
                if (this.interactive) linesVisible = linesVisible - 1;

                linesVisible -= 2; // Account for scroll bar at the bottom.

                if (this.log.visible != linesVisible)
                {
                    this.log.visible = linesVisible;
                    this.logger.debug(`Setting visible lines to ${linesVisible}`);
                    return true;
                }
            }
        }
        catch (e)
        {
            this.logger.error(`Exception calculating visible lines in console: ${e}`);
        }

        return false;
    }

    previous(e: ConsoleLogScrollEventArgs)
    {
        // disable scroll on up
        if (this.scrollLockEnabled)
        {
            this.scrollLocked = false;
            this.scrollLockedChange.emit(this.scrollLocked);
        }

        if (!e) e = new ConsoleLogScrollEventArgs();
        this.log.previous(e.singleRow, e.fullDocument);
    }

    next(e: ConsoleLogScrollEventArgs)
    {
        if (!e) e = new ConsoleLogScrollEventArgs();
        this.log.next(e.singleRow, e.fullDocument);

        // re-enable scroll lock when we hit the end
        if (this.scrollLockEnabled && !this.log.hasNext)
        {
            this.scrollLocked = true;
            this.scrollLockedChange.emit(this.scrollLocked);
        }
    }

    reset()
    {
        this.log.reset();
    }

    enableKeyDownHandler()
    {
        this.m_handleKeyDown = true;
        this.refresh();
    }

    disableKeyDownHandler()
    {
        this.m_handleKeyDown = false;
    }

    refresh(focus: boolean = false)
    {
        if (this.scrollLockEnabled && !this.log.hasNext)
        {
            focus = true;
        }

        this.calculateVisibleLines();

        this.log.refresh(focus);
    }

    submitInput()
    {
        if (this.inputText)
        {
            let command     = new ConsoleLogCommandEventArgs();
            command.command = this.inputText;
            this.commandSubmitted.emit(command);

            if (!command.cancel)
            {
                this.inputHistory.push(this.inputText);
                this.inputHistoryOffset = 0;
                this.inputText          = null;
            }
        }
    }

    onConsoleKeydown(event: ConsoleLogKeyEventArgs)
    {
        if (!this.m_handleKeyDown)
        {
            event.cancel = true;
            return;
        }

        const ArrowUp: number   = 38;
        const ArrowDown: number = 40;

        let keycode: number = event.keycode;

        if (this.interactive && this.inputHistory && this.inputHistory.length)
        {
            let index = this.inputHistory.length - this.inputHistoryOffset;
            if (index < 0) index = 0;
            if (index > this.inputHistory.length - 1) index = this.inputHistory.length - 1;

            if (keycode == ArrowUp)
            {
                this.inputText          = this.inputHistory[index];
                this.inputHistoryOffset = this.inputHistoryOffset + 1;
                if (this.inputHistoryOffset >= this.inputHistory.length) this.inputHistoryOffset = this.inputHistory.length;
                event.cancel = true;
            }
            else if (keycode == ArrowDown)
            {
                this.inputText          = this.inputHistory[index];
                this.inputHistoryOffset = this.inputHistoryOffset - 1;
                if (this.inputHistoryOffset < 0) this.inputHistoryOffset = 0;
                event.cancel = true;
            }
        }
    }

    //--//

    onFilterUpdate(filters: ApplicationLogFilter)
    {
        this.log.filter(filters);
    }

    //--//

    @ViewChild("contextMenu", {static: true}) contextMenu: ContextMenuComponent;

    @ViewChild("contextMenuTriggerWrapper", {
        read  : ElementRef,
        static: true
    }) contextMenuTriggerWrapper: ElementRef;

    @ViewChild(ConsoleLogFilterComponent) filters: ConsoleLogFilterComponent;

    public async onContextMenu(event: MouseEvent,
                               column: LogColumn,
                               filter: string,
                               entry: IConsoleLogEntry)
    {
        // cancel the original event so we don't show native context menu
        event.preventDefault();
        event.stopPropagation();

        if (this.contextMenu.close())
        {
            // wait for 250ms if it was previously open
            await Future.delayed(250);
        }

        if (filter)
        {
            filter = filter.trim();
        }

        ContextMenuComponent.positionMenu(this.contextMenuTriggerWrapper.nativeElement, this.element.nativeElement.querySelector(".console-log-container"), event);

        let root = new ContextMenuItemComponent();
        switch (column)
        {
            case "thread":
                this.addContextItems(root, filter, (filters) => filters.threads);
                break;

            case "host":
                this.addContextItems(root, filter, (filters) => filters.hosts);
                break;

            case "level":
                this.addContextItems(root, filter, (filters) => filters.levels);
                break;

            case "selector":
                this.addContextItems(root, filter, (filters) => filters.selectors);
                break;
        }

        root.addItem("Recenter without text filter", () =>
        {
            this.filters.updateFilters((filters) =>
                                       {
                                           filters.filter = "";
                                       });

            this.log.go(entry.lineNumber);
        });

        if (root.subMenuItems.length > 0)
        {
            this.contextMenu.open(root.subMenuItems);
        }
    }

    private addContextItems(root: ContextMenuItemComponent,
                            filter: string,
                            accessor: (filters: ApplicationLogFilter) => string[])
    {
        root.addItem(`Show rows with '${filter}'`, () =>
        {
            this.filters.updateFilters((filters) =>
                                       {
                                           let list = accessor(filters);
                                           if (!list.includes(filter))
                                           {
                                               list.push(filter);
                                           }
                                       });
        });

        root.addItem(`Hide rows with '${filter}'`, () =>
        {
            this.filters.updateFilters((filters) =>
                                       {
                                           let list          = accessor(filters);
                                           let negatedFilter = `!${filter}`;
                                           if (!list.includes(negatedFilter))
                                           {
                                               list.push(negatedFilter);
                                           }
                                       });
        });
    }

    //--//


    public newLogEntry(item: IApplicationLog,
                       incrementLine = true): IConsoleLogEntry
    {
        return new ApplicationLogEntry((html) => this.bypassSecurityTrustHtml(html), item, incrementLine);
    }

    public updateColumns(columns: ColumnConfig[])
    {
        this.columns = columns;
    }

    private handleResize(entries: ResizeObserverEntry[])
    {
        if (entries.length > 1) return;

        for (let entry of entries)
        {
            let resizer: HTMLElement = <HTMLElement>entry.target;
            let column: Element      = resizer.parentElement;

            // User has not touched this column yet
            if (!resizer.style?.width) continue;

            let columnIdx = 0;
            while (column.previousElementSibling)
            {
                columnIdx++;
                column = column.previousElementSibling;
            }

            let colConfig = this.enabledColumns[columnIdx] || this.messageColumn;
            if (Array.isArray(entry.borderBoxSize))
            {
                colConfig.width = entry.borderBoxSize[0].inlineSize;
            }
            else
            {
                // Safari does not have borderBoxSize so use the contentRect and adjust for padding
                let borderBoxSize: ResizeObserverSize = <any>entry.borderBoxSize;
                colConfig.width                       = borderBoxSize?.inlineSize || entry.contentRect.width + 10;
            }
            this.detectChanges();
        }
    }
}

export class ConsoleLogCommandEventArgs
{
    command: string;

    cancel: boolean;
}

@Directive({
               selector: "[console-log-scroller]"
           })
export class ConsoleLogScrollerDirective
{
    private readonly logger: Logger;

    @Output() startReached = new EventEmitter<ConsoleLogScrollEventArgs>();

    @Output() endReached = new EventEmitter<ConsoleLogScrollEventArgs>();

    @Output() consoleKeydown = new EventEmitter<ConsoleLogKeyEventArgs>();

    /**
     * the native element.
     */
    private m_element: HTMLElement;

    constructor(private element: ElementRef,
                logService: LoggingService)
    {
        this.logger = logService.getLogger(ConsoleLogComponent);

        this.m_element = element.nativeElement;
    }

    onStartReached(singleRow: boolean,
                   fullDocument: boolean)
    {
        let e          = new ConsoleLogScrollEventArgs();
        e.singleRow    = singleRow;
        e.fullDocument = fullDocument;

        this.startReached.emit(e);
    }

    onEndReached(singleRow: boolean,
                 fullDocument: boolean)
    {
        let e          = new ConsoleLogScrollEventArgs();
        e.singleRow    = singleRow;
        e.fullDocument = fullDocument;

        this.endReached.emit(e);
    }

    @HostListener("scroll", ["$event"]) onScroll(event: Event)
    {
        let tracker = event.target;
        if (tracker instanceof HTMLElement)
        {
            if (tracker.scrollTop < 0)
            {
                this.onStartReached(false, false);
            }
            else if (tracker.scrollTop > 0)
            {
                this.onEndReached(false, false);
            }
        }
    }

    @HostListener("wheel", ["$event"]) onWheel(event: WheelEvent)
    {
        this.onWheelPositionChanged(event);
    }

    onWheelPositionChanged(event: WheelEvent)
    {
        let delta = Math.max(-1, Math.min(1, -event.deltaY));

        if (delta < 0)
        {
            this.onEndReached(false, false);
        }
        else if (delta > 0)
        {
            this.onStartReached(false, false);
        }

        preventDefaultEvent(event);
    }

    @HostListener("document:keydown", ["$event"]) onKeyDown(event: KeyboardEvent)
    {
        const ArrowUp: number   = 38;
        const PageUp: number    = 33;
        const ArrowDown: number = 40;
        const PageDown: number  = 34;
        const Home: number      = 36;
        const End: number       = 35;

        let keycode: number = event.keyCode;
        let key             = event.key;

        this.logger.debugVerbose(`On Key: ${key} ${keycode}`);

        let args     = new ConsoleLogKeyEventArgs();
        args.key     = key;
        args.keycode = keycode;
        this.consoleKeydown.emit(args);

        if (!args.cancel)
        {
            switch (keycode)
            {
                case ArrowUp:
                    this.onStartReached(true, false);
                    break;

                case PageUp:
                    this.onStartReached(false, false);
                    break;

                case Home:
                    this.onStartReached(false, true);
                    break;

                case ArrowDown:
                    this.onEndReached(true, false);
                    break;

                case PageDown:
                    this.onEndReached(false, false);
                    break;

                case End:
                    this.onEndReached(false, true);
                    break;

                default:
                    return;
            }

            preventDefaultEvent(event);
        }
    }
}

export class ConsoleLogScrollEventArgs
{
    // Whether to only scroll a single row. If false, a scroll will be a partial page.
    singleRow: boolean = false;

    // Whether to scroll to the top or bottom of the log.
    fullDocument: boolean = false;
}

export class ConsoleLogKeyEventArgs
{
    cancel: boolean;

    keycode: number;

    key: string;
}

@Component({
               selector: "o3-console-log-virtual-scroll",
               template: "<div #thumb class=\"console-log-virtual-scroll-thumb\" [style.height]=\"thumbHeightStyle\" [style.top]=\"thumbTopStyle\"></div>",
               host    : {"class": "console-log-virtual-scroll"}
           })
export class ConsoleLogVirtualScrollComponent extends BaseComponent
{
    /**
     * the log.
     */
    log: ConsoleLog;

    /**
     * the scroll thumb element.
     */
    @ViewChild("thumb", {static: true}) thumb: ElementRef;

    /**
     * the native element.
     */
    private m_element: HTMLElement;
    private m_body: HTMLElement;
    private m_dragging: boolean     = false;
    private m_draggingPageY: number = 0;
    private m_draggingTop: number   = 0;

    private readonly m_minThumbHeight = 15;

    private get scrollHeight(): number
    {
        return this.m_element ? this.m_element.clientHeight : 0;
    }

    /**
     * the height of the scroll thumb.
     */
    get thumbHeight(): number
    {
        if (this.log && this.m_element)
        {
            let visible = this.log.visible;
            let count   = this.log.count;

            if (visible < count)
            {
                let heightRatio = safeDivision(visible, count);
                let thumbHeight = this.scrollHeight * heightRatio;

                return Math.max(thumbHeight, this.m_minThumbHeight);
            }
        }

        return 0;
    }

    /**
     * the height of the scroll thumb as a style value.
     */
    get thumbHeightStyle(): string
    {
        return `${this.thumbHeight}px`;
    }

    private get maxThumbTop(): number
    {
        let visible = this.log.visible;
        let count   = this.log.count;

        return visible < count ? this.scrollHeight - this.thumbHeight : 0;
    }

    /**
     * the top position of the scroll thumb.
     */
    get thumbTop(): number
    {
        if (this.log && this.m_element)
        {
            if (!this.m_dragging)
            {
                let visible           = this.log.visible;
                let count             = this.log.count;
                let countMinusVisible = count - visible;

                let topRatio = (countMinusVisible <= 0) ? 0 : safeDivision(this.log.start, countMinusVisible);
                return this.maxThumbTop * topRatio;
            }
            else
            {
                let top = this.extractThumbTopFromElement();
                return Math.min(top, this.maxThumbTop);
            }
        }
        return 0;
    }

    /**
     * the top position of the scroll thumb as a style value.
     */
    get thumbTopStyle(): string
    {
        return `${this.thumbTop}px`;
    }

    constructor(inj: Injector,
                private element: ElementRef,
                private renderer: Renderer2)
    {
        super(inj);

        this.m_element = element.nativeElement;
        this.m_body    = document.documentElement.querySelector("body");
    }

    /**
     * bind the component to the specified log.
     */
    bind(log: ConsoleLog)
    {
        // capture our log
        this.log = log;

        this.subscribeToMouseDrag(this.thumb.nativeElement,
                                  (e,
                                   mouseDown,
                                   mouseUp) => this.handleDragEvent(e, e.pageY, mouseDown, mouseUp));

        this.subscribeToTouchDrag(this.thumb.nativeElement,
                                  (e,
                                   mouseDown,
                                   mouseUp) => this.handleDragEvent(e, e.targetTouches?.[0]?.pageY, mouseDown, mouseUp));
    }

    private handleDragEvent(e: Event,
                            pageY: number,
                            mouseDown: boolean,
                            mouseUp: boolean)
    {
        if (pageY === undefined) return;

        if (mouseDown)
        {
            this.m_draggingPageY = pageY;
            this.m_draggingTop   = this.extractThumbTopFromElement();
            this.m_dragging      = true;
        }
        else if (mouseUp)
        {
            if (this.m_dragging)
            {
                this.updateStartFromThumb();

                // turn off dragging
                this.m_body.removeEventListener("selectstart", preventDefaultEvent, false);
                this.m_dragging = false;
            }
        }
        else
        {
            this.updateStartFromThumb();
            e.preventDefault();
            let offset = this.m_draggingTop - this.m_draggingPageY;
            let top    = offset + pageY;

            this.m_body.addEventListener("selectstart", preventDefaultEvent, false);

            // ensure we aren't outside of the client height for the scroll bar
            top = Math.max(0, top);
            top = Math.min(top, this.maxThumbTop);

            // set position of thumb
            // renderer.setElementStyle was deprecated in ng5
            // this.renderer.setElementStyle(this.thumb.nativeElement, "top", `${top}px`);

            this.thumb.nativeElement.style.top = `${top}px`;

            this.updateStartFromThumb();
        }
    }

    private updateStartFromThumb()
    {
        let visible           = this.log.visible;
        let count             = this.log.count;
        let countMinusVisible = count - visible;

        let start: number;

        if (countMinusVisible <= 0)
        {
            start = 0;
        }
        else
        {
            // calculate the start position of the log based on thumb position
            let topRatio = safeDivision(this.thumbTop, this.maxThumbTop);
            start        = countMinusVisible * topRatio;
        }

        this.log.go(start);
    }

    private extractThumbTopFromElement(): number
    {
        return parseFloat(getComputedStyle(this.thumb.nativeElement).top);
    }
}

function safeDivision(num: number,
                      den: number): number
{
    return den != 0 ? num / den : 0;
}

function preventDefaultEvent(e: Event)
{
    e.preventDefault();
    e.stopPropagation();
}

/**
 * Holds information for an entry in the console.
 */
export class ConsoleLog
{
    private m_entries: IConsoleLogEntry[] = [];

    private m_filteredRanges: { index: number; range: IApplicationLogRange }[];

    private m_filter: ApplicationLogFilter;

    /**
     * The number of entries to show;
     */
    visible: number = 100;

    /**
     * The number of entries in the log.
     */
    count: number = 0;

    /**
     * The current start position of the log view.
     */
    start: number = 0;

    /**
     * The current end position of the log view.
     */
    end: number = 0;

    view: IConsoleLogEntry[] = [];

    private pendingRefresh: boolean;

    constructor(public provider: IConsoleLogProvider)
    {
    }

    /**
     * Returns true if the log has previous entries.
     */
    get hasPrevious(): boolean
    {
        return this.start > 0;
    }

    /**
     * Returns true if the log has more entries.
     */
    get hasNext(): boolean
    {
        return this.end < this.count;
    }

    /**
     * Forces the component to update the log entries.
     * @param {boolean} focus If true, it will move the view to the end of the log.
     */
    async refresh(focus: boolean)
    {
        if (this.m_filter)
        {
            await this.updateFilter();
        }
        else
        {
            this.count = this.provider.getLogCount();
        }

        if (focus)
        {
            this.goEnd(this.count);
        }
        else
        {
            // Update end.
            this.go(this.start);
        }
    }

    /**
     * Go to the next page.
     */
    next(singleRow: boolean,
         fullDocument: boolean)
    {
        if (this.hasNext)
        {
            if (fullDocument)
            {
                this.goEnd(this.count);
            }
            else
            {
                let shift = singleRow ? 1 : this.visible / 3;

                this.goEnd(this.end + shift);
            }
        }
    }

    /**
     * Go to the previous page.
     */
    previous(singleRow: boolean,
             fullDocument: boolean)
    {
        if (this.hasPrevious)
        {
            if (fullDocument)
            {
                this.go(0);
            }
            else
            {
                let shift = singleRow ? 1 : this.visible / 3;

                this.go(this.start - shift);
            }
        }
    }

    /**
     * Go to the start position specified.
     * @param start
     */
    go(start: number)
    {
        start = Math.floor(start);

        start = Math.min(start, this.count - this.visible);

        let newStart = Math.max(start, 0);
        let newEnd   = Math.min(newStart + this.visible, this.count);

        this.refreshView(newStart, newEnd);
    }

    /**
     * Go to the end position specified.
     * @param end
     */
    goEnd(end: number)
    {
        end = Math.floor(end);

        let newEnd   = Math.min(end, this.count);
        let newStart = Math.max(newEnd - this.visible, 0);

        this.refreshView(newStart, newEnd);
    }

    /**
     * Reset the log.
     */
    reset()
    {
        this.m_entries = [];

        this.refreshView(0, 0);
    }

    async filter(filters: ApplicationLogFilter)
    {
        if (filters)
        {
            let ranges         = await this.provider.performFilter(filters);
            let filteredRanges = [];
            let count          = 0;
            let start          = 0;
            for (let range of ranges)
            {
                for (let i = range.startOffset; i <= range.endOffset; i++)
                {
                    filteredRanges.push({
                                            index: i,
                                            range: range
                                        });
                    count++;

                    // Pick closest start index
                    if (Math.abs(i - this.start) < Math.abs(filteredRanges[start].index - this.start))
                    {
                        start = filteredRanges.length - 1;
                    }
                }
            }

            this.m_filter         = filters;
            this.m_filteredRanges = filteredRanges;
            this.count            = count;

            this.refreshView(0, 0);
            this.go(start);
        }
        else if (this.m_filteredRanges)
        {
            let correctedStart = this.start;
            let filteredRange  = this.m_filteredRanges[correctedStart];

            if (filteredRange) correctedStart = filteredRange.index;

            this.m_filter         = null;
            this.m_filteredRanges = null;
            this.count            = this.provider.getLogCount();
            this.go(correctedStart);
        }
    }

    private async updateFilter()
    {
        let filter = this.m_filter;
        let ranges = this.m_filteredRanges;
        if (filter && ranges)
        {
            let count     = this.count;
            let lastRange = ranges[ranges.length - 1];
            if (lastRange?.range)
            {
                filter.startOffset = lastRange.range.endOffset + 1;
            }

            let newRanges = await this.provider.performFilter(filter);
            for (let range of newRanges)
            {
                for (let i = range.startOffset; i <= range.endOffset; i++)
                {
                    ranges.push({
                                    index: i,
                                    range: range
                                });
                    count++;
                }
            }
            this.count            = count;
            this.m_filteredRanges = ranges;
        }
    }

    private refreshView(newStart: number,
                        newEnd: number)
    {
        if (isNaN(newStart) || isNaN(newEnd))
        {
            return;
        }

        if (this.start != newStart || this.end != newEnd)
        {
            this.start = newStart;
            this.end   = newEnd;

            if (!this.pendingRefresh)
            {
                this.pendingRefresh = true;
                this.recomputeView();
            }
        }
    }

    private async recomputeView()
    {
        while (true)
        {
            let pendingRanges: IApplicationLogRange[] = [];
            let lastRange: IApplicationLogRange       = null;

            const start = this.start;
            const end   = this.end;

            for (let pos = start; pos < end; pos++)
            {
                let lineOffset = this.getFilteredIndex(pos);
                let line       = this.m_entries[lineOffset];
                if (!line)
                {
                    if (lastRange?.endOffset == lineOffset)
                    {
                        lastRange.endOffset++;
                    }
                    else
                    {
                        lastRange = {
                            startOffset: lineOffset,
                            endOffset  : lineOffset + 1
                        };
                        pendingRanges.push(lastRange);
                    }
                }
            }

            if (pendingRanges.length > 0)
            {
                await Promise.all(pendingRanges.map(async (range) =>
                                                    {
                                                        let startFetch = range.startOffset;
                                                        let newEntries = await this.provider.getLogPage(startFetch, range.endOffset);
                                                        for (let newEntry of newEntries)
                                                        {
                                                            if (!newEntry) break;

                                                            this.m_entries[startFetch++] = newEntry;
                                                        }
                                                    }));
            }

            //
            // Because of async/await, multiple operations might overlap.
            // Keep looping until the local state matches the log's state.
            //
            if (this.start == start && this.end == end)
            {
                let view = [];

                for (let pos = start; pos < end; pos++)
                {
                    let lineOffset = this.getFilteredIndex(pos);
                    let line       = this.m_entries[lineOffset];
                    if (line) // Only add good lines.
                    {
                        view.push(line);
                    }
                }

                this.view           = view;
                this.pendingRefresh = false;
                return;
            }
        }
    }

    private getFilteredIndex(index: number): number
    {
        let filteredRanges = this.m_filteredRanges;
        return (filteredRanges && filteredRanges[index]) ? filteredRanges[index].index : index;
    }

    private getFilteredRange(index: number): IApplicationLogRange
    {
        let filteredRanges = this.m_filteredRanges;
        return (filteredRanges && filteredRanges[index]) ? filteredRanges[index].range : null;
    }
}
