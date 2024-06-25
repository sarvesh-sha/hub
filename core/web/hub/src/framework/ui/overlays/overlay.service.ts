import {Overlay, OverlayRef, PositionStrategy, ViewportRuler} from "@angular/cdk/overlay";
import {OverlayConfig as CdkOverlayConfig} from "@angular/cdk/overlay/overlay-config";
import {ComponentPortal, Portal, TemplatePortal} from "@angular/cdk/portal";
import {ScrollDispatcher} from "@angular/cdk/scrolling";
import {ComponentRef, EmbeddedViewRef, Injectable} from "@angular/core";
import {NavigationStart, Router} from "@angular/router";
import {MaximizePositionStrategy} from "framework/ui/overlays/MaximizePositionStrategy";
import {CollisionShape, TrackedPositionStrategy} from "framework/ui/overlays/TrackedPositionStrategy";
import {Future} from "framework/utils/concurrency";

@Injectable({providedIn: "root"})
export class OverlayService
{
    constructor(private m_cdkOverlay: Overlay,
                private m_ruler: ViewportRuler,
                private m_scroll: ScrollDispatcher,
                public router: Router)
    {}

    public createOverlay(cfg: CdkOverlayConfig,
                         portal: Portal<any>,
                         behaviors?: OverlayBehaviors<any>): OverlayInstance<any>
    {
        let overlayRef = this.m_cdkOverlay.create(cfg);
        let portalRef  = overlayRef.attach(portal);

        return new OverlayInstance(this, overlayRef, portalRef, behaviors);
    }

    public createViewOverlay(cfg: CdkOverlayConfig,
                             portal: TemplatePortal<any>,
                             behaviors?: OverlayBehaviors<EmbeddedViewRef<any>>): OverlayInstance<EmbeddedViewRef<any>>
    {
        return this.createOverlay(cfg, portal, behaviors);
    }

    public createComponentOverlay<T>(cfg: CdkOverlayConfig,
                                     portal: ComponentPortal<T>,
                                     behaviors?: OverlayBehaviors<ComponentRef<T>>): OverlayInstance<ComponentRef<T>>
    {
        return this.createOverlay(cfg, portal, behaviors);
    }

    public generateMaximizeStrategy(sourceContainer: HTMLElement,
                                    maximizeContainer: HTMLElement,
                                    scale: number,
                                    speed: number): AnimatedPositionStrategy
    {
        return new MaximizePositionStrategy(this.m_ruler, this.m_scroll, sourceContainer, maximizeContainer, speed, speed, scale, scale);
    }

    public generateTrackedStrategy(sourceContainer: HTMLElement,
                                   boundsContainer: HTMLElement,
                                   collisionShape: CollisionShape): PositionStrategy
    {
        return new TrackedPositionStrategy(this.m_ruler, this.m_scroll, sourceContainer, boundsContainer, collisionShape);
    }
}

export class OverlayInstance<T extends ComponentRef<any> | EmbeddedViewRef<any> | any>
{
    private m_isAnimated: boolean = false;

    constructor(private m_service: OverlayService,
                public overlayRef: OverlayRef,
                public portal: T,
                public behaviors?: OverlayBehaviors<T>)
    {
        // Check if the position strategy used is an AnimatedPositionStrategy
        this.m_isAnimated = this.isAnimatedPositionStrategy(overlayRef.getConfig().positionStrategy);

        // If behaviors are provided, apply them
        if (behaviors)
        {
            // Register any behaviors
            this.applyBehaviors();

            // Immediately trigger post-attach callback
            if (behaviors.afterAttach) behaviors.afterAttach(this);
            // If not using animated position strategy, immediately fire post-attach-animation callback
            if (!this.m_isAnimated && behaviors.afterAttachAnimation) behaviors.afterAttachAnimation(this);
        }

        // Listen for self-initiated detachments to trigger cleanup
        this.overlayRef.detachments()
            .subscribe(() =>
                       {
                           this.dispose();
                       });
    }

    public async dispose()
    {
        let strategy = this.overlayRef.getConfig().positionStrategy;

        // First detach the position strategy
        strategy.detach();

        // Trigger post-detach callback if given
        if (this.behaviors?.afterDetach) this.behaviors.afterDetach(this);
        // If not using animated position strategy, immediately fire post-attach-animation callback
        if (!this.m_isAnimated && this.behaviors?.afterDetachAnimation) this.behaviors?.afterDetachAnimation(this);

        // If it is an animated position strategy, wait for the detach to finish
        if (this.m_isAnimated)
        {
            await (<AnimatedPositionStrategy>strategy).detaching;
        }

        // Dispose of the overlay
        this.overlayRef.dispose();

        // Trigger post-dispose callback if given
        if (this.behaviors?.afterDispose) this.behaviors.afterDispose(this);
    }

    private applyBehaviors()
    {
        if (!this.behaviors) return;

        if (this.behaviors.dismissOnEscape)
        {
            this.overlayRef.keydownEvents()
                .subscribe((event) =>
                           {
                               if (event.code === "Escape") this.dispose();
                           });
        }

        if (this.behaviors.dismissOnBackdrop)
        {
            this.overlayRef.backdropClick()
                .subscribe((event) =>
                           {
                               if (event.button === 0) this.dispose();
                           });
        }

        if (this.behaviors.dismissOnNavigate)
        {
            this.m_service.router.events.subscribe((event) =>
                                                   {
                                                       if (event instanceof NavigationStart) this.dispose();
                                                   });
        }

        if (this.m_isAnimated && this.behaviors?.afterAttachAnimation)
        {
            (<AnimatedPositionStrategy>this.overlayRef.getConfig().positionStrategy).attaching.then(() => this.behaviors.afterAttachAnimation(this));
        }

        if (this.m_isAnimated && this.behaviors?.afterDetachAnimation)
        {
            (<AnimatedPositionStrategy>this.overlayRef.getConfig().positionStrategy).detaching.then(() => this.behaviors.afterDetachAnimation(this));
        }
    }

    private isAnimatedPositionStrategy(strategy: PositionStrategy): boolean
    {
        return "attaching" in strategy && "detaching" in strategy;
    }
}

export interface OverlayBehaviors<T extends ComponentRef<any> | EmbeddedViewRef<any> | any>
{
    /** Automatically call dispose() on the overlay if the "Escape" key is pressed */
    dismissOnEscape?: boolean,
    /** Automatically call dispose() on the overlay if the backdrop area is primary-clicked */
    dismissOnBackdrop?: boolean,
    /** Automatically call dispose() on the overlay if a navigation event happens */
    dismissOnNavigate?: boolean,
    /** Fired immediately after attach() is called on the PositionStrategy of the overlay */
    afterAttach?: (instance?: OverlayInstance<T>) => void,
    /** Fired immediately after attach() is called on the PositionStrategy of the overlay,
     * or if an AnimatedPositionStrategy is being used it is fired after the attach animation is complete */
    afterAttachAnimation?: (instance?: OverlayInstance<T>) => void,
    /** Fired immediately after detach() is called on the PositionStrategy of the overlay */
    afterDetach?: (instance?: OverlayInstance<T>) => void,
    /** Fired immediately after detach() is called on the PositionStrategy of the overlay,
     * or if an AnimatedPositionStrategy is being used it is fired after the detach animation is complete */
    afterDetachAnimation?: (instance?: OverlayInstance<T>) => void,
    /** Fired immediately after dispose() is called on the PositionStrategy of the overlay */
    afterDispose?: (instance?: OverlayInstance<T>) => void
}

export interface AnimatedPositionStrategy extends PositionStrategy
{
    isAttaching: boolean,
    isAttached: boolean,
    attaching: Future<any>,
    isDetaching: boolean,
    isDetached: boolean,
    detaching: Future<any>,
    isDisposing: boolean,
    isDisposed: boolean,
    disposing: Future<any>
}
