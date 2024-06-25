import {Component, Inject} from "@angular/core";
import {SafeHtml} from "@angular/platform-browser";
import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./dialog-confirm.component.html"
           })
export class DialogConfirmComponent
{
    public title: string;
    public text: string | SafeHtml;

    constructor(public dialogRef: OverlayDialogRef<boolean>,
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
                                text: string | SafeHtml): Promise<boolean>
    {
        return OverlayComponent.open(comp, DialogConfirmComponent,
                                     {
                                         data: {
                                             title: title || "Are you Sure?",
                                             text : text
                                         }
                                     });
    }
}

interface DialogConfig
{
    title: string;
    text: string | SafeHtml;
}
