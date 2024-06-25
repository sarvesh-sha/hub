import {Component, ViewChild} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {DataAggregationExtended, DataAggregationType} from "app/shared/aggregation/data-aggregation.component";
import {AggregationNodeBindingConfigurerComponent} from "app/shared/assets/aggregation-node-binding/aggregation-node-binding-configurer.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-aggregation-node-configurer-step",
               templateUrl: "./widget-editor-wizard-aggregation-node-configurer-step.component.html",
               styleUrls  : ["./widget-editor-wizard-dialog.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardAggregationNodeConfigurerComponent)]
           })
export class WidgetEditorWizardAggregationNodeConfigurerComponent extends WizardStep<WidgetEditorWizardState>
{
    get typedWidget(): Models.AggregationTableWidgetConfiguration
    {
        return this.data.editor.widget instanceof Models.AggregationTableWidgetConfiguration && this.data.editor.widget;
    }

    get visualizationMode(): Models.HierarchicalVisualizationType
    {
        return this.typedWidget.visualizationMode || Models.HierarchicalVisualizationType.TABLE;
    }

    set visualizationMode(mode: Models.HierarchicalVisualizationType)
    {
        this.typedWidget.visualizationMode = mode;
    }

    @ViewChild("test_nodeBindingConfigurer") test_nodeBindingConfigurer: AggregationNodeBindingConfigurerComponent;

    public getLabel(): string
    {
        return "Aggregation Nodes";
    }

    public isEnabled(): boolean
    {
        if (!this.data.editor.allowWidgetTypes(Models.AggregationTableWidgetConfiguration)) return false;
        return this.data.editor.dataAggregationExt.type === DataAggregationType.Bindings;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        let widgetExt = new DataAggregationExtended(Models.AggregationTableWidgetConfiguration.deepClone(this.typedWidget), false);
        return widgetExt.type === DataAggregationType.Bindings && widgetExt.isValid();
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
