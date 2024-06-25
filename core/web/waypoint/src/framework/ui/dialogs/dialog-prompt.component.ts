import {Component, Inject} from "@angular/core";
import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./dialog-prompt.component.html"
           })
export class DialogPromptComponent
{
    public title: string;
    public text: string;

    public value: string;

    constructor(public dialogRef: OverlayDialogRef<string>,
                @Inject(OVERLAY_DATA) data: DialogConfig)
    {
        if (data)
        {
            this.title = data.title;
            this.text  = data.text;
        }
    }

    public static async execute(comp: BaseComponent,
                                title: string,
                                text: string): Promise<string>
    {
        return OverlayComponent.open(comp, DialogPromptComponent,
                                     {
                                         data: {
                                             title: title,
                                             text : text
                                         }
                                     });
    }
}

interface DialogConfig
{
    title: string;
    text: string;
}
