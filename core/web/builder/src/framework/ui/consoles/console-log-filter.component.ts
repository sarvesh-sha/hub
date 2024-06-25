import {Component, EventEmitter, Injector, Output, ViewChild} from "@angular/core";
import {BaseComponent} from "framework/ui/components";
import {ApplicationLogFilter} from "framework/ui/consoles/console-log";
import {ControlOption} from "framework/ui/control-option";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";
import {Subject} from "rxjs";
import {debounceTime} from "rxjs/operators";

@Component({
               selector   : "o3-console-log-filter",
               styleUrls  : ["./console-log-filter.component.scss"],
               templateUrl: "./console-log-filter.component.html"
           })
export class ConsoleLogFilterComponent extends BaseComponent
{
    @Output() requestChange = new EventEmitter<ApplicationLogFilter>();

    @ViewChild(StandardFormOverlayComponent) overlay: StandardFormOverlayComponent;

    pristine: boolean;
    hasFilters: boolean;

    editRequest = new ApplicationLogFilter();

    request = new ApplicationLogFilter();

    chips: FilterChip[] = [];

    levels = [
        new ControlOption<string>("DebugObnoxious", "DebugObnoxious"),
        new ControlOption<string>("DebugVerbose", "DebugVerbose"),
        new ControlOption<string>("Debug", "Debug"),
        new ControlOption<string>("Info", "Info"),
        new ControlOption<string>("Warn", "Warn"),
        new ControlOption<string>("Error", "Error")
    ];

    overlayConfig = OverlayConfig.onTopDraggable({
                                                     width    : 325,
                                                     maxHeight: "90vh"
                                                 });

    filterText: string;

    filterUpdated = new Subject<void>();

    constructor(inj: Injector)
    {
        super(inj);

        this.subscribeToObservable(this.filterUpdated.pipe(debounceTime(250)), () => this.onFilterUpdate());
        this.chips.push(new LogFilterChip("level", () =>
        {
            this.editRequest.levels = [];
            this.applyFilterEdits();
        }, () => this.request.levels));
        this.chips.push(new LogFilterChip("host", () =>
        {
            this.editRequest.hosts = [];
            this.applyFilterEdits();
        }, () => this.request.hosts));
        this.chips.push(new LogFilterChip("selector", () =>
        {
            this.editRequest.selectors = [];
            this.applyFilterEdits();
        }, () => this.request.selectors));
        this.chips.push(new LogFilterChip("thread", () =>
        {
            this.editRequest.threads = [];
            this.applyFilterEdits();
        }, () => this.request.threads));
    }

    public closeOverlay(): void
    {
        this.overlay?.closeOverlay();
    }

    public isOpen(): boolean
    {
        return this.overlay?.isOpen();
    }

    public openOverlay(): void
    {
        this.editRequest = this.request.clone();
        this.updatePristine();
        this.overlay?.openOverlay();
    }

    public updateFilters(callback: (filters: ApplicationLogFilter) => void)
    {
        callback(this.editRequest);
        this.applyFilterEdits();
    }

    //--//

    onFilterUpdate()
    {
        this.applyFilterEdits();
    }

    public applyFilterEdits()
    {
        this.request = this.editRequest.clone();
        this.updatePristine();
        this.emitChange();
    }

    public resetEditRequest()
    {
        this.editRequest = new ApplicationLogFilter();
        this.updatePristine();
    }

    public updatePristine()
    {
        this.pristine   = this.editRequest.equals(this.request);
        this.hasFilters = !this.editRequest.equals(new ApplicationLogFilter());
    }

    private emitChange()
    {
        this.requestChange.emit(this.hasFilters ? this.request : null);
    }
}


export class LogFilterChip extends FilterChip
{
    constructor(label: string,
                filterClear: () => void,
                filterGetter: () => string[])
    {
        super(label, filterClear, filterGetter, null);
    }
}

