import {IntermediatePoint, IProjectable} from "framework/ui/charting/charting-framework";
import {Transform, Vector2} from "framework/ui/charting/charting-math";
import {ChartPixel} from "framework/ui/charting/core/basics";
import {SimplePubSub} from "framework/utils/pubsub";

interface IDrawable
{
    drawableData: () => IntermediatePoint[];
}

abstract class BufferedDrawable implements IProjectable
{
    protected buffer: IntermediatePoint[] = [];
    protected transform: Transform        = new Transform();

    protected constructor(public source: IDrawable)
    {

    }

    abstract draw(ctx: CanvasRenderingContext2D): void

    public get dataTransform(): Transform
    {
        return this.transform;
    }

    rebuffer(): void
    {
        if (this.transform.isIdentity())
        {
            this.buffer = this.source.drawableData()
                              .map((point) => this.mapCopy(point));
        }
        else
        {
            this.buffer = this.source.drawableData()
                              .map((point) => this.mapTransform(point));
        }
    }

    project(vec: Vector2): Vector2
    {
        return this.transform.apply(vec);
    }

    unproject(vec: Vector2): Vector2
    {
        return this.transform.clone()
                   .invert()
                   .apply(vec);
    }

    private mapCopy(point: IntermediatePoint): IntermediatePoint
    {
        return point;
    }

    private mapTransform(point: IntermediatePoint): IntermediatePoint
    {
        let transformed = this.transform.apply(point);
        return new IntermediatePoint(transformed.x, transformed.y, point);
    }
}

export interface ILineStyle
{
    color: string,
    width: number
}

export interface IDrawableAsLine extends IDrawable {}

export class LineStyle implements ILineStyle
{
    constructor(public color: string = "black",
                public width: number = 1)
    {

    }
}

export class Line extends BufferedDrawable
{
    private m_path: Path2D = new Path2D();

    constructor(public source: IDrawableAsLine,
                public style: ILineStyle = new LineStyle())
    {
        super(source);
    }

    draw(ctx: CanvasRenderingContext2D): void
    {
        // Set line style
        ctx.strokeStyle = this.style.color;
        ctx.lineWidth   = this.style.width;

        // Stroke path to draw
        ctx.stroke(this.m_path);
    }

    rebuffer(): void
    {
        // Call default rebuffer behavior
        super.rebuffer();

        // Update the path using the buffer
        this.preparePath();
    }

    private preparePath(): void
    {
        // Create an empty path
        this.m_path = new Path2D();

        // Do nothing if less that 2 drawable points
        if (this.buffer.length < 2) return;

        // Add path points
        this.m_path.moveTo(this.buffer[0].x, this.buffer[0].y);
        for (let i = 1; i < this.buffer.length; i++) this.m_path.lineTo(this.buffer[i].x, this.buffer[i].y);
    }
}

export class DynamicLine extends Line
{
    protected changed = true;

    constructor(public source: IDrawableAsLine & SimplePubSub,
                public style: ILineStyle = new LineStyle())
    {
        super(source, style);

        // Subscribe to change events for data source and transform
        this.transform.subscribe("change", () => this.changeCallback());
        this.source.subscribe("change", () => this.changeCallback());
    }

    draw(ctx: CanvasRenderingContext2D): void
    {
        // Automatically rebuffer if there was a change
        if (this.changed)
        {
            this.rebuffer();
            this.changed = false;
        }

        // Draw as normal
        super.draw(ctx);
    }

    private changeCallback(): void
    {
        this.changed = true;
    }
}


export interface IStrokeStyle extends ILineStyle {}

export class StrokeStyle extends LineStyle implements IStrokeStyle
{
    constructor(public color: string = "black",
                public width: number = 1)
    {
        super(color, width);
    }
}

export interface IPointStyle
{
    color: string | ((point: ChartPixel) => string),
    size: number | ((point: ChartPixel) => number),
    stroke: IStrokeStyle
}

export interface IDrawableAsPoints extends IDrawable {}

export class PointStyle implements IPointStyle
{
    constructor(public color: string | ((point: ChartPixel) => string) = "black",
                public size: number | ((point: ChartPixel) => number)  = 1,
                public stroke: IStrokeStyle                            = null)
    {

    }
}

export class Points extends BufferedDrawable
{
    private m_path: Path2D = new Path2D();

    constructor(public source: IDrawableAsPoints,
                public style: IPointStyle = new PointStyle())
    {
        super(source);
    }

    draw(ctx: CanvasRenderingContext2D): void
    {
        // Save context state
        ctx.save();

        if (this.m_path)
        {
            this.drawFromBuffer(ctx);
        }
        else
        {
            this.drawImmediate(ctx);
        }

        // Restore context state
        ctx.restore();
    }

    rebuffer(): void
    {
        // Call default rebuffer behavior
        super.rebuffer();

        // Update path if the style is batchable
        if (this.isStyleBatchable())
        {
            this.preparePath();
        }
        else
        {
            this.m_path = null;
        }
    }

    private preparePath(): void
    {
        // Make a new path buffer
        this.m_path = new Path2D();

        // Abort if less that 1 drawable point
        if (this.buffer.length < 1) return;

        // Extract info used to build path
        let constantSize = typeof this.style.size === "number";
        let radiusValue  = constantSize ? <number>this.style.size / 2 : undefined;
        let radiusFn     = constantSize ? undefined : <(point: ChartPixel) => number>this.style.size;
        let radius;

        // Add the first point separately
        radius = constantSize ? radiusValue : radiusFn(this.buffer[0].source);
        this.m_path.arc(this.buffer[0].x, this.buffer[0].y, radius, 0, Math.PI * 2, true);

        // Add all other points
        for (let i = 1; i < this.buffer.length; i++)
        {
            radius = constantSize ? radiusValue : radiusFn(this.buffer[i].source);

            this.m_path.moveTo(this.buffer[i].x + radius, this.buffer[i].y);
            this.m_path.arc(this.buffer[i].x, this.buffer[i].y, radius, 0, Math.PI * 2, true);
        }

        // Finish and close the path
        this.m_path.closePath();
    }

    private drawFromBuffer(ctx: CanvasRenderingContext2D)
    {
        // Check if we need to stroke as well
        let hasStroke = this.hasVisibleStroke();

        // Set fill and stroke styles
        ctx.fillStyle = <string>this.style.color;
        if (hasStroke)
        {
            ctx.strokeStyle = <string>this.style.stroke.color;
            ctx.lineWidth   = <number>this.style.stroke.width;
        }

        // Fill points
        ctx.fill(this.m_path);

        // Stroke points if needed
        if (hasStroke) ctx.stroke(this.m_path);
    }

    private drawImmediate(ctx: CanvasRenderingContext2D)
    {
        // Do not render if less that 1 drawable point
        if (this.buffer.length < 1) return;

        // Extract info used to draw points
        let hasStroke = this.hasVisibleStroke();

        let constantSize = typeof this.style.size === "number";
        let radiusValue  = constantSize ? <number>this.style.size / 2 : undefined;
        let radiusFn     = constantSize ? undefined : <(point: ChartPixel) => number>this.style.size;

        let constantColor = typeof this.style.color === "string";
        let colorValue    = constantColor ? <string>this.style.color : undefined;
        let colorFn       = constantColor ? undefined : <(point: ChartPixel) => string>this.style.color;

        // Set the stroke style if there is a stroke
        if (hasStroke)
        {
            ctx.strokeStyle = this.style.stroke.color;
            ctx.lineWidth   = this.style.stroke.width;
        }

        // Draw all path points
        for (let i = 0; i < this.buffer.length; i++)
        {
            ctx.fillStyle = constantColor ? colorValue : colorFn(this.buffer[i].source);

            ctx.beginPath();
            ctx.arc(this.buffer[i].x, this.buffer[i].y, constantSize ? radiusValue : radiusFn(this.buffer[i].source), 0, Math.PI * 2, true);
            ctx.closePath();

            ctx.fill();
            if (hasStroke) ctx.stroke();
        }
    }

    private hasVisibleStroke(): boolean
    {
        if (this.style.stroke && this.style.stroke.width !== 0) return true;
        return false;
    }

    private isStyleBatchable(): boolean
    {
        if (typeof this.style.color !== "string") return false;
        if (this.hasVisibleStroke())
        {
            if (typeof this.style.stroke.color !== "string") return false;
            if (typeof this.style.stroke.width !== "number") return false;
        }

        return true;
    }
}

export class DynamicPoints extends Points
{
    private m_changed = true;

    constructor(public source: IDrawableAsPoints & SimplePubSub,
                public style: IPointStyle = new PointStyle())
    {
        super(source, style);

        // Subscribe to change events for data source and transform
        this.transform.subscribe("change", () => this.changeCallback());
        this.source.subscribe("change", () => this.changeCallback());
    }

    draw(ctx: CanvasRenderingContext2D): void
    {
        // Automatically rebuffer if there was a change
        if (this.m_changed)
        {
            this.rebuffer();
            this.m_changed = false;
        }

        // Draw as normal
        super.draw(ctx);
    }

    private changeCallback(): void
    {
        this.m_changed = true;
    }
}
