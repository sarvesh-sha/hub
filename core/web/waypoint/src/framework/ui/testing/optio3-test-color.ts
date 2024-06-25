import {Directive, ElementRef, Input} from "@angular/core";

@Directive({
               selector: "[optio3TestColor]",
               exportAs: "optio3TestColor"
           })
export class Optio3TestColor
{
    private m_value: string;

    @Input("optio3TestColor") set value(value: string)
    {
        this.m_value = value;
        this.apply();
    }

    constructor(private elementRef: ElementRef)
    {
    }

    private apply(): void
    {
        this.elementRef.nativeElement.setAttribute("o3-test-color", this.m_value);
    }
}
