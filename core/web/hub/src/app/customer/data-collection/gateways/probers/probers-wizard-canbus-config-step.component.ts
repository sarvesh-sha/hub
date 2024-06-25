import {Component} from "@angular/core";

import {ProberState} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import * as Models from "app/services/proxy/model/models";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-gateway-probers-wizard-canbus-config-step",
               templateUrl: "./probers-wizard-canbus-config-step.component.html",
               providers  : [
                   WizardStep.createProvider(ProbersWizardCanbusConfigStep)
               ]
           })
export class ProbersWizardCanbusConfigStep extends WizardStep<ProberState>
{
    public get config(): Models.ProberOperationForCANbus
    {
        return <Models.ProberOperationForCANbus>this.data.operation;
    }

    public get configForRawRead(): Models.ProberOperationForCANbusToRawRead
    {
        if (this.data.operation instanceof Models.ProberOperationForCANbusToRawRead)
        {
            return this.data.operation;
        }

        return null;
    }

    public get configForDecodedRead(): Models.ProberOperationForCANbusToDecodedRead
    {
        if (this.data.operation instanceof Models.ProberOperationForCANbusToDecodedRead)
        {
            return this.data.operation;
        }

        return null;
    }

    //--//

    public getLabel() { return "Configure CANbus settings"; }

    public isEnabled()
    {
        return this.data.configuringOperation && this.data.operation instanceof Models.ProberOperationForCANbus;
    }

    public isValid()
    {
        return true;
    }

    public isNextJumpable()
    {
        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
        if (this.config.port == undefined)
        {
            this.config.port = "can0";
        }

        if (this.config.frequency == undefined)
        {
            this.config.frequency = 250000;
        }
    }
}
