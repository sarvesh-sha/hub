import {Directive} from "@angular/core";
import {AbstractControl, NG_VALIDATORS, ValidationErrors, Validator} from "@angular/forms";

@Directive({
               selector : "[macAddress]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: MACAddressDirective,
                       multi      : true
                   }
               ]
           })
export class MACAddressDirective implements Validator
{
    validate(control: AbstractControl): ValidationErrors | null
    {
        return MACAddressDirective.isValid(control.value, true) ? null : {"macAddress": "Invalid MAC Address format"};
    }

    public static isValid(value: string,
                          emptyIsValid: boolean): boolean
    {
        if (!value) return emptyIsValid;

        let parsed = MACAddressDirective.fromMACString(value);
        return !!parsed;
    }

    public static fromMACString(value: string): [d1: number, d2: number, d3: number, d4: number, d5: number, d6: number]
    {
        let regExp = /^([a-fA-F0-9]{2}):([a-fA-F0-9]{2}):([a-fA-F0-9]{2}):([a-fA-F0-9]{2}):([a-fA-F0-9]{2}):([a-fA-F0-9]{2})$/;

        let res = regExp.exec(value);
        if (res == null) return null;

        let parts = res.slice(1, 7);


        let [d1, d2, d3, d4, d5, d6] = parts.map((p) => parseInt(p, 16));

        return [
            d1,
            d2,
            d3,
            d4,
            d5,
            d6
        ];
    }

    public static toMACString(d1: number,
                              d2: number,
                              d3: number,
                              d4: number,
                              d5: number,
                              d6: number): string
    {
        return [
            d1,
            d2,
            d3,
            d4,
            d5,
            d6
        ].map((d) => (d || 0).toString(16))
         .map((hex) => hex.length < 2 ? "0" + hex : hex)
         .join(":")
         .toUpperCase();
    }
}

@Directive({
               selector : "[macAddressRequired]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: MACAddressRequiredDirective,
                       multi      : true
                   }
               ]
           })
export class MACAddressRequiredDirective implements Validator
{
    validate(control: AbstractControl): ValidationErrors | null
    {
        return MACAddressDirective.isValid(control.value, false) ? null : {"macAddressRequired": "Invalid MAC Address format"};
    }
}
