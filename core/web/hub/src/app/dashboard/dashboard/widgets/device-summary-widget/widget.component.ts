import {ChangeDetectionStrategy, Component} from "@angular/core";

import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector       : "o3-device-summary-widget",
               templateUrl    : "./widget.template.html",
               styleUrls      : ["./widget.styles.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class DeviceSummaryWidgetComponent extends WidgetBaseComponent<Models.DeviceSummaryWidgetConfiguration, DeviceSummaryWidgetConfigurationExtended>
{
    private static readonly secondaryRowWidthLowerBound: number       = 180;
    private static readonly secondaryRowWidthCutoff: number           = 315;
    private static readonly secondaryRowHeightCutoffExemption: number = 130;

    values: any = {};

    get secondRowEnabled(): boolean
    {
        let width  = this.widthRaw;
        let height = this.heightRaw;

        return width && width >= DeviceSummaryWidgetComponent.secondaryRowWidthLowerBound &&
               (width >= DeviceSummaryWidgetComponent.secondaryRowWidthCutoff ||
                height && height >= DeviceSummaryWidgetComponent.secondaryRowHeightCutoffExemption);
    }

    public async bind()
    {
        await super.bind();

        if (!this.config.name) this.config.name = "Device Summary";

        let filters = Models.DeviceFilterRequest.newInstance({
                                                                 locationIDs      : this.config.locations,
                                                                 locationInclusive: true
                                                             });

        this.values = await this.app.domain.widgetData.getDeviceAggregates(filters);
    }

    public viewDevices()
    {
        let params = [];
        if (this.config.locations && this.config.locations.length)
        {
            params.push({
                            param: "locationID",
                            value: this.config.locations.join(",")
                        });
        }

        this.app.ui.navigation.go("/devices/summary", [], params);
    };

    public async refreshSize(): Promise<boolean>
    {
        return true;
    }

    protected getClipboardData(): ClipboardEntryData<Models.DeviceSummaryWidgetConfiguration, null>
    {
        let model = Models.DeviceSummaryWidgetConfiguration.deepClone(this.config);

        return new class extends ClipboardEntryData<Models.DeviceSummaryWidgetConfiguration, null>
        {
            constructor()
            {
                super("device summary");
            }

            public getDashboardWidget(): Models.DeviceSummaryWidgetConfiguration
            {
                return Models.DeviceSummaryWidgetConfiguration.deepClone(model);
            }

            public getReportItem(): null
            {
                return null;
            }
        }();
    }
}

@WidgetDef({
               friendlyName      : "Device Summary",
               typeName          : "DEVICE_SUMMARY",
               model             : Models.DeviceSummaryWidgetConfiguration,
               component         : DeviceSummaryWidgetComponent,
               dashboardCreatable: false,
               subgroupCreatable : false,
               maximizable       : false,
               defaultWidth      : 6,
               defaultHeight     : 3,
               hostScalableText  : false,
               needsProtector    : false,
               documentation     : {
                   description: "The Device Summary widget allows you to configure a summarized, filtered view of the desired devices.",
                   examples   : []
               }
           })
export class DeviceSummaryWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.DeviceSummaryWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        return [];
    }
}
