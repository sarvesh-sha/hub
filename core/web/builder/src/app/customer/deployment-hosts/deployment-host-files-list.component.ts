import {Component, EventEmitter, Injector, Input, Output} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentHostFileExtended} from "app/services/domain/deployment-host-files.service";

import {UtilsService} from "framework/services/utils.service";
import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {DatatableContextMenuEvent} from "framework/ui/datatables/datatable.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

@Component({
               selector   : "o3-deployment-host-files-list",
               templateUrl: "./deployment-host-files-list.component.html"
           })
export class DeploymentHostFilesListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<DeploymentHostFileExtended, DeploymentHostFileExtended, DeploymentHostFileExtended>
{
    private m_files: DeploymentHostFileExtended[];

    public get files(): DeploymentHostFileExtended[]
    {
        return this.m_files;
    }

    @Input()
    public set files(value: DeploymentHostFileExtended[])
    {
        this.m_files = value;
        this.table.refreshData();
    }

    @Output() fileClicked = new EventEmitter<DeploymentHostFileExtended>();

    @Output() fileMenu = new EventEmitter<DatatableContextMenuEvent<DeploymentHostFileExtended>>();

    //--//

    table: DatatableManager<DeploymentHostFileExtended, DeploymentHostFileExtended, DeploymentHostFileExtended>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = new DatatableManager<DeploymentHostFileExtended, DeploymentHostFileExtended, DeploymentHostFileExtended>(this, () => this.getViewState());
    }

    getItemName(): string
    {
        return "Host Files";
    }

    async getList(): Promise<DeploymentHostFileExtended[]>
    {
        let files = this.m_files || [];

        let sortBindings = this.mapSortBindings(this.table.sort);
        if (sortBindings && sortBindings.length > 0)
        {
            let sort = sortBindings[0];

            files.sort((valueA,
                        valueB) =>
                       {
                           let res: number;

                           switch (sort.column)
                           {
                               case "path":
                                   res = UtilsService.compareStrings(valueA.model.path, valueB.model.path, true);
                                   break;

                               case "downloadedOn":
                                   res = MomentHelper.compareDates(valueA.model.downloadedOn, valueB.model.downloadedOn);
                                   break;

                               case "uploadedOn":
                                   res = MomentHelper.compareDates(valueA.model.uploadedOn, valueB.model.uploadedOn);
                                   break;

                               case "createdOn":
                                   res = MomentHelper.compareDates(valueA.model.createdOn, valueB.model.createdOn);
                                   break;
                           }

                           return sort.ascending ? res : -res;
                       });
        }

        return files;
    }

    async getPage(offset: number,
                  limit: number): Promise<DeploymentHostFileExtended[]>
    {
        return this.table.slicePage(offset, limit);
    }

    async transform(rows: DeploymentHostFileExtended[]): Promise<DeploymentHostFileExtended[]>
    {
        return rows;
    }

    itemClicked(columnId: string,
                item: DeploymentHostFileExtended)
    {
        this.fileClicked.emit(item);
    }

    handleContextMenu(event: DatatableContextMenuEvent<DeploymentHostFileExtended>)
    {
        let details = event.row;
        if (!details) return;

        switch (event.columnProperty)
        {
            case "path":
                this.fileMenu.emit(event);
                break;
        }
    }
}
