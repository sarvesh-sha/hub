import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {InteractionBehaviorComponent} from "app/shared/interaction-handling/interaction-behavior.component";
import {TablesModule} from "app/shared/tables/tables.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [InteractionBehaviorComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  TablesModule
              ],
              exports     : [InteractionBehaviorComponent]
          })
export class InteractionHandlingModule {}
