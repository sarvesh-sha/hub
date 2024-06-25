import {Component, Injector, ViewChild} from "@angular/core";
import {ReportError} from "app/app.service";

import {RepositoryCheckoutsListComponent} from "app/customer/repository-checkouts/repository-checkouts-list.component";

import * as SharedSvc from "app/services/domain/base.service";
import {RepositoryExtended} from "app/services/domain/repositories.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-repositories-detail-page",
               templateUrl: "./repositories-detail-page.component.html",
               styleUrls  : ["./repositories-detail-page.component.scss"]
           })
export class RepositoriesDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    id: string;
    extended: RepositoryExtended;
    extendedRemoveChecks: Models.ValidationResult[];
    extendedNoRemoveReason: string;

    @ViewChild("childList", { static: false }) childList: RepositoryCheckoutsListComponent;

    constructor(inj: Injector)
    {
        super(inj);

        this.extended = this.app.domain.repositories.allocateInstance();
    }

    protected async onNavigationComplete()
    {
        this.id = this.getPathParameter("id");

        if (this.id)
        {
            let repositories = this.app.domain.repositories;

            repositories.logger.debug(`Loading Repository: ${this.id}`);
            let extended = await repositories.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.extended = extended;

            this.extendedRemoveChecks   = await this.extended.checkRemove();
            this.extendedNoRemoveReason = this.fromValidationToReason("Remove is disabled because:", this.extendedRemoveChecks);

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.name;
            repositories.logger.debug(`Loaded Repository: ${JSON.stringify(this.extended.model)}`);
        }
    }

    save()
    {
        this.app.framework.errors.error("NOT_IMPLEMENTED", "This feature is not implemented.");
    }


    @ReportError
    async remove()
    {
        await this.extended.remove();
        this.exit();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
