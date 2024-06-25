import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {PreviewInvokerComponent} from "app/shared/utils/preview-invoker/preview-invoker.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [PreviewInvokerComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [PreviewInvokerComponent]
          })
export class PreviewInvokerModule {}
