import {Component, ViewChild} from "@angular/core";

import {CustomFieldData} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.component";
import {AlertDefinitionExtended} from "app/services/domain/alert-definitions.service";
import * as Models from "app/services/proxy/model/models";
import {AlertSeverityConfigurerComponent} from "app/shared/alerts/alert-severity/alert-severity-configurer.component";

import {UtilsService} from "framework/services/utils.service";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-custom-report-field-alert-table-step",
               templateUrl: "./custom-report-field-wizard-alert-table-step.component.html",
               providers  : [WizardStep.createProvider(CustomReportFieldWizardAlertTableStepComponent)]
           })
export class CustomReportFieldWizardAlertTableStepComponent extends WizardStep<CustomFieldData>
{
    get typedElement(): Models.CustomReportElementAlertTable
    {
        return UtilsService.asTyped(this.data.element, Models.CustomReportElementAlertTable);
    }

    @ViewChild(AlertSeverityConfigurerComponent) severityConfigurer: AlertSeverityConfigurerComponent;

    allAlertRules: boolean;
    allLocations: boolean;
    allAlertTypes: boolean;
    allAlertStatuses: boolean;

    private m_selectedAlertRules: string[] = [];
    get selectedAlertRules(): string[]
    {
        return this.m_selectedAlertRules;
    }

    set selectedAlertRules(rules: string[])
    {
        this.m_selectedAlertRules    = rules;
        this.typedElement.alertRules = rules.map((rule) => AlertDefinitionExtended.newIdentity(rule));
    }

    get rollupType(): string
    {
        return this.typedElement?.rollupType || "";
    }

    set rollupType(type: string)
    {
        this.typedElement.rollupType = <any>type || null;
    }

    public updateAlertRuleSelection()
    {
        if (this.allAlertRules)
        {
            this.typedElement.alertRules = [];
        }
    }

    public updateLocationSelection()
    {
        if (this.allLocations)
        {
            this.typedElement.locations = [];
        }
    }

    public updateAlertTypeSelection()
    {
        if (this.allAlertTypes)
        {
            this.typedElement.alertTypeIDs = [];
        }
    }

    public updateAlertStatusSelection()
    {
        if (this.allAlertStatuses)
        {
            this.typedElement.alertStatusIDs = [];
        }
    }

    //--//

    public getLabel(): string
    {
        return "Alert Table";
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
        if (!this.wizard.stepForm?.valid) return false;

        let alertTable = this.typedElement;
        if (!alertTable) return false;

        if (!this.allAlertRules && !alertTable.alertRules.length) return false;
        if (!this.allLocations && !alertTable.locations.length) return false;
        if (!this.allAlertTypes && !alertTable.alertTypeIDs.length) return false;
        if (!this.allAlertStatuses && !alertTable.alertStatusIDs.length) return false;
        if (this.severityConfigurer && !this.severityConfigurer.valid) return false;

        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
        let alertTable = this.typedElement;
        if (alertTable)
        {
            this.m_selectedAlertRules = alertTable.alertRules.map((rule) => rule.sysId);

            this.allLocations     = !alertTable.locations.length;
            this.allAlertTypes    = !alertTable.alertTypeIDs.length;
            this.allAlertRules    = !alertTable.alertRules.length;
            this.allAlertStatuses = !alertTable.alertStatusIDs.length;
        }
    }

    public async onData(): Promise<void>
    {
        await super.onData();

        await this.data.ensureData();
    }
}
