import {Type} from "@angular/core";
import * as Base from "app/customer/engines/shared/base";
import * as Lists from "app/customer/engines/shared/lists";
import {StatementForEachBlock} from "app/customer/engines/shared/lists";

import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";

import * as Models from "app/services/proxy/model/models";

import * as Blockly from "blockly";
import {Lookup, UtilsService} from "framework/services/utils.service";

import {Block, BlockDef} from "framework/ui/blockly/block";
import {DynamicToolboxCategory} from "framework/ui/blockly/toolbox-category";
import {VariableRenamePromptComponent} from "framework/ui/blockly/variable-rename-prompt.component";
import {DialogPromptComponent} from "framework/ui/dialogs/dialog-prompt.component";

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_variables_set_dynamic",
              model       : Models.EngineVariableAssignment
          })
export class StatementVariableSetBlock extends Base.StatementBlock<Models.EngineVariableAssignment>
{
    private static readonly c_field_VAR_NAME  = "VARNAME";
    private static readonly c_field_VAR_VALUE = "VAL";

    protected initFields(): void
    {
        this.appendValueInput(StatementVariableSetBlock.c_field_VAR_VALUE)
            .appendField("set")
            .appendField(new Blockly.FieldDropdown(() => this.getVariables()), StatementVariableSetBlock.c_field_VAR_NAME)
            .appendField("to");
    }

    public getVar(): VariableDescriptor
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        return component.variableHolder.getVariableByName(this.getFieldString(StatementVariableSetBlock.c_field_VAR_NAME));
    }

    public setVar(newVar: string)
    {
        let field = <Blockly.FieldDropdown>this.getField(StatementVariableSetBlock.c_field_VAR_NAME);
        // Reset option cache
        field.getOptions(false);

        this.setField(StatementVariableSetBlock.c_field_VAR_NAME, newVar);
    }

    public getAssignment<S extends Base.ExpressionBlock<any>>(): S
    {
        return Block.cast(this.getInputTargetBlock(StatementVariableSetBlock.c_field_VAR_VALUE));
    }

    public setAssignmentCheck(check: string | string[])
    {
        let valInput = this.getInput(StatementVariableSetBlock.c_field_VAR_VALUE);
        valInput.setCheck(check);
    }

    public setAssignmentValue(block: Block<any>)
    {
        let valInput = this.getInput(StatementVariableSetBlock.c_field_VAR_VALUE);
        valInput.connection.connect(block.outputConnection);
    }

    private getVariables()
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let vars      = component.variableHolder.getAllVariables();

        if (!vars.length)
        {
            return [
                [
                    "loading",
                    "loading"
                ]
            ];
        }

        return vars.map((variable) => [
            variable.name,
            variable.name
        ]);
    }

    toModel(): Models.EngineVariableAssignment
    {
        let variableSet   = super.toModel();
        variableSet.value = this.getInputModel(StatementVariableSetBlock.c_field_VAR_VALUE);

        let varDesc = this.getVar();
        if (varDesc)
        {
            variableSet.variable = varDesc.toModel();
        }

        return variableSet;
    }


    protected initFromModel(model: Models.EngineVariableAssignment): void
    {
        this.createConnectedBlock(StatementVariableSetBlock.c_field_VAR_VALUE, model.value);

        if (model.variable)
        {
            let component = this.getComponent<AppBlocklyWorkspaceComponent>();
            component.variableHolder.getVariableByName(model.variable.name, true);
            this.setVar(model.variable.name);
        }
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_variables_get_dynamic",
              model       : Models.EngineExpressionGetVariable
          })
export class ExpressionGetVariableBlock extends Base.ExpressionBlock<Models.EngineExpressionGetVariable>
{
    private static readonly c_field_VAR_NAME = "VARNAME";

    protected initFields(): void
    {
        super.initFields();

        let setOutputType = (newValue: string) =>
        {
            let varDesc = this.getVarByName(newValue);
            if (varDesc && varDesc.type)
            {
                this.setOutput(true, Block.computeTypeClosure(varDesc.type));
            }
            else
            {
                this.setOutput(true);
            }

            return newValue;
        };

        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown(() => this.getVariables(), setOutputType), ExpressionGetVariableBlock.c_field_VAR_NAME);

        this.setOutput(true);
    }

    public getVar(): VariableDescriptor
    {
        return this.getVarByName(this.getFieldString(ExpressionGetVariableBlock.c_field_VAR_NAME));
    }

    public setVar(newVar: string)
    {
        let field = <Blockly.FieldDropdown>this.getField(ExpressionGetVariableBlock.c_field_VAR_NAME);
        // Reset option cache
        field.getOptions(false);

        return this.setField(ExpressionGetVariableBlock.c_field_VAR_NAME, newVar);
    }

    private getVarByName(name: string)
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        return component.variableHolder.getVariableByName(name);
    }

    private getVariables()
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let vars      = component.variableHolder.getAllVariables();
        if (!vars.length)
        {
            return [
                [
                    "loading",
                    "loading"
                ]
            ];
        }

        return vars.map((variable) => [
            variable.name,
            variable.name
        ]);
    }

    toModel(): Models.EngineExpressionGetVariable
    {
        let variableGet = super.toModel();
        let variable    = this.getVar();

        variableGet.variable = Models.EngineVariableReference.newInstance({name: variable.name});
        return variableGet;
    }

    protected initFromModel(model: Models.EngineExpressionGetVariable): void
    {
        if (model.variable)
        {
            let component = this.getComponent<AppBlocklyWorkspaceComponent>();
            component.variableHolder.getVariableByName(model.variable.name, true);
            this.setVar(model.variable.name);
        }
    }

    public inferOutputType(): Type<Models.EngineValue>
    {
        let varDesc = this.getVar();
        if (varDesc && varDesc.type)
        {
            return varDesc.type;
        }

        return super.inferOutputType();
    }
}

//--//

export class VariablesCategory extends DynamicToolboxCategory
{
    getCategoryName()
    {
        return "variables_category";
    }

    public generateBlocks(): Element[]
    {
        if (this.m_color)
        {
            Block.setBlockColor(ExpressionGetVariableBlock, this.m_color);
            Block.setBlockColor(StatementVariableSetBlock, this.m_color);
        }

        let elements = [];

        elements.push(this.createVariableButton());
        elements.push(this.renameVariableButton());
        elements.push(...this.getVariableElements());

        return elements;
    }

    private createVariableButton(): Element
    {
        let callbackKey     = "CREATE_VARIABLE";
        let createVarButton = this.getButton("New variable", callbackKey);

        this.m_workspace.workspace.registerButtonCallback(callbackKey, () => this.promptNewVariable());

        return createVarButton;
    }

    private renameVariableButton(): Element
    {
        let callbackKey     = "RENAME_VARIABLE";
        let createVarButton = this.getButton("Rename variable", callbackKey);

        this.m_workspace.workspace.registerButtonCallback(callbackKey, () => this.promptRename());

        return createVarButton;
    }

    private promptNewVariable()
    {
        let workspaceComp = <AppBlocklyWorkspaceComponent>this.m_workspace;

        DialogPromptComponent.execute(workspaceComp, "Create new variable", "Variable Name")
                             .then((name) =>
                                   {
                                       if (name)
                                       {
                                           if (workspaceComp.variableHolder.getVariableByName(name))
                                           {
                                               // already exists
                                               Blockly.alert(`Variable with name "${name}" already exists.`, () => this.promptNewVariable());
                                           }
                                           else
                                           {
                                               workspaceComp.variableHolder.addVariable(name);

                                               // HACK: Create and delete blockly variable to trigger refresh of flyout
                                               workspaceComp.workspace.createVariable(name, "", name);
                                               workspaceComp.workspace.deleteVariableById(name);
                                           }
                                       }
                                   });
    }

    private promptRename()
    {
        let workspaceComp  = <AppBlocklyWorkspaceComponent>this.m_workspace;
        let variableHolder = workspaceComp.variableHolder;

        VariableRenamePromptComponent.open(workspaceComp, {
            variables: variableHolder.getAllVariables()
                                     .map((v) => v.name),
            renameVariable(old: string,
                           newName: string): void
            {
                if (old && newName)
                {
                    variableHolder.renameVariable(old, newName);

                    // HACK: Create and delete blockly variable to trigger refresh of flyout
                    workspaceComp.workspace.createVariable(newName, "", newName);
                    workspaceComp.workspace.deleteVariableById(newName);
                }
            }
        });
    }

    private getVariableElements(): Element[]
    {
        let result: Element[] = [];
        let workspace         = <AppBlocklyWorkspaceComponent>this.m_workspace;
        let variables         = workspace.variableHolder.getAllVariables();

        if (variables.length)
        {
            let lastVar = variables[variables.length - 1];
            let lastVarModel = lastVar.toModel();

            let setModel = Models.EngineVariableAssignment.newInstance({variable: lastVarModel});
            let setBlock = Block.fromModel(workspace.workspace, setModel);
            result.push(setBlock.toDom());
            setBlock.remove();

            let varRef = Models.EngineVariableReference.newInstance({name: lastVar.name});
            let getModel  = Models.EngineExpressionGetVariable.newInstance({variable: varRef});

            let getBlock = Block.fromModel(workspace.workspace, getModel);
            result.push(getBlock.toDom());
            getBlock.remove();
        }

        return result;
    }
}

//--//

export class VariableDescriptor
{
    constructor(public readonly name: string)
    {
    }

    type: Type<Models.EngineValue>;

    toModel(): Models.EngineVariable
    {
        return Models.EngineVariable.newInstance({
                                                     name: this.name,
                                                     type: Block.getDescriptorIdForType(this.type)
                                                 });
    }
}

export class VariableHolder
{
    private m_nameToDescriptor: Lookup<VariableDescriptor> = {};

    constructor(private m_workspace: AppBlocklyWorkspaceComponent)
    {
    }

    public getAllVariables(): VariableDescriptor[]
    {
        const variables = Object.keys(this.m_nameToDescriptor)
                                .map((key) => this.m_nameToDescriptor[key]);

        variables.sort((a,
                        b) => UtilsService.compareStrings(a.name, b.name, true));

        return variables;
    }

    public getVariableByName(name: string,
                             createIfMissing?: boolean): VariableDescriptor
    {
        let varDesc = this.m_nameToDescriptor[name];
        if (!varDesc && createIfMissing)
        {
            varDesc                       = new VariableDescriptor(name);
            this.m_nameToDescriptor[name] = varDesc;
        }

        return varDesc;
    }

    public addVariable(name: string,
                       type?: string)
    {
        let varDesc = this.getVariableByName(name, true);
        if (type)
        {
            varDesc.type = Block.getTypeForDescriptorId(type);
        }
    }

    public renameVariable(old: string,
                          newName: string)
    {
        let varDesc = this.getVariableByName(old, false);
        if (varDesc)
        {
            let newVar  = this.getVariableByName(newName, true);
            newVar.type = varDesc.type;

            this.m_workspace.data.renameVariable(old, newName);
            this.m_workspace.refreshTab();

            this.removeVariable(old);
        }
    }

    public removeVariable(name: string)
    {
        delete this.m_nameToDescriptor[name];
    }

    public clear()
    {
        return this.m_nameToDescriptor = {};
    }

    public computeVariableIfNeeded(block: Block<any>,
                                   blockCreated = false)
    {
        if (block instanceof StatementForEachBlock || block instanceof StatementVariableSetBlock || (block instanceof ExpressionGetVariableBlock && !blockCreated))
        {
            let varDesc = block.getVar();
            if (varDesc)
            {
                this.computeVariableTypes([varDesc]);
            }
        }
    }

    public computeAllVariableTypes()
    {
        let vars = this.getAllVariables();
        this.computeVariableTypes(vars);
    }

    private computeVariableTypes(vars: VariableDescriptor[])
    {
        let variableAssignments: StatementVariableSetBlock[] = this.m_workspace.getBlocksOfType(StatementVariableSetBlock, false);
        let forEachUses: Lists.StatementForEachBlock[]       = this.m_workspace.getBlocksOfType(Lists.StatementForEachBlock, false);
        let variableUses: ExpressionGetVariableBlock[]       = this.m_workspace.getBlocksOfType(ExpressionGetVariableBlock, false);

        variableAssignments.forEach((a) =>
                                    {
                                        let varDesc = a.getVar();
                                        if (varDesc?.type && vars.indexOf(varDesc) >= 0)
                                        {
                                            a.setAssignmentCheck(Block.computeTypeClosure(varDesc.type));
                                        }
                                    });
        variableUses.forEach((u) =>
                             {
                                 let varDesc = u.getVar();
                                 if (varDesc?.type && vars.indexOf(varDesc) >= 0)
                                 {
                                     u.setOutput(true, Block.computeTypeClosure(varDesc.type));
                                 }
                             });

        for (let variable of vars)
        {
            let matchingAssignments = variableAssignments.filter((v) => v.getVar() === variable);
            let matchingForEach     = forEachUses.filter((v) => v.getVar() === variable);
            let matchingUses        = variableUses.filter((v) => v.getVar() === variable);

            let varType: Type<Models.EngineValue>;
            for (let assignmentBlock of matchingAssignments)
            {
                let assignment = assignmentBlock.getAssignment();
                if (assignment)
                {
                    varType = assignment.inferOutputType();
                    break;
                }
            }

            for (let forEachBlock of matchingForEach)
            {
                let blockCheck = forEachBlock.getItemType();
                if (blockCheck)
                {
                    varType = blockCheck;
                    break;
                }
            }

            let typeClosure = Block.computeTypeClosure(varType);

            if (varType)
            {
                variable.type = varType;
                matchingAssignments.forEach((a) =>
                                            {
                                                a.setAssignmentCheck(typeClosure);
                                            });

                matchingUses.forEach((u) =>
                                     {
                                         u.setOutput(true, typeClosure);
                                     });
            }
            else
            {
                variable.type = null;
                matchingAssignments.forEach((a) =>
                                            {
                                                a.setAssignmentCheck(null);
                                            });

                matchingUses.forEach((u) =>
                                     {
                                         u.setOutput(true, null);
                                     });
            }
        }
    }

    public generateUniqueName()
    {
        let variableList = this.getAllVariables();
        let newName      = "";
        if (variableList.length)
        {
            let nameSuffix  = 1;
            let letters     = "ijkmnopqrstuvwxyzabcdefgh";  // No 'l'.
            let letterIndex = 0;
            let potName     = letters.charAt(letterIndex);
            while (!newName)
            {
                let inUse = false;
                for (let i = 0; i < variableList.length; i++)
                {
                    if (variableList[i].name.toLowerCase() == potName)
                    {
                        // This potential name is already used.
                        inUse = true;
                        break;
                    }
                }
                if (inUse)
                {
                    // Try the next potential name.
                    letterIndex++;
                    if (letterIndex == letters.length)
                    {
                        // Reached the end of the character sequence so back to 'i'.
                        // a new suffix.
                        letterIndex = 0;
                        nameSuffix++;
                    }
                    potName = letters.charAt(letterIndex);
                    if (nameSuffix > 1)
                    {
                        potName += nameSuffix;
                    }
                }
                else
                {
                    // We can use the current potential name.
                    newName = potName;
                }
            }
        }
        else
        {
            newName = "i";
        }
        return newName;
    };
}
