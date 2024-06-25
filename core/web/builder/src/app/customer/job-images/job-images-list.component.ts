import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {JobExtended} from "app/services/domain/jobs.service";
import {RegistryImageDependencies} from "app/services/domain/registry-images.service";
import {RegistryTaggedImageExtended} from "app/services/domain/registry-tagged-images.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {inParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-job-images-list",
               templateUrl: "./job-images-list.component.html"
           })
export class JobImagesListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, RegistryTaggedImageExtended, ImageDecoded>
{
    private m_extended: JobExtended;

    public get extended(): JobExtended
    {
        return this.m_extended;
    }

    @Input()
    public set extended(value: JobExtended)
    {
        this.m_extended = value;
        this.table.refreshData();
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, RegistryTaggedImageExtended, ImageDecoded>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.registryTaggedImages, this);
    }

    getItemName(): string
    {
        return "Job Images";
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        if (!this.extended || !this.extended.model.generatedImages)
        {
            return [];
        }

        return this.extended.model.generatedImages;
    }

    getPage(offset: number,
            limit: number): Promise<RegistryTaggedImageExtended[]>
    {
        return this.app.domain.registryTaggedImages.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: RegistryTaggedImageExtended[]): Promise<ImageDecoded[]>
    {
        let results = rows.map((row) =>
                               {
                                   let res      = new ImageDecoded();
                                   res.extended = row;
                                   return res;
                               });

        await inParallel(results, (result) => this.fetchHash(result));

        return results;
    }

    async fetchHash(res: ImageDecoded): Promise<void>
    {
        let ext = res.extended;

        res.createdOn     = ext.model.createdOn;
        res.tag           = ext.model.tag;
        res.releaseStatus = ext.model.releaseStatus;

        let image = await ext.getImage();

        let item = new RegistryImageDependencies();
        await item.init(ext);

        for (let i = 0; i < item.usages.entries.length; i++)
        {
            let detail = item.usages.entries[i];
            if (detail.context instanceof JobExtended && this.extended.sameIdentity(detail.context))
            {
                item.usages.entries.splice(i--, 1);
            }
        }

        res.dependencies = item;

        res.imageSha      = image.model.imageSha;
        res.architecture  = image.model.architecture;
        res.targetService = image.getTargetService();
    }

    itemClicked(columnId: string,
                item: ImageDecoded)
    {
        this.app.ui.navigation.push([
                                        "image",
                                        item.extended.model.sysId
                                    ]);
    }
}

class ImageDecoded
{
    extended: RegistryTaggedImageExtended;

    dependencies: RegistryImageDependencies;

    createdOn: Date;

    imageSha: string;

    tag: string;

    architecture: Models.DockerImageArchitecture;

    targetService: Models.DeploymentRole;
    releaseStatus: Models.RegistryImageReleaseStatus;
}

