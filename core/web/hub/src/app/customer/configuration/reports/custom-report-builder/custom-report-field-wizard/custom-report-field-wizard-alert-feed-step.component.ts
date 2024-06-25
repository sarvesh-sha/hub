import {Component} from "@angular/core";

import {CustomFieldData} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.component";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-custom-report-field-alert-feed-step",
               templateUrl: "./custom-report-field-wizard-alert-feed-step.component.html",
               providers  : [WizardStep.createProvider(CustomReportFieldWizardAlertFeedStepComponent)]
           })
export class CustomReportFieldWizardAlertFeedStepComponent extends WizardStep<CustomFieldData>
{
    get typedElement(): Models.CustomReportElementAlertFeed
    {
        return UtilsService.asTyped(this.data.element, Models.CustomReportElementAlertFeed);
    }

    allLocations: boolean;
    allAlertTypes: boolean;

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
            this.typedElement.alertTypes = [];
        }
    }

    //--//

    public getLabel(): string
    {
        return "Alert Feed";
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

        let alertFeed = this.typedElement;
        if (!alertFeed) return false;

        if (!this.allLocations && !alertFeed.locations.length) return false;
        if (!this.allAlertTypes && !alertFeed.alertTypes.length) return false;

        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
        let alertFeed      = this.typedElement;
        this.allLocations  = !alertFeed.locations.length;
        this.allAlertTypes = !alertFeed.alertTypes.length;
    }

    public async onData(): Promise<void>
    {
        await super.onData();

        await this.data.ensureData();
    }
}
