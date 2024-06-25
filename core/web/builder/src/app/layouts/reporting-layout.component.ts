import {Component, OnInit, ViewEncapsulation} from "@angular/core";
import {ActivatedRouteSnapshot, Router} from "@angular/router";

import {AppService} from "app/app.service";
import {UsersService} from "app/services/domain/users.service";

@Component({
               selector     : "o3-reporting-layout",
               templateUrl  : "./reporting-layout.component.html",
               styleUrls    : ["./reporting-layout.component.scss"],
               encapsulation: ViewEncapsulation.None
           })
export class ReportingLayoutComponent implements OnInit
{

    width: number;

    constructor(private appService: AppService,
                private router: Router,
                private userService: UsersService)
    {
    }

    ngOnInit()
    {
        this.findWidth(this.router.routerState.snapshot.root);
    }

    private findWidth(route: ActivatedRouteSnapshot): boolean
    {
        let width = route.params["sys_width"];
        if (width)
        {
            this.width = +width;
            return true;
        }

        for (let child of route.children)
        {
            if (this.findWidth(child))
            {
                return true;
            }
        }

        return false;
    }
}
