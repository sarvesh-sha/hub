import {Component, DoCheck, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import {OverlayController} from "framework/ui/overlays/overlay-base";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-chip-list-overlay[label][chips]",
               templateUrl: "./chip-list-overlay.component.html",
               styleUrls  : ["./chip-list-overlay.component.scss"]
           })
export class ChipListOverlayComponent implements OverlayController,
                                                 DoCheck
{
    @Input() label: string;
    @Input() chips: ChipListOverlayItem[] = [];
    @Input() withTooltip: boolean         = false;

    @ViewChild(OverlayComponent, {static: true}) overlay: OverlayComponent;

    @Output() chipDeleted = new EventEmitter<ChipListOverlayItem>();

    private numChips: number = 0;

    overlayConfig = OverlayConfig.onTopDraggable();

    ngDoCheck()
    {
        let numChips = this.chips?.length || 0;

        if (numChips !== this.numChips)
        {
            if (numChips > this.numChips) this.overlay?.pullIntoView(0);

            this.numChips = numChips;
        }
    }

    public deleteChip(deleteIndex: number)
    {
        if (this.numChips === 1) this.closeOverlay();

        let chip = this.chips.splice(deleteIndex, 1)[0];
        this.chipDeleted.emit(chip);
    }

    public closeOverlay(): void
    {
        this.overlay.closeOverlay();
    }

    public isOpen(): boolean
    {
        return this.overlay.isOpen;
    }

    public openOverlay(): void
    {
        if (!this.isOpen()) this.toggleOverlay();
    }

    public toggleOverlay(open?: boolean): void
    {
        if (open === undefined || open !== this.overlay.isOpen) this.overlay.toggleOverlay();
    }
}

export interface ChipListOverlayItem
{
    label: string;

    tooltip?: string;

    remove(): void;
}
