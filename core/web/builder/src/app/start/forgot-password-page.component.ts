import {Component, Injector} from "@angular/core";

import {AppService} from "app/app.service";
import {AuthorizationDef} from "app/services/domain/auth.guard";
import * as SharedSvc from "app/services/domain/base.service";
import {LogoService} from "framework/ui/logo/logo.service";

@AuthorizationDef({
                      noAuth: true
                  }) //
@Component({
               selector   : "o3-forgot-password-page",
               templateUrl: "./forgot-password-page.component.html",
               styleUrls  : ["./start.component.scss"]
           })
export class ForgotPasswordPageComponent extends SharedSvc.BaseApplicationComponent
{
    emailAddress: string;

    constructor(inj: Injector,
                private appService: AppService,
                public logo: LogoService)
    {
        super(inj);
    }

    ngOnInit()
    {
        super.ngOnInit();

        this.appService.setState("topnavTitle", "Forgot Password");
        this.appService.setState("startPage", true);
        this.appService.setState("pageFullscreen", true);
    }

    async forgotPassword()
    {
        try
        {
            let user = await this.app.domain.users.forgotPassword(this.emailAddress);
            this.app.routing.navigate(["./start/login"]);
        }
        catch (error)
        {
            /* does nothing */
        }
    }

    cancel()
    {
        this.app.routing.navigate(["./start/login"]);
    }
}
