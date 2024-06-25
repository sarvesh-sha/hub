import {Component, EventEmitter, Input, Output} from "@angular/core";
import {FormControl} from "@angular/forms";

import {TestGroup} from "app/test-selector/test-selector.component";

@Component({
               selector   : "o3-test-category-filter",
               templateUrl: "./test-category-filter.component.html"
           })
export class TestCategoryFilterComponent
{
    availableCategories: string[];
    categories: string[];
    selectedCategories = new Set<string>();

    @Input() set testGroups(testGroups: TestGroup[])
    {
        this.availableCategories = testGroups.map((group) => group.category);
        this.selectedCategories  = new Set(this.availableCategories);
        this.categories          = Array.from(this.selectedCategories);
    }

    get categorySelectMessage(): string
    {
        if (this.categories.length > 1)
        {
            return `${this.categories.length} selected`;
        }

        return this.categories[0];
    }

    @Output() filteredCategoriesUpdated = new EventEmitter<Set<string>>();

    updateSelectedCategories()
    {
        this.selectedCategories = new Set(this.categories);
        this.filteredCategoriesUpdated.emit(this.selectedCategories);
    }
}
