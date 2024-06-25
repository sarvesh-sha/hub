import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {DashboardBannerComponent} from "app/dashboard/dashboard/dashboard-banner.component";
import {DashboardPageComponent} from "app/dashboard/dashboard/dashboard-page.component";
import {DashboardRoutingModule} from "app/dashboard/dashboard/dashboard-routing.module";
import {DashboardToolbarComponent} from "app/dashboard/dashboard/dashboard-toolbar.component";
import {WidgetModule} from "app/dashboard/dashboard/widgets/widget.module";
import {SimulatedDataDialogComponent} from "app/dashboard/experiments/simulated-data-dialog.component";
import {WidgetEditorWizardModule} from "app/dashboard/widget-editor-wizard/widget-editor-wizard.module";
import {MultipleGraphConfigurationModule} from "app/shared/assets/configuration/multiple-graph-configuration.module";
import {ColorsModule} from "app/shared/colors/colors.module";
import {BrandingConfigurationModule} from "app/shared/forms/branding-configuration/branding-configuration.module";
import {AppImageModule} from "app/shared/image/app-image.module";
import {UndoModule} from "app/shared/undo/undo.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  DashboardBannerComponent,
                  DashboardPageComponent,
                  SimulatedDataDialogComponent,
                  DashboardToolbarComponent
              ],
              imports: [
                  CommonModule,
                  FrameworkUIModule,
                  WidgetModule,
                  DashboardRoutingModule,
                  BrandingConfigurationModule,
                  MultipleGraphConfigurationModule,
                  ColorsModule,
                  UndoModule,
                  WidgetEditorWizardModule
              ]
          })
export class DashboardModule {}
