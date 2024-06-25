import {ScrollDispatcher, ViewportRuler} from "@angular/cdk/scrolling";
import {ChangeDetectionStrategy, Component, ElementRef, Injector, Input, ViewChild} from "@angular/core";

import {UtilsService} from "framework/services/utils.service";
import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {BaseComponent} from "framework/ui/components";
import {Debouncer} from "framework/utils/debouncers";

@Component({
               selector       : "o3-skeleton-screen",
               templateUrl    : "./skeleton-screen.component.html",
               styleUrls      : ["./skeleton-screen.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class SkeletonScreenComponent extends BaseComponent
{
    @Input() loading: boolean = true;
    @Input() inView: boolean  = true;

    @Input() horizontalBounce: boolean = false;
    @Input() fadeInOut: boolean        = false;

    private m_maxHeightPercent: number = 70;
    @Input() set maxHeightPercent(pctg: number)
    {
        this.m_maxHeightPercent = UtilsService.clamp(1, 100, pctg || this.m_maxHeightPercent);
    }

    get maxHeightPercent(): number
    {
        return this.m_maxHeightPercent;
    }

    @Input() set numRows(num: number)
    {
        this.rowFrames = new Array(Math.max(1, num || 0));

        // let the next change detection cycle occur and the parent elem's height be established
        setTimeout(() => this.checkView());
    }

    private m_viewWindow: VerticalViewWindow;
    @Input() set viewWindow(viewWindow: VerticalViewWindow)
    {
        this.m_viewWindow = viewWindow;
        this.m_viewThrottler.invoke();
    }

    @ViewChild("rowsContainer",
               {
                   static: true,
                   read  : ElementRef
               }) rowsContainer: ElementRef;

    private m_viewThrottler = new Debouncer(40, async () => this.checkView());

    private m_inViewLow: number;
    private m_inViewHigh: number;

    get multipleRows(): boolean
    {
        return this.rowFrames.length > 1;
    }

    get animationClass(): string
    {
        if (this.horizontalBounce) return "o3-skeleton-screen--horizontal-bounce";
        if (this.fadeInOut) return "o3-skeleton-screen--fading";

        return null;
    }

    rowFrames: void[] = new Array(1);

    constructor(inj: Injector,
                private viewport: ViewportRuler,
                private scrolling: ScrollDispatcher)
    {
        super(inj);

        // Subscribe to viewport change events
        this.subscribeToObservableUntil(this.viewport.change(0), () => this.m_viewThrottler.invoke(), () => !!this.m_viewWindow);
        this.subscribeToObservableUntil(this.scrolling.scrolled(0), () => this.m_viewThrottler.invoke(), () => !!this.m_viewWindow);
    }

    rowInView(rowIdx: number)
    {
        if (!this.multipleRows) return this.inView;
        if (isNaN(this.m_inViewLow)) return false;
        if (isNaN(this.m_inViewHigh)) return false;
        return rowIdx >= this.m_inViewLow && rowIdx < this.m_inViewHigh;
    }

    private checkView()
    {
        if (this.multipleRows)
        {
            const viewPadding = 150;
            const rowsRect    = this.rowsContainer.nativeElement.getBoundingClientRect();

            let viewTop: number;
            let viewBottom: number;
            if (this.m_viewWindow)
            {
                viewTop    = this.m_viewWindow.viewTop;
                viewBottom = this.m_viewWindow.viewTop + this.m_viewWindow.viewHeight;
            }
            else
            {
                const viewportRect = this.viewport.getViewportRect();
                viewBottom         = viewportRect.height - rowsRect.top + viewPadding;
                viewTop            = viewBottom - viewportRect.height - 2 * viewPadding;
            }

            const rowsPerPx   = this.rowFrames.length / rowsRect.height;
            this.m_inViewLow  = Math.floor(viewTop * rowsPerPx);
            this.m_inViewHigh = Math.ceil(viewBottom * rowsPerPx);
            this.markForCheck();
        }
    }
}
