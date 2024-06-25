import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {AxisAlignedBoundingBox, Transform, Vector2} from "framework/ui/charting/charting-math";
import {SimplePubSub} from "framework/utils/pubsub";

export interface IProjectable
{
    project: (vec: Vector2) => Vector2,
    unproject: (vec: Vector2) => Vector2
}

export interface ITickInfo
{
    dt: number,
    time: number,
    tick: number
}

export class TickInfo implements ITickInfo
{
    constructor(public dt: number,
                public time: number,
                public tick: number)
    {}
}

export class Ticker extends SimplePubSub
{
    private m_elapsedTicks: number = 0;
    private m_elapsedTime: number  = 0;
    private m_isRunning: boolean   = false;

    private m_next: number;
    private m_scheduledAt: number;

    constructor(startImmediately: boolean = false)
    {
        super();

        if (startImmediately) this.start();
    }

    get tick(): number
    {
        return this.m_elapsedTicks;
    }

    get time(): number
    {
        return this.m_elapsedTime;
    }

    get isRunning(): boolean
    {
        return this.m_isRunning;
    }

    public start(): void
    {
        if (!this.m_isRunning)
        {
            this.m_scheduledAt = performance.now();
            this.m_next        = requestAnimationFrame((time) => this.doTick(time));
            this.m_isRunning   = true;
        }
    }

    public stop(): void
    {
        if (this.m_isRunning)
        {
            cancelAnimationFrame(this.m_next);

            this.m_scheduledAt = undefined;
            this.m_next        = undefined;
            this.m_isRunning   = false;
        }
    }

    subscribe(event: string,
              callback: (info: ITickInfo) => void,
              priority: number = 0): void
    {
        super.subscribe(event, callback, priority);
    }

    unsubscribe(event: string,
                callback: (info: ITickInfo) => void): void
    {
        super.unsubscribe(event, callback);
    }

    dispose(): void
    {
        this.stop();
        this.unsubscribeAll();
    }

    private doTick(time: number): void
    {
        let dt = Math.max(0, time - this.m_scheduledAt);

        this.m_elapsedTime += dt;
        this.m_elapsedTicks++;

        let info = new TickInfo(dt, this.m_elapsedTime, this.m_elapsedTicks);
        this.executeCallbacks("tick", info);

        if (this.m_isRunning)
        {
            this.m_scheduledAt = time;
            this.m_next        = requestAnimationFrame((time) => this.doTick(time));
        }
        else
        {
            this.m_scheduledAt = undefined;
            this.m_next        = undefined;
        }
    }
}

export class Canvas
{
    public isMounted: boolean = false;
    public target: HTMLElement;

    public dom: HTMLElement;

    public interactionLayer: Layer;
    public layerContainer: HTMLElement;
    public layers: Layer[] = [];

    public aabb: AxisAlignedBoundingBox = new AxisAlignedBoundingBox(0, 0, 0, 0);

    public dpiScale: number        = 1;
    public dpiTransform: Transform = new Transform();

    public readonly ticker: Ticker = new Ticker();

    get dirty(): boolean
    {
        return this.layers.some((layer) => layer.dirty);
    }

    constructor(width: number,
                height: number)
    {
        // Create a container element
        this.dom = document.createElement("div");
        this.dom.setAttribute("o3-canvas", "");

        // Create a container for all other visual layers
        this.layerContainer = document.createElement("div");
        this.dom.appendChild(this.layerContainer);

        // Create an interaction layer
        this.interactionLayer = new Layer(this);
        this.dom.appendChild(this.interactionLayer.dom);

        // Set the initial size
        this.resize(width, height);

        // Register the render method on the ticker and start rendering
        this.ticker.subscribe("tick", (info) => this.render(info), 0);
    }

    mount(target: HTMLElement): void
    {
        if (this.isMounted) this.unmount();

        this.target = target;
        this.target.appendChild(this.dom);
        this.isMounted = true;
    }

    unmount(): void
    {
        if (this.isMounted)
        {
            this.target.removeChild(this.dom);
            this.target    = undefined;
            this.isMounted = false;
        }
    }

    reposition(x: number,
               y: number): void
    {
        this.aabb.x = x;
        this.aabb.y = y;

        this.interactionLayer.reposition();
        for (let layer of this.layers) layer.reposition();
    }

    resize(width: number,
           height: number): void
    {
        // Update the bounding box
        this.aabb.width  = width;
        this.aabb.height = height;

        // Check and set DPI scaling
        let helper    = new ChartHelpers(this.interactionLayer.ctx);
        this.dpiScale = helper.pixelRatio();
        this.dpiTransform.reset();
        this.dpiTransform.scale(new Vector2(this.dpiScale, this.dpiScale));

        // Resize all layers
        this.interactionLayer.onResize();
        for (let layer of this.layers) layer.onResize();

        // Force re-render if the ticker is off
        if (!this.ticker.isRunning) this.render(new TickInfo(0, this.ticker.time, this.ticker.tick));
    }

    appendLayer(): Layer
    {
        let layer = new Layer(this);
        this.layers.push(layer);
        this.layerContainer.appendChild(layer.dom);
        return layer;
    }

    prependLayer(): Layer
    {
        let layer = new Layer(this);
        this.layers.unshift(layer);
        this.layerContainer.insertBefore(layer.dom, this.layerContainer.firstChild);
        return layer;
    }

    render(info: ITickInfo): void
    {
        // Render dirty layers
        for (let layer of this.layers)
        {
            if (layer.dirty) layer.render(info);
        }
    }

    dispose(): void
    {
        this.ticker.dispose();
        for (let layer of this.layers)
        {
            layer.dispose();
        }
    }

    setDebugState(tag: string, value: string = null)
    {
        if(value)
        {
            this.dom.setAttribute(`o3-debug-${tag}`, value);
        }
        else
        {
            this.dom.removeAttribute(`o3-debug-${tag}`)
        }
    }
}

export class Layer extends SimplePubSub implements IProjectable
{
    public dirty: boolean = true;

    public dom: HTMLCanvasElement;
    public ctx: CanvasRenderingContext2D;

    public transform: Transform = new Transform();

    get width(): number
    {
        return this.container.aabb.width;
    }

    get height(): number
    {
        return this.container.aabb.height;
    }

    get x(): number
    {
        return this.container.aabb.x;
    }

    get y(): number
    {
        return this.container.aabb.y;
    }

    constructor(public container: Canvas)
    {
        super();

        this.dom = document.createElement("canvas");
        this.ctx = this.dom.getContext("2d");

        this.dom.style.position = "absolute";
        this.reposition();

        this.onResize();
    }

    reposition()
    {
        this.dom.style.left = `${this.x}px`;
        this.dom.style.top  = `${this.y}px`;
    }

    moveUp(): void
    {
        let layers = this.container.layers;
        let index  = layers.indexOf(this);
        if (index > 0)
        {
            let swap          = layers[index - 1];
            layers[index - 1] = layers[index];
            layers[index]     = swap;
        }
    }

    moveDown(): void
    {
        let layers = this.container.layers;
        let index  = layers.indexOf(this);
        if (index >= 0 && index < layers.length - 1)
        {
            let swap          = layers[index + 1];
            layers[index + 1] = layers[index];
            layers[index]     = swap;
        }
    }

    onResize(): void
    {
        this.dom.width        = this.width * this.container.dpiScale;
        this.dom.height       = this.height * this.container.dpiScale;
        this.dom.style.width  = this.width + "px";
        this.dom.style.height = this.height + "px";

        this.container.dpiTransform.setContext(this.ctx);

        this.dirty = true;
    }

    attachRenderCallback(callback: (ctx: CanvasRenderingContext2D,
                                    info: ITickInfo) => void,
                         priority: number = 0)
    {
        this.subscribe("render", callback, priority);
    }

    render(info: ITickInfo): void
    {
        if (!this.dirty) return;

        this.ctx.save();

        // Clear previous contents
        this.clear();

        // Apply the layer transform
        this.transform.applyToContext(this.ctx);

        // Execute individual renderer callbacks
        let callbacks = this.getEventCallbacks("render");
        for (let entry of callbacks)
        {
            this.ctx.save();
            entry.callback(this.ctx, info);
            this.ctx.restore();
        }

        this.ctx.restore();

        this.dirty = false;
    }

    clear(): void
    {
        this.ctx.clearRect(0, 0, this.width, this.height);
    }

    subscribe(event: string,
              callback: (ctx: CanvasRenderingContext2D,
                         info: ITickInfo) => void,
              priority: number = 0): void
    {
        super.subscribe(event, callback, priority);
    }

    unsubscribe(event: string,
                callback: (ctx: CanvasRenderingContext2D,
                           info: ITickInfo) => void): void
    {
        super.unsubscribe(event, callback);
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

    dispose(): void
    {
        this.unsubscribeAll();
    }
}

export class Point extends Vector2
{
    constructor(public x: number,
                public y: number,
                public meta?: any)
    {
        super(x, y);
    }

    static fromVector(vec2: Vector2,
                      meta?: any): Point
    {
        return new Point(vec2.x, vec2.y, meta);
    }
}

export class IntermediatePoint extends Point
{
    constructor(public x: number,
                public y: number,
                public source: Point,
                meta?: any)
    {
        super(x, y, meta);

        // Default behavior is to always re-reference to original source
        if (source instanceof IntermediatePoint)
        {
            this.source = (<IntermediatePoint>this.source).source;
        }
    }
}
