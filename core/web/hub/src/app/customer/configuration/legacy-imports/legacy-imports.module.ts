import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {LegacyImportsDetailPageComponent} from "app/customer/configuration/legacy-imports/legacy-imports-detail-page.component";
import {LegacyImportsListComponent} from "app/customer/configuration/legacy-imports/legacy-imports-list.component";
import {LegacyImportsRoutingModule} from "app/customer/configuration/legacy-imports/legacy-imports-routing.module";
import {LegacyImportsRunDialogComponent} from "app/customer/configuration/legacy-imports/legacy-imports-run-dialog.component";
import {LegacyImportsSummaryPageComponent} from "app/customer/configuration/legacy-imports/legacy-imports-summary-page.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  LegacyImportsListComponent,
                  LegacyImportsSummaryPageComponent,
                  LegacyImportsDetailPageComponent,
                  LegacyImportsRunDialogComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  LegacyImportsRoutingModule
              ]
          })
export class LegacyImportsModule {}
