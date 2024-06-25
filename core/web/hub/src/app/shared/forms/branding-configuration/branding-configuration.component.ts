import {Component, EventEmitter, Input, Output} from "@angular/core";

import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {FileReadType, ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";

@Component({
               selector   : "o3-branding-configuration[data]",
               styleUrls  : ["./branding-configuration.component.scss"],
               templateUrl: "./branding-configuration.component.html"
           })
export class BrandingConfigurationComponent extends BaseApplicationComponent
{
    @Input() data: Models.BrandingConfiguration;
    @Input() logoContainerHeight: number   = 50;
    @Input() defaultPrimaryColor: string   = "#FFFFFF";
    @Input() defaultSecondaryColor: string = "#000000";

    set logo(logo: string)
    {
        logo = logo || "";
        if (this.data.logoBase64 != logo)
        {
            this.data.logoBase64 = logo;
            this.logoUpdated.emit();
        }
    }

    @Output() logoUpdated = new EventEmitter<void>();

    async uploadLogo()
    {
        this.logo = await ImportDialogComponent.open(this, "Import Logo", {
            returnRawBlobs: () => false,
            parseFile     : async (base64Logo: string) => typeof base64Logo === "string" ? base64Logo : null
        }, FileReadType.asDataURL);
    }
}
