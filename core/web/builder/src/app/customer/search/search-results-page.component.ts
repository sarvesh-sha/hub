import {Component, Injector} from "@angular/core";
import {PageEvent} from "@angular/material/paginator";

import * as SharedSvc from "app/services/domain/base.service";
import {SearchResult, SearchResultGroup, SearchResultGroups, SearchResultType} from "app/services/domain/search.service";
import * as Models from "app/services/proxy/model/models";
import {SearchBindingService} from "app/services/ui/search-binding.service";
import {DatatableRowActivateEvent} from "framework/ui/datatables/datatable.component";

@Component({
               selector   : "o3-search-results-page",
               templateUrl: "./search-results-page.component.html"
           })
export class SearchResultsPageComponent extends SharedSvc.BaseComponentWithRouter
{
    searchText: string;
    searchResults: SearchResultGroups;
    searching: boolean           = false;
    hasSearched: boolean         = false;
    areaLimit: SearchResultType  = null;

    constructor(inj: Injector,
                public search: SearchBindingService)
    {
        super(inj);
    }

    ngOnInit()
    {
        super.ngOnInit();

        this.app.ui.navigation.breadcrumbCurrentLabel = "Search Results";

        this.subscribeToObservable(this.search.searchCompleted, () => this.displayResults());
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
        if (this.search.lastSearchGroups || this.search.lastSearchResults) this.displayResults();
    }

    async performSearch()
    {
        this.searching = true;
        let request: Models.SearchRequest;
//        if (this.hasDeviceElementFilters)
//        {
//            this.deviceElementSearchRequest.query = this.searchText;
//
//            request = this.deviceElementSearchRequest;
//        }
//        else
        {
            request = Models.SearchRequest.newInstance({query: this.searchText});
        }

        await this.search.search(request);
        this.searching = false;
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

        this.searchText    = this.search.lastSearch.query;
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

