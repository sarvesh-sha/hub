import {Component, Inject} from "@angular/core";
import {BlocklyWorkspaceComponent} from "framework/ui/blockly/blockly-workspace.component";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-extract-variable",
               templateUrl: "./extract-variable.component.html"
           })
export class ExtractVariableComponent
{
    newVariableName: string;

    constructor(public dialogRef: OverlayDialogRef<string>,
                @Inject(OVERLAY_DATA) public data: ExtractVariableData)
    {}

    ngOnInit()
    {
    }

    static open(comp: BlocklyWorkspaceComponent,
                variableNames: string[]): Promise<string>
    {
        return OverlayComponent.open(comp, ExtractVariableComponent,
                                     {
                                         config: OverlayConfig.dialog({width: 400}),
                                         data  : {
                                             workspace    : comp,
                                             variableNames: variableNames
                                         }
                                     });
    }
}

export interface ExtractVariableData
{
    workspace: BlocklyWorkspaceComponent;
    variableNames: string[];
}
