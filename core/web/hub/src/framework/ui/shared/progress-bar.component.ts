import {Component, ElementRef, Input} from "@angular/core";

import {UtilsService} from "framework/services/utils.service";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";

@Component({
               selector   : "o3-progress-bar",
               templateUrl: "./progress-bar.component.html",
               styleUrls  : ["./progress-bar.component.scss"]
           })
export class ProgressBarComponent
{
    private static readonly WIDE_WIDTH_THRESHOLD = 100;

    private m_percent: number = 0;
    @Input() set percent(percent: number)
    {
        this.m_percent = Math.round(UtilsService.clamp(0, 100, percent || 0));
    }

    get percent(): number
    {
        return this.m_percent;
    }

    @Input() context: string;

    private m_colorFn: (percent: number) => string;
    @Input() set colorFn(fn: (percent: number) => string)
    {
        this.m_colorFn = fn;
    }

    get colorFn(): (percent: number) => string
    {
        return this.m_colorFn || ChartColorUtilities.progressColorFn;
    }

    get widthPercent(): number
    {
        return this.percent || 100;
    }

    get disableTooltip(): boolean
    {
        return this.m_elem.nativeElement.clientWidth > ProgressBarComponent.WIDE_WIDTH_THRESHOLD;
    }

    constructor(private readonly m_elem: ElementRef)
    {}
}
