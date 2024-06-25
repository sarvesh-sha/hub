import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import {CustomerServiceExtended, CustomerServicesService} from "app/services/domain/customer-services.service";
import {CustomerExtended, CustomersService} from "app/services/domain/customers.service";
import {DeploymentHostExtended, DeploymentHostsService} from "app/services/domain/deployment-hosts.service";
import {SettingsService} from "app/services/domain/settings.service";
import * as Models from "app/services/proxy/model/models";

import {ErrorService} from "framework/services/error.service";

@Injectable()
export class SearchService
{
    /**
     * Constructor
     */
    constructor(private errors: ErrorService,
                private api: ApiService,
                private customers: CustomersService,
                private customerServices: CustomerServicesService,
                private deploymentHosts: DeploymentHostsService,
                private settings: SettingsService)
    {
    }

    async getSearchResultSet(request: Models.SearchRequest,
                             limit: number,
                             offset: number = 0): Promise<Models.SearchResultSet>
    {
        try
        {
            request.query = request.query || "";
            request.query = request.query.trim();

            if (request.query == "")
            {
                return null;
            }

            return await this.api.search.search(request, offset, limit);
        }
        catch (err)
        {
            this.errors.error(err.code, err.message);
            return null;
        }
    }

    async getSearchResults(searchResult: Models.SearchResultSet): Promise<SearchResult[]>
    {
        let results: SearchResult[] = [];

        if (searchResult)
        {
            // get all alerts and devices
            let [
                    customerList, customerServiceList, deploymentHostList
                ] = await Promise.all([
                                          this.customers.getExtendedBatch(searchResult.customers),
                                          this.customerServices.getExtendedBatch(searchResult.customerServices),
                                          this.deploymentHosts.getExtendedBatch(searchResult.deploymentHosts)
                                      ]);

            // TODO: Make user service follow same pattern as the rest.
            let userIds                 = searchResult.users.map((ri) => ri.sysId);
            let userList: Models.User[] = [];
            if (userIds.length > 0)
            {
                userList = (await this.settings.getUsersList()).filter((u) => userIds.indexOf(u.sysId) >= 0);
            }

            await Promise.all([
                                  this.getCustomerResults(customerList, results),
                                  this.getCustomerServiceResults(customerServiceList, results),
                                  this.getDeploymentHostResults(deploymentHostList, results),
                                  this.getUserResults(userList, results)
                              ]);
        }

        return results;
    }

    async getCustomerResults(lst: CustomerExtended[],
                             result: SearchResult[])
    {
        for (let item of lst)
        {
            let sr  = new SearchResult();
            sr.type = SearchResultType.CUSTOMER;
            sr.id   = item.model.sysId;
            sr.text = item.model.name;
            sr.url  = "/customers/item/" + item.model.sysId;

            result.push(sr);
        }
    }

    async getCustomerServiceResults(lst: CustomerServiceExtended[],
                                    result: SearchResult[])
    {
        for (let item of lst)
        {
            let cust = await item.getOwningCustomer();

            let sr  = new SearchResult();
            sr.type = SearchResultType.CUSTOMER_SERVICE;
            sr.id   = item.model.sysId;
            sr.text = item.model.name;
            sr.url  = "/customers/item/" + cust.model.sysId + "/service/" + item.model.sysId;

            result.push(sr);
        }
    }

    async getDeploymentHostResults(lst: DeploymentHostExtended[],
                                   result: SearchResult[])
    {
        for (let item of lst)
        {
            let sr  = new SearchResult();
            sr.type = SearchResultType.DEPLOYMENT_HOST;
            sr.id   = item.model.sysId;
            sr.text = `${item.model.hostId} / ${item.model.hostName}`;
            sr.url  = "/deployments/item/" + item.model.sysId;

            result.push(sr);
        }
    }

    getUserResults(userList: Models.User[],
                   result: SearchResult[])
    {
        // filter devices
        for (let match of userList)
        {
            result.push({
                            type   : SearchResultType.USER,
                            id     : match.sysId,
                            text   : `${match.firstName} ${match.lastName}`,
                            subtext: match.emailAddress,
                            url    : "/configuration/users/user/" + match.sysId
                        });
        }
    }

    getSearchGroups(searchArea: string): SearchResultGroups
    {
        let groups: SearchResultGroups = new SearchResultGroups();

        if (searchArea == "customer")
        {
            groups.push(SearchResultType.CUSTOMER, "Customer");
            groups.push(SearchResultType.CUSTOMER_SERVICE, "Customer Service");
            groups.push(SearchResultType.DEPLOYMENT_HOST, "Deployment Host");
        }
        else if (searchArea == "customerService")
        {
            groups.push(SearchResultType.CUSTOMER_SERVICE, "Customer Service");
            groups.push(SearchResultType.CUSTOMER, "Customer");
            groups.push(SearchResultType.DEPLOYMENT_HOST, "Deployment Host");
        }
        else
        {
            groups.push(SearchResultType.DEPLOYMENT_HOST, "Deployment Host");
            groups.push(SearchResultType.CUSTOMER_SERVICE, "Customer Service");
            groups.push(SearchResultType.CUSTOMER, "Customer");
        }

        return groups;
    }

    private handleErrors<T>(promise: Promise<T>): Promise<T>
    {
        promise.catch(error =>
                      {
                          this.errors.error(error.code, error.message);
                      });

        return promise;
    }

    private joinIfNotNull(...array: string[]): string
    {
        return array.filter((n) => !!n)
                    .join(" - ");
    }
}

export class SearchResultGroup
{
    type: SearchResultType;
    name: string;
    results: SearchResult[]          = [];
    displayedResults: SearchResult[] = [];
    total: number;
    initiallyExpanded: boolean       = true;

    pageIndex: number = 0;
    pageSize: number  = 10;

    public get hasResults(): boolean
    {
        return this.results.length > 0;
    }

    public get remainingCount(): number
    {
        if (this.total - this.displayedResults.length > 0)
        {
            return this.total - this.displayedResults.length;
        }

        return 0;
    }

    setTotal(resultSet: Models.SearchResultSet)
    {
        if (!resultSet) return;

        switch (this.type)
        {
            case SearchResultType.CUSTOMER:
                this.total = resultSet.totalCustomers;
                break;

            case SearchResultType.CUSTOMER_SERVICE:
                this.total = resultSet.totalCustomerServices;
                break;

            case SearchResultType.DEPLOYMENT_HOST:
                this.total = resultSet.totalDeploymentHosts;
                break;

            case SearchResultType.USER:
                this.total = resultSet.totalUsers;
                break;

            default:
                this.total = 0;
                break;
        }
    }

    showFirstN(n: number)
    {
        for (let i = 0; i < n && i < this.results.length; i++)
        {
            this.displayedResults.push(this.results[i]);
        }
    }
}

export class SearchResultGroups
{
    groups: SearchResultGroup[] = [];

    get hasResults(): boolean
    {
        for (let group of this.groups)
        {
            if (group.hasResults)
            {
                return true;
            }
        }

        return false;
    }

    get countOfGroupsWithResults(): number
    {
        let count = 0;
        for (let group of this.groups)
        {
            if (group.hasResults) count++;
        }

        return count;
    }

    push(type: SearchResultType,
         name: string)
    {
        let group  = new SearchResultGroup();
        group.type = type;
        group.name = name;
        this.groups.push(group);
    }

    showFirstN(n: number = 3)
    {
        for (let group of this.groups)
        {
            group.showFirstN(n);
        }
    }

    getGroup(type: SearchResultType): SearchResultGroup
    {
        return this.groups.find(g => g.type === type);
    }

    limitToType(type: SearchResultType): SearchResultGroups
    {
        for (let group of this.groups)
        {
            group.initiallyExpanded = group.type == type;
        }

        return this;
    }

    setTotals(resultSet: Models.SearchResultSet)
    {
        if (!resultSet) return;

        for (let group of this.groups)
        {
            group.setTotal(resultSet);
        }
    }
}

export class SearchResult
{
    type: SearchResultType;
    id: string;
    text: string;
    subtext?: string;
    url: string;
    isSummary?: boolean = false;
    checked?: boolean;
}

const SearchResultTypePrivate = {
    CUSTOMER        : "CUSTOMER",
    CUSTOMER_SERVICE: "CUSTOMER_SERVICE",
    DEPLOYMENT_HOST : "DEPLOYMENT_HOST",
    USER            : "USER",
    SEARCHALL       : "SEARCHALL"
};

export type SearchResultType = keyof typeof SearchResultTypePrivate;

export const SearchResultType: { [P in SearchResultType]: P } = <any>SearchResultTypePrivate;
