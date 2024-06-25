import {Component, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import {BaseApplicationComponent} from "app/services/domain/base.service";
import {FiltersService} from "app/services/domain/filters.service";
import * as Models from "app/services/proxy/model/models";

import {DialogPromptComponent} from "framework/ui/dialogs/dialog-prompt.component";
import {OverlayController} from "framework/ui/overlays/overlay-base";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {TabActionDirective} from "framework/ui/shared/tab-action.directive";

@Component({
               selector   : "o3-filter-overlay[pristine][resetFilterButtonDisabled]",
               templateUrl: "./filter-overlay.component.html"
           })
export class FilterOverlayComponent<T> extends BaseApplicationComponent implements OverlayController
{
    @Input() pristine: boolean;
    @Input() resetFilterButtonDisabled: boolean;

    @Input() isStep: boolean = false;

    @Input() serializer: FilterSerializable<T>;

    @ViewChild(StandardFormOverlayComponent) overlay: StandardFormOverlayComponent;

    @Output() submitted     = new EventEmitter<void>();
    @Output() filterApplied = new EventEmitter<void>();
    @Output() filterCleared = new EventEmitter<void>();

    overlayConfig = OverlayConfig.onTopDraggable({
                                                     width    : 325,
                                                     maxHeight: "90vh"
                                                 });

    savedFilters: Models.FilterPreferences[] = [];
    actions: TabActionDirective[]            = [];

    constructor(inj: Injector,
                private filtersSvc: FiltersService)
    {
        super(inj);
    }

    public closeOverlay(): void
    {
        this.overlay?.closeOverlay();
    }

    public isOpen(): boolean
    {
        return this.overlay?.isOpen();
    }

    public openOverlay(): void
    {
        this.overlay?.openOverlay();
        this.initFilters();
    }

    public toggleOverlay(open?: boolean): void
    {
        this.overlay?.toggleOverlay(open);
    }

    public primaryLabel(): string
    {
        return this.isStep ? "Next" : "Apply";
    }

    public primaryDisabled(): boolean
    {
        return this.isStep ? false : this.pristine;
    }

    public primaryClose(): boolean
    {
        return this.isStep;
    }

    public async initFilters()
    {
        this.savedFilters = [];
        this.actions      = [];

        // Do nothing if there is not serializer/deserializer pair or getter/setter pair given
        if (!this.serializer) return;

        // Wait for filter service to initialize
        await this.filtersSvc.initialized;

        // Add the save new filter action
        let makeNew             = new TabActionDirective();
        makeNew.label           = "New...";
        makeNew.labelFirstLevel = "Save As";
        makeNew.callback        = () => this.saveFilter();
        this.actions.push(makeNew);


        // Load existing saved filters
        this.savedFilters = this.filtersSvc.savedFilters;

        // Make actions for all filters
        for (let filter of this.savedFilters)
        {
            let saveAction   = new TabActionDirective();
            let loadAction   = new TabActionDirective();
            let deleteAction = new TabActionDirective();

            // Add an action to save to this filter
            this.actions.push(saveAction);
            saveAction.label           = filter.name;
            saveAction.labelFirstLevel = "Save As";
            saveAction.callback        = () => this.updateFilter(filter);

            // Add an action to load this filter
            this.actions.push(loadAction);
            loadAction.label           = filter.name;
            loadAction.labelFirstLevel = "Load";
            loadAction.callback        = () => this.loadFilter(filter);

            // Add an action to delete this filter
            this.actions.push(deleteAction);
            deleteAction.label           = filter.name;
            deleteAction.labelFirstLevel = "Delete";
            deleteAction.callback        = () => this.deleteFilter(filter);
        }
    }

    public loadFilter(filter: Models.FilterPreferences)
    {
        // Deserialize and set the filter
        this.serializer.setter(this.serializer.deserializer(filter));
        this.app.framework.errors.success(`Saved filter "${filter.name}" has been applied`, -1);
    }

    public async deleteFilter(filter: Models.FilterPreferences)
    {
        // Delete the filter
        await this.filtersSvc.deleteFilter(filter);
        this.app.framework.errors.success(`Deleted saved filter "${filter.name}"`, -1);

        // Reload filters
        await this.initFilters();
    }

    public async updateFilter(oldFilter: Models.FilterPreferences)
    {
        // Get and serialize the filter state
        let newFilter = this.serializer.serializer(this.serializer.getter());

        // Update the filter
        await this.filtersSvc.updateFilter(oldFilter, newFilter);
        this.app.framework.errors.success(`Updated saved filter "${oldFilter.name}"`, -1);

        // Reload filters
        await this.initFilters();
    }


    public async saveFilter()
    {
        DialogPromptComponent.execute(this, "Save Filters", "Enter a name for the new filter")
                             .then(async (name) =>
                                   {
                                       // Only process if name given
                                       if (name)
                                       {
                                           // Get and serialize filter state
                                           let filter = this.serializer.serializer(this.serializer.getter());

                                           // Save the new filter
                                           await this.filtersSvc.saveFilter(filter, name);
                                           this.app.framework.errors.success(`Saved new filter as "${filter.name}"`, -1);

                                           // Reload filters
                                           await this.initFilters();
                                       }
                                   });

    }
}

export interface FilterSerializable<T>
{
    deserializer: (filter: Models.FilterPreferences) => T;
    serializer: (filter: T) => Models.FilterPreferences;
    getter: () => T;
    setter: (filter: T) => void;
}
