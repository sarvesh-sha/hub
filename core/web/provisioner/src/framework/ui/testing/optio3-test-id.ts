import {Directive, ElementRef, Input} from "@angular/core";

@Directive({
               selector: "[optio3TestId]",
               exportAs: "optio3TestId"
           })
export class Optio3TestId
{
    private m_id: string;

    @Input("optio3TestId") set message(value: string)
    {
        this.m_id = value;
        this.apply();
    }

    constructor(private elementRef: ElementRef)
    {
    }

    private apply(): void
    {
        this.elementRef.nativeElement.setAttribute("o3-test-id", this.m_id);

        // mabl element selection doesn't take into account o3-test-id
        let className = this.m_id?.replace(/\s/g, "-");
        if (className) this.elementRef.nativeElement.classList.add("o3-test--" + className);
    }
}
