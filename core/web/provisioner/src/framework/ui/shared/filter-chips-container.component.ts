import {Component, ElementRef, EventEmitter, Injector, Input, Output} from "@angular/core";
import {UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";
import {ChipListOverlayItem} from "framework/ui/shared/chip-list-overlay.component";

@Component({
               selector   : "o3-filter-chips-container",
               templateUrl: "./filter-chips-container.component.html",
               styleUrls  : ["./filter-chips-container.component.scss"]
           })
export class FilterChipsContainerComponent extends BaseComponent
{
    private static readonly chipMaxWidth: number = 220; // keep synced with mat-chip max width

    @Input() chips: FilterChip[] = [];

    maxNumChips: number = 1;

    get activeChips(): FilterChip[]
    {
        return (this.chips || []).filter((chip) => chip.isActive());
    }

    get numChips(): number
    {
        return this.activeChips.length;
    }

    @Output() editFilterRequest: EventEmitter<void> = new EventEmitter<void>();

    constructor(inj: Injector,
                private element: ElementRef)
    {
        super(inj);
    }

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        setTimeout(this.updateMaxNumChips.bind(this));
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();

        this.updateMaxNumChips();
    }

    private updateMaxNumChips()
    {
        // more sophisticated logic would be able to fit more
        const iconButtonWidthPx = 40 + 4; // 40px + 4px right margin
        let parentWidth         = this.element.nativeElement.parentElement?.clientWidth || iconButtonWidthPx;
        this.maxNumChips        = Math.floor((parentWidth - iconButtonWidthPx) / FilterChipsContainerComponent.chipMaxWidth);
    }

    hasFilter(chip: FilterChip): boolean
    {
        return chip.isActive();
    }

    filterText(chip: FilterChip): string
    {
        return chip.getTooltipInfo();
    }

    clearFilter(chip: FilterChip | ChipListOverlayItem,
                e?: MouseEvent)
    {
        if (e) e.stopPropagation();
        chip.remove();
    }

    getLabel(chip: FilterChip): string
    {
        return chip.label || "";
    }
}

export class FilterChip implements ChipListOverlayItem
{
    protected idToLabel: { [id: string]: string } = {};
    private filterIDs: string[]                   = [];
    private filterNames: string[]                 = [];

    get label(): string
    {
        let filterNames = this.getFilterNames();
        let label       = "";
        if (this.showCountInLabel) label += filterNames.length + " ";
        label += this.pluralize ? UtilsService.pluralize(this.categoryLabel, filterNames.length) : this.categoryLabel;
        return label;
    }

    get tooltip(): string
    {
        return this.getTooltipInfo();
    }

    constructor(public readonly categoryLabel: string,
                public readonly remove: () => void,
                protected readonly filterGetter: () => string[],
                mappingInfo: { [id: string]: string } | ControlOption<string>[] = {},
                protected readonly pluralize: boolean                           = true,
                protected readonly showCountInLabel: boolean                    = true)
    {
        if (mappingInfo instanceof Array)
        {
            this.addToMapping(mappingInfo);
        }
        else
        {
            this.idToLabel = mappingInfo;
        }
    }

    private addToMapping(options: ControlOption<string>[])
    {
        for (let option of options || [])
        {
            this.idToLabel[option.id] = option.label;
            this.addToMapping(option.children);
        }
    }

    public isActive(): boolean
    {
        let filterNames = this.filterGetter();
        return filterNames && filterNames.length > 0;
    }

    public getTooltipInfo(): string
    {
        return this.getFilterNames()
                   .join(", ");
    }

    private getFilterNames(): string[]
    {
        let filterNames = this.filterGetter();
        if (filterNames instanceof Array)
        {
            if (this.idToLabel) this.filterNames = filterNames.map((id: string) => this.idToLabel[id] || id);

            this.filterIDs = filterNames;
        }

        return this.idToLabel ? this.filterNames : this.filterIDs;
    }
}

