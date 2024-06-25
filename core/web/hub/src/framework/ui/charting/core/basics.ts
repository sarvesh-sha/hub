import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ChartPointRange, ChartValueTransform, ChartValueTransformer} from "framework/ui/charting/core/data-sources";

export enum ChartPointStyle
{
    circle,
    triangle,
    rect,
    rectRounded,
    rectRot,
    cross,
    crossRot,
    star,
    line,
    dash,
    pinCheck,
    pinExclamation,
    target
}

export enum ChartLineType
{
    Line      = <any>"Line",
    StepLeft  = <any>"StepLeft",
    StepRight = <any>"StepRight",
    Smooth    = <any>"Smooth"
}

//--//

export class ChartPixel
{
    constructor(public readonly x: number,
                public readonly y: number)
    {
    }

    toString(): string
    {
        return `[${this.x}x${this.y}]`;
    }
}

export class ChartValue<T>
{
    constructor(public category: string,
                public value: T)
    {
    }
}

export class ChartRange<T>
{
    constructor(public x1: T,
                public x2: T)
    {
    }
}

export class ChartValueRange
{
    get diff(): number
    {
        if (this.min == undefined || this.max == undefined) return NaN;
        return this.max - this.min;
    }

    constructor(public min?: number,
                public max?: number)
    {}

    expandForValue(value: number)
    {
        if (!isNaN(value ?? undefined))
        {
            if (this.min === undefined || this.min > value) this.min = value;
            if (this.max === undefined || this.max < value) this.max = value;
        }
    }

    expandToContain(range: ChartValueRange)
    {
        if (range)
        {
            this.expandForValue(range.min);
            this.expandForValue(range.max);
        }
    }

    forceMin(value: number)
    {
        if (!isNaN(value ?? undefined))
        {
            this.min = value;
            if (this.max != undefined && this.min > this.max) this.max = this.min;
        }
    }

    forceMax(value: number)
    {
        if (!isNaN(value ?? undefined))
        {
            this.max = value;
            if (this.min != undefined && this.max < this.min) this.min = this.max;
        }
    }

    forceToRange(range: ChartValueRange)
    {
        this.forceMin(range.min);
        this.forceMax(range.max);
    }

    isInRange(value: number): boolean
    {
        return value >= this.min && value <= this.max;
    }

    isSame(other: ChartValueRange): boolean
    {
        if (!other) return false;
        return this.min === other.min && this.max === other.max;
    }

    clone(): ChartValueRange
    {
        return new ChartValueRange(this.min, this.max);
    }
}

//--//

/**
 *
 * @param decimal: 0 <= decimal <= 1; to be mapped to a number between 0 and 1 based on power
 * @param power: positive non-zero number
 *               values under 1 resemble cubic curve; approaches y = 0.5 as power -> 0
 *               power = 1 produces linear 1:1 mapping; approaches step fxn as power -> infinity
 */
export function smoothDecimal(decimal: number,
                              power: number): number
{
    return Math.pow(decimal, power) /
           (Math.pow(decimal, power) + Math.pow(1 - decimal, power));
}

//--//

export enum BoxAnchor
{
    TopLeft,
    Top,
    TopRight,
    Left,
    Center,
    Right,
    BottomLeft,
    Bottom,
    BottomRight,
}

export class ChartBox extends ChartPixel
{
    constructor(x: number,
                y: number,
                public readonly width: number,
                public readonly height: number)
    {
        super(x, y);
    }

    get top(): number
    {
        return this.y;
    }

    get bottom(): number
    {
        return this.y + this.height;
    }

    get left(): number
    {
        return this.x;
    }

    get right(): number
    {
        return this.x + this.width;
    }

    hitCheck(x: number,
             y: number): boolean
    {
        return this.hitCheckHorizontal(x) && this.hitCheckVertical(y);
    }

    hitCheckHorizontal(x: number): boolean
    {
        x -= this.x;

        return x >= 0 && x < this.width;
    }

    hitCheckHorizontalBefore(x: number): boolean
    {
        return x < this.x;
    }

    hitCheckHorizontalAfter(x: number): boolean
    {
        return x > this.x + this.width;
    }

    hitCheckVertical(y: number): boolean
    {
        y -= this.y;

        return y >= 0 && y < this.height;
    }

    isFullyVisible(target: ChartBox): boolean
    {
        let clipRight    = this.x + this.width;
        let clipBottom   = this.y + this.height;
        let targetRight  = target.x + target.width;
        let targetBottom = target.y + target.height;

        return this.x <= target.x && this.y <= target.y && targetRight <= clipRight && targetBottom <= clipBottom;
    }

    toString(): string
    {
        return `[${this.x}x${this.y} ${this.width}x${this.height}]`;
    }
}

export class ChartClipArea extends ChartBox
{
    static readonly EmptyPlaceholder = new ChartClipArea(-1, -1, 1, 1);

    applyClipping(canvas: CanvasRenderingContext2D,
                  callback: () => void)
    {
        canvas.save();
        canvas.beginPath();
        canvas.rect(this.x, this.y, this.width, this.height);
        canvas.clip();

        callback();

        canvas.restore();
    }
}

//--//

export enum VerticalChartMarkerPlacement
{
    TOP, CENTER, BOTTOM, FLOAT_UP, FLOAT_DOWN
}

export enum HorizontalChartMarkerPlacement
{
    LEFT, CENTER, RIGHT, FLOAT_LEFT, FLOAT_RIGHT
}

export abstract class ChartMarker<T>
{
    private static readonly edgeBuffer: number = 2;

    xPlacement: HorizontalChartMarkerPlacement = HorizontalChartMarkerPlacement.FLOAT_LEFT;
    yPlacement: VerticalChartMarkerPlacement   = VerticalChartMarkerPlacement.FLOAT_DOWN;

    protected constructor(public source: T)
    {
    }

    abstract get label(): string;

    abstract get tooltip(): string;

    abstract get shape(): ChartPointStyle;

    abstract get color(): string;

    abstract get size(): number;

    abstract get strokeWidthMultiplier(): number;

    abstract get x(): number;

    abstract get y(): number;

    abstract get navigable(): boolean;

    abstract navigate(): void;

    //--//

    toPixel(transform: ChartValueTransformer<ChartPointRange<any>>): ChartPixel
    {
        let x = this.calculateMarkerX(transform);
        let y = this.calculateMarkerY(transform);

        return new ChartPixel(x, y);
    }

    render(helper: ChartHelpers,
           transform: ChartValueTransform,
           withDash: boolean = false,
           overrideColor?: string)
    {
        let canvas = helper.canvas;
        let pixel  = this.toPixel(transform);
        let radius = this.size / 2;
        let color  = overrideColor || this.color;

        if (pixel.x >= (transform.clip.x - radius) && pixel.x <= (transform.clip.right + radius))
        {
            // Draw the time-line line if needed
            if (withDash)
            {
                let dash = [
                    2,
                    3
                ];

                canvas.save();
                canvas.strokeStyle = color;
                canvas.lineWidth   = 1.5;
                canvas.setLineDash(dash);
                canvas.beginPath();
                canvas.moveTo(pixel.x, transform.clip.y);
                canvas.lineTo(pixel.x, transform.clip.bottom);
                canvas.stroke();
                canvas.restore();
            }

            // Draw the point marker
            helper.drawPoint(this.shape, color, radius, pixel.x, pixel.y, this.strokeWidthMultiplier);
        }
    }

    private calculateMarkerX(transform: ChartValueTransformer<ChartPointRange<any>>): number
    {
        let x = this.x;

        switch (this.xPlacement)
        {
            case HorizontalChartMarkerPlacement.FLOAT_LEFT:
                if (x) return transform.fromMillisecondToXCoordinate(x);
            // fallthrough
            case HorizontalChartMarkerPlacement.LEFT:
                return transform.clip.left + (this.size / 2) + ChartMarker.edgeBuffer;

            case HorizontalChartMarkerPlacement.CENTER:
                return (transform.clip.left + transform.clip.width) / 2;

            case HorizontalChartMarkerPlacement.FLOAT_RIGHT:
                if (x) return transform.fromMillisecondToXCoordinate(x);
            // fallthrough
            case HorizontalChartMarkerPlacement.RIGHT:
                return transform.clip.right - ((this.size / 2) + ChartMarker.edgeBuffer);

            default:
                return transform.clip.right - ((this.size / 2) + ChartMarker.edgeBuffer);
        }
    }

    private calculateMarkerY(transform: ChartValueTransformer<ChartPointRange<any>>): number
    {
        let y = this.y ?? undefined;

        switch (this.yPlacement)
        {
            case VerticalChartMarkerPlacement.FLOAT_UP:
                if (!isNaN(y)) return transform.fromValueToYCoordinate(y);
            // fallthrough
            case VerticalChartMarkerPlacement.TOP:
                return transform.clip.top + (this.size / 2) + ChartMarker.edgeBuffer;

            case VerticalChartMarkerPlacement.CENTER:
                return (transform.clip.top + transform.clip.height) / 2;

            case VerticalChartMarkerPlacement.FLOAT_DOWN:
                if (!isNaN(y)) return transform.fromValueToYCoordinate(y);
            // fallthrough
            case VerticalChartMarkerPlacement.BOTTOM:
                return transform.clip.bottom - ((this.size / 2) + ChartMarker.edgeBuffer);

            default:
                return transform.clip.bottom - ((this.size / 2) + ChartMarker.edgeBuffer);
        }
    }
}

//--//

export class Transform
{
    constructor(public a: number  = 1,
                public b: number  = 0,
                public c: number  = 0,
                public d: number  = 1,
                public tx: number = 0,
                public ty: number = 0)
    {}

    // Manually sets the transformation matrix state
    set(a: number,
        b: number,
        c: number,
        d: number,
        tx: number,
        ty: number): Transform
    {
        this.a  = a;
        this.b  = b;
        this.c  = c;
        this.d  = d;
        this.tx = tx;
        this.ty = ty;

        return this;
    }

    // Resets the transform to the default state (identity matrix)
    reset(): Transform
    {
        this.a  = 1;
        this.b  = 0;
        this.c  = 0;
        this.d  = 1;
        this.tx = 0;
        this.ty = 0;

        return this;
    }

    // Creates a copy of the current transform
    clone(): Transform
    {
        return new Transform(this.a, this.b, this.c, this.d, this.tx, this.ty);
    }

    // Gets a string representation of the transformation matrix
    toString(): string
    {
        return `[ ${this.a}\t${this.c}\t${this.tx}\n  ${this.b}\t${this.d}\t${this.ty}\n  0\t0\t1 ]`;
    }

    // Applies a translation to the transform
    translate(delta: ChartPixel): Transform
    {
        this.tx += delta.x * this.a + delta.y * this.c;
        this.ty += delta.x * this.b + delta.y * this.d;

        return this;
    };

    // Applies a scale to the transform
    scale(factor: ChartPixel,
          center?: ChartPixel): Transform
    {
        if (center) this.translate(center);

        this.a *= factor.x;
        this.b *= factor.x;
        this.c *= factor.y;
        this.d *= factor.y;

        if (center) this.translate(new ChartPixel(-center.x, -center.y));

        return this;
    };

    // Applies a rotation to the transform
    rotate(angle: number,
           center?: ChartPixel): Transform
    {
        let x = center ? center.x : 0;
        let y = center ? center.y : 0;

        let cos = Math.cos(angle);
        let sin = Math.sin(angle);
        let a   = this.a;
        let b   = this.b;
        let c   = this.c;
        let d   = this.d;
        let tx  = x - x * cos + y * sin;
        let ty  = y - x * sin - y * cos;

        this.a = cos * a + sin * c;
        this.b = cos * b + sin * d;
        this.c = -sin * a + cos * c;
        this.d = -sin * b + cos * d;
        this.tx += tx * a + ty * c;
        this.ty += tx * b + ty * d;

        return this;
    };

    // Appends the given transform to the current transform
    append(transform: Transform): Transform
    {
        let a1  = this.a;
        let b1  = this.b;
        let c1  = this.c;
        let d1  = this.d;
        let a2  = transform.a;
        let b2  = transform.b;
        let c2  = transform.c;
        let d2  = transform.d;
        let tx2 = transform.tx;
        let ty2 = transform.ty;

        this.a = a2 * a1 + c2 * c1;
        this.b = b2 * a1 + d2 * c1;
        this.c = a2 * b1 + c2 * d1;
        this.d = b2 * b1 + d2 * d1;
        this.tx += tx2 * a1 + ty2 * c1;
        this.ty += tx2 * b1 + ty2 * d1;

        return this;
    }

    // Prepends the given transform to the current transform
    prepend(transform: Transform): Transform
    {
        let a1  = this.a;
        let b1  = this.b;
        let c1  = this.c;
        let d1  = this.d;
        let tx1 = this.tx;
        let ty1 = this.ty;
        let a2  = transform.a;
        let b2  = transform.b;
        let c2  = transform.c;
        let d2  = transform.d;
        let tx2 = transform.tx;
        let ty2 = transform.ty;

        this.a  = a2 * a1 + b2 * b1;
        this.b  = a2 * c1 + b2 * d1;
        this.c  = c2 * a1 + d2 * b1;
        this.d  = c2 * c1 + d2 * d1;
        this.tx = a2 * tx1 + b2 * ty1 + tx2;
        this.ty = c2 * tx1 + d2 * ty1 + ty2;

        return this;
    }

    // Inverts this transform's underlying matrix
    invert(): Transform
    {
        let a           = this.a;
        let b           = this.b;
        let c           = this.c;
        let d           = this.d;
        let tx          = this.tx;
        let ty          = this.ty;
        let determinant = a * d - b * c;

        if (determinant && !isNaN(determinant) && isFinite(tx) && isFinite(ty))
        {
            this.a  = d / determinant;
            this.b  = -b / determinant;
            this.c  = -c / determinant;
            this.d  = a / determinant;
            this.tx = (c * ty - d * tx) / determinant;
            this.ty = (b * tx - a * ty) / determinant;

            return this;
        }

        return null;
    }

    // Applies the transform to a ChartPixel
    apply(point: ChartPixel): ChartPixel
    {
        return this.applyXY(point.x, point.y);
    }

    // Applies the transform to an arbitrary x/y pair
    applyXY(x: number,
            y: number): ChartPixel
    {
        return new ChartPixel(x * this.a + y * this.c + this.tx, x * this.b + y * this.d + this.ty);
    }
}
