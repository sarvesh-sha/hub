import {Component} from "@angular/core";

import {CustomFieldData} from "app/customer/configuration/panes/wizard/pane-field-wizard/pane-field-wizard.component";
import {TimeSeriesChartConfigurationExtended} from "app/customer/visualization/time-series-utils";
import * as Models from "app/services/proxy/model/models";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-pane-field-details-step",
               templateUrl: "./pane-field-wizard-details-step.component.html",
               providers  : [
                   WizardStep.createProvider(PaneFieldWizardDetailsStepComponent)
               ]
           })
export class PaneFieldWizardDetailsStepComponent extends WizardStep<CustomFieldData>
{
    public getLabel() { return "Details"; }

    public isEnabled()
    {
        return this.data && !!this.data.element;
    }

    public isValid()
    {
        let aggregationField = this.asAggregatedValueField();
        if (aggregationField) return aggregationField.controlPointGroup?.selections?.identities?.length > 0;

        let currentValueField = this.asCurrentValueField();
        if (currentValueField) return !!currentValueField.pointInput?.nodeId;

        let chartField = this.asChartField();
        if (chartField) return TimeSeriesChartConfigurationExtended.hasDataSources(chartField.config);

        let locationBindingField = this.asAlertCountField() || this.asAlertFeedField() || this.asPathMapField();
        if (locationBindingField) return !!locationBindingField.locationInput?.nodeId;

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
    }

    asAggregatedValueField(): Models.PaneFieldConfigurationAggregatedValue
    {
        if (this.data.element instanceof Models.PaneFieldConfigurationAggregatedValue)
        {
            return this.data.element;
        }

        return undefined;
    }

    asChartField(): Models.PaneFieldConfigurationChart
    {
        if (this.data.element instanceof Models.PaneFieldConfigurationChart)
        {
            return this.data.element;
        }

        return undefined;
    }

    asAlertFeedField(): Models.PaneFieldConfigurationAlertFeed
    {
        if (this.data.element instanceof Models.PaneFieldConfigurationAlertFeed)
        {
            return this.data.element;
        }

        return null;
    }

    asAlertCountField(): Models.PaneFieldConfigurationAlertCount
    {
        if (this.data.element instanceof Models.PaneFieldConfigurationAlertCount)
        {
            return this.data.element;
        }

        return null;
    }

    asCurrentValueField(): Models.PaneFieldConfigurationCurrentValue
    {
        if (this.data.element instanceof Models.PaneFieldConfigurationCurrentValue)
        {
            return this.data.element;
        }

        return null;
    }

    asPathMapField(): Models.PaneFieldConfigurationPathMap
    {
        if (this.data.element instanceof Models.PaneFieldConfigurationPathMap)
        {
            return this.data.element;
        }

        return null;
    }

    getPointInput(id: string): Models.AssetGraphBinding
    {
        if (!id) return null;

        return Models.AssetGraphBinding.newInstance({nodeId: id});
    }

    updateAlertCountActive(active: boolean)
    {
        let alertCountField = this.asAlertCountField();
        if (alertCountField)
        {
            let isDefault              = this.data.defaultLabel === alertCountField.label || !alertCountField.label;
            alertCountField.onlyActive = active;
            if (isDefault) alertCountField.label = this.data.defaultLabel;
        }
    }
}
