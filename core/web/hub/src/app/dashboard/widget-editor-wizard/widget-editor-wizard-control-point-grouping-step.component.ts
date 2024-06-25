import {Component, ViewChild} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import {ControlPointsGroupExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {DataAggregationType} from "app/shared/aggregation/data-aggregation.component";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {ControlPointGroupingStepComponent} from "app/shared/assets/control-point-grouping-step/control-point-grouping-step.component";

import {UtilsService} from "framework/services/utils.service";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-control-point-grouping-step",
               templateUrl: "./widget-editor-wizard-control-point-grouping-step.component.html",
               providers  : [WizardStep.createProvider(WidgetEditorWizardControlPointGroupingStepComponent)]
           })
export class WidgetEditorWizardControlPointGroupingStepComponent extends WizardStep<WidgetEditorWizardState>
{
    get aggregationSummaryWidget(): Models.AggregationWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.AggregationWidgetConfiguration);
    }

    get aggregationTableWidget(): Models.AggregationTableWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.AggregationTableWidgetConfiguration);
    }

    get aggregationTrendWidget(): Models.AggregationTrendWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.AggregationTrendWidgetConfiguration);
    }

    get visualizationMode(): Models.HierarchicalVisualizationType
    {
        return this.aggregationTableWidget?.visualizationMode;
    }

    set visualizationMode(mode: Models.HierarchicalVisualizationType)
    {
        let aggTable = this.aggregationTableWidget;
        if (aggTable) aggTable.visualizationMode = mode;
    }

    get graphsHost(): GraphConfigurationHost
    {
        if (this.data.editor.widget instanceof Models.AggregationWidgetConfiguration)
        {
            return this.data.editor.dashboardGraphsHost;
        }

        return null;
    }

    @ViewChild("test_groupingStep") test_groupingStep: ControlPointGroupingStepComponent;

    public getLabel(): string
    {
        let label = "Control Point Group";
        if (this.aggregationSummaryWidget) return label;
        return label + "s";
    }

    public isValid(): boolean
    {
        if (!this.data.editor.controlPointGroups.length) return false;
        return this.data.editor.controlPointGroups.every((group) => ControlPointsGroupExtended.isValid(group, !!this.aggregationTableWidget, false, !this.aggregationSummaryWidget, true));
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public async onStepSelected()
    {
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public isEnabled(): boolean
    {
        if (this.data.editor.allowWidgetTypes(Models.AggregationTableWidgetConfiguration)) return this.data.editor.dataAggregationExt.type === DataAggregationType.Groups;
        return this.data.editor.allowWidgetTypes(Models.AggregationWidgetConfiguration, Models.AggregationTrendWidgetConfiguration);
    }
}
