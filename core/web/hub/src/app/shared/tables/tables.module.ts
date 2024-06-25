import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {ControlOptionListComponent} from "app/shared/tables/control-option-list.component";
import {MapListActionsComponent} from "app/shared/tables/map-list-actions.component";
import {MapListComponent, MapListFormDirective} from "app/shared/tables/map-list.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  ControlOptionListComponent,
                  MapListActionsComponent,
                  MapListComponent,
                  MapListFormDirective
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [
                  ControlOptionListComponent,
                  MapListActionsComponent,
                  MapListComponent,
                  MapListFormDirective
              ]
          })
export class TablesModule {}
