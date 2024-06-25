import {Component, Injector, ViewChild} from "@angular/core";

import {AppContext} from "app/app.service";
import {ControlPointsSelectionExtended} from "app/services/domain/report-definitions.service";
import * as Models from "app/services/proxy/model/models";
import {DataSourceWizardPurpose, DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";
import {ControlPointSelectorComponent} from "app/shared/search/control-point-selector.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-data-source-wizard-points-step",
               templateUrl: "./data-source-wizard-points-step.component.html",
               providers  : [WizardStep.createProvider(DataSourceWizardPointsStepComponent)]
           })
export class DataSourceWizardPointsStepComponent extends WizardStep<DataSourceWizardState>
{
    public selection: ControlPointsSelectionExtended;

    get maxSelection(): number
    {
        return this.data.singleSelect ? 1 : 200;
    }

    @ViewChild("test_cpSelector") test_cpSelector: ControlPointSelectorComponent;

    constructor(inj: Injector,
                private m_app: AppContext)
    {
        super(inj);

        this.selection = new ControlPointsSelectionExtended(this.m_app.domain);
    }

    async onData()
    {
        await super.onData();

        this.prepareSelection();
    }

    public getLabel()
    {
        return "Pick Control Points";
    }

    public isEnabled()
    {
        switch (this.data.type)
        {
            case Models.TimeSeriesChartType.STANDARD:
                return !this.data.overrideType;

            case Models.TimeSeriesChartType.SCATTER:
                return true;

            case Models.TimeSeriesChartType.GRAPH:
                return this.data.purpose === DataSourceWizardPurpose.visualization && this.data.overrideType === Models.TimeSeriesChartType.STANDARD;

            default:
                return false;
        }
    }

    public isValid()
    {
        switch (this.data.type)
        {
            case Models.TimeSeriesChartType.GRAPH:
                return true;

            case Models.TimeSeriesChartType.STANDARD:
                return this.selection.count > 0;

            case Models.TimeSeriesChartType.SCATTER:
                return this.selection.count > 1;

            default:
                return false;
        }
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
        this.prepareSelection();
    }

    public sync(identities: Models.RecordIdentity[])
    {
        if (identities.length >= this.maxSelection)
        {
            this.selection.setIdentities(identities.slice(0, this.maxSelection));
        }

        this.data.ids = identities;
    }

    private prepareSelection()
    {
        this.selection = new ControlPointsSelectionExtended(this.m_app.domain);
        for (let record of this.data.ids) this.selection.select(record);
    }
}
