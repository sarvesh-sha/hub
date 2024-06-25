import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ReportError} from "app/app.service";
import {DeviceElementsDetailPageComponent} from "app/customer/device-elements/device-elements-detail-page.component";
import {AssetExtended, DeviceElementExtended, DeviceExtended, GatewayExtended, LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AlertExtended} from "app/services/domain/events.service";
import * as Models from "app/services/proxy/model/models";
import {AlertTimelineItem} from "app/shared/timelines/timeline.component";

import {UtilsService} from "framework/services/utils.service";

@Component({
               selector   : "o3-alerts-detail-page",
               templateUrl: "./alerts-detail-page.component.html",
               styleUrls  : ["./alerts-detail-page.component.scss"]
           })
export class AlertsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    alertID: string;
    alertData: AlertExtended;

    alertAsset: AssetExtended;
    alertLocation: LocationExtended;
    alertDevice: DeviceExtended;

    alertSeverityLabel: string;

    history: AlertTimelineItem[];

    occurredDate: Date;

    private extendedDescription: string;

    @ViewChild("alertForm", {static: true}) alertForm: NgForm;

    get pristine(): boolean
    {
        if (!this.alertData) return true;
        if (!this.alertForm) return true;
        if (!this.alertForm.pristine) return false;

        return UtilsService.equivalentStrings(this.extendedDescription, this.alertData.model.extendedDescription);
    }

    constructor(inj: Injector)
    {
        super(inj);

        this.alertData = this.app.domain.events.wrapTypedModel(AlertExtended, new Models.Alert());
    }

    protected onNavigationComplete()
    {
        this.alertID = this.getPathParameter("id");

        this.loadAlert();
    }

    private async loadAlert()
    {
        if (this.alertID)
        {
            this.app.domain.events.logger.info(`Loading Alert: ${this.alertID}`);
            let alertData = await this.app.domain.events.getTypedExtendedById(AlertExtended, this.alertID);

            if (!alertData)
            {
                this.exit();
                return;
            }

            await alertData.getAll();

            this.extendedDescription = alertData.model.extendedDescription;
            this.alertData           = alertData;

            this.alertAsset    = await alertData.getAsset();
            this.alertLocation = await alertData.getLocation();
            this.alertDevice   = await alertData.getDevice();

            if (!this.alertLocation && this.alertAsset instanceof DeviceExtended)
            {
                this.alertLocation = await this.alertAsset.getLocation();
            }

            let breadcrumbCurrentLabel = `${alertData.typedModel.type}`;

            this.app.ui.navigation.breadcrumbCurrentLabel = breadcrumbCurrentLabel;
            this.app.domain.events.logger.info(`Alert Loaded: ${JSON.stringify(this.alertData.model)}`);

            this.alertSeverityLabel = await this.app.domain.alerts.describeSeverity(this.alertData.typedModel.severity);

            let history  = await this.alertData.getHistory();
            this.history = AlertTimelineItem.createList(history.map((ext) => ext.model));

            let lastOccurrence = history.find((h) => h.model.type === Models.AlertEventType.reopened || h.model.type === Models.AlertEventType.created);
            this.occurredDate  = lastOccurrence?.model.createdOn || this.alertData.model.createdOn;

            //--//

            this.removeAllDbSubscriptions();

            this.subscribeOneShot(alertData,
                                  async (ext,
                                         action) =>
                                  {
                                      this.loadAlert();
                                  });

        }

        this.alertForm.form.markAsPristine();
        this.detectChanges();
    }

    canNavigateTo(): boolean
    {
        if (this.alertAsset instanceof DeviceExtended)
        {
            return true;
        }

        if (this.alertAsset instanceof DeviceElementExtended)
        {
            return true;
        }

        if (this.alertAsset instanceof GatewayExtended)
        {
            return true;
        }

        return false;
    }

    navigateTo()
    {
        if (this.alertAsset instanceof DeviceExtended)
        {
            this.app.ui.navigation.go("/alerts/alert", [
                this.alertData.model.sysId,
                "device",
                this.alertAsset.model.sysId
            ]);
            return;
        }

        if (this.alertAsset instanceof DeviceElementExtended)
        {
            DeviceElementsDetailPageComponent.navigate(this.app, this.alertAsset);
            return;
        }

        if (this.alertAsset instanceof GatewayExtended)
        {
            this.app.ui.navigation.go("/gateways/gateway", [this.alertAsset.model.sysId]);
            return;
        }
    }

    //--//

    get canMute()
    {
        if (this.alertData)
        {
            switch (this.alertData.typedModel.status)
            {
                case Models.AlertStatus.active:
                    return true;
            }
        }

        return false;
    }

    get canUnmute()
    {
        if (this.alertData)
        {
            switch (this.alertData.typedModel.status)
            {
                case Models.AlertStatus.muted:
                    return true;
            }
        }

        return false;
    }

    get canResolve()
    {
        if (this.alertData)
        {
            switch (this.alertData.typedModel.status)
            {
                case Models.AlertStatus.active:
                case Models.AlertStatus.muted:
                    return true;
            }
        }

        return false;
    }

    get canClose()
    {
        if (this.alertData)
        {
            switch (this.alertData.typedModel.status)
            {
                case Models.AlertStatus.resolved:
                    return true;
            }
        }

        return false;
    }

    async muteAlert()
    {
        this.alertData.typedModel.status = Models.AlertStatus.muted;
        await this.save();
    }

    async unmuteAlert()
    {
        this.alertData.typedModel.status = Models.AlertStatus.active;
        await this.save();
    }

    async resolveAlert()
    {
        this.alertData.typedModel.status = Models.AlertStatus.resolved;
        await this.save();
    }

    async closeAlert()
    {
        this.alertData.typedModel.status = Models.AlertStatus.closed;
        await this.save();
    }

    @ReportError
    async save()
    {
        await this.alertData.save();

        this.app.framework.errors.success("Alert updated", -1);

        await this.cancel();
    }

    async cancel()
    {
        await this.alertData.refresh();

        await this.loadAlert();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
