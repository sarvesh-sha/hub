import {Component} from "@angular/core";

import * as Models from "app/services/proxy/model/models";
import {DataSourceWizardPurpose, DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-data-source-wizard-pivot-table-step",
               templateUrl: "./data-source-wizard-pivot-table-step.component.html",
               styleUrls  : ["./data-source-wizard-pivot-table-step.component.scss"],
               providers  : [WizardStep.createProvider(DataSourceWizardPivotTableStepComponent)]
           })
export class DataSourceWizardPivotTableStepComponent extends WizardStep<DataSourceWizardState>
{
    public height: number = 0;

    public getLabel(): string
    {
        return "Column Structure";
    }

    public isEnabled(): boolean
    {
        return this.data.purpose === DataSourceWizardPurpose.visualization && this.data.type === Models.TimeSeriesChartType.HIERARCHICAL;
    }

    public isValid(): boolean
    {
        return this.data.pivotTable && !!this.data.hierarchy?.virtualNodes.length;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }

    public updateHeight(heightPx: number)
    {
        this.height = heightPx;
        this.wizard?.detectChanges();
    }

    public onColumnPivot()
    {
        this.data.hierarchy.virtualNodes = this.data.pivot.virtualNodes;
        this.data.updatePivotTable();
    }
}
