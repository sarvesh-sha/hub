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
    @Input("o3UniqueValues") values: any[];
    @Input("o3UniqueValuesInitiallyForgiving") forgivingInitialState: boolean = true;

    private m_initialValue: any;

    validate(control: AbstractControl): ValidationErrors
    {
        if (control.pristine)
        {
            this.m_initialValue = control.value;
        }

        let acceptableInitial = this.forgivingInitialState || control.value === this.m_initialValue;
        if (!acceptableInitial && this.values && this.values.some((s) => s === control.value))
        {
            return {o3UniqueValues: true};
        }

        return null;
    }
}

@Directive({
               selector : "[o3AllEquivalentValues]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: AllEquivalentValuesValidatorDirective,
                       multi      : true
                   }
               ]
           })
export class AllEquivalentValuesValidatorDirective<T> implements Validator
{
    @Input("o3AllEquivalentValues") otherValues: T[];
    @Input("o3AllEquivalentMappingFn") mappingFn: (i: any) => T = (val) => val;

    validate(control: AbstractControl): ValidationErrors
    {
        if (!this.otherValues?.length) return null;

        let value = this.mappingFn(control.value);
        if (this.otherValues.some((curr) => this.mappingFn(curr) !== value))
        {
            return {o3AllEquivalentValues: true};
        }

        return null;
    }
}
