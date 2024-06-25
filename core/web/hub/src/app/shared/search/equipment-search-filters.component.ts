import {Component, EventEmitter, Input, Output} from "@angular/core";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-equipment-search-filters",
               templateUrl: "./equipment-search-filters.component.html"
           })
export class EquipmentSearchFiltersComponent
{
    @Input()
    public model: Models.EquipmentSearchRequestFilters;

    @Input()
    public label: string = "Filters";

    @Output()
    public filtersUpdated = new EventEmitter<void>();

    static getDefaultFilters(): Models.EquipmentSearchRequestFilters
    {
        return Models.EquipmentSearchRequestFilters.newInstance({});
    }
}
