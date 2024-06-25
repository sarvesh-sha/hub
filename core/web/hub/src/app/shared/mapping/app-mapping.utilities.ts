import * as Models from "app/services/proxy/model/models";

import {ChartColorUtilities} from "framework/ui/charting/core/colors";

export class AppMappingUtilities
{
    static iconOptions(): IconOption[]
    {
        return [
            new IconOption(Models.MapPinIcon.Pin, "Pin Style", this.makePinIcon),
            new IconOption(Models.MapPinIcon.Circle, "Circle Style", this.makeCircleIcon),
            new IconOption(Models.MapPinIcon.Dot, "Dot Style", this.makeDotIcon)
        ];
    }

    static makeIcon(text: string,
                    size: number,
                    color: string,
                    style: Models.MapPinIcon = Models.MapPinIcon.Pin): string
    {
        switch (style)
        {
            case Models.MapPinIcon.Pin:
                return this.makePinIcon(text, size, color);
            case Models.MapPinIcon.Circle:
                return this.makeCircleIcon(text, size, color);
            case Models.MapPinIcon.Dot:
                return this.makeDotIcon(text, size, color);
        }
    }

    static makeCircleIcon(text: string  = "",
                          size: number  = 32,
                          color: string = ChartColorUtilities.getColorById("Map Colors", "mapred").hex): string
    {
        let darker = ChartColorUtilities.safeChroma(color)
                                        .brighten(-1.5)
                                        .desaturate(0.33);

        let lighter = ChartColorUtilities.safeChroma(color)
                                         .luminance(0.925)
                                         .desaturate(0.05);

        let highlight = ChartColorUtilities.safeChroma(color)
                                           .brighten(1.5);

        let dark   = `<circle cx="16" cy="16" r="16" style="fill: ${darker}" />`;
        let outer  = `<circle cx="16" cy="16" r="14" style="fill: ${color}" />`;
        let light  = `<circle cx="16" cy="16" r="12" style="fill: ${lighter}" />`;
        let inner  = `<circle cx="16" cy="16" r="10" fill="${color}"/>`;
        let font   = `font-size: 11px; font-weight: bold; font-family: 'Open Sans', 'Helvetica Neue', sans-serif;`;
        let stroke = `<text x="16" y="20" style="${font} stroke: ${highlight}; stroke-width: 1.5px" text-anchor="middle">${text || ""}</text>`;
        let value  = `<text x="16" y="20" style="${font} fill: #000000;" text-anchor="middle">${text || ""}</text>`;

        return `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${size}" height="${size}" viewBox="0 0 32 32" xml:space="preserve">${dark}${outer}${light}${inner}${stroke}${value}</svg>`;
    }

    static makeDotIcon(text: string  = "",
                       size: number  = 32,
                       color: string = ChartColorUtilities.getColorById("Map Colors", "mapred").hex): string
    {
        let lighter = ChartColorUtilities.safeChroma(color)
                                         .luminance(0.925)
                                         .desaturate(0.05);

        let outer = `<circle cx="16" cy="16" r="16" style="fill: ${color}" />`;
        let inner = `<circle cx="16" cy="16" r="12" fill="${lighter}"/>`;
        let font  = `font-size: 11px; font-weight: bold; font-family: 'Open Sans', 'Helvetica Neue', sans-serif;`;
        let value = `<text x="16" y="20" style="${font} fill: #000000;" text-anchor="middle">${text || ""}</text>`;

        return `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${size}" height="${size}" viewBox="0 0 32 32" xml:space="preserve">${outer}${inner}${value}</svg>`;
    }

    static makePinIcon(text: string  = "",
                       size: number  = 32,
                       color: string = ChartColorUtilities.getColorById("Map Colors", "mapred").hex): string
    {
        let darker = ChartColorUtilities.safeChroma(color)
                                        .brighten(-1.5)
                                        .desaturate(0.2);
        let stroke = ChartColorUtilities.safeChroma(color)
                                        .brighten(-0.55)
                                        .desaturate(0.1);

        let rawPath     = `M 15.25 31.1 Q 14.5 25 8 14 A 9.23 9.23 0 1 1 24 14 Q 17.5 25 16.75 31.1 A 0.75 0.75 0 1 1 15.25 31.1 Z`;
        let fillPin     = `<path d="${rawPath}" fill="${color}"></path>`;
        let strokePin   = `<path d="${rawPath}" stroke="${stroke}" stroke-width="1.33"></path>`;
        let circle      = `<circle cx="16" cy="9.15" r="7" fill="${darker}" />`;
        let smallcircle = `<circle cx="16" cy="9.15" r="3.5" fill="${darker}" />`;
        let font        = `font-size: 10.75px; font-weight: bold; font-family: 'Open Sans', 'Helvetica Neue', sans-serif;`;
        let value       = `<text x="15.9" y="13.15" style="${font} fill: #FFFFFF;" text-anchor="middle">${text || ""}</text>`;
        let group       = `<g transform-origin="center" transform="scale(0.96)">${strokePin}${fillPin}${text ? circle : smallcircle}${value}</g>`;

        return `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${size}" height="${size}" viewBox="0 0 32 32" xml:space="preserve">${group}</svg>`;
    }

    static sameLocation(a: Models.LongitudeLatitude,
                        b: Models.LongitudeLatitude): boolean
    {
        return a?.longitude == b?.longitude && a?.latitude == b?.latitude;
    }
}

export interface PinRenderer
{
    (text: string,
     size: number,
     color: string): string
}

export class IconOption
{
    constructor(public id: Models.MapPinIcon,
                public label: string,
                public renderer: PinRenderer)
    {}
}
