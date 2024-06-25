import {OverlayRef, ViewportRuler} from "@angular/cdk/overlay";
import {OverlayReference} from "@angular/cdk/overlay/overlay-reference";
import {ScrollDispatcher} from "@angular/cdk/scrolling";

import {AnimatedPositionStrategy} from "framework/ui/overlays/overlay.service";
import {Future} from "framework/utils/concurrency";

import {Subscription} from "rxjs";

export class MaximizePositionStrategy implements AnimatedPositionStrategy
{
    public isAttaching: boolean   = false;
    public isAttached: boolean    = false;
    public attaching: Future<any> = new Future<any>();
    public isDetaching: boolean   = false;
    public isDetached: boolean    = false;
    public detaching: Future<any> = new Future<any>();
    public isDisposing: boolean   = false;
    public isDisposed: boolean    = false;
    public disposing: Future<any> = new Future<any>();

    private m_reference: OverlayReference;
    private m_overlay: HTMLElement | null;
    private m_backdrop: HTMLElement | null;

    private m_resizeSubscription         = Subscription.EMPTY;
    private m_sourceScrollSubscription   = Subscription.EMPTY;
    private m_maximizeScrollSubscription = Subscription.EMPTY;

    constructor(private viewport: ViewportRuler,
                private scroll: ScrollDispatcher,
                private m_sourceContainer: HTMLElement,
                private m_maximizeContainer: HTMLElement,
                private m_attachTime: number,
                private m_detachTime: number,
                private m_xPercent: number,
                private m_yPercent: number)
    {}

    attach(overlayRef: OverlayRef): void
    {
        // Do nothing if attaching or attached
        if (this.isAttaching || this.isAttached) return;

        // Flag as attaching
        this.isAttaching = true;

        // Initialize state
        this.m_reference = overlayRef;
        this.m_overlay   = overlayRef.hostElement;
        this.m_backdrop  = overlayRef.backdropElement;
        this.m_resizeSubscription.unsubscribe();
        this.m_sourceScrollSubscription.unsubscribe();
        this.m_maximizeScrollSubscription.unsubscribe();

        // Make the backdrop fit the maximize container
        this.mirrorElementScaled(this.m_maximizeContainer, this.m_backdrop);
        // Make the overlay fit the source container
        this.mirrorElementScaled(this.m_sourceContainer, this.m_overlay);

        // Make the overlay content fit the overlay container
        this.m_reference.overlayElement.style.width  = "100%";
        this.m_reference.overlayElement.style.height = "100%";

        // Set up overlay transition
        this.m_overlay.style.position   = "absolute";
        this.m_overlay.style.transition = `top ease ${this.m_attachTime}ms, left ease ${this.m_attachTime}ms, width ease ${this.m_attachTime}ms, height ease ${this.m_attachTime}ms`;

        // Defer to next animation frame to allow DOM to settle
        requestAnimationFrame(() =>
                              {
                                  // Set up scroll and resize watchers
                                  this.m_resizeSubscription       = this.viewport.change(0)
                                                                        .subscribe(() => this.apply());
                                  this.m_sourceScrollSubscription = this.scroll.ancestorScrolled(this.m_sourceContainer, 0)
                                                                        .subscribe(() => this.apply());

                                  this.m_maximizeScrollSubscription = this.scroll.ancestorScrolled(this.m_maximizeContainer, 0)
                                                                          .subscribe(() => this.apply());
                                  // Apply the current maximize state
                                  this.apply();

                                  // Wait for animation to finish
                                  setTimeout(() =>
                                             {
                                                 // Resolve attaching future and update flags
                                                 this.isAttaching = false;
                                                 this.isAttached  = true;
                                                 this.attaching.resolve();

                                                 // Remove transition style
                                                 this.m_overlay.style.transition = null;
                                             }, this.m_attachTime);
                              });
    }

    apply()
    {
        // Update backdrop and overlay container sizes
        this.mirrorElementScaled(this.m_maximizeContainer, this.m_backdrop);
        this.mirrorElementScaled(this.m_maximizeContainer, this.m_overlay, this.m_xPercent, this.m_yPercent);

        // Ensure overlay content is still filling the container
        this.m_reference.overlayElement.style.width  = "100%";
        this.m_reference.overlayElement.style.height = "100%";
    }

    async detach()
    {
        // Do nothing if detaching or detached
        if (this.isDetaching || this.isDetached) return;

        // Flag as detaching
        this.isDetaching = true;

        // If not done attaching, wait for attach to finish
        await this.attaching;

        // Set up overlay transition
        this.m_overlay.style.position   = "absolute";
        this.m_overlay.style.transition = `top ease ${this.m_detachTime}ms, left ease ${this.m_detachTime}ms, width ease ${this.m_detachTime}ms, height ease ${this.m_detachTime}ms`;

        // Stop listening for scroll and resize events
        this.m_resizeSubscription.unsubscribe();
        this.m_sourceScrollSubscription.unsubscribe();
        this.m_maximizeScrollSubscription.unsubscribe();

        // Defer to next animation frame to allow DOM to settle
        requestAnimationFrame(() =>
                              {
                                  // Make the overlay fit back to the source container
                                  this.mirrorElementScaled(this.m_sourceContainer, this.m_overlay);

                                  // Wait for animation to finish
                                  setTimeout(() =>
                                             {
                                                 // Resolve detaching future and update flags
                                                 this.isDetaching = false;
                                                 this.isDetached  = true;
                                                 this.detaching.resolve();

                                                 // Remove transition style
                                                 this.m_overlay.style.transition = null;
                                             }, this.m_detachTime);
                              });
    }

    async dispose()
    {
        // Do nothing if disposing or disposed
        if (this.isDisposing || this.isDisposed) return;

        // If we are not detached or detaching, start that process
        if (!this.isDetaching && !this.isDetached) this.detach();

        // Mark as disposing
        this.isDisposing = true;

        // Wait for detachment to complete
        await this.detaching;

        // Resolve disposing future and update flags
        this.isDisposing = false;
        this.isDisposed  = true;
        this.disposing.resolve();
    }

    private mirrorElementScaled(input: HTMLElement,
                                output: HTMLElement,
                                scaleX: number = 1.0,
                                scaleY: number = 1.0)
    {
        if (!input || !output) return;
        let rect            = input.getBoundingClientRect();
        let dx              = (1 - scaleX) * rect.width;
        let dy              = (1 - scaleY) * rect.height;
        output.style.top    = `${rect.top + (dy / 2)}px`;
        output.style.left   = `${rect.left + (dx / 2)}px`;
        output.style.width  = `${rect.width - dx}px`;
        output.style.height = `${rect.height - dy}px`;
    }
}
