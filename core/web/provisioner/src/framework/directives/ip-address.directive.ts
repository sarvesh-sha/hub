import {Directive} from "@angular/core";
import {AbstractControl, NG_VALIDATORS, ValidationErrors, Validator} from "@angular/forms";

@Directive({
               selector : "[ipAddress]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: IpAddressDirective,
                       multi      : true
                   }
               ]
           })
export class IpAddressDirective implements Validator
{
    validate(control: AbstractControl): ValidationErrors | null
    {
        return IpAddressDirective.isValid(control.value, true) ? null : {"ipAddress": "Invalid IP Address format"};
    }

    public static isValid(value: string,
                          emptyIsValid: boolean): boolean
    {
        if (!value) return emptyIsValid;

        let regExp = /^(\d+)\.(\d+)\.(\d+)\.(\d+)$/;

        let res = regExp.exec(value);
        if (res == null) return false;

        for (let pos = 1; pos < 5; pos++)
        {
            let num = +res[pos];

            if (num < 0 || num > 255) return false;
        }

        return true;
    }
}

@Directive({
               selector : "[ipAddressRequired]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: IpAddressRequiredDirective,
                       multi      : true
                   }
               ]
           })
export class IpAddressRequiredDirective implements Validator
{
    validate(control: AbstractControl): ValidationErrors | null
    {
        return IpAddressDirective.isValid(control.value, false) ? null : {"ipAddressRequired": "Invalid IP Address format"};
    }
}
