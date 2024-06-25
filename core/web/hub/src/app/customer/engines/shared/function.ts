import {Type} from "@angular/core";
import {UUID} from "angular2-uuid";
import * as Base from "app/customer/engines/shared/base";

import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";
import * as Models from "app/services/proxy/model/models";
import * as Blockly from "blockly";
import {Lookup, UtilsService} from "framework/services/utils.service";

import {Block, BlockDef} from "framework/ui/blockly/block";
import {BlocklyExtendedWorkspace} from "framework/ui/blockly/blockly-workspace.component";

abstract class ProcedureCallBase<T extends Models.EngineStatementProcedureCall | Models.EngineExpressionFunctionCall> extends Base.AppBlock<T>
{
    private static argInput     = (i: number) => "ARG" + i;
    private static argNameInput = (i: number) => "ARGNAME" + i;
    private static topRowInput  = "TOPROW";
    private static nameField    = "funcName";

    private m_arguments: string[];

    private m_unknownFunction: [string, string];

    protected initFields(): void
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let block     = this;

        function getFunctions(): string[][]
        {
            let functions = component.functionHolder.getFunctions() || [];

            const options = functions.map((fn) =>
                                          {
                                              return [
                                                  fn.name,
                                                  fn.functionId
                                              ];
                                          });

            const unknownFunc = block.m_unknownFunction;
            if (unknownFunc && !options.find((o) => o[1] === unknownFunc[1]))
            {
                options.push(unknownFunc);
            }

            if (!options.length)
            {
                options.push([
                                 "name",
                                 ""
                             ]);
            }

            return options;
        }

        let dropdown = new Blockly.FieldDropdown(getFunctions);

        let oldSetValue   = dropdown.setValue.bind(dropdown);
        dropdown.setValue = (newValue) =>
        {
            if (newValue && !this.updateDefinition(newValue))
            {
                this.m_unknownFunction = [
                    "Unknown Function",
                    newValue
                ];
                dropdown.getOptions(false);
            }

            oldSetValue(newValue);
        };

        this.appendDummyInput(ProcedureCallBase.topRowInput)
            .appendField("call function")
            .appendField(dropdown, ProcedureCallBase.nameField);
        this.m_arguments = [];
        this.updateDefinition(this.getCallId());
    }

    private getDefinition(id: string): Models.EngineProcedureDeclaration
    {
        return this.getComponent<AppBlocklyWorkspaceComponent>()
                   .functionHolder
                   .getFunctionById(id);
    }

    private updateDefinition(id: string): boolean
    {
        let definition = this.getDefinition(id);
        if (definition)
        {
            this.updateFromModel(definition);
            return true;
        }

        return false;
    }

    protected initFromModel(model: T): void
    {
        if (!model.functionId) return;

        let args = model.arguments || [];
        args     = args.filter((arg) => !!arg.variable);

        if (!this.updateDefinition(model.functionId))
        {
            // Could not find definition, manually update shape so we can use the model
            this.m_arguments = args.map((arg) => arg.variable.name);
            this.updateShape();
        }

        this.setFunc(model.functionId);

        for (let i = 0; i < args.length; i++)
        {
            let arg = args[i];
            this.setField(ProcedureCallBase.argNameInput(i), arg.variable.name);
            this.createConnectedBlock(ProcedureCallBase.argInput(i), arg.value);
        }
    }

    public setFunc(newId: string)
    {
        let field = <Blockly.FieldDropdown>this.getField(ProcedureCallBase.nameField);
        // Reset option cache
        field.getOptions(false);

        this.setField(ProcedureCallBase.nameField, newId);
    }

    public toModel(): T
    {
        let varHolder    = this.getComponent<AppBlocklyWorkspaceComponent>().variableHolder;
        let model        = super.toModel();
        model.functionId = this.getFieldString(ProcedureCallBase.nameField);
        model.arguments  = [];
        for (let i = 0; i < this.m_arguments.length; i++)
        {
            let varDesc = varHolder.getVariableByName(this.m_arguments[i], true);

            let varAssignment = Models.EngineVariableAssignment.newInstance({
                                                                                id      : "NotABlock",
                                                                                variable: varDesc.toModel(),
                                                                                value   : this.getInputModel(ProcedureCallBase.argInput(i))
                                                                            });
            model.arguments.push(varAssignment);
        }
        return model;
    }

    public getCallId()
    {
        return this.getFieldString(ProcedureCallBase.nameField);
    }

    public updateName()
    {
        let field = <Blockly.FieldDropdown>this.getField(ProcedureCallBase.nameField);

        // Reset option cache
        const options = field.getOptions(false);
        const option  = options.find((o) => o[1] === field.getValue());

        // TODO: Remove this on next release of Blockly
        (<any>field).selectedOption_ = option;

        field.forceRerender();
    }

    public updateFromBlock(defBlock: ProcedureBlock)
    {
        let argumentNames = defBlock.getArgumentNames();
        let argumentIds   = defBlock.getArgumentIds();
        let mutatorOpen   = defBlock && defBlock.mutator && defBlock.mutator.isVisible();
        this.updateFromDefinition(argumentNames, mutatorOpen);
        this.updateName();
    }

    public updateFromModel(defBlock: Models.EngineProcedureDeclaration)
    {
        let args          = defBlock.arguments || [];
        let argumentNames = args.map((arg) => arg.name);
        let argumentIds   = args.map(() => "");

        this.updateFromDefinition(argumentNames, false);
    }

    private updateFromDefinition(argumentNames: string[],
                                 mutatorOpen: boolean)
    {
        // Data structures:
        // this.arguments = ['x', 'y']
        //     Existing param names.
        // this.m_quarkConnections {piua: null, f8b_: Blockly.Connection}
        //     Look-up of argumentIds to connections plugged into the call block.
        // this.m_quarkIds = ['piua', 'f8b_']
        //     Existing param IDs.
        // Note that m_quarkConnections may include IDs that no longer exist, but
        // which might reappear if a param is reattached in the mutator.


        if (this.m_arguments.length === argumentNames.length && this.m_arguments.every((val,
                                                                                        i) => argumentNames[i] === val))
        {
            // No change.
            return;
        }

        this.setCollapsed(false);
        // Switch off rendering while the block is rebuilt.
        let savedRendered = this.rendered;
        this.rendered     = false;
        // Update the m_quarkConnections with existing connections.

        let connections: any = {};
        for (let i = 0; i < this.m_arguments.length; i++)
        {
            let input = this.getInput(ProcedureCallBase.argInput(i));
            if (input)
            {
                let connection = input.connection.targetConnection;
                let argument   = this.m_arguments[i];

                connections[argument] = connection;
                if (mutatorOpen && connection && argumentNames.indexOf(argument) == -1)
                {
                    // This connection should no longer be attached to this block.
                    connection.disconnect();
                    connection.getSourceBlock()
                              .bumpNeighbours();
                }
            }
        }
        // Rebuild the block's arguments.
        this.m_arguments = [].concat(argumentNames);

        this.updateShape();
        // Reconnect any child blocks.
        for (let i = 0; i < this.m_arguments.length; i++)
        {
            let name = this.m_arguments[i];
            if (name in connections)
            {
                let connection = connections[name];
                if (!Blockly.Mutator.reconnect(connection, this, ProcedureCallBase.argInput(i)))
                {
                    // Block no longer exists or has been attached elsewhere.
                    delete connections[name];
                }
            }
        }
        // Restore rendering and show the changes.
        this.rendered = savedRendered;
        if (this.rendered)
        {
            (this as any).render();
        }

    }

    /**
     * Modify this block to have the correct number of arguments.
     * @private
     * @this Blockly.Block
     */
    private updateShape()
    {
        let i = 0;
        for (i = 0; i < this.m_arguments.length; i++)
        {
            let field = this.getField(ProcedureCallBase.argNameInput(i));
            if (field)
            {
                // Ensure argument name is up to date.
                // The argument name field is deterministic based on the mutation,
                // no need to fire a change event.
                Blockly.Events.disable();
                try
                {
                    field.setValue(this.m_arguments[i]);
                }
                finally
                {
                    Blockly.Events.enable();
                }
            }
            else
            {
                // Add new input.
                field     = new Blockly.FieldLabel(this.m_arguments[i]);
                let input = this.appendValueInput(ProcedureCallBase.argInput(i))
                                .setAlign(Blockly.ALIGN_RIGHT)
                                .appendField(field, ProcedureCallBase.argNameInput(i));
                input.init();
            }
        }
        // Remove deleted inputs.
        while (this.getInput(ProcedureCallBase.argInput(i)))
        {
            this.removeInput(ProcedureCallBase.argInput(i));
            i++;
        }

        // Add 'with:' if there are parameters, remove otherwise.
        let topRow = this.getInput(ProcedureCallBase.topRowInput);
        if (topRow)
        {
            if (this.m_arguments.length)
            {
                if (!this.getField("WITH"))
                {
                    topRow.appendField("with", "WITH");
                    topRow.init();
                }
            }
            else
            {
                if (this.getField("WITH"))
                {
                    topRow.removeField("WITH");
                }
            }
        }
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "procedure_call",
              model       : Models.EngineStatementProcedureCall
          })
export class StatementProcedureCallBlock extends ProcedureCallBase<Models.EngineStatementProcedureCall>
{
    protected initFields(): void
    {
        super.initFields();

        this.setPreviousStatement(true);
        this.setNextStatement(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "function_call",
              model       : Models.EngineExpressionFunctionCall
          })
export class ExpressionFunctionCallBlock extends ProcedureCallBase<Models.EngineExpressionFunctionCall>
{
    protected initFields(): void
    {
        super.initFields();

        this.setOutput(true);
    }

    public inferOutputType(): Type<Models.EngineValue>
    {
        let desc = this.getDescriptor();
        return desc.config.outputType;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "procedure_def",
              model       : Models.EngineProcedureDeclaration
          })
export class ProcedureBlock extends Base.ComputationBlock<Models.EngineProcedureDeclaration>
{
    private static idInput     = "id";
    private static nameInput   = "name";
    private static paramsInput = "PARAMS";

    private m_arguments: string[];
    private m_paramIds: string[];

    private m_forceId: boolean;

    protected initFields(): void
    {
        this.decompose = (workspace: Blockly.WorkspaceSvg) =>
        {
            let containerBlock = <Blockly.BlockSvg>workspace.newBlock("procedures_mutatorcontainer");
            containerBlock.initSvg();

            containerBlock.getInput("STATEMENT_INPUT")
                          .setVisible(false);

            // Parameter list.
            let connection = containerBlock.getInput("STACK").connection;
            for (let i = 0; i < this.m_arguments.length; i++)
            {
                let paramBlock = <Blockly.BlockSvg>workspace.newBlock(Block.getBlockName(ProcedureArgumentMutatorBlock));
                paramBlock.initSvg();
                paramBlock.setFieldValue(this.m_arguments[i], "NAME");
                // Store the old location.
                (<any>paramBlock).oldLocation = i;
                connection.connect(paramBlock.previousConnection);
                connection = paramBlock.nextConnection;
            }
            // Initialize procedure's callers with blank IDs.
            this.mutateCallers();
            return containerBlock;
        };

        this.compose = (containerBlock: Blockly.BlockSvg) =>
        {
            // Parameter list.
            this.m_arguments = [];
            this.m_paramIds  = [];
            let paramBlock   = containerBlock.getInputTargetBlock("STACK");
            while (paramBlock)
            {
                let varName = paramBlock.getFieldValue("NAME");
                this.m_arguments.push(varName);
                this.m_paramIds.push(paramBlock.id);
                paramBlock = paramBlock.nextConnection && paramBlock.nextConnection.targetBlock();
            }

            this.updateParams();
            this.mutateCallers();
        };

        this.addMutator((container) =>
                        {
                            container.setAttribute("name", this.getFieldValue("NAME"));
                            for (let i = 0; i < this.m_arguments.length; i++)
                            {
                                let parameter = document.createElement("arg");
                                let name      = this.m_arguments[i];
                                parameter.setAttribute("name", name);
                                if (this.m_paramIds)
                                {
                                    parameter.setAttribute("paramId", this.m_paramIds[i]);
                                }
                                container.appendChild(parameter);
                            }
                        }, (container) =>
                        {
                            this.m_arguments = [];
                            this.m_paramIds  = [];
                            for (let i = 0, childNode: Element; childNode = <Element>container.childNodes[i]; i++)
                            {
                                if (childNode.nodeName.toLowerCase() == "arg")
                                {
                                    let varName = childNode.getAttribute("name");
                                    this.m_arguments.push(varName);

                                    let paramId = childNode.getAttribute("paramId");
                                    this.m_paramIds.push(paramId);
                                }
                            }
                            this.updateParams();
                            if (!this.isInFlyout)
                            {
                                this.mutateCallers();
                            }
                        });

        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let inFlyout  = this.isInFlyout;
        let block     = this;

        function updateNames(this: Blockly.FieldTextInput,
                             newName: string)
        {
            let id  = block.getDefinitionId();
            newName = component.functionHolder.getLegalName(newName, id);

            if (!inFlyout)
            {
                component.functionHolder.rename(id, newName);
            }

            return newName;
        }

        function updateId(this: Blockly.FieldTextInput,
                          newId: string)
        {
            const id = block.getDefinitionId();
            if (id)
            {
                return id;
            }

            const idIsUnused = newId && !component.functionHolder.getFunctionById(newId);

            if (block.m_forceId || idIsUnused || inFlyout)
            {
                block.m_forceId = false;
                return newId;
            }

            return UUID.UUID();
        }

        this.appendDummyInput()
            .appendField(new Blockly.FieldTextInput("", updateId), ProcedureBlock.idInput)
            .setVisible(false);

        this.appendDummyInput()
            .appendField("function")
            .appendField(new Blockly.FieldTextInput(component.functionHolder.getLegalName("name", block.getDefinitionId()), updateNames), ProcedureBlock.nameInput)
            .appendField("", ProcedureBlock.paramsInput);

        this.appendStatementInput(Base.ComputationBlock.StatementsInputName)
            .appendField("do");


        this.setMutator(new Blockly.Mutator([Block.getBlockName(ProcedureArgumentMutatorBlock)]));

        this.m_arguments = [];
    }

    protected initFromModel(model: Models.EngineProcedureDeclaration): void
    {
        this.m_forceId = true;
        this.setField(ProcedureBlock.idInput, model.functionId);
        this.setField(ProcedureBlock.nameInput, model.name);
        let args         = model.arguments || [];
        this.m_arguments = args.map((arg) => arg.name);
        this.m_paramIds  = args.map(() => "");
        this.updateParams();
        this.mutateCallers();

        super.initFromModel(model);
    }

    public toModel(): Models.EngineProcedureDeclaration
    {
        let model        = super.toModel();
        model.functionId = this.getDefinitionId();
        model.name       = this.getFieldString(ProcedureBlock.nameInput);
        model.arguments  = this.m_arguments.map((arg) => Models.EngineVariableReference.newInstance({name: arg}));
        return model;
    }

    public getArgumentNames()
    {
        return this.m_arguments;
    }

    public getArgumentIds()
    {
        return this.m_paramIds;
    }

    public getVariableModels(): Models.EngineVariable[]
    {
        if (!this.m_arguments)
        {
            return [];
        }

        let result: Models.EngineVariable[] = [];

        let varHolder = this.getComponent<AppBlocklyWorkspaceComponent>().variableHolder;

        for (let arg of this.m_arguments)
        {
            let varModel = varHolder.getVariableByName(arg);
            if (varModel)
            {
                result.push(varModel.toModel());
            }
        }

        return result;
    }

    public getDefinitionId()
    {
        return this.getFieldString(ProcedureBlock.idInput);
    }

    private updateParams()
    {
        // Check for duplicated arguments.
        let badArg                = false;
        let hash: Lookup<boolean> = {};
        for (let i = 0; i < this.m_arguments.length; i++)
        {
            if (hash[this.m_arguments[i].toLowerCase()])
            {
                badArg = true;
                break;
            }
            hash[this.m_arguments[i].toLowerCase()] = true;
        }
        if (badArg)
        {
            this.setWarningText("Duplicated argument");
        }
        else
        {
            this.setWarningText(null);
        }
        // Merge the arguments into a human-readable list.
        let paramString = "";
        if (this.m_arguments.length)
        {
            paramString = "with " + this.m_arguments.join(", ");
        }
        // The params field is deterministic based on the mutation,
        // no need to fire a change event.
        Blockly.Events.disable();
        try
        {
            this.setField(ProcedureBlock.paramsInput, paramString);
        }
        finally
        {
            Blockly.Events.enable();
        }
    }

    public mutateCallers()
    {
        let allCallers = this.getComponent()
                             .getBlocksOfType<ProcedureCallBase<any>>(ProcedureCallBase, false);
        let callers    = allCallers.filter((bl: ProcedureCallBase<any>) =>
                                           {
                                               return bl.getCallId() === this.getDefinitionId();
                                           });

        for (let caller of callers)
        {
            (caller as ProcedureCallBase<any>).updateFromBlock(this);
        }
    }
}


@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_procedure_arg_mutator",
              model       : null
          })
export class ProcedureArgumentMutatorBlock extends Block<void>
{
    protected initFields(): void
    {
        let workspace                  = <BlocklyExtendedWorkspace>Blockly.Mutator.findParentWs(this.workspace);
        let varHolder                  = (workspace.optio3Component as AppBlocklyWorkspaceComponent).variableHolder;
        let createdVariables: string[] = [];
        let validator                  = function (varName: string)
        {
            varName = varName.replace(/[\s]+/g, " ")
                             .trim();
            if (!varName)
            {
                return null;
            }

            if (!varHolder.getVariableByName(varName))
            {
                varHolder.getVariableByName(varName, true);
                createdVariables.push(varName);
            }

            return varName;
        };

        let defaultVariableName = "x";

        let createNewVariable = function (newText: string)
        {
            while (createdVariables.length)
            {
                let varName = createdVariables.shift();
                if (newText !== varName)
                {
                    varHolder.removeVariable(varName);
                }
            }

            varHolder.getVariableByName(newText, true);
        };

        let field: any         = new Blockly.FieldTextInput(defaultVariableName, validator);
        field.onFinishEditing_ = createNewVariable;

        this.appendDummyInput()
            .appendField("input name:")
            .appendField(field, "NAME");
        this.setColour(123);
        this.setPreviousStatement(true);
        this.setNextStatement(true);
    }

    protected initForToolbox(): void
    {
    }

    protected initFromModel(): void
    {
    }

    public toModel(): void
    {
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "function_return",
              model       : Models.EngineStatementFunctionReturn
          })
export class FunctionReturnBlock extends Base.AppBlock<Models.EngineStatementFunctionReturn>
{
    protected initFields(): void
    {
        this.appendDummyInput()
            .appendField("return");
        this.setPreviousStatement(true);
        this.setNextStatement(false);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "function_return_value",
              model       : Models.EngineStatementFunctionReturnValue
          })
export class FunctionReturnValueBlock extends Base.AppBlock<Models.EngineStatementFunctionReturnValue>
{
    protected initFields(): void
    {
        let block_value = this.registerConnectedBlock<Models.EngineExpression>("return",
                                                                               false,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value);
        this.appendConnectedBlock(block_value)
            .appendField("return");

        this.setPreviousStatement(true);
        this.setNextStatement(false);
    }
}

export class FunctionHolder
{
    private m_functions = new Map<string, Models.EngineProcedureDeclaration>();

    constructor(private m_workspace: AppBlocklyWorkspaceComponent)
    {
    }

    public getFunctions(): Models.EngineProcedureDeclaration[]
    {
        const functions = [...this.m_functions.values()];

        functions.sort((a,
                        b) => UtilsService.compareStrings(a.name, b.name, true));

        return functions;
    }

    public getFunctionById(id: string)
    {
        return this.m_functions.get(id);
    }

    public addFunction(func: Models.EngineProcedureDeclaration)
    {
        this.m_functions.set(func.functionId, func);
    }

    public getLegalName(newName: string,
                        ignoreId: string)
    {
        newName = newName.trim();
        while (!this.isLegalName(newName, ignoreId))
        {
            newName += "2";
        }

        return newName;
    }

    private isLegalName(newName: string,
                        ignoreId: string)
    {
        let allFuncs = [...this.m_functions.values()];
        let func     = allFuncs.find((f) => f.name === newName);
        return !func || func.functionId === ignoreId;
    }

    public rename(id: string,
                  newName: string): void
    {
        let func = this.getFunctionById(id);
        if (func)
        {
            func.name = newName;

            // Update foreground blocks
            let functionCalls: ProcedureCallBase<any>[] = this.m_workspace.getBlocksOfType(<any>ProcedureCallBase, false);
            functionCalls.forEach((fc) =>
                                  {
                                      fc.updateName();
                                  });
        }
    }

    public update()
    {
        // First, initialize the variables.
        let newFunctions = new Map<string, Models.EngineProcedureDeclaration>();

        let newFuncModels = this.m_workspace.data.filterBlocks<Models.EngineProcedureDeclaration>((bl) => bl instanceof Models.EngineProcedureDeclaration);
        for (let func of newFuncModels)
        {
            newFunctions.set(func.functionId, func);
        }

        this.m_functions = newFunctions;

        // for visible functions, update visible callers
        let visibleFunctions = this.m_workspace.getBlocksOfType<ProcedureBlock>(ProcedureBlock);
        for (let funcBlock of visibleFunctions)
        {
            this.mutateCallers(funcBlock);
        }
    }

    private mutateCallers(funcBlock: ProcedureBlock)
    {
        // Mutate any visible callers
        funcBlock.mutateCallers();

        // Update variables for anything else
        let callers = this.getCallers(funcBlock.getDefinitionId());
        for (let caller of callers)
        {
            let oldArguments                     = caller.arguments || [];
            let newArguments                     = funcBlock.getVariableModels();
            let newVars: Models.EngineVariable[] = [];
            caller.arguments                     = newArguments.map((variable) =>
                                                                    {
                                                                        let existingValue = oldArguments.find((assignment) => assignment.variable?.name === variable.name);
                                                                        if (existingValue)
                                                                        {
                                                                            oldArguments = oldArguments.filter((arg) => arg != existingValue);
                                                                            return existingValue;
                                                                        }

                                                                        newVars.push(variable);
                                                                        return Models.EngineVariableAssignment.newInstance({variable});
                                                                    });

            // Handle renamed variables
            for (let arg of caller.arguments)
            {
                if (newVars.find((variable) => variable.name === arg.variable.name) && oldArguments.length)
                {
                    let oldArg = oldArguments.shift();
                    arg.value  = oldArg.value;
                }
            }
        }
    }

    private getCallers(id?: string): (Models.EngineExpressionFunctionCall | Models.EngineStatementProcedureCall)[]
    {
        return this.m_workspace.data.filterBlocks<Models.EngineExpressionFunctionCall | Models.EngineStatementProcedureCall>((block) => block instanceof Models.EngineStatementProcedureCall
                                                                                                                                        || block instanceof Models.EngineExpressionFunctionCall)
                   .filter((bl) => !id || bl.functionId === id);
    }
}
