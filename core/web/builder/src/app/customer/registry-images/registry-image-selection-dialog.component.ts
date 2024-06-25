import {Component, Inject, Injector} from "@angular/core";

import {AppDomainContext} from "app/services/domain";
import {JobExtended} from "app/services/domain/jobs.service";
import {RegistryImageExtended} from "app/services/domain/registry-images.service";
import {RegistryTaggedImageExtended} from "app/services/domain/registry-tagged-images.service";

import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";

import {BaseDialogComponentSingleSelect, BaseDialogConfig, BaseDialogSelection} from "framework/ui/dialogs/base.dialog";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {inParallel} from "framework/utils/concurrency";

@Component({
               templateUrl: "./registry-image-selection-dialog.component.html"
           })
export class RegistryImageSelectionDialogComponent extends BaseDialogComponentSingleSelect<RegistryImageSelectionDialogComponent, ImageDescriptor>
{
    private domain: AppDomainContext;

    refreshing: boolean;
    refreshed: boolean;

    constructor(dialogRef: OverlayDialogRef<ImageDescriptor>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: DialogConfig)
    {
        super(dialogRef, inj);

        this.domain = this.inject(AppDomainContext);
    }

    public static open(comp: BaseComponent,
                       architecture: Models.DockerImageArchitecture,
                       purposes: Models.DeploymentRole[],
                       purposeText: string,
                       okButton: string): Promise<ImageDescriptor>
    {
        let cfg            = new DialogConfig();
        cfg.architecture   = architecture;
        cfg.purposes       = purposes;
        cfg.dialogPurpose  = purposeText;
        cfg.dialogOkButton = okButton;

        return BaseDialogComponentSingleSelect.openInner(comp, RegistryImageSelectionDialogComponent, cfg, false, "60%");
    }

    protected async loadItems()
    {
        let newItems: ImageDescriptor[] = [];

        let images = await this.domain.registryImages.getExtendedAll();
        await inParallel(images,
                         async (image,
                                index) =>
                         {
                             if (!image.isCompatibleWithTarget(this.data.architecture))
                             {
                                 return;
                             }

                             let service = image.getTargetService();

                             if (this.data.purposes)
                             {
                                 if (!service)
                                 {
                                     return;
                                 }

                                 if (this.data.purposes.indexOf(service) < 0)
                                 {
                                     return;
                                 }
                             }

                             let taggedImages = await image.getReferencingTags();
                             for (let taggedImage of taggedImages)
                             {
                                 if (taggedImage.model.tag.endsWith(":latest") && taggedImages.length > 1)
                                 {
                                     // Skip the "latest" tag if there are multiple tags pointing to the same image.
                                     return;
                                 }

                                 let item         = new ImageDescriptor();
                                 item.job         = await taggedImage.getOwingJob();
                                 item.image       = image;
                                 item.taggedImage = taggedImage;
                                 item.service     = service;
                                 newItems.push(item);
                             }
                         });

        if (newItems.length > 0)
        {
            for (let item of newItems) this.addNewItem(item);

            this.sortItems((l,
                            r) =>
                           {
                               let lReleaseOrder = l.taggedImage.releaseOrder;
                               let rReleaseOrder = r.taggedImage.releaseOrder;

                               if (lReleaseOrder != rReleaseOrder)
                               {
                                   return lReleaseOrder - rReleaseOrder;
                               }

                               let lMoment = MomentHelper.parse(l.job ? l.job.model.createdOn : l.taggedImage.model.createdOn);
                               let rMoment = MomentHelper.parse(r.job ? r.job.model.createdOn : r.taggedImage.model.createdOn);

                               if (lMoment.isAfter(rMoment))
                               {
                                   return -1;
                               }

                               if (rMoment.isAfter(lMoment))
                               {
                                   return 1;
                               }

                               let diff = UtilsService.compareStrings(l.serviceText, r.serviceText, true);
                               if (diff == 0)
                               {
                                   diff = UtilsService.compareStrings(l.taggedImage.model.tag, r.taggedImage.model.tag, true);
                               }

                               return diff;
                           });
        }
    }

    async startRefresh()
    {
        this.refreshed  = true;
        this.refreshing = true;
        this.detectChanges();

        try
        {
            let status = await this.domain.registryImages.refresh();
            if (status.tagsAdded || status.tagsRemoved)
            {
                this.loadData();
            }
        }
        finally
        {
            this.refreshing = false;
            this.detectChanges();
        }
    }

    protected async onEmptyFilterResults()
    {
    }

    protected shouldDisplay(pattern: string,
                            item: ImageDescriptor): boolean
    {
        if (this.containsPattern(pattern, <any>item.service)) return true;

        if (item.job)
        {
            if (this.containsPattern(pattern, item.job.model.name)) return true;
        }
        else
        {
            if (this.containsPattern(pattern, item.taggedImage.model.tag)) return true;
        }

        return false;
    }

}

class DialogConfig extends BaseDialogConfig
{
    architecture: Models.DockerImageArchitecture;

    purposes: Models.DeploymentRole[];
}

export class ImageDescriptor extends BaseDialogSelection
{
    job: JobExtended;

    taggedImage: RegistryTaggedImageExtended;

    image: RegistryImageExtended;

    service: Models.DeploymentRole;

    get serviceText(): string
    {
        return this.service ? <any>this.service : "<no service>";
    }

    get jobText(): string
    {
        return this.job?.model?.name || "<no job info>";
    }
}
