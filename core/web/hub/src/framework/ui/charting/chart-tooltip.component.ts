import {BreakpointObserver} from "@angular/cdk/layout";
import {ConnectionPositionPair, Overlay, OverlayConfig, OverlayRef, PositionStrategy} from "@angular/cdk/overlay";
import {OriginConnectionPosition} from "@angular/cdk/overlay/position/connected-position";
import {CdkPortal} from "@angular/cdk/portal";
import {Component, Injector, Input, ViewChild} from "@angular/core";
import {SafeHtml} from "@angular/platform-browser";

import {BaseComponent} from "framework/ui/components";
import {LAYOUT_WIDTH_SM} from "framework/ui/layout";
import {RelativeLocation} from "framework/ui/utils/relative-location-styles";

import {Subscription} from "rxjs";

@Component({
               selector   : "o3-chart-tooltip",
               templateUrl: "./chart-tooltip.component.html"
           })
export class ChartTooltipComponent extends BaseComponent
{
    private static readonly tooltipOffset: number = 5;

    private m_xOffset: number = 0;
    private m_isMobile: boolean;
    get isMobile(): boolean
    {
        return this.m_isMobile;
    }

    set isMobile(isMobile: boolean)
    {
        this.remove();
        this.m_isMobile = isMobile;
    }

    get leftOffset(): string
    {
        if (this.m_isMobile)
        {
            return `calc(50% - ${this.m_xOffset}px)`;
        }
        return null;
    }

    private m_breakpointSub: Subscription;
    private m_mobilePositions = [
        RelativeLocation.Top,
        RelativeLocation.Bottom
    ];

    @Input() targetElement: HTMLElement    = null;
    @Input() positions: RelativeLocation[] = [
        RelativeLocation.Right,
        RelativeLocation.Left
    ];

    @ViewChild("tooltipTemplate", {static: true}) tooltipTemplate: CdkPortal;

    tooltipHtml: SafeHtml = null;
    tooltipText: string   = null;

    private m_disconnected: boolean = false;

    private tooltip: OverlayRef = null;

    constructor(inj: Injector,
                public overlay: Overlay,
                breakpointObserver: BreakpointObserver)
    {
        super(inj);
        this.m_breakpointSub = breakpointObserver.observe(`(min-width: ${LAYOUT_WIDTH_SM}px)`)
                                                 .subscribe((handsetBreakpoint) => this.isMobile = !handsetBreakpoint.matches);
    }

    ngOnDestroy()
    {
        super.ngOnDestroy();

        this.remove();
        this.m_breakpointSub.unsubscribe();
        this.m_disconnected = true;
    }

    render(x: number,
           y: number,
           htmlValue: SafeHtml = "",
           textValue: string   = "")
    {
        if (this.m_disconnected) return;

        // Remove any existing tooltip
        this.remove();

        // Create tooltip overlay
        let config              = new OverlayConfig();
        config.positionStrategy = this.getPositionStrategy(x, y, 0);
        config.scrollStrategy   = this.overlay.scrollStrategies.close();
        this.tooltip            = this.overlay.create(config);

        // Update tooltip content before attaching and setting up tooltip
        this.tooltipHtml = htmlValue;
        this.tooltipText = textValue;
        this.detectChanges();

        this.renderTemplate(x, y);
    }

    renderTemplate(x: number,
                   y: number)
    {
        this.tooltip.attach(this.tooltipTemplate);
        this.tooltip.updatePosition();

        if (this.m_isMobile)
        {
            let trueX              = this.targetElement ? this.targetElement.getBoundingClientRect().x + x : x;
            let tooltipWidthHalved = this.tooltip.overlayElement.clientWidth / 2;
            let left               = trueX - tooltipWidthHalved;
            let right              = trueX + tooltipWidthHalved;

            this.m_xOffset = 0;
            if (left < 0)
            {
                this.m_xOffset = -left;
            }
            else if (right > window.innerWidth)
            {
                this.m_xOffset = window.innerWidth - right;
            }

            if (this.m_xOffset) this.tooltip.updatePositionStrategy(this.getPositionStrategy(x, y, this.m_xOffset));
        }
    }

    remove()
    {
        if (this.tooltip)
        {
            this.tooltip.dispose();
            this.tooltip     = null;
            this.tooltipText = null;
            this.tooltipHtml = null;
        }
    }

    private getPositionStrategy(x: number,
                                y: number,
                                xOffset: number): PositionStrategy
    {
        return this.overlay.position()
                   .flexibleConnectedTo(this.targetElement)
                   .withPositions(this.getPositionOptions(x, y, xOffset))
                   .withFlexibleDimensions(false)
                   .withPush(false);
    }

    private getPositionOptions(x: number,
                               y: number,
                               xOffset: number): ConnectionPositionPair[]
    {
        let positions                        = this.m_isMobile ? this.m_mobilePositions : this.positions;
        let origin: OriginConnectionPosition = {
            originX: "start",
            originY: "top"
        };
        return positions.map((position) =>
                             {
                                 switch (position)
                                 {
                                     case RelativeLocation.Bottom:
                                         return new ConnectionPositionPair(origin, {
                                             overlayX: "center",
                                             overlayY: "top"
                                         }, x + xOffset, y + ChartTooltipComponent.tooltipOffset, [
                                                                               "chart-tooltip-container",
                                                                               "bottom"
                                                                           ]);

                                     case RelativeLocation.Top:
                                         return new ConnectionPositionPair(origin, {
                                             overlayX: "center",
                                             overlayY: "bottom"
                                         }, x + xOffset, y - ChartTooltipComponent.tooltipOffset, [
                                                                               "chart-tooltip-container",
                                                                               "top"
                                                                           ]);

                                     case RelativeLocation.Left:
                                         return new ConnectionPositionPair(origin, {
                                             overlayX: "end",
                                             overlayY: "center"
                                         }, x - ChartTooltipComponent.tooltipOffset, y, [
                                                                               "chart-tooltip-container",
                                                                               "left"
                                                                           ]);

                                     default:
                                         return new ConnectionPositionPair(origin, {
                                             overlayX: "start",
                                             overlayY: "center"
                                         }, x + ChartTooltipComponent.tooltipOffset, y, [
                                                                               "chart-tooltip-container",
                                                                               "right"
                                                                           ]);
                                 }
                             });
    }
}

