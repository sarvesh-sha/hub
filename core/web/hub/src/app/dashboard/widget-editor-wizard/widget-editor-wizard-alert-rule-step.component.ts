import {Component} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {ALL_ENUMS_ID, triStateChange} from "app/shared/selection/enum-selection";
import {ControlOptionItem} from "app/shared/tables/control-option-list.component";

import {DatatableSelectionChangeSummary} from "framework/ui/datatables/datatable-manager";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-alert-rule-step",
               templateUrl: "./widget-editor-wizard-alert-rule-step.component.html",
               styleUrls  : ["./widget-editor-wizard-dialog.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardAlertRuleStepComponent)]
           })
export class WidgetEditorWizardAlertRuleStepComponent extends WizardStep<WidgetEditorWizardState>
{
    alertRuleOptions: AlertRuleOption[] = [];
    alertRuleSelection: Set<string>     = new Set<string>();

    public async onData()
    {
        await super.onData();

        let rules             = await this.data.app.domain.alertDefinitions.getExtendedList();
        this.alertRuleOptions = [new AlertRuleOption(ALL_ENUMS_ID, "Any Alert Rule", "")];
        for (let rule of rules)
        {
            this.alertRuleOptions.push(new AlertRuleOption(rule.model.sysId, rule.model.title, rule.model.description));
        }

        this.readAlertRules();
    }

    public getLabel() { return "Alert Rules"; }

    public isEnabled()
    {
        return this.data.editor.allowWidgetTypes(Models.AlertTableWidgetConfiguration);
    }

    public isValid()
    {
        for (let i in this.data.editor.selectedAlertRule)
        {
            if (this.data.editor.selectedAlertRule[i])
            {
                return true;
            }
        }
        return false;
    }

    public isNextJumpable()
    {
        return true;
    }

    public async onNext()
    {
        return false;
    }

    public async onStepSelected()
    {
        this.readAlertRules();
    }

    readAlertRules()
    {
        this.alertRuleSelection = this.data.editor.readSelectionData(this.data.editor.selectedAlertRule);
    }

    writeAlertRules()
    {
        this.data.editor.selectedAlertRule = this.data.editor.writeSelectionData(this.alertRuleSelection);
        this.data.editor.syncAlertTypes();
    }

    onOptionChange(change: DatatableSelectionChangeSummary<string>)
    {
        this.alertRuleSelection = triStateChange(change, this.alertRuleSelection);
        this.writeAlertRules();
    }
}

class AlertRuleOption implements ControlOptionItem<string>
{
    constructor(public readonly id: string,
                public readonly label: string,
                public readonly description: string)
    {
    }
}
