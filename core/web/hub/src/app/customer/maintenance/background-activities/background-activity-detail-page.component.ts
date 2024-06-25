import {Component, Injector} from "@angular/core";

import {BackgroundActivityExtended} from "app/services/domain/background-activities.service";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-background-activity-detail-page",
               templateUrl: "./background-activity-detail-page.component.html",
               styleUrls  : ["./background-activity-detail-page.component.scss"]
           })
export class BackgroundActivityDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    activityId: string;
    activity: BackgroundActivityExtended;
    activityDetails: string = "<loading...>";

    activityRemoveChecks: Models.ValidationResult[];
    activityNoRemoveReason: string;

    hasLink: boolean;

    constructor(inj: Injector)
    {
        super(inj);

        this.activity = this.app.domain.backgroundActivities.allocateInstance();
    }

    protected onNavigationComplete()
    {
        this.activityId = this.getPathParameter("id");

        this.refresh();
    }

    async refresh()
    {
        if (this.activityId)
        {
            this.app.domain.backgroundActivities.logger.debug(`Loading Activity: ${this.activityId}`);
            let extended = await this.app.domain.backgroundActivities.getExtendedById(this.activityId);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.activity = extended;

            this.activityRemoveChecks   = await this.activity.checkRemove();
            this.activityNoRemoveReason = this.fromValidationToReason("Remove is disabled because:", this.activityRemoveChecks);

            this.hasLink = this.activity.model.context != null;

            let value            = await extended.activityDetails();
            this.activityDetails = JSON.stringify(value, null, 4);

            // this.appService.navigation.breadcrumbCurrentLabel = extended.model.name;
            this.app.domain.backgroundActivities.logger.debug(`Loaded Activity: ${JSON.stringify(this.activity.model)}`);

            //--//

            this.removeAllDbSubscriptions();

            this.subscribeOneShot(extended, () => this.refresh());
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    async cancel()
    {
        this.activity = await this.activity.cancel();
    }

    async remove()
    {
        await this.activity.remove();
    }

    get hasFailed(): boolean
    {
        if (this.activity.model.status == Models.BackgroundActivityStatus.FAILED) return true;
        return false;
    }

    get isRunning(): boolean
    {
        return this.activity && !this.activity.isDone();
    }

    async navigateToContext()
    {
        let context = this.activity.model.context;
        switch (context?.table)
        {
            case Models.GatewayAsset.RECORD_IDENTITY:
                this.app.ui.navigation.go("/gateways/gateway", [
                    context.sysId
                ]);
                break;
        }
    }
}
