import {ChangeDetectionStrategy, Component, ViewChild} from "@angular/core";

import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {AlertTimelineItem, TimelineComponent, TimelineItem} from "app/shared/timelines/timeline.component";
import {UtilsService} from "framework/services/utils.service";

@Component({
               selector       : "o3-alert-feed-widget",
               templateUrl    : "./widget.template.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AlertFeedWidgetComponent extends WidgetBaseComponent<Models.AlertFeedWidgetConfiguration, AlertFeedWidgetConfigurationExtended>
{
    alerts: AlertTimelineItem[]  = [];
    timelineInitialized: boolean = false;

    @ViewChild(TimelineComponent, {static: true}) timelineComponent: TimelineComponent;

    public async bind()
    {
        await super.bind();

        if (!this.config.name) this.config.name = "Alert Feed";

        await this.loadAlertData();
    }

    private async loadAlertData()
    {
        if (!this.config.timeRange) this.config.timeRange = RangeSelectionExtended.newModel(Models.TimeRangeId.Last30Days);
        let timeRange = new RangeSelectionExtended(this.config.timeRange);
        let history   = await this.app.domain.widgetData.getAlertFeed(this.config.locations,
                                                                      this.config.alertTypes,
                                                                      timeRange.getMin()
                                                                               .toDate(),
                                                                      timeRange.getMax()
                                                                               .toDate());
        this.alerts   = AlertTimelineItem.createList(history.map((ext) => ext.model));
        this.markForCheck();
    }

    public async refreshContent()
    {
        await super.refreshContent();

        await this.loadAlertData();
    }

    public async refreshSize(): Promise<boolean>
    {
        return this.timelineComponent.refreshHeight();
    }

    public viewAlert(item: TimelineItem): void
    {
        if (item)
        {
            this.app.ui.navigation.go("/alerts/alert", [item.eventId]);
        }
    }

    public exportToExcel()
    {
        this.timelineComponent.exportToExcel();
    }

    protected getClipboardData(): ClipboardEntryData<Models.AlertFeedWidgetConfiguration, Models.ReportLayoutItem>
    {
        let model = Models.AlertFeedWidgetConfiguration.deepClone(this.config);

        return new class extends ClipboardEntryData<Models.AlertFeedWidgetConfiguration, Models.ReportLayoutItem>
        {
            constructor()
            {
                super("alert feed");
            }

            public getDashboardWidget(): Models.AlertFeedWidgetConfiguration
            {
                return Models.AlertFeedWidgetConfiguration.deepClone(model);
            }

            public getReportItem(): Models.ReportLayoutItem
            {
                let element = Models.CustomReportElementAlertFeed.newInstance({
                                                                                  locations : UtilsService.arrayCopy(model.locations),
                                                                                  alertTypes: UtilsService.arrayCopy(model.alertTypes)
                                                                              });
                return Models.ReportLayoutItem.newInstance({element: element});
            }
        }();
    }
}


@WidgetDef({
               friendlyName      : "Alert Feed",
               typeName          : "ALERT_FEED",
               model             : Models.AlertFeedWidgetConfiguration,
               component         : AlertFeedWidgetComponent,
               classes           : ["scrollable"],
               dashboardCreatable: true,
               subgroupCreatable : true,
               maximizable       : true,
               defaultWidth      : 6,
               defaultHeight     : 5,
               hostScalableText  : false,
               needsProtector    : true,
               documentation     : {
                   description: "The Alert Feed widget displays a vertical timeline history of alerts over the selected period of time.",
                   examples   : [
                       {
                           file       : "widgets/ALERT_FEED/demo.png",
                           label      : "Alert Feed",
                           description: "Alert feed using the default demo alerts displayed as a 6x6 widget."
                       }
                   ]
               }

           })
export class AlertFeedWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.AlertFeedWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
        this.model.timeRange = RangeSelectionExtended.newModel(Models.TimeRangeId.Last30Days);
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        return [];
    }
}
