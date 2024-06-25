import {Directive} from "@angular/core";
import {FormControl, FormGroupDirective, NgForm} from "@angular/forms";
import {ErrorStateMatcher} from "@angular/material/core";
import {MatInput} from "@angular/material/input";

class InteractiveErrorStateMatcher implements ErrorStateMatcher
{
    isErrorState(control: FormControl,
                 form: FormGroupDirective | NgForm): boolean
    {
        return !!(control && control.invalid && (control.dirty || control.touched));
    }
}


@Directive({
               selector: "[matInput][o3InteractiveErrors]"
           })
export class InteractiveErrorStateMatcherDirective
{
    constructor(private m_formField: MatInput)
    {
        this.m_formField.errorStateMatcher = new InteractiveErrorStateMatcher();
    }
}
