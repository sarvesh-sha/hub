import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {CustomReportFieldWizardAggregatedValueStepComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard-aggregated-value-step.component";
import {CustomReportFieldWizardAggregationTrendStepComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard-aggregation-trend-step.component";
import {CustomReportFieldWizardAlertFeedStepComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard-alert-feed-step.component";
import {CustomReportFieldWizardAlertTableStepComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard-alert-table-step.component";
import {CustomReportFieldWizardChartSetStepComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard-chart-set-step.component";
import {CustomReportFieldWizardDataAggregationStepComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard-data-aggregation-step.component";
import {CustomReportFieldWizardDeviceElementListStepComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard-device-element-list-step.component";
import {CustomReportFieldWizardGraphStepComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard-graph-step.component";
import {CustomReportFieldWizardNameStepComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard-name-step.component";
import {CustomReportFieldWizardRichTextStepComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard-rich-text-step.component";
import {CustomReportFieldWizardTypeStepComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard-type-step.component";
import {CustomReportFieldWizardComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.component";
import {AlertSeverityModule} from "app/shared/alerts/alert-severity/alert-severity.module";
import {AggregationNodeBindingConfigurerModule} from "app/shared/assets/aggregation-node-binding/aggregation-node-binding-configurer.module";
import {AssetGraphSelectorModule} from "app/shared/assets/asset-graph-selectors/asset-graph-selector.module";
import {MultipleGraphConfigurationModule} from "app/shared/assets/configuration/multiple-graph-configuration.module";
import {ControlPointGroupingStepModule} from "app/shared/assets/control-point-grouping-step/control-point-grouping-step.module";
import {ChartingModule} from "app/shared/charting/charting.module";
import {ColorsModule} from "app/shared/colors/colors.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {TablesModule} from "app/shared/tables/tables.module";

import {FrameworkUIModule} from "framework/ui";
import {MarkdownModule} from "framework/ui/markdown/markdown.module";

@NgModule({
              declarations: [
                  CustomReportFieldWizardAggregationTrendStepComponent,
                  CustomReportFieldWizardAggregatedValueStepComponent,
                  CustomReportFieldWizardAlertFeedStepComponent,
                  CustomReportFieldWizardAlertTableStepComponent,
                  CustomReportFieldWizardChartSetStepComponent,
                  CustomReportFieldWizardComponent,
                  CustomReportFieldWizardDataAggregationStepComponent,
                  CustomReportFieldWizardDeviceElementListStepComponent,
                  CustomReportFieldWizardGraphStepComponent,
                  CustomReportFieldWizardNameStepComponent,
                  CustomReportFieldWizardRichTextStepComponent,
                  CustomReportFieldWizardTypeStepComponent
              ],
              imports     : [
                  AggregationNodeBindingConfigurerModule,
                  AlertSeverityModule,
                  AssetGraphSelectorModule,
                  ChartingModule,
                  ColorsModule,
                  CommonModule,
                  ControlPointGroupingStepModule,
                  FrameworkUIModule,
                  MarkdownModule,
                  MultipleGraphConfigurationModule,
                  SelectorModule,
                  TablesModule
              ],
              exports     : [CustomReportFieldWizardComponent]
          })
export class CustomReportFieldWizardModule {}
