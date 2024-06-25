import {Component, Inject} from "@angular/core";

import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./control-point-preview.component.html"
           })
export class ControlPointPreviewComponent<T>
{
    private static active: Map<string, OverlayDialogRef<any>> = new Map<string, OverlayDialogRef<any>>();

    constructor(public dialogRef: OverlayDialogRef<T>,
                @Inject(OVERLAY_DATA) public data: ControlPointPreviewContext<T>)
    {
    }

    public static toggle<T>(comp: BaseComponent,
                            id: string,
                            showMenu: boolean): OverlayDialogRef<T>
    {
        if (this.active.has(id))
        {
            this.close(id);
            return null;
        }
        else
        {
            return this.openControlPointSummary(comp, id, showMenu);
        }
    }

    public static openControlPointSummary<T>(comp: BaseComponent,
                                             id: string,
                                             showMenu: boolean): OverlayDialogRef<T>
    {
        // Prevent duplicates from opening
        if (this.active.has(id)) return null;

        let data      = new ControlPointPreviewContext<T>();
        data.id       = id;
        data.showMenu = showMenu;

        // Create and open overlay
        let cfg = OverlayConfig.onTopDraggable({
                                                   coverAnchorWhenDisplayed: false,
                                                   isAnimationsEnabled     : true,
                                                   maxWidth                : "90vw",
                                                   width                   : 650,
                                                   maxHeight               : "75vw",
                                                   overlayClass            : "no-scroll"
                                               });
        let ref = OverlayComponent.openRef<any, T>(comp, ControlPointPreviewComponent, {
            config: cfg,
            data  : data
        });

        // Save reference to overlay
        this.active.set(id, ref);

        // Set up cleanup
        let subscription = ref.afterClose()
                              .subscribe(() =>
                                         {
                                             this.active.delete(id);
                                             subscription.unsubscribe();
                                         });

        // Return reference for additional control
        return ref;
    }

    public static close(id: string)
    {
        // Only close if the ref still exists
        if (this.active.has(id))
        {
            // Trigger close, ref will be cleaned up automatically
            this.active.get(id)
                .close();
        }
    }
}

class ControlPointPreviewContext<T>
{
    id: string;
    showMenu: boolean;
}
