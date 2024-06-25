import {Component, EventEmitter, Injector, Input, Output} from "@angular/core";

import {ProberObjectDetails} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import * as SharedSvc from "app/services/domain/base.service";
import {UtilsService} from "framework/services/utils.service";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-gateway-probers-object-list",
               templateUrl: "./probers-object-list.component.html"
           })
export class ProbersObjectListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<ResultDecoded, ResultDecoded, ResultDecoded>
{
    @Input() enableFiltering: boolean;

    @Input() allowSelection: boolean;

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

    @Input() set objects(lst: ProberObjectDetails[])
    {
        this.m_results = (lst || []).map((a) =>
                                         {
                                             let res    = new ResultDecoded();
                                             res.result = a;
                                             return res;
                                         });

        this.table.refreshData();
    }

    @Output() objectClicked   = new EventEmitter<ProberObjectDetails>();
    @Output() objectsSelected = new EventEmitter<ProberObjectDetails[]>();

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

                                                        got = got || result.result.device.textForTransport.indexOf(filter) >= 0;
                                                        got = got || result.result.device.textForIdentity.indexOf(filter) >= 0;
                                                        got = got || result.result.device.textForMAC.indexOf(filter) >= 0;
                                                        got = got || result.result.device.annotations.indexOf(filter) >= 0;
                                                        got = got || result.result.objectId.indexOf(filter) >= 0;

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
                                 case "transport":
                                     res = UtilsService.compareStrings(valueA.result.device.textForTransport, valueB.result.device.textForTransport, true);
                                     break;

                                 case "identity":
                                     res = UtilsService.compareStrings(valueA.result.device.textForIdentity, valueB.result.device.textForIdentity, true);
                                     break;

                                 case "mac":
                                     res = UtilsService.compareStrings(valueA.result.device.textForMAC, valueB.result.device.textForMAC, true);
                                     break;

                                 case "objectId":
                                     res = UtilsService.compareStrings(valueA.result.objectId, valueB.result.objectId, true);
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
        this.objectClicked.emit(item.result);
    }

    selectionChanged()
    {
        let selected = this.m_results.filter((details) => details.selected)
                           .map((details) => details.result);

        this.objectsSelected.emit(selected);
    }
}

class ResultDecoded
{
    result: ProberObjectDetails;
    selected: boolean;
}
