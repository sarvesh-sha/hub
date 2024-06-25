import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {PaneDetailsPageComponent} from "app/customer/configuration/panes/pane-details-page.component";
import {PaneFieldWizardDetailsStepComponent} from "app/customer/configuration/panes/wizard/pane-field-wizard/pane-field-wizard-details-step.component";
import {PaneFieldWizardTypeStepComponent} from "app/customer/configuration/panes/wizard/pane-field-wizard/pane-field-wizard-type-step.component";
import {PaneFieldWizardComponent} from "app/customer/configuration/panes/wizard/pane-field-wizard/pane-field-wizard.component";
import {PaneFieldsEditorComponent} from "app/customer/configuration/panes/wizard/pane-fields-editor.component";
import {PaneWizardDialogComponent} from "app/customer/configuration/panes/wizard/pane-wizard-dialog.component";
import {PaneWizardFieldsStepComponent} from "app/customer/configuration/panes/wizard/pane-wizard-fields-step.component";
import {PaneWizardGraphStepComponent} from "app/customer/configuration/panes/wizard/pane-wizard-graph-step.component";
import {PaneWizardNameStepComponent} from "app/customer/configuration/panes/wizard/pane-wizard-name-step.component";
import {PaneListPageComponent} from "app/customer/configuration/panes/pane-list-page.component";
import {PanesRoutingModule} from "app/customer/configuration/panes/panes-routing.module";
import {ContextPaneModule} from "app/dashboard/context-pane";
import {AssetGraphSelectorModule} from "app/shared/assets/asset-graph-selectors/asset-graph-selector.module";
import {AssetGraphStepModule} from "app/shared/assets/asset-graph-step/asset-graph-step.module";
import {ControlPointGroupingStepModule} from "app/shared/assets/control-point-grouping-step/control-point-grouping-step.module";
import {TimeSeriesContainerModule} from "app/shared/charting/time-series-container/time-series-container.module";
import {EquivalentUnitsSelectorModule} from "app/shared/dropdowns/equivalent-units-selector/equivalent-units-selector.module";
import {BrandingConfigurationModule} from "app/shared/forms/branding-configuration/branding-configuration.module";
import {TablesModule} from "app/shared/tables/tables.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  PaneDetailsPageComponent,
                  PaneFieldsEditorComponent,
                  PaneListPageComponent,
                  PaneFieldWizardComponent,
                  PaneFieldWizardDetailsStepComponent,
                  PaneFieldWizardTypeStepComponent,
                  PaneWizardDialogComponent,
                  PaneWizardFieldsStepComponent,
                  PaneWizardGraphStepComponent,
                  PaneWizardNameStepComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  ContextPaneModule,
                  PanesRoutingModule,
                  BrandingConfigurationModule,
                  AssetGraphSelectorModule,
                  AssetGraphStepModule,
                  ControlPointGroupingStepModule,
                  EquivalentUnitsSelectorModule,
                  TimeSeriesContainerModule,
                  TablesModule
              ]
          })
export class PanesModule {}
