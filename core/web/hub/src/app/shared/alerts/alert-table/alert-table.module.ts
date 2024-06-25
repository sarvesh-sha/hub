import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AlertTableComponent} from "app/shared/alerts/alert-table/alert-table.component";

import {FrameworkUIModule} from "framework/ui";
import {SortModule} from "framework/ui/shared/sort/sort.module";

@NgModule({
              declarations: [AlertTableComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  SortModule
              ],
              exports     : [AlertTableComponent]
          })
export class AlertTableModule {}
