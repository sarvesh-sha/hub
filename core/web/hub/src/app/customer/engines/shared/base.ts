import {ComponentType} from "@angular/cdk/portal";
import {Type} from "@angular/core";

import * as Models from "app/services/proxy/model/models";

import * as Blockly from "blockly";
import {UtilsService} from "framework/services/utils.service";

import {BaseBlockModel, Block} from "framework/ui/blockly/block";
import {ModalDialogField} from "framework/ui/blockly/modal-dialog-field";

//--//

export class ConnectedBlockHandler<S extends BaseBlockModel, TB>
{
    blockName: string;
    shadow: boolean;
    typeClosure: string | string[];

    setter: (model: TB,
             value: S) => void;

    getter: (model: TB) => S;
}

class FieldHandler<TB>
{
    fieldName: string;

    setter: (model: TB,
             value: string) => void;

    getter: (model: TB) => any;
}

class FieldModalHandler<S, T extends ComponentType<S>, M, TB>
{
    modalField: ModalDialogField<S, T, M>;

    setter: (model: TB,
             value: M) => void;

    getter: (model: TB) => M;
}

export abstract class AppBlock<T extends Models.EngineBlock> extends Block<T>
{
    private m_fieldHandlers: FieldHandler<T>[];
    private m_fieldModalHandlers: FieldModalHandler<any, any, any, T>[];
    private m_connectedBlockHandlers: ConnectedBlockHandler<any, T>[];

    protected registerStringField(fieldName: string,
                                  setter: (model: T,
                                           value: string) => void,
                                  getter: (model: T) => any): string
    {
        let handler       = new FieldHandler<T>();
        handler.fieldName = fieldName;
        handler.setter    = setter;
        handler.getter    = getter;

        if (!this.m_fieldHandlers) this.m_fieldHandlers = [];

        this.m_fieldHandlers.push(handler);

        return fieldName;
    }

    protected registerIntegerField(fieldName: string,
                                   setter: (model: T,
                                            value: number) => void,
                                   getter: (model: T) => number): string
    {
        let setterSub = (model: T,
                         value: string) =>
        {
            setter(model, value ? parseInt(value) : undefined);
        };

        return this.registerStringField(fieldName, setterSub, getter);
    }

    protected registerFloatField(fieldName: string,
                                 setter: (model: T,
                                          value: number) => void,
                                 getter: (model: T) => number): string
    {
        let setterSub = (model: T,
                         value: string) =>
        {
            setter(model, value ? parseFloat(value) : undefined);
        };

        return this.registerStringField(fieldName, setterSub, getter);
    }

    protected registerBooleanField(fieldName: string,
                                 setter: (model: T,
                                          value: boolean) => void,
                                 getter: (model: T) => boolean): string
    {
        let setterSub = (model: T,
                         value: string) =>
        {
            setter(model, value === "TRUE");
        };

        return this.registerStringField(fieldName, setterSub, getter);
    }

    protected registerModalField<S1, T1 extends ComponentType<S1>, M1>(modalField: ModalDialogField<S1, T1, M1>,
                                                                       setter: (model: T,
                                                                                value: M1) => void,
                                                                       getter: (model: T) => M1)
    {
        let handler        = new FieldModalHandler<S1, T1, M1, T>();
        handler.modalField = modalField;
        handler.setter     = setter;
        handler.getter     = getter;

        if (!this.m_fieldModalHandlers) this.m_fieldModalHandlers = [];

        this.m_fieldModalHandlers.push(handler);
    }

    //--//

    protected registerConnectedBlock<S1 extends BaseBlockModel>(blockName: string,
                                                                shadow: boolean,
                                                                setter: (model: T,
                                                                         value: S1) => void,
                                                                getter: (model: T) => S1,
                                                                ...types: Type<Models.EngineValue>[]): ConnectedBlockHandler<S1, T>
    {
        let handler         = new ConnectedBlockHandler<S1, T>();
        handler.blockName   = blockName;
        handler.shadow      = shadow;
        handler.typeClosure = types.length > 0 ? Block.computeTypeClosure(...types) : null;
        handler.setter      = setter;
        handler.getter      = getter;

        if (!this.m_connectedBlockHandlers) this.m_connectedBlockHandlers = [];

        this.m_connectedBlockHandlers.push(handler);

        return handler;
    }

    //--//

    protected initForToolbox(model: T): void
    {
        this.initFromModel(model);

        for (let h of this.m_connectedBlockHandlers || [])
        {
            if (h.shadow)
            {
                this.getInputTargetBlock(h.blockName)
                    .setShadow(true);
            }
        }
    }

    protected initFromModel(model: T): void
    {
        for (let h of this.m_fieldHandlers || [])
        {
            this.setField(h.fieldName, h.getter(model));
        }

        for (let h of this.m_fieldModalHandlers || [])
        {
            h.modalField.init(h.getter(model));
        }

        for (let h of this.m_connectedBlockHandlers || [])
        {
            this.createConnectedBlock(h.blockName, h.getter(model));
        }
    }

    public toModel(): T
    {
        let model: T = this.newModel();
        model.id     = this.id;

        if (!this.getParent())
        {
            let coord = this.getCoordinates();
            model.x   = coord.x;
            model.y   = coord.y;
        }

        for (let h of this.m_fieldHandlers || [])
        {
            h.setter(model, this.getFieldValue(h.fieldName));
        }

        for (let h of this.m_fieldModalHandlers || [])
        {
            h.setter(model, this.getData());
        }

        for (let h of this.m_connectedBlockHandlers || [])
        {
            h.setter(model, this.getInputModel(h.blockName));
        }

        return model;
    }

    public appendConnectedBlock(block: ConnectedBlockHandler<any, any>): Blockly.Input
    {
        let res = this.appendValueInput(block.blockName);

        if (block.typeClosure)
        {
            res = res.setCheck(block.typeClosure);
        }

        return res;
    }

    public appendTypedValueInput(name: string,
                                 type: Type<Models.EngineValue>,
                                 ...types: Type<Models.EngineValue>[]): Blockly.Input
    {
        return this.appendValueInput(name)
                   .setCheck(Block.computeTypeClosure(type, ...types));
    }

    protected getDropdownFromEnum(e: any,
                                  sort: boolean,
                                  ...extraItems: string[][]): string[][]
    {
        let result = [...extraItems];
        let names  = UtilsService.getEnumNames(e);

        if (sort)
        {
            names.sort((a,
                        b) => UtilsService.compareStrings(a, b, true));
        }

        for (let name of names)
        {
            result.push([
                            name,
                            e[name]
                        ]);
        }

        return result;
    }
}

export abstract class ComputationBlock<T extends Models.EngineComputation> extends AppBlock<T>
{
    public static StatementsInputName = "statements";

    protected initFromModel(model: T)
    {
        super.initFromModel(model);

        this.createStatements(ComputationBlock.StatementsInputName, model.statements);
    }

    public toModel(): T
    {
        let model        = super.toModel();
        model.statements = this.getStatementsModels(ComputationBlock.StatementsInputName) || [];
        return model;
    }
}

export abstract class ExpressionBlock<T extends Models.EngineExpression> extends AppBlock<T>
{
    protected initFields(): void
    {
        let type = this.inferOutputType();
        this.setOutput(true, Block.computeTypeClosure(type));
    }

    protected initFromModel(model: T): void
    {
        super.initFromModel(model);

        let type = this.inferOutputType();
        this.setOutput(true, Block.computeTypeClosure(type));
    }

    public inferOutputType(): Type<Models.EngineValue>
    {
        let desc = this.getDescriptor();
        return desc.config.outputType;
    }
}

export abstract class BinaryOperatorBlock<T extends Models.EngineOperatorBinary> extends ExpressionBlock<T>
{
    protected block_a: ConnectedBlockHandler<Models.EngineExpression, T>;
    protected block_b: ConnectedBlockHandler<Models.EngineExpression, T>;

    protected initOperatorFields(type_a: Type<Models.EngineValue>,
                                 type_b: Type<Models.EngineValue>): void
    {
        this.block_a = this.registerConnectedBlock<Models.EngineExpression>("A",
                                                                            false,
                                                                            (model,
                                                                             value) => model.a = value,
                                                                            (model) => model.a,
                                                                            type_a);

        this.block_b = this.registerConnectedBlock<Models.EngineExpression>("B",
                                                                            false,
                                                                            (model,
                                                                             value) => model.b = value,
                                                                            (model) => model.b,
                                                                            type_b);

        super.initFields();
    }

    public getTypedInputA<S extends ExpressionBlock<T>>(): S
    {
        return Block.cast(this.getInputTargetBlock(this.block_a.blockName));
    }

    public getTypedInputB<S extends ExpressionBlock<T>>(): S
    {
        return Block.cast(this.getInputTargetBlock(this.block_b.blockName));
    }
}

export abstract class UnaryOperatorBlock<T extends Models.EngineOperatorUnary> extends ExpressionBlock<T>
{
    protected block_a: ConnectedBlockHandler<Models.EngineExpression, T>;

    protected initOperatorFields(...type_a: Type<Models.EngineValue>[]): void
    {
        this.block_a = this.registerConnectedBlock<Models.EngineExpression>("A",
                                                                            false,
                                                                            (model,
                                                                             value) => model.a = value,
                                                                            (model) => model.a,
                                                                            ...type_a);

        super.initFields();
    }
}

export abstract class StatementBlock<T extends Models.EngineStatement> extends AppBlock<T>
{
    protected initBlock()
    {
        super.initBlock();
        this.setPreviousStatement(true);
        this.setNextStatement(true);
    }
}

//--//

export interface ListHelperHost
{
    regenerateShape(): void;

    handleNewInput(index: number,
                   input: Blockly.Input): Type<Models.EngineValue>;
}

export class ListHelper<T extends Block<M> & ListHelperHost, M extends BaseBlockModel | void>
{
    private m_itemCount: number;

    constructor(private m_host: T)
    {
    }

    init()
    {
        this.m_host.addMutator((container) =>
                               {
                                   container.setAttribute("items", this.m_itemCount.toString());
                               }, (container) =>
                               {
                                   this.m_itemCount = parseInt(container.getAttribute("items"), 10);
                                   this.m_host.regenerateShape();
                               });

        this.m_host.decompose = (workspace: Blockly.WorkspaceSvg): Blockly.BlockSvg =>
        {
            let containerBlock = <Blockly.BlockSvg>workspace.newBlock("lists_create_with_container");
            containerBlock.initSvg();
            let connection = containerBlock.getInput("STACK").connection;
            for (let i = 0; i < this.m_itemCount; i++)
            {
                let itemBlock = <Blockly.BlockSvg>workspace.newBlock("lists_create_with_item");
                itemBlock.initSvg();
                connection.connect(itemBlock.previousConnection);
                connection = itemBlock.nextConnection;
            }
            return containerBlock;
        };

        this.m_host.compose = (containerBlock: Blockly.BlockSvg) =>
        {
            let itemBlock   = containerBlock.getInputTargetBlock("STACK");
            // Count number of inputs.
            let connections = [];
            while (itemBlock)
            {
                connections.push((<any>itemBlock).valueConnection_);
                itemBlock = itemBlock.nextConnection && itemBlock.nextConnection.targetBlock();
            }
            // Disconnect any children that don't belong.
            for (let i = 0; i < this.m_itemCount; i++)
            {
                let connection = this.m_host.getInput("ADD" + i).connection.targetConnection;
                if (connection && connections.indexOf(connection) == -1)
                {
                    connection.disconnect();
                }
            }
            this.m_itemCount = connections.length;
            this.m_host.regenerateShape();
            // Reconnect any child blocks.
            for (var i = 0; i < this.m_itemCount; i++)
            {
                Blockly.Mutator.reconnect(connections[i], this.m_host, "ADD" + i);
            }
        };

        (<any>this.m_host).saveConnections = (containerBlock: Blockly.BlockSvg) =>
        {
            let itemBlock = containerBlock.getInputTargetBlock("STACK");
            let i         = 0;
            while (itemBlock)
            {
                let input                         = this.m_host.getInput("ADD" + i);
                (<any>itemBlock).valueConnection_ = input && input.connection.targetConnection;
                i++;
                itemBlock = itemBlock.nextConnection && itemBlock.nextConnection.targetBlock();
            }
        };

        this.m_itemCount = 0;
        this.m_host.regenerateShape();
        this.m_host.setMutator(new Blockly.Mutator(["lists_create_with_item"]));
    }

    setInputs(inputs: Models.EngineExpression[])
    {
        inputs = inputs || [];

        this.m_itemCount = inputs.length;
        this.m_host.regenerateShape();

        for (let i = 0; i < inputs.length; i++)
        {
            this.m_host.createConnectedBlock("ADD" + i, inputs[i]);
        }
    }

    updateShapeForInputs()
    {
        // Add new inputs.
        let i = 0;

        for (; i < this.m_itemCount; i++)
        {
            if (!this.m_host.getInput("ADD" + i))
            {
                let input = this.m_host.appendValueInput("ADD" + i);
                let type  = this.m_host.handleNewInput(i, input);
                if (type)
                {
                    input.setCheck(Block.computeTypeClosure(type));
                }
            }
        }

        // Remove deleted inputs.
        while (this.m_host.getInput("ADD" + i))
        {
            this.m_host.removeInput("ADD" + i);
            i++;
        }
    }

    get itemCount(): number
    {
        return this.m_itemCount;
    }

    collectInputs(): Models.EngineExpression[]
    {
        let lst = [];
        for (let i = 0; i < this.m_itemCount; i++)
        {
            lst.push(this.m_host.getInputModel("ADD" + i));
        }

        return lst;
    }
}
