import {Component, Injector} from "@angular/core";
import {AppContext} from "app/app.service";
import {AlertRuleWizardState} from "app/customer/configuration/alert-rules/wizard/alert-rule-wizard-dialog.component";
import {AlertDefinitionExtended} from "app/services/domain/alert-definitions.service";
import * as Models from "app/services/proxy/model/models";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-alert-rule-library-step",
               templateUrl: "./alert-rule-wizard-library-step.component.html",
               providers  : [
                   WizardStep.createProvider(AlertRuleWizardLibraryStepComponent)
               ]
           })
export class AlertRuleWizardLibraryStepComponent extends WizardStep<AlertRuleWizardState>
{
    app: AppContext;

    constructor(inj: Injector)
    {
        super(inj);
        this.app = new AppContext(inj);
    }

    libraryDefinitions: AlertDefinitionExtended[];

    getLabel() { return "Start"; }

    async ngAfterViewInit()
    {
        let filters             = Models.AlertDefinitionFilterRequest.newInstance({purposes: [Models.AlertDefinitionPurpose.Library]});
        this.libraryDefinitions = await this.app.domain.alertDefinitions.getExtendedList(filters);
    }

    async templateChanged()
    {
        let chosenDefinition = null;
        if (this.data.rules.selectedTemplateId)
        {
            chosenDefinition = this.libraryDefinitions.find((d) => d.model.sysId === this.data.rules.selectedTemplateId);
        }

        await this.data.rules.initFromTemplate(chosenDefinition);
    }

    isEnabled()
    {
        return this.libraryDefinitions?.length > 1;
    }

    isValid()
    {
        return true;
    }

    isNextJumpable()
    {
        return true;
    }

    async onNext()
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}


