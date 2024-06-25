import {Component, Injector} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {CustomerSharedUserExtended} from "app/services/domain/customer-shared-users.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import {DatabaseMode} from "app/services/proxy/model/models";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-customer-shared-users-creation-page",
               templateUrl: "./customer-shared-users-creation-page.component.html"
           })
export class CustomerSharedUsersCreationPageComponent extends SharedSvc.BaseComponentWithRouter
{
    customer: CustomerExtended;

    user: Models.UserCreationRequest;
    roles: string;

    constructor(inj: Injector)
    {
        super(inj);

        this.user                                     = new Models.UserCreationRequest();
        this.app.ui.navigation.breadcrumbCurrentLabel = "New User";
    }

    protected async onNavigationComplete()
    {
        let id = this.getPathParameter("custId");
        this.customer = await this.app.domain.customers.getExtendedById(id);
        if (!this.customer)
        {
            this.exit();
        }
    }

    async save()
    {
        let settings = this.app.domain.settings;
        settings.logger.info(`Creating Shared User: ${this.user.emailAddress}`);

        // sync roles
        this.user.roles = CustomerSharedUserExtended.parseRoles(this.roles);

        await this.customer.createUser(this.user);
        settings.logger.info(`User Shared Saved: ${JSON.stringify(this.user)}`);
        this.exit();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
