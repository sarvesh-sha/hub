import {Component, Injector} from "@angular/core";

import {ReportError} from "app/app.service";
import {CustomerServiceSelectionDialogComponent} from "app/customer/customer-services/customer-service-selection-dialog.component";
import {RegistryImageSelectionDialogComponent} from "app/customer/registry-images/registry-image-selection-dialog.component";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceBackupExtended} from "app/services/domain/customer-service-backups.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {RegistryTaggedImageExtended} from "app/services/domain/registry-tagged-images.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-customer-service-backups-detail-page",
               templateUrl: "./customer-service-backups-detail-page.component.html",
               styleUrls  : ["./customer-service-backups-detail-page.component.scss"]
           })
export class ConsumerServiceBackupsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    id: string;
    extended: CustomerServiceBackupExtended;
    enableHostMigration: boolean;

    images: { [key: string]: RegistryTaggedImageExtended } = {};

    constructor(inj: Injector)
    {
        super(inj);

        this.extended = this.app.domain.customerServiceBackups.allocateInstance();
    }

    protected async onNavigationComplete()
    {
        this.id = this.getPathParameter("backupId");

        this.loadData();
    }

    //--//

    private async loadData()
    {
        if (this.id)
        {
            this.app.domain.customerServiceBackups.logger.debug(`Loading Task: ${this.id}`);
            let extended = await this.app.domain.customerServiceBackups.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.extended            = extended;
            this.enableHostMigration = extended.model.fileId && extended.model.trigger == Models.BackupKind.HostMigration;

            let images: { [key: string]: RegistryTaggedImageExtended } = {};
            for (let imageSpec of this.extended.model.roleImages)
            {
                images[imageSpec.image.sysId] = await this.extended.getImage(imageSpec.role, imageSpec.architecture);
            }
            this.images = images;

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.fileId;
            this.app.domain.customerServiceBackups.logger.debug(`Loaded Task: ${JSON.stringify(this.extended.model)}`);

            //--//

            this.removeAllDbSubscriptions();

            this.subscribeOneShot(extended,
                                  async (ext,
                                         action) =>
                                  {
                                      this.loadData();
                                  });
        }
    }

    //--//

    @ReportError
    async rollback()
    {
        await this.extended.rollbackToService(this);
    }

    @ReportError
    async rollbackForeign()
    {
        let dialogService = await CustomerServiceSelectionDialogComponent.open(this, "rollback to this backup", "Select", Models.DeploymentOperationalStatus.idle);
        if (dialogService == null)
        {
            return;
        }

        let svc = dialogService.svc;
        if (svc == null)
        {
            return;
        }

        await this.extended.rollbackToService(this, svc);
    }

    @ReportError
    async rollbackAndUpgrade()
    {
        await this.extended.rollbackAndUpgradeToService(this);
    }

    @ReportError
    async rollbackAndUpgradeForeign()
    {
        let dialogService = await CustomerServiceSelectionDialogComponent.open(this, "rollback to this backup", "Select", Models.DeploymentOperationalStatus.idle);
        if (dialogService == null)
        {
            return;
        }

        let svc = dialogService.svc;
        if (svc == null)
        {
            return;
        }

        await this.extended.rollbackAndUpgradeToService(this, svc);
    }

    @ReportError
    async hostMigration()
    {
        if (await this.confirmOperation("Click Yes to confirm rollback for host migration."))
        {
            let svc = await this.extended.getOwningCustomerService();

            let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                  roles     : [],
                                                                                  fromBackup: this.extended.getIdentity()
                                                                              });

            for (let imageSpec of this.extended.model.roleImages)
            {
                let image    = await this.extended.getImage(imageSpec.role, imageSpec.architecture);
                let roleSpec = svc.addImageToDesiredState(desiredState, imageSpec.role, imageSpec.architecture, image);

                if (imageSpec.role == Models.DeploymentRole.gateway)
                {
                    roleSpec.shutdownIfDifferent = true;
                    roleSpec.launchIfMissing     = true;
                }
                else
                {
                    roleSpec.shutdown = true;
                    roleSpec.launch   = true;
                }
            }

            await svc.applyDesiredState(desiredState);

            let cust = await svc.getOwningCustomer();

            this.app.ui.navigation.go("/customers/item", [
                cust.model.sysId,
                "service",
                svc.model.sysId
            ]);
        }
    }

    download()
    {
        window.open(this.extended.getUrlForDownload(), "_blank");
    }

    @ReportError
    async remove()
    {
        if (await this.confirmOperation("Click Yes to confirm deletion of this backup."))
        {
            this.removeAllDbSubscriptions();

            await this.extended.remove();

            this.exit();
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
