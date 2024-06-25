import {ChangeDetectionStrategy, Component, ElementRef, Injector, Input} from "@angular/core";
import {UtilsService} from "framework/services/utils.service";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {BaseComponent} from "framework/ui/components";

@Component({
               selector       : "o3-progress-icon",
               templateUrl    : "./progress-icon.component.html",
               styleUrls      : ["./progress-icon.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ProgressIconComponent extends BaseComponent
{
    private containerSize: number;

    @Input() public percent: number;
    @Input() public icon: string;
    @Input() size: number                         = null;
    @Input() progressBarWidth: number             = 3;
    @Input() colorFn: (percent: number) => string = ChartColorUtilities.progressColorFn;

    constructor(injector: Injector,
                private readonly m_elem: ElementRef)
    {
        super(injector);
    }

    public get displaySize(): number
    {
        return Math.max(this.size || this.containerSize, 16);
    }

    public get safePercent(): number
    {
        return UtilsService.clamp(0, 100, this.percent);
    }

    public get formattedPercent(): string
    {
        return `${Math.round(this.safePercent)}%`;
    }

    public get fontSize(): number
    {
        return Math.min(12, this.displaySize / 3.25);
    }

    public get background(): string
    {
        return `url("data:image/svg+xml,${encodeURIComponent(this.generateSvgRing())}")`;
    }

    public ngOnInit()
    {
        super.ngOnInit();
        this.updateContainerSize();
    }

    public sizeStyles(): { "width": string, "height": string }
    {
        return {
            "width" : `${this.displaySize}px`,
            "height": `${this.displaySize}px`
        };
    }

    public imageStyles(): { "background-image": string, "border-width": string }
    {
        return {
            "background-image": `url("${this.icon}")`,
            "border-width"    : `${this.progressBarWidth}px`
        };
    }

    private updateContainerSize()
    {
        let element: HTMLElement = (<HTMLElement>this.m_elem.nativeElement).parentElement;
        this.containerSize       = Math.min(element.clientWidth, element.clientHeight);
    }

    private generateSvgRing(): string
    {
        let circumference = Math.PI * this.displaySize;
        let radius        = this.displaySize / 2;
        let offset        = -(circumference / 4);
        let width         = this.progressBarWidth * 2;
        let color         = this.colorFn(this.safePercent);
        let percent       = this.safePercent / 100;

        let rectStatic    = "width='100%' height='100%' fill='none' stroke-linecap='butt'";
        let rectDashArray = `${percent * circumference} ${(1 - percent) * circumference}`;
        let rect          = `<rect ${rectStatic} rx='${radius}' ry='${radius}' stroke='${color}' stroke-width='${width}' stroke-dasharray='${rectDashArray}' stroke-dashoffset='${offset}'/>`;
        return `<svg width='100%' height='100%' xmlns='http://www.w3.org/2000/svg'>${rect}</svg>`;
    }
}
