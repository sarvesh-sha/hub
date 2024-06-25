import * as Models from "app/services/proxy/model/models";

export enum ConditionNodeType
{
    ANYTHING, EQUIPMENT, POINT, TAG, LOCATION, METRICS, METRICSOUTPUT, ASSET, CLASSIFIED
}

export class ConditionNode
{
    public value: string = null;

    private m_type: ConditionNodeType;
    get type(): ConditionNodeType
    {
        return this.m_type;
    }

    private m_negate: boolean;
    get negate(): boolean
    {
        return this.m_negate;
    }

    public controlOptionID: string;

    constructor(type: ConditionNodeType = ConditionNodeType.TAG,
                negate: boolean         = false)
    {
        this.update(ConditionNodes.resolve(type, negate));
    }

    update(option: ConditionNodeOption)
    {
        this.m_type   = option.type;
        this.m_negate = option.negate;

        this.controlOptionID = option.optionId;

        this.value = null;
    }

    static isConditionNode(model: Models.TagsCondition): boolean
    {
        if (model === null) return true; // "Anything" node

        if (model instanceof Models.TagsConditionUnary)
        {
            if (ConditionNode.isConditionNode(model.a)) return true;
        }

        let cond = ConditionNodes.fromModel(model);
        return cond.type != ConditionNodeType.ANYTHING;
    }

    static fromModel(model: Models.TagsCondition): ConditionNode
    {
        // Check if the model given can be parsed as a condition
        if (!ConditionNode.isConditionNode(model)) return null;

        return ConditionNodes.fromModel(model);
    }

    isValid(): boolean
    {
        if (this.type === ConditionNodeType.ANYTHING) return true;
        return !!this.value;
    }

    toModel(): Models.TagsCondition
    {
        let cond = ConditionNodes.resolve(this.type, false);
        if (!cond.toModel)
        {
            return null;
        }

        let inner = cond.toModel(this.value);
        return this.negate ? Models.TagsConditionUnaryNot.newInstance({a: inner}) : inner;
    }
}

export class LogicNode
{
    get operation(): Models.TagsConditionOperator
    {
        return this.m_operation;
    }

    get negate(): boolean
    {
        return this.m_negate;
    }

    private m_children: (LogicNode | ConditionNode)[] = [];
    get children(): (LogicNode | ConditionNode)[]
    {
        return this.m_children;
    }

    constructor(private m_operation: Models.TagsConditionOperator = Models.TagsConditionOperator.And,
                private m_negate: boolean                         = false)
    {}

    getLabel()
    {
        switch (this.m_operation)
        {
            case Models.TagsConditionOperator.And:
                return "AND";
            case Models.TagsConditionOperator.Or:
                return "OR";
            case Models.TagsConditionOperator.Xor:
                return "XOR";
        }

        return "";
    }

    cycleOperation()
    {
        if (this.m_operation === Models.TagsConditionOperator.And)
        {
            this.m_operation = Models.TagsConditionOperator.Or;
        }
        else
        {
            this.m_operation = Models.TagsConditionOperator.And;
            this.m_negate    = !this.m_negate;
        }
    }

    isAnd(): boolean
    {
        return this.m_operation === Models.TagsConditionOperator.And;
    }

    isOr(): boolean
    {
        return this.m_operation === Models.TagsConditionOperator.Or;
    }

    static isLogicNode(model: Models.TagsCondition): boolean
    {
        if (model instanceof Models.TagsConditionBinary) return true;
        if (model instanceof Models.TagsConditionUnary)
        {
            if (model.a instanceof Models.TagsConditionBinary) return true;
        }

        return false;
    }

    static fromModel(model: Models.TagsCondition): LogicNode
    {
        // Check if the model given can be parsed as a composition
        if (!LogicNode.isLogicNode(model)) return null;

        // Make a new composition tag
        let tag = new LogicNode();

        // Check for negation
        if (model instanceof Models.TagsConditionUnary)
        {
            tag.m_negate = model instanceof Models.TagsConditionUnaryNot;
            model        = <Models.TagsConditionUnary>model.a;
        }

        // Walk tree and flatten to children
        let typed      = <Models.TagsConditionBinaryLogic>model;
        let type       = typed.op;
        tag.m_children = parse([], type, typed);

        // Adopt op type
        tag.m_operation = typed.op;

        // Return the tag
        return tag;

        // Function to recursively flatten a binary condition tree
        function parse(children: (LogicNode | ConditionNode)[],
                       type: Models.TagsConditionOperator,
                       model: Models.TagsCondition)
        {
            let typed              = <Models.TagsConditionBinaryLogic>model;
            let isNotBinaryLogic   = !(model instanceof Models.TagsConditionBinaryLogic);
            let isDifferentLogicOp = !isNotBinaryLogic && typed.op !== type;
            if (isNotBinaryLogic || isDifferentLogicOp)
            {
                children.push(isDifferentLogicOp ? LogicNode.fromModel(model) : ConditionNode.fromModel(model));
            }
            else
            {
                parse(children, type, typed.a);
                parse(children, type, typed.b);
            }

            return children;
        }
    }

    isValid(): boolean
    {
        return this.children.length > 0 && !!this.m_operation;
    }

    toModel(): Models.TagsCondition
    {
        // Get a transformed list of all children
        let list = this.children.map((child) =>
                                     {
                                         if (child instanceof ConditionNode) return child.toModel();
                                         if (child instanceof LogicNode) return child.toModel();
                                         return undefined;
                                     });

        // Find the initial amount to reduce to round off the list to a power if 2
        // This makes sure our tree is balanced
        let b = list.length - Math.pow(2, Math.floor(Math.log2(list.length)));

        // Perform initial reduction
        reduce(b, list, this.m_operation);

        // Continue reduction until 1 item remains
        while (list.length > 1) reduce(Math.floor(list.length / 2), list, this.m_operation);

        // Return the last remaining node since it is now the root
        return this.m_negate ? Models.TagsConditionUnaryNot.newInstance({a: list[0]}) : list[0];

        // Function to reduce n pairs
        function reduce(n: number,
                        list: Models.TagsCondition[],
                        op: Models.TagsConditionOperator)
        {
            let max       = Math.min(n, Math.floor(list.length / 2));
            let pairs     = [];
            let processed = [];

            // Build the pairs to reduce
            for (let i = 0; i < max; i++)
            {
                pairs.push([
                               list.shift(),
                               list.shift()
                           ]);
            }

            // Reduce the pairs
            for (let pair of pairs)
            {
                processed.push(Models.TagsConditionBinaryLogic.newInstance({
                                                                               op: op,
                                                                               a : pair[0],
                                                                               b : pair[1]
                                                                           }));
            }

            // Add them back to the list
            list.push(...processed);
        }
    }
}

class ConditionNodeOption
{
    public readonly optionId: string;

    constructor(public readonly type: ConditionNodeType,
                public readonly negate: boolean,
                public readonly label: string,
                public fromModel: (model: Models.TagsCondition) => string,
                public toModel: (value: string) => Models.TagsCondition)
    {
        this.optionId = `${type},${negate}`;
    }
}

export namespace ConditionNodes
{
    export function resolve(type: ConditionNodeType,
                            negate: boolean): ConditionNodeOption
    {
        let anything = null;

        for (let cond of ConditionNodeOptions)
        {
            if (cond.type == type && cond.negate == negate) return cond;

            if (cond.type == ConditionNodeType.ANYTHING) anything = cond;
        }

        return anything;
    }

    export function fromModel(model: Models.TagsCondition): ConditionNode
    {
        let negate = false;

        if (model instanceof Models.TagsConditionUnary)
        {
            negate = model instanceof Models.TagsConditionUnaryNot;
            model  = model.a;
        }

        for (let cond of ConditionNodeOptions)
        {
            if (cond.fromModel)
            {
                let value = cond.fromModel(model);
                if (value !== undefined)
                {
                    let tag   = new ConditionNode(cond.type, negate);
                    tag.value = value;
                    return tag;
                }
            }
        }

        return new ConditionNode(ConditionNodeType.ANYTHING, negate);
    }
}

export const ConditionNodeOptions: ConditionNodeOption[] = [
    new ConditionNodeOption(ConditionNodeType.ANYTHING, false, "Is Anything", null, null),

    new ConditionNodeOption(ConditionNodeType.POINT,
                            false,
                            "With Point Class",
                            (model) => (model instanceof Models.TagsConditionPointClass) ? (model.pointClass || null) : undefined,
                            (val) => Models.TagsConditionPointClass.newInstance({pointClass: val})),
    new ConditionNodeOption(ConditionNodeType.POINT, true, "Without Point Class", null, null),

    new ConditionNodeOption(ConditionNodeType.EQUIPMENT,
                            false,
                            "With Equipment Class",
                            (model) => (model instanceof Models.TagsConditionEquipmentClass) ? (model.equipmentClass || null) : undefined,
                            (val) => Models.TagsConditionEquipmentClass.newInstance({equipmentClass: val})),
    new ConditionNodeOption(ConditionNodeType.EQUIPMENT, true, "Without Equipment Class", null, null),

    new ConditionNodeOption(ConditionNodeType.ASSET,
                            false,
                            "Is Asset",
                            (model) => (model instanceof Models.TagsConditionIsAsset) ? (model.sysId || null) : undefined,
                            (val) => Models.TagsConditionIsAsset.newInstance({sysId: val})),
    new ConditionNodeOption(ConditionNodeType.ASSET, true, "Is Not Asset", null, null),

    new ConditionNodeOption(ConditionNodeType.CLASSIFIED,
                            false,
                            "Is Classified",
                            (model) => (model instanceof Models.TagsConditionIsClassified) ? null : undefined,
                            (val) => Models.TagsConditionIsClassified.newInstance({})),
    new ConditionNodeOption(ConditionNodeType.CLASSIFIED, true, "Is Not Classified", null, null),

    new ConditionNodeOption(ConditionNodeType.TAG,
                            false,
                            "With Tag",
                            (model) => (model instanceof Models.TagsConditionTerm) ? (model.tag || null) : undefined,
                            (val) => Models.TagsConditionTerm.newInstance({tag: val})),
    new ConditionNodeOption(ConditionNodeType.TAG, true, "Without Tag", null, null),

    new ConditionNodeOption(ConditionNodeType.LOCATION,
                            false,
                            "In Location",
                            (model) => (model instanceof Models.TagsConditionLocation) ? (model.locationSysId || null) : undefined,
                            (val) => Models.TagsConditionLocation.newInstance({locationSysId: val})),
    new ConditionNodeOption(ConditionNodeType.LOCATION, true, "Not in Location", null, null),

    new ConditionNodeOption(ConditionNodeType.METRICS,
                            false,
                            "Is Metrics",
                            (model) => (model instanceof Models.TagsConditionMetrics) ? (model.metricsSysId || null) : undefined,
                            (val) => Models.TagsConditionMetrics.newInstance({metricsSysId: val})),
    new ConditionNodeOption(ConditionNodeType.METRICS, true, "Is Not Metrics", null, null),

    new ConditionNodeOption(ConditionNodeType.METRICSOUTPUT,
                            false,
                            "Is Metrics Output",
                            (model) => (model instanceof Models.TagsConditionMetricsOutput) ? (model.metricsOutput || null) : undefined,
                            (val) => Models.TagsConditionMetricsOutput.newInstance({metricsOutput: val})),
    new ConditionNodeOption(ConditionNodeType.METRICSOUTPUT, true, "Is Not Metrics Output", null, null)
];
