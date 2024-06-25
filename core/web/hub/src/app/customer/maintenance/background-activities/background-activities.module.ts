import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {BackgroundActivitiesListComponent} from "app/customer/maintenance/background-activities/background-activities-list.component";
import {BackgroundActivitiesRoutingModule} from "app/customer/maintenance/background-activities/background-activities-routing.module";
import {BackgroundActivitiesSummaryPageComponent} from "app/customer/maintenance/background-activities/background-activities-summary-page.component";
import {BackgroundActivityDetailPageComponent} from "app/customer/maintenance/background-activities/background-activity-detail-page.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  BackgroundActivitiesSummaryPageComponent,
                  BackgroundActivitiesListComponent,
                  BackgroundActivityDetailPageComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  BackgroundActivitiesRoutingModule
              ]
          })
export class BackgroundActivitiesModule {}
