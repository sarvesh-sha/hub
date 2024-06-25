import {Component, Inject} from "@angular/core";
import {BaseComponent} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-variable-rename-prompt",
               templateUrl: "./variable-rename-prompt.component.html"
           })
export class VariableRenamePromptComponent
{
    options: ControlOption<string>[];
    selectedVariable: string;
    newName: string;

    constructor(public dialogRef: OverlayDialogRef<void>,
                @Inject(OVERLAY_DATA) public data: VariableRenamer)
    {}

    ngOnInit()
    {
        if (this.data)
        {
            this.options = this.data.variables.map((variable) => new ControlOption<string>(variable, variable));
        }
    }

    static open(comp: BaseComponent,
                renamer: VariableRenamer)
    {
        return OverlayComponent.open(comp, VariableRenamePromptComponent, {data: renamer});
    }
}

export interface VariableRenamer
{
    variables: string[];

    renameVariable(old: string,
                   newName: string): void;
}
