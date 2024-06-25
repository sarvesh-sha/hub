import {Component, Injector} from "@angular/core";

import {AppService} from "app/app.service";
import {AuthorizationDef} from "app/services/domain/auth.guard";
import * as SharedSvc from "app/services/domain/base.service";
import {LogoService} from "framework/ui/logo/logo.service";

@AuthorizationDef({
                      noAuth: true
                  }) //
@Component({
               selector   : "o3-login-page",
               templateUrl: "./login-page.component.html",
               styleUrls  : ["./start.component.scss"]
           })
export class LoginPageComponent extends SharedSvc.BaseComponentWithRouter
{
    username: string = null;
    password: string = null;
    pending: boolean = false;

    private returnUrl: string = null;

    constructor(inj: Injector,
                public appService: AppService,
                public logo: LogoService)
    {
        super(inj);
    }

    ngOnDestroy()
    {
        super.ngOnDestroy();

        this.appService.setState("startPage", false);
        this.appService.setState("pageFullscreen", false);
    }

    protected async onNavigationComplete()
    {
        this.returnUrl = this.getQueryParameter("r");

        if (this.returnUrl && this.returnUrl.toLowerCase()
                                  .startsWith("/start/login"))
        {
            this.returnUrl = null;
        }

        this.appService.setState("topnavTitle", "Log In");
        this.appService.setState("startPage", true);
        this.appService.setState("pageFullscreen", true);

        // check if we are already authenticated
        // (this handles cases where the user has the login page bookmarked or is using a link to it - but still has valid login)
        if (this.app.domain.users.isAuthenticated || this.app.domain.users.wasPreviouslyAuthenticated())
        {

            // if so, verify our session is still valid, and if
            // so, simply proceed through to the logged in view
            try
            {
                await this.app.domain.users.checkLoggedIn();
                if (this.returnUrl)
                {
                    let parts  = this.returnUrl.split(";");
                    let params = this.parseParams(this.returnUrl);
                    this.app.routing.router.navigate([
                                                         parts[0],
                                                         params
                                                     ]);
                }
                else
                {
                    this.app.routing.router.navigate(["./home"]);
                }
            }
            catch (error)
            {
                /* does nothing */
            }
        }
    }

    async signin()
    {
        if (!this.pending)
        {
            try
            {
                this.pending = true;
                await this.app.domain.users.login(this.username, this.password);
                if (this.returnUrl && this.returnUrl.length > 2)
                {
                    let parts  = this.returnUrl.split(";");
                    let params = this.parseParams(this.returnUrl);
                    this.app.routing.router.navigate([
                                                         parts[0],
                                                         params
                                                     ]);
                }
                else
                {
                    this.app.routing.router.navigate(["./home"]);
                }

            }
            catch (error)
            {
                /* does nothing */
                console.error(`Couldn't login due to ${error}`);
            }
            finally
            {
                this.pending = false;
            }
        }
    }

    parseParams(url: string): any
    {
        let parts                             = this.returnUrl.split(";");
        let params: { [key: string]: string } = {};

        if (parts.length > 1)
        {
            for (let p = 0; p < parts.length; p++)
            {
                if (p != 0)
                {
                    let part       = parts[p];
                    let paramParts = part.split("=");
                    if (paramParts.length == 2)
                    {
                        params[paramParts[0]] = paramParts[1];
                    }
                }
            }
        }

        return params;
    }

    forgotPassword()
    {
        this.app.framework.errors.dismiss();
        this.app.routing.router.navigate(["./start/forgotpassword"]);
    }

    resetPassword()
    {
        this.app.framework.errors.dismiss();
        this.app.routing.router.navigate(["./start/resetpassword"]);
    }
}
