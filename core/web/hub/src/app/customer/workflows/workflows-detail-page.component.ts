import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";
import {ReportError} from "app/app.service";

import {AssetExtended, DeviceExtended, LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";
import {UserExtended} from "app/services/domain/user-management.service";
import * as Models from "app/services/proxy/model/models";
import {WorkflowTimelineItem} from "app/shared/timelines/timeline.component";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-workflows-detail-page",
               templateUrl: "./workflows-detail-page.component.html",
               styleUrls  : ["./workflows-detail-page.component.scss"]
           })
export class WorkflowsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    workflowId: string;
    workflowData: WorkflowExtended;

    alertAsset: AssetExtended;
    workflowLocation: LocationExtended;

    history: WorkflowTimelineItem[];
    workflowPriorityOptions: ControlOption<Models.WorkflowPriority>[];

    users: ControlOption<string>[] = [];

    private extendedDescription: string;

    get pristine(): boolean
    {
        if (!this.workflowData) return true;
        if (!this.workflowForm) return true;
        if (!this.workflowForm.pristine) return false;

        return UtilsService.equivalentStrings(this.extendedDescription, this.workflowData.model.extendedDescription);
    }

    @ViewChild("workflowForm", {static: true}) workflowForm: NgForm;

    constructor(inj: Injector)
    {
        super(inj);
    }

    protected onNavigationComplete()
    {
        this.workflowId = this.getPathParameter("id");

        this.loadWorkflow();
    }

    private async loadWorkflow()
    {
        if (this.workflowId)
        {
            this.app.domain.events.logger.info(`Loading Workflow: ${this.workflowId}`);
            let workflowData = await this.app.domain.events.getTypedExtendedById(WorkflowExtended, this.workflowId);

            if (!workflowData)
            {
                this.exit();
                return;
            }

            this.users = await this.app.bindings.getUsers(true);

            await workflowData.getAll();

            this.extendedDescription = workflowData.model.extendedDescription;
            this.workflowData        = workflowData;
            this.detectChanges();

            this.workflowPriorityOptions = await this.app.bindings.getWorkflowPriorities();
            this.alertAsset              = await workflowData.getAsset();
            this.workflowLocation        = await workflowData.getLocation();

            if (!this.workflowLocation && this.alertAsset instanceof DeviceExtended)
            {
                this.workflowLocation = await this.alertAsset.getLocation();
            }

            let types                                     = await this.app.bindings.getWorkflowTypes();
            let type                                      = types.find((t) => t.id === workflowData.typedModel.type);
            this.app.ui.navigation.breadcrumbCurrentLabel = type.label;

            this.app.domain.events.logger.info(`Workflow Loaded: ${JSON.stringify(this.workflowData.model)}`);

            let history  = await this.workflowData.getHistory();
            this.history = WorkflowTimelineItem.createList(history.map((ext) => ext.model));

            //--//

            this.removeAllDbSubscriptions();

            this.subscribeOneShot(workflowData,
                                  async (ext,
                                         action) =>
                                  {
                                      this.loadWorkflow();
                                  });
        }

        this.workflowForm.form.markAsPristine();
        this.detectChanges();
    }

    //--//

    get assignedTo(): string
    {
        return this.workflowData.typedModel.assignedTo && this.workflowData.typedModel.assignedTo.sysId || "";
    }

    set assignedTo(id: string)
    {
        let ri                                  = id ? UserExtended.newIdentity(id) : null;
        this.workflowData.typedModel.assignedTo = ri;
    }

    async assignUser(id: string)
    {
        let user = this.users.find((user) => user.id === id);
        if (user && await this.confirmOperation(`Are you sure you want to assign workflow to ${user.label}?`))
        {
            await this.workflowData.assignToUser(id);
        }
        else
        {
            this.assignedTo = null;
        }
    }

    //--//

    get canEnableDisable(): boolean
    {
        switch (this.workflowData.typedModel.status)
        {
            case Models.WorkflowStatus.Disabled:
            case Models.WorkflowStatus.Closed:
                return true;
        }

        return false;
    }

    get isEnabled(): boolean
    {
        return this.workflowData.typedModel.status !== Models.WorkflowStatus.Disabled &&
               this.workflowData.typedModel.status !== Models.WorkflowStatus.Disabling;
    }

    async isEnabledChanged(enabled: boolean)
    {
        this.workflowData.typedModel.status = enabled ? Models.WorkflowStatus.Active : Models.WorkflowStatus.Disabling;
        await this.save();
    }

    //--//

    get canReopen()
    {
        if (this.workflowData)
        {
            switch (this.workflowData.typedModel.status)
            {
                case Models.WorkflowStatus.Resolved:
                case Models.WorkflowStatus.Closed:
                    return true;
            }
        }

        return false;
    }

    get canResolve()
    {
        if (this.workflowData)
        {
            switch (this.workflowData.typedModel.status)
            {
                case Models.WorkflowStatus.Active:
                    return true;
            }
        }

        return false;
    }

    get canClose()
    {
        if (this.workflowData)
        {
            switch (this.workflowData.typedModel.status)
            {
                case Models.WorkflowStatus.Active:
                case Models.WorkflowStatus.Resolved:
                    return true;
            }
        }

        return false;
    }

    async reopenWorkflow()
    {
        this.workflowData.typedModel.status = Models.WorkflowStatus.Active;
        await this.save();
    }

    async resolveWorkflow()
    {
        this.workflowData.typedModel.status = Models.WorkflowStatus.Resolved;
        await this.save();
    }

    async closeWorkflow()
    {
        this.workflowData.typedModel.status = Models.WorkflowStatus.Closed;
        await this.save();
    }

    @ReportError
    async save()
    {
        await this.workflowData.save();

        this.app.framework.errors.success("Workflow updated", -1);

        await this.cancel();
    }

    async cancel()
    {
        await this.workflowData.refresh();

        await this.loadWorkflow();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
