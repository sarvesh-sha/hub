import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {ColorGradientLegendBarsComponent} from "app/shared/colors/color-gradient-legend-bars.component";
import {ColorGradientLegendComponent} from "app/shared/colors/color-gradient-legend.component";

import {ColorPaletteComponent} from "app/shared/colors/color-palette.component";
import {ColorPickerConfigurationComponent} from "app/shared/colors/color-picker-configuration.component";
import {ColorPickerFlatComponent} from "app/shared/colors/color-picker-flat.component";
import {ColorPickerGradientStopComponent} from "app/shared/colors/color-picker-gradient-stop.component";
import {ColorPickerGradientComponent} from "app/shared/colors/color-picker-gradient.component";
import {ColorPickerComponent} from "app/shared/colors/color-picker.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  ColorGradientLegendComponent,
                  ColorGradientLegendBarsComponent,
                  ColorPaletteComponent,
                  ColorPickerComponent,
                  ColorPickerConfigurationComponent,
                  ColorPickerFlatComponent,
                  ColorPickerGradientComponent,
                  ColorPickerGradientStopComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [
                  ColorGradientLegendComponent,
                  ColorPaletteComponent,
                  ColorPickerComponent,
                  ColorPickerConfigurationComponent,
                  ColorPickerFlatComponent,
                  ColorPickerGradientComponent,
                  ColorPickerGradientStopComponent
              ]
          })
export class ColorsModule {}
