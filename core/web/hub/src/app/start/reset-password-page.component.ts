import {Component, Injector} from "@angular/core";

import {AppService, ReportError} from "app/app.service";
import {AuthorizationDef} from "app/services/domain/auth.guard";
import * as SharedSvc from "app/services/domain/base.service";
import {LogoService} from "framework/ui/logo/logo.service";

@AuthorizationDef({
                      noAuth: true
                  }) //
@Component({
               selector   : "o3-reset-password-page",
               templateUrl: "./reset-password-page.component.html",
               styleUrls  : ["./start.component.scss"]
           })
export class ResetPasswordPageComponent extends SharedSvc.BaseApplicationComponent
{
    emailAddress: string;

    token: string;

    password: string;
    passwordConfirmation: string;

    constructor(inj: Injector,
                private appService: AppService,
                public logo: LogoService)
    {
        super(inj);
    }

    ngOnInit(): void
    {
        super.ngOnInit();

        this.appService.setState("topnavTitle", "Reset Password");
        this.appService.setState("startPage", true);
        this.appService.setState("pageFullscreen", true);
    }

    @ReportError
    async resetPassword()
    {
        let user = await this.app.domain.users.resetPassword(this.emailAddress, this.token, this.password, this.passwordConfirmation);
        this.app.routing.router.navigate(["/start/login"]);
    }

    cancel()
    {
        this.app.routing.router.navigate(["/start/login"]);
    }
}
