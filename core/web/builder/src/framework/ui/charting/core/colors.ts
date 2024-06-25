import * as chroma from "chroma-js";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";

function safeChroma(from: string | number): chroma.Color
{
    try
    {
        return chroma(from);
    }
    catch (e)
    {
        return chroma("black");
    }
}

export class Colorized<T>
{
    constructor(public readonly model: T)
    {
    }

    color: string;
}

export class ColorOption
{
    private m_chroma: chroma.Color;

    set color(color: string)
    {
        this.m_chroma = safeChroma(color);
    }

    get hex(): string
    {
        return this.m_chroma.hex("rgb");
    }

    get color(): string
    {
        return this.m_chroma.css();
    }

    get hsl(): number[]
    {
        return this.m_chroma.hsl();
    }

    get rgb(): number[]
    {
        return this.m_chroma.rgb();
    }

    constructor(public id: string,
                color: string,
                public name: string)
    {
        this.color = color;
    }
}

//--//

export class ColorScheme
{
    public readonly colors: string[];

    public static readonly defaultColors = [
        "#5eed5e",
        "#ed5e76"
    ];

    constructor(colors?: string[])
    {
        if (!colors || colors.length === 0)
        {
            colors = ColorScheme.defaultColors;
        }
        else if (colors.length === 1)
        {
            colors.unshift("#FFFFFF");
        }

        this.colors = colors;
    }
}

/*
 * We define this interface to mirror the ColorGradient data model
 * since we cannot import server models into framework components.
 */
export interface IColorGradient
{
    computeStops(dataMin: number,
                 dataMax: number): ColorGradientStop[];

    computeRangeMin(dataMin: number): number;

    computeRangeMax(dataMax: number): number;

    isValid(): boolean;
}

export class ColorGradientStop
{
    /**
     * @param value - The value at which the gradient should reflect the specified color
     * @param color - Hex color value
     */
    constructor(public value: number,
                public color: string)
    {
    }

    clone(): ColorGradientStop
    {
        return new ColorGradientStop(this.value, this.color);
    }
}

export class ColorGradientExtended
{
    constructor(public model: IColorGradient)
    {
        if (!this.model)
        {
            this.model = {
                computeStops(dataMin: number,
                             dataMax: number): ColorGradientStop[]
                {
                    return [
                        new ColorGradientStop(dataMin, ColorGradientExtended.getDefaultStartColor()),
                        new ColorGradientStop(dataMax, ColorGradientExtended.getDefaultEndColor())
                    ];
                },

                computeRangeMin(dataMin: number): number { return dataMin; },

                computeRangeMax(dataMax: number): number { return dataMax; },

                isValid(): boolean { return true; }
            };
        }
    }

    static getDefaultStartColor(): string
    {
        return ChartColorUtilities.getDefaultColorById("green").hex;
    }

    static getDefaultEndColor(): string
    {
        return ChartColorUtilities.getDefaultColorById("red").hex;
    }
}

export abstract class ColorMapper
{
    abstract interpolationMode: ColorSegmentInterpolationMode;

    get colorStops(): ColorGradientStop[]
    {
        return this.m_colorStops.map((colorStop) => colorStop.clone());
    }

    protected readonly m_colorStops: ColorGradientStop[];

    constructor(colorStops: ColorGradientStop[])
    {
        this.m_colorStops = ColorMapper.cleanGradientStops(colorStops);
    }

    abstract getColor(value: number): string;

    static contextualizeGradientStops(stops: ColorGradientStop[],
                                      min: number,
                                      max: number): ColorGradientStop[]
    {
        stops = stops.map((stop) => stop.clone());

        // fill in min/max from Models.ColorConfiguration.segments
        if (stops[0].value == null) stops[0].value = min;
        if (stops[stops.length - 1].value == null) stops[stops.length - 1].value = max;

        // fill in midpoints
        for (let i = 1; i < stops.length - 1; i++)
        {
            let stop = stops[i];
            if (stop.value == null) stop.value = (stops[i - 1].value + stops[i + 1].value) / 2;
        }

        return stops;
    }

    static cleanGradientStops(stops: ColorGradientStop[]): ColorGradientStop[]
    {
        stops = stops.map((stop) => stop.clone());

        let customMin;
        let customMax;
        for (let i = 1; i < stops.length - 1; i++)
        {
            let stop = stops[i];
            if (!isNaN(stop.value))
            {
                customMin = Math.min(stop.value, customMin ?? Number.MAX_VALUE);
                customMax = Math.max(stop.value, customMax ?? -Number.MAX_VALUE);
            }
        }

        if (customMin != undefined && stops[0].value > customMin)
        {
            let deleteCount = stops[1].value > customMin ? 2 : 1;
            stops.splice(0, deleteCount);
        }

        if (customMax != undefined && stops[stops.length - 1].value < customMax)
        {
            let deleteCount = stops.length > 3 && stops[stops.length - 2].value < customMax ? 2 : 1;
            stops.splice(stops.length - deleteCount, deleteCount);
        }

        for (let i = 1; i < stops.length - 1; i++)
        {
            let prevValue = stops[i - 1].value;
            let nextValue = stops[i + 1].value;
            if (!isNaN(prevValue) && !isNaN(nextValue) && prevValue === nextValue)
            {
                stops.splice(i--, 1);
            }
        }

        return stops;
    }
}

export class StepwiseColorMapper extends ColorMapper
{
    interpolationMode = ColorSegmentInterpolationMode.STEP;

    getColor(value: number): string
    {
        // go from left to right to respect custom values that are not necessarily contained within min/max range
        let afterIdx = this.m_colorStops.findIndex((stop) => stop.value > value);

        let stop: ColorGradientStop;
        switch (afterIdx)
        {
            case -1:
                stop = this.m_colorStops[this.m_colorStops.length - 2];
                break;

            case 0:
                stop = this.m_colorStops[0];
                break;

            default:
                stop = this.m_colorStops[afterIdx - 1];
                break;
        }

        return stop?.color;
    }

    static fromEnumsToColorMapper(enumValues: { value: number }[],
                                  paletteId: PaletteId): StepwiseColorMapper
    {
        let colors: string[] = [];
        let stops            = enumValues.map((enumValue) =>
                                              {
                                                  let color = ChartColorUtilities.nextBestColor(colors, paletteId);
                                                  return new ColorGradientStop(enumValue.value, color);
                                              });
        let lastStop         = stops[stops.length - 1];
        stops.push(new ColorGradientStop(lastStop.value, lastStop.color));

        return new StepwiseColorMapper(stops);
    }
}

export class ColorGradientContinuous extends ColorMapper
{
    private static readonly minStepsPerDaisyLink: number               = 5;
    protected static readonly lowPrecisionDaisyChainLength: number     = 32;
    private static readonly maxColorGradientLength: number             = 230;
    private static readonly defaultColorMode: chroma.InterpolationMode = "lch";

    protected readonly scales: chroma.Scale[];

    interpolationMode = ColorSegmentInterpolationMode.LINEAR;

    get min(): number
    {
        return this.m_colorStops[0].value;
    }

    get max(): number
    {
        return this.m_colorStops[this.m_colorStops.length - 1].value;
    }

    constructor(colorStops: ColorGradientStop[],
                private readonly colorMode: chroma.InterpolationMode = ColorGradientContinuous.defaultColorMode)
    {
        super(colorStops);

        this.scales  = [];
        let nextStop = this.m_colorStops[0];
        for (let i = 1; i < this.m_colorStops.length; i++)
        {
            let prevStop = nextStop;
            nextStop     = this.m_colorStops[i];

            this.scales.push(chroma.scale([
                                              prevStop.color,
                                              nextStop.color
                                          ])
                                   .domain([
                                               prevStop.value,
                                               nextStop.value
                                           ])
                                   .mode(this.colorMode)
                                   .correctLightness());
        }
    }

    protected getBucketIndex(value: number): number
    {
        value = UtilsService.clamp(this.min, this.max, value);

        let index = 0;
        for (let stop of this.m_colorStops) index += Math.ceil(UtilsService.clamp(0, 1, value - stop.value));

        return Math.max(0, index - 1);
    }

    getColor(value: number): string
    {
        return this.scales[this.getBucketIndex(value)](value)
            .hex();
    }

    getCanvasGradient(canvas: CanvasRenderingContext2D,
                      x1: number,
                      y1: number,
                      x2: number,
                      y2: number): CanvasGradient
    {
        let gradient = canvas.createLinearGradient(x1, y1, x2, y2);
        let colors   = ColorGradientContinuous.generateGradientColors(this.m_colorStops, false);
        if (colors.length === 1) colors.push(colors[0]);
        let divider = colors.length - 1;
        for (let i = 0; i < colors.length; i++) gradient.addColorStop(i / divider, colors[i]);

        return gradient;
    }

    // returns colors for a correctly weighted gradient or multi-gradient
    static generateGradientColors(colorStops: ColorGradientStop[],
                                  highPrecision: boolean              = true,
                                  numColorsOverride?: number,
                                  colorMode: chroma.InterpolationMode = ColorGradientContinuous.defaultColorMode): string[]
    {
        let numStops = colorStops.length;

        if (numStops > 2) // certain permutations of scale colors generate messed up multi-step gradients: create them separately and then combine
        {
            let totalRange = colorStops[numStops - 1].value - colorStops[0].value;
            let minRange   = totalRange;
            for (let i = 1; i < numStops; i++)
            {
                let diff = colorStops[i].value - colorStops[i - 1].value;
                if (diff > 0) minRange = Math.min(diff, minRange);
            }

            let linksPerUnitRange = ColorGradientContinuous.minStepsPerDaisyLink / minRange;
            if (highPrecision) linksPerUnitRange *= 5;
            let numColors = totalRange * linksPerUnitRange;

            let multiplier = 1;
            if (!isNaN(numColorsOverride)) multiplier = numColorsOverride / numColors;

            if (!highPrecision && numColors > ColorGradientContinuous.lowPrecisionDaisyChainLength)
            {
                multiplier = ColorGradientContinuous.lowPrecisionDaisyChainLength / numColors;
            }
            else if (highPrecision && numColors > ColorGradientContinuous.maxColorGradientLength)
            {
                multiplier = ColorGradientContinuous.maxColorGradientLength / numColors;
            }
            multiplier *= linksPerUnitRange;

            let gradients: string[][] = [];
            let next                  = colorStops[0];
            for (let i = 1; i < colorStops.length; i++)
            {
                let prev = next;
                next     = colorStops[i];

                if (prev.value == next.value) continue;

                let scale = chroma.scale([
                                             prev.color,
                                             next.color
                                         ])
                                  .mode(colorMode)
                                  .correctLightness();

                let valueRange = 1;
                if (!isNaN(next.value) && !isNaN(prev.value))
                {
                    scale      = scale.domain([
                                                  prev.value,
                                                  next.value
                                              ]);
                    valueRange = next.value - prev.value;
                }

                let colors = scale.colors(Math.max(1, valueRange * multiplier));
                gradients.push(colors);
            }

            let daisyChainedGradients = [gradients[0][0]];
            for (let gradient of gradients)
            {
                for (let i = 1; i < gradient.length; i++)
                {
                    daisyChainedGradients.push(gradient[i]);
                }
            }

            return daisyChainedGradients;
        }
        else if (numStops === 2)
        {
            let numColors = numColorsOverride || (highPrecision ? ColorGradientContinuous.lowPrecisionDaisyChainLength : Math.round(ColorGradientContinuous.lowPrecisionDaisyChainLength / 2));
            return chroma.scale(colorStops.map((stop) => stop.color))
                         .mode(colorMode)
                         .correctLightness()
                         .colors(numColors);
        }
        else
        {
            return colorStops.map((colorStop) => colorStop.color);
        }
    }
}

export class ColorGradientDiscrete extends ColorGradientContinuous
{
    private readonly colorBuckets: ColorGradientBucket[];

    interpolationMode = ColorSegmentInterpolationMode.LINEAR;

    constructor(colorStops: ColorGradientStop[],
                numColors: number = ColorGradientContinuous.lowPrecisionDaisyChainLength,
                colorMode?: chroma.InterpolationMode)
    {
        super(colorStops, colorMode);

        this.colorBuckets = [];
        let currValue     = this.m_colorStops[0].value;
        for (let i = 0; i < this.scales.length; i++)
        {
            let nextValue           = this.m_colorStops[i + 1].value;
            let precomputedGradient = new ColorGradientBucket(currValue, nextValue, this.scales[i], numColors);
            this.colorBuckets.push(precomputedGradient);
            currValue = nextValue;
        }
    }

    getColor(value: number): string
    {
        return this.colorBuckets[this.getBucketIndex(value)].getColor(value);
    }
}

class ColorGradientBucket
{
    private readonly diff: number;
    private colors: string[];

    constructor(private readonly min: number,
                max: number,
                scale: chroma.Scale,
                numColors: number)
    {
        this.diff = max - min;
        if (this.diff)
        {
            this.colors = scale.colors(numColors);
        }
        else
        {
            let onlyColor = scale(max)
                .hex();
            this.colors   = [onlyColor];
        }
    }

    getColor(value: number)
    {
        let decimal = (value - this.min) / this.diff;
        let index   = UtilsService.clamp(0, this.colors.length - 1, Math.floor(decimal * this.colors.length) || 0);
        return this.colors[index];
    }
}

export enum ColorSegmentInterpolationMode
{
    STEP   = "STEP",
    LINEAR = "LINEAR"
}

export class ColorSegmentBackground
{
    public readonly background: string;

    constructor(startColor: string,
                endColor: string,
                mode: ColorSegmentInterpolationMode)
    {
        if (mode === ColorSegmentInterpolationMode.LINEAR && startColor && endColor)
        {
            this.background = `linear-gradient(90deg, ${ColorGradientContinuous.generateGradientColors([
                                                                                                           new ColorGradientStop(0, startColor),
                                                                                                           new ColorGradientStop(1, endColor)
                                                                                                       ], false)} 100%)`;
        }
        else
        {
            this.background = startColor || "unset";
        }
    }
}

function getGradientPalette(name: string,
                            color1: string,
                            color2: string,
                            paletteSize: number    = 10,
                            darkenAmount: number   = 0.1,
                            brightenAmount: number = 0.25): ColorOption[]
{
    let dark   = true;
    let colors = ColorGradientContinuous.generateGradientColors([
                                                                    new ColorGradientStop(0, color1),
                                                                    new ColorGradientStop(1, color2)
                                                                ], undefined, paletteSize)
                                        .map((color,
                                              idx) =>
                                             {
                                                 if (idx === 0 || idx === paletteSize - 1) return color;
                                                 let chromaColor = safeChroma(color);
                                                 chromaColor     = dark ? chromaColor.darken(darkenAmount) : chromaColor.brighten(brightenAmount);
                                                 dark            = !dark;
                                                 return chromaColor.hex();
                                             });

    return colors.map((color,
                       idx) => new ColorOption(name.toLocaleLowerCase() + idx, color, color));
}

function mapPalette(colors: ColorOption[]): Lookup<ColorOption>
{
    return UtilsService.extractLookup(colors);
}

const DefaultColorPalette = {
    aqua    : new ColorOption("aqua", "hsl(195,80%,65%)", "Aqua"),
    blue    : new ColorOption("blue", "hsl(215,80%,65%)", "Blue"),
    lavender: new ColorOption("lavender", "hsl(240,80%,65%)", "Lavender"),
    purple  : new ColorOption("purple", "hsl(285,80%,65%)", "Purple"),
    pink    : new ColorOption("pink", "hsl(325,80%,65%)", "Pink"),
    red     : new ColorOption("red", "hsl(350,80%,65%)", "Red"),
    orange  : new ColorOption("orange", "hsl(15,80%,65%)", "Orange"),
    yellow  : new ColorOption("yellow", "hsl(50,80%,65%)", "Yellow"),
    green   : new ColorOption("green", "hsl(120,80%,65%)", "Green"),
    teal    : new ColorOption("teal", "hsl(160,80%,65%)", "Teal")
};

const colorOptionPalettes: Lookup<Lookup<ColorOption>> = {
    null             : DefaultColorPalette,
    "Warm"           : {
        warm0: new ColorOption("warm0", "hsl(303,70%,65%)", "Violet"),
        warm1: new ColorOption("warm1", "hsl(317,70%,65%)", "Orchid"),
        warm2: new ColorOption("warm2", "hsl(331,70%,65%)", "Warm Orchid"),
        warm3: new ColorOption("warm3", "hsl(345,70%,65%)", "Violet Red"),
        warm4: new ColorOption("warm4", "hsl(360,70%,65%)", "Red"),
        warm5: new ColorOption("warm5", "hsl(12,70%,65%)", "Light Coral"),
        warm6: new ColorOption("warm6", "hsl(24,70%,65%)", "Dark Salmon"),
        warm7: new ColorOption("warm7", "hsl(36,70%,65%)", "Sandy Brown"),
        warm8: new ColorOption("warm8", "hsl(48,70%,65%)", "Burly Wood"),
        warm9: new ColorOption("warm9", "hsl(60,70%,65%)", "Khaki")
    },
    "Cold"           : {
        cold0: new ColorOption("cold0", "hsl(110,70%,65%)", "Yellow Green"),
        cold1: new ColorOption("cold1", "hsl(135,70%,65%)", "Lush Green"),
        cold2: new ColorOption("cold2", "hsl(160,70%,65%)", "Aquamarine"),
        cold3: new ColorOption("cold3", "hsl(180,70%,65%)", "Cyan"),
        cold4: new ColorOption("cold4", "hsl(195,70%,65%)", "Sky Blue"),
        cold5: new ColorOption("cold5", "hsl(210,70%,65%)", "Corn Flower Blue"),
        cold6: new ColorOption("cold6", "hsl(225,70%,65%)", "Blue"),
        cold7: new ColorOption("cold7", "hsl(240,70%,65%)", "Slate Blue"),
        cold8: new ColorOption("cold8", "hsl(255,70%,65%)", "Dark Periwinkle"),
        cold9: new ColorOption("cold9", "hsl(270,70%,65%)", "Purple")
    },
    "Black-White"    : mapPalette(getGradientPalette("Black-White", "#000000", "#FFFFFF", 10, 0, 0)),
    "Green-Red"      : mapPalette(getGradientPalette("Green-Red", "#7DE87D", "#E87D8F")),
    "Red-Blue"       : mapPalette(getGradientPalette("Red-Blue", "#E87D8F", "#7DAAE8")),
    "Blue-Green"     : mapPalette(getGradientPalette("Blue-Green", "#7DAAE8", "#7DE87D")),
    "Magenta-Cyan"   : mapPalette(getGradientPalette("Magenta-Cyan", "#E87DBB", "#7DE8C4")),
    "Cyan-Yellow"    : mapPalette(getGradientPalette("Cyan-Yellow", "#7DE8C4", "#E8D67D")),
    "Yellow-Magenta" : mapPalette(getGradientPalette("Yellow-Magenta", "#E8D67D", "#E87DBB")),
    "Red"            : {
        red0: new ColorOption("red0", "#E57373", "Dark Salmon"),
        red1: new ColorOption("red1", "#EF5552", "Tomato"),
        red2: new ColorOption("red2", "#DB3333", "Fire Brick"),
        red3: new ColorOption("red3", "#C62828", "Crimson"),
        red4: new ColorOption("red4", "#B71C1C", "Dark Red")
    },
    "Orange"         : {
        orange0: new ColorOption("orange0", "#FFAF3D", "Sand Orange"),
        orange1: new ColorOption("orange1", "#FF9800", "Light Orange"),
        orange2: new ColorOption("orange2", "#f88400", "Orange"),
        orange3: new ColorOption("orange3", "#EF6C00", "Dark Orange"),
        orange4: new ColorOption("orange4", "#E65100", "Red Orange")
    },
    "Yellow"         : {
        yellow0: new ColorOption("yellow0", "#FFFF8D", "Light Yellow"),
        yellow1: new ColorOption("yellow1", "#FFFF46", "Yellow"),
        yellow2: new ColorOption("yellow2", "#FFEA00", "Yellow Gold"),
        yellow3: new ColorOption("yellow3", "#FFD600", "Gold"),
        yellow4: new ColorOption("yellow4", "#FBC12E", "Goldenrod")
    },
    "Green"          : {
        green0: new ColorOption("green0", "#81C784", "Light Sea Green"),
        green1: new ColorOption("green1", "#58B560", "Sea Green"),
        green2: new ColorOption("green2", "#43A047", "Dark Sea Green"),
        green3: new ColorOption("green3", "#2E7D32", "Forest Green"),
        green4: new ColorOption("green4", "#1B5E20", "Dark Green")
    },
    "Blue"           : {
        blue0: new ColorOption("blue0", "#52ACF5", "Sky Blue"),
        blue1: new ColorOption("blue1", "#2196F3", "Light Blue"),
        blue2: new ColorOption("blue2", "#1C7FDB", "Dodger Blue"),
        blue3: new ColorOption("blue3", "#1565C0", "Royal Blue"),
        blue4: new ColorOption("blue4", "#0D47A1", "Dark Slate Blue")
    },
    "Purple"         : {
        purple0: new ColorOption("purple0", "#AB47BC", "Orchid"),
        purple1: new ColorOption("purple1", "#9C27B0", "Dark Orchid"),
        purple2: new ColorOption("purple2", "#8522a6", "Dark Magenta"),
        purple3: new ColorOption("purple3", "#6A1B9A", "Purple"),
        purple4: new ColorOption("purple4", "#4A148C", "Indigo Purple")
    },
    "Gray"           : {
        gray0: new ColorOption("gray0", "#859AA5", "Light Gray"),
        gray1: new ColorOption("gray1", "#607D8B", "Light Slate Gray"),
        gray2: new ColorOption("gray2", "#4D6470", "Dim Gray"),
        gray3: new ColorOption("gray3", "#37474F", "Slate Gray"),
        gray4: new ColorOption("gray4", "#263238", "Dark Slate Gray")
    },
    "Map Colors"     : {
        mapred   : new ColorOption("mapred", "#FF5F5B", "Map Red"),
        mapgreen : new ColorOption("mapgreen", "#5BE43D", "Map Green"),
        maporange: new ColorOption("maporange", "#FF891C", "Map Orange"),
        mapblue  : new ColorOption("mapblue", "#3776F4", "Map Blue"),
        mapyellow: new ColorOption("mapyellow", "#E7DA39", "Map Yellow")
    },
    "Map Path Colors": {
        mapred   : new ColorOption("mapred", "#FF5F5B", "Map Red"),
        mapgreen : new ColorOption("mapgreen", "#5BE43D", "Map Green"),
        maporange: new ColorOption("maporange", "#FF891C", "Map Orange"),
        mapblue  : new ColorOption("mapblue", "#3776F4", "Map Blue"),
        mappurple: new ColorOption("mappurple", "#D480F5", "Map Purple")
    }
};

class ColorPalette
{
    constructor(public readonly label: string,
                public readonly colorOptions: Lookup<ColorOption>)
    {
    }
}

const colorPalettes = {
    ""               : new ColorPalette("Default", DefaultColorPalette),
    "Warm"           : new ColorPalette("Warm", colorOptionPalettes["Warm"]),
    "Cold"           : new ColorPalette("Cold", colorOptionPalettes["Cold"]),
    "Black-White"    : new ColorPalette("Black-White", colorOptionPalettes["Black-White"]),
    "Green-Red"      : new ColorPalette("Green-Red", colorOptionPalettes["Green-Red"]),
    "Red-Blue"       : new ColorPalette("Red-Blue", colorOptionPalettes["Red-Blue"]),
    "Blue-Green"     : new ColorPalette("Blue-Green", colorOptionPalettes["Blue-Green"]),
    "Magenta-Cyan"   : new ColorPalette("Magenta-Cyan", colorOptionPalettes["Magenta-Cyan"]),
    "Cyan-Yellow"    : new ColorPalette("Cyan-Yellow", colorOptionPalettes["Cyan-Yellow"]),
    "Yellow-Magenta" : new ColorPalette("Yellow-Magenta", colorOptionPalettes["Yellow-Magenta"]),
    "Red"            : new ColorPalette("Red", colorOptionPalettes["Red"]),
    "Orange"         : new ColorPalette("Orange", colorOptionPalettes["Orange"]),
    "Yellow"         : new ColorPalette("Yellow", colorOptionPalettes["Yellow"]),
    "Green"          : new ColorPalette("Green", colorOptionPalettes["Green"]),
    "Blue"           : new ColorPalette("Blue", colorOptionPalettes["Blue"]),
    "Purple"         : new ColorPalette("Purple", colorOptionPalettes["Purple"]),
    "Gray"           : new ColorPalette("Gray", colorOptionPalettes["Gray"]),
    "Map Colors"     : new ColorPalette("Map Colors", colorOptionPalettes["Map Colors"]),
    "Map Path Colors": new ColorPalette("Map Path Colors", colorOptionPalettes["Map Path Colors"])
};

export type PaletteId = keyof typeof colorPalettes;
export type ColorIds<K extends PaletteId> = keyof typeof colorOptionPalettes[K];
export type DefaultColorIds = keyof typeof DefaultColorPalette;

export class ChartColorUtilities
{
    public static readonly defaultGradient           = "Green-Red";
    public static readonly defaultPalette: PaletteId = "";

    public static readonly palettes: Lookup<ColorPalette> = colorPalettes;
    private static colorToPalette: Lookup<PaletteId>;

    public static gradientPresets: Lookup<ColorScheme> = {
        "Black-White"   : new ColorScheme([
                                              "#000000",
                                              "#FFFFFF"
                                          ]),
        "Green-Red"     : new ColorScheme([
                                              ChartColorUtilities.getDefaultColorById("green").hex,
                                              ChartColorUtilities.getDefaultColorById("red").hex
                                          ]),
        "Red-Blue"      : new ColorScheme([
                                              ChartColorUtilities.getDefaultColorById("red").hex,
                                              ChartColorUtilities.getDefaultColorById("blue").hex
                                          ]),
        "Blue-Green"    : new ColorScheme([
                                              ChartColorUtilities.getDefaultColorById("blue").hex,
                                              ChartColorUtilities.getDefaultColorById("green").hex
                                          ]),
        "Magenta-Cyan"  : new ColorScheme([
                                              ChartColorUtilities.getDefaultColorById("pink").hex,
                                              ChartColorUtilities.getDefaultColorById("teal").hex
                                          ]),
        "Cyan-Yellow"   : new ColorScheme([
                                              ChartColorUtilities.getDefaultColorById("teal").hex,
                                              ChartColorUtilities.getDefaultColorById("yellow").hex
                                          ]),
        "Yellow-Magenta": new ColorScheme([
                                              ChartColorUtilities.getDefaultColorById("yellow").hex,
                                              ChartColorUtilities.getDefaultColorById("pink").hex
                                          ]),
        "Reds"          : new ColorScheme([
                                              "#E57373",
                                              "#AF1010"
                                          ]),
        "Oranges"       : new ColorScheme([
                                              "#FFBF66",
                                              "#E65100"
                                          ]),
        "Yellows"       : new ColorScheme([
                                              "#FFFF8D",
                                              "#FBC12E"
                                          ]),
        "Greens"        : new ColorScheme([
                                              "#81C784",
                                              "#1B5E20"
                                          ]),
        "Blues"         : new ColorScheme([
                                              "#73BCF7",
                                              "#0D47A1"
                                          ]),
        "Purples"       : new ColorScheme([
                                              "#C37CD0",
                                              "#4A148C"
                                          ]),
        "Pinks"         : new ColorScheme([
                                              "#F48FB1",
                                              "#AD1457"
                                          ]),
        "Grays"         : new ColorScheme([
                                              "#859AA5",
                                              "#263238"
                                          ])
    };

    public static getPalette(paletteId: PaletteId): Lookup<ColorOption>
    {
        return ChartColorUtilities.palettes[paletteId]?.colorOptions;
    }

    public static getPaletteOptions(paletteId?: PaletteId): ColorOption[]
    {
        let colorOptions = ChartColorUtilities.getPalette(paletteId);
        if (!colorOptions) colorOptions = ChartColorUtilities.getPalette(ChartColorUtilities.defaultPalette);
        return Object.keys(colorOptions)
                     .map((color) => colorOptions[color]);
    }

    public static getGradientPreset(preset?: string): ColorScheme
    {
        return ChartColorUtilities.gradientPresets[preset] || ChartColorUtilities.gradientPresets[ChartColorUtilities.defaultGradient];
    }

    public static paletteOptions(): ControlOption<PaletteId>[]
    {
        return Object.keys(ChartColorUtilities.palettes)
                     .map((id) =>
                          {
                              let paletteId = <PaletteId>id;
                              return new ControlOption(paletteId, ChartColorUtilities.palettes[paletteId].label);
                          });
    }

    public static gradientPresetNames(): string[]
    {
        let names = [];
        for (const name in ChartColorUtilities.gradientPresets)
        {
            names.push(name);
        }

        return names;
    }

    public static getDefaultColorById(id: DefaultColorIds): ColorOption
    {
        return DefaultColorPalette[id];
    }

    public static getColorById<K extends PaletteId>(paletteId: K,
                                                    id: ColorIds<K>): ColorOption
    {
        let palette = ChartColorUtilities.getPalette(paletteId);
        return palette ? palette[id] : undefined;
    }

    public static getColorOptions(palette?: PaletteId): ControlOption<ColorOption>[]
    {
        let colors = ChartColorUtilities.getPaletteOptions(palette);
        return colors.map((color) => new ControlOption(color, color.name));
    }

    public static nextBestColor(colors: string[],
                                palette?: PaletteId,
                                skipAdd?: boolean): string
    {
        let bestColor;
        let activePalette = ChartColorUtilities.getPaletteOptions(palette);
        if (colors.length === 0)
        {
            bestColor = activePalette[0].hex;
        }
        else
        {
            // Map all used colors in palette to number of uses
            let usage = new Map<string, number>();
            for (let usedColor of colors)
            {
                if (usedColor)
                {
                    if (!activePalette.find((color) => color.hex === usedColor)) continue;

                    if (usage.has(usedColor))
                    {
                        usage.set(usedColor, usage.get(usedColor) + 1);
                    }
                    else
                    {
                        usage.set(usedColor, 1);
                    }
                }
            }

            let unusedColor;
            for (let i = 0; i < activePalette.length; i++)
            {
                let color = ChartColorUtilities.getColor(i, palette);
                if (!usage.has(color))
                {
                    unusedColor = color;
                    break;
                }
            }

            if (unusedColor)
            {
                bestColor = unusedColor;
            }
            else
            {
                let colorsByUsageAscending = Array.from(usage.entries())
                                                  .sort((a,
                                                         b) => a[1] - b[1]);

                bestColor = colorsByUsageAscending[0][0];
            }
        }

        if (!skipAdd) colors.push(bestColor);
        return bestColor;
    }

    /**
     * Get background colors for elements of a chart type that can display multiple colors (e.g. bar chart).
     * @param {number} total - The number of colors to retrieve; should match the number of a data points in the chart.
     * @param {number} [monoColorIndex] - The index of the color to select from the default chart colors; to be used if chart should be monochrome.
     */
    public static getColors(total: number,
                            monoColorIndex?: number): string[]
    {
        let palette = ChartColorUtilities.getPaletteOptions();

        let colors: string[] = [];
        for (let i = 0; i < total; i++)
        {
            let colorIndex = (monoColorIndex || i) % palette.length;
            let color      = palette[colorIndex];
            colors.push(color.hex);
        }
        return colors;
    };

    /**
     * Get a background color for a chart type that can only display a single color (e.g. line chart).
     * @param {number} [itemIndex=0] - The index of the color to select from the default chart colors.
     * @param palette - optionally select palette other than defaultPalette
     */
    public static getColor(itemIndex: number = 0,
                           palette?: PaletteId): string
    {
        let paletteOptions = ChartColorUtilities.getPaletteOptions(palette);
        let colorIndex     = (itemIndex && itemIndex * 3 % paletteOptions.length) || 0;

        return paletteOptions[colorIndex].hex;
    }

    public static getComplementaryColor(from: string): string
    {
        let hsv = ChartColorUtilities.safeChroma(from)
                                     .hsv();

        return chroma.hsv(hsv[0] + 180, hsv[1], hsv[2])
                     .hex();
    }

    public static getPaletteFromColor(color: string): PaletteId
    {
        ChartColorUtilities.ensureColorToPalette();

        if (ChartColorUtilities.colorToPalette.hasOwnProperty(color))
        {
            return ChartColorUtilities.colorToPalette[color] || ChartColorUtilities.defaultPalette;
        }

        return null;
    }

    private static ensureColorToPalette()
    {
        if (!ChartColorUtilities.colorToPalette)
        {
            ChartColorUtilities.colorToPalette = {};
            for (let paletteId in ChartColorUtilities.palettes)
            {
                let palette = ChartColorUtilities.palettes[paletteId];
                for (let colorId in palette.colorOptions)
                {
                    ChartColorUtilities.colorToPalette[palette.colorOptions[colorId].hex] = <PaletteId>paletteId;
                }
            }
        }
    }

    public static safeChroma(from: string | number): chroma.Color
    {
        return safeChroma(from);
    }
}

export type Fill = string | CanvasPattern | CanvasGradient;
