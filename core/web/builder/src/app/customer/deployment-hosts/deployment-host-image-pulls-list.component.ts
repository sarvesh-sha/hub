import {Component, EventEmitter, Injector, Input, Output} from "@angular/core";
import {AppContext} from "app/app.service";

import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentHostImagePullExtended} from "app/services/domain/deployment-host-image-pulls.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";
import * as Models from "app/services/proxy/model/models";
import {JobStatus, RecordIdentity} from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {PersistViewState} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";

import {DatatableManager, DatatableSelectionManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {DatatableContextMenuEvent} from "framework/ui/datatables/datatable.component";
import {inParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-deployment-host-image-pulls-list",
               templateUrl: "./deployment-host-image-pulls-list.component.html"
           })
export class DeploymentHostImagePullsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<ImageDetails, ImageDetails, ImageDetails>
{
    private m_host: DeploymentHostExtended;

    public get host(): DeploymentHostExtended
    {
        return this.m_host;
    }

    @Input() public showHost: boolean;

    @Input()
    public set host(value: DeploymentHostExtended)
    {
        this.m_host = value;
        this.table.refreshData();
    }

    @Output() imageClicked = new EventEmitter<DeploymentHostImagePullExtended>();

    @Output() imageMenu = new EventEmitter<DatatableContextMenuEvent<ImageSelection>>();

    //--//

    @PersistViewState() private m_filterText: string = null;

    public get filterText(): string
    {
        return this.m_filterText;
    }

    public set filterText(value: string)
    {
        this.m_filterText = value;
        this.table.refreshData();
    }

    //--//

    @PersistViewState() private m_filterStatus: Models.JobStatus;
    public states: ControlOption<Models.JobStatus>[];

    public get filterStatus(): Models.JobStatus
    {
        let filterStatus = this.m_filterStatus;
        if (!filterStatus)
        {
            filterStatus = this.showHost ? Models.JobStatus.EXECUTING : JobStatus.INITIALIZED;
        }

        return filterStatus;
    }

    public set filterStatus(value: Models.JobStatus)
    {
        this.m_filterStatus = value;
        this.table.refreshData();
    }

    //--//

    table: DatatableManager<ImageDetails, ImageDetails, ImageDetails>;
    private tableSelectionManager: ImagePullSelectionManager;

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        this.persistViewStateReady(); // Needed to properly enable @PersistViewState

        this.states = [
            DeploymentHostImagePullsListComponent.getOption(Models.JobStatus.INITIALIZED, "Any State"),
            DeploymentHostImagePullsListComponent.getOption(Models.JobStatus.EXECUTING, "Executing"),
            DeploymentHostImagePullsListComponent.getOption(Models.JobStatus.FAILED, "Failed"),
            DeploymentHostImagePullsListComponent.getOption(Models.JobStatus.COMPLETED, "Completed")
        ];

        this.table                  = new DatatableManager<ImageDetails, ImageDetails, ImageDetails>(this, () => this.getViewState());
        this.tableSelectionManager  = new ImagePullSelectionManager(this.table);
        this.table.selectionManager = this.tableSelectionManager;

    }

    private static getOption(id: Models.JobStatus,
                             label: string): ControlOption<Models.JobStatus>
    {
        let opt   = new ControlOption<Models.JobStatus>();
        opt.id    = id;
        opt.label = label;
        return opt;
    }

    getItemName(): string
    {
        return "Image Pulls";
    }

    async getList(): Promise<ImageDetails[]>
    {
        let filters       = new Models.DeploymentHostImagePullFilterRequest();
        filters.hostSysId = this.m_host?.model?.sysId;

        if (this.filterStatus != Models.JobStatus.INITIALIZED)
        {
            filters.statusFilter = this.filterStatus;
        }

        let sortBindings = this.mapSortBindings(this.table.sort);
        if (sortBindings && sortBindings.length > 0)
        {
            filters.sortBy = sortBindings;
        }

        let ids = await this.app.domain.deploymentHostImagePulls.getFiltered(filters);

        let details = (ids || []).map(ri =>
                                      {
                                          let res = new ImageDetails();
                                          res.ri  = ri;
                                          return res;
                                      });

        if (this.filterText)
        {
            await inParallel(details, (row) => row.load(this.app));

            details = details.filter((a) =>
                                     {
                                         let filterText = this.filterText.toLocaleLowerCase();

                                         if (this.matchFilter(filterText, a.hostName))
                                         {
                                             return true;
                                         }

                                         if (this.matchFilter(filterText, a.ext.model.image))
                                         {
                                             return true;
                                         }

                                         return false;
                                     });
        }

        if (sortBindings && sortBindings.length > 0)
        {
            await inParallel(details, (row) => row.load(this.app));

            let sort = sortBindings[0];

            switch (sort.column)
            {
                case "image":
                    details.sort((valueA,
                                  valueB) =>
                                 {
                                     let res = UtilsService.compareStrings(valueA.ext.model.image, valueB.ext.model.image, true);
                                     return sort.ascending ? res : -res;
                                 });
                    break;

                case "host":
                    details.sort((valueA,
                                  valueB) =>
                                 {
                                     let res = UtilsService.compareStrings(valueA.hostName, valueB.hostName, true);
                                     return sort.ascending ? res : -res;
                                 });
                    break;
            }
        }

        return details;
    }

    async getPage(offset: number,
                  limit: number): Promise<ImageDetails[]>
    {
        let rows = await this.table.slicePage(offset, limit);

        await inParallel(rows, (row) => row.load(this.app));

        return rows;
    }

    async transform(rows: ImageDetails[]): Promise<ImageDetails[]>
    {
        return rows;
    }

    async itemClicked(columnId: string,
                      item: ImageDetails)
    {
        if (columnId == "host")
        {
            let host = await item.ext.getOwningDeployment();

            this.app.ui.navigation.go("/deployments", [
                "item",
                host.model.sysId
            ]);
        }
        else
        {
            this.imageClicked.emit(item.ext);
        }
    }

    handleContextMenu(event: DatatableContextMenuEvent<ImageDetails>)
    {
        let details = event.row;
        if (!details) return;

        let imageSelection          = new ImageSelection();
        imageSelection.singleSelect = details.ext;

        let selection = this.tableSelectionManager.selection;
        if (selection.size > 1)
        {
            imageSelection.multiSelect = [];
            for (let selected of selection)
            {
                imageSelection.multiSelect.push(selected.ext);
            }
        }

        let event2            = new DatatableContextMenuEvent<ImageSelection>();
        event2.root           = event.root;
        event2.row            = imageSelection;
        event2.columnProperty = event.columnProperty;
        this.imageMenu.emit(event2);

    }
}

export class ImageSelection
{
    singleSelect: DeploymentHostImagePullExtended;
    multiSelect: DeploymentHostImagePullExtended[];
}

class ImageDetails
{
    ri: RecordIdentity;
    ext: DeploymentHostImagePullExtended;
    hostName: string;

    async load(app: AppContext)
    {
        if (!this.ext)
        {
            let row = await app.domain.deploymentHostImagePulls.getExtendedByIdentity(this.ri);

            this.ext      = row;
            this.hostName = await row.getHostDisplayName();
        }
    }
}

export class ImagePullSelectionManager extends DatatableSelectionManager<ImageDetails, ImageDetails, ImageDetails>
{
    constructor(table: DatatableManager<ImageDetails, any, ImageDetails>)
    {
        super(table, false, false, (k) => k, (v) => v);
    }

    public handleItemChecked(row: ImageDetails): void
    {
        let wasSelected = this.isChecked(row);

        this.setChecked(row, !wasSelected);
    }
}
