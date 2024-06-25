import {ChangeDetectionStrategy, Component, ViewChild} from "@angular/core";
import {UUID} from "angular2-uuid";

import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {AssetGraphExtended, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {AssetContextSubscriptionPayload} from "app/services/domain/dashboard-management.service";
import {ControlPointsGroupExtended, WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {AggregationComponent} from "app/shared/aggregation/aggregation.component";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {Lookup} from "framework/services/utils.service";

@Component({
               selector       : "o3-aggregation-widget",
               templateUrl    : "./widget.template.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AggregationWidgetComponent extends WidgetBaseComponent<Models.AggregationWidgetConfiguration, AggregationWidgetConfigurationExtended>
{
    @ViewChild(AggregationComponent, {static: true}) aggregation: AggregationComponent;

    private m_isBound = false;

    private m_graphExt: SharedAssetGraphExtended;

    public test_contextId: string;

    public tooltip: string = "";

    get subtext(): string
    {
        if (!this.container.showingToolbar)
        {
            return this.configExt.model.name;
        }

        return null;
    }

    public async bind()
    {
        await super.bind();

        if (!this.config.name) this.config.name = "Aggregation Summary";

        let pointInput = this.config.controlPointGroup?.pointInput;
        if (!pointInput?.graphId)
        {
            await this.bindAggregation();
        }
    }

    protected async dashboardUpdated()
    {
        await super.dashboardUpdated();

        let pointInput = this.config.controlPointGroup?.pointInput;
        if (pointInput)
        {
            let currContextId: string;
            await this.registerContextSubscriptions(new AssetContextSubscriptionPayload(pointInput.selectorId, async (context) =>
            {
                if (context != null && context.sysId !== currContextId)
                {
                    this.test_contextId = currContextId = context.sysId;

                    this.m_graphExt = await this.dashboard.getResolvedGraph(pointInput.graphId);
                    if (!this.m_graphExt) return;

                    let response                                        = await this.m_graphExt.resolveWithContext([context]);
                    this.config.controlPointGroup.selections.identities = response.resolveIdentities(pointInput);

                    let contextName = await this.dashboard.getContextName(pointInput.selectorId);
                    this.tooltip    = `Values from "${contextName}"`;

                    if (!this.m_isBound)
                    {
                        this.m_isBound = true;
                        await this.bindAggregation();
                    }
                    else
                    {
                        await this.refreshContent();
                    }
                }
            }));
        }
        else
        {
            this.m_graphExt = null;
        }

        this.markForCheck();
    }

    private async bindAggregation()
    {
        if (!this.wasDestroyed())
        {
            this.detectChanges();
            await this.aggregation.bind();
        }
    }

    public async refreshSize(): Promise<boolean>
    {
        return true;
    }

    public async refreshContent(): Promise<void>
    {
        await super.refreshContent();

        if (!this.aggregation || this.aggregation.loading) return;
        await this.aggregation.refreshContent();
    }

    protected getClipboardData(): ClipboardEntryData<Models.AggregationWidgetConfiguration, Models.ReportLayoutItem>
    {
        let model = Models.AggregationWidgetConfiguration.deepClone(this.config);

        let graph: Models.AssetGraph;
        if (this.m_graphExt)
        {
            graph = this.m_graphExt.model;
        }
        else if (this.config.controlPointGroup?.graph)
        {
            graph = this.config.controlPointGroup.graph;
        }

        let sharedGraph = Models.SharedAssetGraph.deepClone({
                                                                id   : UUID.UUID(),
                                                                graph: graph || AssetGraphExtended.emptyModel()
                                                            });

        return new class extends ClipboardEntryData<Models.AggregationWidgetConfiguration, Models.ReportLayoutItem>
        {
            constructor()
            {
                super("aggregation summary");
            }

            public getDashboardWidget(): Models.AggregationWidgetConfiguration
            {
                return Models.AggregationWidgetConfiguration.deepClone(model);
            }

            public getReportItem(oldToNewGraphId: Lookup<string>): Models.ReportLayoutItem
            {
                let cpGroup = Models.ControlPointsGroup.deepClone(model.controlPointGroup);
                if (!cpGroup) return null;

                let graphId: string;
                if (oldToNewGraphId && sharedGraph)
                {
                    graphId = oldToNewGraphId[sharedGraph.id];
                    if (!graphId) return null;
                }

                if (graphId && cpGroup.pointInput)
                {
                    cpGroup.graph      = null;
                    cpGroup.pointInput = Models.AssetGraphBinding.newInstance({
                                                                                  graphId: graphId,
                                                                                  nodeId : cpGroup.pointInput.nodeId
                                                                              });
                }

                let element = Models.CustomReportElementAggregatedValue.newInstance({
                                                                                        label            : model.name,
                                                                                        controlPointGroup: cpGroup
                                                                                    });
                return Models.ReportLayoutItem.newInstance({element: element});
            }

            public getReportGraphs(): Models.SharedAssetGraph[]
            {
                return sharedGraph ? [sharedGraph] : [];
            }
        }();
    }
}

@WidgetDef({
               friendlyName      : "Aggregation Summary",
               typeName          : "AGGREGATION",
               model             : Models.AggregationWidgetConfiguration,
               component         : AggregationWidgetComponent,
               dashboardCreatable: true,
               subgroupCreatable : true,
               maximizable       : false,
               defaultWidth      : 6,
               defaultHeight     : 3,
               hostScalableText  : true,
               needsProtector    : true,
               documentation     : {
                   description: "The Aggregation Summary widget lets you define an output to be computed on the selected set of control points for a given time range.",
                   examples   : [
                       {
                           file       : "widgets/AGGREGATION/temperatures.png",
                           label      : "Temperature Aggregation Summary",
                           description: "Summary of temperatures over the last 2 days in a 3x3 widget."
                       }
                   ]
               }

           })
export class AggregationWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.AggregationWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
        let model = this.model;

        model.manualFontScaling = true;
        model.fontMultiplier    = 1;
        model.controlPointGroup = ControlPointsGroupExtended.newModel({selections: Models.ControlPointsSelection.newInstance({identities: []})});
        model.filterableRange   = Models.FilterableTimeRange.newInstance({range: RangeSelectionExtended.newModel()});
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        let binding = this.model.controlPointGroup?.pointInput;
        return binding ? [Models.AssetGraphBinding.deepClone(binding)] : [];
    }
}
