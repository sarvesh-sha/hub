import {ContentChildren, Directive, Input, QueryList} from "@angular/core";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Directive({
               selector : "o3-wizard-step-group",
               providers: [WizardStep.createProvider(WizardStepGroupDirective)]
           })
export class WizardStepGroupDirective
{
    @Input() public isEnabled: () => boolean;

    @ContentChildren(WizardStep) public steps: QueryList<WizardStep<any>>;
}
