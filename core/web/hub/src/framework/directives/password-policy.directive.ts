import {Directive, Input} from "@angular/core";
import {AbstractControl, NG_VALIDATORS, ValidationErrors, Validator} from "@angular/forms";

@Directive({
               selector : "[o3PasswordPolicy]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: PasswordPolicyDirective,
                       multi      : true
                   }
               ]
           })
export class PasswordPolicyDirective implements Validator
{
    validate(control: AbstractControl): ValidationErrors | null
    {
        let value: string = control.value;

        if (!value || value.length < 8)
        {
            return {"password": "Password must be at least 8 characters"};
        }

        if (!/.*[a-zA-Z].*/.test(value))
        {
            return {"password": "Password must contain a letter"};
        }

        if (!/.*[0-9].*/.test(value))
        {
            return {"password": "Password must contain a number"};
        }

        if (!/.*[^a-zA-Z0-9].*/.test(value))
        {
            return {"password": "Password must contain a special character"};
        }

        return null;
    }
}

@Directive({
               selector : "[o3PasswordMatch]",
               providers: [
                   {
                       provide    : NG_VALIDATORS,
                       useExisting: PasswordMatchDirective,
                       multi      : true
                   }
               ]
           })
export class PasswordMatchDirective implements Validator
{
    @Input("o3PasswordMatch") password: string;

    validate(control: AbstractControl): ValidationErrors | null
    {
        let value: string = control.value;

        if (value != this.password)
        {
            return {"password": "Passwords don't match"};
        }

        return null;
    }
}

