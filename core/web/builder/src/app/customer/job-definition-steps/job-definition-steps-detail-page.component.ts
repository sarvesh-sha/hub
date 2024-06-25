import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";

import * as JobNamespace from "app/services/domain/job-definition-steps.service";
import {JobDefinitionStepExtended} from "app/services/domain/job-definition-steps.service";

@Component({
               selector   : "o3-job-definition-steps-detail-page",
               templateUrl: "./job-definition-steps-detail-page.component.html",
               styleUrls  : ["./job-definition-steps-detail-page.component.scss"]
           })
export class JobDefinitionStepsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    id: string;
    extended: JobDefinitionStepExtended;

    namespaceForTypes = JobNamespace;

    constructor(inj: Injector)
    {
        super(inj);

        this.extended = this.app.domain.jobDefinitionSteps.allocateInstance();
    }

    protected async onNavigationComplete()
    {
        this.id = this.getPathParameter("stepId");

        if (this.id)
        {
            let jobDefinitionSteps = this.app.domain.jobDefinitionSteps;

            jobDefinitionSteps.logger.debug(`Loading JobDefinitionStep: ${this.id}`);
            let extended = await jobDefinitionSteps.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.extended = extended;

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.name;
            jobDefinitionSteps.logger.debug(`Loaded JobDefinitionStep: ${JSON.stringify(this.extended.model)}`);
        }
    }

    save()
    {
        this.app.framework.errors.error("NOT_IMPLEMENTED", "This feature is not implemented.");
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    isType(type: typeof JobDefinitionStepExtended)
    {
        return this.extended instanceof type;
    }
}
