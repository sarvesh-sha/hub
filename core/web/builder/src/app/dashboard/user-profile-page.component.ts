import {Component, Injector} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-user-profile-page",
               templateUrl: "./user-profile-page.component.html"
           })
export class UserProfilePageComponent extends SharedSvc.BaseApplicationComponent
{
    user: Models.User;

    constructor(inj: Injector)
    {
        super(inj);
    }

    ngOnInit()
    {
        this.user = this.app.domain.users.user;
    }

    async save()
    {
        if (this.user)
        {
            this.app.domain.users.logger.debug(`Saving User Settings: ${this.user.sysId}`);

            try
            {
                this.user = await this.app.domain.users.saveUser(this.user);
                this.app.domain.users.logger.debug(`Saved User Settings: ${JSON.stringify(this.user)}`);
                this.exit();
            }
            catch (e)
            {
                this.app.domain.users.logger.debug(`Error Saving User Settings: ${e}`);
                this.revertAndExit();
            }
        }
    }

    revertAndExit()
    {
        this.app.domain.users.refreshCurrentUser();
        this.exit();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
