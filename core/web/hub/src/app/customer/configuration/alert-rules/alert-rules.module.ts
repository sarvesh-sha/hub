import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {AlertRuleDetailsPageComponent} from "app/customer/configuration/alert-rules/alert-rule-details-page.component";
import {AlertRuleListPageComponent} from "app/customer/configuration/alert-rules/alert-rule-list-page.component";
import {AlertRulesRoutingModule} from "app/customer/configuration/alert-rules/alert-rules-routing.module";
import {AlertRuleInputFieldComponent} from "app/customer/configuration/alert-rules/wizard/alert-rule-input-field.component";
import {AlertRuleWizardDialogComponent} from "app/customer/configuration/alert-rules/wizard/alert-rule-wizard-dialog.component";
import {AlertRuleWizardGraphStepComponent} from "app/customer/configuration/alert-rules/wizard/alert-rule-wizard-graph-step.component";
import {AlertRuleWizardInputStepComponent} from "app/customer/configuration/alert-rules/wizard/alert-rule-wizard-input-step.component";
import {AlertRuleWizardLibraryStepComponent} from "app/customer/configuration/alert-rules/wizard/alert-rule-wizard-library-step.component";
import {AlertRuleWizardNameStepComponent} from "app/customer/configuration/alert-rules/wizard/alert-rule-wizard-name-step.component";
import {CommonConfigurationModule} from "app/customer/configuration/common/common.module";
import {ReportLivePreviewModule} from "app/customer/configuration/reports/report-live-preview/report-live-preview.module";
import {EnginesModule} from "app/customer/engines/engines.module";
import {AssetGraphStepModule} from "app/shared/assets/asset-graph-step/asset-graph-step.module";
import {DirectivesModule} from "app/shared/directives/directives.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {SearchModule} from "app/shared/search/search.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  AlertRuleListPageComponent,
                  AlertRuleWizardDialogComponent,
                  AlertRuleWizardNameStepComponent,
                  AlertRuleInputFieldComponent,
                  AlertRuleWizardGraphStepComponent,
                  AlertRuleWizardInputStepComponent,
                  AlertRuleWizardLibraryStepComponent,
                  AlertRuleDetailsPageComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  EnginesModule,
                  ReportLivePreviewModule,
                  CommonConfigurationModule,
                  AlertRulesRoutingModule,
                  AssetGraphStepModule,
                  TimeRangeModule,
                  DirectivesModule,
                  SearchModule
              ]
          })
export class AlertRulesModule {}
