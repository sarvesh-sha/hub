import {ChangeDetectionStrategy, Component, Input} from "@angular/core";

import {AlertsSummaryPageComponent, AlertsSummaryPageNavigationOptions} from "app/customer/alerts/alerts-summary-page.component";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {AlertTimelineItem} from "app/shared/timelines/timeline.component";

@Component({
               selector       : "o3-pane-alert-feed-field",
               templateUrl    : "./pane-alert-feed-field.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class PaneAlertFeedFieldComponent extends BaseApplicationComponent
{
    public label: string;
    private m_locationId: string;

    @Input()
    public set alertFeed(feed: Models.PaneFieldAlertFeed)
    {
        this.m_locationId = feed.value?.sysId;
        this.label        = feed.label;

        if (this.m_locationId)
        {
            this.initialized = false;
            this.init();
        }
    }

    initialized: boolean         = false;
    history: AlertTimelineItem[] = [];

    navigate(options: AlertsSummaryPageNavigationOptions = {})
    {
        options.locationIDs = [this.m_locationId];
        AlertsSummaryPageComponent.navigate(this.app, options);
    }

    private async init()
    {
        let activeAlerts = await this.app.domain.alerts.getExtendedAll(Models.AlertFilterRequest.newInstance({
                                                                                                                 locationIDs   : [this.m_locationId],
                                                                                                                 alertStatusIDs: [Models.AlertStatus.active]
                                                                                                             }));

        let history = [];

        for (let alert of activeAlerts)
        {
            let alertHistory = await alert.getHistory();
            if (alertHistory.length > 0)
            {
                history.push(alertHistory[0].model);
            }
        }

        this.history     = AlertTimelineItem.createList(history);
        this.initialized = true;
        this.markForCheck();
    }
}
