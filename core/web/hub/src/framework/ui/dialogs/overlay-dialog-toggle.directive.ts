import {Directive, HostListener, Input} from "@angular/core";
import {OverlayController} from "framework/ui/overlays/overlay-base";

import {OverlayComponent} from "framework/ui/overlays/overlay.component";

@Directive({
               selector: "[o3-dialog-toggle]",
               exportAs: "o3-dialog-toggle"
           })
export class OverlayDialogToggleDirective
{
    @Input("o3-dialog-toggle") dialog: OverlayComponent | OverlayController;

    @Input() stopEventPropagation: boolean = false;

    @HostListener("click", ["$event"]) onClick($event: Event)
    {
        if (!this.dialog) return;

        this.dialog.toggleOverlay();

        if (this.stopEventPropagation)
        {
            $event.stopPropagation();
        }
    }
}
