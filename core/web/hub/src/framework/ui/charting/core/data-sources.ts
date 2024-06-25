import {Type} from "@angular/core";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ScaledPointSet} from "framework/ui/charting/charting-data";
import {Point} from "framework/ui/charting/charting-framework";
import {AxisAlignedBoundingBox, Vector2} from "framework/ui/charting/charting-math";
import {ChartClipArea, ChartLineType, ChartMarker, ChartPixel, ChartValueRange} from "framework/ui/charting/core/basics";
import {ChartColorUtilities, ColorGradientContinuous, ColorGradientStop, ColorMapper} from "framework/ui/charting/core/colors";
import {ChartTimeRange, ChartTimeWindow} from "framework/ui/charting/core/time";
import {ComponentContext} from "framework/ui/components";
import {ContextMenuItemComponent} from "framework/ui/context-menu/context-menu.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

import moment from "framework/utils/moment";

export interface ChartPointSubscriber
{
    transitionToActive(): void;

    newSamples(timestamp: moment.Moment): void;

    transitionToInactive(): void;
}

export abstract class ChartPointProvider
{
    constructor(public readonly rangeStart: moment.Moment,
                public readonly rangeEnd: moment.Moment,
                public readonly source: ChartPointSource<any>)
    {
    }

    public abstract startStreamingSamples(maxSamplesPerBatch: number,
                                          subscriber: ChartPointSubscriber): void;

    public abstract stopStreamingSamples(): void;

    public abstract computeMin(rangeStart: moment.Moment,
                               rangeEnd: moment.Moment): Promise<number>;

    public abstract computeMax(rangeStart: moment.Moment,
                               rangeEnd: moment.Moment): Promise<number>;

    public abstract computeSum(rangeStart: moment.Moment,
                               rangeEnd: moment.Moment): Promise<number>;

    public abstract accessContext<T>(type: Type<T>): T;

    public abstract prepareContextMenu(point: ChartPoint<any>,
                                       root: ContextMenuItemComponent): Promise<boolean>;
}

export interface ChartPointEnumeratedValue
{
    name?: string;
    value?: number;
}

export class ChartPointRangeLookupResult
{
    constructor(public range: ChartPointRange<any>,
                public min: number,
                public max: number)
    {}
}

export class ChartPointRange<T>
{
    private static s_uniqueSeq = 0; // A global counter, used to assign a unique id to each range, such that we can use them in a map.

    public readonly id = ChartPointRange.s_uniqueSeq++;

    private m_cursor                   = 0;
    private m_timestamps: Float64Array = new Float64Array(1000);
    private m_values: Float64Array     = new Float64Array(1000);
    private m_flags: Int8Array         = new Int8Array(1000);

    constructor(public readonly source: ChartPointSource<T>)
    {
    }

    get size(): number
    {
        return this.m_cursor;
    }

    get minDate(): number
    {
        return this.m_cursor > 0 ? this.m_timestamps[0] : null;
    }

    get maxDate(): number
    {
        return this.m_cursor > 0 ? this.m_timestamps[this.m_cursor - 1] : null;
    }

    merge(ranges: ChartPointRange<T>[])
    {
        let total = 0;

        for (let range of ranges)
        {
            let rangeSize = range.size;
            if (rangeSize > 0)
            {
                // Insert extra point between ranges.
                if (total > 0)
                {
                    total++;
                }

                total += rangeSize;
            }
        }

        this.m_cursor     = total;
        this.m_timestamps = new Float64Array(total);
        this.m_values     = new Float64Array(total);
        this.m_flags      = new Int8Array(total);

        let offset = 0;

        for (let range of ranges)
        {
            let rangeSize = range.size;
            if (rangeSize > 0)
            {
                let timestamps = range.getTimestamps();
                let values     = range.getNumericValues();
                let flags      = range.getFlags();

                // Insert extra point between ranges.
                if (offset > 0)
                {
                    this.m_values[offset]     = NaN;
                    this.m_flags[offset]      = ChartPointRange.fromType(ChartPointType.NoValue);
                    this.m_timestamps[offset] = Math.max(this.m_timestamps[offset - 1] + 1, timestamps[0] - 1); // Subtract one millisecond
                    offset++;
                }

                this.m_timestamps.set(timestamps, offset);
                this.m_values.set(values, offset);
                this.m_flags.set(flags, offset);
                offset += rangeSize;
            }
        }
    }

    addPoint(timestampInMillisec: number,
             value: T,
             type: ChartPointType)
    {
        this.addPointRaw(timestampInMillisec, this.source.getNumericValue(value), type);
    }

    addPointRaw(timestampInMillisec: number,
                numericValue: number,
                type: ChartPointType)
    {
        let capacity = this.m_timestamps.length;
        let cursor   = this.m_cursor++;
        if (cursor >= capacity)
        {
            if (capacity >= 100000)
            {
                capacity += 100000;
            }
            else
            {
                capacity = 10 * capacity;
            }

            this.m_timestamps = ChartPointRange.grow64(this.m_timestamps, capacity);
            this.m_values     = ChartPointRange.grow64(this.m_values, capacity);
            this.m_flags      = ChartPointRange.grow8(this.m_flags, capacity);
        }

        this.m_timestamps[cursor] = timestampInMillisec;
        this.m_values[cursor]     = numericValue;
        this.m_flags[cursor]      = ChartPointRange.fromType(type);
    }

    toPoint(index: number | null): ChartPoint<T>
    {
        if (index === null) return null;

        return this.source.convertToPoint(this.toTimestamp(index), this.toNumericValue(index), this.toType(index), this.isSelected(index));
    }

    //--//

    static grow64(oldArray: Float64Array,
                  capacity: number): Float64Array
    {
        let newArray = new Float64Array(capacity);
        if (oldArray)
        {
            newArray.set(oldArray);
        }
        return newArray;
    }

    static grow32(oldArray: Float32Array,
                  capacity: number): Float32Array
    {
        let newArray = new Float32Array(capacity);
        if (oldArray)
        {
            newArray.set(oldArray);
        }
        return newArray;
    }

    static grow8(oldArray: Int8Array,
                 capacity: number): Int8Array
    {
        let newArray = new Int8Array(capacity);
        if (oldArray)
        {
            newArray.set(oldArray);
        }
        return newArray;
    }

    //--//

    static shrink64(oldArray: Float64Array,
                    capacity: number): Float64Array
    {
        let newArray = new Float64Array(capacity);
        if (oldArray)
        {
            newArray.set(oldArray.subarray(0, capacity));
        }
        return newArray;
    }

    static shrink32(oldArray: Float32Array,
                    capacity: number): Float32Array
    {
        let newArray = new Float32Array(capacity);
        if (oldArray)
        {
            newArray.set(oldArray.subarray(0, capacity));
        }
        return newArray;
    }

    static shrink8(oldArray: Int8Array,
                   capacity: number): Int8Array
    {
        let newArray = new Int8Array(capacity);
        if (oldArray)
        {
            newArray.set(oldArray.subarray(0, capacity));
        }
        return newArray;
    }

    //--//

    getTimestamps(): Float64Array
    {
        return this.m_timestamps.subarray(0, this.m_cursor);
    }

    toTimestamp(index: number | null): number
    {
        if (index === null) return null;

        return this.m_timestamps[index];
    }

    //--//

    getNumericValues(): Float64Array
    {
        return this.m_values.subarray(0, this.m_cursor);
    }

    toNumericValue(index: number | null): number
    {
        if (index === null) return null;

        return this.m_values[index];
    }

    //--//

    getFlags(): Int8Array
    {
        return this.m_flags.subarray(0, this.m_cursor);
    }

    static asType(rawFlag: number): ChartPointType
    {
        return <ChartPointType>(rawFlag & ChartPointFlag_Mask);
    }

    static fromType(type: ChartPointType): number
    {
        return type;
    }

    toType(index: number | null): ChartPointType
    {
        if (index === null) return null;

        return ChartPointRange.asType(this.m_flags[index]);
    }

    //--//

    static asSelected(rawFlag: number): boolean
    {
        return (rawFlag & ChartPointFlag_Selected) != 0;
    }

    static fromSelected(selected: boolean): number
    {
        return selected ? ChartPointFlag_Selected : 0;
    }

    isSelected(index: number | null): boolean
    {
        if (index === null) return false;

        return ChartPointRange.asSelected(this.m_flags[index]);
    }

    setSelected(index: number | null,
                state: boolean)
    {
        if (index !== null)
        {
            let value = this.m_flags[index];
            if (state)
            {
                value |= ChartPointFlag_Selected;
            }
            else
            {
                value &= ~ChartPointFlag_Selected;
            }
            this.m_flags[index] = value;
        }
    }

    findPointIndex(timestampInMillisec: number,
                   requireExact: boolean = true): number
    {
        let pos = this.findInsertionPoint(timestampInMillisec);
        if (pos >= 0)
        {
            return pos;
        }

        if (!requireExact)
        {
            pos = ~pos - 1; // This is the index that has a smaller timestamp.
            if (pos < this.m_cursor) return pos;
        }

        return null;
    }

    findPoint(timestampInMillisec: number,
              requireExact: boolean = true): ChartPoint<T>
    {
        let pos = this.findPointIndex(timestampInMillisec, requireExact);
        return this.toPoint(pos);
    }

    findInsertionPoint(timestampInMillisec: number): number
    {
        return UtilsService.binarySearchForFloat64Array(this.m_timestamps, this.m_cursor, timestampInMillisec);
    }

    firstPointIndex(): number | null
    {
        for (let i = 0; i < this.m_cursor; i++)
        {
            let val = this.m_values[i];
            if (isFinite(val))
            {
                return i;
            }
        }

        return null;
    }

    lastPointIndex(): number | null
    {
        for (let i = this.m_cursor; i > 0;)
        {
            let val = this.m_values[--i];
            if (isFinite(val))
            {
                return i;
            }
        }

        return null;
    }

    firstPoint(): ChartPoint<T>
    {
        return this.toPoint(this.firstPointIndex());
    }

    lastPoint(): ChartPoint<T>
    {
        return this.toPoint(this.lastPointIndex());
    }

    findInternalRange(minMS: number,
                      maxMS: number,
                      skipNonValid: boolean): ChartPointRangeLookupResult
    {
        let minIdx = this.findInsertionPoint(minMS);
        if (minIdx < 0)
        {
            minIdx = ~minIdx; // The timestamp at this index is definitely larger, by design.

            if (skipNonValid)
            {
                while (minIdx < this.m_cursor && !isFinite(this.m_values[minIdx])) minIdx++;
            }
        }

        let maxIdx = this.findInsertionPoint(maxMS);
        if (maxIdx < 0)
        {
            maxIdx = ~maxIdx; // The timestamp at this index is definitely larger, by design.

            // Move to the previous index, which is going to be smaller.
            maxIdx--;

            if (skipNonValid)
            {
                while (maxIdx >= 0 && !isFinite(this.m_values[maxIdx])) maxIdx--;
            }
        }

        return new ChartPointRangeLookupResult(this, minIdx, maxIdx);
    }

    findBounding(timestampInMillisec: number): ChartPointBound<T>
    {
        let minPos: number;
        let maxPos: number;

        let pos = this.findInsertionPoint(timestampInMillisec);
        if (pos >= 0)
        {
            minPos = pos;
            maxPos = pos;
        }
        else
        {
            maxPos = ~pos;
            minPos = maxPos - 1;
            // The timestamp lies between these two indices.
        }

        // Look for two valid samples.
        while (minPos >= 0 && !isFinite(this.m_values[minPos])) minPos--;
        while (maxPos < this.m_cursor && !isFinite(this.m_values[maxPos])) maxPos++;

        if (minPos < 0 || maxPos >= this.m_cursor) return null; // To the left or the right of the range, no bounds possible.

        return new ChartPointBound<T>(this.toPoint(minPos), this.toPoint(maxPos));
    }

    expandRange(res: ChartValueRange)
    {
        for (let pos = 0; pos < this.m_cursor; pos++)
        {
            let num = this.m_values[pos];
            if (isFinite(num)) res.expandForValue(num);
        }
    }
}

export enum ChartPointType
{
    Missing = 0,
    Value   = 1,
    NoValue = 2,
}

// The mask covers all the possible encodings of ChartPointType
const ChartPointFlag_Mask     = 0x03;
const ChartPointFlag_Selected = 0x04;

//--//

export class ChartPoint<T>
{
    constructor(public readonly owningSource: ChartPointSource<T> = new PlaceHolderSource<T>(null, null),
                public readonly timestampInMillisec: number,
                public value: T,
                public numberValue: number,
                public type: ChartPointType,
                public selected: boolean)
    {
    }

    isSame(other: ChartPoint<T>): boolean
    {
        return this.numberValue == other.numberValue && this.timestampInMillisec == other.timestampInMillisec;
    }

    get isValid(): boolean
    {
        return true;
    }

    getTooltip(): string
    {
        return this.owningSource.getTooltip(this);
    }

    getTooltipText(): string
    {
        return this.owningSource.getTooltipText(this);
    }

    getProcessedTimestamp(): string
    {
        let timestamp = MomentHelper.friendlyFormatVerboseUS(this.xAsMoment);
        if (this.hasDifferentZone())
        {
            let utcOffset = -(this.utcOffset - MomentHelper.utcOffset(undefined, this.timestampInMillisec)) / 60;
            if (utcOffset !== 0) timestamp += ` (${utcOffset > 0 ? "+" : ""}${utcOffset}h)`;
        }

        return timestamp;
    }

    hasDifferentZone(): boolean
    {
        return this.owningSource && !!this.owningSource.zoneName;
    }

    get utcOffset(): number
    {
        return MomentHelper.utcOffset(this.owningSource.zoneName || undefined, this.timestampInMillisec);
    }

    get xAsMoment(): moment.Moment
    {
        return MomentHelper.parse(this.timestampInMillisec, this.owningSource?.zoneName);
    }

    get displayValue(): string
    {
        return this.owningSource.getDisplayValue(this.value);
    }

    get colorMapping(): string
    {
        return this.owningSource.getColorMapping(this);
    }

    //--//

    toPixel(transform: ChartValueConverter<ChartPointRange<any>>): ChartPixelWithContext<T>
    {
        let xCoord = transform.fromMillisecondToXCoordinate(this.timestampInMillisec);
        let yCoord = transform.fromValueToYCoordinate(this.numberValue);

        return new ChartPixelWithContext(xCoord, yCoord, this.type, this);
    }

    //--//

    static indexOf(array: ChartPoint<any>[],
                   point: ChartPoint<any>): number
    {
        return (array && point) ? array.findIndex((candidate) => candidate.isSame(point)) : -1;
    }

    static pushIfMissing(array: ChartPoint<any>[],
                         point: ChartPoint<any>)
    {
        if (array && point && ChartPoint.indexOf(array, point) < 0) array.push(point);
    }
}

export class ChartPointWithTransform<T>
{
    constructor(public readonly point: ChartPoint<T>,
                public readonly transform: ChartValueConverter<any>)
    {
    }

    toPixel(): ChartPixelWithContext<T>
    {
        return this.point.toPixel(this.transform);
    }
}

export class ChartPointBound<T>
{
    constructor(public readonly left: ChartPoint<T>,
                public readonly right: ChartPoint<T>)
    {
    }

    interpolate(timestamp: moment.Moment)
    {
        let y1 = this.left?.numberValue;
        let y2 = this.right?.numberValue;

        if (!this.left) return y2;
        if (!this.right) return y1;

        if (this.left.timestampInMillisec === this.right.timestampInMillisec) return y1;

        let x1 = this.left.timestampInMillisec;
        let x2 = this.right.timestampInMillisec;
        let x  = timestamp.valueOf();

        return ((y2 - y1) / (x2 - x1)) * (x - x1) + y1;
    }
}

//--//

export abstract class ChartValueConverter<I>
{
    private m_cache: Lookup<any> = {};

    abstract getYZeroOffset(): number;

    fromTimestampToXCoordinate(x: moment.Moment): number
    {
        return this.fromMillisecondToXCoordinate(x.valueOf());
    }

    abstract fromMillisecondToXCoordinate(x: number): number;

    abstract fromValueToYCoordinate(y: any): number;

    flushCache()
    {
        this.m_cache = {};
    }

    computeIfMissing<T>(range: I,
                        prefix: string,
                        generator: (range: I) => T): T
    {
        let key = this.generateKey(range, prefix);

        let value = this.m_cache[key];
        if (!value)
        {
            value = generator(range);

            this.m_cache[key] = value;
        }

        return <T>value;
    }

    protected abstract generateKey(range: I,
                                   prefix: string): string;
}

export abstract class ChartValueTransformer<I> extends ChartValueConverter<I>
{
    constructor(public domain: ChartTimeWindow,
                public clip: ChartClipArea)
    {
        super();
    }

    abstract fromXCoordinateToMillisecond(x: number): number;

    abstract fromYCoordinateToValue(y: number): number;
}

export class ChartPointToPixel
{
    public static generateMapping(domain: ChartTimeWindow,
                                  clip: ChartClipArea,
                                  timeOffset: number,
                                  valueOffset: number)
    {
        let xRangeInMillisec = domain.timeRangeInMilliseconds;
        let yRange           = domain.valueRange;

        let xRatio = xRangeInMillisec != 0 ? clip.width / xRangeInMillisec : 1E20; // Large ratio => small deviation from center value
        let yRatio = yRange != 0 ? clip.height / yRange : 1E20; // Large ratio => small deviation from center value

        let xOffset = clip.x - (timeOffset + domain.startMillisecond) * xRatio;
        let yOffset = clip.y + clip.height + (valueOffset + domain.minValue) * yRatio;

        let mapping = new ChartPointToPixel(xOffset, xRatio, yOffset, -yRatio);

        let yZeroOffset = mapping.applyY(0);
        if (yZeroOffset < clip.y)
        {
            yZeroOffset = clip.y;
        }
        else if (yZeroOffset > clip.y + clip.height)
        {
            yZeroOffset = clip.y + clip.height;
        }
        mapping.m_yZeroOffset = yZeroOffset;

        return mapping;
    }

    private m_yZeroOffset: number;
    get yZeroOffset(): number
    {
        return this.m_yZeroOffset;
    }

    constructor(public readonly xOffset: number,
                public readonly xRatio: number,
                public readonly yOffset: number,
                public readonly yRatio: number)
    {}

    public applyX(timestamp: number): number
    {
        return this.xOffset + timestamp * this.xRatio;
    }

    public applyY(value: number): number
    {
        return this.yOffset + value * this.yRatio;
    }

    public applyReverseX(x: number): number
    {
        return (x - this.xOffset) / this.xRatio;
    }

    public applyReverseY(y: number): number
    {
        return (y - this.yOffset) / this.yRatio;
    }
}

export class ChartPointsRenderView
{
    timestamps: Float64Array;
    values: Float64Array;
    flags: Int8Array;

    constructor(public readonly transferFunction: ChartPointToPixel)
    {
    }

    get length(): number
    {
        return this.timestamps.length;
    }

    fromIndexToXCoordinate(index: number): number
    {
        return this.transferFunction.applyX(this.timestamps[index]);
    }

    fromIndexToYCoordinate(index: number): number
    {
        return this.transferFunction.applyY(this.values[index]);
    }

    fromXCoordinateToTimestamp(x: number): number
    {
        return this.transferFunction.applyReverseX(x);
    }

    fromYCoordinateToValue(y: number): number
    {
        return this.transferFunction.applyReverseY(y);
    }
}

export class ChartValueTransform extends ChartValueTransformer<ChartPointRange<any>>
{
    private m_zeroTransferFunction: ChartPointToPixel;

    constructor(domain: ChartTimeWindow,
                clip: ChartClipArea)
    {
        super(domain, clip);

        this.m_zeroTransferFunction = ChartPointToPixel.generateMapping(domain, clip, 0, 0);
    }

    getYZeroOffset(): number
    {
        return this.m_zeroTransferFunction.yZeroOffset;
    }

    fromMillisecondToXCoordinate(msec: number): number
    {
        return this.m_zeroTransferFunction.applyX(msec);
    }

    fromValueToYCoordinate(y: number): number
    {
        return this.m_zeroTransferFunction.applyY(y);
    }

    fromXCoordinateToMillisecond(x: number): number
    {
        return this.m_zeroTransferFunction.applyReverseX(x);
    }

    fromYCoordinateToValue(y: number): number
    {
        return this.m_zeroTransferFunction.applyReverseY(y);
    }

    fromPixelToSample(dataSource: ChartPointSource<any>,
                      x: number,
                      y: number): ChartPoint<any>
    {
        let timestamp = this.fromXCoordinateToMillisecond(x);
        let value     = this.fromYCoordinateToValue(y);

        if (timestamp < this.domain.startMillisecond || timestamp > this.domain.endMillisecond) return null;
        if (value < this.domain.minValue || value > this.domain.maxValue) return null;

        return new ChartPoint<any>(dataSource, timestamp, value, value, ChartPointType.Value, false);
    }

    protected generateKey(range: ChartPointRange<any>,
                          prefix: string): string
    {
        return `${prefix}/${range.id}`;
    }
}

//--//

export class ChartPixelWithContext<T> extends ChartPixel
{
    constructor(x: number,
                y: number,
                public readonly type: ChartPointType,
                public readonly ctx: ChartPoint<T>)
    {
        super(x, y);
    }

    toString(): string
    {
        return `[${this.x}x${this.y} - ${this.ctx.xAsMoment} - ${this.ctx.displayValue}]`;
    }
}

//--//

type DecimationDisplay = "Minimum" | "Average" | "Maximum";

export abstract class ChartValueFormatter<T>
{
    public abstract getTooltip(point: ChartPoint<T>): string

    public abstract getTooltipText(point: ChartPoint<T>): string
}

export abstract class ChartPointSource<T>
{
    provider: ChartPointProvider;

    label: string;
    color: string;
    enabled = true;
    index: number;

    smoothness           = .3;
    lineWidth            = 3;
    lineType             = ChartLineType.Line;
    fillArea             = true;
    hideDecimation       = true;
    decimationDisplay: DecimationDisplay;
    autoAggregation      = false;
    expectedSamplingRate = 0;

    showMovingAverage: number;
    onlyShowMovingAverage: boolean;

    showExpandedPoints = true;
    showPoints         = true;
    baseRadius         = 2;

    zoomable = false;

    zoneName: string;

    markerLine                  = false;
    markers: ChartMarker<any>[] = [];

    noValueMarker: number;
    ranges: ChartPointRange<T>[]              = [];
    rangeOverride: ChartValueRange            = null;
    private m_mergedRange: ChartPointRange<T> = null;

    m_state = VisualizationDataSourceState.Active;
    get state(): VisualizationDataSourceState
    {
        return this.m_state;
    }

    set state(state: VisualizationDataSourceState)
    {
        if (this.m_state !== VisualizationDataSourceState.Deleted)
        {
            this.m_state = state;
        }
    }

    constructor(public readonly app: ComponentContext,
                private readonly formatter: ChartValueFormatter<T>)
    {
    }

    restore(state: VisualizationDataSourceState): boolean
    {
        if (!isVisible(this.m_state) && isVisible(state))
        {
            this.m_state = state;
            return true;
        }
        return false;
    }

    addRange(): ChartPointRange<T>
    {
        let res = new ChartPointRange<T>(this);

        this.ranges.push(res);
        this.flushMergedRange();

        return res;
    }

    flushMergedRange()
    {
        this.m_mergedRange = null;
    }

    getMergedRange(): ChartPointRange<T>
    {
        if (!this.m_mergedRange)
        {
            let res = new ChartPointRange<T>(this);
            res.merge(this.ranges);
            this.m_mergedRange = res;
        }

        return this.m_mergedRange;
    }

    firstPoint(): ChartPoint<T>
    {
        for (let range of this.ranges)
        {
            let firstPoint = range.firstPoint();
            if (firstPoint && firstPoint.isValid) return firstPoint;
        }

        return null;
    }

    lastPoint(): ChartPoint<T>
    {
        for (let i = this.ranges.length - 1; i >= 0; i--)
        {
            let lastPoint = this.ranges[i].lastPoint();
            if (lastPoint && lastPoint.isValid) return lastPoint;
        }

        return null;
    }

    findPoint(timestamp: moment.Moment | number): ChartPoint<T>
    {
        timestamp = typeof timestamp == "number" ? timestamp : timestamp.valueOf();

        let firstPoint = this.firstPoint();
        if (!firstPoint || firstPoint.timestampInMillisec > timestamp) return null;

        for (let range of this.ranges)
        {
            let lastPoint = range.lastPoint();
            if (lastPoint.timestampInMillisec < timestamp) continue;

            return range.findPoint(timestamp);
        }

        return null;
    }

    findNearestPoint(timestamp: moment.Moment): ChartPoint<T>
    {
        let timestampInMillisec = timestamp.valueOf();
        return this.findRange(timestampInMillisec)
                   ?.findPoint(timestampInMillisec, false);
    }

    findRange(timestamp: number): ChartPointRange<any>
    {
        let lowestDiff: number;
        let closestRange: ChartPointRange<T>;
        for (let range of this.ranges)
        {
            let timestamps = range.getTimestamps();
            let low        = timestamps[0];
            let high       = timestamps[timestamps.length - 1];
            if (low <= timestamp && timestamp <= high) return range;

            let diff = Math.min(Math.abs(timestamp - low), Math.abs(timestamp - high));
            if (!closestRange || lowestDiff > diff)
            {
                lowestDiff   = diff;
                closestRange = range;
            }
        }

        return closestRange;
    }

    findBoundingPoints(timestamp: moment.Moment): ChartPointBound<T>
    {
        let timestampInMillisec = timestamp.valueOf();

        // Find the two bounding points to the given time
        for (let range of this.ranges)
        {
            let chartPointBound = range.findBounding(timestampInMillisec);
            if (chartPointBound) return chartPointBound;
        }

        // Return null if there were not two bounding points found
        return null;
    }

    extractMinMaxTimestamps(): ChartTimeRange
    {
        let res = new ChartTimeRange();

        if (this.provider)
        {
            if (this.provider.rangeStart) res.minAsMoment = this.provider.rangeStart;
            if (this.provider.rangeEnd) res.maxAsMoment = this.provider.rangeEnd;
        }

        for (let range of this.ranges)
        {
            res.expandRange(range.minDate, range.maxDate);
        }

        return res;
    }

    extractMinMaxValues(): ChartValueRange
    {
        let res = new ChartValueRange();

        for (let range of this.ranges)
        {
            for (let value of range.getNumericValues())
            {
                if (isFinite(value)) res.expandForValue(value);
            }
        }

        return res;
    }

    updateValueRange(valueRange: ChartValueRange,
                     startMs: number,
                     endMs: number,
                     investigateValues: boolean = true): string[]
    {
        let enumRange = this.getEnumeratedRange();
        if (enumRange?.length > 0)
        {
            valueRange.expandForValue(0);
            valueRange.expandForValue(enumRange.length - 1);
            return enumRange;
        }
        else if (investigateValues)
        {
            //
            // Keep track of the samples right before and after the valid window.
            // We start drawing the charts from the outside of the visible window, we need to include them in the range.
            //
            let firstValue = undefined;
            let lastValue  = undefined;

            for (let range of this.ranges)
            {
                let timestamps = range.getTimestamps();
                let values     = range.getNumericValues();

                for (let index = 0; index < timestamps.length; index++)
                {
                    let val = values[index];
                    if (isFinite(val))
                    {
                        let xMillisec = timestamps[index];
                        if (xMillisec < startMs)
                        {
                            firstValue = val;
                        }
                        else if (xMillisec > endMs)
                        {
                            if (lastValue === undefined) lastValue = val;
                        }
                        else
                        {
                            valueRange.expandForValue(val);
                        }
                    }
                }
            }

            if (isFinite(firstValue)) valueRange.expandForValue(firstValue);
            if (isFinite(lastValue)) valueRange.expandForValue(lastValue);
        }

        return null;
    }

    getTooltip(point: ChartPoint<T>): string
    {
        return this.formatter.getTooltip(point);
    }

    getTooltipText(point: ChartPoint<T>): string
    {
        return this.formatter.getTooltipText(point);
    }

    //--//

    public static generateTooltipEntry(key: string,
                                       value: string,
                                       withBreakTag: boolean = true): string
    {
        let res = "";
        if (key && value)
        {
            res += `<span class="chart-prop">${key}</span>: ${value}`;
            if (withBreakTag) res += `<br>`;
        }

        return res;
    }

    public static generateTooltipEntryText(key: string,
                                           value: string,
                                           withNewLine: boolean = true): string
    {
        let res = "";
        if (key && value)
        {
            res += `${key}: ${value}`;
            if (withNewLine) res += "\n";
        }

        return res;
    }

    //--//

    public static valueTooltipTemplate(point: ChartPoint<any>,
                                       abbreviation: string): string
    {
        let tooltip = "";
        switch (point.type)
        {
            case ChartPointType.Value:
                tooltip = `<span class="chart-prop">Value</span>: ${point.displayValue}`;
                if (abbreviation) tooltip += ` ${abbreviation}`;
                break;

            case ChartPointType.NoValue:
                tooltip = `<span class="chart-prop">No Value Due To Sensor Not Sending Data</span>`;
                break;

            case ChartPointType.Missing:
                tooltip = `<span class="chart-prop">No Value Due To Sensor Communication Issue</span>`;
                break;
        }

        return tooltip;
    }

    public static valueTooltipText(point: ChartPoint<any>,
                                   abbreviation: string): string
    {
        let tooltip = "";
        switch (point.type)
        {
            case ChartPointType.Value:
                tooltip = ChartPointSource.generateTooltipEntryText("Value", `${point.displayValue}`, false);
                if (abbreviation) tooltip += ` ${abbreviation}`;
                break;

            case ChartPointType.NoValue:
                tooltip = "No Value Due To Sensor Not Sending Data";
                break;

            case ChartPointType.Missing:
                tooltip = "No Value Due To Sensor Communication Issue";
                break;
        }

        return tooltip;
    }

    //--//

    public isValid(): boolean
    {
        return true;
    }

    abstract isDiscrete(): boolean;

    abstract convertToPoint(timestamp: number,
                            value: number,
                            type: ChartPointType,
                            selected: boolean): ChartPoint<T>;

    abstract getNumericValue(value: T): number;

    abstract getDisplayValue(value: T): string;

    abstract getColorMapping(point: ChartPoint<T>): string;

    abstract getEnumeratedRange(): string[];
}

export class PlaceHolderSource<T> extends ChartPointSource<T>
{
    public isValid(): boolean
    {
        return false;
    }

    public isDiscrete(): boolean
    {
        return false;
    }

    public getColorMapping(point: ChartPoint<T>): string
    {
        return undefined;
    }

    public convertToPoint(timestamp: number,
                          value: number,
                          type: ChartPointType,
                          selected: boolean): ChartPoint<T>
    {
        return undefined;
    }

    public getDisplayValue(value: T): string
    {
        return undefined;
    }

    public getEnumeratedRange(): string[]
    {
        return undefined;
    }

    public getNumericValue(value: T): number
    {
        return 0;
    }
}

export class DataSourceBoolean extends ChartPointSource<boolean>
{
    public isDiscrete(): boolean
    {
        return false;
    }

    public static formatValue(value: number): string
    {
        return value == 0 ? "No" : "Yes";
    }

    public convertToPoint(timestamp: number,
                          value: number,
                          type: ChartPointType,
                          selected: boolean): ChartPoint<boolean>
    {
        return new ChartPoint<boolean>(this, timestamp, value == 1, value, type, selected);
    }

    public getNumericValue(value: boolean): number
    {
        return value ? 1 : 0;
    }

    public getDisplayValue(value: boolean): string
    {
        return DataSourceBoolean.formatValue(this.getNumericValue(value));
    }

    public getColorMapping(point: ChartPoint<boolean>): string
    {
        return null;
    }

    public getEnumeratedRange(): string[]
    {
        return null;
    }
}

export class DataSourceInteger extends ChartPointSource<number>
{
    public static formatValue(value: number): string
    {
        return Math.round(value)
                   .toString();
    }

    constructor(app: ComponentContext,
                formatter: ChartValueFormatter<number>)
    {
        super(app, formatter);
    }

    public isDiscrete(): boolean
    {
        return false;
    }

    public convertToPoint(timestamp: number,
                          value: number,
                          type: ChartPointType,
                          selected: boolean): ChartPoint<number>
    {
        return new ChartPoint<number>(this, timestamp, value, value, type, selected);
    }

    public getNumericValue(value: number): number
    {
        return value;
    }

    public getDisplayValue(value: number): string
    {
        return DataSourceInteger.formatValue(value);
    }

    public getColorMapping(point: ChartPoint<number>): string
    {
        if (point.value == 0)
        {
            return ChartColorUtilities.getColor(0);
        }
        else if (point.value > 0)
        {
            return ChartColorUtilities.getColor(1);
        }
        else
        {
            return ChartColorUtilities.getColor(2);
        }
    }

    public getEnumeratedRange(): string[]
    {
        return null;
    }
}

export class DataSourceDecimal extends ChartPointSource<number>
{
    public static formatValue(value: number): string
    {
        return (Math.round(value * 100) / 100).toString();
    }

    constructor(app: ComponentContext,
                formatter: ChartValueFormatter<number>)
    {
        super(app, formatter);
    }

    public isDiscrete(): boolean
    {
        return false;
    }

    public convertToPoint(timestamp: number,
                          value: number,
                          type: ChartPointType,
                          selected: boolean): ChartPoint<number>
    {
        return new ChartPoint<number>(this, timestamp, value, value, type, selected);
    }

    public getNumericValue(value: number): number
    {
        return value;
    }

    public getDisplayValue(value: number): string
    {
        return DataSourceDecimal.formatValue(value);
    }

    public getColorMapping(point: ChartPoint<number>): string
    {
        if (point.value == 0)
        {
            return ChartColorUtilities.getColor(0);
        }
        else if (point.value > 0)
        {
            return ChartColorUtilities.getColor(1);
        }
        else
        {
            return ChartColorUtilities.getColor(2);
        }
    }

    public getEnumeratedRange(): string[]
    {
        return null;
    }
}

export class DataSourceEnumerated extends ChartPointSource<number | string>
{
    public static mapValuesToNames(values: ChartPointEnumeratedValue[]): { [key: number]: string; }
    {
        let map: { [key: number]: string; } = {};
        for (let value of values) map[value.value] = value.name;

        return map;
    }

    public static formatValue(value: number | string,
                              mapping: { [key: number]: string; }): string
    {
        let result: string = null;

        if (typeof value == "string")
        {
            result = value;
        }
        else if (typeof value == "number")
        {
            result = mapping[value] ? mapping[value] : null;
        }
        else if (typeof value == "boolean")
        {
            result = mapping[value ? 1 : 0] ? mapping[value ? 1 : 0] : null;
        }

        return UtilsService.capitalizeFirstLetterAllWords(result);
    }

    private fromNameToOffset: Lookup<number>  = {};
    private fromValueToOffset: Lookup<number> = {};
    private fromValueToName: Lookup<string>   = {};
    private names: string[]                   = [];

    constructor(app: ComponentContext,
                formatter: ChartValueFormatter<number | string>,
                values: ChartPointEnumeratedValue[])
    {
        super(app, formatter);

        for (let value of values || [])
        {
            this.registerValue(value.name, value.value);
        }
    }

    public registerValue(name: string,
                         value: number)
    {
        let offset = this.names.length;

        this.fromNameToOffset[name]   = offset;
        this.fromValueToOffset[value] = offset;
        this.fromValueToName[value]   = name;

        this.names.push(name);
    }

    public isDiscrete(): boolean
    {
        return true;
    }

    public convertToPoint(timestamp: number,
                          value: number,
                          type: ChartPointType,
                          selected: boolean): ChartPoint<number | string>
    {
        return new ChartPoint<number | string>(this, timestamp, this.names[value], value, type, selected);
    }

    public getNumericValue(value: number | string): number
    {
        if (typeof value == "number")
        {
            return this.fromValueToOffset[value];
        }

        if (typeof value == "boolean")
        {
            return this.fromValueToOffset[value ? 1 : 0];
        }

        if (typeof value == "string")
        {
            let res = this.fromNameToOffset[value];

            if (res !== undefined)
            {
                return res;
            }

            let valueNum = this.names.length;
            this.registerValue(value, valueNum);
            return valueNum;
        }

        return 0;
    }

    public getDisplayValue(value: number | string): string
    {
        return DataSourceEnumerated.formatValue(value, this.fromValueToName);
    }

    public getColorMapping(point: ChartPoint<number | string>): string
    {
        if (this.names.length)
        {
            let index = this.getNumericValue(point.value);
            return ChartColorUtilities.getColor(index);
        }

        return null;
    }

    public getEnumeratedRange(): string[]
    {
        return this.names;
    }
}

export class DataSourceBitSet extends ChartPointSource<string[]>
{
    private m_values: string[][]     = [];
    private m_lookup: Lookup<number> = {};

    public static formatValue(value: string[]): string
    {
        if (value && value.length)
        {
            let labels = [];
            for (let label of value)
            {
                label = UtilsService.replaceAll(label, "_", " ");
                label = UtilsService.capitalizeFirstLetterAllWords(label);
                labels.push(label);
            }
            return labels.join(", ");
        }
        else
        {
            return "Nothing Reported";
        }
    }

    constructor(app: ComponentContext,
                formatter: ChartValueFormatter<string[]>)
    {
        super(app, formatter);
    }

    public isDiscrete(): boolean
    {
        return true;
    }

    public convertToPoint(timestamp: number,
                          value: number,
                          type: ChartPointType,
                          selected: boolean): ChartPoint<string[]>
    {
        return new ChartPoint<string[]>(this, timestamp, this.m_values[value], value, type, selected);
    }

    public getNumericValue(value: string[]): number
    {
        let text  = DataSourceBitSet.formatValue(value);
        let index = this.m_lookup[text];
        if (index === undefined)
        {
            index = this.m_values.length;

            this.m_values[index] = [...value];
            this.m_lookup[text]  = index;
        }

        return index;
    }

    public getDisplayValue(value: string[]): string
    {
        return DataSourceBitSet.formatValue(value);
    }

    public getColorMapping(point: ChartPoint<string[]>): string
    {
        if (point.value && point.value.length)
        {
            return ChartColorUtilities.getColor(1);
        }
        else
        {
            return ChartColorUtilities.getColor(0);
        }
    }

    public getEnumeratedRange(): string[]
    {
        return null;
    }
}

export class DataSourceEnumeratedSet extends ChartPointSource<string[]>
{
    private m_values: string[][]     = [];
    private m_lookup: Lookup<number> = {};

    public static formatValue(value: string[]): string
    {
        if (value && value.length)
        {
            return value.join(", ");
        }
        else
        {
            return "Nothing Reported";
        }
    }

    constructor(app: ComponentContext,
                formatter: ChartValueFormatter<string[]>)
    {
        super(app, formatter);
    }

    public isDiscrete(): boolean
    {
        return true;
    }

    public convertToPoint(timestamp: number,
                          value: number,
                          type: ChartPointType,
                          selected: boolean): ChartPoint<string[]>
    {
        return new ChartPoint<string[]>(this, timestamp, this.m_values[value], value, type, selected);
    }

    public getNumericValue(value: string[]): number
    {
        let text  = DataSourceBitSet.formatValue(value);
        let index = this.m_lookup[text];
        if (index === undefined)
        {
            index = this.m_values.length;

            this.m_values[index] = [...value];
            this.m_lookup[text]  = index;
        }

        return index;
    }

    public getDisplayValue(value: string[]): string
    {
        return DataSourceEnumeratedSet.formatValue(value);
    }

    public getColorMapping(point: ChartPoint<string[]>): string
    {
        let parts = point.value;
        return ChartColorUtilities.getColor(parts == null ? 0 : parts.length + 1);
    }

    public getEnumeratedRange(): string[]
    {
        return null;
    }
}

export enum VisualizationDataSourceState
{
    Active   = "Active",
    Deleted  = "Deleted",
    Target   = "Target",
    Muted    = "Muted",
    Disabled = "Disabled"
}

export function isVisible(sourceState: VisualizationDataSourceState): boolean
{
    switch (sourceState)
    {
        case VisualizationDataSourceState.Active:
        case VisualizationDataSourceState.Muted:
        case VisualizationDataSourceState.Target:
            return true;
    }

    return false;
}


export class ScatterPlotSubSource
{
    public labelGenerator: (label?: string) => string;
    public unitsDisplay: string;

    constructor(public name: string,
                public location: string)
    {

    }
}

export class DataSourceTuple extends ChartPointSource<ScatterPlotPropertyTuple<any>>
{
    private readonly m_discrete: boolean;

    tupleId: string;
    tuples: ScatterPlotPropertyTuple<number>[] = [];

    colorSegments: ColorGradientStop[];

    constructor(app: ComponentContext,
                public dataSources: ScatterPlotPropertyTuple<ChartPointSource<any>>,
                public subSources: ScatterPlotPropertyTuple<ScatterPlotSubSource>,
                formatter: ChartValueFormatter<ScatterPlotPropertyTuple<any>>)
    {
        super(app, formatter);

        this.m_discrete = dataSources.valueX?.isDiscrete() && dataSources.valueY?.isDiscrete() &&
                          (!dataSources.valueZ || dataSources.valueZ.isDiscrete());
    }

    public isDiscrete(): boolean
    {
        return this.m_discrete;
    }

    public getColorMapping(point: ChartPoint<ScatterPlotPropertyTuple<number>>): string
    {
        return "";
    }

    public getDisplayValue(value: ScatterPlotPropertyTuple<number>): string
    {
        return "";
    }

    public getEnumeratedRange(): string[]
    {
        return [];
    }

    public convertToPoint(timestamp: number,
                          value: number,
                          type: ChartPointType,
                          selected: boolean): ChartPoint<ScatterPlotPropertyTuple<any>>
    {
        return new ChartPoint<ScatterPlotPropertyTuple<any>>(this, timestamp, this.tuples[value], value, type, selected);
    }

    public getNumericValue(value: ScatterPlotPropertyTuple<number>): number
    {
        let pos = this.tuples.length;
        this.tuples.push(value);
        return pos;
    }
}

export class ScatterPlotPoint extends ChartPoint<ScatterPlotPropertyTuple<any>>
{
    get isValid(): boolean
    {
        if (this.value.valueX == null) return false;
        if (this.value.valueY == null) return false;
        return !((<DataSourceTuple>this.owningSource).dataSources.valueZ && this.value.valueZ == null);
    }

    toPixel(transform: ScatterPlotTransformer): ChartPixelWithContext<ScatterPlotPropertyTuple<any>>
    {
        let postMappingX = this.getNumericValue((<DataSourceTuple>this.owningSource).dataSources.valueX, this.value.valueX);
        let postMappingY = this.getNumericValue((<DataSourceTuple>this.owningSource).dataSources.valueY, this.value.valueY);
        let projection   = transform.getProjection(postMappingX, postMappingY);

        return new ChartPixelWithContext(projection && projection.x, projection && projection.y, this.type, this);
    }

    getNumericValue(dataSource: ChartPointSource<any>,
                    preMapping: number): number
    {
        if (!dataSource) return preMapping;
        let mappedValue = dataSource.getNumericValue(preMapping);
        return mappedValue != null ? mappedValue : preMapping;
    }
}

export class ScatterPlotPropertyTuple<T>
{
    constructor(public timestamp: number,
                public valueX: T,
                public valueY: T,
                public valueZ?: T)
    {
    }
}

export class ScatterPlotTransformer extends ChartValueConverter<ChartPointRange<ScatterPlotPropertyTuple<number>>>
{
    private readonly m_baseY: number;
    private readonly m_scaler: ScaledPointSet;
    private readonly m_yZeroOffset: number;

    colorGenerator: ColorGradientContinuous;

    constructor(public readonly clipForDataArea: ChartClipArea,
                public readonly processedZoom: ProcessedZoomState,
                private readonly zValueRange?: ProcessedChartValueRange)
    {
        super();

        this.m_scaler = new ScaledPointSet([
                                               new Point(this.processedZoom.leftValue, this.processedZoom.topValue),
                                               new Point(this.processedZoom.rightValue, this.processedZoom.bottomValue)
                                           ]);

        this.m_scaler.setOutput(new AxisAlignedBoundingBox(clipForDataArea.x, clipForDataArea.y, clipForDataArea.width, clipForDataArea.height));

        this.m_baseY = 2 * clipForDataArea.y + clipForDataArea.height;

        this.m_yZeroOffset = UtilsService.clamp(this.processedZoom.bottomValue, this.fromValueToYCoordinate(0), this.processedZoom.topValue);
    }

    public buildColorGenerator(gradientStops: ColorGradientStop[])
    {
        if (this.zValueRange)
        {
            let range = this.zValueRange.range;
            if (range.diff > 0 && gradientStops?.length >= 2)
            {
                this.colorGenerator = new ColorGradientContinuous(ColorMapper.contextualizeGradientStops(gradientStops, range.min, range.max));
            }
        }
    }

    public hitCheck(sourceTuple: DataSourceTuple,
                    tuple: ScatterPlotPropertyTuple<number>): ChartPixelWithContext<ScatterPlotPropertyTuple<number>>
    {
        let point = new ScatterPlotPoint(sourceTuple, tuple.timestamp, tuple, 0, ChartPointType.Value, false);
        let pixel = point.toPixel(this);
        if (this.clipForDataArea.hitCheck(pixel.x, pixel.y)) return pixel;

        return null;
    }

    public getYZeroOffset(): number
    {
        return this.m_yZeroOffset;
    }


    public fromMillisecondToXCoordinate(xValue: number): number
    {
        return this.m_scaler.project(new Vector2(xValue, 0)).x;
    }

    public fromValueToYCoordinate(yValue: number): number
    {
        let projection = this.m_scaler.project(new Vector2(0, yValue));

        return this.m_baseY - projection.y;
    }

    public fromPointToColor(point: ScatterPlotPoint): string
    {
        let dataSourceTuple = <DataSourceTuple>point.owningSource;
        let zValue          = point.getNumericValue(dataSourceTuple.dataSources.valueZ, point.value.valueZ);

        return !isNaN(zValue) ? this.colorGenerator?.getColor(zValue) : undefined;
    }

    public getCanvasGradient(canvas: CanvasRenderingContext2D,
                             x1: number,
                             y1: number,
                             x2: number,
                             y2: number): CanvasGradient
    {
        return this.colorGenerator?.getCanvasGradient(canvas, x1, y1, x2, y2);
    }

    public getProjection(xValue: number,
                         yValue: number): Vector2
    {
        if (isNaN(xValue) || isNaN(yValue)) return undefined;

        let projection = this.m_scaler.project(new Vector2(xValue, yValue));
        projection.y   = this.m_baseY - projection.y;
        return projection;
    }

    public fromXCoordinateToDomain(xCoord: number): number
    {
        return this.m_scaler.unproject(new Vector2(xCoord, 0)).x;
    }

    public fromYCoordinateToValue(yCoord: number): number
    {
        return this.m_scaler.unproject(new Vector2(0, this.m_baseY - yCoord)).y;
    }

    public getInvertedProjection(xCoord: number,
                                 yCoord: number): Vector2
    {
        if (!this.clipForDataArea.hitCheck(xCoord, yCoord)) return null;
        return this.m_scaler.unproject(new Vector2(xCoord, this.m_baseY - yCoord));
    }

    protected generateKey(range: ChartPointRange<ScatterPlotPropertyTuple<number>>,
                          prefix: string): string
    {
        return `${prefix}/${range.id}`;
    }
}

export class ProcessedZoomState
{
    static readonly EmptyPlaceholder = new ProcessedZoomState(0, 0, 1, 1);

    constructor(public topValue: number,
                public bottomValue: number,
                public leftValue: number,
                public rightValue: number)
    {
    }

    toString()
    {
        return `${this.topValue},${this.bottomValue},${this.leftValue},${this.rightValue}`;
    }
}

export class ProcessedChartValueRange
{
    private m_enumRange: string[];

    get enumRange(): string[]
    {
        return this.m_enumRange;
    }

    set enumRange(enums: string[])
    {
        if (enums)
        {
            this.m_enumRange = enums;
            this.range       = new ChartValueRange();
            this.range.expandForValue(0);
            this.range.expandForValue(enums.length);
        }
    }

    range: ChartValueRange = new ChartValueRange();

    expandRange(range: ChartValueRange): boolean
    {
        if (!this.m_enumRange && range.min != null)
        {
            this.range.expandToContain(range);
            return true;
        }
        return false;
    }

    deepCopy(): ProcessedChartValueRange
    {
        let newRange = new ProcessedChartValueRange();
        if (this.m_enumRange)
        {
            newRange.enumRange = this.m_enumRange;
        }
        else
        {
            newRange.expandRange(this.range);
        }

        return newRange;
    }
}
