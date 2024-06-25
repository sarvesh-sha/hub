import {Component, Inject} from "@angular/core";

import {BaseComponent} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./rich-text-report-date-selection-dialog.component.html"
           })
export class RichTextReportDateSelectionDialogComponent
{
    public selection: Set<RichTextReportDateMode> = new Set<RichTextReportDateMode>();
    public options                                = [
        new ControlOption<RichTextReportDateMode>(RichTextReportDateMode.Date, "Date only"),
        new ControlOption<RichTextReportDateMode>(RichTextReportDateMode.DateTime, "Date and time")
    ];

    constructor(public dialogRef: OverlayDialogRef<RichTextReportDateMode>,
                @Inject(OVERLAY_DATA) data: DialogConfig)
    {
        // Set up default selection
        this.selection.add(data.defaultMode);
    }

    public static async execute(comp: BaseComponent,
                                defaultMode: RichTextReportDateMode = RichTextReportDateMode.Date): Promise<RichTextReportDateMode>
    {
        let config = OverlayConfig.onTopDraggable({
                                                      showBackdrop: true,
                                                      minWidth    : 250
                                                  });

        return OverlayComponent.open(comp, RichTextReportDateSelectionDialogComponent,
                                     {
                                         config: config,
                                         data  : {defaultMode: defaultMode}
                                     });
    }

    public onSelectionChange(selection: Set<RichTextReportDateMode>)
    {
        this.selection.clear();
        if (selection.size > 0)
        {
            this.selection.add(selection.values()
                                        .next().value);
        }
    }

    public onInsert()
    {
        if (this.selection.size > 0)
        {
            this.dialogRef.close(this.selection.values()
                                     .next().value);
        }
    }
}

export enum RichTextReportDateMode
{
    Date,
    DateTime
}

interface DialogConfig
{
    defaultMode: RichTextReportDateMode;
}
