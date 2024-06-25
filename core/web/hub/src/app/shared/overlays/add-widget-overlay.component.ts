import {Component, EventEmitter, Output, ViewChild} from "@angular/core";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";

@Component({
               selector   : "o3-add-widget-overlay",
               templateUrl: "./add-widget-overlay.component.html"
           })
export class AddWidgetOverlayComponent
{
    overlayConfig = OverlayConfig.newInstance({minWidth: "400px"});

    widgetTitle: string = "";

    @ViewChild(StandardFormOverlayComponent, {static: true}) overlay: StandardFormOverlayComponent;

    @Output() addWidgetNotifier: EventEmitter<string> = new EventEmitter<string>();

    notifyToAddWidget(): void
    {
        this.addWidgetNotifier.emit(this.widgetTitle);
    }
}
