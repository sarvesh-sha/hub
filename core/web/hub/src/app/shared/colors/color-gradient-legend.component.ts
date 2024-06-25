import {Component, ElementRef, Injector, Input, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";

import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ColorGradientStop, ColorMapper, ColorSegmentBackground, ColorSegmentInterpolationMode} from "framework/ui/charting/core/colors";
import {ChartFont} from "framework/ui/charting/core/text";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-color-gradient-legend[colorMapper]",
               templateUrl: "./color-gradient-legend.component.html"
           })
export class ColorGradientLegendComponent extends SharedSvc.BaseApplicationComponent
{
    private static readonly labelHorizontalPadding = 4;

    legendSegments: LegendSegment[] = [];

    @Input() set colorMapper(mapper: ColorMapper)
    {
        let stops = mapper?.colorStops;
        if (!stops?.length) return;

        this.legendSegments = [];
        for (let i = 1; i < stops.length; i++) this.legendSegments.push(new LegendSegment(stops[i - 1], stops[i], mapper.interpolationMode));

        this.checkWidth(true);
    }

    private m_legendPurpose: string = "";
    @Input() set legendPurpose(purpose: string)
    {
        this.m_legendPurpose = purpose ? purpose + " legend" : "";
    }

    get legendPurpose(): string
    {
        return this.m_legendPurpose;
    }

    get containerTooltip(): string
    {
        if (this.extraPxRequired > 0)
        {
            let prefix = this.overlay.isOpen ? "Hide " : "Show ";
            return prefix + "the " + this.legendPurpose;
        }
        else if (this.extraPxRequired === 0)
        {
            return this.legendPurpose;
        }
        else
        {
            return "";
        }
    }

    get usingOverlay(): boolean
    {
        return !!this.extraPxRequired;
    }

    @ViewChild(OverlayComponent, {static: true}) overlay: OverlayComponent;

    private width: number;
    private extraPxRequired: number;
    private textMeasurer: ChartHelpers = new ChartHelpers();
    private textFont: ChartFont        = new ChartFont();

    overlayConfig: OverlayConfig;

    constructor(inj: Injector,
                private element: ElementRef)
    {
        super(inj);
        this.textFont.size = 14;
    }

    public ngAfterViewInit()
    {
        super.ngAfterViewInit();
        this.checkWidth();
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();
        this.checkWidth();
    }

    refreshSize()
    {
        this.checkWidth();
    }

    toggleOverlay()
    {
        if (this.extraPxRequired) this.overlay.toggleOverlay();
    }

    private checkWidth(override: boolean = false)
    {
        if (!this.legendSegments.length) return;

        let width = this.element.nativeElement.clientWidth || 0;
        if (override || this.width !== width)
        {
            this.width           = width;
            this.extraPxRequired = 0;

            const horizontalPadding = 8;
            let segmentLength       = (this.width - horizontalPadding) / this.legendSegments.length;

            let prevOffset = 2 * ColorGradientLegendComponent.labelHorizontalPadding + this.measureText(this.legendSegments[0].startValue);
            for (let segment of this.legendSegments)
            {
                let halfWidth        = ColorGradientLegendComponent.labelHorizontalPadding + this.measureText(segment.endValue) / 2;
                this.extraPxRequired = Math.max(this.extraPxRequired, prevOffset + halfWidth - segmentLength);
                prevOffset           = halfWidth;
            }

            let lastLabelOffset  = 2 * ColorGradientLegendComponent.labelHorizontalPadding + this.measureText(this.legendSegments[this.legendSegments.length - 1].endValue);
            this.extraPxRequired = Math.max(this.extraPxRequired, prevOffset + lastLabelOffset - segmentLength);

            if (this.extraPxRequired)
            {
                this.overlayConfig = OverlayConfig.onTopDraggable({
                                                                      width : 1.5 * (this.width + this.extraPxRequired * this.legendSegments.length),
                                                                      height: 89
                                                                  });
                this.detectChanges();
            }
        }
    }

    private measureText(text: string): number
    {
        return this.textMeasurer.measureText(this.textFont, text);
    }
}

export class LegendSegment
{
    public readonly backgroundCss: string;
    public readonly startValue: string;
    public readonly endValue: string;

    constructor(start: ColorGradientStop,
                end: ColorGradientStop,
                interpolationMode: ColorSegmentInterpolationMode)
    {
        this.backgroundCss = new ColorSegmentBackground(start.color, end.color, interpolationMode).background;
        this.startValue    = start.value.toLocaleString(undefined, {maximumFractionDigits: 2});
        this.endValue      = end.value.toLocaleString(undefined, {maximumFractionDigits: 2});
    }
}
