import {Component} from "@angular/core";

import * as Models from "app/services/proxy/model/models";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";
import {AlertTimelineItem} from "app/shared/timelines/timeline.component";

import moment from "framework/utils/moment";

@Component({
               selector   : "o3-report-element-alert-feed",
               templateUrl: "./report-element-alert-feed.component.html"
           })
export class ReportElementAlertFeedComponent extends ReportElementBaseComponent<ReportElementAlertFeedData, ReportElementAlertFeedConfiguration>
{
    alerts: AlertTimelineItem[];

    async afterConfigurationChanges()
    {
        let maxNumAlerts = this.config.forPreview ? 3 : undefined;
        let history      = await this.app.domain.widgetData.getAlertFeed(this.data.locations, this.data.alertTypes, this.data.start, this.data.end, maxNumAlerts);
        this.alerts      = AlertTimelineItem.createList(history.map((ext) => ext.model));

        this.markAsComplete();
    }
}

export class ReportElementAlertFeedConfiguration extends ReportElementConfigurationBase
{
    public static newReportModel(label: string,
                                 start: moment.Moment,
                                 end: moment.Moment,
                                 locations: string[],
                                 alertTypes: Models.AlertType[])
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.AlertFeed;
        model.configuration = new ReportElementAlertFeedConfiguration();
        model.data          = new ReportElementAlertFeedData(label, start.toDate(), end.toDate(), locations, alertTypes);
        return model;
    }
}

export class ReportElementAlertFeedData extends ReportElementDataBase
{
    constructor(readonly label: string,
                readonly start: Date,
                readonly end: Date,
                readonly locations: string[],
                readonly alertTypes: Models.AlertType[])
    {
        super();
    }
}
