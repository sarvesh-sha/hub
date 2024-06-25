import {Component, Injector} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-customer-shared-secrets-creation-page",
               templateUrl: "./customer-shared-secrets-creation-page.component.html"
           })
export class CustomerSharedSecretsCreationPageComponent extends SharedSvc.BaseComponentWithRouter
{
    customer: CustomerExtended;

    model: Models.CustomerSharedSecret;
    roles: string;

    constructor(inj: Injector)
    {
        super(inj);

        this.model                                    = new Models.CustomerSharedSecret();
        this.app.ui.navigation.breadcrumbCurrentLabel = "New Secret";
    }

    protected async onNavigationComplete()
    {
        let id        = this.getPathParameter("custId");
        this.customer = await this.app.domain.customers.getExtendedById(id);
        if (!this.customer)
        {
            this.exit();
        }
    }

    async save()
    {
        let settings = this.app.domain.settings;
        settings.logger.info(`Creating Shared Secret: ${this.model.context}`);

        this.model = await this.customer.createSecret(this.model);
        settings.logger.info(`Shared Secret Saved: ${JSON.stringify(this.model)}`);
        this.exit();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
