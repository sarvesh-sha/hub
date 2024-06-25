import {Directive, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import {BaseApplicationComponent} from "app/services/domain/base.service";
import {FiltersService} from "app/services/domain/filters.service";
import * as Models from "app/services/proxy/model/models";
import {FilterOverlayComponent} from "app/shared/filter/filter-overlay.component";

import {UtilsService} from "framework/services/utils.service";
import {OverlayController} from "framework/ui/overlays/overlay-base";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";

@Directive()
export abstract class FiltersAdapter<T> extends BaseApplicationComponent implements OverlayController
{
    initialized: boolean = false;

    protected m_editRequest: T;
    get editRequest(): T
    {
        return this.m_editRequest;
    }

    set editRequest(request: T)
    {
        this.m_editRequest = request;
        this.resetEditModels();
        this.editRequestUpdated();
    }

    protected m_request: T;

    @Input() set request(request: T)
    {
        if (request && this.m_request !== request)
        {
            this.m_request = request;
            if (this.initialized) this.syncNewRequest();
        }
    }

    protected m_local: boolean = false;
    @Input() set local(local: boolean)
    {
        this.m_local = local;
        if (this.initialized) this.emitNewChips();
    }

    pristine: boolean;

    protected m_hasEditFilters: boolean;
    get hasEditFilters(): boolean
    {
        if (this.m_hasEditFilters === undefined) this.m_hasEditFilters = !this.areEquivalent(this.m_editRequest, this.newRequestInstance());
        return this.m_hasEditFilters;
    }

    protected m_hasFilters: boolean;
    get hasFilters(): boolean
    {
        if (this.m_hasFilters === undefined) this.m_hasFilters = !this.areEquivalent(this.m_request, this.emptyRequestInstance());
        return this.m_hasFilters;
    }

    @ViewChild(FilterOverlayComponent) overlay: FilterOverlayComponent<T>;

    @Output() requestChange     = new EventEmitter<T>();
    @Output() filterChipsChange = new EventEmitter<FilterChip[]>();

    constructor(inj: Injector,
                protected filtersSvc: FiltersService)
    {
        super(inj);
    }

    public async ngAfterViewInit()
    {
        super.ngAfterViewInit();
        await this.filtersSvc.initialized;
        this.syncNewRequest();
        this.initialized = true;
        this.detectChanges();
        this.emitNewChips();
    }

    protected editRequestUpdated() {}

    protected abstract emptyRequestInstance(): T;

    protected abstract newRequestInstance(request?: T): T;

    protected abstract updateGlobalFilters(): void;

    protected abstract syncWithGlobalFilters(): void;

    protected abstract appendChips(chips: FilterChip[]): Promise<void>;

    protected abstract serializeFilter(input: T,
                                       output: Models.FilterPreferences): void

    protected abstract deserializeFilter(input: Models.FilterPreferences,
                                         output: T): void

    protected syncNewRequest()
    {
        if (!this.m_local) this.syncWithGlobalFilters();
        this.resetEditRequest();
        this.applyFilterEdits(true);
        this.m_hasFilters = this.m_hasEditFilters = undefined;
    }

    protected async emitNewChips()
    {
        let chips: FilterChip[] = [];
        await this.appendChips(chips);
        this.filterChipsChange.emit(chips);
    }

    // call super when overridden
    protected areEquivalent(requestA: T,
                            requestB: T): boolean
    {
        if (!requestA && !requestB) return true;
        return !!requestA && !!requestB;
    }

    public applyFilterEdits(fromLoad: boolean = false)
    {
        this.m_request = this.m_editRequest;
        this.resetEditRequest();
        if (!this.m_local && !fromLoad) this.updateGlobalFilters();
        this.requestChange.emit(this.m_request);
    }

    protected resetEditRequest()
    {
        this.editRequest = this.newRequestInstance(this.m_request);

        this.m_hasFilters = this.m_hasEditFilters = undefined;
        this.pristine     = true;
    }

    public clearFilterEdits()
    {
        this.editRequest = this.newRequestInstance();
        this.updatePristine();
    }

    // update intermediate values that contain information necessary for request (eg: pointClassIDs is an intermediate for tagsQuery filter)
    protected resetEditModels() {}

    public updatePristine(): void
    {
        this.m_hasFilters = this.m_hasEditFilters = undefined;
        this.pristine     = this.areEquivalent(this.m_request, this.m_editRequest);
    }

    public closeOverlay(): void
    {
        this.overlay.closeOverlay();
    }

    public isOpen(): boolean
    {
        return this.overlay.isOpen();
    }

    public openOverlay(): void
    {
        this.resetEditRequest();
        this.overlay.openOverlay();
    }

    public toggleOverlay(): void
    {
        this.overlay.isOpen() ? this.closeOverlay() : this.openOverlay();
    }
}

@Directive()
export abstract class LocationFiltersAdapter<T extends LocationFilter> extends FiltersAdapter<T>
{
    @Input() set excludeLocation(excludeLocation: boolean)
    {
        this.m_excludeLocation = excludeLocation;
        if (this.initialized) this.emitNewChips();
    }

    get excludeLocation(): boolean
    {
        return this.m_excludeLocation;
    }

    private m_excludeLocation: boolean = false;

    // call super if overriden
    protected updateGlobalFilters()
    {
        if (!this.excludeLocation) this.filtersSvc.locationIDs = this.m_request.locationIDs;
    }

    // call super if overriden
    protected syncWithGlobalFilters()
    {
        if (!this.excludeLocation) this.m_request.locationIDs = this.filtersSvc.locationIDs;
    }

    // call super if overriden
    protected async appendChips(chips: FilterChip[])
    {
        if (!this.excludeLocation)
        {
            chips.push(new FilterChip("Location",
                                      () =>
                                      {
                                          this.resetEditRequest();
                                          this.m_editRequest.locationIDs = [];
                                          this.applyFilterEdits();
                                      },
                                      () => this.m_request.locationIDs,
                                      await this.app.bindings.getLocationsOptions()));
        }
    }

    // call super if overriden
    protected areEquivalent(requestA: T,
                            requestB: T): boolean
    {
        if (!super.areEquivalent(requestA, requestB)) return false;

        return this.excludeLocation || UtilsService.compareArraysAsSets(requestA.locationIDs, requestB.locationIDs);
    }

    protected serializeFilter(input: T,
                              output: Models.FilterPreferences)
    {
        if (!this.excludeLocation) output.locationIDs = input.locationIDs;
    }

    protected deserializeFilter(input: Models.FilterPreferences,
                                output: T)
    {
        if (!this.excludeLocation) output.locationIDs = input.locationIDs;
    }
}

interface LocationFilter
{
    locationIDs: string[];
}
