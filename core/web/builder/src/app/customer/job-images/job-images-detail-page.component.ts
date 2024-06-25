import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";

import {RegistryImageExtended} from "app/services/domain/registry-images.service";

@Component({
               selector   : "o3-job-images-detail-page",
               templateUrl: "./job-images-detail-page.component.html",
               styleUrls  : ["./job-images-detail-page.component.scss"]
           })
export class JobImagesDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    id: string;
    extended: RegistryImageExtended;

    constructor(inj: Injector)
    {
        super(inj);

        this.extended = this.app.domain.registryImages.allocateInstance();
    }

    protected async onNavigationComplete()
    {
        this.id = this.getPathParameter("imageId");

        if (this.id)
        {
            let registryImages = this.app.domain.registryImages;

            registryImages.logger.debug(`Loading RegistryImage: ${this.id}`);
            let extended = await registryImages.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            this.extended = extended;

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.imageSha;
            registryImages.logger.debug(`Loaded RegistryImage: ${JSON.stringify(this.extended.model)}`);
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
