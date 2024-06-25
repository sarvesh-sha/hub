import {WidgetManipulator} from "app/dashboard/dashboard/widgets/widget-manipulator";

import {DragScroller, ScrollerConfig, ScrollerRenderBounds, ScrollState} from "framework/ui/utils/drag-scroller";
import {Future} from "framework/utils/concurrency";

export class WidgetManagerScroller extends DragScroller
{
    private m_previouslyColumnar: boolean;

    constructor(config: ScrollerConfig,
                private readonly m_widgetManipulator: WidgetManipulator)
    {
        super(config);
    }

    protected stateUpdated()
    {
        switch (this.m_state)
        {
            case ScrollState.Up:
            case ScrollState.Down:
                if (this.m_widgetManipulator.resizing) this.m_scrollSpeedMultiplier /= DragScroller.resizingScrollDivisor;
                break;
        }
    }

    protected async processScroll(detectChanges: boolean)
    {
        this.m_widgetManipulator.clearDomRects();

        while (true)
        {
            // Yield processor.
            await Future.delayed(0);

            if (this.m_widgetManipulator.destroyed) break;

            this.updateViewportFreshness();

            let somethingChanged = false;
            for (let outline of this.m_widgetManipulator.widgetOutlineExts)
            {
                if (outline.updateViewable())
                {
                    somethingChanged = true;
                }
            }

            if (somethingChanged && detectChanges)
            {
                this.m_widgetManipulator.markForCheck();
                detectChanges = false;
                continue;
            }

            break;
        }
    }

    private updateViewportFreshness()
    {
        const currTop = this.scrollTop;
        if (!this.m_renderBounds ||
            this.m_widgetManipulator.columnar !== this.m_previouslyColumnar ||
            Math.abs(currTop - this.m_prevScrollTop) >= DragScroller.virtualScrollingUpdateDelta)
        {
            this.m_prevScrollTop      = currTop;
            this.m_previouslyColumnar = this.m_widgetManipulator.columnar;
            this.m_renderBounds       = new ScrollerRenderBounds(Math.floor(currTop) - DragScroller.virtualScrollingBuffer,
                                                                 Math.ceil(currTop + this.m_scrollElement.clientHeight) + DragScroller.virtualScrollingBuffer);
        }
    }
}
