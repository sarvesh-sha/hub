import {Injectable} from "@angular/core";

import {SearchResult, SearchResultGroup, SearchResultGroups, SearchResultType, SearchService} from "app/services/domain/search.service";

import * as Models from "app/services/proxy/model/models";

import {Subject} from "rxjs";

@Injectable()
export class SearchBindingService
{
    /**
     * The last search performed.
     */
    lastSearch: Models.SearchRequest;

    /**
     * The last search area used.
     */
    lastSearchArea: string;

    /**
     * The results of last search performed.
     */
    lastSearchResults: SearchResult[];

    /**
     * The result set of last search performed.
     */
    lastSearchResultSet: Models.SearchResultSet;

    /**
     * True when a search is being performed.
     */
    searching: boolean = false;

    /**
     * Raised when a search completes
     */
    searchCompleted: Subject<void> = new Subject<void>();

    /**
     * The number of results to return for a given search.
     */
    limit: number = 10;

    lastSearchGroups: SearchResultGroups;

    /**
     * Constructor
     */
    constructor(private searchService: SearchService)
    {
    }

    async search(searchRequest: Models.SearchRequest): Promise<SearchResult[]>
    {
        this.lastSearch          = searchRequest;
        this.lastSearchResults   = [];
        this.lastSearchResultSet = null;
        this.lastSearchGroups    = null;

        if (searchRequest.query)
        {
            this.searching = true;

            this.lastSearchResultSet = await this.searchService.getSearchResultSet(this.lastSearch, this.limit, 0);

            this.lastSearchResults = await this.searchService.getSearchResults(this.lastSearchResultSet);

            this.searching = false;
        }
        this.searchCompleted.next();

        return this.lastSearchResults;
    }

    async searchAdditional(group: SearchResultGroup,
                           pageIndex: number,
                           pageSize: number)
    {
        if (this.lastSearch)
        {
            group.pageIndex = pageIndex;
            group.pageSize  = pageSize;

            this.searching = true;

            this.lastSearchResultSet = await this.searchService.getSearchResultSet(this.lastSearch, group.pageSize, group.pageIndex * group.pageSize);

            let results = await this.searchService.getSearchResults(this.lastSearchResultSet);

            this.searching = false;

            group.results = this.filter(results, group.type);

            group.displayedResults = group.results;
        }
    }

    async searchAndLimit(searchRequest: Models.SearchRequest,
                         limitPerCategory: number = 3,
                         searchArea?: string): Promise<SearchResult[]>
    {
        this.lastSearchArea = searchArea;

        let results: SearchResult[]        = [];
        let initialResults: SearchResult[] = await this.search(searchRequest);

        let groups: SearchResultGroups = this.searchService.getSearchGroups(searchArea);
        groups.setTotals(this.lastSearchResultSet);

        for (let group of groups.groups)
        {
            let filtered = this.filterAndLimit(initialResults, group.type, group.name, group.total, limitPerCategory);
            results      = results.concat(filtered);
        }
        return results;
    }

    async searchAndGroup(searchRequest: Models.SearchRequest,
                         searchArea?: string): Promise<SearchResultGroups>
    {
        this.lastSearchArea = searchArea;

        let results = await this.search(searchRequest);

        return this.groupResults(results, searchArea);
    }

    groupResults(results: SearchResult[],
                 searchArea?: string): SearchResultGroups
    {
        let groups: SearchResultGroups = this.searchService.getSearchGroups(searchArea);
        groups.setTotals(this.lastSearchResultSet);

        for (let group of groups.groups)
        {
            group.results = this.filterAndLimit(results, group.type, group.name, group.total, 1000);
        }

        groups.showFirstN(this.limit);

        this.lastSearchGroups = groups;

        return groups;
    }

    private filter(results: SearchResult[],
                   resultType: SearchResultType): SearchResult[]
    {
        return results.filter((result) =>
                              {
                                  return result.type == resultType;
                              });
    }

    private filterAndLimit(results: SearchResult[],
                           resultType: SearchResultType,
                           resultTypeName: string,
                           total: number,
                           limit: number = 3): SearchResult[]
    {
        let filtered = this.filter(results, resultType);
        if (filtered.length > limit)
        {
            let limited: SearchResult[] = [];
            for (let i = 0; i < limit; i++)
            {
                limited.push(filtered[i]);
            }
            limited.push({
                             type     : resultType,
                             text     : `View All ${total} ${resultTypeName} Results`,
                             url      : `/search/${resultType.toString()
                                                             .toLowerCase()}`,
                             id       : "SUMMARY",
                             isSummary: true
                         });
            return limited;
        }
        else
        {
            return filtered;
        }
    }
}
