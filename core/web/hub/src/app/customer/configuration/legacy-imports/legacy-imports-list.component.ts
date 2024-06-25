import {Component, Injector} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";
import {ImportedMetadataExtended} from "app/services/domain/data-imports.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-legacy-imports-list",
               templateUrl: "./legacy-imports-list.component.html"
           })
export class LegacyImportsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, ImportedMetadataExtended, ImportedMetadataFlat>
{
    table: DatatableManager<Models.RecordIdentity, ImportedMetadataExtended, ImportedMetadataFlat>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.dataImports, this);
    }

    getItemName(): string { return "Normalization"; }

    getTableConfigId(): string { return "legacy-imports"; }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        return await this.app.domain.dataImports.getList();
    }

    getPage(offset: number,
            limit: number): Promise<ImportedMetadataExtended[]>
    {
        return this.app.domain.dataImports.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: ImportedMetadataExtended[]): Promise<ImportedMetadataFlat[]>
    {
        return await mapInParallel(rows,
                                   async (row,
                                          index) =>
                                   {
                                       let result      = new ImportedMetadataFlat();
                                       result.extended = row;

                                       return result;
                                   });
    }

    itemClicked(columnId: string,
                item: ImportedMetadataFlat)
    {
        this.app.ui.navigation.go("/legacy-imports/item", [item.extended.model.sysId]);
    }
}

class ImportedMetadataFlat
{
    extended: ImportedMetadataExtended;
}
