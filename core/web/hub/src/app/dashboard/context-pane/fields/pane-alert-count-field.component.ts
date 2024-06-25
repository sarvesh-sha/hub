import {ChangeDetectionStrategy, Component, Input} from "@angular/core";

import {AlertsSummaryPageComponent, AlertsSummaryPageNavigationOptions} from "app/customer/alerts/alerts-summary-page.component";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

@Component({
               selector       : "o3-pane-alert-count-field",
               templateUrl    : "./pane-alert-count-field.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class PaneAlertCountFieldComponent extends BaseApplicationComponent
{
    private m_locationId: string;
    private m_range: Models.RangeSelection;

    public activeAlerts: string;
    public totalAlerts: string;

    public onlyActive: boolean;
    public label: string;

    @Input()
    public set alertCount(field: Models.PaneFieldAlertCount)
    {
        this.m_locationId = field.value?.sysId;
        this.onlyActive   = field.onlyActive;
        this.label        = field.label;
        this.init();
    }

    @Input()
    public set range(range: Models.RangeSelection)
    {
        this.m_range = range;
        this.init();
    }

    navigateToActive()
    {
        let options = {alertStatusIDs: [Models.AlertStatus.active]};
        this.navigate(options);
    }

    navigate(options: AlertsSummaryPageNavigationOptions = {})
    {
        options.locationIDs = [this.m_locationId];
        AlertsSummaryPageComponent.navigate(this.app, options);
    }

    private async init()
    {
        if (!this.m_locationId || !this.m_range)
        {
            return;
        }

        let rangeExtended = new RangeSelectionExtended(this.m_range);
        let activeAlerts  = await this.app.domain.alerts.getExtendedAll(Models.AlertFilterRequest.newInstance({
                                                                                                                  locationIDs   : [this.m_locationId],
                                                                                                                  alertStatusIDs: [Models.AlertStatus.active]
                                                                                                              }));

        let allAlerts = await this.app.domain.alerts.getExtendedAll(Models.AlertFilterRequest.newInstance({
                                                                                                              locationIDs: [this.m_locationId],
                                                                                                              rangeEnd   : rangeExtended.getMax()
                                                                                                                                        .toDate(),
                                                                                                              rangeStart : rangeExtended.getMin()
                                                                                                                                        .toDate()
                                                                                                          }));

        let inactiveAlerts = allAlerts.filter((alert) => alert.typedModel.status !== Models.AlertStatus.active);


        this.activeAlerts = `${activeAlerts.length || 0}`;
        this.totalAlerts  = `${inactiveAlerts.length + activeAlerts.length || 0}`;

        this.detectChanges();
    }
}
