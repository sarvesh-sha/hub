import {Component, ViewChild} from "@angular/core";

import {TimeSeriesChartConfigurationExtended} from "app/customer/visualization/time-series-utils";
import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import {AssetGraphExtended} from "app/services/domain/asset-graph.service";
import * as Models from "app/services/proxy/model/models";
import {DataAggregationType} from "app/shared/aggregation/data-aggregation.component";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {MultipleGraphConfigurationComponent} from "app/shared/assets/configuration/multiple-graph-configuration.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";
import {mapInParallelNoNulls} from "framework/utils/concurrency";

@Component({
               selector   : "o3-widget-editor-wizard-graphs-step",
               templateUrl: "./widget-editor-wizard-graphs-step.component.html",
               providers  : [WizardStep.createProvider(WidgetEditorWizardGraphsStepComponent)]
           })
export class WidgetEditorWizardGraphsStepComponent extends WizardStep<WidgetEditorWizardState>
{
    @ViewChild("test_localGraphs") test_localGraphs: MultipleGraphConfigurationComponent;

    checkingForGps: boolean = false;

    get assetStructureSelectorWidget(): Models.AssetGraphSelectorWidgetConfiguration
    {
        return this.data.editor.widget instanceof Models.AssetGraphSelectorWidgetConfiguration && this.data.editor.widget;
    }

    get dataAggregationWidget(): Models.AggregationTableWidgetConfiguration
    {
        return this.data.editor.widget instanceof Models.AggregationTableWidgetConfiguration && this.data.editor.widget;
    }

    get graphsHost(): GraphConfigurationHost
    {
        return this.dataAggregationWidget ? this.data.editor.localGraphsHost : this.data.editor.dashboardGraphsHost;
    }

    public getLabel(): string
    {
        let label = "Edit Asset Structure";
        if (!this.dataAggregationWidget) label += "s";
        return label;
    }

    public isEnabled(): boolean
    {
        if (this.data.editor.widget instanceof Models.AggregationTableWidgetConfiguration)
        {
            return this.data.editor.dataAggregationExt.type === DataAggregationType.Bindings;
        }
        if (this.data.editor.widget instanceof Models.TimeSeriesWidgetConfiguration)
        {
            return this.data.editor.widget.charts.some((chart) => chart.type === Models.TimeSeriesChartType.COORDINATE);
        }

        return this.data.editor.allowWidgetTypes(Models.AssetGraphSelectorWidgetConfiguration);
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        if (this.assetStructureSelectorWidget && this.checkingForGps) return false;

        let checker = this.dataAggregationWidget ? this.data.editor.localGraphsChecker : this.data.editor.dashboardGraphsChecker;
        return checker?.isValid;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected(): Promise<void>
    {
    }

    public async onData(): Promise<void>
    {
        await super.onData();

        await this.graphUpdated();
    }

    async graphUpdated()
    {
        const dataAggregation = this.dataAggregationWidget;
        if (dataAggregation && this.data.editor.dataAggregationExt.type === DataAggregationType.Bindings)
        {
            this.data.editor.dataAggregationExt.updateGraph(this.graphsHost.getGraphs()[0]?.graph);
        }
        else
        {
            try
            {
                this.checkingForGps = true;
                let gpsRecords      = await TimeSeriesChartConfigurationExtended.getAvailableGps(this.data.app.domain);
                let gpsIds          = gpsRecords.map((recordId) => recordId.sysId);

                if (gpsIds.length)
                {
                    this.data.editor.validExternalGpsBindings = await mapInParallelNoNulls(this.data.editor.dashboardGraphsHost.getGraphs(), async (graph) =>
                    {
                        let graphExt = new AssetGraphExtended(this.data.app.domain, graph.graph);
                        let resolved = await graphExt.resolve();

                        let nodeId = resolved.findNodeIdWithIntersectingAssets(gpsIds);
                        return nodeId ? Models.AssetGraphBinding.newInstance({
                                                                                 graphId: graph.id,
                                                                                 nodeId : nodeId
                                                                             }) : null;
                    });
                }
                else
                {
                    this.data.editor.validExternalGpsBindings = [];
                }
            }
            catch (e)
            {
                this.data.editor.validExternalGpsBindings = [];
            }
            finally
            {
                this.checkingForGps = false;
                this.markForCheck();
            }
        }
    }
}
