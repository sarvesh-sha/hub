import {Component, Injector, Input} from "@angular/core";
import {ReportPreviewDialogComponent} from "app/customer/configuration/reports/wizard/report-preview-dialog.component";

import * as SharedSvc from "app/services/domain/base.service";
import {ReportExtended} from "app/services/domain/reports.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-report-history-list",
               templateUrl: "./report-history-list.component.html"
           })
export class ReportHistoryListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, ReportExtended, ReportFlat>
{
    @Input() set parent(parent: Models.ReportDefinition)
    {
        this.m_parent = parent;
        this.table.refreshData();
    }

    table: DatatableManager<Models.RecordIdentity, ReportExtended, ReportFlat>;

    private m_parent: Models.ReportDefinition;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.reports, this);
    }

    getItemName(): string { return "Reports"; }

    getTableConfigId(): string { return "report-history"; }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        if (!this.m_parent)
        {
            return [];
        }

        let filters = Models.ReportFilterRequest.newInstance({
                                                                 sortBy       : this.mapSortBindings(this.table.sort),
                                                                 definitionIds: [this.m_parent.sysId]
                                                             });

        return await this.app.domain.reports.getList(filters);
    }

    getPage(offset: number,
            limit: number): Promise<ReportExtended[]>
    {
        return this.app.domain.reports.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: ReportExtended[]): Promise<ReportFlat[]>
    {
        return mapInParallel(rows, async (row) =>
        {
            let rowFlat      = new ReportFlat();
            rowFlat.extended = row;
            let version      = await row.getDefinitionVersion();
            rowFlat.version  = `v${version.model.version}`;
            return rowFlat;
        });
    }

    itemClicked(columnId: string,
                item: ReportFlat)
    {
        switch (columnId)
        {
            case  "view":
                if (item.extended.isFinished)
                {
                    this.view(null, item.extended);
                }
                else if (item.extended.isFailed)
                {
                    item.extended.retry();
                }
                break;

            default:
                this.app.ui.navigation.push([
                                                "history",
                                                item.extended.model.sysId
                                            ]);
                break;
        }
    }

    view(event: Event,
         item: ReportExtended)
    {
        if (event) event.stopPropagation();

        let url = item.getDownloadUrl(this.m_parent.title);
        if (url)
        {
            ReportPreviewDialogComponent.open(this, {reportUrl: url});
        }
    }
}

class ReportFlat
{
    extended: ReportExtended;
    version: string;
}
