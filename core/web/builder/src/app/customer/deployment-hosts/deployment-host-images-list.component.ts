import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

@Component({
               selector   : "o3-deployment-host-images-list",
               templateUrl: "./deployment-host-images-list.component.html"
           })
export class DeploymentHostImagesListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.DeploymentHostImage, Models.DeploymentHostImage, Models.DeploymentHostImage>
{
    private m_images: Models.DeploymentHostImage[];

    public get images(): Models.DeploymentHostImage[]
    {
        return this.m_images;
    }

    @Input()
    public set images(value: Models.DeploymentHostImage[])
    {
        this.m_images = value;
        this.table.refreshData();
    }

    //--//

    table: DatatableManager<Models.DeploymentHostImage, Models.DeploymentHostImage, Models.DeploymentHostImage>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table      = new DatatableManager<Models.DeploymentHostImage, Models.DeploymentHostImage, Models.DeploymentHostImage>(this, () => this.getViewState());
        this.table.sort = [
            {
                prop: "tag",
                dir : "desc"
            }
        ];
    }

    getItemName(): string
    {
        return "Host Images";
    }

    async getList(): Promise<Models.DeploymentHostImage[]>
    {
        let images = this.m_images || [];

        let sortBindings = this.mapSortBindings(this.table.sort);
        if (sortBindings && sortBindings.length > 0)
        {
            let sort = sortBindings[0];

            images.sort((valueA,
                         valueB) =>
                        {
                            let res: number;

                            switch (sort.column)
                            {
                                case "id":
                                    res = UtilsService.compareStrings(valueA.id, valueB.id, true);
                                    break;

                                case "tag":
                                    res = UtilsService.compareStrings(valueA.tag, valueB.tag, true);
                                    break;

                                case "size":
                                    res = valueA.size - valueB.size;
                                    break;

                                case "created":
                                    res = MomentHelper.compareDates(valueA.created, valueB.created);
                                    break;

                                case "lastRefreshed":
                                    res = MomentHelper.compareDates(valueA.lastRefreshed, valueB.lastRefreshed);
                                    break;

                                case "lastUsed":
                                    res = MomentHelper.compareDates(valueA.lastUsed, valueB.lastUsed);
                                    break;
                            }

                            return sort.ascending ? res : -res;
                        });
        }

        return images;
    }

    async getPage(offset: number,
                  limit: number): Promise<Models.DeploymentHostImage[]>
    {
        return this.table.slicePage(offset, limit);
    }

    async transform(rows: Models.DeploymentHostImage[]): Promise<Models.DeploymentHostImage[]>
    {
        return rows;
    }

    itemClicked(columnId: string,
                item: Models.DeploymentHostImage)
    {
    }
}
