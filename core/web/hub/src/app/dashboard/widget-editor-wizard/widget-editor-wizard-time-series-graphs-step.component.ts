import {Component, ElementRef, QueryList, ViewChildren} from "@angular/core";
import {UUID} from "angular2-uuid";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-time-series-graphs-step",
               templateUrl: "./widget-editor-wizard-time-series-graphs-step.component.html",
               styleUrls  : [
                   "./widget-editor-wizard-dialog.component.scss",
                   "./widget-editor-wizard-time-series-graphs-step.component.scss"
               ],
               providers  : [WizardStep.createProvider(WidgetEditorWizardTimeSeriesGraphsComponent)]
           })
export class WidgetEditorWizardTimeSeriesGraphsComponent extends WizardStep<WidgetEditorWizardState>
{
    public static readonly newSelectorId = "new_selector_id";

    gpsClassId: string;

    externalBindingInfo: ExternalBindingInfo[]              = [];
    graphToNewSelectorNameLookup: Lookup<Lookup<string>>    = {};
    existingSelectorNames: Map<string, Set<string>>         = new Map();
    graphToSelectorOptions: Lookup<ControlOption<string>[]> = {};
    gpsGraphOptions: ControlOption<string>[]                = [];

    get externalGpsBindingsAvailable(): boolean
    {
        return !!this.data.editor.validExternalGpsBindings.length;
    }

    private get typedWidget(): Models.TimeSeriesWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.TimeSeriesWidgetConfiguration);
    }

    @ViewChildren("test_newSelectorName", {read: ElementRef}) test_newSelectorNames: QueryList<ElementRef<HTMLElement>>;

    public getLabel(): string
    {
        return "Selectors";
    }

    public isEnabled(): boolean
    {
        let widget = this.typedWidget;
        if (!widget) return false;

        let hasExternalGraphs = this.data.editor.graphLookup?.size;
        for (let chart of widget.charts)
        {
            if (hasExternalGraphs && chart.type === Models.TimeSeriesChartType.COORDINATE)
            {
                if (this.externalGpsBindingsAvailable) return true;
            }
            if (chart.graph?.externalBindings?.length) return true;
        }
        return false;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        let widget = this.typedWidget;
        if (!widget) return false;

        for (let configidx = 0; configidx < widget.charts.length; configidx++)
        {
            let chart = widget.charts[configidx];
            switch (chart.type)
            {
                case Models.TimeSeriesChartType.GRAPH:
                case Models.TimeSeriesChartType.STANDARD:
                    for (let externalBinding of chart.graph?.externalBindings || [])
                    {
                        if (!this.externalBindingValid(externalBinding)) return false;
                    }
                    break;

                case Models.TimeSeriesChartType.COORDINATE:
                    let bindingInfo = this.externalBindingInfo[configidx];
                    if (bindingInfo?.hasExternalGpsBinding)
                    {
                        let externalBinding = chart.graph.externalBindings[0];
                        if (!this.externalBindingValid(externalBinding)) return false;
                    }
                    break;
            }
        }

        if (!this.graphToNewSelectorNameLookup) return false;
        for (let graphId in this.graphToNewSelectorNameLookup)
        {
            let existingNames = this.existingSelectorNames.get(graphId);
            for (let newName in this.graphToNewSelectorNameLookup[graphId])
            {
                if (existingNames.has(newName)) return false;
            }
        }

        return true;
    }

    private externalBindingValid(externalBinding: Models.AssetGraphBinding): boolean
    {
        if (!externalBinding) return false;
        if (!externalBinding.selectorId) return false;
        return externalBinding.selectorId !== WidgetEditorWizardTimeSeriesGraphsComponent.newSelectorId;
    }

    public onNext(): Promise<boolean>
    {
        return undefined;
    }

    private equivalentBindingInfo(newBindingInfo: ExternalBindingInfo[]): boolean
    {
        if (this.externalBindingInfo.length !== newBindingInfo.length) return false;

        for (let i = 0; i < this.externalBindingInfo.length; i++)
        {
            if (!this.externalBindingInfo[i].equals(newBindingInfo[i])) return false;
        }

        return true;
    }

    public async onData(): Promise<void>
    {
        super.onData();

        if (!this.gpsClassId)
        {
            this.gpsClassId = `${await this.data.app.domain.normalization.getWellKnownEquipmentClassId(Models.WellKnownEquipmentClass.GPS)}`;
        }
    }

    public async onStepSelected()
    {
        await this.updateOptions();
    }

    public async updateOptions()
    {
        await this.updateSelectorOptions();
        let newBindingInfo = this.typedWidget.charts.map((config) => new ExternalBindingInfo(this, config, this.graphToSelectorOptions, this.data.editor.dashboardGraphsHost));
        if (!this.equivalentBindingInfo(newBindingInfo)) this.externalBindingInfo = newBindingInfo;
        this.syncSelectorIds();

        this.markForCheck();
    }

    private async updateSelectorOptions()
    {
        for (let chart of this.typedWidget.charts)
        {
            for (let binding of chart.graph?.externalBindings || [])
            {
                let graphId = binding.graphId;
                if (!this.graphToSelectorOptions[graphId])
                {
                    let options                          = await this.data.editor.dashboardExt.getSelectorOptions(graphId, true);
                    this.graphToSelectorOptions[graphId] = options;

                    let existingNames = new Set<string>();
                    for (let option of options)
                    {
                        let hyphenIndex = option.label.indexOf("-");
                        existingNames.add(option.label.substring(0, hyphenIndex - 1));
                    }
                    this.existingSelectorNames.set(graphId, existingNames);

                    options.unshift(new ControlOption(WidgetEditorWizardTimeSeriesGraphsComponent.newSelectorId, "New Selector"));
                }
            }
        }

        if (this.externalGpsBindingsAvailable)
        {
            let graphs           = this.data.editor.dashboardGraphsHost.getGraphs();
            this.gpsGraphOptions = this.data.editor.validExternalGpsBindings.map((binding) =>
                                                                                 {
                                                                                     let graphId   = binding.graphId;
                                                                                     let graphName = graphs.find((graph) => graph.id === graphId).name;
                                                                                     return new ControlOption(graphId, graphName);
                                                                                 });
        }
    }

    public syncSelectorIds()
    {
        this.graphToNewSelectorNameLookup = {};
        for (let bindingInfo of this.externalBindingInfo)
        {
            for (let binding of bindingInfo.bindings)
            {
                let newSelectorNameToId = this.graphToNewSelectorNameLookup[binding.graphId];
                if (!newSelectorNameToId)
                {
                    this.graphToNewSelectorNameLookup[binding.graphId] = newSelectorNameToId = {};
                }

                if (binding.newSelectorChosen && binding.newSelectorName)
                {
                    let selectorId        = newSelectorNameToId[binding.newSelectorName] || UUID.UUID();
                    binding.newSelectorId = newSelectorNameToId[binding.newSelectorName] = selectorId;
                }
                else
                {
                    binding.newSelectorName = "";
                    binding.newSelectorId   = null;
                }
            }
        }

        if (this.isValid())
        {
            this.data.editor.newSelectorNameLookup = {};
            for (let graphId in this.graphToNewSelectorNameLookup)
            {
                let newSelectorNameToId = this.graphToNewSelectorNameLookup[graphId];
                for (let name in newSelectorNameToId)
                {
                    this.data.editor.newSelectorNameLookup[newSelectorNameToId[name]] = Models.SharedAssetSelector.newInstance({name: name});
                }
            }

            this.data.editor.timeSeriesCharts = this.typedWidget.charts.map((chart) => Models.TimeSeriesChartConfiguration.deepClone(chart));
        }
    }
}

class ExternalBindingInfo
{
    private readonly m_graphLookup: Lookup<Models.SharedAssetGraph>;

    readonly bindings: ExternalBinding[] = [];

    get forGpsMap(): boolean
    {
        return this.config.type === Models.TimeSeriesChartType.COORDINATE;
    }

    private readonly m_gpsBinding: Models.AssetGraphBinding = new Models.AssetGraphBinding();

    get gpsGraphId(): string
    {
        return this.m_gpsBinding.graphId;
    }

    set gpsGraphId(graphId: string)
    {
        this.updateGpsBinding(graphId);
        this.gpsBindingChanged();
    }

    set hasExternalGpsBinding(external: boolean)
    {
        if (external)
        {
            if (this.config.graph.externalBindings.length === 0) this.config.graph.externalBindings[0] = this.m_gpsBinding;
        }
        else
        {
            if (this.config.graph.externalBindings.length > 0)
            {
                this.config.graph.externalBindings.pop();
                this.bindings.pop();
            }
        }

        if (this.forGpsMap) this.m_host.updateOptions();
    }

    get hasExternalGpsBinding(): boolean
    {
        return !!this.config.graph.externalBindings.length;
    }

    constructor(private readonly m_host: WidgetEditorWizardTimeSeriesGraphsComponent,
                public readonly config: Models.TimeSeriesChartConfiguration,
                private readonly m_graphToSelectorOptions: Lookup<ControlOption<string>[]>,
                private readonly m_externalGraphHost: GraphConfigurationHost)
    {
        if (!config.graph) config.graph = new Models.TimeSeriesGraphConfiguration();
        if (!config.graph.externalBindings) config.graph.externalBindings = [];
        if (config.type === Models.TimeSeriesChartType.COORDINATE && this.hasExternalGpsBinding) this.m_gpsBinding = this.config.graph.externalBindings[0];

        this.m_graphLookup = UtilsService.extractLookup(m_externalGraphHost.getGraphs() || []);
        if (this.m_gpsBinding && !this.m_gpsBinding.graphId && this.m_host.gpsGraphOptions?.length) this.updateGpsBinding(this.m_host.gpsGraphOptions[0].id);

        let graphIdToBindings: Lookup<Models.AssetGraphBinding[]> = {};
        for (let externalBinding of config.graph.externalBindings)
        {
            let graphId = externalBinding.graphId;
            if (!graphIdToBindings[graphId])
            {
                graphIdToBindings[graphId] = [];
            }

            graphIdToBindings[graphId].push(externalBinding);
        }

        for (let graphId in graphIdToBindings)
        {
            this.bindings.push(new ExternalBinding(this.m_graphLookup[graphId], graphIdToBindings[graphId], this.m_graphToSelectorOptions[graphId] || []));
        }
        this.bindings = this.bindings.sort((bindingA,
                                            bindingB) => UtilsService.compareStrings(bindingA.graphName, bindingB.graphName, true));
    }

    private updateGpsBinding(graphId: string)
    {
        if (this.m_gpsBinding.graphId !== graphId)
        {
            this.m_gpsBinding.graphId = graphId;
            this.m_gpsBinding.nodeId  = this.m_host.data.editor.validExternalGpsBindings.find((binding) => binding.graphId === graphId).nodeId;
        }
    }

    async gpsBindingChanged()
    {
        let graphId = this.m_gpsBinding.graphId;
        if (graphId && this.m_gpsBinding.nodeId)
        {
            if (!this.bindings[0] || this.bindings[0].graphId !== graphId)
            {
                this.bindings[0] = new ExternalBinding(this.m_graphLookup[graphId], this.config.graph.externalBindings, this.m_graphToSelectorOptions[graphId] || []);
            }
        }

        if (this.forGpsMap) await this.m_host.updateOptions();
    }

    equals(other: ExternalBindingInfo): boolean
    {
        if (!other) return false;
        if (this.bindings.length !== other.bindings.length) return false;

        for (let i = 0; i < this.bindings.length; i++)
        {
            if (!this.bindings[i].equals(other.bindings[i])) return false;
        }

        return true;
    }
}

class ExternalBinding
{
    get graphId(): string
    {
        return this.graph?.id;
    }

    get graphName(): string
    {
        return this.graph?.name;
    }

    get numSourcesTooltip(): string
    {
        return `${this.externalBindings.length} bound ${UtilsService.pluralize("source", this.externalBindings.length)}`;
    }

    private m_selectorId: string;
    get selectorId(): string
    {
        return this.m_selectorId;
    }

    set selectorId(selectorId: string)
    {
        this.m_selectorId = selectorId;

        this.syncSelectorId();
    }

    private m_newSelectorId: string;
    set newSelectorId(newSelectorId: string)
    {
        this.m_newSelectorId = newSelectorId;

        this.syncSelectorId();
    }

    get newSelectorChosen(): boolean
    {
        return this.m_selectorId === WidgetEditorWizardTimeSeriesGraphsComponent.newSelectorId;
    }

    newSelectorName: string = "";

    constructor(private readonly graph: Models.SharedAssetGraph,
                private readonly externalBindings: Models.AssetGraphBinding[],
                options: ControlOption<string>[])
    {
        let selectorId = externalBindings.find((binding) => binding.selectorId)?.selectorId;
        if (!options.find((option) => option.id === selectorId)) selectorId = null;

        if (!selectorId)
        {
            let selectedIdx = options.length > 1 ? 1 : 0;
            selectorId      = options[selectedIdx]?.id;
        }

        this.selectorId = selectorId;
    }

    private syncSelectorId()
    {
        let selectorId = this.newSelectorChosen && this.m_newSelectorId ? this.m_newSelectorId : this.m_selectorId;
        for (let binding of this.externalBindings)
        {
            binding.selectorId = selectorId;
        }
    }

    equals(other: ExternalBinding): boolean
    {
        if (this.graphId !== other.graphId) return false;
        if (this.selectorId !== other.selectorId) return false;

        return UtilsService.compareArraysAsSets(this.externalBindings.map((binding) => binding.nodeId),
                                                other.externalBindings.map((binding) => binding.nodeId));
    }
}
