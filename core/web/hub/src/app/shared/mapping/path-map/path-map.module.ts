import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {PathMapComponent} from "app/shared/mapping/path-map/path-map.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [PathMapComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [PathMapComponent]
          })
export class PathMapModule {}
