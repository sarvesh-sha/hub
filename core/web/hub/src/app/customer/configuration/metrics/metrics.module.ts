import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {MetricDefinitionDetailsPageComponent} from "app/customer/configuration/metrics/metric-definition-details-page.component";
import {MetricListPageComponent} from "app/customer/configuration/metrics/metric-list-page.component";
import {MetricsRoutingModule} from "app/customer/configuration/metrics/metrics-routing.module";
import {MetricWizardDialogComponent} from "app/customer/configuration/metrics/wizard/metric-wizard-dialog.component";
import {MetricWizardGraphStepComponent} from "app/customer/configuration/metrics/wizard/metric-wizard-graph-step.component";
import {MetricWizardInputFieldComponent} from "app/customer/configuration/metrics/wizard/metric-wizard-input-field.component";
import {MetricWizardNameStepComponent} from "app/customer/configuration/metrics/wizard/metric-wizard-name-step.component";
import {EnginesModule} from "app/customer/engines/engines.module";
import {AssetGraphStepModule} from "app/shared/assets/asset-graph-step/asset-graph-step.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {UnitEditorModule} from "app/shared/units/unit-editor.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  MetricListPageComponent,
                  MetricDefinitionDetailsPageComponent,
                  MetricWizardDialogComponent,
                  MetricWizardGraphStepComponent,
                  MetricWizardInputFieldComponent,
                  MetricWizardNameStepComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  EnginesModule,
                  MetricsRoutingModule,
                  AssetGraphStepModule,
                  TimeRangeModule,
                  UnitEditorModule
              ]
          })
export class MetricsModule {}
