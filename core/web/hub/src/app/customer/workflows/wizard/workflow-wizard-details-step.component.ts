import {Component, ViewChild} from "@angular/core";
import {WorkflowDetailsEditorComponent} from "app/customer/workflows/wizard/workflow-details-editor.component";
import {WorkflowWizardData} from "app/customer/workflows/wizard/workflow-wizard.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-workflow-wizard-details-step",
               templateUrl: "./workflow-wizard-details-step.component.html",
               providers  : [WizardStep.createProvider(WorkflowWizardDetailsStepComponent)]
           })
export class WorkflowWizardDetailsStepComponent extends WizardStep<WorkflowWizardData>
{
    @ViewChild(WorkflowDetailsEditorComponent) editor: WorkflowDetailsEditorComponent;

    public getLabel(): string
    {
        return "Details";
    }

    public isEnabled(): boolean
    {
        return true;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        this.detectChanges();
        return this.editor && this.editor.isValid();
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
