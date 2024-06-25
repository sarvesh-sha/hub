import {CdkOverlayOrigin} from "@angular/cdk/overlay";
import {Component, Input, ViewChild} from "@angular/core";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-overlay-input",
               styleUrls  : ["./overlay-input.component.scss"],
               templateUrl: "./overlay-input.component.html"
           })
export class OverlayInputComponent
{
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_readonly: boolean | "";

    @Input() hintLabel: string;

    @Input() label: string;

    @Input() valueText: string;

    @Input() @CoerceBoolean() readonly: boolean;

    @Input() overlay: OverlayComponent;

    @ViewChild("overlayOrigin", {static: true}) overlayOrigin: CdkOverlayOrigin;

    @Input() iconClass: string;

    get isDialog(): boolean
    {
        return this.overlay != null && this.overlay.overlay != null && !this.overlay.overlayOrigin;
    }

    get hasIcon(): boolean
    {
        return !!this.iconClass;
    }

    toggleOverlay()
    {
        if (this.readonly)
        {
            return;
        }

        this.overlay.toggleOverlay(this.overlayOrigin);
    }
}
