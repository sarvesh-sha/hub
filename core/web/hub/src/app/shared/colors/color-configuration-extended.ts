import {UnitsService} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {ColorStopPoint} from "app/services/proxy/model/models";

import {ChartColorUtilities, ColorGradientStop, ColorMapper, ColorScheme, IColorGradient, PaletteId} from "framework/ui/charting/core/colors";
import {inParallel} from "framework/utils/concurrency";

export class ColorConfigurationExtended implements IColorGradient
{
    get hasGradient(): boolean
    {
        return this.model.segments?.length > 1;
    }

    constructor(public model: Models.ColorConfiguration)
    {
        if (!this.model)
        {
            this.model = new Models.ColorConfiguration();
        }

        if (!this.model.segments)
        {
            this.model.segments = [];
        }
    }

    public getPaletteColor(idx: number): string
    {
        return ChartColorUtilities.getColor(idx, <PaletteId>this.model.paletteName);
    }

    public computeRangeMax(dataMax: number): number
    {
        let max: number;

        for (let segment of this.model.segments)
        {
            switch (segment.stopPoint)
            {
                case Models.ColorStopPoint.MAX:
                    return dataMax;

                case Models.ColorStopPoint.CUSTOM:
                    max = max === undefined ? segment.stopPointValue : Math.max(max, segment.stopPointValue);
                    break;
            }
        }

        return max;
    }

    public computeRangeMin(dataMin: number): number
    {
        let min: number;

        for (let segment of this.model.segments)
        {
            switch (segment.stopPoint)
            {
                case Models.ColorStopPoint.MIN:
                    return dataMin;

                case Models.ColorStopPoint.CUSTOM:
                    min = min === undefined ? segment.stopPointValue : Math.min(min, segment.stopPointValue);
                    break;
            }
        }

        return min;
    }

    // pre: if i < j represent indices of custom stops in this.model.segments, this.model.segments[i].stopPointValue <= this.model.segments[j].stopPointValue
    // min/max values derived from data or range override
    // midpoints derived from adjacent values
    public computeStops(dataMin?: number,
                        dataMax?: number): ColorGradientStop[]
    {
        let stops = this.model.segments?.map(
            (segment) =>
            {
                switch (segment.stopPoint)
                {
                    case Models.ColorStopPoint.CUSTOM:
                        return new ColorGradientStop(segment.stopPointValue, segment.color);

                    default:
                        return new ColorGradientStop(undefined, segment.color);
                }
            }) || [];

        if (dataMin != undefined && dataMax != undefined) return ColorMapper.contextualizeGradientStops(stops, dataMin, dataMax);

        return stops;
    }

    public isValid(): boolean
    {
        return ColorConfigurationExtended.isValid(this.model);
    }

    //--//

    public static newModel(onlyPalette: boolean = false): Models.ColorConfiguration
    {
        let [start, end] = ColorScheme.defaultColors;
        let model        = new Models.ColorConfiguration();
        if (!onlyPalette)
        {
            model.segments = [
                Models.ColorSegment.newInstance({
                                                    color    : start,
                                                    stopPoint: Models.ColorStopPoint.MIN
                                                }),
                Models.ColorSegment.newInstance({
                                                    color    : end,
                                                    stopPoint: Models.ColorStopPoint.MAX
                                                })
            ];
        }

        return model;
    }

    public static emptyModel(): Models.ColorConfiguration
    {
        return Models.ColorConfiguration.newInstance({segments: []});
    }

    public static defaultWidgetModel(color: string = ChartColorUtilities.getDefaultColorById("blue").hex): Models.ColorConfiguration
    {
        return Models.ColorConfiguration.newInstance({
                                                         segments: [
                                                             Models.ColorSegment.newInstance({
                                                                                                 color    : color,
                                                                                                 stopPoint: ColorStopPoint.MIN
                                                                                             }),
                                                             Models.ColorSegment.newInstance({
                                                                                                 color    : color,
                                                                                                 stopPoint: ColorStopPoint.MAX
                                                                                             })
                                                         ]
                                                     });
    }

    public static async convertUnits(unitsSvc: UnitsService,
                                     model: Models.ColorConfiguration,
                                     from: Models.EngineeringUnitsFactors,
                                     to: Models.EngineeringUnitsFactors)
    {
        await inParallel(model?.segments || [], async (stop) =>
        {
            if (stop.stopPoint === Models.ColorStopPoint.CUSTOM)
            {
                stop.stopPointValue = await unitsSvc.convert(stop.stopPointValue, from, to);
            }
        });
    }

    public static isValid(model: Models.ColorConfiguration): boolean
    {
        if (!model) return false;

        let prevCustom;
        for (let segment of model.segments || [])
        {
            if (segment.stopPoint === Models.ColorStopPoint.CUSTOM)
            {
                if (segment.stopPointValue != null && (prevCustom == null || prevCustom <= segment.stopPointValue))
                {
                    prevCustom = segment.stopPointValue;
                }
                else
                {
                    return false;
                }
            }
        }

        return true;
    }

    public static areEquivalent(modelA: Models.ColorConfiguration,
                                modelB: Models.ColorConfiguration,
                                ignorePalette: boolean = false): boolean
    {
        if (modelA === modelB) return true;
        if (!modelA || !modelB) return false;
        if (!ignorePalette && modelA.paletteName !== modelB.paletteName) return false;
        if (!modelA.segments && !modelB.segments) return true;
        if (modelA.segments.length !== modelB.segments.length) return false;
        return modelA.segments.every((segmentA,
                                      idx) =>
                                     {
                                         let segmentB = modelB.segments[idx];
                                         if (segmentA === segmentB) return true;
                                         if (!segmentA || !segmentB) return false;
                                         return segmentA.color === segmentB.color &&
                                                segmentA.stopPoint === segmentB.stopPoint &&
                                                (segmentA.stopPoint !== Models.ColorStopPoint.CUSTOM || segmentA.stopPointValue == segmentB.stopPointValue);
                                     });
    }
}
