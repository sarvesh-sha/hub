import {Directive, TemplateRef} from "@angular/core";

@Directive({selector: "[o3-wizard-step-template]"})
export class WizardStepContentTemplateDirective
{
    constructor(public template: TemplateRef<any>) { }
}
