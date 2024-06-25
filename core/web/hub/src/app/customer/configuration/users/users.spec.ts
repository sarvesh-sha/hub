import {NgModule} from "@angular/core";
import {UUID} from "angular2-uuid";

import {UserDetailPageComponent} from "app/customer/configuration/users/user-detail-page.component";
import {UserListPageComponent} from "app/customer/configuration/users/user-list-page.component";
import {UserWizardDialogComponent} from "app/customer/configuration/users/wizard/user-wizard-dialog.component";
import {UserWizardFieldsStep} from "app/customer/configuration/users/wizard/user-wizard-fields.step";
import {UserWizardInfoStep} from "app/customer/configuration/users/wizard/user-wizard-info.step";
import * as Models from "app/services/proxy/model/models";
import {Test} from "app/test/base-tests";
import {TestCase, waitFor} from "app/test/driver";
import {WizardDriver} from "app/test/drivers/wizard-driver";

import {DialogConfirmComponent} from "framework/ui/dialogs/dialog-confirm.component";

abstract class UserTest extends Test
{
    protected m_page: UserListPageComponent;
    protected m_userRequest = Models.UserCreationRequest.newInstance({
                                                                         firstName   : "ATest",
                                                                         lastName    : "User",
                                                                         emailAddress: `test-${UUID.UUID()}@user.com`,
                                                                         password    : "Password123!",
                                                                         roles       : []
                                                                     });

    protected m_wizardDriver: WizardDriver = this.m_driver.getDriver(WizardDriver);

    public async init(): Promise<void>
    {
        await super.init();
        this.m_page = await this.m_driver.navigate(UserListPageComponent, "/configuration/users");
    }

    public async cleanup(): Promise<void>
    {
    }

    protected createUser(): Promise<Models.User>
    {
        return this.m_driver.app.domain.users.createUser(this.m_userRequest);
    }

    protected async deleteUser()
    {
        const user = this.m_page.usersList.table.rows.find((r) => r.ext.model.emailAddress === this.m_userRequest.emailAddress);
        await this.m_driver.app.domain.users.deleteUser(user.ext.model);
        await waitFor(() => !this.m_page.usersList.table.rows.find((r) => r.ext.model.emailAddress === this.m_userRequest.emailAddress), "User was not cleaned up.");
    }

    protected async openUser(emailAddress: string): Promise<void>
    {
        let user = await waitFor(() => this.m_page.usersList.table.rows.find((r) => r && r.ext.model.emailAddress === emailAddress), "Failed to find created user");

        // opens the created user for edit
        this.m_page.usersList.itemClicked("", user);
    }
}

@TestCase({
              id        : "user_loads",
              name      : "User List Loads",
              categories: ["Users"]
          })
class UserListLoadsTest extends UserTest
{
    public async execute(): Promise<void>
    {
        await waitFor(() => this.m_page.usersList.table.count > 5, "Rows were not greater than 5");
    }
}

@TestCase({
              id        : "user_create",
              name      : "Create new user",
              categories: ["Users"]
          })
class CreateNewUserTest extends UserTest
{
    public async execute(): Promise<void>
    {
        let oldCount = this.m_page.usersList.table.count;

        this.m_page.new();

        const wizard = await this.m_wizardDriver.getWizard(UserWizardDialogComponent);

        const infoStep = await this.m_wizardDriver.getStep(wizard, UserWizardInfoStep);
        await waitFor(() => !!infoStep.test_firstName, "Form not found");
        await this.m_driver.sendText(infoStep.test_firstName, "first name", this.m_userRequest.firstName);
        await this.m_driver.sendText(infoStep.test_lastName, "last name", this.m_userRequest.lastName);
        await this.m_driver.sendText(infoStep.test_email, "email", this.m_userRequest.emailAddress);
        await this.m_driver.sendText(infoStep.test_password, "password", this.m_userRequest.password);
        await this.m_driver.sendText(infoStep.test_passwordConfirm, "password confirmation", this.m_userRequest.password);

        wizard.next();

        const fieldStep = await this.m_wizardDriver.getStep(wizard, UserWizardFieldsStep);

        wizard.save();

        const userDetailPage = await this.m_driver.getComponent(UserDetailPageComponent);

        userDetailPage.exit();

        this.m_page = await this.m_driver.getComponent(UserListPageComponent);

        await waitFor(() => this.m_page.usersList.table.count > oldCount, "New user did not show up in list.");
    }

    public async cleanup(): Promise<void>
    {
        await this.deleteUser();
    }
}

@TestCase({
              id        : "user_delete",
              name      : "Delete user",
              categories: ["Users"]
          })
class DeleteUserTest extends UserTest
{
    private m_user: Models.User;

    public async init(): Promise<void>
    {
        await super.init();
        this.m_user = await this.createUser();
    }

    public async execute(): Promise<void>
    {
        await this.openUser(this.m_userRequest.emailAddress);

        let oldCount = this.m_page.usersList.table.count;

        const userDetailPage = await this.m_driver.getComponent(UserDetailPageComponent);

        userDetailPage.remove();

        const confirm = await this.m_driver.getComponent(DialogConfirmComponent);

        await confirm.dialogRef.close(true);

        this.m_page = await this.m_driver.getComponent(UserListPageComponent);
        await waitFor(() => this.m_page.usersList.table.count < oldCount, "User was not removed from list.");
    }

    public async cleanup(): Promise<void>
    {
    }
}

@TestCase({
              id        : "user_edit",
              name      : "Edit user last name and role",
              categories: ["Users"]
          })
class EditUserTest extends UserTest
{
    private m_user: Models.User;

    public async init(): Promise<void>
    {
        await super.init();
        this.m_user = await this.createUser();
    }

    public async execute(): Promise<void>
    {
        await this.openUser(this.m_userRequest.emailAddress);

        const userDetailPage = await this.m_driver.getComponent(UserDetailPageComponent);

        userDetailPage.edit();

        const wizard = await this.m_wizardDriver.getWizard(UserWizardDialogComponent);

        // edit the user last name
        const infoStep = await this.m_wizardDriver.getStep(wizard, UserWizardInfoStep);

        await waitFor(() => !!infoStep.test_lastName, "Form not found");
        await this.m_driver.sendText(infoStep.test_lastName, "last name", "Doe");

        // edit the user's role
        wizard.next();
        const fieldStep = await this.m_wizardDriver.getStep(wizard, UserWizardFieldsStep);

        await waitFor(() => !!fieldStep.test_roleSelect, "Form not found");
        fieldStep.test_roleSelect.value = [fieldStep.data.roleOptions.find((opt) => opt.label === "SYS.ADMIN").id];
        fieldStep.test_roleSelect.submit();

        wizard.save();

        userDetailPage.exit();

        this.m_page = await this.m_driver.getComponent(UserListPageComponent);
        await waitFor(() => this.m_page.usersList.table.rows.find((r) => r && r.ext.model.lastName === "UserDoe"), "Last name wasn't updated.");
        await waitFor(() => this.m_page.usersList.table.rows.find((r) => r && r.ext.model.lastName === "UserDoe").rolesText === "Administrator", "Role wasn't updated");
    }

    public async cleanup(): Promise<void>
    {
        await this.deleteUser();
    }
}


@NgModule({imports: []})
export class UsersTestsModule {}
