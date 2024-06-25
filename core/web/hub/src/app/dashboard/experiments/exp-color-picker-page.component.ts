import {Component, Injector} from "@angular/core";

import {ExperimentsBasePageComponent} from "app/dashboard/experiments/exp-base-page.component";

import * as Models from "app/services/proxy/model/models";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {ColorSelection} from "app/shared/colors/color-palette.component";
import * as chroma from "chroma-js";

import {ChartValueRange} from "framework/ui/charting/core/basics";
import {ChartColorUtilities, ColorGradientContinuous, ColorGradientStop, ColorOption, ColorSegmentInterpolationMode} from "framework/ui/charting/core/colors";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-experiments-color-picker-page",
               templateUrl: "./exp-color-picker-page.component.html",
               styleUrls  : ["./exp-color-picker-page.component.scss"]
           })
export class ExperimentsColorPickerPageComponent extends ExperimentsBasePageComponent
{
    text: string = "1";

    colorMix1: string = "blue";
    colorMix2: string = "red";
    mixedColor: string;

    onlyPalette: boolean = true;

    colorConfig: Models.ColorConfiguration = ColorConfigurationExtended.newModel();

    color: string = "red";

    interpolation: ColorSegmentInterpolationMode = ColorSegmentInterpolationMode.LINEAR;
    stops: Models.ColorSegment[]                 = [
        Models.ColorSegment.newInstance({
                                            color    : "red",
                                            stopPoint: Models.ColorStopPoint.MIN
                                        }),
        Models.ColorSegment.newInstance({
                                            color    : "blue",
                                            stopPoint: Models.ColorStopPoint.MAX
                                        })
    ];

    brightenAmt: number = 0.25;
    darkenAmt: number   = 0.1;

    paletteStart: string             = ChartColorUtilities.getDefaultColorById("green").hex;
    paletteEnd: string               = ChartColorUtilities.getDefaultColorById("red").hex;
    paletteOptions: ColorSelection[] = [];

    constructor(inj: Injector)
    {
        super(inj);
    }

    updateMix()
    {
        let chroma1     = ChartColorUtilities.safeChroma(this.colorMix1);
        let chroma2     = ChartColorUtilities.safeChroma(this.colorMix2);
        this.mixedColor = chroma.mix(chroma1, chroma2)
                                .hex();
    }

    updateGradientPalette()
    {
        let dark            = true;
        let chartValueRange = new ChartValueRange();
        const numColors     = 10;
        chartValueRange.expandForValue(0);
        chartValueRange.expandForValue(1);
        let colors = ColorGradientContinuous.generateGradientColors([
                                                                        new ColorGradientStop(0, this.paletteStart),
                                                                        new ColorGradientStop(1, this.paletteEnd)
                                                                    ], undefined, numColors)
                                            .map((color,
                                                  idx) =>
                                                 {
                                                     if (idx === 0 || idx === numColors - 1) return color;

                                                     let chromaColor = ChartColorUtilities.safeChroma(color);
                                                     chromaColor     = dark ? chromaColor.darken(this.darkenAmt) : chromaColor.brighten(this.brightenAmt);
                                                     dark            = !dark;
                                                     return chromaColor.hex();
                                                 });

        let id              = 0;
        this.paletteOptions = colors.map((color) => new ColorSelection(new ControlOption(new ColorOption(`${id++}`, color, color), color)));
    }
}
