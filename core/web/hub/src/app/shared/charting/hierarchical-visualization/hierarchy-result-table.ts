import * as Models from "app/services/proxy/model/models";

export class HierarchyResult
{
    constructor(public readonly roots: HierarchyResultNode[])
    {}
}

export class HierarchyResultNode
{
    public readonly children: HierarchyResultNode[] = [];

    constructor(public readonly id: string,
                public readonly label: string,
                public readonly type: HierarchyResultNodeType)
    {}
}

export enum HierarchyResultNodeType
{
    controlPoint = "controlPoint",
    group        = "group"
}

export class ResultTable
{
    public columns: ResultColumn[] = [];
    public rows: ResultRow[]       = [];

    constructor(public table: Models.Asset[][],
                query: Models.TagsJoinQuery)
    {
        this.columns = ResultColumn.fromExpanded(this);
        this.rows    = ResultRow.fromExpanded(this);

        // Assign order number
        for (let i = 0; i < this.renderableColumnsLast.length; i++) this.columns[i].order = i;

        // Add in labels
        for (let i = 0; i < this.naturalOrderedColumns.length; i++)
        {
            let term = query.terms[i];
            if (QueryNode.isEncoded(term.id)) this.columns[i].label = QueryNode.decodeLabel(term.id);
        }
    }

    get renderableColumnsLast(): ResultColumn[]
    {
        return this.columns.sort((a,
                                  b) =>
                                 {
                                     let aValue = a.isRenderable ? 1 : 0;
                                     let bValue = b.isRenderable ? 1 : 0;
                                     return aValue - bValue;
                                 });
    }

    get orderedColumns(): ResultColumn[]
    {
        return this.columns.sort((a,
                                  b) => a.order - b.order);
    }

    get orderedRows(): ResultRow[]
    {
        return this.rows.sort((a,
                               b) => a.order - b.order);
    }

    get naturalOrderedColumns(): ResultColumn[]
    {
        return this.columns.sort((a,
                                  b) => a.index - b.index);
    }

    get naturalOrderedRows(): ResultRow[]
    {
        return this.rows.sort((a,
                               b) => a.index - b.index);
    }

    toHierarchyResult(): HierarchyResult
    {
        let levels: Map<string, ResultValue>[]         = [];
        let models: Map<string, HierarchyResultNode>[] = [];

        // Set up maps and levels
        for (let column of this.columns)
        {
            levels.push(new Map<string, ResultValue>());
            models.push(new Map<string, HierarchyResultNode>());
        }

        // Process rows
        for (let row of this.orderedRows)
        {
            let values = row.orderedValues;
            for (let i = 0; i < values.length; i++)
            {
                let value    = values[i];
                let levelMap = levels[i];
                let modelMap = models[i];

                if (!levelMap.has(value.asset.sysId)) levelMap.set(value.asset.sysId, value);
                if (!modelMap.has(value.asset.sysId)) modelMap.set(value.asset.sysId, new HierarchyResultNode(value.asset.sysId, value.asset.name, value.type));
            }
        }

        // Build hierarchy
        for (let i = models.length - 1; i > 0; i--)
        {
            for (let value of Array.from(levels[i].values()))
            {
                let ordered = value.row.orderedValues;
                let child   = models[i].get(ordered[i].asset.sysId);
                let parent  = models[i - 1].get(ordered[i - 1].asset.sysId);

                parent.children.push(child);
            }
        }

        return new HierarchyResult(Array.from(models[0].values()));
    }
}

class ResultColumn
{
    public table: ResultTable;
    public label: string;
    public id: string;
    public isRenderable: boolean;
    public index: number;
    public order: number;
    public primary: boolean;

    static fromExpanded(table: ResultTable): ResultColumn[]
    {
        let columns = [];
        let row     = table.table[0];
        for (let i = 0; i < row.length; i++)
        {
            let asset           = row[i];
            let column          = new ResultColumn();
            column.id           = `${i}`;
            column.index        = i;
            column.order        = i;
            column.isRenderable = asset instanceof Models.DeviceElement;
            column.label        = asset.__type;
            column.table        = table;

            columns.push(column);
        }

        return columns;
    }

    get values(): Models.Asset[]
    {
        let values = [];
        for (let row of this.table.table)
        {
            values.push(row[this.index]);
        }

        return values;
    }
}

class ResultRow
{
    public table: ResultTable;
    public index: number;
    public order: number;
    public selected: boolean = true;

    static fromExpanded(table: ResultTable): ResultRow[]
    {
        let rows = [];

        // Build all data rows
        for (let i = 0; i < table.table.length; i++)
        {
            let row   = new ResultRow();
            row.index = i;
            row.order = i;
            row.table = table;

            rows.push(row);
        }

        return rows;
    }

    get values(): Models.Asset[]
    {
        let values = [];
        for (let cell of this.table.table[this.index])
        {
            values.push(cell);
        }

        return values;
    }

    get orderedValues(): ResultValue[]
    {
        return this.table.orderedColumns.map((column) => new ResultValue(this.table.table[this.index][column.index], this, column));
    }
}

class ResultValue
{
    public readonly type: HierarchyResultNodeType;

    constructor(public asset: Models.Asset,
                public row: ResultRow,
                public column: ResultColumn)
    {
        this.type = this.asset instanceof Models.DeviceElement ? HierarchyResultNodeType.controlPoint : HierarchyResultNodeType.group;
    }
}

export class QueryNode
{
    public label: string                   = "";
    public id: string                      = null;
    public condition: Models.TagsCondition = null;
    public relationship: Models.AssetRelationship;
    public conditionValid: boolean         = false;
    public previous: QueryNode;
    public next: QueryNode;

    static fromModel(model: Models.TagsJoinQuery): QueryNode[]
    {
        if (!model) return [];

        // Build up the nodes
        let terms = new Map<string, QueryNode>();
        for (let term of model.terms)
        {
            let node       = new QueryNode();
            node.condition = term.conditions;
            node.id        = term.id;
            if (QueryNode.isEncoded(term.id)) node.label = QueryNode.decodeLabel(term.id);

            terms.set(term.id, node);
        }

        // Add the relationships back in
        for (let join of model.joins)
        {
            let left  = terms.get(join.leftSide);
            let right = terms.get(join.rightSide);
            if (left && right)
            {
                left.relationship = join.relation;
                left.next         = right;
                right.previous    = left;
            }
        }

        // Return the nodes
        return Array.from(terms.values());
    }

    static toModel(nodes: QueryNode[]): Models.TagsJoinQuery
    {
        // Assign id's to the nodes
        let ids                         = 1;
        let map: Map<string, QueryNode> = new Map<string, QueryNode>();

        for (let node of nodes)
        {
            if (node.label)
            {
                node.id = QueryNode.encodeLabel(node.label);
            }
            else
            {
                node.id = `${ids}`;
                ids++;
            }

            map.set(node.id, node);
        }

        // Build terms for the model
        let terms = Array.from(map.entries())
                         .map((entry) =>
                              {
                                  return Models.TagsJoinTerm.newInstance({
                                                                             id        : entry[1].id,
                                                                             conditions: entry[1].condition
                                                                         });
                              });

        // Build joins for the model
        let joins = [];
        for (let i = 1; i < terms.length; i++)
        {
            let l    = terms[i - 1];
            let r    = terms[i];
            let node = map.get(l.id);
            joins.push(Models.TagsJoin.newInstance({
                                                       leftSide : l.id,
                                                       rightSide: r.id,
                                                       relation : node.relationship
                                                   }));
        }

        // Return a new model
        return Models.TagsJoinQuery.newInstance({
                                                    terms: terms,
                                                    joins: joins
                                                });
    }

    static encodeLabel(decoded: string): string
    {
        return btoa(decoded);
    }

    static decodeLabel(encoded: string): string
    {
        return atob(encoded);
    }

    static isEncoded(input: string): boolean
    {
        try
        {
            atob(input);
        }
        catch (e)
        {
            return false;
        }

        return true;
    }

    static columnLabels(model: Models.TagsJoinQuery)
    {
        return model.terms.map((term,
                                index) =>
                               {
                                   if (this.isEncoded(term.id)) return this.decodeLabel(term.id);
                                   return `Column ${index + 1}`;
                               });
    }

    isValid(): boolean
    {
        return this.isLabelValid() && this.isConditionValid() && this.isRelationshipValid();
    }

    isLabelValid(): boolean
    {
        return !!this.label;
    }

    isConditionValid(): boolean
    {
        return this.conditionValid;
    }

    isRelationshipValid(): boolean
    {
        return !!this.relationship;
    }
}
