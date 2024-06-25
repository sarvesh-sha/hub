import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerSharedSecretExtended} from "app/services/domain/customer-shared-secrets.service";
import {CustomerExtended} from "app/services/domain/customers.service";

@Component({
               selector   : "o3-customer-shared-secrets-detail-page",
               templateUrl: "./customer-shared-secrets-detail-page.component.html",
               styleUrls  : [
                   "./customer-shared-secrets-detail-page.component.scss"
               ]
           })
export class CustomerSharedSecretsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    customer: CustomerExtended;
    ext: CustomerSharedSecretExtended;

    constructor(inj: Injector)
    {
        super(inj);

        this.customer = this.app.domain.customers.allocateInstance();
        this.ext      = this.app.domain.customerSharedSecrets.allocateInstance();
    }

    protected async onNavigationComplete()
    {
        this.customer = await this.app.domain.customers.getExtendedById(this.getPathParameter("custId"));
        if (!this.customer)
        {
            this.exit();
        }

        this.ext = await this.app.domain.customerSharedSecrets.getExtendedById(this.getPathParameter("secretId"));
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
