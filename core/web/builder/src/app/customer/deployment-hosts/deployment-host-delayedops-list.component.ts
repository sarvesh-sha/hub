import {Component, EventEmitter, Injector, Input, Output} from "@angular/core";
import {BackgroundActivityExtended} from "app/services/domain/background-activities.service";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider, SimpleSelectionManager} from "framework/ui/datatables/datatable-manager";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-deployment-host-delayedops-list",
               templateUrl: "./deployment-host-delayedops-list.component.html"
           })
export class DeploymentHostDelayedOpsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<OperationDecoded, OperationDecoded, OperationDecoded>
{
    private m_operations: OperationDecoded[];

    @Input()
    public set operations(value: Models.DelayedOperation[])
    {
        this.refreshData(value);
    }

    @Output() operationsSelected = new EventEmitter<Models.DelayedOperation[]>();

    @Output() operationClicked = new EventEmitter<Models.DelayedOperation>();

    //--//

    table: DatatableManager<OperationDecoded, OperationDecoded, OperationDecoded>;
    private tableSelectionManager: SimpleSelectionManager<OperationDecoded, OperationDecoded, OperationDecoded>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table                 = new DatatableManager<OperationDecoded, OperationDecoded, OperationDecoded>(this, () => this.getViewState());
        this.tableSelectionManager = this.table.enableSimpleSelection((k) => k, (v) => v, false, false);
        this.tableSelectionManager.selectionChangeSummary.subscribe(() => this.operationSelected());
    }

    async refreshData(ops: Models.DelayedOperation[])
    {
        this.m_operations = await mapInParallel(ops || [], async (op) =>
        {
            let decoded = new OperationDecoded(op);
            decoded.status = "Inactive";

            if (op.loc_activity?.id)
            {
                decoded.activity = await this.app.domain.backgroundActivities.getExtendedById(op.loc_activity.id);
                if (decoded.activity)
                {
                    decoded.status  =  !decoded.activity || decoded.activity.isDone() ? "Cleanup" : "Active";
                    decoded.details = (await decoded.activity.activityInfo()).join(", ");
                }
            }

            return decoded;
        });

        this.table.refreshData();
    }

    getItemName(): string
    {
        return "Delayed Operations";
    }

    async getList(): Promise<OperationDecoded[]>
    {
        return this.m_operations || [];
    }

    async getPage(offset: number,
                  limit: number): Promise<OperationDecoded[]>
    {
        return this.table.slicePage(offset, limit);
    }

    async transform(rows: OperationDecoded[]): Promise<OperationDecoded[]>
    {
        return rows;
    }

    itemClicked(columnId: string,
                item: OperationDecoded)
    {
        this.operationClicked.emit(item.model);
    }

    operationSelected()
    {
        let selected = [];

        for (let row of this.tableSelectionManager.selection)
        {
            selected.push(row.model);
        }

        this.operationsSelected.emit(selected);
    }
}

class OperationDecoded
{
    activity: BackgroundActivityExtended;

    status: string;

    details: string;

    constructor(public model: Models.DelayedOperation)
    {
    }
}
