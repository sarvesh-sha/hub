import {BreakpointObserver} from "@angular/cdk/layout";
import {ChangeDetectorRef, Directive, Input, TemplateRef, ViewContainerRef} from "@angular/core";

@Directive({selector: "[o3-min-width]"})
export class MinWidthDirective
{
    private showing: boolean;

    constructor(private templateRef: TemplateRef<any>,
                private viewContainer: ViewContainerRef,
                private cdr: ChangeDetectorRef,
                private breakpointObserver: BreakpointObserver)
    {
    }

    @Input("o3-min-width") set requiredWidth(width: number)
    {
        this.updateTemplate(screen.width >= width);

        this.breakpointObserver.observe(`(min-width: ${width}px)`)
            .subscribe((handsetBreakpoint) => this.updateTemplate(handsetBreakpoint.matches));
    }

    updateTemplate(largeEnough: boolean): void
    {
        if (largeEnough)
        {
            if (!this.showing)
            {
                this.viewContainer.createEmbeddedView(this.templateRef);
                this.showing = true;
                this.cdr.markForCheck();
            }
        }
        else
        {
            if (this.showing)
            {
                this.viewContainer.clear();
                this.showing = false;
                this.cdr.markForCheck();
            }
        }
    }
}
