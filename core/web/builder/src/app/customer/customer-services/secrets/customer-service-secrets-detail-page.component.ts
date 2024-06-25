import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceSecretExtended} from "app/services/domain/customer-service-secrets.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";

@Component({
               selector   : "o3-customer-service-secrets-detail-page",
               templateUrl: "./customer-service-secrets-detail-page.component.html",
               styleUrls  : [
                   "./customer-service-secrets-detail-page.component.scss"
               ]
           })
export class CustomerServiceSecretsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    service: CustomerServiceExtended;
    ext: CustomerServiceSecretExtended;

    constructor(inj: Injector)
    {
        super(inj);

        this.service = this.app.domain.customerServices.allocateInstance();
        this.ext     = this.app.domain.customerServiceSecrets.allocateInstance();
    }

    protected async onNavigationComplete()
    {
        this.service = await this.app.domain.customerServices.getExtendedById(this.getPathParameter("svcId"));
        if (!this.service)
        {
            this.exit();
        }

        this.ext = await this.app.domain.customerServiceSecrets.getExtendedById(this.getPathParameter("secretId"));
        if (!this.ext)
        {
            this.exit();
        }

        this.app.ui.navigation.breadcrumbCurrentLabel = (this.ext.model.context + " / " + this.ext.model.key);
    }

    async save()
    {
        await this.ext.save();
        this.exit();
    }

    async remove()
    {
        if (await this.confirmOperation(`Click Yes to confirm deletion of the secret for ${this.ext.model.context} / ${this.ext.model.key}.`))
        {
            let validations = await this.ext.remove();
            if (validations && validations.entries && validations.entries.length == 0) this.exit();
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
