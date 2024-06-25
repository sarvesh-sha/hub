import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerSharedUserExtended} from "app/services/domain/customer-shared-users.service";
import {CustomerExtended} from "app/services/domain/customers.service";

@Component({
               selector   : "o3-customer-shared-users-detail-page",
               templateUrl: "./customer-shared-users-detail-page.component.html",
               styleUrls  : [
                   "./customer-shared-users-detail-page.component.scss"
               ]
           })
export class CustomerSharedUsersDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    customer: CustomerExtended;
    user: CustomerSharedUserExtended;
    roles: string;

    constructor(inj: Injector)
    {
        super(inj);

        this.customer = this.app.domain.customers.allocateInstance();
        this.user     = this.app.domain.customerSharedUsers.allocateInstance();
    }

    protected async onNavigationComplete()
    {
        this.customer = await this.app.domain.customers.getExtendedById(this.getPathParameter("custId"));
        if (!this.customer)
        {
            this.exit();
        }

        this.user = await this.app.domain.customerSharedUsers.getExtendedById(this.getPathParameter("userId"));
        if (!this.user)
        {
            this.exit();
        }

        this.roles = this.user.model.roles.join(" / ");

        this.app.ui.navigation.breadcrumbCurrentLabel = (this.user.model.firstName + " " + this.user.model.lastName);
    }

    async save()
    {
        // sync roles
        this.user.model.roles = CustomerSharedUserExtended.parseRoles(this.roles);

        await this.user.save();
        this.exit();
    }

    async remove()
    {
        if (await this.confirmOperation(`Click Yes to confirm deletion of the user account for ${this.user.model.firstName} ${this.user.model.lastName}.`))
        {
            let validations = await this.user.remove();
            if (validations && validations.entries && validations.entries.length == 0) this.exit();
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
