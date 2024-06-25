import {Component} from "@angular/core";

import {CustomFieldData} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.component";
import {AssetGraphTreeNode} from "app/services/domain/asset-graph.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-custom-report-field-device-element-list-step",
               templateUrl: "./custom-report-field-wizard-device-element-list-step.component.html",
               providers  : [WizardStep.createProvider(CustomReportFieldWizardDeviceElementListStepComponent)]
           })
export class CustomReportFieldWizardDeviceElementListStepComponent extends WizardStep<CustomFieldData>
{
    get typedElement(): Models.CustomReportElementDeviceElementList
    {
        return UtilsService.asTyped(this.data.element, Models.CustomReportElementDeviceElementList);
    }

    public getId(binding: Models.AssetGraphBinding)
    {
        return AssetGraphTreeNode.getIdFromBinding(binding);
    }

    public getBinding(id: string): Models.AssetGraphBinding
    {
        return AssetGraphTreeNode.getBinding(id);
    }

    //--//

    public getLabel(): string
    {
        return "Device Element List";
    }

    public isEnabled(): boolean
    {
        return !!this.typedElement;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        return this.wizard.stepForm?.valid;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
