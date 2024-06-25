import {Overlay} from "@angular/cdk/overlay";
import {Component, Directive, Input} from "@angular/core";

import * as Models from "app/services/proxy/model/models";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-context-pane",
               templateUrl: "./context-pane.component.html",
               styleUrls  : ["./context-pane.component.scss"],
               host       : {
                   "class": "o3-context-pane"
               }
           })
export class ContextPaneComponent
{
    @Input() public paneTitle: string;
    @Input() public subtitle: string;
    @Input() public branding: Models.BrandingConfiguration;
    @Input() public editing: boolean = false;

    public static getOverlayConfig(overlay: Overlay)
    {
        return OverlayConfig.newInstance({
                                             height          : "100%",
                                             showCloseButton : false,
                                             positionStrategy: overlay.position()
                                                                      .global()
                                                                      .top()
                                                                      .left()
                                         });
    }

    public get flexDirection()
    {
        return this.branding.horizontalPlacement === Models.HorizontalAlignment.Left ? "row" : "row-reverse";
    }
}

@Directive({
               selector: "o3-context-pane-header"
           })
export class ContextPaneHeaderDirective
{
}
