import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";

import {Subject, Subscription} from "rxjs";

export interface GraphConfigurationHost
{
    hostContext: string;

    graphsChanged: Subject<void>;

    getGraphs(): Models.SharedAssetGraph[];

    resolveGraphs(): Promise<Map<string, SharedAssetGraphExtended>>;

    canRemove(graphId: string): boolean;

    canRemoveNode(graphId: string,
                  nodeId: string): boolean;
}

export class GraphConfigurationHostChecker
{
    private m_resolver: Subscription;
    private m_resolvedGraphs: Map<string, SharedAssetGraphExtended>;

    get host(): GraphConfigurationHost
    {
        return this.m_host;
    }

    public static isValid(host: GraphConfigurationHost)
    {
        if (!host) return false;
        const graphs = host.getGraphs();
        return graphs.length > 0 && UtilsService.valuesAreUnique(graphs.map((g) => g.name));
    }

    get isValid(): boolean
    {
        if (GraphConfigurationHostChecker.isValid(this.m_host) && this.m_resolvedGraphs)
        {
            for (let externalGraph of this.m_resolvedGraphs.values())
            {
                if (externalGraph.hasTreeNodes()) return true;
            }
        }

        return false;
    }

    constructor(private readonly m_host: GraphConfigurationHost,
                public readonly resolved = new Subject<void>())
    {
        this.m_resolver = this.m_host.graphsChanged.subscribe(() => this.resolveGraphs());

        this.resolveGraphs();
    }

    private async resolveGraphs()
    {
        this.m_resolvedGraphs = null;
        this.m_resolvedGraphs = await this.m_host.resolveGraphs();

        this.resolved.next();
    }

    cleanUp()
    {
        if (this.m_resolver)
        {
            this.m_resolver.unsubscribe();
            this.m_resolver = null;
        }
    }
}

export function getBindingName(host: GraphConfigurationHost,
                               binding: Models.AssetGraphBinding): string
{
    const graphs = host.getGraphs() || [];
    const graph  = graphs.find((g) => g.id === binding?.graphId);
    const node   = graph?.graph.nodes.find((n) => n.id === binding.nodeId);

    return node?.name;
}
