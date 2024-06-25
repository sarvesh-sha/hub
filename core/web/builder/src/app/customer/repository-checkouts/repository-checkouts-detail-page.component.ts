import {Component, Injector} from "@angular/core";
import {ReportError} from "app/app.service";

import * as SharedSvc from "app/services/domain/base.service";
import {RepositoryCheckoutExtended} from "app/services/domain/repository-checkouts.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-repository-checkouts-detail-page",
               templateUrl: "./repository-checkouts-detail-page.component.html",
               styleUrls  : ["./repository-checkouts-detail-page.component.scss"]
           })
export class RepositoryCheckoutsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    id: string;
    extended: RepositoryCheckoutExtended;
    extendedRemoveChecks: Models.ValidationResult[];
    extendedNoRemoveReason: string;

    constructor(inj: Injector)
    {
        super(inj);

        this.extended = this.app.domain.repositoryCheckouts.allocateInstance();
    }

    protected async onNavigationComplete()
    {
        this.id = this.getPathParameter("checkoutId");

        if (this.id)
        {
            let repositories = this.app.domain.repositoryCheckouts;

            repositories.logger.debug(`Loading Repository Checkout: ${this.id}`);
            let extended = await repositories.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.extended = extended;

            this.extendedRemoveChecks   = await this.extended.checkRemove();
            this.extendedNoRemoveReason = this.fromValidationToReason("Remove is disabled because:", this.extendedRemoveChecks);

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.currentBranch;
            repositories.logger.debug(`Loaded Repository Checkout: ${JSON.stringify(this.extended.model)}`);
        }
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
