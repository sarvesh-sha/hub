import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {WorkflowWizardModule} from "app/customer/workflows/wizard/workflow-wizard.module";
import {WorkflowsDetailPageComponent} from "app/customer/workflows/workflows-detail-page.component";
import {WorkflowsListComponent} from "app/customer/workflows/workflows-list.component";
import {WorkflowsRoutingModule} from "app/customer/workflows/workflows-routing.module";
import {WorkflowsSummaryPageComponent} from "app/customer/workflows/workflows-summary-page.component";
import {DirectivesModule} from "app/shared/directives/directives.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {FilterModule} from "app/shared/filter/filter.module";
import {TimelineModule} from "app/shared/timelines/timeline.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  WorkflowsDetailPageComponent,
                  WorkflowsListComponent,
                  WorkflowsSummaryPageComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  DirectivesModule,
                  WorkflowWizardModule,
                  WorkflowsRoutingModule,
                  TimelineModule,
                  FilterModule,
                  SelectorModule
              ]
          })
export class WorkflowsModule {}
