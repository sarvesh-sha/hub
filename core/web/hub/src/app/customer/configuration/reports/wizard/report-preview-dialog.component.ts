import {Component, Inject} from "@angular/core";
import {DomSanitizer, SafeResourceUrl} from "@angular/platform-browser";

import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./report-preview-dialog.component.html",
               styleUrls  : ["./report-preview-dialog.component.scss"]
           })
export class ReportPreviewDialogComponent
{
    private _reportUrl: SafeResourceUrl;

    get reportUrl(): SafeResourceUrl
    {
        if (!this.data.reportUrl) return null;

        if (!this._reportUrl)
        {
            this._reportUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.data.reportUrl);
        }
        return this._reportUrl;
    }

    constructor(public dialogRef: OverlayDialogRef<void>,
                @Inject(OVERLAY_DATA) public data: ReportPreviewDialogConfig,
                private sanitizer: DomSanitizer)
    {
    }

    static open(comp: BaseComponent,
                config: ReportPreviewDialogConfig,
                modal = false)
    {
        const overlayConfig = {
            width          : "90%",
            height         : "90%",
            maxWidth       : "1000px",
            showCloseButton: true
        };

        return OverlayComponent.open(comp, ReportPreviewDialogComponent, {
            config: modal ? OverlayConfig.newInstance(overlayConfig) : OverlayConfig.onTopDraggable(overlayConfig),
            data  : config
        });
    }
}

export interface ReportPreviewDialogConfig
{
    reportUrl?: string;
    failed?: boolean;
}
