import {AfterViewChecked, AfterViewInit, Directive, ElementRef, HostBinding, Input} from "@angular/core";
import {Observable, Subscription} from "rxjs";

@Directive({
               selector: "[o3-height-breakpoint]"
           })
export class HeightBreakpointDirective implements AfterViewInit,
                                                  AfterViewChecked
{
    @HostBinding("class.height-above") isAbove: boolean = false;
    @HostBinding("class.height-equal") isEqual: boolean = false;
    @HostBinding("class.height-below") isBelow: boolean = false;

    @Input("o3-height-breakpoint") breakpoint: number = null;
    @Input("height-check-auto") auto: boolean         = false;

    @Input("height-check-event") set events(value: Observable<any>)
    {
        // Clear any previous subscription
        if (this.m_subscription) this.m_subscription.unsubscribe();

        // If new value is an Observable, save and subscribe to it
        if (value)
        {
            this.m_events       = value;
            this.m_subscription = value.subscribe(() =>
                                                  {
                                                      this.measure();
                                                  });
        }
        // Otherwise null out the Observable and Subscription
        else
        {
            this.m_events       = null;
            this.m_subscription = null;
        }
    }

    private m_events: Observable<any>    = null;
    private m_subscription: Subscription = null;

    constructor(private elementRef: ElementRef)
    {}

    ngAfterViewInit()
    {
        this.measure();
    }

    ngAfterViewChecked()
    {
        if (this.auto) this.measure();
    }

    measure()
    {
        if (isNaN(this.breakpoint))
        {
            this.isAbove = false;
            this.isEqual = false;
            this.isBelow = false;
        }
        else
        {
            let h        = this.elementRef.nativeElement.clientHeight;
            this.isAbove = h > this.breakpoint;
            this.isEqual = h === this.breakpoint;
            this.isBelow = h < this.breakpoint;
        }
    }
}
