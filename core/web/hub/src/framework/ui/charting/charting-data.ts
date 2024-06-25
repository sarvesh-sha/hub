import {ScaleContinuousNumeric, scaleLinear, scaleLog, scalePow} from "d3-scale";
import {IDrawableAsLine, IDrawableAsPoints} from "framework/ui/charting/charting-drawing";
import {IntermediatePoint, IProjectable, Point} from "framework/ui/charting/charting-framework";
import {AxisAlignedBoundingBox, Vector2} from "framework/ui/charting/charting-math";
import {numericSortBy, SortDirection} from "framework/utils/comparators";
import {SimplePubSub} from "framework/utils/pubsub";

export enum ContinuousNumericScaleType
{
    LINEAR = 0, POWER = 1, LOG = 2
}

export namespace ContinuousNumericScaleType
{
    export function instantiate(type: ContinuousNumericScaleType): ScaleContinuousNumeric<number, number>
    {
        switch (type)
        {
            case ContinuousNumericScaleType.LOG:
                return scaleLog();
            case ContinuousNumericScaleType.POWER:
                return scalePow();
            default:
                return scaleLinear();
        }
    }
}

export class PointSet extends SimplePubSub implements IDrawableAsLine,
                                                      IDrawableAsPoints,
                                                      IProjectable
{
    protected dataBox: AxisAlignedBoundingBox;
    protected buffer: IntermediatePoint[] = [];
    protected needsRebuffer               = false;

    constructor(protected points: Point[] = [])
    {
        super();

        // Set point to trigger state updates
        this.setPoints(points);
    }

    getPoints(): Point[]
    {
        return this.points;
    }

    getBounds(): AxisAlignedBoundingBox
    {
        return this.dataBox.clone();
    }

    setPoints(points: Point[]): void
    {
        // Set new points
        this.points = points;

        // Recalculate dataBox
        this.dataBox = AxisAlignedBoundingBox.forPoints(this.points);

        // Notify that data changed
        this.notifyChange();
    }

    appendPoint(point: Point): void
    {
        // Add the point
        this.points.push(point);

        // Update the AABB to include the point
        this.dataBox.updateToFit(point);

        // Notify that data changed
        this.notifyChange();
    }

    prependPoint(point: Point): void
    {
        // Add the point
        this.points.unshift(point);

        // Update the AABB to include the point
        this.dataBox.updateToFit(point);

        // Notify that data changed
        this.notifyChange();
    }

    drawableData(): IntermediatePoint[]
    {
        // Rebuffer if needed
        if (this.needsRebuffer)
        {
            this.rebuffer();
        }

        // Return the intermediary points
        return this.buffer;
    }

    project(vec: Vector2): Vector2
    {
        return vec.clone();
    }

    unproject(vec: Vector2): Vector2
    {
        return vec.clone();
    }

    protected rebuffer(): void
    {
        // Recalculate point buffer by copying values
        this.buffer = this.points.map((p) => new IntermediatePoint(p.x, p.y, p));

        // Clear needsBuffer flag
        this.needsRebuffer = false;
    }

    protected notifyChange(): void
    {
        // Flag as needing rebuffer
        this.needsRebuffer = true;

        // Notify listeners of change event
        this.executeCallbacks("change");
    }
}

export class SortablePointSet extends PointSet
{
    constructor(protected points: Point[] = [])
    {
        super(points);
    }

    sort(property: string, direction: SortDirection): void
    {
        this.points.sort(numericSortBy(property, direction));

        this.notifyChange();
    }
}

export class ScaledPointSet extends SortablePointSet
{
    private m_viewBox: AxisAlignedBoundingBox;
    private m_outputBox: AxisAlignedBoundingBox;

    private m_scaleX: ScaleContinuousNumeric<number, number>;
    private m_scaleY: ScaleContinuousNumeric<number, number>;
    private m_scaleTypeX: ContinuousNumericScaleType = ContinuousNumericScaleType.LINEAR;
    private m_scaleTypeY: ContinuousNumericScaleType = ContinuousNumericScaleType.LINEAR;

    constructor(public points: Point[] = [])
    {
        super(points);

        // Initially set view and window to directly match data
        this.m_viewBox   = this.dataBox.clone();
        this.m_outputBox = this.dataBox.clone();

        // Update the scales
        this.updateScales();
    }

    // X scale type getter/setters
    get scaleTypeX(): ContinuousNumericScaleType
    {
        return this.m_scaleTypeX;
    }

    set scaleTypeX(value: ContinuousNumericScaleType)
    {
        this.m_scaleTypeX = value;
        this.updateScales();
    }

    // Y scale type getter/setters
    get scaleTypeY(): ContinuousNumericScaleType
    {
        return this.m_scaleTypeY;
    }

    set scaleTypeY(value: ContinuousNumericScaleType)
    {
        this.m_scaleTypeY = value;
        this.updateScales();
    }

    // View and output box getters
    get viewBox(): AxisAlignedBoundingBox
    {
        return this.m_viewBox.clone();
    }

    get outputBox(): AxisAlignedBoundingBox
    {
        return this.m_outputBox.clone();
    }

    // Methods to set view and output boxes
    setView(box: AxisAlignedBoundingBox): void
    {
        this.m_viewBox = box.clone();
        this.updateScales();
    }

    setOutput(box: AxisAlignedBoundingBox): void
    {
        this.m_outputBox = box.clone();
        this.updateScales();
    }

    project(vec: Vector2): Vector2
    {
        return new Vector2(this.m_scaleX(vec.x), this.m_scaleY(vec.y));
    }

    unproject(vec: Vector2): Vector2
    {
        return new Vector2(this.m_scaleX.invert(vec.x), this.m_scaleY.invert(vec.y));
    }

    protected rebuffer(): void
    {
        // Recalculate point buffer by scaling values
        this.buffer = this.points.map((p) => new IntermediatePoint(this.m_scaleX(p.x), this.m_scaleY(p.y), p));

        // Clear needsBuffer flag
        this.needsRebuffer = false;
    }

    private updateScales(): void
    {
        this.m_scaleX = ContinuousNumericScaleType.instantiate(this.m_scaleTypeX);
        this.m_scaleY = ContinuousNumericScaleType.instantiate(this.m_scaleTypeY);

        // Set up x scale
        let domainX = [
            this.m_viewBox.x,
            this.m_viewBox.x + this.m_viewBox.width
        ];
        let rangeX  = [
            this.m_outputBox.x,
            this.m_outputBox.x + this.m_outputBox.width
        ];

        this.m_scaleX.domain(domainX);
        this.m_scaleX.range(rangeX);

        // Set up y scale
        let domainY = [
            this.m_viewBox.y,
            this.m_viewBox.y + this.m_viewBox.height
        ];
        let rangeY  = [
            this.m_outputBox.y,
            this.m_outputBox.y + this.m_outputBox.height
        ];

        this.m_scaleY.domain(domainY);
        this.m_scaleY.range(rangeY);

        // Notify that data changed
        this.notifyChange();
    }
}
