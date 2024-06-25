import {ChangeDetectionStrategy, Component, ViewChild} from "@angular/core";

import {SearchResult, SearchResultType, SearchService} from "app/services/domain/search.service";
import * as Models from "app/services/proxy/model/models";
import {SearchBindingService} from "app/services/ui/search-binding.service";
import {AssetSelectorComponent} from "app/shared/search/asset-selector.component";
import {DeviceElementSearchFiltersComponent} from "app/shared/search/device-element-search-filters.component";
import {DatatableComponent} from "framework/ui/datatables/datatable.component";

@Component({
               selector       : "o3-control-point-selector",
               templateUrl    : "./control-point-selector.component.html",
               styleUrls      : ["./asset-selector.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush,
               providers      : [
                   {
                       provide   : SearchBindingService,
                       useFactory: (searchSvc: SearchService) => new SearchBindingService(searchSvc),
                       deps      : [SearchService]
                   }
               ]
           })
export class ControlPointSelectorComponent extends AssetSelectorComponent
{
    searchRequestFilters                = DeviceElementSearchFiltersComponent.getDefaultFilters();
    searchRequest: Models.SearchRequest = Models.SearchRequest.newInstance({
                                                                               filters       : [this.searchRequestFilters],
                                                                               scopeToFilters: true
                                                                           });

    @ViewChild("test_controlPointDataTable") test_cpDataTable: DatatableComponent<any, any, any>;

    private m_allSelected = false;
    get allSelected(): boolean
    {
        return this.results.displayedResults.length && this.m_allSelected;
    }

    set allSelected(checked: boolean)
    {
        this.m_allSelected        = checked;
        this.indeterminateChecked = false;

        for (let result of this.results.displayedResults)
        {
            this.toggleCheck(result, checked, true);
        }

        this.onSelectionChange.emit(this.selection.identities);
    }

    indeterminateChecked: boolean;
    canSelectAll: boolean;

    @ViewChild("test_searchFilters") test_searchFilters: DeviceElementSearchFiltersComponent;

    public ngOnInit()
    {
        // Restore filters from last search
        for (let filter of this.search.lastSearch?.filters || [])
        {
            if (filter instanceof Models.DeviceElementSearchRequestFilters)
            {
                this.searchRequestFilters = filter;
                break;
            }
        }

        super.ngOnInit();
    }

    public displaySelected()
    {
        super.displaySelected();
        this.updateSelectAll();
    }

    public displayResults()
    {
        super.displayResults();
        this.updateSelectAll();
    }

    public toggleCheck(row: SearchResult,
                       checked?: boolean,
                       suppressEvent?: boolean)
    {
        super.toggleCheck(row, checked, suppressEvent);

        if (!suppressEvent) this.updateSelectAll();
    }

    protected finishedPage()
    {
        super.finishedPage();
        this.updateSelectAll();
    }

    private updateSelectAll()
    {
        let numSelected = 0;
        for (let result of this.results.displayedResults)
        {
            if (result.checked) numSelected++;
        }

        this.m_allSelected        = numSelected === this.results.displayedResults.length;
        this.indeterminateChecked = !this.m_allSelected && numSelected > 0;
        this.canSelectAll         = this.selection.identities.length + this.results.displayedResults.length - numSelected <= this.limit;
    }

    public getSearchRequest(term: string): Models.SearchRequest
    {
        this.searchRequest.query   = term;
        this.searchRequest.filters = [this.searchRequestFilters];
        return this.searchRequest;
    }

    public getSearchResultType(): SearchResultType
    {
        return SearchResultType.DEVICE_ELEMENT;
    }
}
