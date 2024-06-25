import {Directive, ElementRef} from "@angular/core";

@Directive({
               selector: "[autofocus]"
           })
export class AutofocusDirective
{
    constructor(private el: ElementRef)
    {

    }

    ngOnInit()
    {

    }

    ngAfterViewInit()
    {
        setTimeout(() => { this.el.nativeElement.focus(); }, 50);
    }
}
