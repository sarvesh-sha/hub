import {Directive, EventEmitter, Injector, Input, Output} from "@angular/core";
import {PageEvent} from "@angular/material/paginator";

import {AssetExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {AssetSelectionExtended} from "app/services/domain/report-definitions.service";
import {SearchResult, SearchResultGroup, SearchResultType} from "app/services/domain/search.service";
import * as Models from "app/services/proxy/model/models";
import {SearchBindingService} from "app/services/ui/search-binding.service";
import {Lookup} from "framework/services/utils.service";

import {Subject} from "rxjs";
import {debounceTime} from "rxjs/operators";

@Directive()
export abstract class AssetSelectorComponent extends SharedSvc.BaseApplicationComponent
{
    private readonly domain: AppDomainContext;

    private readonly m_selectionResults: Map<string, SearchResult> = new Map<string, SearchResult>();
    private m_selection: AssetSelectionExtended<any>;

    results: SearchResultGroup = new SearchResultGroup();

    term: string;

    searchTrigger = new Subject<string>();
    searching     = false;
    hasSearched   = false;

    messages: Lookup<string>;
    messageColor: string = null;

    loading = false;

    @Input() set selection(selection: AssetSelectionExtended<any>)
    {
        this.m_selection = selection;
        if (this.m_selection) this.restore();
    }

    get selection()
    {
        return this.m_selection;
    }

    @Input() flex  = false;
    @Input() limit = 50;

    @Output() onSelectionChange = new EventEmitter<Models.RecordIdentity[]>();

    constructor(inj: Injector,
                public search: SearchBindingService)
    {
        super(inj);

        this.domain = this.inject(AppDomainContext);

        this.m_selection = null;
    }

    ngOnInit()
    {
        super.ngOnInit();

        // Listen for search results
        this.subscribeToObservable(this.search.searchCompleted, () => this.displayResults());

        // Get initial message
        this.updateMessages();
    }

    ngAfterViewInit()
    {
        super.ngAfterViewInit();

        // Wire up debounced search
        this.searchTrigger.pipe(debounceTime(350))
            .subscribe(() => this.performSearch());

        // Trigger initial display
        this.onSearchChanged(this.term);
    }

    inSearchMode()
    {
        return !!this.term;
    }

    onSearchChanged(term: string)
    {
        if (term)
        {
            // If change has a search term, perform a search
            this.searchTrigger.next(term);
        }
        else
        {
            // If no search term, display already selected results
            this.displaySelected();
        }
    }

    canToggle(row: SearchResult,
              value?: boolean)
    {
        if (this.m_selection.identities.length >= this.limit)
        {
            return value == null ? row.checked : !value;
        }

        return true;
    }

    reset()
    {
        this.term = "";
        this.displaySelected();
    }

    toggleCheck(row: SearchResult,
                checked?: boolean,
                suppressEvent?: boolean)
    {
        checked = checked ?? !row.checked;
        if (this.canToggle(row, checked))
        {
            row.checked = checked;
            if (row.checked)
            {
                this.select(row);
            }
            else
            {
                this.deselect(row);
            }

            if (!suppressEvent)
            {
                this.onSelectionChange.emit(this.m_selection.identities);
            }
        }
    }

    abstract getSearchRequest(term: string): Models.SearchRequest;

    abstract getSearchResultType(): SearchResultType;

    async performSearch()
    {
        this.searching    = true;
        let searchRequest = this.getSearchRequest(this.term);

        this.loading = true;
        this.detectChanges();
        await this.search.search(searchRequest);
        this.loading = false;
        this.detectChanges();

        this.searching = false;
    }

    exit() {}

    displayResults()
    {
        let type = this.getSearchResultType();

        if (this.search.lastSearchGroups)
        {
            this.results     = this.search.lastSearchGroups.getGroup(type);
            this.hasSearched = true;
            this.searching   = this.search.searching;
            return;
        }

        let groupedResults = this.search.groupResults(this.search.lastSearchResults, this.search.lastSearchArea);
        groupedResults.limitToType(type);

        this.results     = groupedResults.getGroup(type);
        this.hasSearched = true;
        this.searching   = this.search.searching;

        this.syncSelected(this.results.displayedResults);
        this.detectChanges();
    }

    displaySelected()
    {
        let type           = this.getSearchResultType();
        let results        = Array.from(this.m_selectionResults.values());
        let groupedResults = this.search.groupResults(results, this.search.lastSearchArea);

        groupedResults.limitToType(type);

        this.results     = groupedResults.getGroup(type);
        this.hasSearched = false;
        this.searching   = false;
        this.results.setTotalByValue(results.length);
        this.detectChanges();
    }

    onPage(event: PageEvent,
           group: SearchResultGroup)
    {
        let index = event.pageIndex;
        let size  = event.pageSize;

        if (this.inSearchMode())
        {
            // Seach for the next page
            this.search.searchAdditional(group, index, size)
                .then(() =>
                      {
                          // When results are loaded sync selection state
                          this.syncSelected(this.results.displayedResults);
                          this.finishedPage();
                      });
        }
        else
        {
            // Paginate to the given results
            this.results.displayedResults = this.results.results.slice(index * size, index * size + size);
            this.finishedPage();
        }
    }

    protected finishedPage()
    {
        this.markForCheck();
    }

    private select(row: SearchResult)
    {
        // Ensure the result is in the selection results
        let result = this.m_selectionResults.get(row.id);
        if (!result)
        {
            this.m_selectionResults.set(row.id, row);
            result = row;
        }

        // Sync selection results state
        result.checked = true;

        // Mark as selected in model
        this.m_selection.select(AssetExtended.newIdentityRaw(row.id));

        // Update messages
        this.updateMessages();
    }

    private deselect(row: SearchResult)
    {
        // Remove the result from the selection results
        let result = this.m_selectionResults.get(row.id);
        if (result)
        {
            result.checked = false;
            this.m_selectionResults.delete(row.id);
        }

        // Unmark as selected in model
        this.m_selection.deselect(AssetExtended.newIdentityRaw(row.id));

        // Update messages
        this.updateMessages();
    }

    private syncSelected(results: SearchResult[])
    {
        for (let result of results)
        {
            if (this.m_selectionResults.has(result.id)) result.checked = true;
        }
    }

    private updateMessages()
    {
        this.messages = {
            emptyMessage  : "No data",
            totalMessage  : "total",
            warningMessage: `${this.m_selection.identities.length} / ${this.limit} selected`
        };

        this.messageColor = (this.m_selection.identities.length < this.limit) ? null : "#f44336";
    }

    private async restore()
    {
        // Clear all old results
        this.m_selectionResults.clear();

        // Fetch records to restore results
        this.loading = true;
        this.detectChanges();
        let results  = await this.m_selection.getRecordsAsSearchResults();
        this.loading = false;
        this.detectChanges();

        // Select each search result
        results.forEach((row) => this.select(row));

        // Display all selected results
        if (!this.inSearchMode()) this.displaySelected();
        this.detectChanges();
    }
}
