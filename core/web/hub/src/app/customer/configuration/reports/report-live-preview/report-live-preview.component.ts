import {Component, Input, ViewChild, ViewContainerRef} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";
import {ReportConfig} from "app/services/domain/reporting.service";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";

@Component({
               selector   : "o3-report-live-preview",
               templateUrl: "./report-live-preview.component.html"
           })
export class ReportLivePreviewComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() public config: ReportConfig;

    @ViewChild(StandardFormOverlayComponent, {static: true}) reportPreviewDialog: StandardFormOverlayComponent;

    @ViewChild("reportContainer", {read: ViewContainerRef}) reportContainer: ViewContainerRef;

    reportPreviewDialogConfig = OverlayConfig.onTopDraggable({
                                                                 minWidth : "80%",
                                                                 minHeight: "85%"
                                                             });

    secondaryButtonText: string;
    secondaryButtonCallback: () => void;
    secondaryButtonIsDisabled: () => boolean;

    public toggleOverlay(): void
    {
        this.reportPreviewDialog.toggleOverlay();
    }

    public registerSecondaryButton(text: string,
                                   onClick: () => void,
                                   isDisabled?: () => boolean)
    {
        this.secondaryButtonText       = text;
        this.secondaryButtonCallback   = onClick;
        this.secondaryButtonIsDisabled = isDisabled;
    }

    public unregisterSecondaryButton()
    {
        this.secondaryButtonText       = null;
        this.secondaryButtonCallback   = null;
        this.secondaryButtonIsDisabled = null;
        this.detectChanges();
    }
}
