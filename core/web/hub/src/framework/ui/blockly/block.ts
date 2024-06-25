import {Type} from "@angular/core";
import * as Blockly from "blockly";
import {Lookup} from "framework/services/utils.service";
import {BlocklyExtendedWorkspace, BlocklyWorkspaceComponent} from "framework/ui/blockly/blockly-workspace.component";
import {ComponentContext} from "framework/ui/components";

export interface BaseBlockModel
{
    id: string;

    x?: number;

    y?: number;
}

export interface BlockDefinition<T extends BaseBlockModel | void> extends Type<Block<T>>
{
}

export interface BlockValue
{
    __type: string;
}

class BlockValueDescriptor
{
    private static s_lookup: Map<Type<BlockValue>, BlockValueDescriptor> = new Map();
    private static s_lookupReverse: Map<string, BlockValueDescriptor>    = new Map();

    public readonly id: string;

    superType: BlockValueDescriptor;
    subTypes: BlockValueDescriptor[] = [];

    constructor(public readonly type: Type<BlockValue>)
    {
        let name = this.type.name;

        // Hacky fixup, because Blockly wants to see some well-known names for primitives.
        if (name.endsWith("PrimitiveBoolean")) name = "Boolean";
        if (name.endsWith("PrimitiveNumber")) name = "Number";
        if (name.endsWith("PrimitiveString")) name = "String";

        this.id = name;
    }

    public static reset()
    {
        BlockValueDescriptor.s_lookup.clear();
        BlockValueDescriptor.s_lookupReverse.clear();
    }

    public static register(type: Type<BlockValue>): BlockValueDescriptor
    {
        let desc = BlockValueDescriptor.s_lookup.get(type);
        if (!desc)
        {
            desc = new BlockValueDescriptor(type);
            BlockValueDescriptor.s_lookup.set(type, desc);
            BlockValueDescriptor.s_lookupReverse.set(desc.id, desc);

            let parentProto = Object.getPrototypeOf(type.prototype);
            if (parentProto !== Object.prototype)
            {
                let parentDesc = BlockValueDescriptor.register(parentProto.constructor);

                desc.superType = parentDesc;
                parentDesc.subTypes.push(desc);
            }
        }

        return desc;
    }

    public static lookup(id: string): BlockValueDescriptor
    {
        return BlockValueDescriptor.s_lookupReverse.get(id);
    }
}

export interface BlockConfig<S extends BaseBlockModel, T extends BlockDefinition<S>, S1 extends BlockValue, S2 extends BlockValue>
{
    blockContext: string;
    blockName: string;
    color?: number;
    model: Type<S>;

    outputType?: Type<S1>;
    outputElementType?: Type<S2>;
}

export class BlockDescriptor<S extends BaseBlockModel, T extends BlockDefinition<S>, S1 extends BlockValue, S2 extends BlockValue>
{
    definition: T;

    constructor(public readonly config: BlockConfig<S, T, S1, S2>)
    {
    }

    public linkValueHierarchy()
    {
        if (this.config.outputType)
        {
            BlockValueDescriptor.register(this.config.outputType);
        }

        if (this.config.outputElementType)
        {
            BlockValueDescriptor.register(this.config.outputElementType);
        }
    }
}

export abstract class Block<T extends BaseBlockModel | void> extends Blockly.BlockSvg
{
    private static s_activeNamespaces = new Set<string>();

    private static s_namespaceToBlocks: Map<string, BlockDescriptor<any, any, any, any>[]> = new Map();

    private static s_modelPrototypeToDescriptor: Map<Type<any>, BlockDescriptor<any, any, any, any>> = new Map();

    private static s_nameToInitFunction: Map<string, Function> = new Map();

    private static s_definitionConstructorToDescriptor: Map<BlockDefinition<any>, BlockDescriptor<any, any, any, any>> = new Map();

    private static s_scratchPadCategoryField = "SCRATCH_PAD_CATEGORY";
    private static s_scratchPadEntryField    = "SCRATCH_PAD_ENTRY";

    //--//

    static BlockDef<S extends BaseBlockModel, T extends BlockDefinition<S>, S1 extends BlockValue, S2 extends BlockValue>(config: BlockConfig<S, T, S1, S2>): any
    {
        return function (definition: T)
        {
            let desc = new BlockDescriptor(config);

            //
            // Associate the block configuration with the Workspace namespace.
            //
            let configs = Block.s_namespaceToBlocks.get(config.blockContext) || [];
            configs.push(desc);
            Block.s_namespaceToBlocks.set(config.blockContext, configs);

            if (config.model)
            {
                // Map from model's prototype to block name.
                Block.s_modelPrototypeToDescriptor.set(config.model.prototype, desc);
            }

            //
            // Link definition and configuration.
            //
            Block.s_definitionConstructorToDescriptor.set(definition, desc);
            desc.definition = definition;

            // Link the descriptor to the block's prototype.
            let blockDef          = definition.prototype;
            blockDef.m_descriptor = desc;

            return definition;
        };
    }

    static resetDescriptors(): void
    {
        Block.s_activeNamespaces.clear();
        BlockValueDescriptor.reset();
    }

    static getDescriptorIdForType(type: Type<BlockValue>): string
    {
        if (!type)
        {
            return undefined;
        }

        let desc = BlockValueDescriptor.register(type);
        return desc.id;
    }

    static getTypeForDescriptorId<S extends BlockValue>(type: string): Type<S>
    {
        let desc = BlockValueDescriptor.lookup(type);
        return desc ? <Type<S>>desc.type : undefined;
    }

    static computeTypeClosure(...types: Type<BlockValue>[]): string | string[]
    {
        let ids = new Set<string>();

        let inputs = [];

        for (let type of types)
        {
            if (!type)
            {
                continue;
            }

            inputs.push(type.name);

            let desc = BlockValueDescriptor.register(type);
            Block.computeTypeClosureRecursively(ids, desc);
        }

        let results = [];

        for (let id of ids.keys())
        {
            results.push(id);
        }

        switch (results.length)
        {
            case 0:
                return null;

            case 1:
                return results[0];

            default:
                return results;
        }
    }

    private static computeTypeClosureRecursively(ids: Set<string>,
                                                 desc: BlockValueDescriptor)
    {
        ids.add(desc.id);

        for (let subDesc of desc.subTypes)
        {
            Block.computeTypeClosureRecursively(ids, subDesc);
        }
    }

    static selectPrototypes<C extends BlocklyWorkspaceComponent>(context: string): void
    {
        Block.s_activeNamespaces.add(context);
        let descs = Block.s_namespaceToBlocks.get(context) || [];
        for (let desc of descs)
        {
            let definition = desc.definition;

            Block.updatePrototype(desc, definition);

            desc.linkValueHierarchy();
        }
    }

    private static updatePrototype(desc: BlockDescriptor<any, any, any, any>,
                                   definition: any)
    {
        let blockName = desc.config.blockName;

        // Ensure there's an entry for the block in Blockly.
        let BlocklyBlocks: Lookup<any> = (<any>Blockly).Blocks;
        let existingDef                = BlocklyBlocks[blockName] || {};
        BlocklyBlocks[blockName]       = existingDef;

        // Save the original version of the init method, since we are patching it multiple times.
        let originalInit = Block.s_nameToInitFunction.get(blockName);
        if (!originalInit)
        {
            originalInit = existingDef.init || function () {};

            Block.s_nameToInitFunction.set(blockName, originalInit);
        }

        existingDef.init = function (this: Block<any>)
        {
            let blockDef = definition.prototype;

            Object.setPrototypeOf(this, blockDef);

            if (originalInit)
            {
                originalInit.apply(this);
            }

            if (blockDef.initBlock)
            {
                blockDef.initBlock.apply(this);
            }

            if (desc.config.color)
            {
                this.setColour(desc.config.color);
            }
        };
    }

    public addMutator(setter?: (container: Element) => void,
                      reader?: (container: Element) => void)
    {
        this.mutationToDom = () =>
        {
            let container = document.createElement("mutation");
            if (setter) setter(container);
            container.setAttribute(Block.s_scratchPadCategoryField, this.m_scratchPadCategory);
            container.setAttribute(Block.s_scratchPadEntryField, this.m_scratchPadEntry);
            return container;
        };

        this.domToMutation = (container: Element) =>
        {
            if (reader) reader(container);
            this.m_scratchPadCategory = container.getAttribute(Block.s_scratchPadCategoryField);
            this.m_scratchPadEntry    = container.getAttribute(Block.s_scratchPadEntryField);
        };
    }

    public getDescriptor(): BlockDescriptor<any, any, any, any>
    {
        let prototype = Object.getPrototypeOf(this);
        return prototype.m_descriptor;
    }

    public static getDescriptorForDefinition(definition: BlockDefinition<any>): BlockDescriptor<any, any, any, any>
    {
        return Block.s_definitionConstructorToDescriptor.get(definition);
    }

    public static getBlockName(definition: BlockDefinition<any>): string
    {
        let desc = Block.getDescriptorForDefinition(definition);
        if (desc)
        {
            return desc.config.blockName;
        }

        return null;
    }

    public static cast<T extends Block<any>>(block: Blockly.Block): T
    {
        return <T><any>block;
    }

    public static castArray<T extends Block<any>>(blocks: Blockly.Block[]): T[]
    {
        return <T[]><any>blocks;
    }

    public static fromModel<T extends BaseBlockModel>(workspace: BlocklyExtendedWorkspace,
                                                      model: T): Block<T>
    {
        let modelPrototype = Object.getPrototypeOf(model);
        let desc           = Block.s_modelPrototypeToDescriptor.get(modelPrototype);
        if (desc)
        {
            if (!Block.s_activeNamespaces.has(desc.config.blockContext))
            {
                throw Error(`Context "${desc.config.blockContext}" not active.`);
            }

            let block = Block.cast(workspace.newBlock(desc.config.blockName, model.id || undefined));
            block.initFromModel(model);
            block.initSvg();

            block.moveBy(model.x || 0, model.y || 0);

            return block;
        }

        throw Error(`Can't find prototype for model ${modelPrototype}`);
    }

    public static setBlockColor(definition: BlockDefinition<any>,
                                color: number)
    {
        let desc = Block.getDescriptorForDefinition(definition);
        if (desc)
        {
            desc.config.color = color;
        }
    }

    public static chain(blocks: Block<any>[])
    {
        for (let i = 0; i < blocks.length - 1; i++)
        {
            let block     = blocks[i];
            let nextBlock = blocks[i + 1];
            block.setNext(nextBlock);
        }
    }

    constructor()
    {
        super(null, null, null);
        throw Error("Block instantiated directly.");
    }


    private m_descriptor: BlockDescriptor<any, any, any, any>;

    protected abstract initFields(): void;

    protected abstract initFromModel(model: T): void;

    protected abstract initForToolbox(model: T): void;

    public abstract toModel(): T;

    private extractWorkspace(excludeFlyout = true): BlocklyExtendedWorkspace
    {
        let workspace: BlocklyExtendedWorkspace = <any>this.workspace;
        if (workspace)
        {
            if (workspace.targetWorkspace) // If in a flyout, we need to get to the nexted workspace.
            {
                let extendedTarget = <BlocklyExtendedWorkspace>workspace.targetWorkspace;
                if (excludeFlyout)
                {
                    workspace = extendedTarget;
                }
                else
                {
                    workspace.optio3Context   = extendedTarget.optio3Context;
                    workspace.optio3Component = extendedTarget.optio3Component;
                }
            }

            return workspace;
        }

        return null;
    }

    public getComponent<T extends BlocklyWorkspaceComponent>(): T
    {
        return <T>this.extractWorkspace().optio3Component;
    }

    public getContext(): ComponentContext
    {
        return this.extractWorkspace().optio3Context;
    }

    public injectContext<T>(token: Type<T>): T
    {
        return this.getContext()
                   .inject(token);
    }

    protected initBlock()
    {
        this.addMutator();
        this.initFields();
        if (this.isInFlyout)
        {
            this.initForToolbox(this.newModel());
        }
    }

    public toDom(): Element
    {
        let dom = Blockly.Xml.blockToDom(this);
        return dom instanceof Element ? dom : null;
    }

    public toXml(includeData = true): string
    {
        let dom = this.toDom();

        if (!includeData)
        {
            let dataElements = dom.getElementsByTagName("data");
            for (let i = 0; i < dataElements.length; i++)
            {
                dataElements.item(i)
                            .remove();
            }
        }

        return Blockly.Xml.domToText(dom);
    }

    private m_data: string;
    private m_dataListeners: (() => void)[];

    // @ts-ignore
    public set data(data: string)
    {
        this.setTextData(data);

        for (let listener of this.m_dataListeners)
        {
            listener();
        }
    }

    public get data(): string
    {
        return this.m_data;
    }

    public setTextData(data: string): boolean
    {
        if (data !== this.m_data)
        {
            this.m_data = data;
            return true;
        }

        return false;
    }

    public onData(listener: () => void)
    {
        if (!this.m_dataListeners)
        {
            this.m_dataListeners = [];
        }

        this.m_dataListeners.push(listener);
    }

    public remove(): void
    {
        if (this)
        {
            this.dispose(true);
        }
    }

    private m_scratchPadCategory: string;

    public getScratchPadCategory(): string
    {
        return this.m_scratchPadCategory;
    }

    public setScratchPadCategory(id: string): void
    {
        this.m_scratchPadCategory = id;
    }

    private m_scratchPadEntry: string;

    public getScratchPadEntry(): string
    {
        return this.m_scratchPadEntry;
    }

    public setScratchPadEntry(id: string): void
    {
        this.m_scratchPadEntry = id;
    }

    public getCoordinates(): Blockly.utils.Coordinate
    {
        let coords: any = this.getRelativeToSurfaceXY();
        return new Blockly.utils.Coordinate(coords.x, coords.y);
    }

    public getDescendantsOfType<S extends BaseBlockModel, T extends Block<S>, D extends BlockDefinition<S>>(type: D): T[]
    {
        let descendants = this.getDescendants(true);
        return Block.castArray(descendants.filter((d) => d instanceof type));
    }

    protected newModel(): T
    {
        let desc = this.getDescriptor();
        if (desc && desc.config.model)
        {
            return new desc.config.model();
        }

        return null;
    }

    public getFieldInteger(fieldName: string): number
    {
        let field = this.getFieldValue(fieldName);
        if (field)
        {
            return parseInt(field);
        }

        return 0;
    }

    public getFieldFloat(fieldName: string): number
    {
        let field = this.getFieldValue(fieldName);
        if (field)
        {
            return parseFloat(field);
        }

        return 0;
    }

    public getFieldString(fieldName: string): string
    {
        return this.getFieldValue(fieldName);
    }

    public getData<D>(): D
    {
        if (this.data)
        {
            return <D>JSON.parse(this.data);
        }

        return undefined;
    }

    public setData<D>(data: D): boolean
    {
        return this.setTextData(JSON.stringify(data));
    }

    public getConnectedBlock<S extends BaseBlockModel, T extends Block<S>>(inputName: string): T
    {
        let block = this.getInputTargetBlock(inputName);
        return Block.cast(block);
    }

    public getStatements<S extends BaseBlockModel, T extends Block<S>>(inputName: string): T[]
    {
        let result = [];
        let block  = this.getInputTargetBlock(inputName);
        while (block)
        {
            result.push(Block.cast(block));
            block = block.getNextBlock();
        }

        return result;
    }

    public createConnectedBlock<S extends BaseBlockModel, T extends Block<S>>(inputName: string,
                                                                              model: S): T
    {
        if (model)
        {
            let newBlock: Block<S> = Block.fromModel(this.extractWorkspace(false), model);
            this.connect(inputName, newBlock);
            return <T>newBlock;
        }

        return null;
    }

    public createStatements<S extends BaseBlockModel, T extends Block<S>>(inputName: string,
                                                                          model: S[]): T[]
    {
        let result: T[] = [];
        if (model)
        {
            let connection = this.getInput(inputName).connection;

            for (let i = 0; i < model.length; i++)
            {
                let newBlock = Block.fromModel(this.extractWorkspace(false), model[i]);
                connection.connect(newBlock.previousConnection);
                connection = newBlock.nextConnection;
                result.push(<T>newBlock);
            }
        }

        return result;
    }

    public getInputModel<S extends BaseBlockModel, T extends Block<S>>(inputName: string): S
    {
        let block: T = this.getConnectedBlock<S, T>(inputName);
        return block && block.toModel();
    }

    public getStatementsModels<S extends BaseBlockModel, T extends Block<S>>(statementInputName: string): S[]
    {
        let models: S[] = [];
        let blocks      = this.getStatements(statementInputName);
        blocks          = blocks || [];
        for (let block of blocks)
        {
            if (block)
            {
                let model = block.toModel();
                if (model)
                {
                    models.push(<S>model);
                }
            }
        }
        return models;
    }

    public setField(fieldName: string,
                    value: any)
    {
        if (value !== null && value !== undefined)
        {
            this.setFieldValue(`${value}`, fieldName);
        }

        return value;
    }

    public setNext(nextBlock: Block<any>)
    {
        if (this && this.nextConnection && nextBlock && nextBlock.previousConnection)
        {
            this.nextConnection.connect(nextBlock.previousConnection);
        }
    }

    public getDummyEvent()
    {
        let event        = new Blockly.Events.BlockMove(this);
        event.recordUndo = false;
        return event;
    }

    private connect(inputName: string,
                    block: Block<any>)
    {
        let input = this.getInput(inputName);
        if (!input)
        {
            throw Error(`Input: ${inputName} does not exist on block`);
        }

        input.connection.connect(block.outputConnection);
    }
}

export const BlockDef = Block.BlockDef;
