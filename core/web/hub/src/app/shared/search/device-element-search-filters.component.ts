import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, OnDestroy, Output, SimpleChanges, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {ControlOption} from "framework/ui/control-option";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {ChipListOverlayItem} from "framework/ui/shared/chip-list-overlay.component";

@Component({
               selector       : "o3-device-element-search-filters",
               styleUrls      : ["./device-element-search-filters.component.scss"],
               templateUrl    : "./device-element-search-filters.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class DeviceElementSearchFiltersComponent extends SharedSvc.BaseApplicationComponent implements OnDestroy
{
    public static readonly searchFilterChipMaxWidth: number = 225; // keep synced with mat-chip:max-width in ./"".scss
    private static readonly minTextWidth: number            = 250;

    @Input() public label: string = "Enter your search...";
    @Input() public term: string;
    @Input() public model: Models.DeviceElementSearchRequestFilters;

    maxNumFilterChips: number = 3;

    @Output() public termChange  = new EventEmitter<string>();
    @Output() public modelChange = new EventEmitter<Models.DeviceElementSearchRequestFilters>();

    @ViewChild("filtersDialog", {static: true}) filtersDialog: OverlayComponent;
    @ViewChild("test_searchInput", {read: ElementRef}) test_searchInput: ElementRef;

    private formFieldElement: HTMLElement;

    public chips: AppliedSearchRequestFilter[]        = [];
    public filtersDialogConfig                        = OverlayConfig.onTopDraggable({width: "300px"});
    public samplingFilter: SamplingFilter             = null;
    public classificationFilter: ClassificationFilter = null;
    public visibilityFilter: VisibilityFilter         = null;

    private recalculator: number;

    static getDefaultFilters(): Models.DeviceElementSearchRequestFilters
    {
        return Models.DeviceElementSearchRequestFilters.newInstance({
                                                                        hasAnySampling: true,
                                                                        isClassified  : true,
                                                                        isNotHidden   : true
                                                                    });
    }

    constructor(inj: Injector,
                private elem: ElementRef)
    {
        super(inj);

        this.formFieldElement = elem.nativeElement.parentElement;
        this.updateMaxNumChips();
    }

    ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        // Update chips and config when filter model changes
        if (changes.model)
        {
            this.updateChips();
            this.updateConfig();
        }
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();

        this.updateMaxNumChips();
    }

    public ngOnDestroy(): void
    {
        super.ngOnDestroy();

        if (this.recalculator) clearTimeout(this.recalculator);
    }

    private updateMaxNumChips(): void
    {
        if (!this.formFieldElement) return;

        let inputWidth = this.formFieldElement.clientWidth;
        if (inputWidth === 0)
        {
            const checkIntervalMS = 250;
            this.recalculator     = setTimeout(() =>
                                               {
                                                   this.recalculator = null;
                                                   this.updateMaxNumChips();
                                               }, checkIntervalMS);
        }
        else
        {
            let availableWidth = inputWidth - DeviceElementSearchFiltersComponent.minTextWidth;
            let newMaxNum      = Math.floor(availableWidth / DeviceElementSearchFiltersComponent.searchFilterChipMaxWidth);
            if (newMaxNum !== this.maxNumFilterChips)
            {
                this.maxNumFilterChips = newMaxNum;
                this.markForCheck();
            }
        }
    }

    public removeFilter(chip: AppliedSearchRequestFilter | ChipListOverlayItem,
                        clickEvent?: MouseEvent)
    {
        if (clickEvent) clickEvent.stopPropagation();

        chip.remove();
        this.update();
    }

    public editFilters()
    {
        // Update config state and open the overlay
        this.updateConfig();
        this.filtersDialog.toggleOverlay();
    }

    public update()
    {
        this.updateChips();
        this.modelChange.emit(this.model);
    }

    private updateChips()
    {
        this.chips = [];

        // Check for sampling filter
        let sampling = new SamplingFilter(this.model);
        if (sampling.isApplied()) this.chips.push(sampling);

        // Check for classification filter
        let classification = new ClassificationFilter(this.model);
        if (classification.isApplied()) this.chips.push(classification);

        let visibility = new VisibilityFilter(this.model);
        if (visibility.isApplied()) this.chips.push(visibility);
    }

    private updateConfig()
    {
        if (this.model)
        {
            this.samplingFilter       = new SamplingFilter(this.model);
            this.classificationFilter = new ClassificationFilter(this.model);
            this.visibilityFilter     = new VisibilityFilter(this.model);
        }
        else
        {
            this.samplingFilter       = null;
            this.classificationFilter = null;
            this.visibilityFilter     = null;
        }
    }
}


abstract class AppliedSearchRequestFilter implements ChipListOverlayItem
{
    abstract label: string;

    constructor(public model: Models.DeviceElementSearchRequestFilters) {}

    abstract remove(): void;

    abstract isApplied(): boolean;
}

class SamplingFilter extends AppliedSearchRequestFilter
{
    public label: string = "Sampled";

    get value(): boolean
    {
        return this.model.hasAnySampling;
    }

    set value(state: boolean)
    {
        this.model.hasAnySampling = state;
    }

    remove()
    {
        this.model.hasAnySampling = false;
    }

    isApplied(): boolean
    {
        return !!this.model.hasAnySampling;
    }
}

class ClassificationFilter extends AppliedSearchRequestFilter
{
    public options: ControlOption<BooleanFilterState>[] = [
        new ControlOption(BooleanFilterState.Either, "All"),
        new ControlOption(BooleanFilterState.Yes, "Classified"),
        new ControlOption(BooleanFilterState.No, "Unclassified")
    ];

    get label(): string
    {
        let option = this.findOptionById(this.value);
        return option ? option.label : "";
    }

    get value(): BooleanFilterState
    {
        if (this.model.isClassified && !this.model.isUnclassified) return BooleanFilterState.Yes;
        if (!this.model.isClassified && this.model.isUnclassified) return BooleanFilterState.No;
        if (!this.model.isClassified && !this.model.isUnclassified) return BooleanFilterState.Either;
        return null;
    }

    set value(state: BooleanFilterState)
    {
        switch (state)
        {
            case BooleanFilterState.Yes:
                this.model.isClassified   = true;
                this.model.isUnclassified = false;
                break;

            case BooleanFilterState.No:
                this.model.isClassified   = false;
                this.model.isUnclassified = true;
                break;

            default:
                this.model.isClassified   = false;
                this.model.isUnclassified = false;
        }
    }

    remove()
    {
        this.model.isClassified   = false;
        this.model.isUnclassified = false;
    }

    isApplied(): boolean
    {
        return this.model.isClassified || this.model.isUnclassified;
    }

    findOptionById(state: BooleanFilterState)
    {
        return this.options.find((option) =>
                                 {
                                     return option.id === state;
                                 });
    }
}

class VisibilityFilter extends AppliedSearchRequestFilter
{
    public options: ControlOption<BooleanFilterState>[] = [
        new ControlOption(BooleanFilterState.Either, "All"),
        new ControlOption(BooleanFilterState.Yes, "Visible"),
        new ControlOption(BooleanFilterState.No, "Hidden")
    ];

    get label(): string
    {
        let option = this.findOptionById(this.value);
        return option ? option.label : "";
    }

    get value(): BooleanFilterState
    {
        if (this.model.isNotHidden && !this.model.isHidden) return BooleanFilterState.Yes;
        if (!this.model.isNotHidden && this.model.isHidden) return BooleanFilterState.No;
        if (!this.model.isNotHidden && !this.model.isHidden) return BooleanFilterState.Either;
        return null;
    }

    set value(state: BooleanFilterState)
    {
        switch (state)
        {
            case BooleanFilterState.Yes:
                this.model.isNotHidden = true;
                this.model.isHidden    = false;
                break;

            case BooleanFilterState.No:
                this.model.isNotHidden = false;
                this.model.isHidden    = true;
                break;

            default:
                this.model.isNotHidden = false;
                this.model.isHidden    = false;
        }
    }

    remove()
    {
        this.value = BooleanFilterState.Either;
    }

    isApplied(): boolean
    {
        return this.model.isNotHidden || this.model.isHidden;
    }

    findOptionById(state: BooleanFilterState)
    {
        return this.options.find((option) =>
                                 {
                                     return option.id === state;
                                 });
    }
}

enum BooleanFilterState
{
    Either,
    Yes,
    No
}
