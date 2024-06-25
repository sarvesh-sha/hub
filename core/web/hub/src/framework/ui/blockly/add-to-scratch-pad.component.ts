import {Component, Inject} from "@angular/core";
import {BlocklyWorkspaceComponent, ScratchPad} from "framework/ui/blockly/blockly-workspace.component";
import {ControlOption} from "framework/ui/control-option";
import {DialogPromptComponent} from "framework/ui/dialogs/dialog-prompt.component";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-add-to-scratch-pad",
               templateUrl: "./add-to-scratch-pad.component.html"
           })
export class AddToScratchPadComponent
{
    addNewOption = new ControlOption("addNew", "Add new category...");
    options: ControlOption<string>[];
    selectedCategory: string;

    constructor(public dialogRef: OverlayDialogRef<string>,
                @Inject(OVERLAY_DATA) public data: AddScratchPadData)
    {}

    ngOnInit()
    {
        if (this.data)
        {
            this.refreshOptions();
        }
    }

    modelChanged()
    {
        if (this.selectedCategory === this.addNewOption.id)
        {
            this.selectedCategory = null;
            this.promptNewCategory();
        }
    }

    async promptNewCategory()
    {
        const name = await DialogPromptComponent.execute(this.data.workspace, "Add new category", "Category Name");
        if (name)
        {
            let existing = this.options.find((o) => o.label === name);
            if (existing)
            {
                this.selectedCategory = existing.id;
                this.data.workspace.workspace.optio3Context.framework.errors.error("Duplicate Category", "Category with that name already exists.");
            }
            else
            {
                let category          = this.data.workspace.addScratchPadCategory(name);
                this.selectedCategory = category.id;
                this.refreshOptions();
            }
        }
    }

    private refreshOptions()
    {
        this.options = this.data.scratchPad.categories.map((category) => new ControlOption<string>(category.id, category.name));
        this.options.unshift(this.addNewOption);
    }

    static open(comp: BlocklyWorkspaceComponent,
                scratchPad: ScratchPad): Promise<string>
    {
        return OverlayComponent.open(comp, AddToScratchPadComponent,
                                     {
                                         config: OverlayConfig.dialog({width: 400}),
                                         data  : {
                                             workspace : comp,
                                             scratchPad: scratchPad
                                         }
                                     });
    }
}

export interface AddScratchPadData
{
    workspace: BlocklyWorkspaceComponent;
    scratchPad: ScratchPad;
}
