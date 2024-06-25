import {Component, ElementRef, ViewChild} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {DataAggregationType} from "app/shared/aggregation/data-aggregation.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-data-aggregation-type-step",
               templateUrl: "./widget-editor-wizard-data-aggregation-type-step.component.html",
               styleUrls  : ["./widget-editor-wizard-dialog.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardDataAggregationTypeStepComponent)]
           })
export class WidgetEditorWizardDataAggregationTypeStepComponent extends WizardStep<WidgetEditorWizardState>
{
    typeSelections: Set<DataAggregationType>;

    @ViewChild("test_types", {read: ElementRef}) test_types: ElementRef;

    public typeChanged()
    {
        let type = this.typeSelections.size && this.typeSelections.values()
                                                   .next().value;
        if (!type) return;

        let dataAggregationExt  = this.data.editor.dataAggregationExt;
        dataAggregationExt.type = type;
        switch (type)
        {
            case DataAggregationType.Bindings:
                this.data.editor.initBindingsTable();
                break;

            case DataAggregationType.Groups:
                dataAggregationExt.model.groups           = this.data.editor.controlPointGroups;
                dataAggregationExt.model.filterableRanges = this.data.editor.selectedRanges;
                break;
        }
    }

    public getLabel(): string
    {
        return "Data Aggregation Type";
    }

    public isEnabled(): boolean
    {
        return this.data.editor.allowWidgetTypes(Models.AggregationTableWidgetConfiguration);
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
        if (!this.typeSelections) this.typeSelections = new Set([this.data.editor.dataAggregationExt.type]);
    }
}
