import {BreakpointObserver} from "@angular/cdk/layout";
import {ChangeDetectorRef, Directive, Input, OnDestroy, TemplateRef, ViewContainerRef} from "@angular/core";

import {Subscription} from "rxjs";

@Directive({selector: "[o3MinWidth]"})
export class MinWidthDirective implements OnDestroy
{
    private m_minWidth: number;
    private m_breakpointSub: Subscription;
    private m_showing: boolean;

    @Input("o3MinWidth") set requiredWidth(width: number)
    {
        if (width && width !== this.m_minWidth)
        {
            this.m_minWidth    = width || this.m_minWidth;
            this.m_largeEnough = window.innerWidth >= this.m_minWidth;
            this.updateTemplate();

            this.clearSub();
            this.m_breakpointSub = this.m_breakpointObserver.observe(`(min-width: ${this.m_minWidth}px)`)
                                       .subscribe((handsetBreakpoint) =>
                                                  {
                                                      this.m_largeEnough = handsetBreakpoint.matches;
                                                      this.updateTemplate();
                                                  });
        }
    }

    private m_alwaysShow: boolean = false;
    @Input("o3MinWidthOr") set alwaysShow(show: boolean)
    {
        this.m_alwaysShow = show;
        this.updateTemplate();
    }

    private m_largeEnough: boolean;

    constructor(private m_templateRef: TemplateRef<any>,
                private m_viewContainerRef: ViewContainerRef,
                private m_cdr: ChangeDetectorRef,
                private m_breakpointObserver: BreakpointObserver)
    {
    }

    updateTemplate(): void
    {
        if (this.m_largeEnough || this.m_alwaysShow)
        {
            if (!this.m_showing)
            {
                this.m_viewContainerRef.createEmbeddedView(this.m_templateRef);
                this.m_showing = true;
                this.m_cdr.markForCheck();
            }
        }
        else
        {
            if (this.m_showing)
            {
                this.m_viewContainerRef.clear();
                this.m_showing = false;
                this.m_cdr.markForCheck();
            }
        }
    }

    //--//

    ngOnDestroy()
    {
        this.clearSub();
    }

    private clearSub()
    {
        if (this.m_breakpointSub)
        {
            this.m_breakpointSub.unsubscribe();
            this.m_breakpointSub = null;
        }
    }
}
