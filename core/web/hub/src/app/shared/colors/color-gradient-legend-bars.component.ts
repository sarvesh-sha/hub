import {Component, Input} from "@angular/core";

import {LegendSegment} from "app/shared/colors/color-gradient-legend.component";

@Component({
               selector   : "o3-color-gradient-legend-bars[legendSegments]",
               templateUrl: "./color-gradient-legend-bars.component.html",
               styleUrls  : ["./color-gradient-legend-bars.component.scss"]
           })
export class ColorGradientLegendBarsComponent
{
    @Input() legendSegments: LegendSegment[];
}
