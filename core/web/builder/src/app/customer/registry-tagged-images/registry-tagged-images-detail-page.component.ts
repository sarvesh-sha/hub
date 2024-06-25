import {Component, Injector} from "@angular/core";
import {ReportError} from "app/app.service";

import * as SharedSvc from "app/services/domain/base.service";

import {RegistryImageDependencies, RegistryImageExtended} from "app/services/domain/registry-images.service";
import {RegistryTaggedImageExtended} from "app/services/domain/registry-tagged-images.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-registry-tagged-images-detail-page",
               templateUrl: "./registry-tagged-images-detail-page.component.html",
               styleUrls  : ["./registry-tagged-images-detail-page.component.scss"]
           })
export class RegistryTaggedImagesDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    id: string;
    extended: RegistryTaggedImageExtended;
    image: RegistryImageExtended;
    imageDependendies: RegistryImageDependencies;
    canMarkAsReleaseCandidate = false;
    canMarkAsRelease          = false;

    modelLabelsKeys: string[] = [];
    modelLabels: { [key: string]: LabelDetails; };

    constructor(inj: Injector)
    {
        super(inj);

        this.extended = this.app.domain.registryTaggedImages.allocateInstance();
    }

    protected async onNavigationComplete()
    {
        this.id = this.getPathParameter("imageId");

        if (this.id)
        {
            this.app.domain.registryTaggedImages.logger.debug(`Loading RegistryTaggedImage: ${this.id}`);
            let extended = await this.app.domain.registryTaggedImages.getExtendedById(this.id);
            if (!extended)
            {
                this.exit();
                return;
            }

            await this.setExtended(extended);

            this.app.ui.navigation.breadcrumbCurrentLabel = extended.model.tag;
            this.app.domain.registryTaggedImages.logger.debug(`Loaded RegistryTaggedImage: ${JSON.stringify(this.extended.model)}`);
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    @ReportError
    async markAsReleaseCandidate()
    {
        await this.extended.markForRelease(Models.RegistryImageReleaseStatus.ReleaseCandidate);
    }

    @ReportError
    async markAsRelease()
    {
        await this.extended.markForRelease(Models.RegistryImageReleaseStatus.Release);
    }

    @ReportError
    async distribute(operational: boolean)
    {
        let count = await this.extended.distribute(operational ? Models.DeploymentOperationalStatus.operational : Models.DeploymentOperationalStatus.idle);

        this.app.framework.errors.success(`Starting ${count} downloads...`, -1);
    }

    //--//

    private async setExtended(ext: RegistryTaggedImageExtended)
    {
        this.extended                  = ext;
        this.canMarkAsReleaseCandidate = false;
        this.canMarkAsRelease          = false;

        let keys: string[]                           = [];
        let labels: { [key: string]: LabelDetails; } = {};

        this.image = await this.extended.getImage();
        if (this.image)
        {
            for (let key in this.image.model.labels)
            {
                let details = new LabelDetails();

                keys.push(key);

                let value = this.image.model.labels[key];
                switch (key)
                {
                    case "Optio3_ConfigTemplate":
                        value = window.atob(value);
                        break;
                }

                details.labelLong = value;
                if (value.length > 40)
                {
                    details.labelShort = value.substr(0, 40) + " ...";
                }
                else
                {
                    details.labelShort = value;
                }

                labels[key] = details;
            }

            if (this.image.getTargetService())
            {
                let job = await this.extended.getOwingJob();
                if (!job)
                {
                    // If it doesn't have a job, we can manually mark it as release.
                    this.canMarkAsReleaseCandidate = this.extended.model.releaseStatus != Models.RegistryImageReleaseStatus.ReleaseCandidate;
                    this.canMarkAsRelease          = this.extended.model.releaseStatus != Models.RegistryImageReleaseStatus.Release;
                }
            }
        }

        let item = new RegistryImageDependencies();
        await item.init(ext);

        this.imageDependendies = item;

        this.modelLabelsKeys = keys;
        this.modelLabels     = labels;
    }
}

class LabelDetails
{
    labelShort: string;
    labelLong: string;
}
