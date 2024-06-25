import {Injectable, Injector} from "@angular/core";
import {UUID} from "angular2-uuid";

import {AssetExtended, LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {SettingsService} from "app/services/domain/settings.service";
import * as Models from "app/services/proxy/model/models";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {ITreeNode} from "framework/ui/dropdowns/filterable-tree.component";
import {filterAsync, inParallel, mapInParallel} from "framework/utils/concurrency";
import {Memoizer, ResetMemoizers} from "framework/utils/memoizers";

@Injectable({providedIn: "root"})
export class AssetGraphService
{
    private static s_GraphConfigKey = "sys_assetGraphConfig";

    constructor(private inj: Injector,
                private settings: SettingsService)
    {
    }

    public async resolve(graph: Models.AssetGraph,
                         contexts?: Models.AssetGraphContext[]): Promise<AssetGraphResponseHolder>
    {
        contexts = contexts ?? [];

        let domain: AppDomainContext = this.inj.get(AppDomainContext);
        let ext                      = new AssetGraphExtended(domain, graph);

        let responses: AssetGraphResponseExtended[] = [];
        await inParallel(ext.getRootNodes(), async (root) =>
        {
            let partialGraph     = ext.traverse(root);
            let context          = contexts.find((c) => c.nodeId === root.id || !c.nodeId);
            let partialResponses = await this.resolveByGraph(domain, partialGraph, context);
            responses.push(...partialResponses);
        });

        return new AssetGraphResponseHolder(responses);
    }

    private async resolveByGraph(domain: AppDomainContext,
                                 graph: AssetGraphExtended,
                                 context: Models.AssetGraphContext): Promise<AssetGraphResponseExtended[]>
    {
        let retries = 3;

        outer:
            while (retries > 0)
            {
                let tuples: Models.AssetGraphResponseResolved[] = [];
                let errors                                      = new Map<string, Models.AssetGraphResponseError>();
                let done                                        = false;
                let offset                                      = 0;
                let version                                     = null;
                let numNodes                                    = graph.model.nodes.length;
                let batchSize                                   = Math.floor(10000 / numNodes);

                // A graph matching every asset should be ignored
                if (numNodes === 1 && !graph.model.nodes[0].condition) return [];

                while (!done)
                {
                    let request = Models.AssetGraphRequest.newInstance({
                                                                           graph      : graph.model,
                                                                           context    : context,
                                                                           startOffset: offset,
                                                                           maxResults : batchSize
                                                                       });

                    let resolvedGraph = await SharedSvc.AutoRetryRequest(3, () => domain.apis.assets.evaluateAssetGraph(request));
                    tuples.push(...resolvedGraph.results);

                    for (let id in resolvedGraph.errors)
                    {
                        errors.set(id, <Models.AssetGraphResponseError>resolvedGraph.errors[id]);
                    }

                    if (resolvedGraph.results.length < batchSize)
                    {
                        done = true;
                    }

                    if (version && version !== resolvedGraph.version)
                    {
                        retries--;
                        continue outer;
                    }
                    else if (!version)
                    {
                        version = resolvedGraph.version;
                    }

                    offset += resolvedGraph.results.length;
                }
                return this.split(domain, graph, tuples, errors);
            }

        throw new Error("Failed to resolve graph");
    }

    private split(domain: AppDomainContext,
                  ext: AssetGraphExtended,
                  tuples: Models.AssetGraphResponseResolved[],
                  errors: Map<string, Models.AssetGraphResponseError>): AssetGraphResponseExtended[]
    {
        let root = ext.getRootNodes()[0];
        if (!root) return [];

        let rootIndex = ext.getNodeIds()
                           .indexOf(root.id);

        let splitTuples = new Map<string, Models.AssetGraphResponseResolved[]>();
        for (let response of tuples)
        {
            // key is root asset's sysId
            let keys = response.tuple[rootIndex];
            if (keys.length !== 1) continue;
            let key = keys[0];
            if (!splitTuples.has(key))
            {
                splitTuples.set(key, []);
            }

            splitTuples.get(key)
                       .push(response);
        }

        let result: AssetGraphResponseExtended[] = [];
        for (let tuples of splitTuples.values())
        {
            result.push(new AssetGraphResponseExtended(domain, ext, tuples, errors));
        }

        return result;
    }

    public async getValidAssets(graph: Models.AssetGraph): Promise<Map<string, Models.RecordIdentity[]>>
    {
        return this.getValidAssetsByGraph(graph);
    }

    private async getValidAssetsByGraph(graph: Models.AssetGraph): Promise<Map<string, Models.RecordIdentity[]>>
    {
        let graphHolder = await this.resolve(graph);
        return graphHolder.getPrimaryAssets();
    }

    public async saveConfig(configuration: Models.SharedAssetGraph)
    {
        if (!configuration.id)
        {
            configuration.id = UUID.UUID();
        }

        return this.settings.setTypedPreference(AssetGraphService.s_GraphConfigKey, configuration.id, configuration);
    }

    public getGraphIds(): Promise<string[]>
    {
        return this.settings.getPreferenceValues(AssetGraphService.s_GraphConfigKey);
    }

    public getConfig(id: string): Promise<Models.SharedAssetGraph>
    {
        return this.settings.getTypedPreference(AssetGraphService.s_GraphConfigKey, id, Models.SharedAssetGraph.fixupPrototype);
    }

    public remove(id: string): Promise<boolean>
    {
        return this.settings.removePreference(AssetGraphService.s_GraphConfigKey, id);
    }

    public async parseImport(raw: string): Promise<Models.SharedAssetGraph>
    {
        let value = await this.settings.checkTypedPreferenceValue(AssetGraphService.s_GraphConfigKey, null, raw, Models.SharedAssetGraph.fixupPrototype);
        if (value)
        {
            return value;
        }

        throw Error("Invalid Asset Structure Format");
    }

    public getConfigBatch(ids: string[]): Promise<Models.SharedAssetGraph[]>
    {
        return mapInParallel(ids, (id) => this.getConfig(id));
    }
}

export class AssetGraphResponseHolder
{
    public readonly graphExt: AssetGraphExtended;

    private readonly m_validResponses: AssetGraphResponseExtended[]   = [];
    private readonly m_invalidResponses: AssetGraphResponseExtended[] = [];

    get responses(): AssetGraphResponseExtended[]
    {
        return this.m_validResponses;
    }

    get invalidResponses(): AssetGraphResponseExtended[]
    {
        return this.m_invalidResponses;
    }

    constructor(responses: AssetGraphResponseExtended[])
    {
        for (let resp of responses)
        {
            if (resp.isValid)
            {
                this.m_validResponses.push(resp);
            }
            else
            {
                this.m_invalidResponses.push(resp);
            }

            this.graphExt = resp.assetGraph;
        }
    }

    findValidResponse(assetId: string): AssetGraphResponseExtended
    {
        return this.m_validResponses.find((response) => response.hasAsset(assetId));
    }

    findNodeIdWithIntersectingAssets(assetIds: string[]): string
    {
        for (let response of this.m_validResponses)
        {
            let nodeId = response.findNodeIdWithIntersectingAssets(assetIds);
            if (nodeId) return nodeId;
        }

        return null;
    }

    resolveIdentities(input: Models.AssetGraphBinding): Models.RecordIdentity[]
    {
        let allIdentities = this.m_validResponses.map((response) => response.resolveInputIdentities(input));

        let identities: Models.RecordIdentity[] = [];
        for (let identityArr of allIdentities) identities.push(...identityArr);
        return identities;
    }

    async resolveIdentitiesByContext(input: Models.AssetGraphBinding,
                                     byLocation: boolean): Promise<Lookup<Models.RecordIdentity[]>>
    {
        let organizedIdentities: Lookup<Models.RecordIdentity[]> = {};

        for (let response of this.m_validResponses)
        {
            let context                              = byLocation ? await response.getLocation() : await response.getPrimaryAsset();
            organizedIdentities[context.model.sysId] = response.resolveInputIdentities(input);
        }

        return organizedIdentities;
    }

    resolveBindingTuples(bindings: Models.AssetGraphBinding[],
                         enforceUniqueness: boolean): AssetGraphHierarchyTupleResult[]
    {
        let nodeHierarchies = bindings.map((binding) =>
                                           {
                                               let hierarchy: string[] = [];
                                               let nodeId              = binding.nodeId;
                                               while (nodeId)
                                               {
                                                   hierarchy.unshift(nodeId);
                                                   nodeId = this.graphExt.getNodeParentId(nodeId);
                                               }

                                               return hierarchy;
                                           });

        let tupleResults: AssetGraphHierarchyTupleResult[] = [];
        for (let response of this.m_validResponses)
        {
            tupleResults.push(...response.resolveHierarchyTuples(nodeHierarchies, enforceUniqueness));
        }

        return tupleResults;
    }

    async resolveControlPoints(domain: AppDomainContext,
                               inputs: Models.AssetGraphBinding[]): Promise<Models.DeviceElement[]>
    {
        const tuples      = this.resolveBindingTuples(inputs, false);
        const tupleIds    = tuples.map((tuple) => tuple.tuple);
        const assetTuples = await domain.assets.loadTable(tupleIds);

        const idToDeviceElem = new Map<string, Models.DeviceElement>();
        for (let assetTuple of assetTuples)
        {
            for (let asset of assetTuple)
            {
                if (asset instanceof Models.DeviceElement) idToDeviceElem.set(asset.sysId, asset);
            }
        }

        return [...idToDeviceElem.values()];
    }

    async getPrimaryAssets(): Promise<Map<string, Models.RecordIdentity[]>>
    {
        let response = new Map<string, Models.RecordIdentity[]>();
        await inParallel(this.m_validResponses, async (g) =>
        {
            let rootId = g.assetGraph.getRootNodes()[0].id;
            let id     = await g.getPrimaryIdentity();
            if (!response.has(rootId))
            {
                response.set(rootId, []);
            }
            if (id)
            {
                response.get(rootId)
                        .push(id);
            }
        });
        return response;
    }

    async getControlOptions(sort = true): Promise<Map<string, ControlOption<string>[]>>
    {
        let response = new Map<string, ControlOption<string>[]>();
        await inParallel(this.m_validResponses, async (g) =>
        {
            let rootId = g.assetGraph.getRootNodes()[0].id;
            let option = await g.getControlOption();
            if (!response.has(rootId))
            {
                response.set(rootId, []);
            }
            if (option)
            {
                response.get(rootId)
                        .push(option);
            }
        });

        if (sort)
        {
            for (let [key, list] of response.entries())
            {
                list.sort((a,
                           b) => UtilsService.compareStrings(a.label, b.label, true));
            }

        }

        return response;
    }

    async findResponsesByPrimaryIds(rootId: string,
                                    sysIds: string[]): Promise<AssetGraphResponseExtended[]>
    {
        if (!sysIds)
        {
            return this.m_validResponses;
        }

        return filterAsync(this.m_validResponses, async (response) =>
        {
            if (response.assetGraph.getRootNodes()[0].id === rootId)
            {
                let primary = await response.getPrimaryIdentity();
                return sysIds.some((sysId) => sysId === primary?.sysId);
            }

            return false;
        });
    }
}

export class AssetGraphResponseExtended
{
    public static readonly placeholderDimension: string = "asset_structure_filler";

    private readonly m_assetCache: Lookup<AssetExtended>     = {};
    private readonly m_nodeIdToAssetIds: Lookup<Set<string>> = {};

    constructor(private m_domain: AppDomainContext,
                private m_graph: AssetGraphExtended,
                private m_tuples: Models.AssetGraphResponseResolved[],
                private m_errors: Map<string, Models.AssetGraphResponseError>)
    {
        let nodeIds = m_graph.getNodeIds();
        for (let tuple of m_tuples)
        {
            for (let i = 0; i < nodeIds.length; i++)
            {
                let nodeId   = nodeIds[i];
                let assetSet = this.m_nodeIdToAssetIds[nodeId];
                if (!assetSet)
                {
                    assetSet                        = new Set<string>();
                    this.m_nodeIdToAssetIds[nodeId] = assetSet;
                }

                if (tuple.tuple[i])
                {
                    for (let sysId of tuple.tuple[i])
                    {
                        assetSet.add(sysId);
                    }
                }
            }
        }
    }

    public findNodeIdWithIntersectingAssets(assets: string[]): string
    {
        for (let nodeId in this.m_nodeIdToAssetIds)
        {
            let currAssets = this.m_nodeIdToAssetIds[nodeId];

            for (let asset of assets)
            {
                if (currAssets.has(asset)) return nodeId;
            }
        }

        return null;
    }

    public hasAsset(sysId: string): boolean
    {
        if (this.m_assetCache[sysId]) return true;

        for (let nodeId in this.m_nodeIdToAssetIds)
        {
            if (this.m_nodeIdToAssetIds[nodeId].has(sysId)) return true;
        }

        return false;
    }

    public async getPrimaryIdentity(): Promise<Models.RecordIdentity>
    {
        let asset = await this.getPrimaryAsset();
        return asset?.getIdentity();
    }

    public async getLocation(): Promise<LocationExtended>
    {
        let asset = await this.getPrimaryAsset();
        return asset?.getLocation();
    }

    public async getControlOption(): Promise<ControlOption<string>>
    {
        let asset = await this.getPrimaryAsset();
        return asset ? new ControlOption(asset.model.sysId, asset.model.name) : null;
    }

    public get graphId(): string
    {
        if (this.assetGraph instanceof SharedAssetGraphExtended)
        {
            return this.assetGraph.id;
        }

        return "";
    }

    public get assetGraph(): AssetGraphExtended
    {
        return this.m_graph;
    }

    public get isValid(): boolean
    {
        return this.m_errors.size == 0;
    }

    public async getPrimaryAsset(): Promise<AssetExtended>
    {
        let roots = this.m_graph.getRootNodes();

        // We can only handle single root when looking for assets to bind to the root of the graph.
        if (roots.length !== 1)
        {
            return null;
        }

        let rootId = roots[0].id;

        let assets = await this.resolveAssets(rootId);
        if (assets && assets.length === 1)
        {
            return assets[0];
        }

        return null;
    }

    public resolveInputIdentity(input: Models.AssetGraphBinding): Models.RecordIdentity
    {
        if (input)
        {
            let assetIds = this.m_nodeIdToAssetIds[input.nodeId];
            if (assetIds?.size === 1)
            {
                let id = [...assetIds][0];
                return AssetExtended.newIdentityRaw(id);
            }
        }

        return null;
    }

    public resolveInputIdentities(input: Models.AssetGraphBinding): Models.RecordIdentity[]
    {
        if (input)
        {
            let assetIds = this.m_nodeIdToAssetIds[input.nodeId];
            if (assetIds)
            {
                return UtilsService.mapIterable(assetIds, (id) => AssetExtended.newIdentityRaw(id));
            }
        }

        return [];
    }

    public resolveHierarchyTuples(nodeHierarchies: string[][],
                                  enforceUniqueness: boolean): AssetGraphHierarchyTupleResult[]
    {
        const uniques                                           = new Set<string>();
        const hierarchyTuples: AssetGraphHierarchyTupleResult[] = [];
        for (let tuple of this.m_tuples)
        {
            for (let nodeHierarchy of nodeHierarchies)
            {
                let hierarchyTuple: string[] = [];
                let lastNodeIdx              = nodeHierarchy.length - 1;
                for (let i = lastNodeIdx; i >= 0; i--)
                {
                    let relevantAssets = this.m_nodeIdToAssetIds[nodeHierarchy[i]];
                    if (relevantAssets)
                    {
                        for (let assets of tuple.tuple || [])
                        {
                            let asset = assets?.find((asset) => relevantAssets.has(asset));
                            if (asset)
                            {
                                hierarchyTuple.unshift(asset);
                                break;
                            }
                        }
                    }
                }

                if (hierarchyTuple.length === nodeHierarchy.length)
                {
                    if (enforceUniqueness)
                    {
                        const hierarchyString = hierarchyTuple.join();
                        if (uniques.has(hierarchyString))
                        {
                            continue;
                        }
                        else
                        {
                            uniques.add(hierarchyString);
                        }
                    }

                    hierarchyTuples.push(new AssetGraphHierarchyTupleResult(nodeHierarchy[lastNodeIdx], hierarchyTuple));
                }
            }
        }

        return hierarchyTuples;
    }

    public async resolveInputLocation(input: Models.AssetGraphBinding): Promise<Models.RecordIdentity>
    {
        if (input)
        {
            let assets = await this.resolveAssets(input.nodeId);
            if (assets && assets.length === 1)
            {
                return assets[0].model.location;
            }
        }

        return null;
    }

    public async resolveInputName(input: Models.AssetGraphBinding): Promise<string>
    {
        if (input)
        {
            let assets = await this.resolveAssets(input.nodeId);
            if (assets && assets.length === 1)
            {
                return assets[0].model.name;
            }
        }

        return "";
    }

    private async resolveAssets(nodeId: string): Promise<AssetExtended[]>
    {
        let assetSet   = this.m_nodeIdToAssetIds[nodeId];
        let assetIds   = assetSet ? [...assetSet] : [];
        let idsToFetch = assetIds.filter((id) => !this.m_assetCache[id])
                                 .map((id) => AssetExtended.newIdentityRaw(id));
        if (idsToFetch.length)
        {
            let fetched = await this.m_domain.assets.getExtendedBatch(idsToFetch);
            for (let asset of fetched)
            {
                this.m_assetCache[asset.getIdentity().sysId] = asset;
            }
        }

        return assetIds.map((id) => this.m_assetCache[id]);
    }
}

export class AssetGraphHierarchyTupleResult
{
    assets: Models.Asset[];

    constructor(public readonly nodeId: string,
                public readonly tuple: string[])
    {}

    populateAssets(assetLookup: Lookup<Models.Asset>)
    {
        this.assets = this.tuple.map((sysId) => assetLookup[sysId]);
    }
}

export class AssetGraphExtended
{
    private readonly m_nodes: Lookup<Models.AssetGraphNode> = {};
    private readonly m_reverseGraph: Lookup<string>         = {};
    private readonly m_children: Lookup<Set<string>>        = {};

    public isPristine: boolean = true;

    constructor(protected readonly m_domain: AppDomainContext,
                protected readonly m_model: Models.AssetGraph = new Models.AssetGraph())
    {
        UtilsService.extractLookup(this.nodes, this.m_nodes);

        for (let transform of this.transforms)
        {
            let children = this.m_children[transform.inputId];
            if (!children)
            {
                children                           = new Set<string>();
                this.m_children[transform.inputId] = children;
            }

            children.add(transform.outputId);
            this.m_reverseGraph[transform.outputId] = transform.inputId;
        }
    }

    public get model(): Models.AssetGraph
    {
        return this.m_model;
    }

    @Memoizer
    resolve(): Promise<AssetGraphResponseHolder>
    {
        return this.m_domain.assetGraphs.resolve(this.m_model);
    }

    resolveWithContext(contexts?: Models.AssetGraphContext[]): Promise<AssetGraphResponseHolder>
    {
        return this.m_domain.assetGraphs.resolve(this.m_model, contexts);
    }

    private get nodes(): Models.AssetGraphNode[]
    {
        if (!this.m_model.nodes)
        {
            this.m_model.nodes = [];
        }

        return this.m_model.nodes;
    }

    private set nodes(nodes: Models.AssetGraphNode[])
    {
        this.m_model.nodes = nodes;
    }

    private get transforms(): Models.AssetGraphTransform[]
    {
        if (!this.m_model.transforms)
        {
            this.m_model.transforms = [];
        }

        return this.m_model.transforms;
    }

    private set transforms(transforms: Models.AssetGraphTransform[])
    {
        this.m_model.transforms = transforms;
    }

    static isValid(graph: Models.AssetGraph,
                   allowEmpty: boolean): boolean
    {
        if (!graph) return false;
        if (!graph.nodes.length) return allowEmpty;

        for (let node of graph.nodes)
        {
            if (!this.isValidNode(node))
            {
                return false;
            }
        }

        return true;
    }

    public static emptyModel(): Models.AssetGraph
    {
        return Models.AssetGraph.newInstance({
                                                 nodes     : [],
                                                 transforms: []
                                             });
    }

    private static isValidNode(node: Models.AssetGraphNode): boolean
    {
        return node && !!node.id && !!node.name;
    }

    isValid(allowEmpty: boolean): boolean
    {
        return AssetGraphExtended.isValid(this.model, allowEmpty);
    }

    getNodeName(nodeId: string): string
    {
        return this.m_nodes[nodeId]?.name;
    }

    getNodeNames(): string[]
    {
        return this.nodes.map((n) => n.name);
    }

    getNodeIds(): string[]
    {
        return this.nodes.map((n) => n.id);
    }

    getAncestorHierarchy(nodeId: string): string[]
    {
        let ancestorHierarchy = [];
        let id                = nodeId;
        while (id)
        {
            ancestorHierarchy.unshift(id);
            id = this.getNodeParentId(id);
        }
        return ancestorHierarchy;
    }

    getNodesForDropdown(): string[][]
    {
        let res = this.nodes.map((n) => [
            n.name,
            n.id
        ]);
        res.sort((a,
                  b) => UtilsService.compareStrings(a[0], b[0], true));

        if (res.length == 0)
        {
            res.push([
                         "<invalid graph>",
                         "<invalid graph>"
                     ]);
        }

        return res;
    }

    getRootNodeId(nodeId: string): string
    {
        while (this.m_reverseGraph[nodeId])
        {
            nodeId = this.m_reverseGraph[nodeId];
        }

        return nodeId;
    }

    suggestName(): string
    {
        // Suggest name
        let roots = this.getRootNodes();
        if (roots.length === 1)
        {
            let nameParts: string[] = [];
            let root                = roots[0];
            while (root)
            {
                nameParts.push(root.name);
                let children = this.getNodeChildren(root.id);
                if (children.length === 1)
                {
                    root = children[0];
                }
                else
                {
                    break;
                }
            }

            return nameParts.join(" / ");
        }

        return "";
    }

    @ResetMemoizers
    addNode(node: Models.AssetGraphNode)
    {
        this.nodes.push(node);
        this.m_nodes[node.id] = node;
        this.isPristine       = false;
    }

    @ResetMemoizers
    updateNode(node: Models.AssetGraphNode)
    {
        let index = this.nodes
                        .map((n) => n.id)
                        .indexOf(node.id);
        if (index >= 0)
        {
            this.nodes.splice(index, 1, node);
            this.m_nodes[node.id] = node;
            this.isPristine       = false;
        }
    }

    @ResetMemoizers
    deleteNode(id: string)
    {
        let children = this.m_children[id];
        if (children)
        {
            for (let child of [...children])
            {
                this.deleteNode(child);
            }

            delete this.m_children[id];
        }

        this.nodes      = this.nodes.filter((n) => n.id !== id);
        this.transforms = this.transforms.filter((tr) => tr.inputId !== id && tr.outputId !== id);
        delete this.m_nodes[id];

        let parentId = this.m_reverseGraph[id];
        delete this.m_reverseGraph[id];
        if (parentId)
        {
            this.m_children[parentId].delete(id);
        }

        this.isPristine = false;
    }

    isRoot(id: string): boolean
    {
        return !this.m_reverseGraph[id];
    }

    getNodeById(id: string): Models.AssetGraphNode
    {
        return this.m_nodes[id];
    }

    getNodeParentId(id: string): string
    {
        return this.m_reverseGraph[id];
    }

    getNodeDepth(nodeId: string): number
    {
        let depth = -1;
        nodeId    = this.m_nodes[nodeId]?.id;
        while (nodeId)
        {
            depth++;
            nodeId = this.m_reverseGraph[nodeId];
        }

        return depth;
    }

    getNodeChildren(id: string): Models.AssetGraphNode[]
    {
        let children = this.m_children[id];
        if (!children)
        {
            return [];
        }

        return UtilsService.mapIterable(children, (id) => this.getNodeById(id));
    }

    getRootNodes(): Models.AssetGraphNode[]
    {
        return this.nodes.filter((n) => !this.m_reverseGraph[n.id]);
    }

    traverse(root: Models.AssetGraphNode): AssetGraphExtended
    {
        let other = new AssetGraphExtended(this.m_domain);
        other.addNode(root);
        let queue = [root.id];
        while (queue.length)
        {
            let nodeId = queue.shift();

            let children = this.getNodeChildren(nodeId);
            for (let child of children)
            {
                let transform = this.findTransformByOutputId(child.id);
                other.addNode(child);
                if (transform instanceof Models.AssetGraphTransformRelationship)
                {
                    other.addRelationshipTransform(nodeId, child.id, transform.relationship);
                }

                queue.push(child.id);
            }
        }
        return other;
    }

    @ResetMemoizers
    addRelationshipTransform(inputId: string,
                             outputId: string,
                             relationship: Models.AssetRelationship): Models.AssetGraphTransformRelationship
    {
        let transform = Models.AssetGraphTransformRelationship.newInstance({
                                                                               inputId     : inputId,
                                                                               outputId    : outputId,
                                                                               relationship: relationship
                                                                           });
        this.transforms.push(transform);
        this.m_reverseGraph[outputId] = inputId;
        if (!this.m_children[inputId])
        {
            this.m_children[inputId] = new Set<string>();
        }

        this.m_children[inputId].add(outputId);
        return transform;
    }

    findTransformByOutputId(outputId: string): Models.AssetGraphTransform
    {
        return this.transforms.find((tr) => tr.outputId === outputId);
    }

    hasTreeNodes(): boolean
    {
        return this.getTreeNodes(true)
                   .some((node) => this.hasTreeNodesHelper(node));
    }

    private hasTreeNodesHelper(node: AssetGraphTreeNode): boolean
    {
        if (!node.disableSelection) return true;
        if (!node.children) return false;
        return node.children.some((child) => this.hasTreeNodesHelper(child));
    }

    getTreeNodes(enableLeavesOnly: boolean = false,
                 graphId?: string,
                 graphName?: string,
                 ids?: Set<string>): AssetGraphTreeNode[]
    {
        let result: AssetGraphTreeNode[] = [];
        let roots                        = this.getRootNodes();

        let rootOption: AssetGraphTreeNode;
        if (graphId && graphName)
        {
            rootOption                  = new AssetGraphTreeNode(this, null, graphId, graphName);
            rootOption.disableSelection = enableLeavesOnly;
            result.push(rootOption);
        }

        let nodes: Lookup<AssetGraphTreeNode> = {};
        while (roots.length)
        {
            let nextLevel = new Set<Models.AssetGraphNode>();

            for (let node of roots)
            {
                let n          = new AssetGraphTreeNode(this, node, graphId, graphName);
                nodes[node.id] = n;

                if (ids)
                {
                    ids.add(n.id);
                }

                let parentId = this.getNodeParentId(node.id);
                if (parentId)
                {
                    let parent              = nodes[parentId];
                    parent.disableSelection = enableLeavesOnly;
                    parent.children.push(n);
                }
                else if (rootOption)
                {
                    rootOption.children.push(n);
                }
                else
                {
                    result.push(n);
                }

                let children = this.getNodeChildren(node.id);
                for (let child of children)
                {
                    nextLevel.add(child);
                }
            }

            roots = [...nextLevel];
        }

        return result;
    }
}

export class SharedAssetGraphExtended extends AssetGraphExtended
{
    public static readonly LOCAL_GRAPH_ID: string = "local_graph";

    public static async loadById(domain: AppDomainContext,
                                 id: string): Promise<SharedAssetGraphExtended>
    {
        let raw = await domain.assetGraphs.getConfig(id);
        return new SharedAssetGraphExtended(domain, raw);
    }

    public static async loadGraphs2(domain: AppDomainContext,
                                    models: Lookup<Models.SharedAssetGraph>): Promise<Map<string, SharedAssetGraphExtended>>
    {
        let loadedGraphs = new Map<string, SharedAssetGraphExtended>();

        if (models)
        {
            await inParallel(Object.keys(models), async (graphId) =>
            {
                let extended = new SharedAssetGraphExtended(domain, models[graphId]);
                await extended.resolve();
                loadedGraphs.set(graphId, extended);
            });
        }

        return loadedGraphs;
    }

    public static async loadGraphs(domain: AppDomainContext,
                                   models: Models.SharedAssetGraph[]): Promise<Map<string, SharedAssetGraphExtended>>
    {
        let loadedGraphs = new Map<string, SharedAssetGraphExtended>();

        if (models)
        {
            await inParallel(models, async (graph) =>
            {
                let extended = new SharedAssetGraphExtended(domain, graph);
                await extended.resolve();
                loadedGraphs.set(graph.id, extended);
            });
        }

        return loadedGraphs;
    }

    public static newModel(graph: Models.AssetGraph,
                           id: string,
                           name: string): Models.SharedAssetGraph
    {
        return Models.SharedAssetGraph.newInstance({
                                                       id   : id || UUID.UUID(),
                                                       name : name,
                                                       graph: graph || AssetGraphExtended.emptyModel()
                                                   });
    }

    constructor(domain: AppDomainContext,
                private m_sharedGraphModel: Models.SharedAssetGraph)
    {
        super(domain, m_sharedGraphModel?.graph);
        if (!m_sharedGraphModel.graph)
        {
            m_sharedGraphModel.graph = this.m_model;
        }
    }

    public get id(): string
    {
        return this.m_sharedGraphModel.id;
    }

    public get name(): string
    {
        return this.m_sharedGraphModel.name;
    }

    public set name(name: string)
    {
        this.m_sharedGraphModel.name = name;
    }

    public modelClone(): Models.SharedAssetGraph
    {
        return Models.SharedAssetGraph.deepClone(this.m_sharedGraphModel);
    }

    async save()
    {
        if (!this.m_sharedGraphModel.id)
        {
            this.m_sharedGraphModel.id = UUID.UUID();
        }

        await this.m_domain.assetGraphs.saveConfig(this.m_sharedGraphModel);
    }
}

export class AssetGraphReference
{
    constructor(public graphId: string,
                public nodeId: string,
                public selectorId?: string)
    {}

    static fromBinding(binding: Models.AssetGraphBinding): AssetGraphReference
    {
        return new AssetGraphReference(binding.graphId, binding.nodeId, binding.selectorId);
    }
}

export class AssetGraphTreeNode implements ITreeNode<string>
{
    get numOptions(): number
    {
        let num = this.disableSelection ? 0 : 1;
        return num + this.children.reduce((cum,
                                           child) => cum + child.numOptions, 0);
    }

    disableSelection: boolean = false;

    children: AssetGraphTreeNode[] = [];

    get hasChildren(): boolean
    {
        return this.children.length > 0;
    }

    constructor(private m_graph: AssetGraphExtended,
                private m_model: Models.AssetGraphNode,
                private m_graphId?: string,
                private m_graphName?: string,
                private m_selectorId?: string)
    {

    }

    private static getId(id: string,
                         graphId: string,
                         selectorId: string): string
    {
        return [
            id,
            graphId,
            selectorId
        ].filter((n) => !!n)
         .join("/");
    }

    static getIdFromBinding(binding: Models.AssetGraphBinding,
                            includeSelectorId?: boolean): string
    {
        return this.getId(binding?.nodeId, binding?.graphId, includeSelectorId ? binding?.selectorId : undefined);
    }

    static equivalentBinding(bindingA: Models.AssetGraphBinding,
                             bindingB: Models.AssetGraphBinding): boolean
    {
        return this.getIdFromBinding(bindingA) === this.getIdFromBinding(bindingB);
    }

    static getBinding(id: string): Models.AssetGraphBinding
    {
        if (!id) return null;
        let idParts = id.split("/");
        return Models.AssetGraphBinding.newInstance({
                                                        nodeId    : idParts[0],
                                                        graphId   : idParts[1],
                                                        selectorId: idParts[2]
                                                    });
    }

    get id(): string
    {
        return AssetGraphTreeNode.getId(this.m_model?.id, this.m_graphId, this.m_selectorId);
    }

    get label(): string
    {
        return this.m_model?.name || this.m_graphName;
    }

    set label(name: string)
    {
        this.m_model.name = name;
    }

    get canAddChild(): boolean
    {
        return !this.m_model.allowMultiple;
    }

    get optional(): boolean
    {
        return this.m_model?.optional;
    }

    get allowMultiple(): boolean
    {
        return this.m_model?.allowMultiple;
    }
}
