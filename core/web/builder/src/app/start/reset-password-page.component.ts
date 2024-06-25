import {Component, Injector} from "@angular/core";
import {Params} from "@angular/router";

import {AppService, ReportError} from "app/app.service";
import * as SharedSvc from "app/services/domain/base.service";
import {LogoService} from "framework/ui/logo/logo.service";

@Component({
               selector   : "o3-reset-password-page",
               templateUrl: "./reset-password-page.component.html",
               styleUrls  : ["./start.component.scss"]
           })
export class ResetPasswordPageComponent extends SharedSvc.BaseComponentWithRouter
{
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

    protected onNavigationComplete()
    {
        this.token = this.getPathParameter("token");
    }

    @ReportError
    async resetPassword()
    {
        let user = await this.app.domain.users.resetPassword(this.token, this.password, this.passwordConfirmation);
        this.app.routing.router.navigate(["./start/login"]);
    }

    cancel()
    {
        this.app.routing.router.navigate(["./start/login"]);
    }
}
