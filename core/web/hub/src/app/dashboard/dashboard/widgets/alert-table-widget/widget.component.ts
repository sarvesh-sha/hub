import {ChangeDetectionStrategy, Component, ViewChild} from "@angular/core";

import {AlertsSummaryPageComponent, AlertsSummaryPageNavigationOptions} from "app/customer/alerts/alerts-summary-page.component";
import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {AlertTableComponent} from "app/shared/alerts/alert-table/alert-table.component";

@Component({
               selector       : "o3-alert-table-widget",
               templateUrl    : "./widget.template.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AlertTableWidgetComponent extends WidgetBaseComponent<Models.AlertTableWidgetConfiguration, AlertTableWidgetConfigurationExtended>
{
    ranges: Models.RangeSelection[];

    @ViewChild(AlertTableComponent) alertTable: AlertTableComponent;

    public async bind()
    {
        await super.bind();

        if (!this.config.name) this.config.name = "Alert Table";

        // Apply config fixup
        if (!this.config.severityColors) this.config.severityColors = AlertTableComponent.defaultSeverityColors();

        this.ranges = this.config.filterableRanges.map((filterableRange) => filterableRange.range);
    }

    public async refreshContent()
    {
        await super.refreshContent();

        if (this.alertTable) await this.alertTable.refreshContent();
    }

    public async refreshSize(): Promise<boolean>
    {
        return !!this.alertTable?.refreshSize();
    }

    protected focusUpdated()
    {
        super.focusUpdated();

        this.markForCheck();
    }

    public navigate(summary: Models.SummaryResult)
    {
        let options: AlertsSummaryPageNavigationOptions = {
            "locationIDs"     : this.config.locations,
            "alertRules"      : this.config.alertRules,
            "alertSeverityIDs": this.config.alertSeverityIDs,
            "alertStatusIDs"  : this.config.alertStatusIDs,
            "alertTypeIDs"    : this.config.alertTypeIDs
        };
        switch (this.config.groupBy)
        {
            case Models.SummaryFlavor.location:
                options.locationIDs = [summary.id];
                break;

            case Models.SummaryFlavor.rule:
                options.alertRules = [Models.RecordIdentity.newInstance({sysId: summary.id})];
                break;

            case Models.SummaryFlavor.severity:
                options.alertSeverityIDs = [<Models.AlertSeverity>summary.id];
                break;

            case Models.SummaryFlavor.status:
                options.alertStatusIDs = [<Models.AlertStatus>summary.id];
                break;

            case Models.SummaryFlavor.type:
                options.alertTypeIDs = [<Models.AlertType>summary.id];
                break;
        }

        AlertsSummaryPageComponent.navigate(this.app, options);
    }

    protected getClipboardData(): ClipboardEntryData<Models.AlertTableWidgetConfiguration, Models.ReportLayoutItem>
    {
        let model = Models.AlertTableWidgetConfiguration.deepClone(this.config);

        return new class extends ClipboardEntryData<Models.AlertTableWidgetConfiguration, Models.ReportLayoutItem>
        {
            constructor()
            {
                super("alert table");
            }

            public getDashboardWidget(): Models.AlertTableWidgetConfiguration
            {
                return Models.AlertTableWidgetConfiguration.deepClone(model);
            }

            public getReportItem(): Models.ReportLayoutItem
            {
                let element = Models.CustomReportElementAlertTable.deepClone({
                                                                                 label           : model.name,
                                                                                 groupBy         : model.groupBy,
                                                                                 locations       : model.locations,
                                                                                 rollupType      : model.rollupType,
                                                                                 alertStatusIDs  : model.alertStatusIDs,
                                                                                 alertTypeIDs    : model.alertTypeIDs,
                                                                                 alertSeverityIDs: model.alertSeverityIDs,
                                                                                 severityColors  : model.severityColors,
                                                                                 alertRules      : model.alertRules
                                                                             });
                return Models.ReportLayoutItem.newInstance({element: element});
            }
        }();
    }
}

@WidgetDef({
               friendlyName      : "Alert Table",
               typeName          : "ALERT_TABLE",
               model             : Models.AlertTableWidgetConfiguration,
               component         : AlertTableWidgetComponent,
               classes           : ["scrollable"],
               dashboardCreatable: true,
               subgroupCreatable : true,
               maximizable       : true,
               defaultWidth      : 6,
               defaultHeight     : 3,
               hostScalableText  : false,
               needsProtector    : true,
               documentation     : {
                   description: "The Alert Table widget displays all alerts in a tabular format. It can be configured to filter out alerts based on status and other criteria.",
                   examples   : [
                       {
                           file       : "widgets/ALERT_TABLE/alerts.png",
                           label      : "Alert Table",
                           description: "Alert table using the default settings displayed as a 3x5 widget."
                       }
                   ]
               }
           })
export class AlertTableWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.AlertTableWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
        let model = this.model;

        model.alertSeverityIDs = [];
        model.severityColors   = AlertTableComponent.defaultSeverityColors();
        model.alertStatusIDs   = [Models.AlertStatus.active];
        model.groupBy          = Models.SummaryFlavor.location;
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        return [];
    }
}
