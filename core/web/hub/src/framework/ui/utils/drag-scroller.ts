import {CdkScrollable} from "@angular/cdk/overlay";

import {Subscription} from "rxjs";

export class DragScroller
{
    public static readonly scrollZoneHeightPercent: number = 20;
    public static readonly scrollInterval: number          = 20;

    protected static readonly scrollZoneMinHeight           = 40;
    protected static readonly maxPxScrollPerSec: number     = 1600;
    protected static readonly resizingScrollDivisor: number = 6;

    protected static readonly virtualScrollingBuffer      = 300;
    protected static readonly virtualScrollingUpdateDelta = 100;

    //--//

    private readonly m_scrollSub: Subscription;
    protected readonly m_scrollElement: HTMLElement;

    // virtual scrolling info
    protected m_prevScrollTop: number;
    protected m_renderBounds: ScrollerRenderBounds;
    public get renderBounds(): ScrollerRenderBounds
    {
        return this.m_renderBounds;
    }

    // state info
    protected m_state: ScrollState;
    protected m_scrollSpeedMultiplier: number;
    protected m_scroller: number;
    protected m_cursorY: number;
    protected m_startScrollStop: number;
    protected m_scrollWindow: DOMRect;

    // hover scroll bounds
    protected m_topScrollThreshold: number;
    protected m_botScrollThreshold: number;

    bannerShowing = false;

    get scrollSpeed(): number
    {
        let multiplier = this.config.speedMultiplier * (this.m_scrollSpeedMultiplier || 0.3);
        return multiplier * DragScroller.scrollInterval * DragScroller.maxPxScrollPerSec / 1000;
    }

    get scrollTop(): number
    {
        return this.m_scrollElement?.scrollTop || 0;
    }

    get scrollHeight(): number
    {
        return this.m_scrollElement?.scrollHeight || 0;
    }

    constructor(public readonly config: ScrollerConfig)
    {
        this.m_scrollElement = this.config.scrollElement.getElementRef().nativeElement;
        this.m_scrollSub     = this.config.scrollElement.elementScrolled()
                                   .subscribe(() => this.processScroll(true));
        this.updateScrollWindow();
    }

    public setUpForScroll()
    {
        this.updateScrollWindow();
        this.m_startScrollStop    = this.m_scrollElement.scrollTop || 0;
        let scrollZoneHeight      = Math.max(this.m_scrollWindow.height * DragScroller.scrollZoneHeightPercent / 100, DragScroller.scrollZoneMinHeight);
        this.m_topScrollThreshold = this.m_scrollWindow.top + scrollZoneHeight;
        this.m_botScrollThreshold = this.m_scrollWindow.bottom - scrollZoneHeight;
    }

    private updateScrollWindow()
    {
        this.m_scrollWindow = this.m_scrollElement.getBoundingClientRect();
    }

    public updateViewport(detectChanges: boolean)
    {
        this.processScroll(detectChanges);
    }

    protected async processScroll(detectChanges: boolean)
    {}

    public updateState(scrollState: ScrollState)
    {
        scrollState = scrollState || ScrollState.Stationary;

        let scrollUpdateFn: () => void;
        if (scrollState === ScrollState.Up)
        {
            let windowTop                = this.m_scrollWindow.top;
            this.m_scrollSpeedMultiplier = 1 - (this.m_cursorY - windowTop) / (this.m_topScrollThreshold - windowTop);

            scrollUpdateFn = () => this.m_scrollElement.scrollTop = Math.max(0, this.m_scrollElement.scrollTop - this.scrollSpeed);
        }
        else if (scrollState === ScrollState.Down)
        {
            let windowBottom             = this.m_scrollWindow.bottom;
            this.m_scrollSpeedMultiplier = 1 - (windowBottom - this.m_cursorY) / (windowBottom - this.m_botScrollThreshold);

            scrollUpdateFn = () => this.m_scrollElement.scrollTop = Math.min(this.m_scrollElement.scrollHeight - this.m_scrollWindow.height,
                                                                             this.m_scrollElement.scrollTop + this.scrollSpeed);
        }
        else if (this.m_scroller) // ScrollState.Stationary: only necessary to do anything if there's a scroller to clear
        {
            clearInterval(this.m_scroller);
            this.m_scroller              = undefined;
            this.m_scrollSpeedMultiplier = undefined;
        }

        if (this.m_state !== scrollState)
        {
            this.m_state = scrollState;
            if (scrollUpdateFn) this.m_scroller = setInterval(scrollUpdateFn, DragScroller.scrollInterval);
        }

        this.stateUpdated();
    }

    protected stateUpdated()
    {}

    public scrollToTop()
    {
        if (this.m_scrollElement) this.m_scrollElement.scrollTop = 0;
    }

    protected getNewScrollState(): ScrollState
    {
        if (this.m_cursorY <= this.m_topScrollThreshold)
        {
            return ScrollState.Up;
        }
        else if (this.m_cursorY >= this.m_botScrollThreshold)
        {
            return ScrollState.Down;
        }

        return ScrollState.Stationary;
    }

    public processState(cursorY: number): number
    {
        this.m_cursorY = cursorY;
        this.updateState(this.getNewScrollState());

        return this.m_scrollElement.scrollTop - this.m_startScrollStop;
    }

    public destroy()
    {
        this.m_scrollSub.unsubscribe();
        if (this.m_scroller) clearInterval(this.m_scroller);
    }
}

export class ScrollerConfig
{
    constructor(public readonly speedMultiplier: number,
                public readonly scrollElement: CdkScrollable)
    {
    }
}

export class ScrollerRenderBounds
{
    constructor(public readonly low: number,
                public readonly high: number)
    {
    }
}

export enum ScrollState
{
    Up         = "Up",
    Down       = "Down",
    Stationary = "Stationary"
}
