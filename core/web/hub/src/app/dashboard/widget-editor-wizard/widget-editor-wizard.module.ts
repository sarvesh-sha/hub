import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {WidgetModule} from "app/dashboard/dashboard/widgets/widget.module";
import {ScheduleSelectorOverlayModule} from "app/dashboard/overlays/schedule-selector-overlay.module";
import {WidgetEditorWizardAggregationNodeConfigurerComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-aggregation-node-configurer.component";
import {WidgetEditorWizardAggregationRangeStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-aggregation-range-step.component";
import {WidgetEditorWizardAlertRangeStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-alert-range-step.component";
import {WidgetEditorWizardAlertRuleStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-alert-rule-step.component";
import {WidgetEditorWizardAlertSeverityStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-alert-severity-step.component";
import {WidgetEditorWizardAlertStatusStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-alert-status-step.component";
import {WidgetEditorWizardAlertTableDisplayStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-alert-table-display-step.component";
import {WidgetEditorWizardAlertTypeStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-alert-type-step.component";
import {WidgetEditorWizardControlPointGroupingStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-control-point-grouping-step.component";
import {WidgetEditorWizardControlPointStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-control-point-step.component";
import {WidgetEditorWizardDataAggregationTypeStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-data-aggregation-type-step.component";
import {WidgetEditorWizardDeviceStateStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-device-state-step.component";
import {WidgetEditorWizardDialogComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import {WidgetEditorWizardFontScalingStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-font-scaling-step.component";
import {WidgetEditorWizardGraphSelectorStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-graph-selector-step.component";
import {WidgetEditorWizardGraphsStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-graphs-step.component";
import {WidgetEditorWizardGridStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-grid-step.component";
import {WidgetEditorWizardImageStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-image-step.component";
import {WidgetEditorWizardInteractionBehaviorStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-interaction-behavior-step.component";
import {WidgetEditorWizardLocationsStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-locations-step.component";
import {WidgetEditorWizardManualFontScalingFieldComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-manual-font-scaling-field.component";
import {WidgetEditorWizardNameDescriptionStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-name-description-step.component";
import {WidgetEditorWizardPinConfigStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-pin-config-step.component";
import {WidgetEditorWizardRangesStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-ranges-step.component";
import {WidgetEditorWizardRefreshStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-refresh-step.component";
import {WidgetEditorWizardTextStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-text-step.component";
import {WidgetEditorWizardTimeSeriesGraphsComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-time-series-graphs-step.component";
import {WidgetEditorWizardTimeSeriesStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-time-series-step.component";
import {WidgetEditorWizardTypeStepComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-type-step.component";
import {WidgetEditorWizardWidgetPreviewComponent} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-widget-preview.component";
import {AlertSeverityModule} from "app/shared/alerts/alert-severity/alert-severity.module";
import {AggregationNodeBindingConfigurerModule} from "app/shared/assets/aggregation-node-binding/aggregation-node-binding-configurer.module";
import {AssetGraphSelectorModule} from "app/shared/assets/asset-graph-selectors/asset-graph-selector.module";
import {MultipleGraphConfigurationModule} from "app/shared/assets/configuration/multiple-graph-configuration.module";
import {ControlPointGroupingStepModule} from "app/shared/assets/control-point-grouping-step/control-point-grouping-step.module";
import {ChartingModule} from "app/shared/charting/charting.module";
import {DataSourceWizardModule} from "app/shared/charting/data-source-wizard/data-source-wizard.module";
import {ColorsModule} from "app/shared/colors/colors.module";
import {EquivalentUnitsSelectorModule} from "app/shared/dropdowns/equivalent-units-selector/equivalent-units-selector.module";
import {SelectorModule} from "app/shared/dropdowns/selector.module";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {AppImageModule} from "app/shared/image/app-image.module";
import {InteractionHandlingModule} from "app/shared/interaction-handling/interaction-handling.module";
import {OptionSelectorModule} from "app/shared/options/option-selector.module";
import {TablesModule} from "app/shared/tables/tables.module";

import {FrameworkUIModule} from "framework/ui";
import {CarouselModule} from "framework/ui/carousel/carousel.module";

@NgModule({
              declarations: [
                  WidgetEditorWizardAggregationNodeConfigurerComponent,
                  WidgetEditorWizardAggregationRangeStepComponent,
                  WidgetEditorWizardDataAggregationTypeStepComponent,
                  WidgetEditorWizardDialogComponent,
                  WidgetEditorWizardAlertRangeStepComponent,
                  WidgetEditorWizardAlertRuleStepComponent,
                  WidgetEditorWizardAlertSeverityStepComponent,
                  WidgetEditorWizardAlertStatusStepComponent,
                  WidgetEditorWizardAlertTableDisplayStepComponent,
                  WidgetEditorWizardAlertTypeStepComponent,
                  WidgetEditorWizardControlPointGroupingStepComponent,
                  WidgetEditorWizardControlPointStepComponent,
                  WidgetEditorWizardDeviceStateStepComponent,
                  WidgetEditorWizardDialogComponent,
                  WidgetEditorWizardFontScalingStepComponent,
                  WidgetEditorWizardGraphSelectorStepComponent,
                  WidgetEditorWizardGraphsStepComponent,
                  WidgetEditorWizardGridStepComponent,
                  WidgetEditorWizardImageStepComponent,
                  WidgetEditorWizardInteractionBehaviorStepComponent,
                  WidgetEditorWizardLocationsStepComponent,
                  WidgetEditorWizardManualFontScalingFieldComponent,
                  WidgetEditorWizardNameDescriptionStepComponent,
                  WidgetEditorWizardPinConfigStepComponent,
                  WidgetEditorWizardRangesStepComponent,
                  WidgetEditorWizardRefreshStepComponent,
                  WidgetEditorWizardTextStepComponent,
                  WidgetEditorWizardTimeSeriesGraphsComponent,
                  WidgetEditorWizardTimeSeriesStepComponent,
                  WidgetEditorWizardTypeStepComponent,
                  WidgetEditorWizardWidgetPreviewComponent,
                  WidgetEditorWizardTypeStepComponent,
                  WidgetEditorWizardTimeSeriesStepComponent
              ],
              imports     : [
                  AggregationNodeBindingConfigurerModule,
                  AlertSeverityModule,
                  AssetGraphSelectorModule,
                  CarouselModule,
                  ChartingModule,
                  ColorsModule,
                  CommonModule,
                  ControlPointGroupingStepModule,
                  DataSourceWizardModule,
                  EquivalentUnitsSelectorModule,
                  FrameworkUIModule,
                  AppImageModule,
                  InteractionHandlingModule,
                  MultipleGraphConfigurationModule,
                  OptionSelectorModule,
                  SelectorModule,
                  ScheduleSelectorOverlayModule,
                  TablesModule,
                  TimeRangeModule,
                  WidgetModule
              ],
              exports     : [
                  WidgetEditorWizardDialogComponent
              ]
          })
export class WidgetEditorWizardModule {}
