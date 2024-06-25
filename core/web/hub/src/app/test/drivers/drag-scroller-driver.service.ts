import {CdkScrollable} from "@angular/cdk/overlay";
import {ElementRef, Injectable} from "@angular/core";

import {getCenterPoint, TestDriver, waitFor} from "app/test/driver";
import {Point} from "app/test/events";

import {DragScroller} from "framework/ui/utils/drag-scroller";

@Injectable({providedIn: "root"})
export class DragScrollerDriver
{
    constructor(private m_driver: TestDriver)
    {
    }

    async dragScroll(scrollContainer: CdkScrollable,
                     drag: ElementRef<HTMLElement> | HTMLElement,
                     target: ElementRef<HTMLElement> | HTMLElement,
                     dragElementName: string,
                     targetName: string,
                     canReleaseFn: () => boolean = () => true,
                     hoverDurationMs: number     = 2500)
    {
        if (drag instanceof ElementRef) drag = drag.nativeElement;
        if (target instanceof ElementRef) target = target.nativeElement;

        const containerElem = scrollContainer.getElementRef().nativeElement;
        const containerRect = containerElem.getBoundingClientRect();

        const scrollZoneHeight = containerRect.height * DragScroller.scrollZoneHeightPercent / 100;
        let targetCenter       = getCenterPoint(target);
        const goingUp          = targetCenter.y < containerRect.top + scrollZoneHeight;
        if (this.hoverTargetAvailable(containerElem, containerRect, goingUp, target))
        {
            await this.m_driver.clickAndDrag(drag, target, dragElementName);
        }
        else
        {
            const hoverPoint: Point = {
                x: containerRect.left + containerRect.width / 2,
                y: containerRect.bottom - scrollZoneHeight / 2
            };
            if (goingUp) hoverPoint.y = containerRect.top + scrollZoneHeight / 2;
            await this.m_driver.clickAndDrag(drag, hoverPoint, dragElementName, true);
            await this.m_driver.jitterMouseUntil(async () =>
                                                 {
                                                     await waitFor(() => this.hoverTargetAvailable(containerElem, containerRect, goingUp, <HTMLElement>target),
                                                                   `${targetName} did not come into view`,
                                                                   DragScroller.scrollInterval, hoverDurationMs / DragScroller.scrollInterval);
                                                 }, hoverPoint.x, hoverPoint.y, DragScroller.scrollInterval);

            targetCenter = getCenterPoint(target);
            await this.m_driver.hover(target, targetName);
            await this.m_driver.jitterMouseUntil(async () =>
                                                 {
                                                     if (await waitFor(canReleaseFn, "canReleaseFn condition never resolved", 100, 9))
                                                     {
                                                         await this.m_driver.click(target, targetName);
                                                     }
                                                 }, targetCenter.x, targetCenter.y, DragScroller.scrollInterval);
        }
    }

    private hoverTargetAvailable(containerElem: HTMLElement,
                                 containerRect: DOMRect,
                                 goingUp: boolean,
                                 targetElem: HTMLElement)
    {
        const inView                = this.inView(containerRect, getCenterPoint(targetElem));
        const cannotScrollAnymore   = this.cannotScrollAnymore(containerElem, goingUp);
        const targetOutOfScrollZone = this.outOfScrollZone(containerRect, goingUp, targetElem);
        return inView && (cannotScrollAnymore || targetOutOfScrollZone);
    }

    private inView(containerRect: DOMRect,
                   target: Point): boolean
    {
        return target.y > containerRect.top && target.y < containerRect.bottom;
    }

    private cannotScrollAnymore(containerElem: HTMLElement,
                                goingUp: boolean): boolean
    {
        return goingUp ? containerElem.scrollTop === 0 : containerElem.scrollTop + containerElem.clientHeight === containerElem.scrollHeight;
    }

    private outOfScrollZone(containerRect: DOMRect,
                            goingUp: boolean,
                            elem: HTMLElement): boolean
    {
        const centerPoint      = getCenterPoint(elem);
        const scrollZoneHeight = containerRect.height * DragScroller.scrollZoneHeightPercent / 100;
        return goingUp ? centerPoint.y > containerRect.top + scrollZoneHeight : centerPoint.y < containerRect.bottom - scrollZoneHeight;
    }
}
