import {Component, Type} from "@angular/core";

import {ProberState} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import * as Models from "app/services/proxy/model/models";
import {WizardStep} from "framework/ui/wizards/wizard-step";
import {Memoizer} from "framework/utils/memoizers";

@Component({
               selector   : "o3-gateway-probers-wizard-bacnet-op-step",
               templateUrl: "./probers-wizard-bacnet-op.step.html",
               providers  : [
                   WizardStep.createProvider(ProbersWizardBacnetOpStep)
               ]
           })
export class ProbersWizardBacnetOpStep extends WizardStep<ProberState>
{
    @Memoizer
    public getOperationTypesList(): { type: Type<Models.ProberOperationForBACnet>, title: string }[]
    {
        let res = [];

        res.push({
                     type : Models.ProberOperationForBACnetToDiscoverRouters,
                     title: "Discover Routers"
                 });

        res.push({
                     type : Models.ProberOperationForBACnetToDiscoverBBMDs,
                     title: "Discover BBMDs"
                 });

        res.push({
                     type : Models.ProberOperationForBACnetToReadBBMDs,
                     title: "Analyze BBMDs"
                 });

        res.push({
                     type : Models.ProberOperationForBACnetToDiscoverDevices,
                     title: "Discover Devices through Broadcast"
                 });

        res.push({
                     type : Models.ProberOperationForBACnetToScanSubnetForDevices,
                     title: "Discover Devices through subnet scan"
                 });

        res.push({
                     type : Models.ProberOperationForBACnetToScanMstpTrunkForDevices,
                     title: "Discover Devices through MS/TP trunk scan"
                 });

        res.push({
                     type : Models.ProberOperationForBACnetToAutoDiscovery,
                     title: "Discover Devices through multiple heuristics"
                 });

        res.push({
                     type : Models.ProberOperationForBACnetToReadDevices,
                     title: "Analyze Devices"
                 });

        res.push({
                     type : Models.ProberOperationForBACnetToReadObjectNames,
                     title: "Read Object Names"
                 });

        res.push({
                     type : Models.ProberOperationForBACnetToReadObjects,
                     title: "Read Objects"
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
        return this.data.configuringOperation && this.data.protocolType == Models.ProberOperationForBACnet;
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
