import {Component, Injector} from "@angular/core";
import {PageEvent} from "@angular/material/paginator";

import * as SharedSvc from "app/services/domain/base.service";
import {SearchResult, SearchResultGroup, SearchResultGroups, SearchResultType} from "app/services/domain/search.service";
import * as Models from "app/services/proxy/model/models";
import {SearchBindingService} from "app/services/ui/search-binding.service";
import {DeviceElementSearchFiltersComponent} from "app/shared/search/device-element-search-filters.component";
import {DatatableRowActivateEvent} from "framework/ui/datatables/datatable.component";
import {Subject} from "rxjs";
import {debounceTime, delay} from "rxjs/operators";

@Component({
               selector   : "o3-search-results-page",
               templateUrl: "./search-results-page.component.html"
           })
export class SearchResultsPageComponent extends SharedSvc.BaseComponentWithRouter
{

    searchText: string;
    searchResults: SearchResultGroups;
    searchTrigger: Subject<string> = new Subject<string>();
    searching: boolean             = false;
    hasSearched: boolean           = false;
    areaLimit: SearchResultType    = null;

    deviceElementSearchRequest = DeviceElementSearchFiltersComponent.getDefaultFilters();

    constructor(inj: Injector,
                private search: SearchBindingService)
    {
        super(inj);
    }

    ngOnInit()
    {
        super.ngOnInit();

        this.app.ui.navigation.breadcrumbCurrentLabel = "Search Results";

        this.subscribeToObservable(this.search.searchCompleted, () => this.displayResults());

        // wire up search trigger
        this.searchTrigger
            .pipe(debounceTime(333), delay(250))
            .subscribe((searchText) =>
                       {
                           if (searchText) this.performSearch();
                       });
    }

    protected async onNavigationComplete()
    {
        try
        {
            let area = this.getPathParameter("area");
            if (area)
            {
                this.areaLimit = <SearchResultType>area.toUpperCase();
            }
            else
            {
                this.areaLimit = null;
            }
        }
        catch (e)
        {
            this.areaLimit = null;
        }

        if (this.search.searching) this.searching = true;
        if (this.search.lastSearch) this.searchText = this.search.lastSearch.query;

        if (this.search.lastSearch && this.search.lastSearch.filters)
        {
            for (let filter of this.search.lastSearch.filters)
            {
                if (filter instanceof Models.DeviceElementSearchRequestFilters)
                {
                    this.deviceElementSearchRequest = filter;
                    break;
                }
            }
        }
        if (this.search.lastSearchGroups || this.search.lastSearchResults) this.displayResults();
    }

    async performSearch()
    {
        this.searching = true;
        let request    = Models.SearchRequest.newInstance({
                                                              query         : this.searchText,
                                                              scopeToFilters: false,
                                                              filters       : []
                                                          });
        if (this.hasDeviceElementFilters)
        {
            request.filters.push(this.deviceElementSearchRequest);
        }

        await this.search.search(request);
        this.searching = false;
    }

    get hasDeviceElementFilters(): boolean
    {
        return this.deviceElementSearchRequest &&
               (this.deviceElementSearchRequest.hasAnySampling ||
                this.deviceElementSearchRequest.hasNoSampling ||
                this.deviceElementSearchRequest.isClassified ||
                this.deviceElementSearchRequest.isUnclassified);
    }

    displayResults()
    {
        if (this.search.lastSearchGroups)
        {
            this.searchResults = this.search.lastSearchGroups;
            this.hasSearched   = true;
            this.searching     = this.search.searching;
            return;
        }

        // get grouped results
        let groupedResults = this.search.groupResults(this.search.lastSearchResults, this.search.lastSearchArea);

        // limit to area (if specified)
        if (this.areaLimit) groupedResults.limitToType(this.areaLimit);

        // clear area limit after first application
        this.areaLimit = null;

        if (!this.searchText)
        {
            this.searchText = this.search.lastSearch.query;
        }
        this.searchResults = groupedResults;
        this.hasSearched   = true;
        this.searching     = this.search.searching;
    }

    onPage(event: PageEvent,
           group: SearchResultGroup)
    {
        this.search.searchAdditional(group, event.pageIndex, event.pageSize);
    }

    viewResult(event: DatatableRowActivateEvent<SearchResult>)
    {
        this.app.ui.navigation.go(event.row.url);
    }

    toggle(panel: any)
    {
        panel.toggle();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}

