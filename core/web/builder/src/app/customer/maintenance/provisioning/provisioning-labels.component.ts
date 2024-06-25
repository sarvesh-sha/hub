import {Component, Injector} from "@angular/core";
import {AppService} from "app/app.service";
import * as SharedSvc from "app/services/domain/base.service";

@Component({
               templateUrl: "./provisioning-labels.component.html"
           })
export class ProvisioningLabelsComponent extends SharedSvc.BaseComponentWithRouter
{
    labels: string[][];

    constructor(inj: Injector,
                public appService: AppService)
    {
        super(inj);
    }

    protected onNavigationComplete()
    {
        // this.appService.setState("startPage", true);
        this.appService.setState("pageFullscreen", true);

        this.labels = [[]];

        let hostIds = <string>this.getQueryParameter("hostIds");
        let group   = 0;

        for (let hostId of hostIds.split(","))
        {
            let labels = this.labels[group];
            if (labels.length == 3)
            {
                labels               = [];
                this.labels[++group] = labels;
            }

            let image = this.app.domain.apis.deploymentHosts.getProvisioningLabel__generateUrl(hostId);
            labels.push(image);
        }
    }
}
