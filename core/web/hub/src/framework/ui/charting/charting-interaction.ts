import {Renderer2} from "@angular/core";

import {UtilsService} from "framework/services/utils.service";
import {Easing, Sequence} from "framework/ui/charting/charting-animation";
import {ITickInfo, Ticker} from "framework/ui/charting/charting-framework";
import {AxisAlignedBoundingBox, Transform, Vector2} from "framework/ui/charting/charting-math";
import {SimplePubSub} from "framework/utils/pubsub";

export class PanZoom extends SimplePubSub
{
    private m_ticker                          = new Ticker();
    private m_viewBox: AxisAlignedBoundingBox = null;
    private m_virtualViewport: Vector2        = null;

    private m_mouseDown: () => void  = null;
    private m_mouseUp: () => void    = null;
    private m_mouseLeave: () => void = null;
    private m_mouseMove: () => void  = null;
    private m_mouseWheel: () => void = null;

    private m_panning: boolean     = false;
    private m_zooming: boolean     = false;
    private m_panVelocity: Vector2 = new Vector2(0, 0);
    private m_zoomVelocity         = 0;
    private m_panTimeout: number   = null;
    private m_zoomOrigin: Vector2  = null;

    private m_zoomTarget: AxisAlignedBoundingBox = null;
    private m_zoomDuration: number               = null;
    private m_scaleSequence: Sequence            = null;
    private m_xSequence: Sequence                = null;
    private m_ySequence: Sequence                = null;

    public interactable = true;
    public transform    = new Transform();
    public translation  = new Vector2(0, 0);
    public scale        = 1;

    constructor(private renderer: Renderer2,
                private viewport: HTMLElement,
                public options: PanZoomOptions = new PanZoomOptions())
    {
        super();

        // Set up the ticker
        this.m_ticker.subscribe("tick", (info) => this.tick(info));

        // Measure the target box
        this.measureTarget();

        // Bind DOM events
        this.m_mouseDown  = this.renderer.listen(this.viewport, "mousedown", (e: MouseEvent) => this.onPanStart(e));
        this.m_mouseWheel = this.renderer.listen(this.viewport, "mousewheel", (e: WheelEvent) => this.onZoom(e));
    }

    public dispose()
    {
        // Unregister DOM listeners
        if (this.m_mouseDown) this.m_mouseDown();
        if (this.m_mouseUp) this.m_mouseUp();
        if (this.m_mouseLeave) this.m_mouseLeave();
        if (this.m_mouseMove) this.m_mouseMove();
        if (this.m_mouseWheel) this.m_mouseWheel();
    }

    public isZoomingTo(): boolean
    {
        if (this.m_scaleSequence && !this.m_scaleSequence.isDone()) return true;
        return false;
    }

    public zoomProgress(): number
    {
        if (this.isZoomingTo()) return this.m_scaleSequence.percent();
        return 1;
    }

    public resetTo(view: AxisAlignedBoundingBox)
    {
        // Assign a new zoom target
        this.m_zoomTarget   = null;
        this.m_zoomDuration = 0;

        // Clear velocity and other parameters
        this.m_panVelocity  = new Vector2(0, 0);
        this.m_zoomVelocity = 0;
        this.m_zoomOrigin   = null;

        // Calculate a target scale and target translation
        let scale       = Math.min((this.m_viewBox.width / view.width), (this.m_viewBox.height / view.height));
        let translation = view.center();

        // Immediately apply new settings
        this.scale = scale;
        this.translation = translation;
        this.setTransform();

        // Sleep ticker if awake
        this.sleep();
    }

    public zoomTo(view: AxisAlignedBoundingBox,
                  duration: number = 3000)
    {
        // Assign a new zoom target
        this.m_zoomTarget   = view;
        this.m_zoomDuration = duration;

        // Clear velocity and other parameters
        this.m_panVelocity  = new Vector2(0, 0);
        this.m_zoomVelocity = 0;
        this.m_zoomOrigin   = null;

        // Calculate a target scale and target translation
        let scale       = Math.min((this.m_viewBox.width / this.m_zoomTarget.width), (this.m_viewBox.height / this.m_zoomTarget.height));
        let translation = this.m_zoomTarget.center();

        // Create a new sequences to animate
        this.m_scaleSequence = new Sequence([
                                                {
                                                    value       : this.scale,
                                                    time        : 0,
                                                    interpolator: Easing.linear
                                                },
                                                {
                                                    value       : UtilsService.clamp(this.options.minZoom, this.options.maxZoom, scale),
                                                    time        : duration,
                                                    interpolator: Easing.easeInOutQuart
                                                }
                                            ]);
        this.m_xSequence     = new Sequence([
                                                {
                                                    value       : this.translation.x,
                                                    time        : 0,
                                                    interpolator: Easing.linear
                                                },
                                                {
                                                    value       : translation.x,
                                                    time        : duration,
                                                    interpolator: Easing.easeInOutQuart
                                                }
                                            ]);
        this.m_ySequence     = new Sequence([
                                                {
                                                    value       : this.translation.y,
                                                    time        : 0,
                                                    interpolator: Easing.linear
                                                },
                                                {
                                                    value       : translation.y,
                                                    time        : duration,
                                                    interpolator: Easing.easeInOutQuart
                                                }
                                            ]);

        // Wake the ticker
        this.wake();
    }

    public cancelZoomTo()
    {
        this.m_zoomTarget = null;
    }

    private onPanStart(event: MouseEvent)
    {
        // Short circuit if not interactable
        if (!this.interactable) return;

        // Flag as panning
        this.m_panning = true;

        // Cancel any zoomTo
        this.cancelZoomTo();

        // Reset pan velocity tracker and stop any animation or smoothing
        this.m_panVelocity = new Vector2(0, 0);
        this.sleep();

        // Attach other event listeners to track mouse movement
        this.m_mouseUp    = this.renderer.listen("body", "mouseup", (e: MouseEvent) => this.onPanEnd(e));
        this.m_mouseLeave = this.renderer.listen("body", "mouseleave", (e: MouseEvent) => this.onPanEnd(e));
        this.m_mouseMove  = this.renderer.listen("body", "mousemove", (e: MouseEvent) => this.onPan(e));

        // Notify of pan start
        this.executeCallbacks("panStart");
    }

    private onPanEnd(event: MouseEvent)
    {
        // Short circuit if not interactable
        if (!this.interactable) return;

        // Flag as not panning
        this.m_panning = false;

        // Cancel any pan timeout
        this.cancelPanTimeout();

        // Remove active panning listeners
        this.m_mouseUp();
        this.m_mouseLeave();
        this.m_mouseMove();

        // If smoothing is enabled wake up the ticker to apply smoothing
        if (this.options.smooth)
        {
            this.wake();
        }

        // Notify of pan end
        this.executeCallbacks("panEnd");
    }

    private onPan(event: MouseEvent)
    {
        // Short circuit if not interactable
        if (!this.interactable) return;

        // Only process if actively panning
        if (this.m_panning)
        {
            this.translation.x -= event.movementX / this.scale;
            this.translation.y -= event.movementY / this.scale;

            let angle       = Math.abs(this.m_panVelocity.angle(new Vector2(event.movementX, event.movementY)));
            let angleWeight = angle > Math.PI / 2 ? (((angle - (Math.PI / 2)) / (Math.PI / 2)) * 0.8) : 0;

            this.m_panVelocity.x = (0.8 - angleWeight) * this.m_panVelocity.x + (0.2 + angleWeight) * (-event.movementX / this.scale);
            this.m_panVelocity.y = (0.8 - angleWeight) * this.m_panVelocity.y + (0.2 + angleWeight) * (-event.movementY / this.scale);
            this.m_zoomVelocity *= this.options.zoomDrag / 2;

            this.setTransform();

            // If there was an existing pan timeout, clear it
            this.cancelPanTimeout();
            // Schedule a new pan timeout
            this.schedulePanTimeout();
        }
    }

    private onZoom(event: WheelEvent)
    {
        // Short circuit if not interactable
        if (!this.interactable) return;

        // Flag as zooming
        this.m_zooming = true;

        // Cancel event defaults
        event.stopPropagation();
        event.preventDefault();

        // Calculate a scale factor delta
        let delta = 0;
        if (event.deltaY < 0)
        {
            delta = Math.min(Math.sqrt(Math.abs(event.deltaY)) * this.options.zoomMultiplier, 1);
        }
        else if (event.deltaY > 0)
        {
            delta = -Math.min(Math.sqrt(Math.abs(event.deltaY)) * this.options.zoomMultiplier, 0.5);
        }

        // Calculate an origin point
        let origin;
        if (!isNaN(event.offsetX) && !isNaN(event.offsetY))
        {
            let x  = (event.offsetX - (this.viewport.clientWidth / 2));
            let y  = (event.offsetY - (this.viewport.clientHeight / 2));
            origin = new Vector2(x, y);
        }

        // Track the zoom velocity
        this.m_zoomVelocity = 0.5 * this.m_zoomVelocity + 0.5 * delta;

        // Apply the scale factor and update the transform
        this.applyZoom(1 + delta, origin);
        this.setTransform();

        // If smoothing is enabled wake up the ticker to apply smoothing
        if (this.options.smooth)
        {
            if (origin) this.m_zoomOrigin = origin;
            this.wake();
        }

        // Notify of zoom
        this.executeCallbacks("zoom");
    }

    private schedulePanTimeout()
    {
        this.m_panTimeout = setTimeout(() =>
                                       {
                                           if (this.m_panning)
                                           {
                                               this.m_panVelocity.scale(0.2);
                                               this.schedulePanTimeout();
                                           }
                                       }, 50);
    }

    private cancelPanTimeout()
    {
        if (this.m_panTimeout)
        {
            clearTimeout(this.m_panTimeout);
            this.m_panTimeout = null;
        }
    }

    private tick(info: ITickInfo)
    {
        // If there is a target box track to that, otherwise just apply velocity and taper off
        if (this.m_zoomTarget)
        {
            let lastTranslation = this.translation.clone();
            let lastScale       = this.scale;

            // Update the targeting animation
            this.m_scaleSequence.update(info.dt);
            this.m_xSequence.update(info.dt);
            this.m_ySequence.update(info.dt);

            // Apply the animation
            this.scale         = this.m_scaleSequence.get();
            this.translation.x = this.m_xSequence.get();
            this.translation.y = this.m_ySequence.get();

            // Update velocities
            this.m_panVelocity  = this.translation.differenceVector(lastTranslation);
            this.m_zoomVelocity = this.scale - lastScale;

            // Update the transform
            this.setTransform();

            // Exit target mode if animation is finished
            if (this.m_scaleSequence.isDone()) this.m_zoomTarget = null;

            // Notify of targeting tick
            this.executeCallbacks("zoomToTick");
        }
        else if (this.options.smooth)
        {
            // Only pan if not actively panning
            if (!this.m_panning)
            {
                // Protects against NaN and Infinities
                if (isNaN(this.m_panVelocity.x) || !isFinite(this.m_panVelocity.x)) this.m_panVelocity.x = 0;
                if (isNaN(this.m_panVelocity.y) || !isFinite(this.m_panVelocity.y)) this.m_panVelocity.y = 0;

                this.m_panVelocity.scale(this.options.panDrag);
                this.translation.x += this.m_panVelocity.x;
                this.translation.y += this.m_panVelocity.y;
            }

            // Only zoom if not actively zooming
            if (!this.m_zooming)
            {
                // Protects against NaN and Infinities
                if (isNaN(this.m_zoomVelocity) || !isFinite(this.m_zoomVelocity)) this.m_zoomVelocity = 0;

                this.m_zoomVelocity *= this.options.zoomDrag;
                this.applyZoom(1 + this.m_zoomVelocity, this.m_zoomOrigin);
            }
            else
            {
                this.m_zooming = false;
            }

            // Only update the transform if something was smoothed
            if (!this.m_panning || !this.m_zooming) this.setTransform();

            // Stop smoothing if the view is mostly settled
            if (this.m_panVelocity.length() < 1 && this.m_zoomVelocity < 0.05) this.m_ticker.stop();
        }
        else
        {
            this.sleep();
        }
    }

    private applyZoom(delta: number,
                      origin?: Vector2)
    {
        // Save the old scale value
        let old = this.scale;

        // Apply the scale and apply bounds
        this.scale = UtilsService.clamp(this.options.minZoom, this.options.maxZoom, this.scale * delta);

        // If an origin was given, calculate an offset
        if (origin)
        {
            let zoom = this.scale / old;
            let dx   = (origin.x / (old * zoom)) - (origin.x / old);
            let dy   = (origin.y / (old * zoom)) - (origin.y / old);

            // Apply the offset
            this.translation.x -= dx;
            this.translation.y -= dy;
        }
    }

    public targetChanged(reset: boolean            = true,
                         viewportOverride: Vector2 = null)
    {
        // Override the target if virtual viewport is provided
        if (viewportOverride)
        {
            this.m_virtualViewport = viewportOverride;
        }
        else
        {
            this.m_virtualViewport = null;
        }

        // Force a re-measure of the target
        this.measureTarget(reset);
    }

    private wake()
    {
        this.m_ticker.start();
    }

    private sleep()
    {
        this.m_ticker.stop();
    }

    private measureTarget(reset: boolean = true)
    {
        // Update the target element viewport box
        // Use the virtual viewport override if it is set
        let w          = this.m_virtualViewport ? this.m_virtualViewport.x : this.viewport.clientWidth;
        let h          = this.m_virtualViewport ? this.m_virtualViewport.y : this.viewport.clientHeight;
        this.m_viewBox = new AxisAlignedBoundingBox(w / -2, h / -2, w, h);

        if (reset)
        {
            this.translation = this.m_viewBox.center();
            this.scale       = 1;
            this.setTransform();
        }
    }

    private setTransform()
    {
        let center = this.m_viewBox.center();
        this.transform.reset();
        this.transform.translate(new Vector2(center.x, center.y));
        this.transform.scale(new Vector2(this.scale, this.scale));
        this.transform.translate(new Vector2(-this.translation.x, -this.translation.y));


        // Notify of transform change
        this.executeCallbacks("change", this.transform);
    }

    private containingBody(target: HTMLElement): HTMLElement
    {
        while (target && target.tagName !== "body") target = target.parentElement;
        return target;
    }
}

export class PanZoomOptions
{
    constructor(public minZoom: number                = 0.25,
                public maxZoom: number                = 20,
                public zoomMultiplier: number         = 0.015,
                public zoomDrag: number               = 0.75,
                public panDrag: number                = 0.9,
                public smooth: boolean                = true,
                public bounds: AxisAlignedBoundingBox = null)
    {}
}

export interface EventReceiver
{
    onMouseMove(x: number,
                y: number): boolean;

    onMouseLeave(): void;
}
