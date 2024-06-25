import {Directive} from "@angular/core";
import {AbstractControl, NG_VALIDATORS, ValidationErrors, Validator} from "@angular/forms";
import {IpAddressDirective} from "framework/directives/ip-address.directive";

@Directive({
               selector : "[ipAddressWithPort]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: IpAddressWithPortDirective,
                       multi      : true
                   }
               ]
           })
export class IpAddressWithPortDirective implements Validator
{
    validate(control: AbstractControl): ValidationErrors | null
    {
        return IpAddressWithPortDirective.isValid(control.value, true) ? null : {"ipAddress": "Invalid IP Address format"};
    }

    public static isValid(value: string,
                          emptyIsValid: boolean): boolean
    {
        if (!value) return emptyIsValid;

        let parts = value.split(":");
        if (parts.length != 2) return false;

        if (!IpAddressDirective.isValid(parts[0], false)) return false;

        if (parts[1].length == 0) return false;
        let port = +parts[1];
        return (port >= 0 && port < 65536);
    }
}

@Directive({
               selector : "[ipAddressWithPortRequired]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: IpAddressWithPortRequiredDirective,
                       multi      : true
                   }
               ]
           })
export class IpAddressWithPortRequiredDirective implements Validator
{
    validate(control: AbstractControl): ValidationErrors | null
    {
        return IpAddressWithPortDirective.isValid(control.value, false) ? null : {"ipAddressWithPortRequired": "Invalid IP Address format"};
    }
}
