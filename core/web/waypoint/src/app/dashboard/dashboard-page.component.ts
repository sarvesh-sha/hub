import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import * as SharedSvc from "app/services/domain/base.service";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-dashboard-page",
               templateUrl: "./dashboard-page.component.html",
               styleUrls  : ["./dashboard-page.component.scss"]
           })
export class DashboardPageComponent extends SharedSvc.BaseApplicationComponent
{
    @ViewChild("waypointForm", {static: true}) waypointForm: NgForm;

    //--//

    checkedNetwork: boolean  = false;
    checkingNetwork: boolean = false;
    networkStatus: Models.NetworkStatus;

    checkedDestinations: boolean  = false;
    checkingDestinations: boolean = false;
    networkDestinations: NetworkDestinationResult[];

    staticIp: string = "";
    gateway: string  = "";

    constructor(inj: Injector)
    {
        super(inj);
    }

    ngAfterViewInit()
    {
        this.checkAll();

        this.loadOptions();
    }

    async checkAll()
    {
        this.checkNetwork();
        this.checkDestinations();
    }

    async checkNetwork()
    {
        this.checkingNetwork = true;

        try
        {
            this.networkStatus = await this.app.domain.apis.networks.checkStatus();
        }
        finally
        {
            this.checkedNetwork  = true;
            this.checkingNetwork = false;
            this.detectChanges();
        }
    }

    async checkDestinations()
    {
        this.checkingDestinations = true;

        try
        {
            this.networkDestinations = [];
            this.networkDestinations.push(this.checkDestination("https://builder.dev.optio3.io"));
            this.networkDestinations.push(this.checkDestination("https://www.google.com"));
            this.networkDestinations.push(this.checkDestination("https://www.bing.com"));

            for (let dest of this.networkDestinations)
            {
                await dest.promise;
            }
        }
        finally
        {
            this.checkedDestinations  = true;
            this.checkingDestinations = false;
            this.detectChanges();
        }
    }

    checkDestination(url: string): NetworkDestinationResult
    {
        let res     = new NetworkDestinationResult();
        res.url     = url;
        res.promise = this.checkDestinationInner(res);

        return res;
    }

    async checkDestinationInner(ctx: NetworkDestinationResult): Promise<void>
    {
        let req = new Models.NetworkDestinationRequest();
        req.url = ctx.url;

        let resp = await this.app.domain.apis.networks.checkDestination(req);

        // Refresh for Angular.
        this.networkDestinations = [...this.networkDestinations];

        ctx.result = resp.result;
        this.detectChanges();
    }

    async loadOptions()
    {
        let staticIp = await this.app.domain.apis.adminTasks.getOption(<any>Models.BootConfigOptions.StaticIp);
        let gateway  = await this.app.domain.apis.adminTasks.getOption(<any>Models.BootConfigOptions.Gateway);

        if (staticIp != null)
        {
            this.staticIp = staticIp.value;
            this.gateway  = gateway ? gateway.value : null;
        }
        else
        {
            this.staticIp = "";
            this.gateway  = "";
        }
    }

    async resetStaticIp()
    {
        this.waypointForm.resetForm();

        await this.loadOptions();
    }

    async setStaticIp()
    {
        await this.updateOption(Models.BootConfigOptions.StaticIp, this.staticIp);
        await this.updateOption(Models.BootConfigOptions.Gateway, this.gateway);

        await this.resetStaticIp();
    }

    async unsetStaticIp()
    {
        this.staticIp = "";
        this.gateway  = "";

        await this.setStaticIp();
    }

    async updateOption(option: Models.BootConfigOptions,
                       value: string)
    {
        if (value)
        {
            await this.app.domain.apis.adminTasks.setOption(<any>option, value);
        }
        else
        {
            await this.app.domain.apis.adminTasks.unsetOption(<any>option);
        }
    }

    public async restart()
    {
        if (await this.confirmOperation(`Are you sure you want to restart the unit?`))
        {
            this.app.domain.apis.adminTasks.reboot();
        }
    }
}

class NetworkDestinationResult
{
    public url: string;
    public result: string = "Checking...";
    public promise: Promise<void>;
}
