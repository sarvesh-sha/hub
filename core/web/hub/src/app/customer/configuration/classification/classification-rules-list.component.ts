import {Component, Injector} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";
import {NormalizationExtended} from "app/services/domain/normalization.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-classification-rules-list",
               templateUrl: "./classification-rules-list.component.html"
           })
export class ClassificationRulesListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, NormalizationExtended, NormalizationFlat>
{
    table: DatatableManager<Models.RecordIdentity, NormalizationExtended, NormalizationFlat>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.normalization, this);
    }

    getTableConfigId(): string { return "classification"; }

    getItemName(): string { return "Versions"; }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        return await this.app.domain.normalization.getList();
    }

    getPage(offset: number,
            limit: number): Promise<NormalizationExtended[]>
    {
        return this.app.domain.normalization.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: NormalizationExtended[]): Promise<NormalizationFlat[]>
    {
        return await mapInParallel(rows,
                                   async (row,
                                          index) =>
                                   {
                                       let result      = new NormalizationFlat();
                                       result.extended = row;

                                       return result;
                                   });
    }

    itemClicked(columnId: string,
                item: NormalizationFlat)
    {
        this.app.ui.navigation.go("/classification/item", [item.extended.model.sysId]);
    }
}

class NormalizationFlat
{
    extended: NormalizationExtended;
}
