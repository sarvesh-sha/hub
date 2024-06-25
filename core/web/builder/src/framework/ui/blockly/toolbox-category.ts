import {Type} from "@angular/core";
import * as Blockly from "blockly";
import {Block, BlockDefinition} from "framework/ui/blockly/block";
import {BlocklyWorkspaceComponent} from "framework/ui/blockly/blockly-workspace.component";

export interface ToolboxCategory
{
    /**
     * Color value for the category. Will be applied to each block in the category.
     * Must be a Hue value according to Blockly's color specifications: https://developers.google.com/blockly/guides/create-custom-blocks/define-blocks
     */
    color?: number;

    /**
     * List of block definitions to include in the category
     */
    definitions: BlockDefinition<any>[];

    /**
     * A custom category handler to generate dynamic categories
     */
    custom?: Type<DynamicToolboxCategory>;
}

export abstract class DynamicToolboxCategory
{
    constructor(protected m_workspace: BlocklyWorkspaceComponent,
                protected m_color: number,
                protected m_definitions: BlockDefinition<any>[],
                protected m_id?: string)
    {
        m_workspace.workspace.registerToolboxCategoryCallback(this.getCategoryName(), () =>
        {
            Blockly.Events.disable();
            let result = this.generateBlocks();
            Blockly.Events.enable();
            return result;
        });

    }

    public abstract getCategoryName(): string;

    public abstract generateBlocks(): Element[];

    protected getElement(definition: BlockDefinition<any>): Element
    {
        if (this.m_color)
        {
            Block.setBlockColor(definition, this.m_color);
        }

        let block = Block.cast(this.m_workspace.workspace.newBlock(Block.getBlockName(definition)));
        let dom   = block.toDom();
        block.remove();
        return dom;
    }

    protected getLabel(text: string): Element
    {
        return this.getElementFromString(`<label text="${text}"></label>`);
    }

    protected getSep(gap: number): Element
    {
        return this.getElementFromString(`<sep gap="${gap}"></sep>`);
    }

    protected getButton(text: string, callbackKey: string): Element
    {
        return this.getElementFromString(`<button text="${text}" callbackKey="${callbackKey}"></button>`)
    }

    private getElementFromString(text: string): Element
    {
        return Blockly.Xml.textToDom(text);
    }

    destroy()
    {
        this.m_workspace.workspace.removeToolboxCategoryCallback(this.getCategoryName());
        this.m_workspace = null;
    }
}
