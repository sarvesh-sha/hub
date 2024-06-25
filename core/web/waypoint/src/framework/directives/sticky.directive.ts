import {AfterViewChecked, AfterViewInit, Directive, ElementRef, Input, OnDestroy, Renderer2} from "@angular/core";


@Directive({
               selector: "[o3-sticky]"
           })
export class StickyDirective implements AfterViewInit,
                                        AfterViewChecked,
                                        OnDestroy
{
    private m_stickyWatch: string             = null;
    private m_stickySide: string              = "top";
    private m_stickyWatchElement: Element     = null;
    private m_stickyWatchListener: () => void = null;

    @Input("stickyWatch") set stickyWatch(selector: string)
    {
        this.m_stickyWatch = selector;
        this.update();
    }

    @Input("stickySide") set stickySide(side: string)
    {
        this.m_stickySide = side;
        this.update();
    }

    constructor(private elementRef: ElementRef,
                private renderer: Renderer2)
    {
    }

    ngAfterViewInit()
    {
        this.update();
    }

    ngAfterViewChecked()
    {
        this.update();
    }

    ngOnDestroy()
    {
        if (this.m_stickyWatchListener) this.m_stickyWatchListener();
    }

    update()
    {
        this.watchElement();
        this.onScroll();
    }

    private onScroll()
    {
        // Take a scroll measurement is there is a watched element
        if (this.m_stickyWatchElement)
        {
            let scroll    = this.m_stickyWatchElement.scrollHeight;
            let client    = this.m_stickyWatchElement.clientHeight;
            let offset    = this.m_stickyWatchElement.scrollTop;
            let hasScroll = scroll > client;

            // Apply style based on mode and offset
            if (this.m_stickySide === "top")
            {
                let delta = (hasScroll ? offset : 0);
                this.renderer.setStyle(this.elementRef.nativeElement, "position", "absolute");
                this.renderer.setStyle(this.elementRef.nativeElement, "top", delta + "px");
            }
            else if (this.m_stickySide === "bottom")
            {
                let delta = hasScroll ? (scroll - client) - offset : 0;
                this.renderer.setStyle(this.elementRef.nativeElement, "position", "absolute");
                this.renderer.setStyle(this.elementRef.nativeElement, "bottom", delta + "px");
            }
            else
            {
                this.renderer.removeStyle(this.elementRef.nativeElement, "position");
                this.renderer.removeStyle(this.elementRef.nativeElement, "top");
                this.renderer.removeStyle(this.elementRef.nativeElement, "bottom");
            }
        }
    }

    private watchElement()
    {
        // If we are already listening, stop listening to that elements
        if (this.m_stickyWatchListener) this.m_stickyWatchListener();

        // If there is a selector, try to find the element to watch
        if (this.m_stickyWatch)
        {
            let element = this.getClosest(this.elementRef.nativeElement, this.m_stickyWatch);

            // If an elements to watch was found, watch it
            if (element)
            {
                this.m_stickyWatchElement  = element;
                this.m_stickyWatchListener = this.renderer.listen(element, "scroll", this.onScroll.bind(this));
            }
        }
    }

    private getClosest(elem: Node,
                       selector: string): Element
    {
        for (; elem && elem !== document; elem = elem.parentNode)
        {
            if (elem instanceof HTMLElement && elem.matches(selector)) return elem;
        }
        return null;
    }
}
