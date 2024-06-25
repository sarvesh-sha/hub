import {Component, ViewChild} from "@angular/core";
import {UserListComponent} from "app/customer/configuration/users/user-list.component";
import {UserWizardDialogComponent, UserWizardState} from "app/customer/configuration/users/wizard/user-wizard-dialog.component";
import * as SharedSvc from "app/services/domain/base.service";

@Component({
               selector   : "o3-user-list-page",
               templateUrl: "./user-list-page.component.html"
           })
export class UserListPageComponent extends SharedSvc.BaseApplicationComponent
{
    @ViewChild(UserListComponent, {static: true}) public usersList: UserListComponent;

    async new()
    {
        await UserWizardDialogComponent.open(new UserWizardState(), this);
    }
}
