import {CdkDragDrop} from "@angular/cdk/drag-drop";
import {Component, ElementRef, Input, ViewChild} from "@angular/core";
import * as Functions from "app/customer/engines/shared/function";
import * as Variables from "app/customer/engines/shared/variables";
import {ExpressionGetVariableBlock, StatementVariableSetBlock} from "app/customer/engines/shared/variables";
import {BlocklyWorkspaceBlocks, BlocklyWorkspaceData} from "app/customer/engines/shared/workspace-data";
import {WorkspaceTab} from "app/customer/engines/shared/workspace-tab";
import {UsersService} from "app/services/domain/users.service";
import * as Models from "app/services/proxy/model/models";

import * as Blockly from "blockly";
import {Block} from "framework/ui/blockly/block";
import {BlocklyWorkspaceComponent, ScratchPad, ScratchPadCategory} from "framework/ui/blockly/blockly-workspace.component";
import {ExtractVariableComponent} from "framework/ui/blockly/extract-variable.component";

@Component({
               selector   : "o3-app-blockly-workspace",
               styleUrls  : ["./workspace.component.scss"],
               templateUrl: "./workspace.component.html"
           })
export class AppBlocklyWorkspaceComponent extends BlocklyWorkspaceComponent
{
    private m_data: BlocklyWorkspaceData;
    private m_currentTab: WorkspaceTab;
    private m_undoStacks = new Map<WorkspaceTab, Blockly.Events.Abstract[]>();
    private m_redoStacks = new Map<WorkspaceTab, Blockly.Events.Abstract[]>();

    private static readonly s_extractVariable = "extractVariable";
    private static readonly s_scratchPadKey   = "ENGINE_SCRATCH_PAD";

    private static readonly s_ignoredChangeElements = [
        "disabled",
        "comment",
        "collapsed"
    ];

    @Input("data")
    public set data(data: BlocklyWorkspaceData)
    {
        this.m_data = data;

        if (this.m_data)
        {
            // Immediately mark pristine to avoid ExpressionChangedAfterChecked errors
            this.markPristine();
            this.m_currentTab = null;

            this.init();
        }
    }

    public get data(): BlocklyWorkspaceData
    {
        return this.m_data;
    }

    @Input()
    public set blocks(blocks: BlocklyWorkspaceBlocks)
    {
        this.m_blocks = blocks;
        this.setNamespaces(blocks.getNamespaces());
        this.init();
    }

    private m_blocks: BlocklyWorkspaceBlocks;

    @ViewChild("container", {static: true})
    private m_container: ElementRef;

    //--//

    private m_variableHolder = new Variables.VariableHolder(this);

    public get variableHolder(): Variables.VariableHolder
    {
        return this.m_variableHolder;
    }

    //--//

    private m_functionHolder = new Functions.FunctionHolder(this);

    public get functionHolder(): Functions.FunctionHolder
    {
        return this.m_functionHolder;
    }

    //--//

    private init(): void
    {
        if (this.workspace && this.m_data && this.m_blocks)
        {
            this.clearWorkspace();
            this.initWorkspace();
        }
    }

    protected async fetchScratchPad()
    {
        const usersService = this.workspace.optio3Context.injector.get(UsersService);
        const keys         = await usersService.getPreferenceValues(AppBlocklyWorkspaceComponent.s_scratchPadKey);
        const categories   = await Promise.all(keys.map((key) => usersService.getTypedPreference(AppBlocklyWorkspaceComponent.s_scratchPadKey, key, Models.ScratchPadCategory.fixupPrototype)));
        return {categories: categories || []};
    }

    protected async saveScratchPadCategory(category: ScratchPadCategory)
    {
        const usersService = this.workspace.optio3Context.injector.get(UsersService);
        await usersService.setTypedPreference(AppBlocklyWorkspaceComponent.s_scratchPadKey, category.id, Models.ScratchPadCategory.newInstance({
                                                                                                                                                   id     : category.id,
                                                                                                                                                   name   : category.name,
                                                                                                                                                   entries: category.entries.map(e => Models.ScratchPadEntry.newInstance(
                                                                                                                                                       {
                                                                                                                                                           id   : e.id,
                                                                                                                                                           model: <Models.EngineBlock>e.model
                                                                                                                                                       }))
                                                                                                                                               }));
    }

    protected async deleteScratchPadCategory(categoryId: string)
    {
        const usersService = this.workspace.optio3Context.injector.get(UsersService);
        await usersService.removePreference(AppBlocklyWorkspaceComponent.s_scratchPadKey, categoryId);
    }

    protected async validateScratchPad(scratchPad: ScratchPad): Promise<ScratchPad>
    {
        const usersService       = this.workspace.optio3Context.injector.get(UsersService);
        const parsed: ScratchPad = {categories: []};
        for (let category of scratchPad?.categories || [])
        {
            parsed.categories.push(await usersService.checkTypedPreferenceValue(AppBlocklyWorkspaceComponent.s_scratchPadKey,
                                                                                category.id,
                                                                                JSON.stringify(category),
                                                                                Models.ScratchPadCategory.fixupPrototype));
        }

        return parsed;
    }

    //--//

    public newTab()
    {
        let newTab = this.m_data.newTab();
        this.switchTab(newTab);
        this.updated.emit();
    }

    public switchTab(tab: WorkspaceTab)
    {
        if (tab === this.m_currentTab)
        {
            return;
        }

        // Store undo/redo state for the tab
        this.m_undoStacks.set(this.m_currentTab, [...this.workspace.undoStack_]);
        this.m_redoStacks.set(this.m_currentTab, [...this.workspace.redoStack_]);

        this.m_currentTab = tab;

        // Restore undo/redo state for the new tab
        this.workspace.clearUndo();
        this.workspace.undoStack_.push(...this.m_undoStacks.get(this.m_currentTab) || []);
        this.workspace.redoStack_.push(...this.m_redoStacks.get(this.m_currentTab) || []);

        this.clearWorkspace();
        this.initWorkspace();
    }

    public refreshTab()
    {
        this.clearWorkspace();
        this.initWorkspace();
    }

    public switchToTabWithBlock(id: string): boolean
    {
        let tabForBlock = this.data.getTabForBlock(id);
        if (tabForBlock && tabForBlock !== this.m_currentTab)
        {
            this.switchTab(tabForBlock);
            this.workspace.centerOnBlock(id);
            return true;
        }

        return false;
    }

    public centerOnBlock(id: string)
    {
        this.switchToTabWithBlock(id);
        const block       = this.getBlockById(id);
        // XY is in workspace coordinates.
        const xy          = block.getRelativeToSurfaceXY();
        // Height/width is in workspace units.
        const heightWidth = block.getHeightWidth();
        // Workspace scale, used to convert from workspace coordinates to pixels.
        const scale       = this.workspace.getScale();

        // Top left of block in pixels, relative to workspace origin (center 0,0).
        const pixelX = xy.x * scale;
        const pixelY = xy.y * scale;

        // Convert from workspace directions to canvas directions.
        const x = -pixelX;
        const y = -pixelY;

        this.workspace.scroll(x, y);
    }

    public closeTab(event: MouseEvent,
                    tab: WorkspaceTab)
    {
        event.stopPropagation();
        let nextTab = this.data.closeTab(tab);

        if (this.isTabActive(tab))
        {
            this.switchTab(nextTab);
        }

        this.updated.emit();
    }

    public tabDragged(event: CdkDragDrop<Models.EngineTab[]>)
    {
        this.data.rearrangeTabs(event.previousIndex, event.currentIndex);
        this.updated.emit();
    }

    public canCloseTab(tab: WorkspaceTab)
    {
        return tab.isEmpty() && this.data.tabs.length > 1;
    }

    public isTabActive(tab: WorkspaceTab)
    {
        return tab === this.m_currentTab;
    }

    //--//

    protected getContainer(): HTMLElement
    {
        return this.m_container && this.m_container.nativeElement;
    }

    protected getToolboxCategories()
    {
        return this.m_blocks.getToolboxCategories();
    }

    protected initWorkspaceBlocks()
    {
        this.registerExtractVariableContextMenu();

        if (!this.m_data)
        {
            return;
        }

        if (!this.m_currentTab)
        {
            this.m_currentTab = this.m_data.tabs[0];
        }

        this.m_data.enumerateAllBlocks((block) =>
                                       {
                                           if (block instanceof Models.EngineExpressionGetVariable)
                                           {
                                               this.variableHolder.addVariable(block.variable.name);
                                           }
                                           else if (block instanceof Models.EngineStatementForEach)
                                           {
                                               this.variableHolder.addVariable(block.variable.name, block.variable.type);
                                           }
                                           else if (block instanceof Models.EngineVariableAssignment)
                                           {
                                               this.variableHolder.addVariable(block.variable.name, block.variable.type);
                                           }
                                           else if (block instanceof Models.EngineProcedureDeclaration)
                                           {
                                               for (let variable of (block.arguments || []))
                                               {
                                                   this.variableHolder.addVariable(variable.name);
                                               }

                                               this.functionHolder.addFunction(block);
                                           }
                                       });


        // Then create the blocks.
        this.m_currentTab.addBlocksToWorkspace(this);

        let wasPristine = !this.isDirty();

        // Finally, refresh the variables' types.
        this.variableHolder.computeAllVariableTypes();

        // Reload all the blocks from the workspace so we have a clean starting state
        this.m_currentTab.collectBlocksFromWorkspace(this);

        this.functionHolder.update();

        if (wasPristine)
        {
            this.markPristine();
        }
    }

    private unregisterExtractVariableContextMenu()
    {
        const registry = Blockly.ContextMenuRegistry.registry;
        if (registry.getItem(AppBlocklyWorkspaceComponent.s_extractVariable))
        {
            registry.unregister(AppBlocklyWorkspaceComponent.s_extractVariable);
        }
    }

    private registerExtractVariableContextMenu()
    {
        this.unregisterExtractVariableContextMenu();

        const registry = Blockly.ContextMenuRegistry.registry;

        const extractVariableConfig = {
            id            : AppBlocklyWorkspaceComponent.s_extractVariable,
            displayText   : "Extract Variable",
            scopeType     : Blockly.ContextMenuRegistry.ScopeType.BLOCK,
            preconditionFn: (scope: Blockly.ContextMenuRegistry.Scope) =>
            {
                if (scope.block.isInFlyout || !scope.block.outputConnection || !this.findNearestStatement(scope.block))
                {
                    return "hidden";
                }

                return "enabled";
            },
            callback      : async (scope: Blockly.ContextMenuRegistry.Scope) =>
            {
                if (!(scope.block instanceof Block))
                {
                    return;
                }

                const newVariableName = await ExtractVariableComponent.open(this,
                                                                            this.variableHolder.getAllVariables()
                                                                                .map((v) => v.name));
                if (newVariableName)
                {
                    this.extractToVariable(newVariableName, scope.block);
                }
            },
            weight        : 1
        };

        registry.register(extractVariableConfig);
    }


    private extractToVariable(newVariableName: string,
                              block: Block<any>)
    {
        Blockly.Events.setGroup(true);

        let statementBlock = this.findNearestStatement(block);

        // Create new variable blocks
        this.variableHolder.addVariable(newVariableName);
        let assignVariable = Models.EngineVariableAssignment.newInstance({
                                                                             variable: Models.EngineVariable.newInstance({name: newVariableName})
                                                                         });

        let refVariable = Models.EngineExpressionGetVariable.newInstance({
                                                                             variable: Models.EngineVariableReference.newInstance({name: newVariableName})
                                                                         });

        let assignBlock = <StatementVariableSetBlock>Block.fromModel(this.workspace, assignVariable);
        let refBlock    = <ExpressionGetVariableBlock>Block.fromModel(this.workspace, refVariable);
        assignBlock.render();
        refBlock.render();

        // Move extracted block around, connect variable reference
        let oldBlockConnection = block.outputConnection.targetConnection;
        block.outputConnection.disconnect();
        assignBlock.setAssignmentValue(block);
        oldBlockConnection.connect(refBlock.outputConnection);

        // Connect variable assignment
        let oldStatementConnection = statementBlock.previousConnection.targetConnection;
        let oldPosition            = statementBlock.getCoordinates();
        if (oldStatementConnection)
        {
            statementBlock.previousConnection.disconnect();
        }
        assignBlock.nextConnection.connect(statementBlock.previousConnection);
        if (oldStatementConnection)
        {
            assignBlock.previousConnection.connect(oldStatementConnection);
        }
        else
        {
            assignBlock.moveTo(oldPosition);
        }

        Blockly.Events.setGroup(false);
    }

    private findNearestStatement(block: Block<any>): Block<any>
    {
        // Find the closest statement.
        let statementBlock = block;
        while (statementBlock)
        {
            if (statementBlock.outputConnection)
            {
                statementBlock = Block.cast(statementBlock.outputConnection.targetBlock());
            }
            else
            {
                break;
            }
        }

        if (!statementBlock?.previousConnection) return null;

        return statementBlock;
    }

    public ngOnDestroy()
    {
        this.m_functionHolder = null;
        this.m_variableHolder = null;
        this.m_currentTab     = null;
        this.m_data.destroy();
        this.m_data = null;
        super.ngOnDestroy();
    }

    protected onChange(event: Blockly.Events.Abstract)
    {
        let eventData = <any>event;

        if (event instanceof Blockly.Events.Create)
        {
            this.blockCreate(event.blockId);
        }

        if (event instanceof Blockly.Events.Delete)
        {
            this.blockDelete(event.blockId, eventData.ids);
        }

        if (event instanceof Blockly.Events.Change)
        {
            if (AppBlocklyWorkspaceComponent.s_ignoredChangeElements.indexOf(eventData.element) === -1)
            {
                this.blockChange(event.blockId);
            }
        }

        if (event instanceof Blockly.Events.Move)
        {
            if (eventData.oldParentId)
            {
                this.blockDisconnect(event.blockId, eventData.oldParentId, !!eventData.oldInputName);
            }

            if (eventData.newParentId)
            {
                this.blockConnect(event.blockId, eventData.newParentId, eventData.newInputName);
            }

            if (!eventData.oldParentId && !eventData.newParentId)
            {
                this.blockMove(event.blockId);
            }
        }

        this.functionHolder.update();
    }

    protected getState(): string
    {
        return this.data.getState();
    }

    private blockCreate(blockId: string)
    {
        let block = this.getBlockById(blockId);
        if (!block)
        {
            // Block deleted, ignore
            return;
        }
        this.m_currentTab.addBlock(block.toModel());
        this.variableHolder.computeVariableIfNeeded(block, true);
    }

    private blockChange(blockId: string)
    {
        let block = this.getBlockById(blockId);
        if (!block)
        {
            // Block deleted before change could process, ignore
            return;
        }
        let model = block.toModel();
        this.m_currentTab.updateBlock(blockId, model);

        this.variableHolder.computeVariableIfNeeded(block);
    }

    private blockDelete(blockId: string,
                        deletedIds: string[])
    {
        this.m_currentTab.deleteBlock(blockId, deletedIds);
    }

    private blockMove(blockId: string)
    {
        let block = this.getBlockById(blockId);
        // Check if block exists in case it was dragged to trash.
        if (block)
        {
            let coords = block.getCoordinates();
            let model  = this.m_currentTab.getBlock(blockId);
            model.x    = coords.x;
            model.y    = coords.y;
        }
    }

    private blockDisconnect(blockId: string,
                            oldParentId: string,
                            fromInput: boolean)
    {
        this.m_currentTab.disconnectBlock(blockId);

        let parentBlock = this.getBlockById(oldParentId);
        if (fromInput)
        {
            this.variableHolder.computeVariableIfNeeded(parentBlock);
        }
    }

    private blockConnect(blockId: string,
                         newParentId: string,
                         newInputName: string)
    {
        let parentBlock = this.getBlockById(newParentId);
        this.m_currentTab.connectBlock(blockId, parentBlock, newInputName);

        this.variableHolder.computeVariableIfNeeded(parentBlock);
    }
}
