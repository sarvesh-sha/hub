import {ChangeDetectionStrategy, Component, ViewChild} from "@angular/core";

import {AssetGraphTreeNode} from "app/services/domain/asset-graph.service";
import * as Models from "app/services/proxy/model/models";
import {MultipleGraphConfigurationComponent} from "app/shared/assets/configuration/multiple-graph-configuration.component";
import {DataSourceWizardPurpose, DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

import {Subscription} from "rxjs";

@Component({
               selector       : "o3-data-source-wizard-graphs-step",
               templateUrl    : "./data-source-wizard-graphs-step.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush,
               providers      : [WizardStep.createProvider(DataSourceWizardGraphsStepComponent)]
           })
export class DataSourceWizardGraphsStepComponent extends WizardStep<DataSourceWizardState>
{
    stepSelected = false;

    get localGraphHeader(): string
    {
        let prefix = "Asset Structure";
        if (!this.singularLocalGraph) prefix += "s";

        return `${prefix} Local To This ${this.data.graphsHost.hostContext}`;
    }

    get showInternalGraphs(): boolean
    {
        if (!this.data.graphsHost) return false;
        return this.data.purpose !== DataSourceWizardPurpose.report;
    }

    get showExternalGraphs(): boolean
    {
        if (!this.data.externalGraphsHost) return false;
        if (this.data.purpose !== DataSourceWizardPurpose.visualization) return true;
        return this.data.type !== Models.TimeSeriesChartType.HIERARCHICAL;
    }

    singularLocalGraph: boolean = false;
    readonlyLocalGraphNames: boolean;
    private m_graphChangeListener: Subscription;
    private graphsResolvedListener: Subscription;

    @ViewChild("test_localGraphs") test_localGraphs: MultipleGraphConfigurationComponent;
    @ViewChild("test_externalGraphs") test_externalGraphs: MultipleGraphConfigurationComponent;

    public getLabel()
    {
        let label = "Edit Asset Structure";
        if (!this.singularLocalGraph) label += "s";
        return label;
    }

    public isEnabled()
    {
        if (!this.data.graphsHost && !this.data.externalGraphsHost) return false;

        switch (this.data.type)
        {
            case Models.TimeSeriesChartType.STANDARD:
                return this.data.purpose === DataSourceWizardPurpose.visualization && this.data.overrideType === Models.TimeSeriesChartType.GRAPH;

            case Models.TimeSeriesChartType.GRAPH:
                return !this.data.overrideType;

            case Models.TimeSeriesChartType.GRAPH_SCATTER:
                return true;

            case Models.TimeSeriesChartType.HIERARCHICAL:
                return true;

            default:
                return false;
        }
    }

    public isValid()
    {
        if (this.data.purpose !== DataSourceWizardPurpose.visualization && this.data.type == Models.TimeSeriesChartType.GRAPH)
        {
            if (!this.data.externalGraphsHost && !this.data.localGraph) return false;
        }

        return this.data.externalGraphsChecker?.isValid || this.data.graphsChecker?.isValid;
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
        let maintainLocalGraph = false;
        if (this.data.purpose === DataSourceWizardPurpose.visualization)
        {
            this.readonlyLocalGraphNames = false;
            if (this.data.type === Models.TimeSeriesChartType.HIERARCHICAL)
            {
                this.singularLocalGraph = true;
                maintainLocalGraph      = true;
            }
        }
        else if (this.data.graphsHost)
        {
            this.readonlyLocalGraphNames = true;
            this.singularLocalGraph      = true;
            maintainLocalGraph           = true;
            this.data.localGraph         = this.data.graphsHost.getGraphs()[0]?.graph;
        }

        if (this.m_graphChangeListener) this.m_graphChangeListener.unsubscribe();
        if (this.data.graphsHost)
        {
            this.m_graphChangeListener = this.subscribeToObservable(this.data.graphsHost.graphsChanged, async () =>
            {
                if (maintainLocalGraph)
                {
                    this.data.localGraph = this.data.graphsHost.getGraphs()[0].graph;

                    const localNodeIds = new Set<string>(this.data.localGraph.nodes.map((node) => node.id));
                    if (this.data.hierarchy.bindings.some((binding) => !localNodeIds.has(binding.leafNodeId)))
                    {
                        this.data.hierarchy.bindings = [];
                        this.data.graphBindings      = [];
                    }
                }
                else if (this.data.purpose === DataSourceWizardPurpose.visualization)
                {
                    let existingGraphs             = await this.data.graphsHost.resolveGraphs();
                    this.data.localGraphSelections = this.data.localGraphSelections.filter((id) =>
                                                                                           {
                                                                                               let binding = AssetGraphTreeNode.getBinding(id);
                                                                                               let graph   = existingGraphs.get(binding.graphId);
                                                                                               return !!graph?.getNodeById(binding.nodeId);
                                                                                           });
                }
            });
        }

        if (!this.graphsResolvedListener)
        {
            this.graphsResolvedListener = this.subscribeToObservable(this.data.graphsResolved, () => this.markForCheck());
        }

        this.stepSelected = true;
    }
}
