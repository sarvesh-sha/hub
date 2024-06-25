import {Component, Injector, ViewChild} from "@angular/core";

import {JobDefinitionStepsListComponent} from "app/customer/job-definition-steps/job-definition-steps-list.component";
import {RepositoryBranchSelectionDialogComponent} from "app/customer/repository-branches/repository-branch-selection-dialog.component";
import {RepositoryCommitSelectionDialogComponent} from "app/customer/repository-commits/repository-commit-selection-dialog.component";

import * as SharedSvc from "app/services/domain/base.service";
import {JobDefinitionStepExtendedForGit} from "app/services/domain/job-definition-steps.service";
import {JobDefinitionExtended} from "app/services/domain/job-definitions.service";

@Component({
               selector   : "o3-job-definitions-detail-page",
               templateUrl: "./job-definitions-detail-page.component.html",
               styleUrls  : ["./job-definitions-detail-page.component.scss"]
           })
export class JobDefinitionsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    id: string;
    extended: JobDefinitionExtended;

    @ViewChild("childList", { static: false }) childList: JobDefinitionStepsListComponent;

    constructor(inj: Injector)
    {
        super(inj);

        this.extended = this.app.domain.jobDefinitions.allocateInstance();
    }

    protected async onNavigationComplete()
    {
        this.id = this.getPathParameter("jobId");

        if (this.id)
        {
            let jobDefinitions = this.app.domain.jobDefinitions;

            jobDefinitions.logger.debug(`Loading JobDefinition: ${this.id}`);
            let extended = await jobDefinitions.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.extended = extended;

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.name;
            jobDefinitions.logger.debug(`Loaded JobDefinition: ${JSON.stringify(this.extended.model)}`);
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

    async trigger()
    {
        for (let step of await this.extended.getSteps())
        {
            if (step instanceof JobDefinitionStepExtendedForGit)
            {
                let jobDefinitions = this.app.domain.jobDefinitions;

                let repo         = await step.getRepo();
                let dialogBranch = await RepositoryBranchSelectionDialogComponent.open(this, repo, "build", "Select");
                if (dialogBranch == null)
                {
                    jobDefinitions.logger.debug(`Build Dialog cancelled`);
                    return;
                }

                let dialogCommit = await RepositoryCommitSelectionDialogComponent.open(this, dialogBranch.branch, "build", "Build");
                if (dialogCommit == null)
                {
                    jobDefinitions.logger.debug(`Build Dialog cancelled`);
                    return;
                }

                let branch     = dialogCommit.branch.model.name;
                let commitHash = dialogCommit.commit.model.commitHash;

                jobDefinitions.logger.info(`Triggering build for ${this.extended.model.name} on ${branch}/${commitHash}`);
                await this.triggerSelected(branch, commitHash);
                return;
            }
        }

        // Just use the latest commit.
        await this.triggerSelected();
    }

    async triggerSelected(branch?: string,
                          commit?: string)
    {
        let job = await this.app.domain.jobDefinitions.trigger(this.id, branch, commit);
        this.app.ui.navigation.go("/", [
            "jobs",
            "item",
            job.sysId
        ]);
    }
}
