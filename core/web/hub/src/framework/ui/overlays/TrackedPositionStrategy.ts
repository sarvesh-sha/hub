import {OverlayRef, PositionStrategy, ViewportRuler} from "@angular/cdk/overlay";
import {OverlayReference} from "@angular/cdk/overlay/overlay-reference";
import {ScrollDispatcher} from "@angular/cdk/scrolling";

import {Subscription} from "rxjs";

export class TrackedPositionStrategy implements PositionStrategy
{
    public isAttached: boolean = false;
    public isDetached: boolean = false;
    public isDisposed: boolean = false;

    private m_reference: OverlayReference;
    private m_overlay: HTMLElement | null;
    private m_backdrop: HTMLElement | null;

    private m_resizeSubscription         = Subscription.EMPTY;
    private m_sourceScrollSubscription   = Subscription.EMPTY;
    private m_boundingScrollSubscription = Subscription.EMPTY;

    constructor(private viewport: ViewportRuler,
                private scroll: ScrollDispatcher,
                private m_sourceContainer: HTMLElement,
                private m_boundingContainer: HTMLElement,
                private m_collisionShape: CollisionShape)
    {}

    attach(overlayRef: OverlayRef): void
    {
        // Do nothing if attached
        if (this.isAttached) return;

        // Flag as attached
        this.isAttached = true;

        // Initialize state
        this.m_reference = overlayRef;
        this.m_overlay   = overlayRef.hostElement;
        this.m_backdrop  = overlayRef.backdropElement;
        this.m_resizeSubscription.unsubscribe();
        this.m_sourceScrollSubscription.unsubscribe();
        this.m_boundingScrollSubscription.unsubscribe();

        // Make the overlay fit the source container
        this.mirrorElementScaled(this.m_sourceContainer, this.m_overlay);

        // Make the overlay content fit the overlay container
        this.m_reference.overlayElement.style.width  = "100%";
        this.m_reference.overlayElement.style.height = "100%";

        // Set up overlay transition
        this.m_overlay.style.position = "absolute";

        // Set up scroll and resize watchers
        this.m_resizeSubscription = this.viewport.change(0)
                                        .subscribe(() => this.apply());

        this.m_sourceScrollSubscription = this.scroll.ancestorScrolled(this.m_sourceContainer, 0)
                                              .subscribe(() => this.apply());

        if (this.m_boundingContainer)
        {
            this.m_boundingScrollSubscription = this.scroll.ancestorScrolled(this.m_boundingContainer, 0)
                                                    .subscribe(() => this.apply());

        }


        // Apply the current maximize state
        this.apply();
    }

    apply()
    {
        // Update overlay container size and position
        this.mirrorElementScaled(this.m_sourceContainer, this.m_overlay);

        // Ensure overlay content is still filling the container
        this.m_reference.overlayElement.style.width  = "100%";
        this.m_reference.overlayElement.style.height = "100%";

        // If not fully inside bounds, auto dispose
        if (this.m_boundingContainer && !this.isFullyInsideBounds(this.m_boundingContainer, this.m_sourceContainer, this.m_collisionShape)) this.m_reference.detach();
    }

    detach()
    {
        // Do nothing if detached
        if (this.isDetached) return;

        // Flag as detached
        this.isDetached = true;

        // Set up overlay transition
        this.m_overlay.style.position = "absolute";

        // Stop listening for scroll and resize events
        this.m_resizeSubscription.unsubscribe();
        this.m_sourceScrollSubscription.unsubscribe();
        if (this.m_boundingScrollSubscription)
        {
            this.m_boundingScrollSubscription.unsubscribe();
        }

        // Make the overlay fit back to the source container
        this.mirrorElementScaled(this.m_sourceContainer, this.m_overlay);
    }

    dispose()
    {
        // Do nothing if disposed
        if (this.isDisposed) return;

        // Detach if not detached already
        if (!this.isDetached) this.detach();

        // Mark as disposed
        this.isDisposed = true;
    }

    private mirrorElementScaled(input: HTMLElement,
                                output: HTMLElement)
    {
        if (!input || !output) return;
        let rect            = input.getBoundingClientRect();
        output.style.top    = `${rect.top}px`;
        output.style.left   = `${rect.left}px`;
        output.style.width  = `${rect.width}px`;
        output.style.height = `${rect.height}px`;
    }

    private isFullyInsideBounds(bounds: HTMLElement,
                                source: HTMLElement,
                                collision: CollisionShape)
    {
        let bRect = bounds.getBoundingClientRect();
        let sRect = source.getBoundingClientRect();

        switch (collision)
        {
            case CollisionShape.FullArea:
                return sRect.top >= bRect.top && sRect.left >= bRect.left && sRect.bottom <= bRect.bottom && sRect.right <= bRect.right;

            case CollisionShape.TopEdge:
                return sRect.top >= bRect.top && sRect.top <= bRect.bottom && sRect.left >= bRect.left && sRect.right <= bRect.right;

            case CollisionShape.BottomEdge:
                return sRect.bottom >= bRect.top && sRect.bottom <= bRect.bottom && sRect.left >= bRect.left && sRect.right <= bRect.right;

            case CollisionShape.LeftEdge:
                return sRect.left >= bRect.left && sRect.left <= bRect.right && sRect.top >= bRect.top && sRect.bottom <= bRect.bottom;

            case CollisionShape.RightEdge:
                return sRect.right >= bRect.left && sRect.right <= bRect.right && sRect.top >= bRect.top && sRect.bottom <= bRect.bottom;
        }

        return false;
    }
}

export enum CollisionShape
{
    FullArea   = "FullArea",
    TopEdge    = "TopEdge",
    BottomEdge = "BottomEdge",
    LeftEdge   = "LeftEdge",
    RightEdge  = "RightEdge"
}
