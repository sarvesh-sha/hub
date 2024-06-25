import {CdkDragDrop, moveItemInArray} from "@angular/cdk/drag-drop";
import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from "@angular/core";

import {AppContext} from "app/app.service";
import {AssetGraphExtended, AssetGraphHierarchyTupleResult} from "app/services/domain/asset-graph.service";
import {LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {RecordIdentity} from "app/services/proxy/model/models";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ChartFont, INTERACTIVE_TREE_HEADER_FONT} from "framework/ui/charting/core/text";
import {Future} from "framework/utils/concurrency";

import {Subject, Subscription} from "rxjs";

@Component({
               selector       : "o3-interactive-tree",
               templateUrl    : "./interactive-tree.component.html",
               styleUrls      : ["./interactive-tree.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class InteractiveTreeComponent extends SharedSvc.BaseApplicationComponent
{
    public static readonly MIN_ROW_HEIGHT          = 16;
    public static readonly HEADER_HEIGHT           = 40;
    private static readonly HEADER_LABEL_FONT_SIZE = 16;
    private static readonly NODE_LABEL_FONT_SIZE   = 12;
    private static readonly MAX_COL_WIDTH          = 225;

    private m_heightsChangedSub: Subscription;

    private m_table: PivotTableView;
    @Input() set table(table: PivotTableView)
    {
        if (table)
        {
            this.m_table = table;

            if (this.m_heightsChangedSub) this.m_heightsChangedSub.unsubscribe();
            this.m_heightsChangedSub = this.subscribeToObservable(this.m_table.heightsChanged, () => this.rebuildTree());

            this.rebuildTree();
        }
    }

    get table(): PivotTableView
    {
        return this.m_table;
    }

    @Input() columnsCollapsible = false;
    @Input() columnsMovable     = false;

    @Output() nodesReordered   = new EventEmitter<void>();
    @Output() minHeightChanged = new EventEmitter<number>();
    @Output() widthChanged     = new EventEmitter<number>();
    @Output() rows             = new EventEmitter<InteractiveTreeRow[]>();

    public tree: InteractiveTreeNode[];
    public columns: ProcessedTreeNode[][];

    public dynamicColWidths: number[] = [];
    public cumWidth: number;
    public minHeight: number;

    headerTooltip(colIdx: number): string
    {
        return `${this.m_table.columns[colIdx].label} of assets from node '${this.m_table.table.headerName(colIdx)}'`;
    }

    toggleCollapsed(node: ProcessedTreeNode)
    {
        node.node.collapsed = !node.node.collapsed;
        this.processTree();
    }

    toggleHidden(node: ProcessedTreeNode)
    {
        let hasOpen = this.nonHiddenSiblings(node).length > 0;
        let isTop   = !node.parent;

        if (!hasOpen)
        {
            if (!isTop) this.toggleHidden(node.parent);
        }
        else
        {
            node.node.hidden = !node.node.hidden;
            this.processTree();
        }
    }

    nonHiddenSiblings(node: ProcessedTreeNode): ProcessedTreeNode[]
    {
        let siblings = node.parent ? node.parent.children : this.columns[0];
        return siblings.filter((sibling) => sibling !== node && !sibling.hidden);
    }

    pivot(event: CdkDragDrop<any, any>)
    {
        if (this.m_table.reorderColumns(event.previousIndex, event.currentIndex))
        {
            this.rebuildTree();
            this.nodesReordered.emit();
        }
    }

    private rebuildTree()
    {
        this.tree = this.m_table.asInteractiveTree();
        this.processTree();
    }

    private processTree()
    {
        this.columns = [];
        for (let rootRow of this.tree)
        {
            // distribute nodes into columns
            for (let node of this.wrap(this, rootRow, 0, null))
            {
                if (!this.columns[node.column]) this.columns[node.column] = [];
                this.columns[node.column].push(node);
            }
        }

        // Calculate fixed sizes from top to bottom
        for (let column of this.columns)
        {
            for (let node of column)
            {
                if (node.hidden || node.underHidden)
                {
                    node.height = 0;
                }
                else if (node.collapsed)
                {
                    node.height = InteractiveTreeComponent.MIN_ROW_HEIGHT;
                    ProcessedTreeNode.distribute(node);
                }
                else if (node.height !== null)
                {
                    ProcessedTreeNode.distribute(node);
                }
            }
        }

        // Process last column to count auto-size rows and sum fixed size rows
        let numAuto    = 0;
        let cumHeight  = 0;
        let lastColumn = this.columns[this.columns.length - 1];
        for (let node of lastColumn)
        {
            if (node.height === null)
            {
                numAuto++;
            }
            else
            {
                cumHeight += node.height;
            }
        }

        cumHeight += InteractiveTreeComponent.HEADER_HEIGHT;

        let percent = (1 / numAuto) * 100;
        let skew    = (cumHeight / numAuto);

        // Process last column to get flex values
        for (let node of lastColumn)
        {
            if (node.height !== null)
            {
                node.flex = `0 0 ${node.height}px`;
            }
            else
            {
                node.flex = `1 1 calc(${percent}% - ${skew}px)`;
            }
        }

        this.dynamicColWidths = [];
        const headerFont      = new ChartFont(undefined, INTERACTIVE_TREE_HEADER_FONT, InteractiveTreeComponent.HEADER_LABEL_FONT_SIZE, "bolder").toFontName();
        for (let i = 0; i < this.columns.length; i++)
        {
            let labels: string[]  = [];
            let hasMultipleLeaves = false;
            for (let cell of this.columns[i])
            {
                labels.push(cell.node.label);

                // Process parent columns to get flex values
                if (i < this.columns.length - 1)
                {
                    let leaves = cell.getLeaves();
                    hasMultipleLeaves ||= leaves.length > 1;

                    if (cell.height !== null)
                    {
                        cell.flex = `0 0 ${cell.height}px`;
                    }
                    else
                    {
                        // Get the leaves of this node
                        let autoLeaves  = 0;
                        let cumLeafSize = 0;

                        for (let leaf of leaves)
                        {
                            if (leaf.height === null)
                            {
                                autoLeaves++;
                            }
                            else
                            {
                                cumLeafSize += leaf.height;
                            }
                        }

                        let percent = (autoLeaves / numAuto) * 100;
                        let skew    = (cumHeight / numAuto) * autoLeaves;
                        cell.flex   = `1 1 calc(${percent}% + ${cumLeafSize}px - ${skew}px)`;
                    }
                }
            }

            if (i < this.m_table.columns.length)
            {
                let header       = this.m_table.table.headerName(this.dynamicColWidths.length);
                let headerLength = ChartHelpers.measureText(headerFont, header.toLocaleUpperCase()) + 20; // 20px horizontal padding

                let smartLength = UtilsService.predictSmartLength(labels, InteractiveTreeComponent.NODE_LABEL_FONT_SIZE, null, null);
                if (hasMultipleLeaves) smartLength += 26; // 26px for expand button

                let colWidth = Math.max(headerLength, smartLength) * 1.05; // ellipsis can be greedy
                if (i > 0) colWidth += 1; // left-border of 1px on all but leftmost column
                colWidth = Math.min(InteractiveTreeComponent.MAX_COL_WIDTH, colWidth);
                this.dynamicColWidths.push(colWidth);
            }
        }

        let height = cumHeight + numAuto * InteractiveTreeComponent.MIN_ROW_HEIGHT;
        if (this.minHeight !== height)
        {
            this.minHeight = height;
            this.minHeightChanged.emit(this.minHeight);
        }

        let width = UtilsService.sum(...this.dynamicColWidths);
        if (width !== this.cumWidth)
        {
            this.cumWidth = width;
            this.widthChanged.emit(this.cumWidth);
        }

        this.rows.emit(this.getRows());
    }

    private getRows(): InteractiveTreeRow[]
    {
        let lastColumn = this.columns[this.columns.length - 1];
        return lastColumn.map((cell,
                               rowIdx) => new InteractiveTreeRow(this.m_table.rows[rowIdx], cell.shouldShowContent(), cell.flex, cell.height));
    }

    private wrap(component: InteractiveTreeComponent,
                 node: InteractiveTreeNode,
                 column: number,
                 parent: ProcessedTreeNode): ProcessedTreeNode[]
    {
        let wrapper            = new ProcessedTreeNode();
        wrapper.component      = component;
        wrapper.parent         = parent;
        wrapper.node           = node;
        wrapper.column         = column;
        wrapper.underCollapsed = parent && (parent.node.collapsed || parent.underCollapsed);
        wrapper.underHidden    = parent && (parent.node.hidden || parent.underHidden);

        let nodes = [wrapper];
        if (node.children?.length > 0)
        {
            let subtrees           = [];
            wrapper.cumFixedHeight = 0;
            for (let child of node.children)
            {
                let wrappedChild = this.wrap(component, child, column + 1, wrapper);
                wrapper.cumFixedHeight += wrappedChild[0].cumFixedHeight;
                subtrees.push(wrappedChild);
            }

            for (let subtree of subtrees)
            {
                if (subtree.length >= 1) wrapper.children.push(subtree[0]);
                nodes.push(...subtree);
            }
        }
        else if (node.cumFixedHeightRaw)
        {
            wrapper.height         = node.cumFixedHeightRaw;
            wrapper.cumFixedHeight = node.cumFixedHeightRaw;
        }

        if (node.hidden)
        {
            wrapper.cumFixedHeight = 0;
        }
        else if (node.collapsed)
        {
            wrapper.cumFixedHeight = InteractiveTreeComponent.MIN_ROW_HEIGHT;
        }
        wrapper.minHeight = wrapper.cumFixedHeight;

        if (node.hidden)
        {
            wrapper.cumFixedHeight = 0;
        }
        else if (node.collapsed)
        {
            wrapper.cumFixedHeight = InteractiveTreeComponent.MIN_ROW_HEIGHT;
        }

        return nodes;
    }
}

export class InteractiveTreeRow
{
    constructor(public readonly pivotRow: PivotTableRow,
                public readonly displayContent: boolean,
                public readonly flexCss: string,
                public readonly size: number)
    {}
}

class ProcessedTreeNode
{
    public component: InteractiveTreeComponent;
    public parent: ProcessedTreeNode;
    public children: ProcessedTreeNode[] = [];
    public node: InteractiveTreeNode;
    public flex: string;
    public column: number;
    public cumFixedHeight: number;
    public height: number                = null;
    public underCollapsed: boolean;
    public underHidden: boolean;

    get hidden(): boolean
    {
        return this.node.hidden;
    }

    get collapsed(): boolean
    {
        return this.node.collapsed;
    }

    private m_minHeight: number;
    set minHeight(minHeight: number)
    {
        this.m_minHeight = minHeight;
    }

    get minHeight(): number
    {
        if (this.hidden || this.underHidden || this.collapsed || this.underCollapsed) return 0;
        return this.m_minHeight;
    }

    get flexStyle(): string
    {
        return this.hidden || this.underHidden ? null : this.flex;
    }

    public static extract(nodes: ProcessedTreeNode[]): InteractiveTreeNode[]
    {
        return nodes.map((node) => node.node);
    }

    public static distribute(parent: ProcessedTreeNode)
    {
        let children = parent.getChildren();

        // Filter out hidden nodes
        let hidden  = [];
        let visible = [];
        for (let node of children)
        {
            if (node.hidden || node.underHidden)
            {
                hidden.push(node);
            }
            else
            {
                visible.push(node);
            }
        }

        // Give all hidden items height of 0
        for (let node of hidden) node.height = 0;

        // If parent has a fixed size, distribute remaining nodes
        if (parent.height !== null)
        {
            if (!parent.underCollapsed && !parent.collapsed)
            {
                let divided = parent.height / visible.length;
                for (let node of visible) node.height = divided;
            }
            else if (visible.length > 0)
            {
                for (let node of visible) node.height = 0;
                visible[0].height = parent.height;
            }
        }
    }

    public getChildren(excludeHidden: boolean    = false,
                       excludeCollapsed: boolean = false): ProcessedTreeNode[]
    {
        if (!this.children) return [];
        return this.children.filter((node) =>
                                    {
                                        if (excludeHidden && (node.hidden || node.underHidden)) return false;
                                        if (excludeCollapsed && node.underCollapsed) return false;
                                        return true;
                                    });
    }

    public getSiblings(excludeHidden: boolean    = false,
                       excludeCollapsed: boolean = false): ProcessedTreeNode[]
    {
        let children = this.column === 0 ? this.component.columns[0] : this.children;
        return children.filter((node) =>
                               {
                                   if (excludeHidden && (node.hidden || node.underHidden)) return false;
                                   if (excludeCollapsed && node.underCollapsed) return false;
                                   if (node === this) return false;
                                   return true;
                               });
    }

    public getLeaves(excludeHidden: boolean    = false,
                     excludeCollapsed: boolean = false): ProcessedTreeNode[]
    {
        let leaves: ProcessedTreeNode[] = [];
        if (this.children?.length > 0)
        {
            for (let child of this.children) leaves.push(...child.getLeaves(excludeHidden, excludeCollapsed));
        }
        else
        {
            let hidden    = excludeHidden && (this.hidden || this.underHidden);
            let collapsed = excludeCollapsed && this.underCollapsed;

            if (!hidden && !collapsed)
            {
                leaves.push(this);
            }
        }

        return leaves;
    }

    public hasMultipleChildren(): boolean
    {
        return this.node.children?.length > 1;
    }

    public shouldShowContent(): boolean
    {
        return !this.hidden && !this.underHidden && !this.underCollapsed;
    }

    public hasDescription(): boolean
    {
        return !!this.node.description;
    }

    public classes()
    {
        return {
            "tree-node"          : this.shouldShowContent(),
            "tree-node-hidden"   : this.hidden,
            "tree-node-omitted"  : this.underHidden,
            "tree-node-collapsed": this.collapsed
        };
    }
}

export interface InteractiveTreeNode
{
    id: string,
    label: string,
    description: string,
    collapsed: boolean,
    hidden: boolean,
    children: InteractiveTreeNode[]
    cumFixedHeightRaw: number;
}

export class PivotTable
{
    public readonly virtualNodes: Models.VirtualAssetGraphNode[];
    public readonly loaded = new Future<boolean>();

    public readonly nodeIdToDepth: Lookup<number>   = {};
    private readonly m_nodeIdToName: Lookup<string> = {};

    public readonly assets: Lookup<Models.Asset>              = {};
    public readonly locations: Lookup<LocationExtended>       = {};
    public readonly points: Lookup<Models.PointClass>         = {};
    public readonly equipments: Lookup<Models.EquipmentClass> = {};

    public readonly virtualLeafNodeId: string;

    constructor(public readonly tuples: AssetGraphHierarchyTupleResult[],
                virtualNodes: Models.VirtualAssetGraphNode[],
                graphExt: AssetGraphExtended,
                public readonly leafNodeIds: string[])
    {
        this.virtualNodes = virtualNodes || [];

        let leafNodeSet = new Set(leafNodeIds);
        let leafDepth   = 0;
        for (let node of graphExt.model.nodes)
        {
            this.m_nodeIdToName[node.id] = node.name;

            let nodeDepth               = graphExt.getNodeDepth(node.id);
            this.nodeIdToDepth[node.id] = nodeDepth;
            if (leafNodeSet.has(node.id)) leafDepth = nodeDepth;
        }

        this.virtualLeafNodeId = this.virtualNodes[this.virtualNodes.length - 1]?.nodeId;
        if (!this.virtualLeafNodeId)
        {
            // an undefined node id is used when there are multiple leaf nodes in use: uses generic column naming too
            this.nodeIdToDepth[this.virtualLeafNodeId]  = leafDepth;
            this.m_nodeIdToName[this.virtualLeafNodeId] = "Leaf Nodes";
        }
    }

    nodeName(nodeId: string): string
    {
        return this.m_nodeIdToName[nodeId];
    }

    headerName(colIdx: number): string
    {
        return this.nodeName(this.virtualNodes[colIdx]?.nodeId) || `Column ${colIdx + 1}`;
    }

    columnIds(index: number): string[]
    {
        return this.tuples.map((tuple) => tuple.tuple[index]);
    }

    columnAssets(index: number): Models.Asset[]
    {
        return this.columnIds(index)
                   .map((id) => this.assets[id]);
    }

    async load(app: AppContext)
    {
        // Get a de-duped set of IDs
        let set = new Set<string>();
        for (let row of this.tuples)
        {
            for (let id of row.tuple) set.add(id);
        }

        // Turn the set into the flat list
        let ids = Array.from(set);

        // Do a batch call to retrieve assets
        let assets = await app.domain.apis.assets.getBatch(ids);

        // Drop non-existing assets.
        assets = assets.filter(asset => !!asset);

        // Add assets to map
        for (let asset of assets) this.assets[asset.sysId] = asset;
        for (let tuple of this.tuples) tuple.populateAssets(this.assets);

        // Get distinct location, pointClass and equipmentClass ids
        let locationIds  = new Set<RecordIdentity>();
        let pointIds     = new Set<string>();
        let equipmentIds = new Set<string>();

        for (let asset of assets)
        {
            if (asset.location) locationIds.add(asset.location);
            if (asset instanceof Models.DeviceElement && asset.pointClassId) pointIds.add(asset.pointClassId);
            if (asset instanceof Models.LogicalAsset && asset.equipmentClassId) equipmentIds.add(asset.equipmentClassId);
        }

        // Resolve all location ids to records
        let locations = await app.domain.assets.getTypedExtendedBatch(LocationExtended, Array.from(locationIds));
        for (let location of locations)
        {
            this.locations[location.model.sysId] = location;
        }

        // Fetch all point/equip classes and resolve them to records
        let rules = await app.bindings.getActiveNormalizationRules();
        rules.pointClasses.forEach((point) =>
                                   {
                                       let pointId = `${point.id}`;
                                       if (pointIds.has(pointId)) this.points[pointId] = point;
                                   });
        rules.equipmentClasses.forEach((equipment) =>
                                       {
                                           let equipmentId = `${equipment.id}`;
                                           if (equipmentIds.has(equipmentId)) this.equipments[equipmentId] = equipment;
                                       });
    }
}

export class VirtualAssetGraphNodeExtended
{
    get label(): string
    {
        return VirtualAssetGraphNodeExtended.fromTypeToLabel(this.model.type) || "";
    }

    constructor(public model: Models.VirtualAssetGraphNode,
                public table: PivotTable,
                public readonly structural?: boolean)
    {}

    public static newModel(nodeId: string,
                           type: Models.VirtualAssetGraphNodeType): Models.VirtualAssetGraphNode
    {
        return Models.VirtualAssetGraphNode.newInstance({
                                                            nodeId   : nodeId,
                                                            type     : type,
                                                            ascending: true,
                                                            collapsed: false
                                                        });
    }

    public static fromTypeToLabel(type: Models.VirtualAssetGraphNodeType): string
    {
        switch (type)
        {
            case Models.VirtualAssetGraphNodeType.Name:
                return "Name";

            case Models.VirtualAssetGraphNodeType.Identifier:
                return "Asset";

            case Models.VirtualAssetGraphNodeType.Location:
                return "Location";

            case Models.VirtualAssetGraphNodeType.FullLocation:
                return "Full Location";

            case Models.VirtualAssetGraphNodeType.PointClass:
                return "Point Class";

            case Models.VirtualAssetGraphNodeType.EquipmentClass:
                return "Equipment Class";
        }

        return null;
    }

    async accessLabel(asset: Models.Asset,
                      pivot: PivotTable): Promise<string>
    {
        switch (this.model.type)
        {
            case Models.VirtualAssetGraphNodeType.Name:
            case Models.VirtualAssetGraphNodeType.Identifier:
                return asset.name;

            case Models.VirtualAssetGraphNodeType.Location:
            case Models.VirtualAssetGraphNodeType.FullLocation:
                if (asset.location)
                {
                    let locExt = pivot.locations[asset.location.sysId];
                    if (locExt)
                    {
                        let locName: string;
                        if (this.model.type === Models.VirtualAssetGraphNodeType.FullLocation)
                        {
                            locName = await locExt.getRecursiveName();
                        }

                        return locName || locExt.typedModel.name;
                    }
                }
                break;

            case Models.VirtualAssetGraphNodeType.PointClass:
                if (asset instanceof Models.DeviceElement)
                {
                    let pt = pivot.points[asset.pointClassId];
                    if (pt)
                    {
                        return pt.pointClassDescription;
                    }
                }
                break;

            case Models.VirtualAssetGraphNodeType.EquipmentClass:
                if (asset instanceof Models.LogicalAsset)
                {
                    let equip = pivot.equipments[asset.equipmentClassId];
                    if (equip)
                    {
                        return equip.description;
                    }
                }
                break;
        }

        // Default to empty
        return "(Empty)";
    }

    accessValue(asset: Models.Asset): string
    {
        if (asset)
        {
            switch (this.model.type)
            {
                case Models.VirtualAssetGraphNodeType.Name:
                case Models.VirtualAssetGraphNodeType.Identifier:
                    return asset.sysId;

                case Models.VirtualAssetGraphNodeType.Location:
                case Models.VirtualAssetGraphNodeType.FullLocation:
                    return asset.location?.sysId;

                case Models.VirtualAssetGraphNodeType.PointClass:
                    return asset instanceof Models.DeviceElement ? asset.pointClassId : null;

                case Models.VirtualAssetGraphNodeType.EquipmentClass:
                    return asset instanceof Models.LogicalAsset ? asset.equipmentClassId : null;
            }

            if (this.structural) return asset.sysId;
        }

        // Default to null
        return null;
    }

    uniqueAssets(table: PivotTable): Map<string, Models.Asset>
    {
        let unique       = new Map<string, Models.Asset>();
        let columnAssets = table.columnAssets(table.nodeIdToDepth[this.model.nodeId]);
        for (let asset of columnAssets)
        {
            let value = this.accessValue(asset);
            if (!unique.has(value)) unique.set(value, asset);
        }

        return unique;
    }
}

export class PivotTableView
{
    public readonly columns: VirtualAssetGraphNodeExtended[] = [];
    private readonly structuralColumns: VirtualAssetGraphNodeExtended[];

    public readonly uniqueNodes = new Map<VirtualAssetGraphNodeExtended, Map<string, PivotTableCell>>();

    public rows: PivotTableRow[] = [];

    private m_leafNodeToFixedHeight: Lookup<number> = {};
    heightsChanged                                  = new Subject<void>();

    public static async new(table: PivotTable,
                            bindings: Models.HierarchicalVisualizationBinding[]): Promise<PivotTableView>
    {
        let pivotTableView = new PivotTableView(table);
        pivotTableView.updateFixedHeights(bindings);
        await pivotTableView.initialize();
        return pivotTableView;
    }

    private constructor(public readonly table: PivotTable)
    {
        // Get all active columns
        this.columns = this.table.virtualNodes.map((node) => new VirtualAssetGraphNodeExtended(node, this.table));

        this.structuralColumns = UtilsService.arrayCopy(this.columns);
        this.structuralColumns.push(new VirtualAssetGraphNodeExtended(VirtualAssetGraphNodeExtended.newModel(this.table.virtualLeafNodeId, null), this.table, true));
    }

    private async initialize()
    {
        // Cache the unique entries per column
        let colsInitialized = [];
        for (let column of this.structuralColumns)
        {
            let assets      = column.uniqueAssets(this.table);
            let nodes       = new Map<string, PivotTableCell>();
            let initialized = [];
            for (let key of assets.keys())
            {
                let unique = new PivotTableCell(this.table, column, assets.get(key));
                initialized.push(unique.initialize());
                nodes.set(key, unique);
            }
            colsInitialized.push(Promise.all(initialized));

            this.uniqueNodes.set(column, nodes);
        }

        // Render when ready
        await Promise.all(colsInitialized);
        this.render();
    }

    updateFixedHeights(bindings: Models.HierarchicalVisualizationBinding[])
    {
        this.m_leafNodeToFixedHeight = {};
        for (let binding of bindings)
        {
            this.m_leafNodeToFixedHeight[binding.leafNodeId] = binding.options?.sizing === Models.HierarchicalVisualizationSizing.FIXED && binding.options.size || null;
        }
        this.heightsChanged.next();
    }

    reorderColumns(oldIndex: number,
                   newIndex: number): boolean
    {
        // If they are the same or out of bounds do nothing
        if (oldIndex === newIndex) return false;
        if (oldIndex < 0 || newIndex < 0) return false;
        if (oldIndex >= this.columns.length || newIndex >= this.columns.length) return false;

        // Reorder columns
        moveItemInArray(this.columns, oldIndex, newIndex);
        moveItemInArray(this.table.virtualNodes, oldIndex, newIndex);

        // Re-render the table
        this.render();

        // Return true to indicate success
        return true;
    }

    asInteractiveTree(): InteractiveTreeNode[]
    {
        if (!this.rows?.length) return [];

        return this.getUniques(0, this.rows.length, 0)
                   .map((range) => this.asInteractiveTreeRecursive(range));
    }

    private asInteractiveTreeRecursive(range: UniqueRange): InteractiveTreeNode
    {
        let children       = null;
        let cumFixedHeight = 0;
        if (range.column < this.structuralColumns.length - 1)
        {
            children = this.getUniques(range.start, range.end, range.column + 1)
                           .map((range) => this.asInteractiveTreeRecursive(range));
            for (let child of children) cumFixedHeight += child.cumFixedHeightRaw;
        }
        else
        {
            for (let i = range.start; i < range.end && i < this.rows.length; i++)
            {
                let fixedHeight = this.m_leafNodeToFixedHeight[this.rows[i].leafNodeId] || 0;
                cumFixedHeight += fixedHeight;
            }
        }

        return {
            id               : range.unique.value,
            label            : range.unique.label,
            description      : null,
            collapsed        : false,
            hidden           : false,
            children         : children,
            cumFixedHeightRaw: cumFixedHeight
        };
    }

    private getUniques(rowStart: number,
                       rowEnd: number,
                       columnIdx: number): UniqueRange[]
    {
        let ranges: UniqueRange[] = [];

        let current = this.rows[rowStart].cells[columnIdx];
        for (let i = rowStart + 1; i < rowEnd; i++)
        {
            let cell = this.rows[i].cells[columnIdx];
            if (!current.groupWith(cell))
            {
                ranges.push(new UniqueRange(rowStart, i, columnIdx, current));
                rowStart = i;
                current  = cell;
            }
        }
        ranges.push(new UniqueRange(rowStart, rowEnd, columnIdx, current));

        return ranges;
    }

    private render()
    {
        // build rows
        this.rows = this.table.tuples.map((tuple) =>
                                          {
                                              const assets        = tuple.assets;
                                              const deviceElement = <Models.DeviceElement>assets.find((asset) => asset instanceof Models.DeviceElement);
                                              const cells         = this.structuralColumns.map((column) =>
                                                                                               {
                                                                                                   const colNodes      = this.uniqueNodes.get(column);
                                                                                                   const relevantAsset = assets[this.table.nodeIdToDepth[column.model.nodeId]];
                                                                                                   return colNodes.get(column.accessValue(relevantAsset));
                                                                                               });

                                              return new PivotTableRow(assets[0].sysId, tuple.nodeId, deviceElement?.sysId, cells);
                                          });

        // sort rows
        for (let i = this.columns.length - 1; i >= 0; i--)
        {
            this.sortColumn(i, this.columns[i].model.ascending);
        }
        this.updateRowIndices();
    }

    private sortColumn(colIdx: number,
                       ascending: boolean)
    {
        this.updateRowIndices();

        // Perform a stable sort
        this.rows.sort((a,
                        b) =>
                       {
                           let aLabel     = a.cells[colIdx].label;
                           let bLabel     = b.cells[colIdx].label;
                           let equivalent = !aLabel && !bLabel || aLabel == bLabel;
                           if (equivalent) return a.rowIdx - b.rowIdx;

                           return UtilsService.compareStrings(aLabel, bLabel, ascending);
                       });
    }

    private updateRowIndices()
    {
        for (let i = 0; i < this.rows.length; i++) this.rows[i].rowIdx = i;
    }
}

class UniqueRange
{
    constructor(public start: number,
                public end: number,
                public column: number,
                public unique: PivotTableCell)
    {}
}

class PivotTableRow
{
    public rowIdx: number;

    constructor(public readonly rootId: string,
                public readonly leafNodeId: string,
                public readonly deviceElemId: string,
                public cells: PivotTableCell[])
    {
    }
}

class PivotTableCell
{
    public value: string;
    public label: string;

    constructor(private table: PivotTable,
                private column: VirtualAssetGraphNodeExtended,
                readonly asset: Models.Asset)
    {
    }

    public async initialize(): Promise<void>
    {
        this.value = this.column.accessValue(this.asset);
        this.label = await this.column.accessLabel(this.asset, this.table);
    }

    groupWith(other: PivotTableCell): boolean
    {
        let compareValue = this.column.model.type === Models.VirtualAssetGraphNodeType.Identifier ||
                           this.column.structural && other.column.structural;
        if (compareValue) return this.value === other.value;
        return this.label === other.label;
    }
}
