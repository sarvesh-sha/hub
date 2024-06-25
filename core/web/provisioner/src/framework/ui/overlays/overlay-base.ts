import {Directive, ViewChild} from "@angular/core";

import {BaseComponent} from "framework/ui/components";
import {OverlayComponent} from "./overlay.component";

@Directive()
export abstract class OverlayBase extends BaseComponent implements OverlayController
{
    @ViewChild(OverlayComponent) private dialog: OverlayComponent;

    public openOverlay()
    {
        if (this.dialog && !this.dialog.isOpen) this.dialog.toggleOverlay();
    }

    public closeOverlay()
    {
        if (this.dialog.isOpen) this.dialog.toggleOverlay();
    }

    public isOpen(): boolean
    {
        return this.dialog && this.dialog.isOpen;
    }

    public toggleOverlay(open?: boolean)
    {
        if (open === undefined || open !== this.dialog.isOpen)
        {
            this.dialog.toggleOverlay();
        }
    }
}

export interface OverlayController
{
    openOverlay(): void;

    closeOverlay(): void;

    toggleOverlay(open?: boolean): void;

    isOpen(): boolean;
}
