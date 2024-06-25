import {Component, OnInit, ViewChild, ViewEncapsulation} from "@angular/core";

import {StandardLayoutComponent} from "app/layouts/standard-layout.component";

@Component({
               selector     : "o3-app",
               templateUrl  : "./app.component.html",
               encapsulation: ViewEncapsulation.None
           })
export class AppComponent implements OnInit
{
    @ViewChild("test_standardLayout") standardLayout: StandardLayoutComponent;

    isStandardLayout: boolean;
    isReportingLayout: boolean;

    ngOnInit()
    {
        // if the url contains ";sys_asreport=true", use reporting layout
        // (Note the "bad practice" of directly using the window object. Since this is an isolated case of a component that could not
        // be re-used outside the web, it was determined that for simplicity, to just assume availability of this global variable. Further
        // there is no way to access the URL using Ng services/providers this early in the lifecycle)
        if (window?.location.href.indexOf(";sys_asreport=true") > 0)
        {
            this.isReportingLayout = true;
        }
        else if (window?.location.href.indexOf(";sys_forprint=true") > 0)
        {
            this.isReportingLayout = true;
        }
        else
        {
            this.isStandardLayout = true;
        }
    }
}
