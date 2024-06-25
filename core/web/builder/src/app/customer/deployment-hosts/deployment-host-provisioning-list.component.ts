import {Component, EventEmitter, Injector, Input, Output} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

@Component({
               selector   : "o3-deployment-host-provisioning-list",
               templateUrl: "./deployment-host-provisioning-list.component.html",
               styleUrls  : ["./deployment-host-provisioning-list.component.scss"]
           })
export class DeploymentHostProvisioningListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<NotesExtended, NotesExtended, NotesExtended>
{
    private m_notes: Models.DeploymentHostProvisioningNotes[];

    public get notes(): Models.DeploymentHostProvisioningNotes[]
    {
        return this.m_notes;
    }

    @Input()
    public set notes(value: Models.DeploymentHostProvisioningNotes[])
    {
        this.m_notes = value;
        this.table.refreshData();
    }

    @Output() noteClicked = new EventEmitter<Models.DeploymentHostProvisioningNotes>();

    //--//

    table: DatatableManager<NotesExtended, NotesExtended, NotesExtended>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = new DatatableManager<NotesExtended, NotesExtended, NotesExtended>(this, () => this.getViewState());
    }

    getItemName(): string
    {
        return "Host Files";
    }

    async getList(): Promise<NotesExtended[]>
    {
        let notes = this.m_notes || [];

        let sortBindings = this.mapSortBindings(this.table.sort);
        if (sortBindings && sortBindings.length > 0)
        {
            let sort = sortBindings[0];

            notes.sort((valueA,
                        valueB) =>
                       {
                           let res: number;

                           switch (sort.column)
                           {
                               case "timestamp":
                                   res = MomentHelper.compareDates(valueA.timestamp, valueB.timestamp);
                                   break;
                           }

                           return sort.ascending ? res : -res;
                       });
        }

        return notes.map((note) =>
                         {
                             let noteExt   = new NotesExtended();
                             noteExt.model = note;

                             if (note.readyForProduction)
                             {
                                 noteExt.event = "Ready For Production";
                             }
                             else if (note.readyForShipping)
                             {
                                 noteExt.event = "Ready For Shipping";
                             }
                             else if (note.deployed)
                             {
                                 noteExt.event = "Deployed";
                             }

                             return noteExt;
                         });
    }

    async getPage(offset: number,
                  limit: number): Promise<NotesExtended[]>
    {
        return this.table.slicePage(offset, limit);
    }

    async transform(rows: NotesExtended[]): Promise<NotesExtended[]>
    {
        return rows;
    }

    itemClicked(columnId: string,
                item: NotesExtended)
    {
        this.noteClicked.emit(item.model);
    }
}

class NotesExtended
{
    model: Models.DeploymentHostProvisioningNotes;

    event: string;
}
