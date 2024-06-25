import {Component, EventEmitter, Injector, Input, Output} from "@angular/core";

import {ProberDeviceDetails} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import * as SharedSvc from "app/services/domain/base.service";
import {UtilsService} from "framework/services/utils.service";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-gateway-probers-device-list",
               templateUrl: "./probers-device-list.component.html"
           })
export class ProbersDeviceListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<ResultDecoded, ResultDecoded, ResultDecoded>
{
    @Input() enableFiltering: boolean;

    @Input() allowSelection: boolean;

    private m_filter: string;

    @Input() set filterDevices(value: string)
    {
        this.m_filter = value;
        this.table.refreshData();
    }

    get filterDevices(): string
    {
        return this.m_filter;
    }

    private m_results: ResultDecoded[] = [];

    @Input() set devices(lst: ProberDeviceDetails[])
    {
        this.m_results = (lst || []).map((a) =>
                                         {
                                             let res    = new ResultDecoded();
                                             res.result = a;
                                             return res;
                                         });

        this.table.refreshData();
    }

    @Output() devicesSelected = new EventEmitter<ProberDeviceDetails[]>();

    table: DatatableManager<ResultDecoded, ResultDecoded, ResultDecoded>;

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        this.table = new DatatableManager<ResultDecoded, ResultDecoded, ResultDecoded>(this, () => this.getViewState());
    }

    getItemName(): string
    {
        return "Discovered Devices";
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

                                                        got = got || result.result.textForTransport.indexOf(filter) >= 0;
                                                        got = got || result.result.textForIdentity.indexOf(filter) >= 0;
                                                        got = got || result.result.textForMAC.indexOf(filter) >= 0;
                                                        got = got || result.result.annotations.indexOf(filter) >= 0;

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
                                     res = UtilsService.compareStrings(valueA.result.textForTransport, valueB.result.textForTransport, true);
                                     break;

                                 case "identity":
                                     res = UtilsService.compareStrings(valueA.result.textForIdentity, valueB.result.textForIdentity, true);
                                     break;

                                 case "mac":
                                     res = UtilsService.compareStrings(valueA.result.textForMAC, valueB.result.textForMAC, true);
                                     break;

                                 case "annotations":
                                     res = UtilsService.compareStrings(valueA.result.annotations, valueB.result.annotations, true);
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
        return;
    }

    selectionChanged()
    {
        let selected = this.m_results.filter((details) => details.selected)
                           .map((details) => details.result);

        this.devicesSelected.emit(selected);
    }
}

class ResultDecoded
{
    result: ProberDeviceDetails;
    selected: boolean;
}
