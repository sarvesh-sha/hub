import {NgModule} from "@angular/core";

import {DashboardPageComponent} from "app/dashboard/dashboard/dashboard-page.component";
import {LoginPageComponent} from "app/start/login-page.component";
import {Test} from "app/test/base-tests";
import {TestCase} from "app/test/driver";

@TestCase({
              id        : "login_success",
              name      : "Verify Login",
              categories: ["Login"]
          })
class VerifyLoginTest extends Test
{
    public async init(): Promise<void>
    {
        await this.ensureLoggedOut();
    }

    public async cleanup(): Promise<void>
    {
    }

    public async execute(): Promise<void>
    {
        const loginPage = await this.m_driver.navigate(LoginPageComponent, "/start/login");

        await this.m_driver.sendText(loginPage.test_emailInput, "email", "admin@demo.optio3.com");
        await this.m_driver.sendText(loginPage.test_passwordInput, "password", "adminPwd");
        await this.m_driver.click(loginPage.test_loginButton, "login button");

        await this.m_driver.getComponent(DashboardPageComponent);
    }
}

@NgModule({})
export class LoginPageTestsModule {}
