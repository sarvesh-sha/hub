import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-customer-service-secrets-creation-page",
               templateUrl: "./customer-service-secrets-creation-page.component.html"
           })
export class CustomerServiceSecretsCreationPageComponent extends SharedSvc.BaseComponentWithRouter
{
    service: CustomerServiceExtended;

    model: Models.CustomerServiceSecret;
    roles: string;

    constructor(inj: Injector)
    {
        super(inj);

        this.model                                    = new Models.CustomerServiceSecret();
        this.app.ui.navigation.breadcrumbCurrentLabel = "New Secret";
    }

    protected async onNavigationComplete()
    {
        let id       = this.getPathParameter("svcId");
        this.service = await this.app.domain.customerServices.getExtendedById(id);
        if (!this.service)
        {
            this.exit();
        }
    }

    async save()
    {
        let settings = this.app.domain.settings;
        settings.logger.info(`Creating Shared Secret: ${this.model.context}`);

        this.model = await this.service.createSecret(this.model);
        settings.logger.info(`Shared Secret Saved: ${JSON.stringify(this.model)}`);
        this.exit();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
