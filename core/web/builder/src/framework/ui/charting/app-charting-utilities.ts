import {SafeHtml} from "@angular/platform-browser";

import {Lookup, MouseTouchEvent, UtilsService} from "framework/services/utils.service";
import {ChartTooltipComponent} from "framework/ui/charting/chart-tooltip.component";
import {Vector2} from "framework/ui/charting/charting-math";
import {BoxAnchor, ChartBox, ChartClipArea, ChartLineType, ChartMarker, ChartPixel, ChartPointStyle, ChartValueRange, HorizontalChartMarkerPlacement, Transform, VerticalChartMarkerPlacement} from "framework/ui/charting/core/basics";
import {ChartChip} from "framework/ui/charting/core/chips";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {ChartPoint, ChartPointRange, ChartPointSource, ChartPointsRenderView, ChartPointType, ChartValueConverter, ChartValueTransform, ChartValueTransformer} from "framework/ui/charting/core/data-sources";
import {ChartFont, ChartTextBox, ChartTextLine, TextOrientation, TextPlacement} from "framework/ui/charting/core/text";
import {ChartTimeWindow} from "framework/ui/charting/core/time";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {RelativeLocation} from "framework/ui/utils/relative-location-styles";

//--//

export const MutedColor     = "#d8d8d8";
export const AxisWidth      = 40;
export const MinEdgePadding = 8;

//--//

export abstract class TooltipPoint
{
    abstract get clickable(): boolean;

    abstract render(helper: ChartHelpers,
                    tooltip: ChartTooltipComponent,
                    sanitizer: (html: string) => SafeHtml,
                    color?: string,
                    radius?: number): void;

    abstract isEquivalent(other: TooltipPoint): boolean;

    abstract handleClick(): void;
}

export class TooltipPointForMarker extends TooltipPoint
{
    get clickable(): boolean
    {
        return this.ctx.navigable;
    }

    constructor(private readonly transform: ChartValueTransform,
                private readonly ctx: ChartMarker<any>)
    {
        super();
    }

    handleClick()
    {
        if (this.clickable) this.ctx.navigate();
    }

    isEquivalent(other: TooltipPoint): boolean
    {
        return other instanceof TooltipPointForMarker && this.ctx == other.ctx;
    }

    render(helper: ChartHelpers,
           tooltip: ChartTooltipComponent,
           sanitizer: (html: string) => SafeHtml,
           color?: string)
    {
        let marker = this.ctx;
        let pixel  = marker.toPixel(this.transform);

        // Draw the point marker
        helper.drawPoint(marker.shape, color || marker.color, (marker.size / 2) * 1.5, pixel.x, pixel.y, this.ctx.strokeWidthMultiplier);

        // Render the tooltip
        tooltip.render(pixel.x, pixel.y, sanitizer(marker.tooltip));
    }
}

export class TooltipPointForSample extends TooltipPoint
{
    get clickable(): boolean
    {
        return false;
    }

    constructor(public readonly transform: ChartValueConverter<any>,
                public readonly point: ChartPoint<any>,
                public readonly pixel: ChartPixel)
    {
        super();
    }

    render(helper: ChartHelpers,
           tooltip: ChartTooltipComponent,
           sanitizer: (html: string) => SafeHtml,
           color?: string,
           radius?: number)
    {
        let point              = this.point;
        let complementaryColor = ChartColorUtilities.getComplementaryColor(color || point.owningSource.color);
        let pixel              = this.pixel;

        radius = radius || 4;
        if (point.selected) radius *= 3;

        helper.drawPoint(ChartPointStyle.circle, complementaryColor, radius, pixel.x, pixel.y);

        let text = point.getTooltip();

        // Render tooltip
        tooltip.render(pixel.x, pixel.y, sanitizer(text));
    }

    isEquivalent(other: TooltipPoint): boolean
    {
        return other instanceof TooltipPointForSample && this.point.owningSource === other.point.owningSource;
    }

    handleClick()
    {
    }
}

export class ChartTickInfo
{
    constructor(public min: number,
                public max: number,
                public tickDist: number,
                public tickPrecision: number)
    {
    }

    generateArray(): number[]
    {
        let res = [];

        let nextTick = UtilsService.getRoundedValue(this.min, this.tickPrecision);
        let count    = 0;

        while (nextTick < this.max)
        {
            res.push(nextTick);

            nextTick = UtilsService.getRoundedValue(this.min + this.tickDist * ++count, this.tickPrecision);
        }

        return res;
    }
}

export class TextMapping
{
    constructor(public readonly lines: string[],
                public readonly fits: boolean)
    {}
}

export class ChartHelpers
{
    private static helperContext: CanvasRenderingContext2D;

    private textLengthsPerFont: Lookup<Lookup<number>> = {};

    constructor(public readonly canvas: CanvasRenderingContext2D = ChartHelpers.getContext())
    {
    }

    //--//

    static getContext(): CanvasRenderingContext2D
    {
        if (!this.helperContext)
        {
            let canvas: HTMLCanvasElement = document.createElement("canvas");
            this.helperContext            = canvas.getContext("2d");
        }

        return this.helperContext;
    }

    static pointDistance(x1: number,
                         y1: number,
                         x2: number,
                         y2: number)
    {
        let xDiff = x1 - x2;
        let yDiff = y1 - y2;

        return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    static pointDistanceFromLine(x1: number,
                                 y1: number,
                                 x2: number,
                                 y2: number,
                                 ptX: number,
                                 ptY: number)
    {
        let xDiff = x2 - x1;
        let yDiff = y2 - y1;

        return Math.abs(xDiff * (y1 - ptY) - yDiff * (x1 - ptX)) / Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    static pointOnLineClosestToTarget(x1: number,
                                      y1: number,
                                      x2: number,
                                      y2: number,
                                      targetX: number,
                                      targetY: number,
                                      clamp: boolean): ChartPixel
    {
        let x12           = x2 - x1;
        let y12           = y2 - y1;
        let lengthSquared = x12 * x12 + y12 * y12;

        if (lengthSquared == 0)
        {
            return clamp ? new ChartPixel(x1, y1) : null;
        }
        else
        {
            let x1pt    = targetX - x1;
            let y1pt    = targetY - y1;
            let dotProd = (x12 * x1pt + y12 * y1pt) / lengthSquared;

            if (clamp)
            {
                dotProd = UtilsService.clamp(0, 1, dotProd);
            }

            return new ChartPixel(x1 + x12 * dotProd, y1 + y12 * dotProd);
        }
    }

    static getMajorTickInfo(numPx: number,
                            pxSpacingForTicks: number,
                            valueRange: ChartValueRange,
                            onlyEnums: boolean): ChartTickInfo
    {
        // Get the amount of major ticks that can fit in a chart with the given minimum pixel spacing
        let numberOfMajorTicks = Math.max(1, Math.trunc(numPx / pxSpacingForTicks));
        let tickDist;

        // Check if range is zero or based on enums
        let range = valueRange.diff;
        let logRange: number;
        if (!onlyEnums && range > 0)
        {
            // Range not zero and not based on enums
            // Figure out the ideal data-space distance between ticks using the formula:
            // dist = 10^(log10(range / ticks))
            // This is essentially picking spacing using a stepper function that aligns to nice
            // data-space increments as opposed to the simple linear relation: range / ticks
            logRange = Math.round(Math.log10(range / numberOfMajorTicks));
            tickDist = Math.pow(10, logRange);
        }
        else
        {
            // Range is zero or is based on enums
            // Set the data-space distance between ticks as 1
            tickDist = 1;
            logRange = 0;
        }

        // Pick a computed number of ticks that uses our nicely rounded data-space tick distance
        let computedNumberOfMajorTicks = Math.trunc(range / tickDist);

        // Check if our computed number of ticks is more dense that the calculated maximum or not
        if (computedNumberOfMajorTicks > numberOfMajorTicks)
        {
            // Computed number of ticks is more dense than the maximum allowed ticks

            // Check if the ticks are for enums or not
            if (onlyEnums)
            {
                // Ticks are only for enums
                // Increase the distance based on how many more ticks were computed versus the
                // maximum ticks - using round makes increase double, triple, quadruple, etc
                tickDist *= Math.round(computedNumberOfMajorTicks / numberOfMajorTicks);
            }
            else
            {
                // Ticks are not for enums
                // If halving the computed ticks makes it less than the max ticks, halve it
                // Otherwise if reducing by 5x makes it less than the max ticks, reduce by 5x
                if (computedNumberOfMajorTicks / 2 < numberOfMajorTicks)
                {
                    tickDist *= 2;
                }
                else if (computedNumberOfMajorTicks / 5 < numberOfMajorTicks)
                {
                    tickDist *= 5;
                }
            }
        }
        else if (!onlyEnums) // Only use fractional ticks if the range is not enumerated
        {
            // Computed number of ticks is more dense than the maximum allowed ticks

            let x5 = computedNumberOfMajorTicks * 5;
            let x2 = computedNumberOfMajorTicks * 2;
            // If we can max the tick data-space distance 5x smaller and the amount of ticks that
            // fit are still less than the max ticks and the pixel-space distance between ticks
            // at least half the ideal pixel-space distance then reduce by 5x.
            // If not, apply the same chack using 2x
            if (x5 < numberOfMajorTicks && (x5 * pxSpacingForTicks) / 2 >= numPx)
            {
                tickDist /= 5;
            }
            else if (x2 < numberOfMajorTicks && (x2 * pxSpacingForTicks) / 2 >= numPx)
            {
                tickDist /= 2;
            }
        }

        let minValue = Math.round(valueRange.min / tickDist - 1) * tickDist;
        let maxValue = Math.round(valueRange.max / tickDist + 1) * tickDist;

        if (!isFinite(minValue) || !isFinite(maxValue))
        {
            minValue = valueRange.min;
            maxValue = valueRange.max;
        }

        let tickPrecision = 0;
        if (logRange < 0) tickPrecision = 1 - logRange;

        return new ChartTickInfo(minValue, maxValue, tickDist, tickPrecision);
    }

    static scaleCanvas(canvasElem: HTMLCanvasElement,
                       debouncingWidth: number,
                       debouncingHeight: number)
    {
        let canvas = canvasElem.getContext("2d");

        let pixelRatio = devicePixelRatio || 1;

        if (!debouncingWidth) debouncingWidth = 1;
        if (!debouncingHeight) debouncingHeight = 1;

        let scaledWidth  = debouncingWidth * pixelRatio;
        let scaledHeight = debouncingHeight * pixelRatio;

        if (canvasElem.width != scaledWidth || canvasElem.height != scaledHeight)
        {
            canvasElem.width         = scaledWidth;
            canvasElem.height        = scaledHeight;
            canvasElem.style.display = "block";
            canvasElem.style.width   = debouncingWidth + "px";
            canvasElem.style.height  = debouncingHeight + "px";

            canvas.scale(pixelRatio, pixelRatio);
        }
    }

    static getCanvasPNG(canvas: HTMLCanvasElement,
                        backgroundColor?: string): string
    {
        if (backgroundColor)
        {
            let context       = canvas.getContext("2d");
            let prevOperation = context.globalCompositeOperation;
            let chart         = context.getImageData(0, 0, canvas.width, canvas.height);

            context.fillStyle                = backgroundColor;
            context.globalCompositeOperation = "destination-over";
            context.fillRect(0, 0, canvas.clientWidth, canvas.clientHeight);

            let pngURL = canvas.toDataURL();

            context.globalCompositeOperation = "copy";
            context.putImageData(chart, 0, 0);
            context.globalCompositeOperation = prevOperation;

            return pngURL;
        }
        else
        {
            return canvas.toDataURL();
        }
    }

    pixelRatio()
    {
        // Give headless browsers a high ratio
        if (/HeadlessChrome/.test(window.navigator.userAgent)) return 2;

        // Determine scaling ratio using device DPI
        let devicePixelRatio  = window.devicePixelRatio || 1;
        let backingStoreRatio = (<any>this.canvas).webkitBackingStorePixelRatio || (<any>this.canvas).mozBackingStorePixelRatio || (<any>this.canvas).msBackingStorePixelRatio
                                || (<any>this.canvas).oBackingStorePixelRatio || (<any>this.canvas).backingStorePixelRatio || 1;

        return devicePixelRatio / backingStoreRatio;
    }

    ellipsis(font: string | ChartFont,
             text: string,
             maxWidth: number)
    {
        const ellipsis = "...";

        let width         = this.measureText(font, text);
        let ellipsisWidth = this.measureText(font, ellipsis);
        if (width <= maxWidth || width <= ellipsisWidth)
        {
            return text;
        }

        let len = text.length;
        while (width >= maxWidth - ellipsisWidth && len-- > 0)
        {
            text  = text.substring(0, len);
            width = this.measureText(font, text);
        }
        return text + ellipsis;
    }

    longestText(font: string | ChartFont,
                texts: string[]): number
    {
        let longest = 0;

        for (let text of texts)
        {
            longest = Math.max(longest, this.measureText(font, text));
        }

        return longest;
    }

    public static measureText(font: string,
                              text: string,
                              canvas?: CanvasRenderingContext2D): number
    {
        if (!canvas) canvas = ChartHelpers.getContext();

        canvas.font     = font;
        let textMetrics = canvas.measureText(text);
        return textMetrics.actualBoundingBoxRight - textMetrics.actualBoundingBoxLeft;
    }

    measureText(font: string | ChartFont,
                text: string): number
    {
        if (!text) return 0;

        if (font instanceof ChartFont)
        {
            font = font.toFontName();
        }

        let textLengths = this.textLengthsPerFont[font];
        if (!textLengths)
        {
            textLengths = {};
            this.textLengthsPerFont[font] = textLengths;
        }

        let len = textLengths[text];
        if (!len)
        {
            len = ChartHelpers.measureText(font, text, this.canvas);
            textLengths[text] = len;
        }

        return len;
    }

    measureLineHeight(font: ChartFont): number
    {
        return this.placeTextInBox(font, TextPlacement.Left, TextOrientation.Horizontal, "Measure this for line height", 0, 0, BoxAnchor.Left, 0).height;
    }

    computeMaxFontSize(text: string,
                       font: ChartFont,
                       width: number,
                       height: number,
                       maxFontSize: number    = 100,
                       maxLines?: number,
                       allowWrapping: boolean = true): number
    {
        const fromCanvasBufferScalar = 0.95; // sometimes canvas renders slightly longer/shorter than standard text: provide some buffer
        const fontFits               = (size: number) =>
        {
            font.size       = size;
            let textMapping = this.mapText(text, width * fromCanvasBufferScalar, font, maxLines, allowWrapping);
            if (!textMapping.fits) return 1;

            let lineHeight     = font.lineHeight * size;
            let linesAvailable = Math.floor(height / lineHeight);
            return textMapping.lines.length <= linesAvailable ? -1 : 1;
        };

        let startFont       = font.size;
        let fontSizeOptions = [...new Array(maxFontSize + 1).keys()];
        let fontSize        = ~UtilsService.binarySearch(fontSizeOptions, 0, fontFits) - 1;
        font.size           = startFont;

        return fontSize;
    }

    mapText(text: string,
            maxWidth: number,
            font: ChartFont        = new ChartFont(),
            maxLines?: number,
            allowWrapping: boolean = true): TextMapping
    {
        let printLines = [];
        let fits       = true;
        if (text)
        {
            let wordLines = text.split("\n")
                                .map((line) => line.split(/\s+/));

            for (let wordLine of wordLines)
            {
                let line      = "";
                let addedSome = false;
                for (let word of wordLine)
                {
                    let toAdd = word + " ";
                    if (this.measureText(font, line + toAdd) < maxWidth)
                    {
                        line += toAdd;
                        addedSome = true;
                    }
                    else
                    {
                        if (fits && !addedSome && this.measureText(font, line) > maxWidth) fits = false;

                        printLines.push(line);
                        addedSome = false;
                        line      = toAdd;

                        if (!allowWrapping) fits = false;
                    }
                }
                if (fits && !addedSome && this.measureText(font, line) > maxWidth) fits = false;
                printLines.push(line);

                if (maxLines && printLines.length > maxLines)
                {
                    return new TextMapping(printLines, false);
                }
            }
        }

        return new TextMapping(printLines, fits);
    }

    placeTextInBox(font: ChartFont,
                   textPlacement: TextPlacement,
                   textOrientation: TextOrientation,
                   text: string,
                   x: number,
                   y: number,
                   boxAnchor: BoxAnchor,
                   boxPadding: number): ChartTextBox
    {
        let angle = 0;
        switch (textOrientation)
        {
            case TextOrientation.VerticalAscending:
                angle = -Math.PI / 2;
                break;

            case TextOrientation.VerticalDescending:
                angle = Math.PI / 2;
                break;
        }
        return this.placeTextInBoxWithAngle(font, textPlacement, angle, text, x, y, boxAnchor, boxPadding);
    }

    placeTextInBoxWithAngle(font: ChartFont,
                            textPlacement: TextPlacement,
                            textAngle: number,
                            text: string,
                            x: number,
                            y: number,
                            boxAnchor: BoxAnchor,
                            boxPadding: number): ChartTextBox
    {
        let textWidth  = 0;
        let textHeight = 0;
        let lines      = text.split("\n");
        let textLines  = [];
        let lineHeight = font.size * 1.2;

        // Find the bounding box of every line of text and the bounding box on all lines together
        // We use the x/y of ChartTextLine to store the width (x) and bottom y coordinate (y) temporarily
        for (let line of lines)
        {
            let width = this.measureText(font, line);

            textWidth = Math.max(textWidth, width);
            textHeight += lineHeight;

            textLines.push(new ChartTextLine(width, textHeight, width, lineHeight, line));
        }

        // Position all text lines in the text box
        textLines = textLines.map((line) =>
                                  {
                                      let x = boxPadding + (textWidth - line.x) / 2;
                                      let y = boxPadding + line.y;
                                      return new ChartTextLine(x, y, line.width, line.height, line.text);
                                  });

        // Calculate bounding axis coordinates
        let minX = 0;
        let maxX = textWidth + boxPadding * 2;
        let minY = 0;
        let maxY = textHeight + boxPadding * 2;

        // Set up a transform to modify all points based on orientation
        let orientationTransform = new Transform();
        orientationTransform.rotate(textAngle);

        // Find new min/max values
        let rotatedPoint = orientationTransform.applyXY(maxX, maxY);

        maxX = Math.max(minX, rotatedPoint.x);
        minX = Math.min(minX, rotatedPoint.x);
        maxY = Math.max(minY, rotatedPoint.y);
        minY = Math.min(minY, rotatedPoint.y);

        // Set up a transform to adjust for the anchor point
        let anchorTransform = new Transform();
        anchorTransform.translate(new ChartPixel(minX, minY));

        switch (boxAnchor)
        {
            case BoxAnchor.TopLeft:
            case BoxAnchor.Left:
            case BoxAnchor.BottomLeft:
                anchorTransform.translate(new ChartPixel(-minX, 0));
                break;

            case BoxAnchor.Center:
            case BoxAnchor.Top:
            case BoxAnchor.Bottom:
                anchorTransform.translate(new ChartPixel(-((minX + maxX) / 2), 0));
                break;

            case BoxAnchor.TopRight:
            case BoxAnchor.Right:
            case BoxAnchor.BottomRight:
                anchorTransform.translate(new ChartPixel(-maxX, 0));
                break;
        }

        switch (boxAnchor)
        {
            case BoxAnchor.TopLeft:
            case BoxAnchor.Top:
            case BoxAnchor.TopRight:
                anchorTransform.translate(new ChartPixel(0, -minY));
                break;

            case BoxAnchor.Left:
            case BoxAnchor.Center:
            case BoxAnchor.Right:
                anchorTransform.translate(new ChartPixel(0, -((minY + maxY) / 2)));
                break;

            case BoxAnchor.BottomLeft:
            case BoxAnchor.Bottom:
            case BoxAnchor.BottomRight:
                anchorTransform.translate(new ChartPixel(0, -maxY));
                break;
        }

        // Calculate the final x/y and width/height
        let width  = maxX - minX;
        let height = maxY - minY;
        let point  = anchorTransform.applyXY(x, y);

        // Return a text bounding box with line and orientation data
        return new ChartTextBox(point.x, point.y, width, height, textLines, textAngle);
    }

    drawTextInBox(font: ChartFont,
                  textPlacement: TextPlacement,
                  textOrientation: TextOrientation,
                  text: string,
                  textStyle: string,
                  x: number,
                  y: number,
                  boxAnchor: BoxAnchor,
                  boxPadding: number,
                  boxStyle?: string,
                  textBlur?: number): ChartTextBox
    {
        // Calculate the containing box for the text
        let box = this.placeTextInBox(font, textPlacement, textOrientation, text, x, y, boxAnchor, boxPadding);

        let translation: Vector2;
        switch (textOrientation)
        {
            case TextOrientation.VerticalAscending:
                translation = new Vector2(0, box.height);
                break;

            case TextOrientation.VerticalDescending:
                translation = new Vector2(box.width, 0);
                break;
        }

        return this.drawAngledBoxText(box, font, text, textStyle, translation, boxStyle, textBlur);
    }

    drawAngledBoxText(box: ChartTextBox,
                      font: ChartFont,
                      text: string,
                      textStyle: string,
                      preRotationTranslation: Vector2,
                      boxStyle: string,
                      textBlur: number): ChartTextBox
    {
        // Save canvas state before drawing
        this.canvas.save();

        // Move canvas origin to the box's top-left
        this.canvas.translate(box.x, box.y);

        // Draw the box if a box style is given
        if (boxStyle != undefined)
        {
            this.canvas.fillStyle = boxStyle;
            this.canvas.fillRect(0, 0, box.width, box.height);
        }

        if (preRotationTranslation) this.canvas.translate(preRotationTranslation.x, preRotationTranslation.y);
        this.canvas.rotate(box.angle);

        // Prepare to draw text
        this.canvas.font         = font.toFontName();
        this.canvas.fillStyle    = textStyle;
        this.canvas.textBaseline = "bottom";

        // Draw all text lines
        for (let textLine of box.lines)
        {
            if (textBlur)
            {
                let scale = this.canvas.getTransform().a;

                this.canvas.save();
                this.canvas.beginPath();
                this.canvas.rect(textLine.x - textBlur - 10, (textLine.y - textLine.height) - textBlur, textLine.width + (textBlur * 2) + 20, textLine.height + (textBlur * 2));
                this.canvas.clip();
                this.canvas.shadowColor   = textStyle;
                this.canvas.shadowBlur    = textBlur * scale;
                this.canvas.shadowOffsetX = (textLine.width + textBlur + 10) * scale;
                this.canvas.fillText(textLine.text, textLine.x - (textLine.width + textBlur + 10), textLine.y);

                this.canvas.restore();
            }
            else
            {
                this.canvas.fillText(textLine.text, textLine.x, textLine.y);
            }
        }

        // Restore canvas state when done drawing
        this.canvas.restore();

        return box;
    }

    fillPattern(fill: HTMLImageElement,
                x: number,
                y: number,
                width: number,
                height: number)
    {
        // Scan and fill the given area with the pattern image as if it were tiled from 0,0

        // Find maximums
        let maxX = x + width;
        let maxY = y + height;

        // Scan area by x
        let currentX = x;
        while (currentX < maxX)
        {

            // Calculate x dimension portion of image to draw
            let sx     = currentX % fill.width;
            let sWidth = Math.min(fill.width - sx, maxX - currentX);
            let dx     = currentX;
            let dWidth = sWidth;

            // Scan area by y
            let currentY = y;
            while (currentY < maxY)
            {

                // Calculate y dimension portion of image to draw
                let sy      = currentY % fill.height;
                let sHeight = Math.min(fill.height - sy, maxY - currentY);
                let dy      = currentY;
                let dHeight = sHeight;

                // Draw image subset to corresponding target fill area subset
                this.canvas.drawImage(fill, sx, sy, sWidth, sHeight, dx, dy, dWidth, dHeight);

                // Progress the y scan
                currentY += dHeight;
            }

            // Progress the x scan
            currentX += dWidth;
        }
    }

    drawImage(source: HTMLImageElement,
              x: number,
              y: number)
    {
        this.canvas.drawImage(source, x - source.width / 2, y - source.height / 2, source.width, source.height);
    }

    drawCanvas(source: HTMLCanvasElement,
               x: number,
               y: number)
    {
        this.canvas.drawImage(source, x - source.width / 2, y - source.height / 2, source.width, source.height);
    }

    drawPoint(source: ChartPointStyle,
              style: string,
              radius: number,
              x: number,
              y: number,
              strokeWidthMultiplier: number = 1)
    {
        if (isNaN(radius) || radius <= 0)
        {
            return;
        }

        let ctx = this.canvas;
        ctx.save();

        ctx.lineWidth   = strokeWidthMultiplier * radius / 2;
        ctx.strokeStyle = style;
        ctx.fillStyle   = style;

        let offset;

        switch (source)
        {
            case ChartPointStyle.pinCheck:
                offset = this.pin(x, y, radius, 105, radius * 0.4, style);
                this.checkMark(x, y + offset, radius, style);
                break;

            case ChartPointStyle.pinExclamation:
                offset = this.pin(x, y, radius, 105, radius * 0.4, style);
                this.exclamationMark(x, y + offset, radius, style);
                break;

            case ChartPointStyle.circle:
                ctx.beginPath();
                ctx.arc(x, y, radius, 0, Math.PI * 2);
                ctx.closePath();
                ctx.fill();
                break;

            case ChartPointStyle.triangle:
            {
                ctx.beginPath();
                let edgeLength = 3 * radius / Math.sqrt(3);
                let height     = edgeLength * Math.sqrt(3) / 2;
                ctx.moveTo(x - edgeLength / 2, y + height / 3);
                ctx.lineTo(x + edgeLength / 2, y + height / 3);
                ctx.lineTo(x, y - 2 * height / 3);
                ctx.closePath();
                ctx.fill();
                break;
            }

            case ChartPointStyle.rect:
            {
                let size = 1 / Math.SQRT2 * radius;
                ctx.beginPath();
                ctx.fillRect(x - size, y - size, 2 * size, 2 * size);
                ctx.strokeRect(x - size, y - size, 2 * size, 2 * size);
                break;
            }

            case ChartPointStyle.rectRounded:
            {
                let offset   = radius / Math.SQRT2;
                let leftX    = x - offset;
                let topY     = y - offset;
                let sideSize = Math.SQRT2 * radius;
                this.roundedRect(leftX, topY, sideSize, sideSize, radius / 2);
                ctx.fill();
                break;
            }

            case ChartPointStyle.rectRot:
            {
                let size = 1 / Math.SQRT2 * radius;
                ctx.beginPath();
                ctx.moveTo(x - size, y);
                ctx.lineTo(x, y + size);
                ctx.lineTo(x + size, y);
                ctx.lineTo(x, y - size);
                ctx.closePath();
                ctx.fill();
                break;
            }

            case ChartPointStyle.cross:
                ctx.beginPath();
                ctx.moveTo(x, y + radius);
                ctx.lineTo(x, y - radius);
                ctx.moveTo(x - radius, y);
                ctx.lineTo(x + radius, y);
                ctx.closePath();
                ctx.stroke();
                break;

            case ChartPointStyle.crossRot:
            {
                ctx.beginPath();
                let xOffset = Math.cos(Math.PI / 4) * radius;
                let yOffset = Math.sin(Math.PI / 4) * radius;
                ctx.moveTo(x - xOffset, y - yOffset);
                ctx.lineTo(x + xOffset, y + yOffset);
                ctx.moveTo(x - xOffset, y + yOffset);
                ctx.lineTo(x + xOffset, y - yOffset);
                ctx.closePath();
                ctx.stroke();
                break;
            }

            case ChartPointStyle.star:
            {
                ctx.beginPath();
                ctx.moveTo(x, y + radius);
                ctx.lineTo(x, y - radius);
                ctx.moveTo(x - radius, y);
                ctx.lineTo(x + radius, y);
                let xOffset = Math.cos(Math.PI / 4) * radius;
                let yOffset = Math.sin(Math.PI / 4) * radius;
                ctx.moveTo(x - xOffset, y - yOffset);
                ctx.lineTo(x + xOffset, y + yOffset);
                ctx.moveTo(x - xOffset, y + yOffset);
                ctx.lineTo(x + xOffset, y - yOffset);
                ctx.closePath();
                ctx.stroke();
                break;
            }

            case ChartPointStyle.line:
                ctx.beginPath();
                ctx.moveTo(x - radius, y);
                ctx.lineTo(x + radius, y);
                ctx.closePath();
                ctx.stroke();
                break;

            case ChartPointStyle.dash:
                ctx.beginPath();
                ctx.moveTo(x, y);
                ctx.lineTo(x + radius, y);
                ctx.closePath();
                ctx.stroke();
                break;

            case ChartPointStyle.target:
                ctx.beginPath();
                ctx.arc(x, y, radius, 0, Math.PI * 2);
                ctx.moveTo(x, y + radius);
                ctx.lineTo(x, y - radius);
                ctx.moveTo(x - radius, y);
                ctx.lineTo(x + radius, y);
                ctx.closePath();
                ctx.stroke();
                break;
        }

        ctx.restore();
    }

    pin(x: number,
        y: number,
        radius: number,
        angle: number,
        thickness: number,
        color: string): number
    {
        let ctx = this.canvas;
        let d2r = Math.PI / 180;
        let r   = radius;
        let r2  = r + thickness;
        let a   = angle * d2r;
        let a2  = (90 * d2r) - (a / 2);
        let h   = r2 / Math.sin(a2);
        let dx  = r2 * Math.cos(a2);
        let dy  = r2 * Math.sin(a2);

        ctx.save();

        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.moveTo(x, y);
        ctx.lineTo(x + dx, y - h + dy);
        ctx.lineTo(x - dx, y - h + dy);
        ctx.closePath();
        ctx.fill();

        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.arc(x, y - h, r2, 0, Math.PI * 2);
        ctx.closePath();
        ctx.fill();

        ctx.fillStyle = "white";
        ctx.beginPath();
        ctx.arc(x, y - h, r, 0, Math.PI * 2);
        ctx.closePath();
        ctx.fill();

        ctx.restore();

        // Return vertical offset to center of pin
        return -h;
    }

    exclamationMark(x: number,
                    y: number,
                    radius: number,
                    color: string)
    {
        let ctx    = this.canvas;
        let width  = radius * 0.3;
        let bottom = y + radius;
        let top    = y - radius;

        ctx.save();

        ctx.strokeStyle = color;
        ctx.lineWidth   = width;

        ctx.beginPath();
        ctx.moveTo(x, bottom - width);
        ctx.lineTo(x, bottom - width * 2);
        ctx.moveTo(x, bottom - width * 2.5);
        ctx.lineTo(x, top + width);
        ctx.closePath();
        ctx.stroke();

        ctx.restore();
    }

    checkMark(x: number,
              y: number,
              radius: number,
              color: string)
    {
        let ctx    = this.canvas;
        let width  = radius * 0.3;
        let bottom = y + radius;
        let top    = y - radius;

        ctx.save();

        ctx.strokeStyle = color;
        ctx.lineWidth   = width;

        ctx.beginPath();
        ctx.moveTo(x - radius + width, y);
        ctx.lineTo(x - (width * 0.75), bottom - (width * 2));
        ctx.lineTo(x + (radius / 2) + (width * 0.25), top + width * 1.75);
        ctx.stroke();

        ctx.restore();
    }

    roundedRect(x: number,
                y: number,
                width: number,
                height: number,
                radius: number)
    {
        this.roundedRectExplicit(x, y, width, height, radius, radius, radius, radius);
    }

    roundedRectExplicit(x: number,
                        y: number,
                        width: number,
                        height: number,
                        topLeftRadius: number,
                        topRightRadius: number,
                        botRightRadius: number,
                        botLeftRadius: number)
    {
        let ctx = this.canvas;

        let maxRadius = Math.min(width / 2, height / 2);
        let radius    = Math.max(0, topLeftRadius, topRightRadius, botRightRadius, botLeftRadius);
        let getRadius = (cornerRadius: number) => Math.min(isNaN(cornerRadius) || cornerRadius < 0 ? radius : cornerRadius, maxRadius);
        ctx.beginPath();
        if (radius)
        {
            topLeftRadius  = getRadius(topLeftRadius);
            topRightRadius = getRadius(topRightRadius);
            botRightRadius = getRadius(botRightRadius);
            botLeftRadius  = getRadius(botLeftRadius);

            ctx.moveTo(x + topLeftRadius, y);
            ctx.lineTo(x + width - topRightRadius, y);
            ctx.quadraticCurveTo(x + width, y, x + width, y + topRightRadius);
            ctx.lineTo(x + width, y + height - botRightRadius);
            ctx.quadraticCurveTo(x + width, y + height, x + width - botRightRadius, y + height);
            ctx.lineTo(x + botLeftRadius, y + height);
            ctx.quadraticCurveTo(x, y + height, x, y + height - botLeftRadius);
            ctx.lineTo(x, y + topLeftRadius);
            ctx.quadraticCurveTo(x, y, x + topLeftRadius, y);
        }
        else
        {
            ctx.rect(x, y, width, height);
        }
        ctx.closePath();
    }

    static roundedRectHitCheck(x: number,
                               y: number,
                               rectX: number,
                               rectY: number,
                               rectWidth: number,
                               rectHeight: number,
                               borderRadius: number): boolean
    {
        borderRadius = borderRadius || 0;
        let rx       = Math.min(borderRadius, rectWidth / 2);
        let ry       = Math.min(borderRadius, rectHeight / 2);
        let rectCore = new ChartBox(rectX + rx, rectY + ry,
                                    rectWidth - rx * 2, rectHeight - ry * 2);

        let horizontal: RelativeLocation.Left | RelativeLocation.Right;
        if (x < rectCore.x)
        {
            horizontal = RelativeLocation.Left;
        }
        else if (x > rectCore.right)
        {
            horizontal = RelativeLocation.Right;
        }

        let rxSquared = rx * rx;
        let rySquared = ry * ry;
        if (y < rectCore.y)
        {
            switch (horizontal)
            {
                case RelativeLocation.Left:
                    if (x > rectCore.x - rx && y > rectCore.y - ry)
                    {
                        let dxSquared = Math.pow(rectCore.x - x, 2);
                        return rectCore.y - y <= Math.sqrt(rySquared - rySquared * dxSquared / rxSquared);
                    }
                    return false;

                case RelativeLocation.Right:
                    if (x < rectCore.right + rx && y > rectCore.y - ry)
                    {
                        let dxSquared = Math.pow(x - rectCore.right, 2);
                        return rectCore.y - y <= Math.sqrt(rySquared - rySquared * dxSquared / rxSquared);
                    }
                    return false;

                default:
                    let upperBox = new ChartBox(rectCore.x, rectCore.y - ry, rectCore.width, ry);
                    return upperBox.hitCheck(x, y);
            }
        }
        else if (y > rectCore.bottom)
        {
            switch (horizontal)
            {
                case RelativeLocation.Left:
                    if (x > rectCore.x - rx && y < rectCore.bottom + ry)
                    {
                        let dxSquared = Math.pow(rectCore.x - x, 2);
                        return y - rectCore.bottom <= Math.sqrt(rySquared - rySquared * dxSquared / rxSquared);
                    }
                    return false;

                case RelativeLocation.Right:
                    if (x < rectCore.right + rx && y < rectCore.bottom + ry)
                    {
                        let dxSquared = Math.pow(x - rectCore.right, 2);
                        return y - rectCore.bottom <= Math.sqrt(rySquared - rySquared * dxSquared / rxSquared);
                    }

                    return false;

                default:
                    let belowBox = new ChartBox(rectCore.x, rectCore.bottom, rectCore.width, ry);
                    return belowBox.hitCheck(x, y);
            }
        }
        else if (horizontal)
        {
            let sideBox = horizontal === RelativeLocation.Left ?
                new ChartBox(rectCore.x - rx, rectCore.y, rx, rectCore.height) :
                new ChartBox(rectCore.right, rectCore.y, rx, rectCore.height);
            return sideBox.hitCheck(x, y);
        }
        else
        {
            return true;
        }
    }
}

export enum CanvasZoneSelectionType
{
    Area           = "Area",
    AreaInverted   = "AreaInverted",
    Point          = "Point",
    XRange         = "XRange",
    XRangeInverted = "XRangeInverted",
    YRange         = "YRange",
    YRangeInverted = "YRangeInverted",
}

export function overlapsRange(low1: number,
                              high1: number,
                              low2: number,
                              high2: number): boolean
{
    return !(high2 < low1 || low2 > high1);
}

export class CanvasZoneSelection
{
    public static readonly tooltipBorderRadius: number = 5;

    private m_chartSource: ChartPointSource<any>;
    get chartSource(): ChartPointSource<any>
    {
        return this.m_chartSource;
    }

    get id(): string
    {
        return (this.title || "") + (this.description || "");
    }

    public hideTooltip: boolean;
    public title         = "";
    public description   = "";
    public invalidReason = "";

    public panel: number;

    private m_marker: AnnotationMarker;
    public get marker(): AnnotationMarker
    {
        if (!this.m_marker)
        {
            this.m_marker = new AnnotationMarker(this);
        }
        return this.m_marker;
    }

    public enabled: boolean = true;

    private m_inViewX: boolean = true;
    private m_inViewY: boolean = true;

    get inView(): boolean
    {
        return this.m_inViewX && this.m_inViewY && !this.outOfViewMessage;
    }

    get showing(): boolean
    {
        return this.enabled && this.inView;
    }

    private m_outOfViewMessage: string = "";
    get outOfViewMessage(): string
    {
        return this.invalidReason || this.m_outOfViewMessage;
    }

    public tooltipDragging: boolean = false;
    public tooltipOffset            = new Vector2(0, 0);

    public tooltipBox: ChartBox;

    private m_hasValues: boolean = false;
    get hasValues(): boolean
    {
        return this.m_hasValues;
    }

    private m_hasCorners: boolean = false;
    get hasCorners(): boolean
    {
        return this.m_hasCorners;
    }

    private m_minX: number;
    get minX(): number
    {
        return this.m_minX;
    }

    private m_maxX: number;
    get maxX(): number
    {
        return this.m_maxX;
    }

    private m_minY: number;
    get minY(): number
    {
        return this.m_minY;
    }

    private m_maxY: number;
    get maxY(): number
    {
        return this.m_maxY;
    }

    private firstCorner: Vector2;
    private secondCorner: Vector2;

    get xCoord1(): number
    {
        return this.firstCorner?.x;
    }

    get yCoord1(): number
    {
        return this.firstCorner?.y;
    }

    get xCoord2(): number
    {
        return this.secondCorner?.x;
    }

    get yCoord2(): number
    {
        return this.secondCorner?.y;
    }

    get clip(): ChartPixel
    {
        let x1 = this.xCoord1;
        let y1 = this.yCoord1;

        if (this.type === CanvasZoneSelectionType.Point) return new ChartPixel(x1, y1);

        let x2 = this.xCoord2;
        let y2 = this.yCoord2;

        return new ChartBox(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    private constructor(readonly type: CanvasZoneSelectionType)
    {
        switch (this.type)
        {
            case CanvasZoneSelectionType.XRangeInverted:
            case CanvasZoneSelectionType.YRangeInverted:
            case CanvasZoneSelectionType.AreaInverted:
                this.hideTooltip = true;
                break;

            default:
                this.hideTooltip = false;
                break;
        }
    }

    public static fromCoordinates(type: CanvasZoneSelectionType,
                                  firstCorner: Vector2,
                                  secondCorner: Vector2): CanvasZoneSelection
    {
        let selection = new CanvasZoneSelection(type);

        selection.firstCorner  = firstCorner;
        selection.secondCorner = secondCorner;
        selection.m_hasCorners = true;
        return selection;
    }

    public static fromValues(type: CanvasZoneSelectionType,
                             minX: number,
                             minY: number,
                             maxX: number,
                             maxY: number)
    {
        let selection = new CanvasZoneSelection(type);
        if (type === CanvasZoneSelectionType.Point)
        {
            selection.m_minX = minX;
            selection.m_minY = minY;
        }
        else
        {
            selection.m_minX = Math.min(minX, maxX);
            selection.m_maxX = Math.max(minX, maxX);
            selection.m_minY = Math.min(minY, maxY);
            selection.m_maxY = Math.max(minY, maxY);
        }

        selection.m_hasValues = true;

        return selection;
    }

    public static fromChartPoint(point: ChartPoint<any>)
    {
        let selection = new CanvasZoneSelection(CanvasZoneSelectionType.Point);

        selection.m_chartSource = point.owningSource;

        selection.m_minX      = point.timestampInMillisec;
        selection.m_minY      = point.numberValue;
        selection.m_hasValues = true;

        selection.description = point.getTooltipText();

        return selection;
    }

    public copyState(other: CanvasZoneSelection)
    {
        if (!other) return;

        this.enabled   = other.enabled;
        this.m_inViewX = other.m_inViewX;
        this.m_inViewY = other.m_inViewY;
    }

    public tooltipHitCheck(x: number,
                           y: number): boolean
    {
        if (!this.showing || !this.tooltipBox) return false;

        return ChartHelpers.roundedRectHitCheck(x, y, this.tooltipBox.x, this.tooltipBox.y,
                                                this.tooltipBox.width, this.tooltipBox.height, CanvasZoneSelection.tooltipBorderRadius);
    }

    public establishForChart(transform: ChartValueTransform,
                             chartWindow: ChartTimeWindow,
                             group: ChartGroup)
    {
        if (!transform) return;

        this.establishCorners(transform);
        this.updateUnviewableMessage(transform, chartWindow, group);
    }

    private updateUnviewableMessage(transform: ChartValueTransform,
                                    chartWindow: ChartTimeWindow,
                                    group: ChartGroup)
    {
        if (!transform)
        {
            this.m_inViewX          = this.m_inViewY = false;
            this.m_outOfViewMessage = "Not in view";
            return;
        }

        let xValue = this.marker.x;
        switch (this.type)
        {
            case CanvasZoneSelectionType.YRange:
            case CanvasZoneSelectionType.YRangeInverted:
                this.m_inViewX = true;
                break;

            case CanvasZoneSelectionType.Point:
                this.m_inViewX = chartWindow.isInTimeRange(xValue);
                break;

            default:
                this.m_inViewX = overlapsRange(chartWindow.startMillisecond, chartWindow.endMillisecond, this.minX, this.maxX);
                break;
        }

        let yValue = transform.fromValueToYCoordinate(this.marker.y);
        switch (this.type)
        {
            case CanvasZoneSelectionType.XRange:
            case CanvasZoneSelectionType.XRangeInverted:
                this.m_inViewY = true;
                break;

            case CanvasZoneSelectionType.Point:
                this.m_inViewY = transform.clip.hitCheckVertical(yValue);
                break;

            default:
                this.m_inViewY = overlapsRange(transform.domain.minValue, transform.domain.maxValue, this.minY, this.maxY);
                break;
        }

        if (!this.m_inViewX)
        {
            this.m_outOfViewMessage = `Not in time range: ${MomentHelper.friendlyFormatConciseUS(xValue)}`;
        }
        else if (!this.m_inViewY)
        {
            let message = "Not in value range";
            let y       = this.marker.y;
            if (!isNaN(y))
            {
                message += `: ${y.toFixed(2)}`;
                let unitsLabel = group?.unitsLabel;
                if (unitsLabel) message += ` ${unitsLabel}`;
            }

            this.m_outOfViewMessage = message;
        }
        else
        {
            this.m_outOfViewMessage = "";
        }
    }

    private establishCorners(transform: ChartValueTransformer<ChartPointRange<any>>)
    {
        if (!transform || !this.hasValues || isNaN(transform.domain?.minValue) || isNaN(transform.domain?.maxValue))
        {
            this.m_hasCorners = false;
            return;
        }

        this.firstCorner = new Vector2(transform.fromMillisecondToXCoordinate(this.minX), transform.fromValueToYCoordinate(this.minY));
        if (this.type !== CanvasZoneSelectionType.Point) this.secondCorner = new Vector2(transform.fromMillisecondToXCoordinate(this.maxX), transform.fromValueToYCoordinate(this.maxY));

        this.m_hasCorners = true;
    }

    public establishValues(transform: ChartValueTransformer<ChartPointRange<any>>)
    {
        if (!this.hasCorners) return;

        if (this.type === CanvasZoneSelectionType.Point)
        {
            this.m_minX = transform.fromXCoordinateToMillisecond(this.firstCorner.x);
            this.m_minY = transform.fromYCoordinateToValue(this.firstCorner.y);
        }
        else
        {
            let x1 = this.firstCorner.x;
            let x2 = this.secondCorner.x;
            let y1 = this.firstCorner.y;
            let y2 = this.secondCorner.y;

            let swap;
            if (x1 > x2)
            {
                swap = x1;
                x1   = x2;
                x2   = swap;
            }

            if (y1 > y2)
            {
                swap = y1;
                y1   = y2;
                y2   = swap;
            }

            this.m_minX = transform.fromXCoordinateToMillisecond(x1);
            this.m_maxX = transform.fromXCoordinateToMillisecond(x2);
            this.m_minY = transform.fromYCoordinateToValue(y1);
            this.m_maxY = transform.fromYCoordinateToValue(y2);
        }

        this.m_hasValues = true;
    }
}

export class CanvasZoneSelector
{
    private m_completedSelection: CanvasZoneSelection;

    private firstCorner: Vector2;
    private secondCorner: Vector2;

    private m_containingClip: ChartClipArea;
    private m_type: CanvasZoneSelectionType;

    get selection(): CanvasZoneSelection
    {
        if (this.m_completedSelection) return this.m_completedSelection;

        return this.firstCorner && this.secondCorner ? CanvasZoneSelection.fromCoordinates(this.m_type, this.firstCorner, this.secondCorner) : null;
    }

    get selectionStarted(): boolean
    {
        return !!this.firstCorner;
    }

    get type(): CanvasZoneSelectionType
    {
        return this.m_type;
    }

    set type(type: CanvasZoneSelectionType)
    {
        if (this.m_type !== type)
        {
            this.clearCorners();
            this.m_type = type;
        }
    }

    prepareLayout(containingClip: ChartClipArea,
                  type?: CanvasZoneSelectionType)
    {
        this.m_containingClip = containingClip;
        this.m_type           = type;
    }

    private clearCorners()
    {
        this.firstCorner = this.secondCorner = null;
    }

    /**
     * returns true if selection was completed via a mouseup
     *
     * @param e
     * @param mouseDown
     * @param mouseUp
     */
    handleSelection(e: MouseTouchEvent,
                    mouseDown: boolean,
                    mouseUp: boolean): boolean
    {
        if (this.m_type == undefined) return false;

        if (mouseDown)
        {
            if (this.m_containingClip?.hitCheck(e.offsetX, e.offsetY))
            {
                this.firstCorner  = new Vector2(e.offsetX, e.offsetY);
                this.secondCorner = null;
            }
        }
        else if (mouseUp)
        {
            let selection = this.selection;
            if (selection)
            {
                this.m_completedSelection = selection;
                return true;
            }
        }
        else if (this.firstCorner)
        {
            if (this.m_containingClip?.hitCheck(e.offsetX, e.offsetY)) this.secondCorner = new Vector2(e.offsetX, e.offsetY);
        }

        return false;
    }

    clearSelection(): boolean
    {
        if (this.selectionStarted)
        {
            this.clearCorners();
            this.m_completedSelection = null;
            return true;
        }

        return false;
    }

    public static renderSelection(canvas: CanvasRenderingContext2D,
                                  clip: ChartClipArea,
                                  selection: CanvasZoneSelection)
    {
        if (!selection?.showing || !selection.hasCorners) return;

        let type = selection?.type;
        if (type == undefined || type === CanvasZoneSelectionType.Point) return;

        let x1    = selection.xCoord1;
        let x2    = selection.xCoord2;
        let xLow  = Math.min(x1, x2);
        let xHigh = Math.max(x1, x2);

        let y1    = selection.yCoord1;
        let y2    = selection.yCoord2;
        let yLow  = Math.min(y1, y2);
        let yHigh = Math.max(y1, y2);

        switch (type)
        {
            case CanvasZoneSelectionType.XRange:
                yLow  = clip.y;
                yHigh = yLow + clip.height;
                break;

            case CanvasZoneSelectionType.YRange:
                xLow  = clip.x;
                xHigh = xLow + clip.width;
                break;
        }

        clip.applyClipping(canvas, () =>
        {
            canvas.globalAlpha = 0.5;
            canvas.fillStyle   = "#d2d2d2";

            switch (type)
            {
                case CanvasZoneSelectionType.XRangeInverted:
                    canvas.fillRect(clip.x, clip.y, xLow - clip.x, clip.height);
                    canvas.fillRect(xHigh, clip.y, clip.width - (xHigh - AxisWidth), clip.height);
                    break;

                case CanvasZoneSelectionType.AreaInverted:
                    let yDiff = yHigh - yLow;
                    canvas.fillRect(clip.x, yLow, xLow - clip.x, yDiff);
                    canvas.fillRect(xHigh, yLow, clip.width - (xHigh - AxisWidth), yDiff);
                // fall through
                case CanvasZoneSelectionType.YRangeInverted:
                    canvas.fillRect(clip.x, clip.y, clip.width, yLow - clip.y);
                    canvas.fillRect(clip.x, yHigh, clip.width, clip.y + clip.height - yHigh);
                    break;

                default:
                    canvas.fillRect(xLow, yLow, xHigh - xLow, yHigh - yLow);
                    break;
            }
        });
    }
}

export class AnnotationMarker extends ChartMarker<CanvasZoneSelection>
{
    private static readonly baseColor = "#959595";

    public readonly x: number;
    public readonly y: number;

    public readonly shape: ChartPointStyle        = ChartPointStyle.target;
    public readonly size: number                  = 12;
    public readonly strokeWidthMultiplier: number = 0.8;

    get label(): string
    {
        return this.source.title;
    }

    get tooltip(): string
    {
        return "";
    }

    get color(): string
    {
        return AnnotationMarker.baseColor;
    }

    get navigable(): boolean
    {
        return false;
    }

    constructor(selection: CanvasZoneSelection)
    {
        super(selection);

        switch (selection.type)
        {
            case CanvasZoneSelectionType.Point:
                this.x = selection.minX;
                this.y = selection.minY;
                break;

            case CanvasZoneSelectionType.XRange:
            case CanvasZoneSelectionType.XRangeInverted:
                this.x          = (selection.minX + selection.maxX) / 2;
                this.yPlacement = VerticalChartMarkerPlacement.CENTER;
                break;

            case CanvasZoneSelectionType.YRange:
            case CanvasZoneSelectionType.YRangeInverted:
                this.y          = (selection.minY + selection.maxY) / 2;
                this.xPlacement = HorizontalChartMarkerPlacement.CENTER;
                break;

            case CanvasZoneSelectionType.Area:
            case CanvasZoneSelectionType.AreaInverted:
                this.x = (selection.minX + selection.maxX) / 2;
                this.y = (selection.minY + selection.maxY) / 2;
                break;
        }
    }

    public navigate(): void
    {
    }
}

export class ChartAnimation
{
    private readonly m_start: number = performance.now();
    private readonly m_end: number;

    private m_animating = true;
    public get animating(): boolean
    {
        return this.m_animating && this.m_progress < 1;
    }

    private m_progress: number;
    public get progress(): number
    {
        return this.m_progress;
    }

    constructor(private readonly m_animationFn: (progressDeci: number) => void,
                private readonly m_durationMs?: number)
    {
        if (!this.m_durationMs || this.m_durationMs < 0) this.m_durationMs = 500;
        this.m_end = this.m_start + this.m_durationMs;

        this.animate();
    }

    public endAnimation()
    {
        this.m_animating = false;
    }

    private animate()
    {
        if (this.m_animating)
        {
            this.m_progress = (Math.min(performance.now(), this.m_end) - this.m_start) / this.m_durationMs;
            requestAnimationFrame(() =>
                                  {
                                      this.m_animationFn(this.m_progress);
                                      if (this.m_progress < 1)
                                      {
                                          this.animate();
                                      }
                                      else
                                      {
                                          this.endAnimation();
                                      }
                                  });
        }
    }
}

export class ChartPanel
{
    leftAxis: ChartAxis;
    rightAxis: ChartAxis;
    groups: ChartGroup[]               = [];
    annotations: CanvasZoneSelection[] = [];
    chips: ChartChip[]                 = [];
    topPadding: number;
    borderColor: string                = "rgb(102, 102, 102)";
    gridColor: string                  = "rgba(102, 102, 102, 0.333)"; // == #cdcdcd on white
    zone: string;
    hideBottomAxis: boolean;
    hideBottomBorder: boolean;
    showAlerts: boolean;
    targetSource: ChartPointSource<any>;
}

export class ChartAxis
{
    label: string;
    color: string;
}

export class ChartGroup
{
    label: string;
    unitsLabel: string;
    useAsCategoryAxis: boolean = true;
    useAsLeftValueAxis: boolean;
    useAsRightValueAxis: boolean;
    noTicks: boolean;
    rangeOverride: ChartValueRange;

    sources: ChartPointSource<any>[] = [];
}

export enum Direction
{
    Up = 1, Right, Down, Left
}

export class CanvasRenderer
{
    public static renderTicks(helper: ChartHelpers,
                              transform: ChartValueConverter<any>,
                              ticks: number[],
                              tickLabels: string[], // generally for enums
                              font: ChartFont   = new ChartFont("#000000", undefined, 11),
                              outer: ChartClipArea,
                              inner: ChartClipArea,
                              axisLocation: RelativeLocation,
                              showFirst: boolean,
                              tickColor: string = "#666666")
    {
        if (!axisLocation || axisLocation === RelativeLocation.Top) return;

        let canvas            = helper.canvas;
        let anchor: BoxAnchor = axisLocation === RelativeLocation.Right ? BoxAnchor.BottomRight : BoxAnchor.BottomLeft;

        let start: number;
        let end: number;
        let tickLabel: number;

        let transformFn: (value: number) => number;
        let coordCheckFn: (coord: number) => boolean;
        let labelInvalidFn: (box: ChartTextBox) => boolean;
        let drawTickFn: (coord: number) => void;
        switch (axisLocation)
        {
            case RelativeLocation.Left:
            case RelativeLocation.Right:
                start     = axisLocation === RelativeLocation.Left ? inner.left : inner.right;
                end       = start + (axisLocation === RelativeLocation.Left ? 7 : -7);
                tickLabel = start + (axisLocation === RelativeLocation.Left ? 2 : -2);

                transformFn    = (y) => transform.fromValueToYCoordinate(y);
                coordCheckFn   = (coord) => coord < inner.bottom && coord >= inner.y;
                drawTickFn     = (coord) =>
                {
                    canvas.moveTo(start, coord);
                    canvas.lineTo(end, coord);
                };
                labelInvalidFn = (box) => box.top < inner.top || box.bottom > inner.bottom;
                break;

            case RelativeLocation.Bottom:
                start     = inner.bottom - 1;
                end       = start - 7;
                tickLabel = start;

                transformFn    = (x) => transform.fromMillisecondToXCoordinate(x);
                coordCheckFn   = (coord) => coord >= inner.left && coord < inner.right;
                drawTickFn     = (coord) =>
                {
                    canvas.moveTo(coord, start);
                    canvas.lineTo(coord, end);
                };
                labelInvalidFn = (box) => box.left < inner.left || box.right > inner.right;
        }

        for (let i = showFirst ? 0 : 1; i < ticks.length; i++)
        {
            let value = ticks[i];
            let coord = transformFn(value);
            if (coordCheckFn(coord))
            {
                canvas.beginPath();
                drawTickFn(coord);

                canvas.strokeStyle = tickColor;
                canvas.lineWidth   = 1;

                canvas.stroke();
            }

            let labelText = tickLabels?.length ? tickLabels[value] : value.toLocaleString();
            if (labelText)
            {
                let x = axisLocation === RelativeLocation.Bottom ? coord + 1 : tickLabel;
                let y = axisLocation === RelativeLocation.Bottom ? tickLabel : coord;

                // Validate that it would be fully visible, skip tick if not
                if (labelInvalidFn(helper.placeTextInBox(font, TextPlacement.Center, TextOrientation.Horizontal, labelText, x, y, anchor, 2)))
                {
                    continue;
                }

                helper.drawTextInBox(font, TextPlacement.Center, TextOrientation.Horizontal, labelText, font.color, x, y, anchor, 2);
            }
        }
    }

    /**
     * returns true if label was truncated
     *
     * @param helper
     * @param label
     * @param associatedColor
     * @param outer
     * @param inner
     * @param labelPadding
     * @param axisLocation
     * @param labelUnits
     */
    public static renderAxisLabel(helper: ChartHelpers,
                                  label: string,
                                  associatedColor: string,
                                  outer: ChartClipArea,
                                  inner: ChartClipArea,
                                  labelPadding: number,
                                  axisLocation: RelativeLocation,
                                  labelUnits?: string): boolean
    {
        if (!label || !axisLocation || axisLocation === RelativeLocation.Top) return false;

        let canvas         = helper.canvas;
        canvas.globalAlpha = 1.0;

        let labelSpace: number;
        if (axisLocation === RelativeLocation.Bottom)
        {
            labelSpace = outer.bottom - inner.bottom;
        }
        else
        {
            labelSpace = axisLocation === RelativeLocation.Left ? inner.left - outer.left : outer.right - inner.right;
        }
        let labelFont = new ChartFont("#000000", undefined, labelSpace - labelPadding * 2);

        let coloredBoxPadding = 1.5;
        let coloredBoxWidth   = labelFont.size - 2 * coloredBoxPadding;

        label           = label || "";
        let labelLength = helper.measureText(labelFont, label);

        const endOfLabel = `... ${labelUnits && `( ${labelUnits} )` || ""}`;
        let unitsLength  = helper.measureText(labelFont, endOfLabel);

        let x: number;
        let y               = (inner.bottom + inner.top) / 2 - (associatedColor ? (labelPadding + coloredBoxWidth) / 2 : 0);
        let orientation: TextOrientation;
        let anchor          = BoxAnchor.Center;
        let placement       = TextPlacement.Center;
        let availableLength = outer.bottom - outer.y;
        let truncated       = false;

        const updateLabelHelper = (noUnitsFn: () => void) =>
        {
            if (labelLength >= availableLength)
            {
                truncated = true;

                if (labelUnits)
                {
                    label = helper.mapText(label, availableLength - unitsLength, labelFont, 1).lines[0];
                    label = label ? label + endOfLabel : labelUnits;
                }
                else
                {
                    placement = TextPlacement.Left;
                    noUnitsFn();
                }
            }
        };

        switch (axisLocation)
        {
            case RelativeLocation.Bottom:
                availableLength = outer.right - outer.x;

                updateLabelHelper(() =>
                                  {
                                      x      = outer.x;
                                      anchor = BoxAnchor.Left;
                                  });

                if (!truncated) x = (inner.x + inner.right) / 2 - (associatedColor ? (labelPadding + coloredBoxWidth) / 2 : 0);

                y           = (outer.bottom + inner.bottom) / 2;
                orientation = TextOrientation.Horizontal;
                break;

            case RelativeLocation.Left:
                updateLabelHelper(() =>
                                  {
                                      y      = outer.bottom + labelPadding;
                                      anchor = BoxAnchor.Bottom;
                                  });

                x           = (outer.left + inner.left) / 2;
                orientation = TextOrientation.VerticalAscending;
                break;

            case RelativeLocation.Right:
                updateLabelHelper(() =>
                                  {
                                      y      = outer.y - labelPadding;
                                      anchor = BoxAnchor.Top;
                                  });

                x           = (outer.right + inner.right) / 2;
                orientation = TextOrientation.VerticalDescending;
                break;
        }

        let textBox = helper.drawTextInBox(labelFont, placement, orientation, label, labelFont.color, x, y, anchor, labelPadding);

        if (associatedColor)
        {
            switch (axisLocation)
            {
                case RelativeLocation.Bottom:
                    x += textBox.width / 2;
                    y += coloredBoxPadding - labelFont.size / 2;
                    break;

                case RelativeLocation.Left:
                case RelativeLocation.Right:
                    x += coloredBoxPadding - labelFont.size / 2;
                    y += textBox.height / 2;
                    break;
            }


            canvas.fillStyle = associatedColor;
            helper.roundedRect(x, y, coloredBoxWidth, coloredBoxWidth, 2.5);
            canvas.fill();
        }

        return truncated;
    }

    /**
     * @param helper
     * @param label
     * @param labelPadding
     * @param associatedColor
     * @param area
     */
    public static renderText(helper: ChartHelpers,
                             label: string,
                             labelPadding: number,
                             associatedColor: string,
                             area: ChartClipArea): void
    {
        if (!label) return;

        helper.canvas.globalAlpha = 1.0;

        let labelFont = new ChartFont(associatedColor, undefined, Math.min(area.height, 20));

        helper.drawTextInBox(labelFont,
                             TextPlacement.Center,
                             TextOrientation.Horizontal,
                             label,
                             labelFont.color,
                             (area.left + area.right) / 2,
                             (area.top + area.bottom) / 2,
                             BoxAnchor.Center,
                             labelPadding);
    }

    public static renderPoints(helper: ChartHelpers,
                               view: ChartPointsRenderView,
                               strokeStyle: string,
                               strokeStyleForMissing: string = "red",
                               strokeStyleForNoValue: string = "yellow",
                               limitWhenDense: boolean       = true)
    {
        let numPoints = view.length;
        if (numPoints > 1 && limitWhenDense)
        {
            let firstX = view.fromIndexToXCoordinate(0);
            let lastX  = view.fromIndexToXCoordinate(numPoints - 1);

            if (numPoints * 4 > (lastX - firstX))
            {
                // Range too dense: skip
                return;
            }
        }

        for (let i = 0; i < numPoints; i++)
        {
            let pixelX = view.fromIndexToXCoordinate(i);
            let pixelY = view.fromIndexToYCoordinate(i);

            let radius = 2;
            if (ChartPointRange.asSelected(view.flags[i])) radius *= 3;

            switch (ChartPointRange.asType(view.flags[i]))
            {
                case ChartPointType.Value:
                    helper.drawPoint(ChartPointStyle.circle, strokeStyle, radius, pixelX, pixelY);
                    break;

                case ChartPointType.Missing:
                    helper.drawPoint(ChartPointStyle.crossRot, strokeStyleForMissing, radius, pixelX, pixelY);
                    break;

                case ChartPointType.NoValue:
                    helper.drawPoint(ChartPointStyle.crossRot, strokeStyleForNoValue, radius, pixelX, pixelY);
                    break;
            }
        }
    }

    static safeMoveTo(canvas: CanvasRenderingContext2D,
                      x: number,
                      y: number)
    {
        // with massive absolute values (not sure of scale required), chrome ignores them and firefox canvas.lineTo breaks the line... this is to avoid that
        const multiplier = 1_000_000;
        let horiz        = canvas.canvas.width * multiplier;
        let vert         = canvas.canvas.height * multiplier;
        canvas.moveTo(UtilsService.clamp(-horiz, 2 * horiz, x), UtilsService.clamp(-vert, 2 * vert, y));
    }

    static safeLineTo(canvas: CanvasRenderingContext2D,
                      x: number,
                      y: number)
    {
        // with massive absolute values (not sure of scale required), chrome ignores them and firefox canvas.lineTo breaks the line... this is to avoid that
        const multiplier = 1_000_000;
        let horiz        = canvas.canvas.width * multiplier;
        let vert         = canvas.canvas.height * multiplier;
        canvas.lineTo(UtilsService.clamp(-horiz, 2 * horiz, x), UtilsService.clamp(-vert, 2 * vert, y));
    }

    public static renderStraight(canvas: CanvasRenderingContext2D,
                                 view: ChartPointsRenderView,
                                 yZeroOffset: number,
                                 strokeStyle: string,
                                 strokeStyleForMissing: string,
                                 strokeStyleForNoValue: string,
                                 lineType: ChartLineType,
                                 fillStyle: string)
    {
        // This method can either stroke or fill depending on the arguments. Stroke supersedes fill if both are provided.
        let canStroke = strokeStyle != null;
        let canFill   = fillStyle != null && !canStroke;

        let previousStroke: string;
        let firstPixelX: number;
        let firstPixelY: number;
        let lastPixelX: number;
        let lastPixelY: number;

        let numPoints = view.length;
        for (let i = 0; i < numPoints; i++)
        {
            let currentStroke: string;

            switch (ChartPointRange.asType(view.flags[i]))
            {
                case ChartPointType.Value:
                    currentStroke = strokeStyle;
                    break;

                case ChartPointType.Missing:
                    currentStroke = strokeStyleForMissing;
                    break;

                case ChartPointType.NoValue:
                    currentStroke = strokeStyleForNoValue;
                    break;
            }

            let pixelX = view.fromIndexToXCoordinate(i);
            let pixelY = view.fromIndexToYCoordinate(i);

            // If the value has become regular or irregular, we stroke the previous path and select a different color.
            if (canStroke && previousStroke !== undefined && previousStroke != currentStroke)
            {
                CanvasRenderer.safeLineTo(canvas, pixelX, pixelY);

                canvas.strokeStyle = previousStroke;
                canvas.stroke();

                firstPixelX = undefined;
            }

            if (firstPixelX === undefined)
            {
                firstPixelX = pixelX;
                firstPixelY = pixelY;

                canvas.beginPath();
                CanvasRenderer.safeMoveTo(canvas, pixelX, pixelY);
            }
            else
            {
                if (lineType == ChartLineType.StepLeft)
                {
                    // Over then up.
                    CanvasRenderer.safeLineTo(canvas, pixelX, lastPixelY);
                }
                else if (lineType == ChartLineType.StepRight)
                {
                    // Up then over.
                    CanvasRenderer.safeLineTo(canvas, lastPixelX, pixelY);
                }

                CanvasRenderer.safeLineTo(canvas, pixelX, pixelY);
            }

            lastPixelX     = pixelX;
            lastPixelY     = pixelY;
            previousStroke = currentStroke;
        }

        if (canStroke && previousStroke !== undefined)
        {
            canvas.strokeStyle = previousStroke;
            canvas.stroke();
        }
        else if (canFill && firstPixelX !== undefined && lastPixelX !== undefined)
        {
            CanvasRenderer.safeLineTo(canvas, lastPixelX, yZeroOffset);
            CanvasRenderer.safeLineTo(canvas, firstPixelX, yZeroOffset);

            canvas.fillStyle = fillStyle;
            canvas.fill();
        }
    }

    public static renderSmooth(canvas: CanvasRenderingContext2D,
                               view: ChartPointsRenderView,
                               yZeroOffset: number,
                               strokeStyle: string,
                               strokeStyleForMissing: string,
                               strokeStyleForNoValue: string,
                               smoothness: number,
                               fillStyle: string)
    {
        // This method can either stroke or fill depending on the arguments. Stroke supercedes fill if both are provided.
        let canStroke = strokeStyle != null;
        let canFill   = fillStyle != null && !canStroke;

        let previousStroke = undefined;

        let firstPixel: ChartPixel;
        let lastPixel: ChartPixel;
        // Control points determine the shape and intensity of the bezier curve between two 'knots,' or pixels.
        // Control points 0 and 1 correspond to the current segment, 2 is the first control point for the next segment.
        let controlPoint0;

        // Because the first and last knots have unique properties, we start with the second and end with the second to last knots in the loop.
        let numPoints = view.length;
        let pixelNminus2: ChartPixel;
        let pixelNminus1: ChartPixel;

        for (let i = 0; i < numPoints; i++)
        {
            let pixelN = new ChartPixel(view.fromIndexToXCoordinate(i), view.fromIndexToYCoordinate(i));

            if (pixelNminus2)
            {
                // We need three knots to determine the position for the center knot's control points.
                let {
                        controlPoint1,
                        controlPoint2
                    } = CanvasRenderer.getControlPoints(pixelNminus2, pixelNminus1, pixelN, smoothness);

                let currentStroke: string;

                switch (ChartPointRange.asType(view.flags[i - 1])) // We actually render the pixel N-1
                {
                    case ChartPointType.Value:
                        currentStroke = strokeStyle;
                        break;

                    case ChartPointType.Missing:
                        currentStroke = strokeStyleForMissing;
                        break;

                    case ChartPointType.NoValue:
                        currentStroke = strokeStyleForNoValue;
                        break;
                }

                // If the value has become regular or irregular, we stroke the previous path and select a different color.
                if (canStroke && previousStroke !== undefined && previousStroke != currentStroke)
                {
                    canvas.strokeStyle = previousStroke;
                    canvas.stroke();

                    canvas.beginPath();
                    canvas.moveTo(pixelNminus2.x, pixelNminus2.y);
                }

                // Designate the first pixel if not already designated.
                if (!firstPixel)
                {
                    firstPixel = pixelNminus2;

                    canvas.beginPath();
                    canvas.moveTo(pixelNminus2.x, pixelNminus2.y);
                }

                // If control point 0 has not been designated, then we are rendering the first segment.
                if (!controlPoint0)
                {
                    // The control point for the first knot will have the same coordinates as the knot, for simplicity.
                    controlPoint0 = pixelNminus2;
                }

                canvas.bezierCurveTo(controlPoint0.x, controlPoint0.y, controlPoint1.x, controlPoint1.y, pixelNminus1.x, pixelNminus1.y);

                // The first control point of the next segment will become the first control point of the current segment.
                controlPoint0 = controlPoint2;

                // Is this the second to last knot? If so, render the final segment.
                if (i == numPoints - 1)
                {
                    // The control point for the last knot will have the same coordinates as the knot, for simplicity.
                    controlPoint1 = pixelN;

                    canvas.bezierCurveTo(controlPoint0.x, controlPoint0.y, controlPoint1.x, controlPoint1.y, pixelN.x, pixelN.y);
                }

                lastPixel      = pixelN;
                previousStroke = currentStroke;
            }

            pixelNminus2 = pixelNminus1;
            pixelNminus1 = pixelN;
        }

        if (canStroke && previousStroke !== undefined)
        {
            canvas.strokeStyle = previousStroke;
            canvas.stroke();
        }
        else if (canFill && firstPixel && lastPixel)
        {
            canvas.lineTo(lastPixel.x, yZeroOffset);
            canvas.lineTo(firstPixel.x, yZeroOffset);

            canvas.fillStyle = fillStyle;
            canvas.fill();
        }
    }

    // Formula for deriving control points found at http://scaledinnovation.com/analytics/splines/aboutSplines.html
    private static getControlPoints(p1: ChartPixel,
                                    p2: ChartPixel,
                                    p3: ChartPixel,
                                    scale: number): { controlPoint1: ChartPixel, controlPoint2: ChartPixel }
    {
        // Calculate the distance between points via the hypotenuse.
        let dist0to1 = Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
        let dist1to2 = Math.sqrt(Math.pow(p3.x - p2.x, 2) + Math.pow(p3.y - p2.y, 2));

        // Ensure that the scaling constant lies between the logical extremes of 0 and 1, 0 rendering straight lines and 1 rendering exagerated curves.
        if (scale < 0) scale = 0;
        if (scale > 1) scale = 1;

        // Calculate the position on the tangent line on which the control points will fall based on scale and the distance between points.
        let scaleFactorA = scale * dist0to1 / (dist0to1 + dist1to2);
        let scaleFactorB = scale * dist1to2 / (dist0to1 + dist1to2);

        // Control Point 0, x and y.
        let cp1x = p2.x - scaleFactorA * (p3.x - p1.x);
        let cp1y = p2.y - scaleFactorA * (p3.y - p1.y);
        // Control Point 1, x and y.
        let cp2x = p2.x + scaleFactorB * (p3.x - p1.x);
        let cp2y = p2.y + scaleFactorB * (p3.y - p1.y);

        return {
            controlPoint1: new ChartPixel(cp1x, cp1y),
            controlPoint2: new ChartPixel(cp2x, cp2y)
        };
    }

    public static renderGridOutline(canvas: CanvasRenderingContext2D,
                                    inner: ChartClipArea,
                                    color: string       = "rgba(102,102,102,1)",
                                    showLeft: boolean   = true,
                                    showBottom: boolean = true,
                                    showRight: boolean  = true)
    {
        canvas.strokeStyle = color;
        canvas.lineWidth   = 1;
        let halfLineWidth  = canvas.lineWidth / 2;

        canvas.beginPath();
        canvas.moveTo(inner.left, inner.top);

        if (showLeft)
        {
            canvas.lineTo(inner.left, inner.bottom - 1 + halfLineWidth);
        }
        else
        {
            canvas.moveTo(inner.left, inner.bottom - 1 + halfLineWidth);
        }

        if (showBottom)
        {
            canvas.lineTo(inner.right - 1 + halfLineWidth, inner.bottom - 1 + halfLineWidth);
        }
        else
        {
            canvas.moveTo(inner.right - 1 + halfLineWidth, inner.bottom - 1 + halfLineWidth);
        }

        if (showRight)
        {
            canvas.lineTo(inner.right - 1 + halfLineWidth, inner.top);
        }
        else
        {
            canvas.moveTo(inner.right - 1 + halfLineWidth, inner.top);
        }

        canvas.stroke();
    }

    public static renderGrid(canvas: CanvasRenderingContext2D,
                             transformer: ChartValueConverter<any>,
                             left: number,
                             right: number,
                             top: number,
                             bottom: number,
                             xTickValues: number[],
                             yTickValues: number[],
                             color: string     = "rgba(102,102,102,0.333)",
                             lineWidth: number = 0.333)
    {
        canvas.save();

        canvas.lineWidth   = lineWidth;
        canvas.strokeStyle = color;

        // Render the horizontal grid lines.
        for (let yValue of yTickValues || [])
        {
            canvas.beginPath();
            let y = transformer.fromValueToYCoordinate(yValue);
            canvas.moveTo(left, y);
            canvas.lineTo(right, y);
            canvas.stroke();
        }

        // Render the vertical grid lines.
        for (let xValue of xTickValues || [])
        {
            canvas.beginPath();
            let x = transformer.fromMillisecondToXCoordinate(xValue);
            canvas.moveTo(x, top);
            canvas.lineTo(x, bottom);
            canvas.stroke();
        }

        canvas.restore();
    }

    /**
     * returns ChartBox that defines where tooltip was drawn
     *
     * @param helper
     * @param font
     * @param label
     * @param text
     * @param x
     * @param y
     * @param maxWidth
     * @param borderRadius
     * @param tooltipPadding
     * @param titleBottomPadding
     * @param shadowBorderWidth
     * @param shadowBorderColor
     */
    public static renderTooltip(helper: ChartHelpers,
                                font: ChartFont,
                                label: string,
                                text: string,
                                x: number,
                                y: number,
                                maxWidth: number,
                                borderRadius: number       = 5,
                                tooltipPadding: number     = 10,
                                titleBottomPadding: number = 3,
                                shadowBorderWidth: number  = 0,
                                shadowBorderColor: string  = "black"): ChartBox
    {
        let canvas = helper.canvas;

        label = label.replace(/\s+/g, " ")
                     .split(" ")
                     .filter((word) => !!word)
                     .join(" ");

        let labelFont  = font.clone();
        labelFont.size = font.size * 1.1;

        let labelLines = helper.mapText(label, maxWidth, labelFont).lines;
        let labelBox   = helper.placeTextInBox(labelFont, TextPlacement.Left, TextOrientation.Horizontal,
                                               labelLines.join("\n"), 0, 0, BoxAnchor.Left, 0);

        let textLines = helper.mapText(text, maxWidth, font).lines;
        let textBox   = helper.placeTextInBox(font, TextPlacement.Left, TextOrientation.Horizontal,
                                              textLines.join("\n"), 0, 0, BoxAnchor.Left, 0);

        let height = labelBox.height + (text ? textBox.height : 0) + 2 * tooltipPadding;
        let width  = Math.max(labelBox.width, textBox.width || 0) + 2 * tooltipPadding;

        if (shadowBorderWidth > 0)
        {
            canvas.save();
            canvas.strokeStyle  = shadowBorderColor;
            canvas.lineWidth    = shadowBorderWidth;
            let halfBorderWidth = shadowBorderWidth / 2;
            helper.roundedRect(x - halfBorderWidth, y - halfBorderWidth,
                               width + shadowBorderWidth, height + shadowBorderWidth,
                               borderRadius + halfBorderWidth);
            canvas.stroke();
            canvas.restore();
        }

        helper.roundedRect(x, y, width, height, borderRadius);
        canvas.globalAlpha = 0.75;
        canvas.fillStyle   = "black";
        canvas.fill();
        canvas.globalAlpha = 1;

        let lineHeight = helper.measureLineHeight(font);

        let lineX = x + tooltipPadding;
        let lineY = y + tooltipPadding + lineHeight / 2;

        for (let labelLine of labelLines)
        {
            helper.drawTextInBox(labelFont, TextPlacement.Left, TextOrientation.Horizontal,
                                 labelLine, "yellow",
                                 lineX, lineY, BoxAnchor.Left, 0);
            lineY += lineHeight;
        }

        lineY += titleBottomPadding;

        for (let textLine of textLines)
        {
            helper.drawTextInBox(font, TextPlacement.Left, TextOrientation.Horizontal,
                                 textLine, "white",
                                 lineX, lineY, BoxAnchor.Left, 0);
            lineY += lineHeight;
        }

        return new ChartBox(x, y, width, height);
    }
}
