import {AfterViewInit, Directive, ElementRef, OnDestroy} from "@angular/core";
import {MatTooltip} from "@angular/material/tooltip";
import {CommonLayout} from "framework/ui/layout";
import {Subscription} from "rxjs";

@Directive({
               selector: "[matTooltip][o3TooltipWhenTruncated]"
           })
export class TooltipWhenTruncatedDirective implements AfterViewInit,
                                                      OnDestroy
{
    private m_layoutSubscription: Subscription;

    constructor(private element: ElementRef,
                private tooltip: MatTooltip,
                private layout: CommonLayout)
    {
        this.m_layoutSubscription = this.layout.contentSizeChanged.subscribe(() =>
                                                                             {
                                                                                 this.updateTooltip();
                                                                             });
    }

    ngAfterViewInit()
    {
        setTimeout(() => this.updateTooltip(), 50);
    }

    public ngOnDestroy(): void
    {
        this.m_layoutSubscription.unsubscribe();
        this.m_layoutSubscription = null;
    }

    private updateTooltip()
    {
        let element: HTMLElement = this.element.nativeElement;
        this.tooltip.disabled    = element.scrollWidth <= element.offsetWidth;
    }
}
