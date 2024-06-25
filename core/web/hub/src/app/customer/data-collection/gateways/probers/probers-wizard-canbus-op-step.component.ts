import {Component, Type} from "@angular/core";

import {ProberState} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import * as Models from "app/services/proxy/model/models";
import {WizardStep} from "framework/ui/wizards/wizard-step";
import {Memoizer} from "framework/utils/memoizers";

@Component({
               selector   : "o3-gateway-probers-wizard-canbus-op-step",
               templateUrl: "./probers-wizard-canbus-op-step.component.html",
               providers  : [
                   WizardStep.createProvider(ProbersWizardCanbusOpStep)
               ]
           })
export class ProbersWizardCanbusOpStep extends WizardStep<ProberState>
{
    @Memoizer
    public getOperationTypesList(): { type: Type<Models.ProberOperationForCANbus>, title: string }[]
    {
        let res = [];

        res.push({
                     type : Models.ProberOperationForCANbusToRawRead,
                     title: "Sample Raw Frames"
                 });

        res.push({
                     type : Models.ProberOperationForCANbusToDecodedRead,
                     title: "Sample Decoded Frames"
                 });

        return res;
    }

    get operationType(): Type<Models.ProberOperation>
    {
        return this.data.operationType;
    }

    set operationType(val: Type<Models.ProberOperation>)
    {
        this.data.operationType = val;

        if (this.data.operationType)
        {
            if (!(this.data.operation instanceof this.data.operationType))
            {
                if (this.data.previousOperation instanceof this.data.operationType)
                {
                    this.data.operation = this.data.previousOperation;
                }
                else
                {
                    this.data.operation = new this.data.operationType();
                }
            }
        }
    }

    //--//

    public getLabel() { return "Select Operation"; }

    public isEnabled()
    {
        return this.data.configuringOperation && this.data.protocolType == Models.ProberOperationForCANbus;
    }

    public isValid()
    {
        if (!this.data.operationType) return false; // Not loaded yet...

        return true;
    }

    public isNextJumpable()
    {
        return this.data.operation instanceof this.data.operationType;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
