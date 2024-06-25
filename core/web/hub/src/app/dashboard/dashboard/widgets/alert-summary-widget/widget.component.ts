import {ChangeDetectionStrategy, Component} from "@angular/core";

import {AlertsSummaryPageComponent, AlertsSummaryPageNavigationOptions} from "app/customer/alerts/alerts-summary-page.component";
import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector       : "o3-alert-summary-widget",
               templateUrl    : "./widget.template.html",
               styleUrls      : ["./widget.styles.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AlertSummaryWidgetComponent extends WidgetBaseComponent<Models.AlertSummaryWidgetConfiguration, AlertSummaryWidgetConfigurationExtended>
{
    private static readonly secondaryRowWidthLowerBound: number       = 180;
    private static readonly secondaryRowWidthCutoff: number           = 295;
    private static readonly secondaryRowHeightCutoffExemption: number = 130;

    values: any = {};

    get secondRowEnabled(): boolean
    {
        let width  = this.widthRaw;
        let height = this.heightRaw;

        return width && width >= AlertSummaryWidgetComponent.secondaryRowWidthLowerBound &&
               (width >= AlertSummaryWidgetComponent.secondaryRowWidthCutoff ||
                height && height >= AlertSummaryWidgetComponent.secondaryRowHeightCutoffExemption);
    }

    public async bind()
    {
        await super.bind();

        if (!this.config.name) this.config.name = "Alert Summary";

        let filters = Models.AlertFilterRequest.newInstance({
                                                                locationIDs      : this.config.locations,
                                                                locationInclusive: true,
                                                                alertTypeIDs     : this.config.alertTypes
                                                            });

        this.values = await this.app.domain.widgetData.getAlertAggregates(filters);
    }

    public viewAlerts()
    {
        this.performViewAlerts();
    }

    public viewCriticalAlerts()
    {
        let options = {alertSeverityIDs: [Models.AlertSeverity.CRITICAL]};
        this.performViewAlerts(options);
    }

    private performViewAlerts(options: AlertsSummaryPageNavigationOptions = {})
    {
        if (this.config.locations && this.config.locations.length)
        {
            options.locationIDs = this.config.locations;
        }
        if (this.config.alertTypes && this.config.alertTypes.length)
        {
            options.alertTypeIDs = this.config.alertTypes;
        }

        AlertsSummaryPageComponent.navigate(this.app, options);
    }

    public async refreshSize(): Promise<boolean>
    {
        return true;
    }

    protected getClipboardData(): ClipboardEntryData<Models.AlertSummaryWidgetConfiguration, null>
    {
        let model = Models.AlertSummaryWidgetConfiguration.deepClone(this.config);

        return new class extends ClipboardEntryData<Models.AlertSummaryWidgetConfiguration, null>
        {
            constructor()
            {
                super("alert summary");
            }

            public getDashboardWidget(): Models.AlertSummaryWidgetConfiguration
            {
                return Models.AlertSummaryWidgetConfiguration.deepClone(model);
            }

            public getReportItem(): null
            {
                return null;
            }
        }();
    }
}

@WidgetDef({
               friendlyName      : "Alert Summary",
               typeName          : "ALERT_SUMMARY",
               model             : Models.AlertSummaryWidgetConfiguration,
               component         : AlertSummaryWidgetComponent,
               dashboardCreatable: true,
               subgroupCreatable : true,
               maximizable       : false,
               defaultWidth      : 6,
               defaultHeight     : 3,
               hostScalableText  : false,
               needsProtector    : false,
               documentation     : {
                   description: "The Alert Summary widget allows you to configure a summarized, filtered view of the desired alerts.",
                   examples   : [
                       {
                           file       : "widgets/ALERT_SUMMARY/details.png",
                           label      : "Alert Summary With Details",
                           description: "Simple alert summary, displayed as a 4x4 widget."
                       },
                       {
                           file       : "widgets/ALERT_SUMMARY/single.png",
                           label      : "Alert Summary Value",
                           description: "Alert summary widget in a small 2x2 size that automatically hides the additional summary details."
                       }
                   ]
               }

           })
export class AlertSummaryWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.AlertSummaryWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        return [];
    }
}
