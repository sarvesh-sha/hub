import {Attribute, Component, EventEmitter, Input, Output} from "@angular/core";

export type SortArrowDirection = "asc" | "desc";
export type SortArrowPosition = "left" | "right";

@Component({
               selector   : "o3-sort-arrow",
               templateUrl: "./sort-arrow.component.html",
               styleUrls  : ["./sort-arrow.component.scss"]
           })
export class SortArrowComponent
{
    @Input() visible: boolean;
    @Input() optio3TestValue: string = '';
    @Input() set initialDirection(initialDirection: SortArrowDirection)
    {
        if (!this.direction) this.direction = initialDirection;
    }

    direction: SortArrowDirection;
    @Output() directionChange: EventEmitter<SortArrowDirection> = new EventEmitter();

    constructor(@Attribute("position") public position: SortArrowPosition,
                @Attribute("firstToggle") public firstToggle: SortArrowDirection)
    {
        if (!this.firstToggle) this.firstToggle = "asc";
    }

    toggleDirection()
    {
        if (!this.visible)
        {
            this.direction = this.firstToggle;
        }
        else
        {
            this.direction = (this.direction == "asc") ? "desc" : "asc";
        }

        this.directionChange.emit(this.direction);
    }
}
