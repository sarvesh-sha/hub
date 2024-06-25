import {Component} from "@angular/core";
import * as Models from "app/services/proxy/model/models";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";
import {LocationFiltersAdapter} from "app/shared/filter/filters-adapter";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-workflow-filters-adapter[request]",
               templateUrl: "./workflow-filters-adapter.component.html"
           })
export class WorkflowFiltersAdapterComponent extends LocationFiltersAdapter<Models.WorkflowFilterRequest>
{
    optionsLoaded: boolean = false;

    workflowStatusOptions: ControlOption<string>[];
    workflowTypeOptions: ControlOption<string>[];
    workflowPriorityOptions: ControlOption<string>[];
    userOptions: ControlOption<string>[];

    public async ngOnInit()
    {
        super.ngOnInit();

        [
            this.workflowStatusOptions,
            this.workflowTypeOptions,
            this.workflowPriorityOptions,
            this.userOptions
        ] = await Promise.all([
                                  this.app.bindings.getWorkflowStatuses(),
                                  this.app.bindings.getWorkflowTypes(),
                                  this.app.bindings.getWorkflowPriorities(),
                                  this.app.bindings.getUsers(true)
                              ]);

        this.optionsLoaded = true;
    }

    protected updateGlobalFilters()
    {
        super.updateGlobalFilters();

        this.filtersSvc.workflowPriorityIDs = this.m_request.workflowPriorityIDs;
        this.filtersSvc.workflowStatusIDs   = this.m_request.workflowStatusIDs;
        this.filtersSvc.workflowTypeIDs     = this.m_request.workflowTypeIDs;
        this.filtersSvc.createdByIDs        = this.m_request.createdByIDs;
        this.filtersSvc.assignedToIDs       = this.m_request.assignedToIDs;
    }

    protected syncWithGlobalFilters()
    {
        super.syncWithGlobalFilters();

        this.m_request.workflowPriorityIDs = <Models.WorkflowPriority[]>this.filtersSvc.workflowPriorityIDs;
        this.m_request.workflowStatusIDs   = <Models.WorkflowStatus[]>this.filtersSvc.workflowStatusIDs;
        this.m_request.workflowTypeIDs     = <Models.WorkflowType[]>this.filtersSvc.workflowTypeIDs;
        this.m_request.createdByIDs        = this.filtersSvc.createdByIDs;
        this.m_request.assignedToIDs       = this.filtersSvc.assignedToIDs;
    }

    protected emptyRequestInstance(): Models.WorkflowFilterRequest
    {
        return new Models.WorkflowFilterRequest();
    }

    protected newRequestInstance(request?: Models.WorkflowFilterRequest): Models.WorkflowFilterRequest
    {
        return Models.WorkflowFilterRequest.newInstance(request);
    }

    protected async appendChips(chips: FilterChip[]): Promise<void>
    {
        await super.appendChips(chips);

        let userOptions = await this.app.bindings.getUsers(true);

        let workflowChips = await Promise.all(
            [
                new FilterChip("Priority",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.workflowPriorityIDs = [];
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.workflowPriorityIDs,
                               await this.app.bindings.getWorkflowPriorities()),
                new FilterChip("Status",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.workflowStatusIDs = [];
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.workflowStatusIDs,
                               await this.app.bindings.getWorkflowStatuses()),
                new FilterChip("Type",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.workflowTypeIDs = [];
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.workflowTypeIDs,
                               await this.app.bindings.getWorkflowTypes()),
                new FilterChip("(Created By) User",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.createdByIDs = [];
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.createdByIDs,
                               userOptions),
                new FilterChip("(Assigned To) User",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.assignedToIDs = [];
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.assignedToIDs,
                               userOptions)
            ]);

        for (let chip of workflowChips) chips.push(chip);
    }

    protected areEquivalent(requestA: Models.WorkflowFilterRequest,
                            requestB: Models.WorkflowFilterRequest): boolean
    {
        if (!super.areEquivalent(requestA, requestB)) return false;

        if (!UtilsService.compareArraysAsSets(requestA.workflowPriorityIDs, requestB.workflowPriorityIDs)) return false;
        if (!UtilsService.compareArraysAsSets(requestA.workflowStatusIDs, requestB.workflowStatusIDs)) return false;
        if (!UtilsService.compareArraysAsSets(requestA.workflowTypeIDs, requestB.workflowTypeIDs)) return false;
        if (!UtilsService.compareArraysAsSets(requestA.createdByIDs, requestB.createdByIDs)) return false;
        return UtilsService.compareArraysAsSets(requestA.assignedToIDs, requestB.assignedToIDs);
    }
}
