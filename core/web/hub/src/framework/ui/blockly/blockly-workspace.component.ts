import {Directive, EventEmitter, Input, Output} from "@angular/core";
import {UUID} from "angular2-uuid";
import * as Blockly from "blockly";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {AddToScratchPadComponent} from "framework/ui/blockly/add-to-scratch-pad.component";
import {BaseBlockModel, Block, BlockDefinition} from "framework/ui/blockly/block";
import {DynamicToolboxCategory, ToolboxCategory} from "framework/ui/blockly/toolbox-category";
import {BaseComponent, ComponentContext} from "framework/ui/components";
import {DialogPromptComponent} from "framework/ui/dialogs/dialog-prompt.component";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";

export class BlocklyExtendedWorkspace extends Blockly.WorkspaceSvg
{
    optio3Component: BlocklyWorkspaceComponent;
    optio3Context: ComponentContext;
    highlightedBlocks_: Blockly.Block[];
}

@Directive()
export abstract class BlocklyWorkspaceComponent extends BaseComponent
{
    private static readonly s_scratchPadAdd    = "scratchPadAdd";
    private static readonly s_scratchPadRemove = "scratchPadDelete";

    private m_workspace: BlocklyExtendedWorkspace;
    private m_workspaceInitialized = false;
    private m_toolboxInitialized   = false;
    private m_blocklyElement: HTMLElement;
    private m_pristineState: string;

    private m_dynamicToolboxes: DynamicToolboxCategory[]     = [];
    private m_scratchPadToolboxes: Lookup<ScratchPadToolbox> = {};
    private m_scratchPad: ScratchPad;

    private get scratchPad(): ScratchPad
    {
        if (!this.m_scratchPad)
        {
            this.m_scratchPad = {categories: []};
        }

        if (!this.m_scratchPad.categories) this.m_scratchPad.categories = [];

        return this.m_scratchPad;
    }

    private set scratchPad(value: ScratchPad)
    {
        this.m_scratchPad = value;
    }

    //--//

    @Input()
    public readonly: boolean = false;

    @Output()
    public updated = new EventEmitter<void>();

    //--//

    protected setNamespaces(namespaces: string[])
    {
        Block.resetDescriptors();

        for (let namespace of namespaces)
        {
            Block.selectPrototypes(namespace);
        }
    }

    //--//

    protected abstract getContainer(): HTMLElement;

    protected abstract initWorkspaceBlocks(): void;

    protected abstract getToolboxCategories(): Lookup<ToolboxCategory>;

    protected abstract onChange(event: Blockly.Events.Abstract): void;

    protected abstract fetchScratchPad(): Promise<ScratchPad>;

    protected abstract saveScratchPadCategory(category: ScratchPadCategory): Promise<void>;

    protected abstract deleteScratchPadCategory(categoryId: string): Promise<void>;

    protected abstract validateScratchPad(scratchPad: ScratchPad): Promise<ScratchPad>;

    private async initScratchPad(): Promise<void>
    {
        this.scratchPad = await this.fetchScratchPad();
        this.sortScratchPadCategories();
        this.initToolboxFromXml();
        this.refreshScratchPad();
    }

    public addScratchPadCategory(name: string): ScratchPadCategory
    {
        let category: ScratchPadCategory = {
            id     : UUID.UUID(),
            name   : name,
            entries: []
        };
        this.scratchPad.categories.push(category);
        this.sortScratchPadCategories();

        this.refreshScratchPad();

        this.saveScratchPadCategory(category);

        return category;
    }

    private addToScratchPad(categoryId: string,
                            block: Block<any>): void
    {
        let category = this.findScratchPadCategory(categoryId);
        let model    = block.toModel();
        if (category && model)
        {
            category.entries.push({
                                      id   : UUID.UUID(),
                                      model: model
                                  });

            this.saveScratchPadCategory(category);
        }
    }

    private findScratchPadCategory(id: string): ScratchPadCategory
    {
        return this.scratchPad.categories.find((c) => c.id === id);
    }

    public renameScratchPadCategory(id: string,
                                    newName: string): void
    {
        let category = this.findScratchPadCategory(id);
        if (category)
        {
            category.name = newName;
            this.sortScratchPadCategories();
            this.refreshScratchPad();

            this.saveScratchPadCategory(category);
        }
    }

    public removeScratchPadCategory(id: string): void
    {
        let scratchPad        = this.scratchPad;
        scratchPad.categories = scratchPad.categories.filter((c) => c.id !== id);
        this.m_scratchPadToolboxes[id]?.destroy();
        delete this.m_scratchPadToolboxes[id];

        this.m_workspace.getToolbox()
            .clearSelection();

        this.refreshScratchPad();

        this.deleteScratchPadCategory(id);
    }

    public removeFromScratchPad(categoryId: string,
                                id: string): void
    {
        let category = this.findScratchPadCategory(categoryId);
        if (category)
        {
            category.entries = category.entries.filter((e) => e.id !== id);

            this.workspace.getToolbox()
                .refreshSelection();

            this.saveScratchPadCategory(category);
        }
    }

    private exportScratchPad()
    {
        DownloadDialogComponent.open(this, "Export Scratch Pad", DownloadDialogComponent.fileName("scratchpad"), this.scratchPad);
    }

    private async importScratchPad()
    {
        const imported = await ImportDialogComponent.open(this, "Import Scratch Pad", {
            returnRawBlobs: () => false,
            parseFile     : (contents: string) => this.validateScratchPad(JSON.parse(contents))
        });

        if (imported)
        {
            this.scratchPad = imported;
            this.sortScratchPadCategories();
            this.refreshScratchPad();
            for (let category of this.scratchPad.categories)
            {
                await this.saveScratchPadCategory(category);
            }
        }
    }

    private sortScratchPadCategories()
    {
        this.scratchPad.categories.sort((a,
                                         b) => UtilsService.compareStrings(a.name, b.name, true));
    }

    public get workspace(): BlocklyExtendedWorkspace
    {
        return this.m_workspace;
    }

    public get flyoutWorkspace(): Blockly.WorkspaceSvg
    {
        return this.workspace.getFlyout()
                   .getWorkspace();
    }

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();
        setTimeout(() => this.refreshSize(), 1000);
    }

    protected afterLayoutChange(): void
    {
        this.refreshSize();
    }

    ngOnDestroy()
    {
        super.ngOnDestroy();

        if (this.m_workspace)
        {
            this.destroyDynamicToolboxes();
            this.destroyScratchPadToolboxes();
            this.unregisterScratchPadContextMenu();
            this.m_workspace.dispose();
            this.m_workspace.optio3Component = null;
            this.m_workspace.optio3Context   = null;
            this.m_workspace                 = null;
            this.m_pristineState             = null;
            this.m_blocklyElement            = null;
        }
    }

    public refreshSize()
    {
        if (!this.m_blocklyElement)
        {
            this.m_blocklyElement                = document.createElement("div");
            this.m_blocklyElement.style.position = "absolute";

            let container = this.getContainer();
            container.appendChild(this.getStyleOverrides());
            container.appendChild(this.m_blocklyElement);

            try
            {
                this.m_workspace = <BlocklyExtendedWorkspace>Blockly.inject(this.m_blocklyElement, {
                    toolbox : this.getInitialToolboxXml(),
                    zoom    : {
                        controls  : true,
                        wheel     : true,
                        startScale: 1.0,
                        maxScale  : 3,
                        minScale  : 0.3,
                        scaleSpeed: 1.2
                    },
                    trashcan: !this.readonly
                });

                this.m_workspace
                    .getToolbox()
                    .getFlyout().autoClose = false;

                this.registerScratchPadContextMenu();

                this.overrideBlocklyPaste();
            }
            catch (err)
            {
                this.m_blocklyElement.remove();
                this.m_blocklyElement = null;
                return;
            }

            this.m_workspace.optio3Component = this;
            this.m_workspace.optio3Context   = this.inject(ComponentContext);

            this.initScratchPad();

            this.initToolboxFromXml();

            this.m_workspace.addChangeListener((event: Blockly.Events.Abstract) =>
                                               {
                                                   if ((<any>event).type === Blockly.Events.UI)
                                                   {
                                                       // Only care about real changes to state.
                                                       return;
                                                   }

                                                   Blockly.Events.disable();
                                                   Blockly.Events.disableOrphans(event);
                                                   Blockly.Events.enable();

                                                   if (this.m_workspaceInitialized)
                                                   {
                                                       this.onChange(event);
                                                   }

                                                   if (this.isDirty())
                                                   {
                                                       this.updated.emit();
                                                   }
                                               });
        }

        if (this.m_workspace)
        {
            let blocklyDiv          = this.m_blocklyElement;
            let blocklyContainer    = this.getContainer();
            blocklyDiv.style.width  = blocklyContainer.offsetWidth + "px";
            blocklyDiv.style.height = blocklyContainer.offsetHeight + "px";
            blocklyDiv.style.top    = "0";
            blocklyDiv.style.left   = "0";
            Blockly.svgResize(this.m_workspace);

            if (!this.m_workspaceInitialized)
            {
                this.initWorkspace();
                this.workspace.options.readOnly = this.readonly;
            }
            else
            {
                this.m_workspace.render();
            }
        }
        this.detectChanges();
    }

    public getBlocksOfType<T extends Block<any>>(type: BlockDefinition<any> | any,
                                                 top = true): T[]
    {
        let blocks = top ? this.m_workspace.getTopBlocks(true) : this.m_workspace.getAllBlocks(true);

        return Block.castArray(blocks.filter((d) => d instanceof type));
    }

    public getAllBlocks(): Block<any>[]
    {
        let blocks = this.m_workspace.getAllBlocks(true);
        return Block.castArray(blocks);
    }

    public getTopBlocks(): Block<any>[]
    {
        let blocks = this.m_workspace.getTopBlocks(true);
        return Block.castArray(blocks);
    }

    public getBlockById(id: string): Block<any>
    {
        return Block.cast(this.m_workspace.getBlockById(id));
    }

    protected abstract getState(): string;

    public markPristine(): void
    {
        this.m_pristineState = this.getState();
    }

    public isDirty(): boolean
    {
        return this.getState() !== this.m_pristineState;
    }

    private initToolboxFromXml()
    {
        if (this.m_workspace && !this.m_toolboxInitialized)
        {
            let xml = this.getToolboxXml();
            this.m_workspace.updateToolbox(xml);
            this.m_toolboxInitialized = true;
        }
    }

    public refreshBlocks()
    {
        if (this.m_workspace)
        {
            this.clearWorkspace();
            this.initWorkspace();
        }
    }

    protected clearWorkspace()
    {
        Blockly.Events.disable();
        this.m_workspace.clear();
        this.m_workspaceInitialized = false;
        Blockly.Events.enable();
    }

    protected initWorkspace()
    {
        if (this.m_workspace && !this.m_workspaceInitialized)
        {
            // Disable events during initialization to prevent perf issues.
            Blockly.Events.disable();

            this.initWorkspaceBlocks();

            this.m_workspace.render();
            Blockly.svgResize(this.m_workspace);

            // Disable orphan blocks
            let blocks = this.getTopBlocks();
            for (let block of blocks)
            {
                Blockly.Events.disableOrphans(block.getDummyEvent());
            }

            Blockly.Events.enable();

            this.m_workspaceInitialized = true;
        }
    }

    public applyHighlights(highlights: Set<string>)
    {
        this.workspace.highlightBlock(null);

        if (highlights)
        {
            for (let block of highlights)
            {
                this.workspace.highlightBlock(block, true);
            }
        }
    }

    private overrideBlocklyPaste()
    {
        let paste    = this.m_workspace.paste.bind(this.m_workspace);
        let updateId = (xmlBlock: Element) =>
        {
            if (xmlBlock.getAttribute("id"))
            {
                xmlBlock.setAttribute("id", Blockly.utils.genUid());
            }
        };

        this.m_workspace.paste = (xmlBlock) =>
        {
            if (xmlBlock instanceof Element)
            {
                // Always generate new ids for blocks when pasting to avoid any conflicts
                updateId(xmlBlock);

                let childBlocks = xmlBlock.getElementsByTagName("block");
                for (let i = 0; i < childBlocks.length; i++)
                {
                    updateId(childBlocks.item(i));
                }
            }

            paste(xmlBlock);
        };
    }

    private unregisterScratchPadContextMenu()
    {
        const registry = Blockly.ContextMenuRegistry.registry;
        if (registry.getItem(BlocklyWorkspaceComponent.s_scratchPadAdd))
        {
            registry.unregister(BlocklyWorkspaceComponent.s_scratchPadAdd);
        }

        if (registry.getItem(BlocklyWorkspaceComponent.s_scratchPadRemove))
        {
            registry.unregister(BlocklyWorkspaceComponent.s_scratchPadRemove);
        }
    }

    private registerScratchPadContextMenu()
    {
        this.unregisterScratchPadContextMenu();

        const registry = Blockly.ContextMenuRegistry.registry;

        const addConfig = {
            id            : BlocklyWorkspaceComponent.s_scratchPadAdd,
            displayText   : "Add to scratch pad",
            scopeType     : Blockly.ContextMenuRegistry.ScopeType.BLOCK,
            preconditionFn: (scope: Blockly.ContextMenuRegistry.Scope) =>
            {
                return scope.block.isInFlyout ? "hidden" : "enabled";
            },
            callback      : async (scope: Blockly.ContextMenuRegistry.Scope) =>
            {
                if (!(scope.block instanceof Block))
                {
                    return;
                }

                const categoryId = await AddToScratchPadComponent.open(this, this.scratchPad);
                if (categoryId)
                {
                    this.addToScratchPad(categoryId, scope.block);
                }
            },
            weight        : 1
        };

        registry.register(addConfig);

        const removeConfig = {
            id            : BlocklyWorkspaceComponent.s_scratchPadRemove,
            displayText   : "Delete from scratch pad",
            scopeType     : Blockly.ContextMenuRegistry.ScopeType.BLOCK,
            preconditionFn: (scope: Blockly.ContextMenuRegistry.Scope) =>
            {
                return scope.block.isInFlyout && scope.block.getScratchPadEntry() ? "enabled" : "hidden";
            },
            callback      : async (scope: Blockly.ContextMenuRegistry.Scope) =>
            {
                if (!(scope.block instanceof Block))
                {
                    return;
                }

                const block = <Block<any>>scope.block;

                const confirmed = await this.confirmOperation("Deleting this entry cannot be undone.");
                if (confirmed)
                {
                    this.removeFromScratchPad(block.getScratchPadCategory(), block.getScratchPadEntry());
                }
            },
            weight        : 1
        };

        registry.register(removeConfig);
    }

    private destroyDynamicToolboxes()
    {
        for (let category of this.m_dynamicToolboxes)
        {
            category.destroy();
            this.workspace.removeToolboxCategoryCallback(category.getCategoryName());
        }

        this.m_dynamicToolboxes = [];
    }

    private destroyScratchPadToolboxes()
    {
        for (let id in this.m_scratchPadToolboxes)
        {
            let category = this.m_scratchPadToolboxes[id];
            category.destroy();
            this.workspace.removeToolboxCategoryCallback(category.getCategoryName());
        }

        this.m_scratchPadToolboxes = {};
    }

    private getToolboxXml()
    {
        let toolboxCategories = this.getToolboxCategories();
        if (!toolboxCategories)
        {
            return "";
        }

        // Disable events during this because we may generate blocks
        Blockly.Events.disable();

        let categories = "";
        for (let category in toolboxCategories)
        {
            let categoryOptions = toolboxCategories[category];
            let blocks          = "";
            let custom          = "";

            let definitions = categoryOptions.definitions || [];

            for (let definition of definitions)
            {
                Block.setBlockColor(definition, categoryOptions.color);
                if (!categoryOptions.custom)
                {
                    let block = Block.cast(this.flyoutWorkspace.newBlock(Block.getBlockName(definition)));
                    blocks += block.toXml();
                    block.remove();
                }
            }

            if (categoryOptions.custom)
            {
                let handler = new categoryOptions.custom(this, categoryOptions.color, definitions);
                handler.generateBlocks(); // Just to configure colors
                this.m_dynamicToolboxes.push(handler);
                custom = `custom="${handler.getCategoryName()}"`;
            }

            categories += `
<category name="${category}"  colour="${categoryOptions.color}" ${custom}>
    ${blocks}
</category>`;
        }

        categories += `
<sep gap="50"></sep>
${this.getScratchPadXml()}`;

        Blockly.Events.enable();

        return `<xml xmlns="http://www.w3.org/1999/xhtml" id="toolbox" style="display: none;">
${categories}
</xml>`;
    }

    private getInitialToolboxXml()
    {
        return `<xml><category name="Loading" colour="20"></category></xml>`;
    }

    private getStyleOverrides()
    {
        let style       = document.createElement("style");
        style.innerHTML = `
        .blocklyWsDragSurface.blocklyOverflowVisible {
            overflow: visible !important;
        }
        .blocklyToolboxDiv {
            display: ${this.readonly ? "none" : "block"};
        }`;

        return style;
    }

    private refreshScratchPad()
    {
        let xml = this.getToolboxXml();
        this.m_workspace.updateToolbox(xml);
        this.refreshSize();
    }

    private getScratchPadXml(): string
    {
        let categories: string[] = [];
        for (let category of this.scratchPad.categories)
        {
            categories.push(`<category name="${category.name}" custom="${category.id}"></category>`);
            if (!this.m_scratchPadToolboxes[category.id])
            {
                this.m_scratchPadToolboxes[category.id] = new ScratchPadToolbox(this, category);
            }
        }

        this.m_workspace.registerButtonCallback("EXPORT_SCRATCH_PAD", () => this.exportScratchPad());
        this.m_workspace.registerButtonCallback("IMPORT_SCRATCH_PAD", () => this.importScratchPad());
        return `
<category name="Scratch Pad" expanded="true" colour="0">
    <button text="Export Scratch Pad" callbackKey="EXPORT_SCRATCH_PAD"></button>
    <sep gap="5"></sep>
    <button text="Import Scratch Pad" callbackKey="IMPORT_SCRATCH_PAD"></button>
    ${categories.join("\n")}
</category>`;
    }
}

export interface ScratchPad
{
    categories: ScratchPadCategory[];
}

export interface ScratchPadCategory
{
    id: string;
    name: string;
    entries: ScratchPadEntry[];
}

export interface ScratchPadEntry
{
    id: string;
    model: BaseBlockModel;
}

class ScratchPadToolbox extends DynamicToolboxCategory
{
    constructor(workspace: BlocklyWorkspaceComponent,
                private m_category: ScratchPadCategory)
    {
        super(workspace, 0, [], m_category.id);
    }

    public getCategoryName(): string
    {
        return this.m_id;
    }

    public generateBlocks(): Element[]
    {
        let results: Element[] = [
            this.renameCategoryButton(),
            this.getSep(5),
            this.deleteCategoryButton(),
            this.getSep(40)
        ];

        if (!this.m_category.entries.length)
        {
            results.push(this.getLabel("No entries"));
        }
        else
        {
            let numFiltered = 0;
            for (let entry of this.m_category.entries)
            {
                try
                {
                    let block = Block.fromModel(this.m_workspace.workspace, entry.model);
                    block.setScratchPadCategory(this.m_category.id);
                    block.setScratchPadEntry(entry.id);
                    results.push(block.toDom());
                    block.remove();

                    results.push(this.getSep(30));
                }
                catch (e)
                {
                    // Failed to create block (probably due to not being part of the active contexts)
                    numFiltered++;
                }
            }

            if (numFiltered > 0)
            {
                results.push(this.getLabel("Some entries have been filtered because they contain unavailable blocks."));
            }
        }

        return results;
    }

    public destroy()
    {
        this.m_workspace.workspace.removeButtonCallback(this.renameCategoryKey);
        this.m_workspace.workspace.removeButtonCallback(this.deleteCategoryKey);
        super.destroy();
    }

    private get renameCategoryKey(): string
    {
        return `RENAME_CATEGORY_${this.getCategoryName()}`;
    }

    private get deleteCategoryKey(): string
    {
        return `DELETE_CATEGORY_${this.getCategoryName()}`;
    }

    private renameCategoryButton(): Element
    {
        let element = this.getButton("Rename category", this.renameCategoryKey);

        this.m_workspace.workspace.registerButtonCallback(this.renameCategoryKey, () => this.promptRenameCategory());

        return element;
    }

    private deleteCategoryButton(): Element
    {
        let element = this.getButton("Delete category", this.deleteCategoryKey);

        this.m_workspace.workspace.registerButtonCallback(this.deleteCategoryKey, () => this.promptDeleteCategory());

        return element;
    }

    private promptRenameCategory()
    {
        DialogPromptComponent.execute(this.m_workspace, "Rename category", "New Name")
                             .then((name) =>
                                   {
                                       if (name)
                                       {
                                           this.m_workspace.renameScratchPadCategory(this.m_category.id, name);
                                       }
                                   });
    }

    private promptDeleteCategory()
    {
        this.m_workspace.confirmOperation("Deleting this category cannot be undone.")
            .then((confirmed) =>
                  {
                      if (confirmed)
                      {
                          this.m_workspace.removeScratchPadCategory(this.m_category.id);
                      }
                  });
    }
}
