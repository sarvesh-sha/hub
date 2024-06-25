import {Directive, ElementRef, Input} from "@angular/core";
import {UsersService} from "app/services/domain/users.service";

@Directive({
               selector: "[hasAccess]"
           })
export class HasAccessDirective
{
    @Input("hasAccess") roles: string[];

    constructor(private elementRef: ElementRef,
                private users: UsersService)
    {
    }

    ngOnInit()
    {
        this.verify();
    }

    private verify(): void
    {
        if (this.roles && this.roles.length)
        {
            if (!this.users.hasAnyRole(this.roles))
            {
                let el: HTMLElement = this.elementRef.nativeElement;
                el.parentNode.removeChild(el);
            }
        }
    }
}
