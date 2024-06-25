import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {AddWidgetOverlayComponent} from "app/shared/overlays/add-widget-overlay.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [AddWidgetOverlayComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [AddWidgetOverlayComponent]
          })
export class AddWidgetOverlayModule {}
