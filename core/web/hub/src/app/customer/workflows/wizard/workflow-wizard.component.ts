import {Component, EventEmitter, Input, Output} from "@angular/core";
import {AppContext} from "app/app.service";
import {DeviceElementExtended, DeviceExtended, LogicalAssetExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-workflow-wizard",
               templateUrl: "./workflow-wizard.component.html"
           })
export class WorkflowWizardComponent extends BaseApplicationComponent
{
    @Input()
    public workflow: WorkflowExtended;

    @Input()
    public deviceContext: DeviceExtended;

    @Input()
    public deviceElementContext: DeviceElementExtended;

    @Input()
    public equipmentContext: LogicalAssetExtended;

    @Output()
    public wizardFinished = new EventEmitter<void>();

    data: WorkflowWizardData;

    ngOnInit()
    {
        super.ngOnInit();
        if (!this.workflow)
        {
            this.workflow = WorkflowExtended.newInstance(this.app.domain.events, new Models.Workflow());
        }

        this.data = new WorkflowWizardData(this.workflow, this.app, this.deviceContext, this.deviceElementContext, this.equipmentContext);
    }

    wizardCancel()
    {
        this.wizardFinished.emit();
    }

    async wizardCommit()
    {
        if (this.data && this.data.workflow)
        {
            if (this.data.workflow.model.sysId)
            {
                await this.data.workflow.save();
            }
            else
            {
                this.data.workflow = await this.app.domain.workflows.create(this.data.workflow.typedModel);
                this.app.framework.errors.success("Workflow Created", -1);
            }
        }

        this.wizardFinished.emit();
    }
}

export class WorkflowWizardData
{
    constructor(public workflow: WorkflowExtended,
                public app: AppContext,
                public deviceContext?: DeviceExtended,
                public deviceElementContext?: DeviceElementExtended,
                public equipmentContext?: LogicalAssetExtended)
    {
    }
}
