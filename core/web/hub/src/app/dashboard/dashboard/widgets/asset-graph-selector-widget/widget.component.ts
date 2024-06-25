import {ChangeDetectionStrategy, Component, ViewChild} from "@angular/core";

import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {AssetContextSubscriptionPayload, WidgetGraph} from "app/services/domain/dashboard-management.service";
import {WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";

import {ControlOption} from "framework/ui/control-option";
import {SelectComponent} from "framework/ui/forms/select.component";

@Component({
               selector       : "o3-asset-graph-selector-widget",
               templateUrl    : "./widget.template.html",
               styleUrls      : ["./widget.styles.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AssetGraphSelectorWidgetComponent extends WidgetBaseComponent<Models.AssetGraphSelectorWidgetConfiguration, AssetGraphSelectorWidgetConfigurationExtended>
{
    public graphOptions: ControlOption<string>[];

    graph: SharedAssetGraphExtended;

    private m_selectedAsset: string;
    public get selectedAsset(): string
    {
        return this.m_selectedAsset;
    }

    public set selectedAsset(asset: string)
    {
        if (this.m_selectedAsset !== asset)
        {
            this.m_selectedAsset = asset;
            this.dashboard.setGraphContext(this.config.selectorId, asset);
        }
    }

    @ViewChild("test_assets") test_assets: SelectComponent<string>;

    public async refreshSize(): Promise<boolean>
    {
        return true;
    }

    public cannotRemoveTooltip(): string
    {
        return this.removable ? "" : "There are widgets that rely on this asset selector";
    }

    private likeSelectorExists(graph?: WidgetGraph): boolean
    {
        if (!graph)
        {
            graph = new WidgetGraph(this.dashboard.widgetManipulator);
            graph.markNode(this);
        }

        let unmarkedLikeSelector = graph.findNode((node) =>
                                                  {
                                                      if (node.marked) return false;
                                                      if (!(node.widget.config instanceof Models.AssetGraphSelectorWidgetConfiguration)) return false;
                                                      return node.selectorIds[0] === this.config.selectorId;
                                                  });
        return !!unmarkedLikeSelector;
    }

    public canRemove(graph: WidgetGraph): boolean
    {
        if (!super.canRemove(graph)) return false;
        if (this.likeSelectorExists(graph)) return true;

        return !graph.findNode((node) =>
                               {
                                   if (node.marked) return false;
                                   if (node.children.length) return false;
                                   return node.selectorIds.some((selectorId) => selectorId === this.config.selectorId);
                               });
    }

    public remove()
    {
        super.remove();

        if (!this.likeSelectorExists())
        {
            this.dashboard.removeSelector(this.config.selectorId);
        }
    }

    public isAssetGraphRelated(widget: WidgetBaseComponent<any, any>): boolean
    {
        let selectorId = this.config.selectorId;
        return widget.selectorBindings.some((binding) => binding.selectorId === selectorId);
    }

    async dashboardUpdated()
    {
        await super.dashboardUpdated();

        let graphId = this.dashboard.getAssociatedGraphId(this.config.selectorId);
        this.graph  = await this.dashboard.getResolvedGraph(graphId);
        if (!this.graph) return;

        if (!this.config.name) this.config.name = this.graph.name;

        await this.registerOptionSubscription(graphId, (options) =>
        {
            this.graphOptions = options;
            this.markForCheck();
        });

        await this.registerContextSubscriptions(new AssetContextSubscriptionPayload(this.config.selectorId, async (context) =>
        {
            if (context instanceof Models.AssetGraphContextAsset)
            {
                this.selectedAsset = context.sysId;
                this.markForCheck();
            }
        }));

        this.markForCheck();
    }

    protected getClipboardData(): ClipboardEntryData<Models.AssetGraphSelectorWidgetConfiguration, null>
    {
        let model    = Models.AssetGraphSelectorWidgetConfiguration.deepClone(this.config);
        let selector = this.dashboard.selectors[this.config.selectorId];

        return new class extends ClipboardEntryData<Models.AssetGraphSelectorWidgetConfiguration, null>
        {
            constructor()
            {
                super("asset selector");

                this.selectors.push(Models.SharedAssetSelector.deepClone(selector));
            }

            public getDashboardWidget(): Models.AssetGraphSelectorWidgetConfiguration
            {
                let id = this.selectors[0]?.id;
                if (id)
                {
                    let res        = Models.AssetGraphSelectorWidgetConfiguration.deepClone(model);
                    res.selectorId = id;
                    return res;
                }

                return null;
            }

            public getReportItem(): null
            {
                return null;
            }
        }();
    }
}

@WidgetDef({
               friendlyName      : "Asset Selector",
               typeName          : "ASSET_STRUCTURE_SELECTOR",
               model             : Models.AssetGraphSelectorWidgetConfiguration,
               component         : AssetGraphSelectorWidgetComponent,
               dashboardCreatable: true,
               subgroupCreatable : true,
               maximizable       : true,
               defaultWidth      : 2,
               defaultHeight     : 2,
               hostScalableText  : false,
               needsProtector    : false,
               documentation     : {
                   description: "The Asset Selector widget allows you to dynamically control other widgets that have subscribed to its asset selection. This allows you to quickly view different slices of data.",
                   examples   : [
                       {
                           file       : "widgets/ASSET_STRUCTURE_SELECTOR/multiple.gif",
                           label      : "Asset Selector Driving Multiple Widgets",
                           description: "Using the Asset Selector to control multiple aggregation summary widgets inside grouping widgets."
                       }
                   ]
               }

           })
export class AssetGraphSelectorWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.AssetGraphSelectorWidgetConfiguration>
{
    public static readonly defaultSelectorName: string = "Selector";

    protected initializeForWizardInner()
    {
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        return [Models.AssetGraphBinding.newInstance({selectorId: this.model.selectorId})];
    }
}
