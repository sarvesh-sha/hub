import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {CommonConfigurationModule} from "app/customer/configuration/common/common.module";
import {LocationDetailPageComponent} from "app/customer/configuration/locations/location-detail-page.component";
import {LocationFencesEditorComponent} from "app/customer/configuration/locations/location-fences-editor.component";
import {LocationListPageComponent} from "app/customer/configuration/locations/location-list-page.component";
import {LocationListComponent} from "app/customer/configuration/locations/location-list.component";
import {LocationsRoutingModule} from "app/customer/configuration/locations/locations-routing.module";
import {LocationWizardDialogComponent} from "app/customer/configuration/locations/wizard/location-wizard-dialog.component";
import {LocationWizardFieldsStep} from "app/customer/configuration/locations/wizard/location-wizard-fields-step.component";
import {LocationWizardInfoStep} from "app/customer/configuration/locations/wizard/location-wizard-info-step.component";
import {TimeRangeModule} from "app/shared/forms/time-range/time-range.module";
import {PinMapModule} from "app/shared/mapping/pin-map/pin-map.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  LocationFencesEditorComponent,
                  LocationWizardDialogComponent,
                  LocationDetailPageComponent,
                  LocationListComponent,
                  LocationListPageComponent,
                  LocationWizardFieldsStep,
                  LocationWizardInfoStep
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  CommonConfigurationModule,
                  LocationsRoutingModule,
                  PinMapModule,
                  TimeRangeModule
              ]
          })
export class LocationsModule {}
