import {AfterViewChecked, Directive, ElementRef, EventEmitter, Injector, Input, Output} from "@angular/core";
import {BaseComponent} from "framework/ui/components";

@Directive({
               selector: "[o3-expand]"
           })
export class ExpandDirective extends BaseComponent implements AfterViewChecked
{
    private m_minHeight: number             = 50;
    private m_maxHeight: number             = 100000;
    private m_ignorableHeight: number       = 0;
    private m_containerElemSelector: string = ".mat-sidenav-content"; // goes up DOM til it finds container that matches selector
    private m_container: Element            = null;
    private m_settled: boolean              = false;
    private m_pending: boolean              = false;
    private m_expanding: boolean            = false;
    private m_disabled: boolean             = false;
    private m_offsetTop: number             = 0;
    private m_windowWidth: number           = 0;
    private m_windowHeight: number          = 0;

    @Input("min") set minHeight(value: number)
    {
        this.m_minHeight = value;
    }

    @Input("max") set maxHeight(value: number)
    {
        this.m_maxHeight = value;
    }

    @Input("container") set container(value: string)
    {
        this.m_containerElemSelector = value;
    }

    @Input("ignore") set ignorableHeight(value: number)
    {
        this.m_ignorableHeight = value;
    }

    @Input("expand-off") set expandOff(value: boolean)
    {
        this.m_disabled = value;
        if (this.m_disabled)
        {
            this.clearStyleHeight();
        }
        else
        {
            this.expand();
        }
    }

    @Output() expansionCompleted: EventEmitter<number> = new EventEmitter<number>();

    constructor(inj: Injector,
                private elementRef: ElementRef)
    {
        super(inj);
    }

    ngAfterViewInit()
    {
        super.ngAfterViewInit();
        this.findContainer();
        if (!this.m_disabled) this.setStyleHeight(this.m_minHeight);
    }

    protected afterLayoutChange(): void
    {
        // if the window size actually changed
        if (window.innerHeight != this.m_windowHeight || window.innerWidth != this.m_windowWidth)
        {
            this.m_windowHeight = window.innerHeight;
            this.m_windowWidth  = window.innerWidth;
            this.m_settled      = false;

            this.ngAfterViewChecked();
        }
    }

    private findContainer(): void
    {
        let curr = this.elementRef.nativeElement;
        while (curr.parentElement)
        {
            if (curr.parentElement.matches(this.m_containerElemSelector))
            {
                this.m_container = curr.parentElement;
                return;
            }
            curr = curr.parentElement;
        }
        this.m_container = document.querySelector(this.m_containerElemSelector);
    }

    ngAfterViewChecked()
    {
        // If our control got moved around, refresh height.
        if (this.m_offsetTop != this.elementRef.nativeElement.offsetTop)
        {
            this.m_offsetTop = this.elementRef.nativeElement.offsetTop;
            this.m_settled   = false;
        }

        if (!this.m_settled && !this.m_pending)
        {
            this.m_pending = true;

            this.findContainer();

            if (this.elementRef.nativeElement.offsetParent == null || !this.m_container)
            {
                // Element not visible, reschedule.
                setTimeout(() =>
                           {
                               this.m_pending = false;
                               this.ngAfterViewChecked();
                           }, 100);
            }
            else
            {
                // if the element is visible (and we found the container)
                // (ref https://stackoverflow.com/questions/19669786/check-if-element-is-visible-in-dom)
                // ensure expanded to containers max point
                // ngAfterViewChecked will be call continuously, so this call will get hit repeatedly until settled
                setTimeout(() =>
                           {
                               this.m_pending = false;
                               this.expand();
                           }, 100);
            }
        }
    }

    expand()
    {
        if (this.m_disabled) return;
        if (this.m_expanding) return;
        this.m_expanding = true;

        let height = this.m_minHeight;

        setTimeout(() =>
                   {
                       if (this.m_disabled)
                       {
                           this.m_settled   = true;
                           this.m_expanding = false;
                           this.clearStyleHeight();
                           return;
                       }

                       // effectively what we're doing here is setting the element to its minimum value and growing it by 100px at a time until it fills the container without overlowing
                       while (true)
                       {
                           this.setStyleHeight(height + this.m_ignorableHeight);

                           // overhang is the amount of content that is not visible within the container, that is overflowing the container
                           let overhang = Math.max(0, this.m_container.scrollHeight - this.m_container.clientHeight);
                           if (overhang > 0)
                           {
                               // once scrollbars visible, reduce height to exact fit
                               height = Math.max(this.m_minHeight, (height - overhang));
                               break;
                           }

                           if (height >= this.m_maxHeight)
                           {
                               height = this.m_maxHeight;
                               break;
                           }

                           // while container does not have scroll bars
                           // increase element size
                           height = height + 100;
                       }

                       this.setStyleHeight(height);

                       this.m_settled   = true;
                       this.m_expanding = false;

                       // notify anyone interested
                       this.expansionCompleted.emit(height);
                   });
    }

    private setStyleHeight(height: number)
    {
        this.elementRef.nativeElement.style.height = height + "px";
    }

    private clearStyleHeight()
    {
        this.elementRef.nativeElement.style.height = null;
    }
}
