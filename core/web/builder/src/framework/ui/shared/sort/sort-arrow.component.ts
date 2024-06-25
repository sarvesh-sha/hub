import {Attribute, Component, ElementRef, EventEmitter, Input, Output} from "@angular/core";

export type SortArrowPosition = "left" | "right";
export type SortArrowDirection = "asc" | "desc";

export function getSortDirection(ascending: boolean): SortArrowDirection
{
    return ascending ? "asc" : "desc";
}

@Component({
               selector   : "o3-sort-arrow",
               templateUrl: "./sort-arrow.component.html",
               styleUrls  : ["./sort-arrow.component.scss"]
           })
export class SortArrowComponent
{
    @Input() active: boolean;

    @Input() set initialDirection(initialDirection: SortArrowDirection)
    {
        if (this.ascending == null && initialDirection)
        {
            this.ascending = initialDirection === "asc";
        }
    }

    @Output() directionChange = new EventEmitter<SortArrowDirection>();

    ascending: boolean;

    readonly firstToggleAscending: boolean;

    constructor(public test_elemRef: ElementRef,
                @Attribute("position") public position: SortArrowPosition,
                @Attribute("firstToggle") firstToggle: SortArrowDirection)
    {
        if (!firstToggle) firstToggle = "asc";

        this.firstToggleAscending = firstToggle === "asc";
    }

    toggleDirection()
    {
        if (!this.active || this.ascending == null)
        {
            this.ascending = this.firstToggleAscending;
        }
        else
        {
            this.ascending = !this.ascending;
        }

        this.directionChange.emit(getSortDirection(this.ascending));
    }
}
