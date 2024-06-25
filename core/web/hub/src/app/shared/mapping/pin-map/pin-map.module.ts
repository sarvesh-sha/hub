import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {PinMapComponent} from "app/shared/mapping/pin-map/pin-map.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [PinMapComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [PinMapComponent]
          })
export class PinMapModule {}
