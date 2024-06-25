import {Component, Injector, Input} from "@angular/core";

import {AssetExtended, DeviceExtended, LocationExtended} from "app/services/domain/assets.service";

import * as SharedSvc from "app/services/domain/base.service";
import {EventExtended, WorkflowDetails, WorkflowExtended} from "app/services/domain/events.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, FilterDebouncer, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-workflows-list",
               templateUrl: "./workflows-list.component.html"
           })
export class WorkflowsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, WorkflowExtended, WorkflowFlat>
{
    private m_filterDebouncer: FilterDebouncer<WorkflowFilter, Models.WorkflowFilterRequest>;

    @Input()
    public set filterText(value: string)
    {
        this.m_filterDebouncer.setProperty("filterText", value);
    }

    public get filterText(): string
    {
        return this.m_filterDebouncer.getProperty("filterText");
    }

    @Input()
    public set filters(value: Models.WorkflowFilterRequest)
    {
        this.m_filterDebouncer.setExternalFilter(value);
    }

    public get filters(): Models.WorkflowFilterRequest
    {
        return this.m_filterDebouncer.getExternalFilter();
    }

    table: DatatableManager<Models.RecordIdentity, EventExtended, WorkflowFlat>;

    constructor(inj: Injector)
    {
        super(inj);

        this.m_filterDebouncer = new FilterDebouncer(() =>
                                                     {
                                                         return new WorkflowFilter();
                                                     },
                                                     () =>
                                                     {
                                                         return this.getViewStateValue<WorkflowFilter>("TABLE_FILTERS");
                                                     },
                                                     (state) =>
                                                     {
                                                         this.setViewStateValue("TABLE_FILTERS", state);
                                                     },
                                                     (state,
                                                      baseFilters) =>
                                                     {
                                                         let filters = Models.WorkflowFilterRequest.deepClone(baseFilters) || new Models.WorkflowFilterRequest();

                                                         if (state.filterText?.length > 1)
                                                         {
                                                             filters.likeFilter = state.filterText;
                                                         }
                                                         else
                                                         {
                                                             filters.likeFilter = undefined;
                                                         }

                                                         return filters;
                                                     },
                                                     (filtersChanged: boolean) =>
                                                     {
                                                         if (filtersChanged)
                                                         {
                                                             this.table.resetPagination();
                                                         }

                                                         this.table.refreshData();
                                                     });

        this.table = this.newTableWithAutoRefresh(this.app.domain.events, this);
    }

    getTableConfigId(): string { return "workflows"; }

    getItemName(): string { return "Workflows"; }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        let filters    = this.m_filterDebouncer.generateFilter();
        filters.sortBy = this.mapSortBindings(this.table.sort);

        let response = await this.app.domain.events.getList(filters);
        let ids      = response.results;

        if (this.app.domain.events.hasAppliedFilters(filters))
        {
            let count                               = await this.app.domain.events.getCount(new Models.WorkflowFilterRequest());
            this.table.config.messages.totalMessage = `of ${count} Workflows`;
        }

        return ids;
    }

    getPage(offset: number,
            limit: number): Promise<WorkflowExtended[]>
    {
        return this.app.domain.events.getTypedPageFromTable(WorkflowExtended, this.table, offset, limit);
    }

    async transform(rows: WorkflowExtended[]): Promise<WorkflowFlat[]>
    {
        let types = await this.app.bindings.getWorkflowTypes();
        return await mapInParallel(rows,
                                   async (row,
                                          index) =>
                                   {
                                       let result      = new WorkflowFlat();
                                       result.extended = row;
                                       result.asset = await row.getAsset();

                                       let type       = types.find((t) => t.id === row.typedModel.type);
                                       result.type    = type ? type.label : "";
                                       result.details = await row.extractDetails();

                                       result.location = await row.getLocation();
                                       if (result.location)
                                       {
                                           result.locationName = await result.location.getRecursiveName();
                                       }

                                       result.createdBy  = await this.getUserName(row.typedModel.createdBy.sysId);
                                       result.assignedTo = await this.getUserName(row.typedModel.assignedTo ? row.typedModel.assignedTo.sysId : null, "Unassigned");

                                       return result;
                                   });
    }

    private async getUserName(id: string,
                              defaultName = "<unknown>")
    {
        if (!id)
        {
            return defaultName;
        }

        let user = await this.app.domain.userManagement.getExtendedById(id);
        if (user)
        {
            return user.fullName;
        }
        else
        {
            return defaultName;
        }
    }

    itemClicked(columnId: string,
                item: WorkflowFlat)
    {
        this.app.ui.navigation.go("/workflows/workflow", [item.extended.model.sysId]);
    }
}

class WorkflowFilter
{
    filterText: string;
}

class WorkflowFlat
{
    extended: WorkflowExtended;
    type: string;
    asset: AssetExtended;

    details: WorkflowDetails;

    location: LocationExtended;
    locationName: string;

    createdBy: string;
    assignedTo: string;
}
