import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {AlertMapComponent} from "app/shared/mapping/alert-map/alert-map.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [AlertMapComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [AlertMapComponent]
          })
export class AlertMapModule {}
