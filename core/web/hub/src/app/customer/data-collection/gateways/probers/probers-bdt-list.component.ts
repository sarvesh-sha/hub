import {Component, Injector, Input} from "@angular/core";

import {ProbersBDTDetails} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import * as SharedSvc from "app/services/domain/base.service";
import {UtilsService} from "framework/services/utils.service";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-gateway-probers-bdt-list",
               templateUrl: "./probers-bdt-list.component.html"
           })
export class ProbersBDTListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<ResultDecoded, ResultDecoded, ResultDecoded>
{
    @Input() enableFiltering: boolean;

    private m_filter: string;

    @Input() set filterObjects(value: string)
    {
        this.m_filter = value;
        this.table.refreshData();
    }

    get filterObjects(): string
    {
        return this.m_filter;
    }

    private m_results: ResultDecoded[] = [];

    @Input() set bdt(lst: ProbersBDTDetails[])
    {
        this.m_results = (lst || []).map((a) =>
                                         {
                                             let res    = new ResultDecoded();
                                             res.result = a;
                                             return res;
                                         });

        this.table.refreshData();
    }

    table: DatatableManager<ResultDecoded, ResultDecoded, ResultDecoded>;

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        this.table = new DatatableManager<ResultDecoded, ResultDecoded, ResultDecoded>(this, () => this.getViewState());
    }

    getItemName(): string
    {
        return "Discovered Objects";
    }

    async getList(): Promise<ResultDecoded[]>
    {
        let results = this.m_results.filter((result) =>
                                            {
                                                try
                                                {
                                                    for (let filter of (this.m_filter || "").split(" "))
                                                    {
                                                        let got = false;

                                                        got = got || result.result.bbmdTransport.indexOf(filter) >= 0;
                                                        got = got || result.result.entry.address?.indexOf(filter) >= 0;
                                                        got = got || result.result.entry.mask?.indexOf(filter) >= 0;

                                                        if (!got) return false;
                                                    }

                                                    return true;
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
                                 case "bbmd":
                                     res = UtilsService.compareStrings(valueA.result.bbmdTransport, valueB.result.bbmdTransport, true);
                                     break;

                                 case "address":
                                     res = UtilsService.compareStrings(valueA.result.entry.address, valueB.result.entry.address, true);
                                     break;

                                 case "mask":
                                     res = UtilsService.compareStrings(valueA.result.entry.mask, valueB.result.entry.mask, true);
                                     break;
                             }

                             return sort.ascending ? res : -res;
                         });
        }

        return results;
    }

    async getPage(offset: number,
                  limit: number): Promise<ResultDecoded[]>
    {
        return this.table.slicePage(offset, limit);
    }

    async transform(rows: ResultDecoded[]): Promise<ResultDecoded[]>
    {
        return rows;
    }

    async itemClicked(columnId: string,
                      item: ResultDecoded)
    {
    }
}

class ResultDecoded
{
    result: ProbersBDTDetails;
    selected: boolean;
}
