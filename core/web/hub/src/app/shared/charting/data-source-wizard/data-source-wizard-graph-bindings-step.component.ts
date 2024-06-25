import {ChangeDetectionStrategy, Component, Injector, ViewChild} from "@angular/core";
import {UUID} from "angular2-uuid";

import {AppContext} from "app/app.service";
import {AssetGraphTreeNode, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {DashboardConfigurationExtended} from "app/services/domain/dashboard-management.service";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {DataSourceWizardPurpose, DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";

import {ControlOption} from "framework/ui/control-option";
import {SelectComponent} from "framework/ui/forms/select.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector       : "o3-data-source-wizard-graph-bindings-step",
               templateUrl    : "./data-source-wizard-graph-bindings-step.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush,
               providers      : [WizardStep.createProvider(DataSourceWizardGraphBindingsStepComponent)]
           })
export class DataSourceWizardGraphBindingsStepComponent extends WizardStep<DataSourceWizardState>
{
    private m_dashboard: DashboardConfigurationExtended;

    public localGraphs: Map<string, SharedAssetGraphExtended>;
    public externalGraphs: Map<string, SharedAssetGraphExtended>;
    public graphGroups: GraphConfigurationHost[];

    private m_currSelectorNames                     = new Set<string>();
    public selectorOptions: ControlOption<string>[] = [];

    showLocalGraphs: boolean;
    showExternalGraphs: boolean;

    get requireSiblings(): boolean
    {
        if (!this.forVisualization) return false;
        return this.data.type === Models.TimeSeriesChartType.HIERARCHICAL;
    }

    get isHierarchical(): boolean
    {
        return this.data.type === Models.TimeSeriesChartType.HIERARCHICAL;
    }

    get newSelectorChosen(): boolean
    {
        let newSelectorId = this.selectorOptions[0]?.id;
        return newSelectorId && this.data.selectorId === newSelectorId;
    }

    get showSelectorFields(): boolean
    {
        if (this.data.purpose !== DataSourceWizardPurpose.dashboard) return false;
        if (!this.selectorOptions.length) return false;
        if (this.data.type !== Models.TimeSeriesChartType.GRAPH && this.data.overrideType !== Models.TimeSeriesChartType.GRAPH) return false;
        return !this.data.locallyBound;
    }

    get forVisualization(): boolean
    {
        return this.data.purpose === DataSourceWizardPurpose.visualization;
    }

    @ViewChild("test_bindingLocal") test_bindingLocal: SelectComponent<string>;
    @ViewChild("test_bindingsLocal") test_bindingsLocal: SelectComponent<string>;
    @ViewChild("test_bindingExternal") test_bindingExternal: SelectComponent<string>;
    @ViewChild("test_bindingsExternal") test_bindingsExternal: SelectComponent<string>;
    @ViewChild("test_selector") test_selector: SelectComponent<string>;

    constructor(inj: Injector,
                private m_app: AppContext)
    {
        super(inj);
    }

    public getLabel(): string
    {
        let label = "Select Binding";
        if (this.forVisualization) label += "s";
        return label;
    }

    public isEnabled(): boolean
    {
        if (!this.data) return false;
        if (this.data.type === Models.TimeSeriesChartType.GRAPH && !this.data.overrideType) return true;
        if (this.isHierarchical) return true;

        return this.forVisualization && this.data.type === Models.TimeSeriesChartType.STANDARD && this.data.overrideType === Models.TimeSeriesChartType.GRAPH;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        if (this.showLocalGraphs)
        {
            if (this.data.locallyBound && !this.data.hierarchy?.bindings?.length) return false;
        }

        if (this.showExternalGraphs)
        {
            if (this.data.purpose === DataSourceWizardPurpose.dashboard && !this.data.locallyBound)
            {
                if (!this.data.graphBinding?.selectorId) return false;
                if (this.newSelectorChosen)
                {
                    if (!this.data.newSelectorName) return false;
                    if (this.m_currSelectorNames.has(this.data.newSelectorName)) return false;
                }
            }
        }

        if (this.forVisualization) return !!this.data.graphBindings.length;
        return !!this.data.graphBinding;
    }

    public async onNext()
    {
        return false;
    }

    public async onStepSelected()
    {
        await this.updateGraph();
    }

    public async onData(): Promise<void>
    {
        await super.onData();

        this.updateGraph();
        this.getSelectors();
    }

    public async graphSelectionChanged(isLocal?: boolean)
    {
        if (isLocal == undefined)
        {
            let newBinding = AssetGraphTreeNode.getBinding(this.data.graphSelection);
            isLocal        = this.localGraphs?.has(newBinding.graphId);
        }

        this.data.locallyBound = isLocal;
        if (isLocal)
        {
            if (this.data.graphBinding) this.data.hierarchy.bindings = [Models.HierarchicalVisualizationBinding.newInstance({leafNodeId: this.data.graphBinding.nodeId})];
        }
        else
        {
            await this.getSelectors();
        }
    }

    public async graphSelectionsChanged()
    {
        await this.data.rebuildPivotTable(this.m_app, []);
        await this.wizard?.markForCheck();
    }

    public selectorIdChanged()
    {
        if (this.newSelectorChosen)
        {
            this.data.newSelectorName = this.m_dashboard.getUniqueSelectorName(this.data.graphBinding.graphId);
        }
        else
        {
            this.data.newSelectorName = null;
        }
    }

    private async updateGraph()
    {
        this.showLocalGraphs = this.showExternalGraphs = false;
        if (this.data.graphsHost)
        {
            this.localGraphs     = await this.data.graphsHost.resolveGraphs();
            this.showLocalGraphs = this.localGraphs.size && this.data.graphsChecker?.isValid;
        }
        if (!this.forVisualization || this.data.type !== Models.TimeSeriesChartType.HIERARCHICAL)
        {
            this.externalGraphs     = await this.data.externalGraphsHost?.resolveGraphs();
            this.showExternalGraphs = this.externalGraphs?.size && this.data.externalGraphsChecker?.isValid;
        }

        if (this.showLocalGraphs && this.showExternalGraphs)
        {
            this.graphGroups = [
                this.data.graphsHost,
                this.data.externalGraphsHost
            ];
        }

        this.detectChanges();
    }


    private async getSelectors()
    {
        if (this.data.purpose === DataSourceWizardPurpose.dashboard && !this.m_dashboard)
        {
            this.m_dashboard = this.m_app.domain.dashboard.currentDashboardConfig.getValue();
        }

        if (this.m_dashboard)
        {
            let graphId = this.data.graphBinding?.graphId;
            if (graphId)
            {
                let contextOptions = await this.m_dashboard.getGraphOptions(graphId);

                this.m_currSelectorNames = new Set();
                let selectorOptions      = await this.m_dashboard.getSelectorOptions(graphId);
                this.selectorOptions     = await mapInParallel(selectorOptions, async (selectorOption) =>
                {
                    this.m_currSelectorNames.add(selectorOption.label);
                    let currContext       = <Models.AssetGraphContextAsset>await this.m_dashboard.getGraphContext(selectorOption.id);
                    let currContextOption = contextOptions.find((contextOption) => contextOption.id === currContext.sysId);

                    if (currContextOption)
                    {
                        selectorOption.label += " - " + currContextOption.label;
                    }

                    return selectorOption;
                });

                this.selectorOptions.unshift(new ControlOption(UUID.UUID(), "New Selector"));

                if (!this.data.graphBinding.selectorId)
                {
                    let selectedIdx                   = this.selectorOptions.length === 1 ? 0 : 1;
                    this.data.graphBinding.selectorId = this.selectorOptions[selectedIdx].id;
                }

                this.markForCheck();
            }
        }
    }
}
