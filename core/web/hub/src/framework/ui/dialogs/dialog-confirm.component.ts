import {Component, Inject, ViewChild} from "@angular/core";
import {SafeHtml} from "@angular/platform-browser";
import {WithTestId} from "framework/directives/optio3-test.directive";

import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";
import {StandardFormComponent} from "framework/ui/overlays/standard-form.component";

@Component({
               templateUrl: "./dialog-confirm.component.html"
           })
export class DialogConfirmComponent
{
    public readonly confirmText = "Yes";
    public readonly denyText = "No";

    get data(): DialogConfig
    {
        return this.m_data;
    }

    @ViewChild("test_standardForm") test_standardForm: StandardFormComponent;

    constructor(public dialogRef: OverlayDialogRef<boolean>,
                @Inject(OVERLAY_DATA) private m_data: DialogConfig)
    {}

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

interface DialogConfig extends WithTestId
{
    title: string;
    text: string | SafeHtml;
}
