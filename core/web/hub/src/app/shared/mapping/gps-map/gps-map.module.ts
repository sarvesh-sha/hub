import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {AssetGraphWizardModule} from "app/shared/assets/asset-graph-wizard/asset-graph-wizard.module";
import {ColorsModule} from "app/shared/colors/colors.module";
import {EquivalentUnitsSelectorModule} from "app/shared/dropdowns/equivalent-units-selector/equivalent-units-selector.module";
import {GpsMapConfigurerComponent} from "app/shared/mapping/gps-map/gps-map-configurer.component";
import {GpsMapComponent} from "app/shared/mapping/gps-map/gps-map.component";
import {TimeScrubberComponent} from "app/shared/mapping/gps-map/time-scrubber.component";
import {PathMapModule} from "app/shared/mapping/path-map/path-map.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  GpsMapComponent,
                  GpsMapConfigurerComponent,
                  TimeScrubberComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  PathMapModule,
                  ColorsModule,
                  EquivalentUnitsSelectorModule,
                  AssetGraphWizardModule
              ],
              exports     : [GpsMapComponent]
          })
export class GpsMapModule {}
