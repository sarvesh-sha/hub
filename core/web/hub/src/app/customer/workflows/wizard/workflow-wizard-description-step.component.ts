import {Component} from "@angular/core";
import {WorkflowWizardData} from "app/customer/workflows/wizard/workflow-wizard.component";
import {UserExtended} from "app/services/domain/user-management.service";

import * as Models from "app/services/proxy/model/models";
import {ControlOption} from "framework/ui/control-option";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-workflow-wizard-description-step",
               templateUrl: "./workflow-wizard-description-step.component.html",
               providers  : [WizardStep.createProvider(WorkflowWizardDescriptionStepComponent)]
           })
export class WorkflowWizardDescriptionStepComponent extends WizardStep<WorkflowWizardData>
{
    workflowPriorityOptions: ControlOption<Models.WorkflowPriority>[] = [];

    users: ControlOption<string>[] = [];

    get assignedTo(): string
    {
        return this.data.workflow.typedModel.assignedTo && this.data.workflow.typedModel.assignedTo.sysId || "";
    }

    set assignedTo(id: string)
    {
        let ri = id ? UserExtended.newIdentity(id) : null;
        this.data.workflow.typedModel.assignedTo = ri;
    }

    public async onData()
    {
        await super.onData();

        this.workflowPriorityOptions = await this.data.app.bindings.getWorkflowPriorities();
        this.users                   = await this.data.app.bindings.getUsers(true);
    }

    public getLabel(): string
    {
        return "Notes";
    }

    public isEnabled(): boolean
    {
        return true;
    }

    public isNextJumpable(): boolean
    {
        return false;
    }

    public isValid(): boolean
    {
        return !!this.data.workflow.model.description;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
