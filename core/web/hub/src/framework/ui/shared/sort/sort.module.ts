import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {FrameworkUIModule} from "framework/ui/index";
import {SortArrowComponent} from "framework/ui/shared/sort/sort-arrow.component";
import {SortConfigurerComponent} from "framework/ui/shared/sort/sort-configurer.component";

@NgModule({
              declarations: [
                  SortArrowComponent,
                  SortConfigurerComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [
                  SortArrowComponent,
                  SortConfigurerComponent
              ]
          })
export class SortModule {}
