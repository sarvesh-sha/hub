import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {ContentWithSubtextComponent} from "framework/ui/layout-helpers/content-with-subtext.component";

@NgModule({
              declarations: [ContentWithSubtextComponent],
              imports     : [CommonModule],
              exports     : [ContentWithSubtextComponent]
          })
export class LayoutHelpersModule {}
