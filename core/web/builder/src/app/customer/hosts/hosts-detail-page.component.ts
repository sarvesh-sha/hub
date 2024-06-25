import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ReportError} from "app/app.service";

import * as SharedSvc from "app/services/domain/base.service";
import {HostExtended} from "app/services/domain/hosts.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-hosts-detail-page",
               templateUrl: "./hosts-detail-page.component.html",
               styleUrls  : ["./hosts-detail-page.component.scss"]
           })
export class HostsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    hostId: string;

    host: HostExtended;

    //--//

    @ViewChild("hostForm", {static: true}) hostForm: NgForm;

    constructor(inj: Injector)
    {
        super(inj);

        this.host = this.app.domain.hosts.wrapModel(new Models.Host());
    }

    protected async onNavigationComplete()
    {
        this.hostId = this.getPathParameter("id");

        if (this.hostId)
        {
            await this.loadHost();
        }
    }

    async loadHost()
    {
        // load host info
        this.host = await this.app.domain.hosts.getExtendedById(this.hostId);
        if (!this.host)
        {
            this.exit();
            return;
        }

        // set breadcrumbs
        let model                                     = this.host.model;
        this.app.ui.navigation.breadcrumbCurrentLabel = model.domainName;

        //--//

    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    showLog()
    {
        this.app.ui.navigation.go("/hosts/host", [
            this.hostId,
            "log"
        ]);
    }
}
