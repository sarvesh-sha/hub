import {Directive, Input} from "@angular/core";
import {AbstractControl, NG_VALIDATORS, ValidationErrors, Validator} from "@angular/forms";

@Directive({
               selector : "[o3UniqueValues]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: UniqueValuesValidatorDirective,
                       multi      : true
                   }
               ]
           })
export class UniqueValuesValidatorDirective implements Validator
{
    public static ERROR_KEY: string = "o3NotUnique";

    @Input("o3UniqueValues") values: any[];
    @Input("o3UniqueValuesAllowSelfMatch") selfMatch: boolean = false;

    private m_initialValue: any;

    validate(control: AbstractControl): ValidationErrors
    {
        if (control.pristine)
        {
            this.m_initialValue = control.value;
        }

        if (this.selfMatch)
        {
            if (control.value == this.m_initialValue)
            {
                return null;
            }
        }

        if (this.values?.some((s) => s == control.value))
        {
            return {[UniqueValuesValidatorDirective.ERROR_KEY]: true};
        }

        return null;
    }

    static isInvalid(errors: ValidationErrors): boolean
    {
        return errors && errors[UniqueValuesValidatorDirective.ERROR_KEY];
    }
}

@Directive({
               selector : "[o3UniqueValuesWithCallback]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: UniqueValuesWithCallbackValidatorDirective,
                       multi      : true
                   }
               ]
           })
export class UniqueValuesWithCallbackValidatorDirective<T, U> implements Validator
{
    public static ERROR_KEY: string = "o3NotUniqueWithCallback";

    @Input("o3UniqueValuesWithCallback") fn: (control: AbstractControl) => boolean;

    validate(control: AbstractControl): ValidationErrors
    {
        if (this.fn(control)) return null;

        return {[UniqueValuesWithCallbackValidatorDirective.ERROR_KEY]: true};
    }

    static isInvalid(errors: ValidationErrors): boolean
    {
        return errors && errors[UniqueValuesWithCallbackValidatorDirective.ERROR_KEY];
    }
}


@Directive({
               selector : "[o3EquivalentValues]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: EquivalentValuesValidatorDirective,
                       multi      : true
                   }
               ]
           })
export class EquivalentValuesValidatorDirective<T, U> implements Validator
{
    public static ERROR_KEY: string = "o3NotEquivalentValues";

    private static identityFn = (value: any) => value;

    @Input("o3EquivalentValues") otherValues: T[];
    @Input("o3AllEquivalentMappingFn") mappingFn: (value: T) => U = EquivalentValuesValidatorDirective.identityFn;

    validate(control: AbstractControl): ValidationErrors
    {
        if (!this.otherValues?.length) return null;

        let value = this.mappingFn(control.value);
        if (this.otherValues.some((curr) => this.mappingFn(curr) !== value))
        {
            return {[EquivalentValuesValidatorDirective.ERROR_KEY]: true};
        }

        return null;
    }

    static isInvalid(errors: ValidationErrors): boolean
    {
        return errors && errors[EquivalentValuesValidatorDirective.ERROR_KEY];
    }
}
