import {Component, ViewChild} from "@angular/core";

import {AppContext} from "app/app.service";
import {AlertsListComponent} from "app/customer/alerts/alerts-list.component";
import {AlertDataExporter} from "app/customer/alerts/alert-data-exporter";
import {AlertDefinitionExtended} from "app/services/domain/alert-definitions.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {AlertFiltersAdapterComponent} from "app/shared/filter/event/alert-filters-adapter.component";

import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";

@Component({
               selector   : "o3-alerts-summary-page",
               templateUrl: "./alerts-summary-page.component.html"
           })
export class AlertsSummaryPageComponent extends SharedSvc.BaseComponentWithRouter
{
    pristine: boolean = true;

    filtersLoaded: boolean;
    localFiltering: boolean;
    hasFilters: boolean   = false;
    firstRefresh: boolean = true;
    filters: Models.AlertFilterRequest;
    chips: FilterChip[]   = [];

    private m_filtersAdapter: AlertFiltersAdapterComponent;
    @ViewChild(AlertFiltersAdapterComponent) set filtersAdapter(adapter: AlertFiltersAdapterComponent)
    {
        if (adapter && this.m_filtersAdapter !== adapter)
        {
            this.m_filtersAdapter = adapter;
            this.hasFilters       = this.m_filtersAdapter.hasFilters;
            this.detectChanges();
        }
    }

    get filtersAdapter(): AlertFiltersAdapterComponent
    {
        return this.m_filtersAdapter;
    }

    @ViewChild(AlertsListComponent) alertsList: AlertsListComponent;

    static navigate(app: AppContext,
                    options: AlertsSummaryPageNavigationOptions = {})
    {
        let params = [];
        if (options.locationIDs)
        {
            params.push({
                            param: "locationID",
                            value: options.locationIDs.join(",")
                        });
        }

        if (options.alertRules)
        {
            let ruleIds = options.alertRules.map((rule) => rule.sysId);
            params.push({
                            param: "alertRule",
                            value: ruleIds.join(",")
                        });
        }

        if (options.alertTypeIDs)
        {
            params.push({
                            param: "alertTypeID",
                            value: options.alertTypeIDs.join(",")
                        });
        }

        if (options.alertSeverityIDs)
        {
            params.push({
                            param: "alertSeverityID",
                            value: options.alertSeverityIDs.join(",")
                        });
        }

        if (options.alertStatusIDs)
        {
            params.push({
                            param: "alertStatusID",
                            value: options.alertStatusIDs.join(",")
                        });
        }

        app.ui.navigation.go("/alerts/summary", [], params);
    }

    protected async onNavigationComplete()
    {
        this.filtersLoaded = false;

        let locationID      = this.getPathParameter("locationID");
        let alertRules      = this.getPathParameter("alertRule");
        let alertTypeID     = this.getPathParameter("alertTypeID");
        let alertSeverityID = this.getPathParameter("alertSeverityID");
        let alertStatusID   = this.getPathParameter("alertStatusID");

        // set filter values if locationID, alertRules, alertTypeID, alertSeverityID, or alertStatusID are provided for alert filtering
        this.localFiltering            = !!locationID || !!alertRules || !!alertTypeID || !!alertSeverityID || !!alertStatusID;
        this.filters                   = new Models.AlertFilterRequest();
        this.filters.locationInclusive = true;

        if (this.localFiltering)
        {
            if (locationID) this.filters.locationIDs = locationID.split(",");

            if (alertRules)
            {
                let rules: string[]     = alertRules.split(",");
                this.filters.alertRules = rules.map((ruleId) => AlertDefinitionExtended.newIdentity(ruleId));
            }

            if (alertTypeID) this.filters.alertTypeIDs = alertTypeID.split(",");

            if (alertSeverityID) this.filters.alertSeverityIDs = alertSeverityID.split(",");

            if (alertStatusID)
            {
                this.filters.alertStatusIDs = alertStatusID.split(",");
            }
            else
            {
                this.filters.alertStatusIDs = [];
            }
        }

        this.filtersLoaded = true;
    }

    refresh()
    {
        if (this.firstRefresh)
        {
            this.firstRefresh = false;
        }
        else
        {
            this.filters.locationInclusive = false;
        }

        this.hasFilters = this.m_filtersAdapter?.hasFilters;
    }

    exportToExcel()
    {
        const fileName       = DownloadDialogComponent.fileName("alerts_summary", ".xlsx");
        const dataDownloader = new AlertDataExporter(this.app.domain, fileName);
        DownloadDialogComponent.openWithGenerator(this, "Export Alerts", fileName, dataDownloader);
    }
}

export interface AlertsSummaryPageNavigationOptions
{
    locationIDs?: string[];
    alertRules?: Models.RecordIdentity[];
    alertTypeIDs?: Models.AlertType[];
    alertSeverityIDs?: Models.AlertSeverity[];
    alertStatusIDs?: Models.AlertStatus[];
}
