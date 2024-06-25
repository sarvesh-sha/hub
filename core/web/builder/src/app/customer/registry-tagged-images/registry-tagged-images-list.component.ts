import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {RegistryImageDependencies} from "app/services/domain/registry-images.service";
import {RegistryTaggedImageExtended} from "app/services/domain/registry-tagged-images.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {DatatableManager, IDatatableDataProvider, SimpleSelectionManager} from "framework/ui/datatables/datatable-manager";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {inParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-registry-tagged-images-list",
               templateUrl: "./registry-tagged-images-list.component.html"
           })
export class RegistryTaggedImagesListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<RegistryImageDependencies, RegistryTaggedImageExtended, RegistryImageDependencies>
{
    @Input() set filter(value: string)
    {
        this.filterImages = value;
        this.table.refreshData();
    }

    table: DatatableManager<RegistryImageDependencies, RegistryTaggedImageExtended, RegistryImageDependencies>;
    private tableSelectionManager: SimpleSelectionManager<RegistryImageDependencies, RegistryImageDependencies, RegistryImageDependencies>;

    private tagToDecoded: Map<string, RegistryImageDependencies>;

    images: RegistryImageDependencies[];

    private filterImages: string;

    constructor(inj: Injector)
    {
        super(inj);

        this.table                 = this.newTableWithAutoRefresh(this.app.domain.registryTaggedImages, this);
        this.tableSelectionManager = this.table.enableSimpleSelection((k) => k, (v) => v, false, false);
    }

    async loadData()
    {
        if (!this.images)
        {
            let tagToDecoded                       = new Map<string, RegistryImageDependencies>();
            let items: RegistryImageDependencies[] = [];

            let images = await this.app.domain.registryImages.getExtendedAll();
            await inParallel(images, async (image) =>
            {
                if (image)
                {
                    let taggedImages = await image.getReferencingTags();
                    for (let taggedImage of taggedImages)
                    {
                        if (taggedImage)
                        {
                            let item = new RegistryImageDependencies();
                            await item.init(taggedImage);

                            tagToDecoded.set(taggedImage.model.sysId, item);
                            items.push(item);
                        }
                    }
                }
            });

            this.tagToDecoded = tagToDecoded;
            this.images       = items;
        }
    }

    getItemName(): string
    {
        return "Tagged Images";
    }

    async getList(): Promise<RegistryImageDependencies[]>
    {
        await this.loadData();

        let results = this.images.filter((img) =>
                                         {
                                             try
                                             {
                                                 if (!this.filterImages) return true;

                                                 if (img.tagged && img.tagged.model.tag.indexOf(this.filterImages) >= 0) return true;

                                                 if (img.image && img.image.model.imageSha.indexOf(this.filterImages) >= 0) return true;

                                                 if (img.service.indexOf(this.filterImages) >= 0) return true;
                                             }
                                             catch (e)
                                             {
                                                 // Filter out items with any issues.
                                             }

                                             return false;
                                         });

        let sortBindings = this.mapSortBindings(this.table.sort);
        if (sortBindings && sortBindings.length > 0)
        {
            let sort = sortBindings[0];

            results.sort((valueA,
                          valueB) =>
                         {
                             let res: number;

                             switch (sort.column)
                             {
                                 default:
                                 case "accountAndName":
                                     res = UtilsService.compareStrings(valueA.accountAndName, valueB.accountAndName, true);
                                     break;

                                 case "tag":
                                     res = UtilsService.compareStrings(valueA.tag, valueB.tag, true);
                                     break;

                                 case "releaseStatus":
                                     res = UtilsService.compareStrings(valueA.releaseStatus, valueB.releaseStatus, true);
                                     break;

                                 case "architecture":
                                     res = UtilsService.compareStrings(valueA.image.model.architecture, valueB.image.model.architecture, true);
                                     break;

                                 case "imageSha":
                                     res = UtilsService.compareStrings(valueA.image.model.imageSha, valueB.image.model.imageSha, true);
                                     break;

                                 case "service":
                                     res = UtilsService.compareStrings(valueA.service, valueB.service, true);
                                     break;

                                 case "usages":
                                     res = valueA.usages.compare(valueB.usages);
                                     if (res == 0)
                                     {
                                         return -MomentHelper.compareDates(valueA.image.model.createdOn, valueB.image.model.createdOn);
                                     }
                                     break;

                                 case "createdOn":
                                     res = MomentHelper.compareDates(valueA.image.model.createdOn, valueB.image.model.createdOn);
                                     break;
                             }

                             return sort.ascending ? res : -res;
                         });
        }

        return results;
    }

    async getPage(offset: number,
                  limit: number): Promise<RegistryTaggedImageExtended[]>
    {
        return this.table.slicePage(offset, limit)
                   .map((img) => img.tagged);
    }

    async transform(rows: RegistryTaggedImageExtended[]): Promise<RegistryImageDependencies[]>
    {
        return rows.map((row) => this.tagToDecoded.get(row.model.sysId));
    }

    async itemClicked(columnId: string,
                      item: RegistryImageDependencies)
    {
        switch (columnId)
        {
            case "selected":
                return;

            case "usages":
                for (let taskSysId of (item.usage.tasks || []))
                {
                    let task = item.usage.lookupTask[taskSysId];

                    this.app.ui.navigation.go("deployments", [
                        "item",
                        task.deployment.sysId,
                        "task",
                        taskSysId
                    ]);
                    return;
                }
                break;
        }

        this.app.ui.navigation.go("/images/item", [item.tagged.model.sysId]);
    }

    //--//

    clearAll()
    {
        this.tableSelectionManager.checkAllItems(false);
    }

    markUnused()
    {
        if (this.images)
        {
            let now = new Date().getTime();

            let count = 0;

            this.images.forEach((img) =>
                                {
                                    if (img.usages.isInUse()) return;

                                    if (!img.image.getTargetService()) return;

                                    if (img.tagged.model.releaseStatus != Models.RegistryImageReleaseStatus.None) return;

                                    let lastUpdate = new Date(img.image.model.createdOn).getTime();
                                    let inactive   = now - lastUpdate;

                                    // Older than N days?
                                    const days = 2;
                                    if (inactive < days * (24 * 3600 * 1000)) return;

                                    this.tableSelectionManager.setChecked(img, true);

                                    count++;
                                });

            if (count != 0)
            {
                this.app.framework.errors.success(`Selected ${count} unused images.`, -1);
            }
        }
    }

    anyImageSelected(): boolean
    {
        return this.tableSelectionManager.selection.size > 0;
    }

    async deleteImages()
    {
        for (let image of this.tableSelectionManager.selection)
        {
            if (image.usage.tasks && image.usage.tasks.length > 0)
            {
                this.app.framework.errors.error("INVALID_STATE", "Cannot delete images in use.");
                return;
            }

            if (image.tagged.model.releaseStatus != Models.RegistryImageReleaseStatus.None)
            {
                this.app.framework.errors.error("INVALID_STATE", `Cannot delete images mark as ${image.tagged.model.releaseStatus}.`);
                return;
            }
        }

        for (let image of this.tableSelectionManager.selection)
        {
            await image.tagged.remove();
        }

        this.images = null;

        this.table.refreshData();
    }
}
