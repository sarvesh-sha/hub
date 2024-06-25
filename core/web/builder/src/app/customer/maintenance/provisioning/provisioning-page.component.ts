import {Component, Injector, ViewChild} from "@angular/core";

import {AuthorizationDef} from "app/services/domain/auth.guard";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";

@AuthorizationDef({
                      noAuth: true
                  }) //
@Component({
               templateUrl: "./provisioning-page.component.html"
           })
export class ProvisioningPageComponent extends SharedSvc.BaseComponentWithRouter
{
    hostId: string;
    hostDisplay: string;
    alreadyShipped: boolean                = true;
    alreadyAssociatedWithCustomer: boolean = true;

    customerInfo: string;
    noteText: string;

    fullyInstalled: boolean;
    sensorsVerified: boolean;
    notesUploaded: boolean;

    @ViewChild("confirmReady") confirmReady: OverlayComponent;

    constructor(inj: Injector)
    {
        super(inj);
    }

    protected onNavigationComplete()
    {
        this.hostId = this.getQueryParameter("hostId");

        this.checkStatus();
    }

    private async checkStatus()
    {
        let status = await this.app.domain.apis.deploymentHostProvisioning.checkStatus(this.hostId);

        this.alreadyShipped                = status.alreadyShipped;
        this.alreadyAssociatedWithCustomer = status.alreadyAssociatedWithCustomer;
        if (!this.alreadyAssociatedWithCustomer)
        {
            if (status.recentlyOnline)
            {
                this.hostDisplay = `${this.hostId} was recently online!`;
            }
            else
            {
                this.hostDisplay = `${this.hostId} was not online in the last 30 minutes...`;

                setTimeout(() => this.checkStatus(), 10000);
            }
        }
        else
        {
            this.hostDisplay = `${this.hostId} already associated with a customer!`;
        }
    }

    async uploadNotes()
    {
        let notes          = new Models.DeploymentHostProvisioningNotes();
        notes.customerInfo = this.customerInfo;
        notes.text         = this.noteText;

        await this.app.domain.apis.deploymentHostProvisioning.addNotes(this.hostId, notes);

        this.app.framework.errors.success("Uploaded notes!", -1);

        this.noteText = null;
    }

    async markAsReady()
    {
        let notes                = new Models.DeploymentHostProvisioningNotes();
        notes.customerInfo       = this.customerInfo;
        notes.text               = this.noteText;
        notes.readyForProduction = true;

        await this.app.domain.apis.deploymentHostProvisioning.addNotes(this.hostId, notes);

        this.app.framework.errors.success("Marked unit as ready for production!", -1);

        this.noteText = null;

        await this.checkStatus();
    }

    async markAsShipping()
    {
        let notes              = new Models.DeploymentHostProvisioningNotes();
        notes.customerInfo     = this.customerInfo;
        notes.text             = this.noteText;
        notes.readyForShipping = true;

        await this.app.domain.apis.deploymentHostProvisioning.addNotes(this.hostId, notes);

        this.app.framework.errors.success("Marked unit as ready for shipping!", -1);

        this.noteText = null;

        await this.checkStatus();
    }
}
