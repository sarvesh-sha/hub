import {Injectable} from "@angular/core";
import {UUID} from "angular2-uuid";

import {AssetGraphExtended, AssetGraphResponseExtended, AssetGraphService, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {SettingsService} from "app/services/domain/settings.service";
import * as Models from "app/services/proxy/model/models";
import {ChartTimeUtilities, TimeRanges} from "framework/ui/charting/core/time";
import {mapInParallel} from "framework/utils/concurrency";

@Injectable()
export class PanesService
{
    private static s_PaneConfigKey = "sys_paneConfig";

    constructor(private settings: SettingsService,
                private assetGraph: AssetGraphService)
    {
    }

    public async getPane(config: Models.PaneConfiguration,
                         response: AssetGraphResponseExtended): Promise<Models.Pane>
    {
        try
        {
            let pane      = new Models.Pane();
            pane.title    = await response.resolveInputName(config.titleInput);
            pane.branding = config.branding;

            let promises: Promise<Models.PaneCard>[] = [];
            for (let element of config.elements)
            {
                let extended = new PaneCardConfigurationExtended(element, response);
                if (extended.canResolve())
                {
                    promises.push(extended.resolve());
                }
            }
            pane.cards = await Promise.all(promises);

            return pane;
        }
        catch (e)
        {
            return null;
        }
    }

    public async evaluate(config: Models.PaneConfiguration,
                          contexts: Models.AssetGraphContext[]): Promise<Models.Pane[]>
    {
        let responseHolder = await this.assetGraph.resolve(config.graph, contexts);
        let panes          = await mapInParallel(responseHolder.responses, (response) => this.getPane(config, response));
        return panes.filter((pane) => pane);
    }

    public getValidAssets(config: Models.PaneConfiguration): Promise<Map<string, Models.RecordIdentity[]>>
    {
        return this.assetGraph.getValidAssets(config.graph);
    }

    public async saveConfig(configuration: Models.PaneConfiguration)
    {
        if (!configuration.id)
        {
            configuration.id = UUID.UUID();
        }

        return this.settings.setTypedPreference(PanesService.s_PaneConfigKey, configuration.id, configuration);
    }

    public getPaneRange(range: Models.RangeSelection): Models.RangeSelection
    {
        let timeRangeId         = TimeRanges.resolve(range?.range, false)?.id;
        let isRelativeTimeRange = ChartTimeUtilities.relativeTimeRanges.some((range) => range.id === timeRangeId);

        return isRelativeTimeRange ? range : null;
    }

    public getPaneIds(): Promise<string[]>
    {
        return this.settings.getPreferenceValues(PanesService.s_PaneConfigKey);
    }

    public getConfig(id: string): Promise<Models.PaneConfiguration>
    {
        return this.settings.getTypedPreference(PanesService.s_PaneConfigKey, id, Models.PaneConfiguration.fixupPrototype);
    }

    public remove(id: string): Promise<boolean>
    {
        return this.settings.removePreference(PanesService.s_PaneConfigKey, id);
    }

    public async parseImport(raw: string): Promise<Models.PaneConfiguration>
    {
        let value = await this.settings.checkTypedPreferenceValue(PanesService.s_PaneConfigKey, null, raw, Models.PaneConfiguration.fixupPrototype);
        if (value)
        {
            return value;
        }

        throw Error("Invalid Pane Format");
    }

    public getConfigBatch(ids: string[]): Promise<Models.PaneConfiguration[]>
    {
        return mapInParallel(ids, (id) => this.getConfig(id));
    }
}

class PaneCardConfigurationExtended
{
    private m_fieldsExtended: PaneFieldConfigurationExtended<any, any>[];

    constructor(protected m_config: Models.PaneCardConfiguration,
                protected m_graph: AssetGraphResponseExtended)
    {
        this.m_fieldsExtended = this.m_config.fields.map((field) => PaneFieldConfigurationExtended.newInstance(field, this.m_graph));
    }

    public canResolve(): boolean
    {
        return this.m_fieldsExtended.some((field) => field.canResolve());
    }

    public async resolve(): Promise<Models.PaneCard>
    {
        let card = Models.PaneCard.newInstance({
                                                   title : this.m_config.title,
                                                   fields: []
                                               });

        let promises: Promise<Models.PaneField>[] = [];
        for (let field of this.m_fieldsExtended)
        {
            if (field.canResolve())
            {
                promises.push(field.resolve());
            }
        }

        card.fields.push(...await Promise.all(promises));

        return card;
    }
}

export abstract class PaneFieldConfigurationExtended<T extends Models.PaneFieldConfiguration, S extends Models.PaneField>
{
    static newInstance(model: Models.PaneFieldConfiguration,
                       graph: AssetGraphResponseExtended): PaneFieldConfigurationExtended<any, any>
    {
        if (model instanceof Models.PaneFieldConfigurationAggregatedValue)
        {
            return new PaneFieldConfigurationAggregatedValueExtended(model, graph);
        }
        else if (model instanceof Models.PaneFieldConfigurationChart)
        {
            return new PaneFieldConfigurationChartExtended(model, graph);
        }
        else if (model instanceof Models.PaneFieldConfigurationAlertCount)
        {
            return new PaneFieldConfigurationAlertCountExtended(model, graph);
        }
        else if (model instanceof Models.PaneFieldConfigurationAlertFeed)
        {
            return new PaneFieldConfigurationAlertFeedExtended(model, graph);
        }
        else if (model instanceof Models.PaneFieldConfigurationCurrentValue)
        {
            return new PaneFieldConfigurationCurrentValueExtended(model, graph);
        }
        else if (model instanceof Models.PaneFieldConfigurationPathMap)
        {
            return new PaneFieldConfigurationPathMapExtended(model, graph);
        }

        throw Error("Unknown pane element");
    }

    protected constructor(protected m_config: T,
                          protected m_graph: AssetGraphResponseExtended)
    {
    }

    abstract collectNodeIds(nodeIds: Set<string>): void;

    abstract canResolve(): boolean;

    abstract resolve(): Promise<S>;
}

class PaneFieldConfigurationAggregatedValueExtended extends PaneFieldConfigurationExtended<Models.PaneFieldConfigurationAggregatedValue, Models.PaneFieldAggregatedValue>
{
    public collectNodeIds(nodeIds: Set<string>): void
    {
        let nodeId = this.m_config.controlPointGroup.pointInput?.nodeId;
        if (nodeId) nodeIds.add(nodeId);
    }

    public canResolve(): boolean
    {
        return !!this.m_graph.resolveInputIdentities(this.m_config?.controlPointGroup.pointInput);
    }

    public async resolve(): Promise<Models.PaneFieldAggregatedValue>
    {
        let identities   = this.m_graph.resolveInputIdentities(this.m_config.controlPointGroup.pointInput);
        let group        = Models.ControlPointsGroup.deepClone(this.m_config.controlPointGroup);
        group.selections = Models.ControlPointsSelection.newInstance({identities: identities});
        return Models.PaneFieldAggregatedValue.newInstance({
                                                               label: this.m_config.label,
                                                               value: group
                                                           });
    }
}

class PaneFieldConfigurationAlertCountExtended extends PaneFieldConfigurationExtended<Models.PaneFieldConfigurationAlertCount, Models.PaneFieldAlertCount>
{
    public collectNodeIds(nodeIds: Set<string>): void
    {
        let nodeId = this.m_config.locationInput?.nodeId;
        if (nodeId) nodeIds.add(nodeId);
    }

    public canResolve(): boolean
    {
        return !!this.m_graph.resolveInputLocation(this.m_config.locationInput);
    }

    public async resolve(): Promise<Models.PaneFieldAlertCount>
    {
        return Models.PaneFieldAlertCount.newInstance({
                                                          label     : this.m_config.label,
                                                          onlyActive: this.m_config.onlyActive,
                                                          value     : await this.m_graph.resolveInputLocation(this.m_config.locationInput)
                                                      });
    }
}

class PaneFieldConfigurationAlertFeedExtended extends PaneFieldConfigurationExtended<Models.PaneFieldConfigurationAlertFeed, Models.PaneFieldAlertFeed>
{
    public collectNodeIds(nodeIds: Set<string>): void
    {
        let nodeId = this.m_config.locationInput?.nodeId;
        if (nodeId) nodeIds.add(nodeId);
    }

    public canResolve(): boolean
    {
        return !!this.m_graph.resolveInputLocation(this.m_config.locationInput);
    }

    public async resolve(): Promise<Models.PaneFieldAlertFeed>
    {
        return Models.PaneFieldAlertFeed.newInstance({
                                                         label: this.m_config.label,
                                                         value: await this.m_graph.resolveInputLocation(this.m_config.locationInput)
                                                     });
    }
}

class PaneFieldConfigurationChartExtended extends PaneFieldConfigurationExtended<Models.PaneFieldConfigurationChart, Models.PaneFieldChart>
{
    public collectNodeIds(nodeIds: Set<string>): void
    {
        for (let source of this.m_config.config?.dataSources || [])
        {
            let nodeId = source.pointBinding?.nodeId;
            if (nodeId) nodeIds.add(nodeId);
        }
    }

    public canResolve(): boolean
    {
        return this.m_config.config.dataSources?.every((source) => !!this.m_graph.resolveInputIdentities(source.pointBinding));
    }

    public async resolve(): Promise<Models.PaneFieldChart>
    {
        const graphId     = UUID.UUID();
        const rootId      = this.m_graph.assetGraph.getRootNodes()[0].id;
        let chartConfig   = Models.TimeSeriesChartConfiguration.newInstance(this.m_config.config);
        chartConfig.type  = Models.TimeSeriesChartType.GRAPH;
        chartConfig.graph = Models.TimeSeriesGraphConfiguration.newInstance({
                                                                                sharedGraphs: [SharedAssetGraphExtended.newModel(this.m_graph.assetGraph.model, graphId, "Default")],
                                                                                contexts    : [
                                                                                    Models.AssetGraphContextAsset.newInstance({
                                                                                                                                  graphId: graphId,
                                                                                                                                  nodeId : rootId,
                                                                                                                                  sysId  : (await this.m_graph.getPrimaryIdentity()).sysId
                                                                                                                              })
                                                                                ]
                                                                            });

        for (let source of chartConfig.dataSources || [])
        {
            source.pointBinding.graphId = graphId;
        }

        return Models.PaneFieldChart.newInstance({
                                                     label: this.m_config.label,
                                                     value: chartConfig
                                                 });
    }
}

class PaneFieldConfigurationCurrentValueExtended extends PaneFieldConfigurationExtended<Models.PaneFieldConfigurationCurrentValue, Models.PaneFieldCurrentValue>
{
    public collectNodeIds(nodeIds: Set<string>): void
    {
        let nodeId = this.m_config.pointInput?.nodeId;
        if (nodeId) nodeIds.add(nodeId);
    }

    public canResolve(): boolean
    {
        return !!this.m_graph.resolveInputIdentity(this.m_config.pointInput);
    }

    public async resolve(): Promise<Models.PaneFieldCurrentValue>
    {
        return Models.PaneFieldCurrentValue.newInstance({
                                                            label       : this.m_config.label,
                                                            value       : this.m_graph.resolveInputIdentity(this.m_config.pointInput),
                                                            unitsFactors: this.m_config.unitsFactors,
                                                            suffix      : this.m_config.suffix
                                                        });
    }
}

class PaneFieldConfigurationPathMapExtended extends PaneFieldConfigurationExtended<Models.PaneFieldConfigurationPathMap, Models.PaneFieldPathMap>
{
    public collectNodeIds(nodeIds: Set<string>): void
    {
        let nodeId = this.m_config.locationInput?.nodeId;
        if (nodeId) nodeIds.add(nodeId);
    }

    public canResolve(): boolean
    {
        return !!this.m_graph.resolveInputIdentity(this.m_config.locationInput);
    }

    public async resolve(): Promise<Models.PaneFieldPathMap>
    {
        return Models.PaneFieldPathMap.newInstance({
                                                       label: this.m_config.label,
                                                       value: this.m_graph.resolveInputIdentity(this.m_config.locationInput)
                                                   });
    }
}

export class PaneConfigurationExtended
{
    static async load(domain: AppDomainContext,
                      id: string)
    {
        let paneConfig = await domain.panes.getConfig(id);
        return new PaneConfigurationExtended(domain, paneConfig);
    }

    private m_graph: AssetGraphExtended;

    constructor(public readonly domain: AppDomainContext,
                private m_model: Models.PaneConfiguration = Models.PaneConfiguration.newInstance({
                                                                                                     branding: Models.BrandingConfiguration.newInstance({horizontalPlacement: Models.HorizontalAlignment.Left})
                                                                                                 }))
    {
    }

    async save()
    {
        if (!this.m_model.id)
        {
            this.m_model.id = UUID.UUID();
        }

        await this.domain.panes.saveConfig(this.m_model);
    }

    get model(): Models.PaneConfiguration
    {
        return this.m_model;
    }

    get graph(): AssetGraphExtended
    {
        if (!this.m_graph && this.m_model.graph)
        {
            this.m_graph = new AssetGraphExtended(this.domain, this.m_model.graph);
        }

        return this.m_graph;
    }

    resetGraph()
    {
        this.m_graph = null;
    }

    get elements(): Models.PaneCardConfiguration[]
    {
        if (!this.m_model.elements)
        {
            this.m_model.elements = [];
        }

        return this.m_model.elements;
    }

    set elements(elements: Models.PaneCardConfiguration[])
    {
        this.m_model.elements = elements;
    }
}
