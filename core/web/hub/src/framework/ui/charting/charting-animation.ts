interface IInterpolator
{
    (t: number): number
}

interface IKeyframe
{
    value: number,
    time: number,
    interpolator: IInterpolator
}

class SequenceOptions
{
    constructor(public loop: boolean                = false,
                public loops: number                = -1,
                public direction: SequenceDirection = SequenceDirection.Forwards,
                public duration: number             = -1)
    {}

}

interface ISequenceSegment
{
    start: number,
    end: number,
    tween: Tween
}

export enum SequenceDirection
{
    Forwards = 0, Backwards = 1, Alternate = 2
}

export class Sequence
{
    private m_time: number                 = 0;
    private m_segmentTime: number          = 0;
    private m_segments: ISequenceSegment[] = [];
    private m_options: SequenceOptions;

    private m_sequenceDuration: number;
    private m_loopDuration: number;
    private m_segmentsDuration: number;
    private m_loop: number;
    private m_parity: number;

    constructor(public keyframes: IKeyframe[],
                options: SequenceOptions = new SequenceOptions())
    {
        // Set configuration options
        this.m_options = options;

        // Calculate segments
        for (let i = 1; i < keyframes.length; i++)
        {
            this.m_segments.push(this.makeSegment(keyframes[i - 1], keyframes[i]));
        }

        // Cache durations
        this.m_sequenceDuration = this.sequenceDuration();
        this.m_loopDuration     = this.loopDuration();
        this.m_segmentsDuration = this.segmentsDuration();
    }

    update(dt: number): void
    {
        this.seek(this.m_time + dt);
    }

    seek(time: number): void
    {
        this.m_time = time;
        this.updateState();
    }

    get(): number
    {
        // Find the correct segment
        let segment = this.getSegment(this.m_segmentTime);

        // Return the current value
        segment.tween.seek(this.m_segmentTime - segment.start);
        return segment.tween.get("value");
    }

    time(): number
    {
        return Math.max(0, Math.min(this.m_sequenceDuration, this.m_time));
    }

    percent(): number
    {
        let time = this.time() % this.m_loopDuration;
        if (this.m_time > this.m_sequenceDuration) time = this.m_loopDuration;
        if (this.m_options.direction === SequenceDirection.Backwards || (this.m_options.direction === SequenceDirection.Alternate && this.m_parity === 1)) time = this.m_loopDuration - time;

        return time / this.m_loopDuration;
    }

    isDone()
    {
        return this.time() !== this.m_time;
    }

    private updateState()
    {
        this.m_loop   = Math.floor(this.m_time / this.m_loopDuration);
        this.m_parity = this.m_loop % 2;

        let time = this.time() % this.m_loopDuration;
        if (this.m_time > this.m_sequenceDuration) time = this.m_loopDuration;
        if (this.m_options.direction === SequenceDirection.Backwards || (this.m_options.direction === SequenceDirection.Alternate && this.m_parity === 1)) time = this.m_loopDuration - time;

        this.m_segmentTime = time * (this.m_segmentsDuration / this.m_loopDuration);
    }

    private sequenceDuration(): number
    {
        // If finite calculate the duration of the entire sequence
        if (this.isFinite())
        {
            // If explicit duration is given then that is the sequence duration,
            // otherwise it is the loop duration * number of loops
            return this.hasExplicitDuration() ? this.m_options.duration : this.loopDuration() * this.maxLoops();
        }

        // Return infinity if sequence is not finite
        return Infinity;
    }

    private loopDuration(): number
    {
        // If there is an explicit sequence duration the loop duration is the segment
        // duration scaled, otherwise it is just the segment duration
        return this.hasExplicitDuration() ? this.m_options.duration / this.segmentsDuration() : this.segmentsDuration();
    }

    private segmentsDuration(): number
    {
        return this.m_segments[this.m_segments.length - 1].end;
    }

    private hasExplicitDuration(): boolean
    {
        return !isNaN(this.m_options.duration) && this.m_options.duration > 0;
    }

    private isFinite(): boolean
    {
        return !this.m_options.loop || (this.m_options.loop && this.m_options.loops > 0);
    }

    private maxLoops(): number
    {
        return this.m_options.loop ? (this.isFinite() ? this.m_options.loops : Infinity) : 1;
    }

    private makeSegment(k1: IKeyframe,
                        k2: IKeyframe): ISequenceSegment
    {
        return {
            start: k1.time,
            end  : k2.time,
            tween: new Tween({value: k1.value}, {value: k2.value}, k2.time - k1.time, k2.interpolator)
        };
    }

    private getSegment(time: number): ISequenceSegment
    {
        let s = this.m_segments;

        if (time < s[0].start) return s[0];
        if (time > s[s.length - 1].end) return s[s.length - 1];

        let low  = 0;
        let high = s.length - 1;
        let mid;

        while (low <= high)
        {
            mid = Math.floor((low + high) / 2);

            if (time < s[mid].start)
            {
                high = mid - 1;
            }
            else if (time > s[mid].end)
            {
                low = mid + 1;
            }
            else
            {
                return s[mid];
            }
        }

        // Reaching this means we somehow have a gap in our sequence, which should not happen.
        // If it does, lets just make a fake segment to linearly interpolate
        return {
            start: s[high].end,
            end  : s[low].start,
            tween: new Tween({value: s[high].tween.end["value"]}, {value: s[low].tween.start["value"]}, s[low].start - s[high].end, Easing.linear)
        };
    }
}

export class Tween
{
    private m_commonMap: any       = {};
    private m_commonList: string[] = [];
    private m_time: number         = 0;

    constructor(public start: any,
                public end: any,
                public duration: number,
                public interpolator: IInterpolator = Easing.easeInOutQuad)
    {
        let properties = Object.getOwnPropertyNames(start);
        for (let property of properties)
        {
            if (typeof start[property] === "number" && typeof end[property] === "number")
            {
                this.m_commonMap[property] = true;
                this.m_commonList.push(property);
            }
        }
    }

    update(dt: number): void
    {
        this.seek(this.m_time + dt);
    }

    seek(time: number): void
    {
        this.m_time = time < 0 ? 0 : time > this.duration ? this.duration : time;
    }

    get(property: string): number
    {
        if (!this.m_commonMap[property]) return undefined;

        return this.start[property] + (this.end[property] - this.start[property]) * this.interpolator(this.m_time / this.duration);
    }

    getAll(): object
    {
        let result: any = {};
        for (let property of this.m_commonList) result[property] = this.get(property);

        return result;
    }
}

export abstract class Easing
{
    public static linear(t: number): number
    {
        return t;
    }

    // Ease-in functions
    public static easeInQuad(t: number): number
    {
        return Math.pow(t, 2);
    }

    public static easeInCubic(t: number): number
    {
        return Math.pow(t, 3);
    }

    public static easeInQuart(t: number): number
    {
        return Math.pow(t, 4);
    }

    public static easeInQuint(t: number): number
    {
        return Math.pow(t, 5);
    }

    // Ease-out functions
    public static easeOutQuad(t: number): number
    {
        return Easing.invert(t, Easing.easeInQuad);
    }

    public static easeOutCubic(t: number): number
    {
        return Easing.invert(t, Easing.easeInCubic);
    }

    public static easeOutQuart(t: number): number
    {
        return Easing.invert(t, Easing.easeInQuart);
    }

    public static easeOutQuint(t: number): number
    {
        return Easing.invert(t, Easing.easeInQuint);
    }

    // Ease in-out functions
    public static easeInOutQuad(t: number): number
    {
        return Easing.inOut(t, Easing.easeInQuad, Easing.easeOutQuad);
    }

    public static easeInOutCubic(t: number): number
    {
        return Easing.inOut(t, Easing.easeInCubic, Easing.easeOutCubic);
    }

    public static easeInOutQuart(t: number): number
    {
        return Easing.inOut(t, Easing.easeInQuart, Easing.easeOutQuart);
    }

    public static easeInOutQuint(t: number): number
    {
        return Easing.inOut(t, Easing.easeInQuint, Easing.easeOutQuint);
    }

    // Utility functions
    private static invert(t: number,
                          fn: IInterpolator): number
    {
        return 1 - fn(1 - t);
    }

    private static inOut(t: number,
                         inFn: IInterpolator,
                         outFn: IInterpolator): number
    {
        if (t <= 0.5)
        {
            return 0.5 * inFn(t * 2);
        }
        else
        {
            return 0.5 * outFn((t - 0.5) * 2) + 0.5;
        }
    }
}
