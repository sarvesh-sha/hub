import {Directive, ElementRef, Input} from "@angular/core";

@Directive({
               selector: "[optio3TestValue]",
               exportAs: "optio3TestValue"
           })
export class Optio3TestValue
{
    private m_value: string;

    @Input("optio3TestValue") set value(value: string)
    {
        this.m_value = value;
        this.apply();
    }

    constructor(private elementRef: ElementRef)
    {
    }

    private apply(): void
    {
        this.elementRef.nativeElement.setAttribute("o3-test-value", this.m_value);
    }
}
