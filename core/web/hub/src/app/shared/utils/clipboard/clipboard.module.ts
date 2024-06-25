import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {ClipboardCopyNotifyComponent} from "app/shared/utils/clipboard/clipboard-copy-notify.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [ClipboardCopyNotifyComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [ClipboardCopyNotifyComponent]
          })
export class ClipboardModule {}
