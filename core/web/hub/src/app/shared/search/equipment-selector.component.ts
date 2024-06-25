import {ChangeDetectionStrategy, Component} from "@angular/core";
import {AssetSelectorComponent} from "app/shared/search/asset-selector.component";
import {EquipmentSearchFiltersComponent} from "app/shared/search/equipment-search-filters.component";
import {SearchResultType} from "app/services/domain/search.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector       : "o3-equipment-selector",
               templateUrl    : "./equipment-selector.component.html",
               styleUrls      : ["./asset-selector.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class EquipmentSelectorComponent extends AssetSelectorComponent
{
    searchRequestFilters                = EquipmentSearchFiltersComponent.getDefaultFilters();
    searchRequest: Models.SearchRequest = Models.SearchRequest.newInstance({
                                                                               filters       : [this.searchRequestFilters],
                                                                               scopeToFilters: true
                                                                           });

    public getSearchRequest(term: string): Models.SearchRequest
    {
        this.searchRequest.query   = term;
        this.searchRequest.filters = [this.searchRequestFilters];
        return this.searchRequest;
    }

    public getSearchResultType(): SearchResultType
    {
        return SearchResultType.EQUIPMENT;
    }
}
