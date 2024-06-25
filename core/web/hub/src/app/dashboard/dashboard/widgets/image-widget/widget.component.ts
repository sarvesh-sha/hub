import {ChangeDetectionStrategy, Component} from "@angular/core";

import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {horizontalAlignmentToRelativeLocation, verticalAlignmentToRelativeLocation} from "app/shared/options/placement-options";

import {RelativeLocation} from "framework/ui/utils/relative-location-styles";

@Component({
               selector       : "o3-image-widget",
               templateUrl    : "./widget.template.html",
               styleUrls      : ["./widget.styles.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ImageWidgetComponent extends WidgetBaseComponent<Models.ImageWidgetConfiguration, ImageWidgetConfigurationExtended>
{
    get horizontalAlignment(): RelativeLocation
    {
        return horizontalAlignmentToRelativeLocation(this.config.image.horizontalPlacement);
    }

    get verticalAlignment(): RelativeLocation
    {
        return verticalAlignmentToRelativeLocation(this.config.image.verticalPlacement);
    }

    public async bind(): Promise<void>
    {
        await super.bind();

        if (!this.config.image) this.config.image = new Models.BrandingConfiguration();
    }

    protected getClipboardData(): ClipboardEntryData<Models.ImageWidgetConfiguration, null>
    {
        let model = Models.ImageWidgetConfiguration.deepClone(this.config);

        return new class extends ClipboardEntryData<Models.ImageWidgetConfiguration, null>
        {
            constructor()
            {
                super("image");
            }

            public getDashboardWidget(): Models.ImageWidgetConfiguration
            {
                return Models.ImageWidgetConfiguration.deepClone(model);
            }

            public getReportItem(): null
            {
                return null;
            }
        }();
    }

    public async refreshSize(): Promise<boolean>
    {
        return true;
    }
}

@WidgetDef({
               friendlyName      : "Image",
               typeName          : "IMAGE",
               model             : Models.ImageWidgetConfiguration,
               component         : ImageWidgetComponent,
               dashboardCreatable: true,
               subgroupCreatable : true,
               maximizable       : true,
               defaultWidth      : 6,
               defaultHeight     : 4,
               hostScalableText  : false,
               needsProtector    : false,
               documentation     : {
                   description: "The Image widget allows you to place images on your dashboard. You can control background color and alignment. Supports all web-usable file formats.",
                   examples   : [
                       {
                           file       : "widgets/IMAGE/image.png",
                           label      : "Image Widget",
                           description: "Image widget containing a generic file type logo centered, displayed as a 3x4 widget."
                       }
                   ]
               }
           })
export class ImageWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.ImageWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
        this.model.image = new Models.BrandingConfiguration();
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        return [];
    }
}
