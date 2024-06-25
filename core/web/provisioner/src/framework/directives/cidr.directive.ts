import {Directive} from "@angular/core";
import {AbstractControl, NG_VALIDATORS, ValidationErrors, Validator} from "@angular/forms";
import {IpAddressDirective} from "framework/directives/ip-address.directive";

@Directive({
               selector : "[cidr]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: CidrDirective,
                       multi      : true
                   }
               ]
           })
export class CidrDirective implements Validator
{
    validate(control: AbstractControl): ValidationErrors | null
    {
        return CidrDirective.isValid(control.value, true) ? null : {"cidr": "Invalid CIDR format"};
    }

    public static isValid(value: string,
                          emptyIsValid: boolean): boolean
    {
        if (!value) return emptyIsValid;

        let parts = value.split("/");
        if (parts.length != 2) return false;

        if (!IpAddressDirective.isValid(parts[0], false)) return false;

        if (parts[1].length == 0) return false;
        let mask = +parts[1];
        return (mask >= 0 && mask < 32);
    }
}

@Directive({
               selector : "[cidrRequired]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: CidrRequiredDirective,
                       multi      : true
                   }
               ]
           })
export class CidrRequiredDirective implements Validator
{
    validate(control: AbstractControl): ValidationErrors | null
    {
        return CidrDirective.isValid(control.value, false) ? null : {"cidrRequired": "Invalid CIDR format"};
    }
}
