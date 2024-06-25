import {Directive, ElementRef, Input, OnDestroy, OnInit} from "@angular/core";
import * as pan from "panzoom";
import {PanZoom} from "panzoom";

let panzoom: typeof pan.default = <any>pan;

@Directive({
               selector: "[o3-pan-zoom]"
           })
export class PanZoomDirective implements OnInit,
                                         OnDestroy
{
    private m_zoom: PanZoom;

    constructor(private element: ElementRef)
    {
    }

    @Input() autoZoomDelay = 0;

    public ngOnInit(): void
    {
        this.m_zoom = panzoom(this.element.nativeElement, {
            smoothScroll : false,
            bounds       : true,
            boundsPadding: .1,
            maxZoom      : 1.1,
            minZoom      : .25
        });
    }

    public ngAfterViewInit(): void
    {
        setTimeout(() => this.autoZoom(), this.autoZoomDelay);
    }

    public ngOnDestroy(): void
    {
        this.m_zoom.dispose();
    }

    public autoZoom(): void
    {
        let element      = this.element.nativeElement;
        let parent       = element.parentElement;
        let widthRatio   = parent.clientWidth / element.clientWidth;
        let heightRatio  = parent.clientHeight / element.clientHeight;
        let ratio        = Math.min(widthRatio, heightRatio);
        let currentRatio = this.m_zoom.getTransform().scale;

        if (!isNaN(ratio))
        {
            this.m_zoom.setTransformOrigin({
                                               x: 0,
                                               y: 0
                                           });
            this.m_zoom.moveTo(0, 0);
            this.m_zoom.zoomTo(0, 0, ratio / currentRatio);
            this.m_zoom.setTransformOrigin(null);
        }
    }
}
