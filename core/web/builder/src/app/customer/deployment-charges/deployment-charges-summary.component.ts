import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";
import * as Models from "app/services/proxy/model/models";
import {ColumnConfiguration, DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {CurrencyPipe, NumberWithSeparatorsPipe} from "framework/ui/formatting/string-format.pipe";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-deployment-charges-summary",
               templateUrl: "./deployment-charges-summary.component.html"
           })
export class DeploymentChargesSummaryComponent extends SharedSvc.BaseApplicationComponent
{
    entries: ChargeItem[] = [];

    private m_charges: Models.DeploymentCellularChargesSummary;
    selectedView: string = "Summary";
    chargeDetails: ChargeDetails;

    public get charges(): Models.DeploymentCellularChargesSummary
    {
        return this.m_charges;
    }

    @Input()
    public set charges(value: Models.DeploymentCellularChargesSummary)
    {
        this.m_charges = value;

        let res: ChargeItem[] = [];

        if (value)
        {
            if (value.last24Hours)
            {
                res.push(new ChargeItem("Last 24 Hours", 1, value.count, value.last24Hours, value.last24HoursPerHost));
            }

            if (value.last24Hours && value.last7Days)
            {
                let values        = ChargeItem.computeDelta(value.last7Days, value.last24Hours);
                let valuesPerHost = ChargeItem.computeDeltaPerHost(value.last7DaysPerHost, value.last24HoursPerHost);
                res.push(new ChargeItem("Delta 1-7 Days", 6, value.count, values, valuesPerHost));
            }

            if (value.last7Days && value.last14Days)
            {
                let values        = ChargeItem.computeDelta(value.last14Days, value.last7Days);
                let valuesPerHost = ChargeItem.computeDeltaPerHost(value.last14DaysPerHost, value.last7DaysPerHost);
                res.push(new ChargeItem("Delta 7-14 Days", 7, value.count, values, valuesPerHost));
            }

            if (value.last14Days && value.last21Days)
            {
                let values        = ChargeItem.computeDelta(value.last21Days, value.last14Days);
                let valuesPerHost = ChargeItem.computeDeltaPerHost(value.last21DaysPerHost, value.last14DaysPerHost);
                res.push(new ChargeItem("Delta 21-14 Days", 7, value.count, values, valuesPerHost));
            }

            if (value.last21Days && value.last30Days)
            {
                let values        = ChargeItem.computeDelta(value.last30Days, value.last21Days);
                let valuesPerHost = ChargeItem.computeDeltaPerHost(value.last30DaysPerHost, value.last21DaysPerHost);
                res.push(new ChargeItem("Delta 30-21 Days", 9, value.count, values, valuesPerHost));
            }

            if (value.last7Days)
            {
                res.push(new ChargeItem("Last 7 Days", 7, value.count, value.last7Days, value.last7DaysPerHost));
            }

            if (value.last14Days)
            {
                res.push(new ChargeItem("Last 14 Days", 14, value.count, value.last14Days, value.last14DaysPerHost));
            }

            if (value.last21Days)
            {
                res.push(new ChargeItem("Last 21 Days", 21, value.count, value.last21Days, value.last21DaysPerHost));
            }

            if (value.last30Days)
            {
                res.push(new ChargeItem("Last 30 Days", 30, value.count, value.last30Days, value.last30DaysPerHost));
            }
        }

        this.entries = res;
    }

    @Input() public topHosts: boolean;

    constructor(inj: Injector)
    {
        super(inj);
    }

    public onClick(sysId: String)
    {
        this.app.ui.navigation.go("/deployments", [
            "item",
            sysId
        ]);
    }

    refreshView()
    {
        switch (this.selectedView)
        {
            case "last24HoursPerHost":
                this.bindHosts(this.charges.last24HoursPerHost);
                break;

            case "last7DaysPerHost":
                this.bindHosts(this.charges.last7DaysPerHost);
                break;

            case "last14DaysPerHost":
                this.bindHosts(this.charges.last14DaysPerHost);
                break;

            case "last21DaysPerHost":
                this.bindHosts(this.charges.last21DaysPerHost);
                break;

            case "last30DaysPerHost":
                this.bindHosts(this.charges.last30DaysPerHost);
                break;
        }
    }

    private async bindHosts(hosts: Models.DeploymentCellularChargePerHost[])
    {
        let output = await mapInParallel(hosts, async (host) =>
        {
            let ext = await this.app.domain.deploymentHosts.getExtendedById(host.sysId);
            return new HostDetails(ext, host.charges);
        });

        this.chargeDetails = new ChargeDetails(this, output);
    }
}

class ChargeItem
{
    billed: string;
    billedMonthly: string;
    billedAverage: string;
    billedHighest: string;
    billedHighestHost: string;
    total: string;
    upload: string;
    download: string;

    constructor(public period: string,
                public days: number,
                public count: number,
                values: Models.DeploymentCellularCharge,
                valuesPerHost: Models.DeploymentCellularChargePerHost[])
    {
        let totalBilled = ChargeItem.computeTotal(values);

        this.billed = CurrencyPipe.format(totalBilled);

        if (values.feesOverage > 0)
        {
            let overage = CurrencyPipe.format(values.feesOverage);

            this.billed = `${this.billed} (${overage} for extra data)`;
        }

        this.billedMonthly = CurrencyPipe.format(totalBilled / days * 30);
        this.billedAverage = CurrencyPipe.format(totalBilled / count);
        this.total         = NumberWithSeparatorsPipe.format(Math.trunc(values.total / (1024 * 1024)));
        this.download      = NumberWithSeparatorsPipe.format(Math.trunc(values.download / (1024 * 1024)));
        this.upload        = NumberWithSeparatorsPipe.format(Math.trunc(values.upload / (1024 * 1024)));

        let highest: Models.DeploymentCellularChargePerHost = null;

        for (let host of valuesPerHost)
        {
            if (!highest || ChargeItem.computeTotal(highest.charges) < ChargeItem.computeTotal(host.charges))
            {
                highest = host;
            }
        }

        if (highest)
        {
            this.billedHighest     = CurrencyPipe.format(ChargeItem.computeTotal(highest.charges));
            this.billedHighestHost = highest.sysId;
        }
    }

    public static computeDelta(a: Models.DeploymentCellularCharge,
                               b: Models.DeploymentCellularCharge): Models.DeploymentCellularCharge
    {
        let c = Models.DeploymentCellularCharge.newInstance(a);
        c.billed -= b.billed;
        c.fees -= b.fees;
        c.feesOverage -= b.feesOverage;
        c.total -= b.total;
        c.upload -= b.upload;
        c.download -= b.download;
        return c;
    }

    public static computeDeltaPerHost(a: Models.DeploymentCellularChargePerHost[],
                                      b: Models.DeploymentCellularChargePerHost[]): Models.DeploymentCellularChargePerHost[]
    {
        let lookup: { [key: string]: Models.DeploymentCellularChargePerHost } = {};

        for (let v of b)
        {
            lookup[v.sysId] = v;
        }

        let c = [];

        for (let v of a)
        {
            let v2 = lookup[v.sysId];
            if (v2 != null)
            {
                let n     = new Models.DeploymentCellularChargePerHost();
                n.sysId   = v.sysId;
                n.charges = ChargeItem.computeDelta(v.charges, v2.charges);
                c.push(n);
            }
        }

        return c;
    }

    private static computeTotal(values: Models.DeploymentCellularCharge): number
    {
        return values.billed + values.fees + Math.max(0, values.feesOverage); // Only consider positive overages.
    }
}

class ChargeDetails implements IDatatableDataProvider<HostDetails, HostDetails, HostDetails>
{
    private m_filter: string;

    get filter(): string
    {
        return this.m_filter;
    }

    set filter(value: string)
    {
        this.m_filter = value;

        this.table.refreshData();
    }

    table: DatatableManager<HostDetails, HostDetails, HostDetails>;
    private m_tableBound = false;

    constructor(private component: DeploymentChargesSummaryComponent,
                public readonly hosts: HostDetails[])
    {
        this.table = new DatatableManager<HostDetails, HostDetails, HostDetails>(this, () => null);
    }

    //--//

    public wasDestroyed(): boolean
    {
        return this.component.wasDestroyed();
    }

    public detectChanges()
    {
        this.component.detectChanges();
    }

    //--//

    public getTableConfigId(): string
    {
        return null;
    }

    async setColumnConfigs(configs: ColumnConfiguration[]): Promise<boolean>
    {
        return true;
    }

    async getColumnConfigs(): Promise<ColumnConfiguration[]>
    {
        return null;
    }

    public getItemName(): string
    {
        return "Hosts";
    }

    public async getList(): Promise<HostDetails[]>
    {
        return this.hosts.filter((hostDetail) =>
                                 {
                                     if (this.m_filter)
                                     {
                                         if (this.contains(hostDetail.ext.displayName))
                                         {
                                             return true;
                                         }

                                         return false;
                                     }

                                     return true;
                                 });
    }

    public async getPage(offset: number,
                         limit: number): Promise<HostDetails[]>
    {
        return this.table.slicePage(offset, limit);
    }

    public itemClicked(columnId: string,
                       item: HostDetails)
    {
        switch (columnId)
        {
            default:
            case "displayName":
                this.component.app.ui.navigation.go("/deployments", [
                    "item",
                    item.ext.model.sysId
                ]);
        }
    }

    public async transform(rows: HostDetails[]): Promise<HostDetails[]>
    {
        return rows;
    }

    private contains(val: string)
    {
        return val && val.toLowerCase()
                         .indexOf(this.m_filter.toLowerCase()) >= 0;
    }
}

class HostDetails
{
    billed: string;
    total: string;
    upload: string;
    download: string;

    constructor(public readonly ext: DeploymentHostExtended,
                values: Models.DeploymentCellularCharge)
    {
        let total = values.billed + values.fees + Math.max(0, values.feesOverage); // Only consider positive overages.

        this.billed   = CurrencyPipe.format(total);
        this.total    = NumberWithSeparatorsPipe.format(Math.trunc(values.total / (1024 * 1024)));
        this.download = NumberWithSeparatorsPipe.format(Math.trunc(values.download / (1024 * 1024)));
        this.upload   = NumberWithSeparatorsPipe.format(Math.trunc(values.upload / (1024 * 1024)));
    }
}
