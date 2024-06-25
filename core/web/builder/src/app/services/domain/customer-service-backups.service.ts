import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";

import {RegistryImageSelectionDialogComponent} from "app/customer/registry-images/registry-image-selection-dialog.component";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {RegistryTaggedImageExtended, ReleaseStatusDetails} from "app/services/domain/registry-tagged-images.service";
import * as Models from "app/services/proxy/model/models";

import {CacheService} from "framework/services/cache.service";
import {ErrorService} from "framework/services/error.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class CustomerServiceBackupsService extends SharedSvc.BaseService<Models.CustomerServiceBackup, CustomerServiceBackupExtended>
{
    constructor(api: ApiService,
                errors: ErrorService,
                cache: CacheService)
    {
        super(api, errors, cache, Models.CustomerServiceBackup, CustomerServiceBackupExtended.newInstance);
    }

    //--//

    protected cachePrefix(): string
    {
        return Models.CustomerServiceBackup.RECORD_IDENTITY;
    }

    protected getRaw(id: string): Promise<Models.CustomerServiceBackup>
    {
        return this.api.customerServiceBackups.get(id);
    }

    protected getBatchRaw(ids: string[]): Promise<Models.CustomerServiceBackup[]>
    {
        return this.api.customerServiceBackups.getBatch(ids);
    }
}

export class CustomerServiceBackupExtended extends SharedSvc.ExtendedModel<Models.CustomerServiceBackup>
{
    static newInstance(svc: CustomerServiceBackupsService,
                       model: Models.CustomerServiceBackup): CustomerServiceBackupExtended
    {
        return new CustomerServiceBackupExtended(svc, model, Models.CustomerServiceBackup.RECORD_IDENTITY);
    }

    public static newIdentity(sysId?: string): Models.RecordIdentity
    {
        return Models.RecordIdentity.newInstance({
                                                     table: Models.CustomerServiceBackup.RECORD_IDENTITY,
                                                     sysId: sysId
                                                 });
    }

    //--//

    @Memoizer
    public getOwningCustomerService(): Promise<CustomerServiceExtended>
    {
        return this.domain.customerServices.getExtendedByIdentity(this.model.customerService);
    }

    get fileDescription(): string
    {
        return this.model.pendingTransfer ? `${this.model.fileId} (upload pending)` : this.model.fileId;
    }

    findImageSpec(role: Models.DeploymentRole,
                  architecture: Models.DockerImageArchitecture): Models.RoleAndArchitectureWithImage
    {
        for (let imageSpec of this.model.roleImages || [])
        {
            if (imageSpec.role == role && imageSpec.architecture == architecture)
            {
                return imageSpec;
            }
        }

        return null;
    }

    async getImage(role: Models.DeploymentRole,
                   architecture: Models.DockerImageArchitecture): Promise<RegistryTaggedImageExtended>
    {
        let imageSpec = this.findImageSpec(role, architecture);
        if (!imageSpec)
        {
            return null;
        }

        return await this.domain.registryTaggedImages.getExtendedByIdentity(imageSpec.image);
    }

    //--//

    async checkRemove(): Promise<Models.ValidationResult[]>
    {
        let result = await this.domain.apis.customerServiceBackups.remove(this.model.sysId, true);
        return result && result.entries ? result.entries : [];
    }

    @ReportError
    async remove(): Promise<Models.ValidationResults>
    {
        await this.flush();
        return this.domain.apis.customerServiceBackups.remove(this.model.sysId);
    }

    //--//

    getUrlForDownload(): string
    {
        return this.domain.apis.customerServiceBackups.stream__generateUrl(this.model.sysId, this.model.fileId + ".tgz");
    }

    //--//

    async rollbackToService(comp: BaseApplicationComponent,
                            svc?: CustomerServiceExtended)
    {
        let ownSvc    = await this.getOwningCustomerService();
        let targetSvc = svc || ownSvc;

        if (await comp.confirmOperation(`Click Yes to confirm rollback of service '${targetSvc.model.name}'.`))
        {
            if (!await targetSvc.isReadyForChange())
            {
                comp.app.framework.errors.warn(`Can't update '${targetSvc.model.name}', currently busy...`);
                return;
            }

            let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                                  roles     : [],
                                                                                  fromBackup: this.getIdentity()
                                                                              });

            for (let imageSpec of this.model.roleImages)
            {
                let image         = await this.getImage(imageSpec.role, imageSpec.architecture);
                let roleSpec      = targetSvc.addImageToDesiredState(desiredState, imageSpec.role, imageSpec.architecture, image);
                roleSpec.shutdown = true;
                roleSpec.launch   = true;
            }

            await targetSvc.applyDesiredState(desiredState);

            let cust = await targetSvc.getOwningCustomer();

            comp.app.ui.navigation.go("/customers/item", [
                cust.model.sysId,
                "service",
                targetSvc.model.sysId
            ]);
        }
    }

    async rollbackAndUpgradeToService(comp: BaseApplicationComponent,
                                      svc?: CustomerServiceExtended,
                                      images?: ReleaseStatusDetails[])
    {
        let ownSvc    = await this.getOwningCustomerService();
        let targetSvc = svc || ownSvc;

        let state = await targetSvc.getState();

        let desiredState = Models.CustomerServiceDesiredState.newInstance({
                                                                              roles     : [],
                                                                              fromBackup: this.getIdentity()
                                                                          });

        if (!await state.enumerateHostsAsync(false,
                                             async (role,
                                                    host) =>
                                             {
                                                 let arch = host.architecture;
                                                 if (!targetSvc.findDesiredState(desiredState, role, arch))
                                                 {
                                                     let image: RegistryTaggedImageExtended;

                                                     for (let imageDetails of images || [])
                                                     {
                                                         let targetRelease = imageDetails?.findMatch(role, arch);
                                                         image             = targetRelease?.image;
                                                         if (image)
                                                         {
                                                             break;
                                                         }
                                                     }

                                                     if (!image)
                                                     {
                                                         let dialogRes = await RegistryImageSelectionDialogComponent.open(comp, arch, [role], `deploy for ${role}`, "Deploy");
                                                         image         = dialogRes?.taggedImage;
                                                         if (!image) return false;
                                                     }

                                                     let roleSpec      = targetSvc.addImageToDesiredState(desiredState, role, arch, image);
                                                     roleSpec.shutdown = true;
                                                     roleSpec.launch   = true;
                                                 }

                                                 return true;
                                             }))
        {
            return;
        }

        if (await comp.confirmOperation(`Click Yes to confirm rollback and upgrade of service '${targetSvc.model.name}'.`))
        {
            await targetSvc.applyDesiredState(desiredState);

            let cust = await targetSvc.getOwningCustomer();

            comp.app.ui.navigation.go("/customers/item", [
                cust.model.sysId,
                "service",
                targetSvc.model.sysId
            ]);
        }
    }
}

export type CustomerServiceBackupChangeSubscription = SharedSvc.DbChangeSubscription<Models.CustomerServiceBackup>;
