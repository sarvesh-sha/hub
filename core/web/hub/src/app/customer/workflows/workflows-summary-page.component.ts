import {Component, Injector, ViewChild} from "@angular/core";
import {AppContext} from "app/app.service";
import {WorkflowsListComponent} from "app/customer/workflows/workflows-list.component";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {WorkflowFiltersAdapterComponent} from "app/shared/filter/event/workflow-filters-adapter.component";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";
import {Colorized} from "framework/ui/charting/core/colors";

@Component({
               selector   : "o3-workflows-summary-page",
               templateUrl: "./workflows-summary-page.component.html",
               styleUrls  : ["./workflows-summary-page.component.scss"]
           })
export class WorkflowsSummaryPageComponent extends SharedSvc.BaseComponentWithRouter
{
    summary: Colorized<Models.SummaryResult>[];

    filtersLoaded: boolean;
    localFiltering: boolean;
    hasFilters: boolean;
    chips: FilterChip[];
    filters: Models.WorkflowFilterRequest;

    private m_filtersAdapter: WorkflowFiltersAdapterComponent;
    @ViewChild(WorkflowFiltersAdapterComponent) set filtersAdapter(adapter: WorkflowFiltersAdapterComponent)
    {
        if (adapter && this.m_filtersAdapter !== adapter)
        {
            this.m_filtersAdapter = adapter;
            this.hasFilters       = this.m_filtersAdapter.hasFilters;
            this.detectChanges();
        }
    }

    get filtersAdapter(): WorkflowFiltersAdapterComponent
    {
        return this.m_filtersAdapter;
    }

    @ViewChild(WorkflowsListComponent) workflowList: WorkflowsListComponent;

    constructor(inj: Injector)
    {
        super(inj);
    }

    static navigate(app: AppContext,
                    options: WorkflowsSummaryPageNavigationOptions = {})
    {
        let params = [];
        if (options.assetIDs)
        {
            params.push({
                            param: "assetID",
                            value: options.assetIDs.join(",")
                        });
        }

        if (options.locationIDs)
        {
            params.push({
                            param: "locationID",
                            value: options.locationIDs.join(",")
                        });
        }

        if (options.workflowPriorityIDs)
        {
            params.push({
                            param: "workflowPriorityID",
                            value: options.workflowPriorityIDs.join(",")
                        });
        }

        if (options.workflowStatusIDs)
        {
            params.push({
                            param: "workflowStatusID",
                            value: options.workflowStatusIDs.join(",")
                        });
        }

        if (options.workflowTypeIDs)
        {
            params.push({
                            param: "workflowTypeID",
                            value: options.workflowTypeIDs.join(",")
                        });
        }

        app.ui.navigation.go("/workflows/summary", [], params);
    }

    protected async onNavigationComplete()
    {
        this.filtersLoaded = false;

        let assetId            = this.getPathParameter("assetID");
        let locationId         = this.getPathParameter("locationID");
        let workflowPriorityID = this.getPathParameter("workflowPriorityID");
        let workflowStatusID   = this.getPathParameter("workflowStatusID");
        let workflowTypeID     = this.getPathParameter("workflowTypeID");

        // set the cache values if locationID or alertTypeID are provided, for alert filtering
        this.localFiltering            = !!assetId || !!locationId || !!workflowTypeID || !!workflowStatusID || !!workflowPriorityID;
        this.filters                   = new Models.WorkflowFilterRequest();
        this.filters.locationInclusive = true;

        if (this.localFiltering)
        {
            if (assetId) this.filters.assetIDs = assetId.split(",");

            if (locationId) this.filters.locationIDs = locationId.split(",");

            if (workflowTypeID) this.filters.workflowTypeIDs = workflowTypeID.split(",");

            if (workflowPriorityID) this.filters.workflowPriorityIDs = workflowPriorityID.split(",");

            if (workflowStatusID) this.filters.workflowStatusIDs = workflowStatusID.split(",");
        }

        this.filtersLoaded = true;
    }

    refresh()
    {
        // location inclusivity only allowed when passed in
        this.filters.locationInclusive = false;

        this.hasFilters = this.m_filtersAdapter?.hasFilters;
    }
}

export interface WorkflowsSummaryPageNavigationOptions
{
    assetIDs?: string[];
    locationIDs?: string[];
    workflowPriorityIDs?: Models.WorkflowPriority[];
    workflowStatusIDs?: Models.WorkflowStatus[];
    workflowTypeIDs?: Models.WorkflowType[];
}
