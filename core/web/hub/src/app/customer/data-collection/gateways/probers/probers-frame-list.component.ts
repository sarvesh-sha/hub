import {Component, EventEmitter, Injector, Input, Output} from "@angular/core";

import {ProberFrameDetails} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import * as SharedSvc from "app/services/domain/base.service";
import {UtilsService} from "framework/services/utils.service";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

@Component({
               selector   : "o3-gateway-probers-frame-list",
               templateUrl: "./probers-frame-list.component.html"
           })
export class ProbersFrameListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<ResultDecoded, ResultDecoded, ResultDecoded>
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

    @Input() set frames(lst: ProberFrameDetails[])
    {
        this.m_results = (lst || []).map((a) =>
                                         {
                                             let res    = new ResultDecoded();
                                             res.result = a;
                                             return res;
                                         });

        this.table.refreshData();
    }

    @Output() frameClicked  = new EventEmitter<ProberFrameDetails>();
    @Output() frameSelected = new EventEmitter<ProberFrameDetails[]>();

    table: DatatableManager<ResultDecoded, ResultDecoded, ResultDecoded>;

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        this.table = new DatatableManager<ResultDecoded, ResultDecoded, ResultDecoded>(this, () => null);
    }

    getItemName(): string
    {
        return "Sampled Frames";
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

                                                        got = got || (result.result.id || "").indexOf(filter) >= 0;

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
                                 case "timestamp":
                                     res = MomentHelper.compareDates(valueA.result.timestamp, valueB.result.timestamp);
                                     break;

                                 case "id":
                                     res = UtilsService.compareStrings(valueA.result.id, valueB.result.id, true);
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
        this.frameClicked.emit(item.result);
    }

    selectionChanged()
    {
        let selected = this.m_results.filter((details) => details.selected)
                           .map((details) => details.result);

        this.frameSelected.emit(selected);
    }
}

class ResultDecoded
{
    result: ProberFrameDetails;
    selected: boolean;
}
