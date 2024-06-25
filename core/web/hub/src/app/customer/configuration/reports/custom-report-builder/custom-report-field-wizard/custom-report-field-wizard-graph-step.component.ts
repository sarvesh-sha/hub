import {Component, Injector} from "@angular/core";

import {CustomReportBuilderComponent} from "app/customer/configuration/reports/custom-report-builder/custom-report-builder.component";
import {CustomFieldData} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.component";

import {GraphConfigurationHostChecker} from "app/shared/assets/configuration/graph-configuration-host";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-custom-report-field-graph-step",
               templateUrl: "./custom-report-field-wizard-graph-step.component.html",
               providers  : [WizardStep.createProvider(CustomReportFieldWizardGraphStepComponent)]
           })
export class CustomReportFieldWizardGraphStepComponent extends WizardStep<CustomFieldData>
{
    constructor(inj: Injector,
                public builder: CustomReportBuilderComponent)
    {
        super(inj);
    }

    public getLabel(): string
    {
        return "Edit Asset Structures";
    }

    public isEnabled(): boolean
    {
        return this.data.requiresAssetStructure();
    }

    public isNextJumpable(): boolean
    {
        return false;
    }

    public isValid(): boolean
    {
        this.detectChanges();
        return GraphConfigurationHostChecker.isValid(this.builder);
    }

    public async onNext(): Promise<boolean>
    {
        await this.builder.resolveGraphs();
        return false;
    }

    public async onStepSelected()
    {
    }
}
