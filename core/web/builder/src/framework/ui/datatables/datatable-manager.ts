import {EventEmitter} from "@angular/core";
import {PageEvent} from "@angular/material/paginator";
import {SortDirection} from "@angular/material/sort";
import {UtilsService} from "framework/services/utils.service";

import {ViewState} from "framework/ui/navigation/view-state.service";
import {Future} from "framework/utils/concurrency";

export class DatatableManager<K, E, T>
{
    loading: boolean = false;

    config = {
        messages: {
            emptyMessage  : "Loading...",
            totalMessage  : "total",
            warningMessage: ""
        }
    };

    private m_all: K[] = [];

    private m_rows: T[] = [];

    get rows(): T[]
    {
        return this.m_rows;
    }

    detectChanges = new EventEmitter<void>();

    //--//

    private m_offset: number;
    private m_listInvalidated = true;
    private m_pageInvalidated = true;
    private m_fetchingData    = false;

    get count(): number
    {
        return this.m_all.length;
    }

    get offset(): number
    {
        if (this.m_offset === undefined)
        {
            this.m_offset = this.getViewState("TABLE_OFFSET", 0);
        }

        return this.m_offset;
    }

    set offset(value: number)
    {
        this.m_offset = value;

        this.setViewState("TABLE_OFFSET", value);
    }

    //--//

    public defaultLimit = 10;

    private m_limit: number;

    get limit(): number
    {
        if (this.m_limit === undefined)
        {
            this.m_limit = Math.max(10, this.getViewState("TABLE_LIMIT", this.defaultLimit));
        }

        return this.m_limit;
    }

    set limit(value: number)
    {
        if (value > 0)
        {
            this.m_limit = value;

            this.setViewState("TABLE_LIMIT", value);
        }
    }

    //--//

    private m_sort: DatatableSort[];

    get sort(): DatatableSort[]
    {
        if (!this.m_sort)
        {
            this.m_sort = this.getViewState<DatatableSort[]>("TABLE_SORT");
        }

        return this.m_sort;
    }

    set sort(value: DatatableSort[])
    {
        this.m_sort = value;

        let oldSort = this.getViewState<DatatableSort[]>("TABLE_SORT");
        if (!UtilsService.compareJson(oldSort, value))
        {
            this.offset = 0;
        }

        if (this.m_sort)
        {
            this.setViewState("TABLE_SORT", value);
        }
    }

    //--//

    private m_suspendRefresh: Promise<void>;

    //--//

    constructor(private provider: IDatatableDataProvider<K, E, T>,
                private viewStateAccessor: () => ViewState)
    {
        let itemName                      = this.provider.getItemName();
        this.config.messages.emptyMessage = `Loading ${itemName}...`;
    }

    private setViewState(key: string,
                         value: any)
    {
        let viewState = this.viewStateAccessor();
        if (viewState) viewState.set(key, value, true);
    }

    private getViewState<T>(key: string,
                            defaultValue: T = null): T
    {
        let viewState = this.viewStateAccessor();
        return (viewState && viewState.get(key, defaultValue)) || defaultValue;
    }

    resetPagination()
    {
        this.offset = 0;
    }

    getTableConfigId(): string
    {
        return this.provider.getTableConfigId();
    }

    getConfigs(): Promise<ColumnConfiguration[]>
    {
        return this.provider.getColumnConfigs();
    }

    async setConfigs(configs: ColumnConfiguration[]): Promise<boolean>
    {
        return this.provider.setColumnConfigs(configs);
    }

    private m_selectionManager: DatatableSelectionManager<K, T, any>;

    set selectionManager(selectionManager: DatatableSelectionManager<K, T, any>)
    {
        this.m_selectionManager = selectionManager;
    }

    get selectionManager(): DatatableSelectionManager<K, T, any>
    {
        return this.m_selectionManager;
    }

    enableSimpleSelection<ID>(keyConverter: (key: K) => ID,
                              valueConverter: (value: T) => ID,
                              single        = false,
                              showSelectAll = true): SimpleSelectionManager<K, T, ID>
    {
        let manager           = new SimpleSelectionManager(this, single, showSelectAll, keyConverter, valueConverter);
        this.selectionManager = manager;
        return manager;
    }

    private m_expansionManager: DatatableExpansionManager<K, T, any>;

    set expansionManager(expansionManager: DatatableExpansionManager<K, T, any>)
    {
        this.m_expansionManager = expansionManager;
    }

    get expansionManager(): DatatableExpansionManager<K, T, any>
    {
        return this.m_expansionManager;
    }

    enableSimpleExpansion<ID>(keyConverter: (key: K) => ID,
                              valueConverter: (value: T) => ID,
                              single        = false,
                              showExpandAll = true): SimpleExpansionManager<K, T, ID>
    {
        let manager           = new SimpleExpansionManager(this, single, showExpandAll, keyConverter, valueConverter);
        this.expansionManager = manager;
        return manager;
    }

    async refreshData()
    {
        // Invalidate the data in flight.
        this.m_listInvalidated = true;
        this.m_pageInvalidated = true;

        await this.refreshDataInner();
    }

    async onPage(event: PageEvent)
    {
        this.limit  = event.pageSize;
        this.offset = event.pageIndex;

        await this.refreshPage();
    }

    async refreshPage()
    {
        // Invalidate the data in flight.
        this.m_pageInvalidated = true;

        await this.refreshDataInner();
    }

    private async refreshDataInner()
    {
        if (this.isRefreshDisabled) return;

        if (!this.m_fetchingData)
        {
            this.m_fetchingData = true;

            // We delay a bit to let all the filters and configurations to be applied to the table.
            await Future.delayed(50);

            // Only show the loading indicator if it takes more than half a second.
            let timer = setTimeout(() =>
                                   {
                                       this.loading = true;
                                       this.detectChanges.emit();
                                   }, 500);

            try
            {
                //
                // Other activities could interleave with refreshData and refreshPage.
                // We use a counter to ensure we only refresh the page if we have rows from the current list.
                //
                while (this.m_listInvalidated || this.m_pageInvalidated)
                {
                    if (this.provider.wasDestroyed()) return;

                    if (this.m_listInvalidated)
                    {
                        this.m_listInvalidated = false;

                        //--//

                        this.m_all = await this.provider.getList() || [];
                        if (this.m_listInvalidated)
                        {
                            await Future.delayed(50);
                            continue; // Data has changed, retry.
                        }

                        if (this.selectionManager) this.selectionManager.update();

                        let itemName                      = this.provider.getItemName();
                        this.config.messages.emptyMessage = `No ${(itemName || "Items")} to display.`;
                        this.config.messages.totalMessage = `Total ${(itemName || "Items")}`;
                        this.detectChanges.emit();
                    }

                    if (this.m_pageInvalidated)
                    {
                        this.m_pageInvalidated = false;

                        //--//

                        // Make sure we always stay inside the range of available entries.
                        let count  = this.count;
                        let offset = Math.min(this.offset, Math.trunc(count / this.limit));
                        if (this.offset != offset)
                        {
                            this.offset = offset;
                            this.detectChanges.emit();
                        }

                        let expected = Math.min(count - (this.offset * this.limit), this.limit);

                        let rowsRaw = await this.provider.getPage(this.offset, this.limit);
                        if (rowsRaw.length != expected)
                        {
                            this.m_listInvalidated = true;
                            this.m_pageInvalidated = true;
                            await Future.delayed(50);
                            continue; // Data has changed, retry.
                        }

                        let rows = await this.provider.transform(rowsRaw);

                        if (this.m_listInvalidated || this.m_pageInvalidated) continue; // Data has changed, retry.

                        let start = this.offset * this.limit;

                        for (let r = 0; r < rows.length; r++)
                        {
                            (<DatatableRow<T>>rows[r]).rowNumber = start + r + 1;
                        }

                        this.m_rows = rows;
                        this.detectChanges.emit();
                    }
                }
            }
            catch (e)
            {
                // Ignore errors.
                console.error(e);
            }
            finally
            {
                clearTimeout(timer);

                this.m_fetchingData = false;

                this.loading = false;
                this.detectChanges.emit();
            }
        }
    }

    onSort(sort: DatatableSort[])
    {
        this.sort = sort;
        this.refreshData();
    }

    rowClicked(columnId: string,
               row: T)
    {
        this.provider.itemClicked(columnId, row);
    }

    indexOfKey(key: K)
    {
        return this.m_all.indexOf(key);
    }

    slicePage(offset: number,
              limit: number): K[]
    {
        return this.slice(offset * limit, limit);
    }

    slice(offset: number,
          limit: number): K[]
    {
        const length = this.count;
        const start  = offset;
        const end    = Math.min(start + limit, length);

        let keys: K[] = [];

        for (let i = start; i < end; i++)
        {
            let key = this.m_all[i];
            if (key) keys.push(key);
        }

        return keys;
    }

    get isRefreshDisabled(): boolean
    {
        return !!this.m_suspendRefresh;
    }

    async disableRefreshWhileProcessing(callback: () => Promise<void>)
    {
        await this.waitForSuspension();

        this.m_suspendRefresh = this.disableRefreshWhileProcessingInner(callback);

        await this.waitForSuspension();
        await this.refreshData();
    }

    private async disableRefreshWhileProcessingInner(callback: () => Promise<void>)
    {
        try
        {
            await callback();
        }
        catch (e)
        {
            // Sink all errors.
        }

        // Add a little delay, to allow database change notifications to trickle through.
        await Future.delayed(100);
    }

    private async waitForSuspension()
    {
        let suspension = this.m_suspendRefresh;
        while (suspension)
        {
            await suspension;

            if (suspension == this.m_suspendRefresh)
            {
                // Only reset if it hasn't changed.
                this.m_suspendRefresh = null;
                break;
            }

            suspension = this.m_suspendRefresh;
        }
    }
}

export interface IDatatableDataProvider<K, E, T>
{
    wasDestroyed(): boolean;

    getItemName(): string;

    getList(): Promise<K[]>;

    getPage(offset: number,
            limit: number): Promise<E[]>;

    transform(rows: E[]): Promise<T[]>;

    itemClicked(columnId: string,
                item: T): void;

    getTableConfigId(): string;

    getColumnConfigs(): Promise<ColumnConfiguration[]>;

    setColumnConfigs(configs: ColumnConfiguration[]): Promise<boolean>;
}

export class DatatableSort
{
    prop: string;
    dir: SortDirection;
}

export abstract class DatatableSelectionManager<K, T, ID>
{
    private readonly m_selection: Set<ID> = new Set<ID>();

    public readonly selectionChange        = new EventEmitter<Set<ID>>();
    public readonly selectionChangeSummary = new EventEmitter<DatatableSelectionChangeSummary<ID>>();

    constructor(protected table: DatatableManager<K, any, T>,
                public single: boolean,
                public showSelectAll: boolean,
                protected readonly keyConverter: (key: K) => ID,
                protected readonly valueConverter: (value: T) => ID)
    {
    }

    set selection(selection: Set<ID>)
    {
        this.m_selection.clear();

        if (selection)
        {
            for (let value of selection)
            {
                this.m_selection.add(value);
                if (this.single) break;
            }
        }

        this.processCheckedItems();
    }

    get selection(): Set<ID>
    {
        return new Set<ID>(this.m_selection);
    }

    update()
    {
        this.processCheckedItems();
    }

    //--//

    private m_allChecked: boolean = false;
    get allChecked(): boolean
    {
        return this.m_allChecked;
    }

    set allChecked(value: boolean)
    {
        this.m_allChecked = value;
        this.checkAllItems(value);
    }

    //--//

    private m_indeterminateChecked: boolean = false;
    get indeterminateChecked(): boolean
    {
        return this.m_indeterminateChecked;
    }

    set indeterminateChecked(value: boolean)
    {
        this.m_indeterminateChecked = value;
    }

    isChecked(row: T): boolean
    {
        let id = this.valueConverter(row);
        return this.m_selection.has(id);
    }

    setChecked(row: T,
               checked: boolean)
    {
        if (this.single)
        {
            this.m_selection.clear();
        }

        let id = this.valueConverter(row);
        if (checked)
        {
            this.m_selection.add(id);
        }
        else
        {
            this.m_selection.delete(id);
        }
    }

    itemChecked(row: T)
    {
        this.handleItemChecked(row);
        this.processCheckedItems();

        let id: ID    = this.valueConverter(row);
        let checked   = this.isChecked(row);
        let selection = this.selection;
        let summary   = new DatatableSelectionChangeSummary(DatatableSelectionTargetType.INDIVIDUAL, id, checked, selection);

        this.selectionChange.emit(selection);
        this.selectionChangeSummary.emit(summary);
    }

    abstract handleItemChecked(row: T): void;

    checkAllItems(state: boolean)
    {
        // Empty or fill selection
        if (state)
        {
            for (let item of this.table.slice(0, this.table.count)) this.m_selection.add(this.keyConverter(item));
        }
        else
        {
            this.m_selection.clear();
        }

        // Update states
        this.processCheckedItems();

        let selection = this.selection;
        let summary   = new DatatableSelectionChangeSummary(DatatableSelectionTargetType.ALL, null, state, selection);
        this.selectionChange.emit(selection);
        this.selectionChangeSummary.emit(summary);
    }

    private processCheckedItems()
    {
        let allChecked = true;
        let anyChecked = false;
        for (let item of this.table.slice(0, this.table.count))
        {
            if (this.m_selection.has(this.keyConverter(item)))
            {
                anyChecked = true;
            }
            else
            {
                allChecked = false;
            }
        }

        this.m_indeterminateChecked = anyChecked && !allChecked;
        this.m_allChecked           = anyChecked;
    }

    public static ensureSelection<T>(selectionManager: DatatableSelectionManager<any, any, T>,
                                     selection: T)
    {
        let selected = selectionManager.selection;
        selected.add(selection);
        selectionManager.selection = selected;
    }
}

export class SimpleSelectionManager<K, T, ID> extends DatatableSelectionManager<K, T, ID>
{
    public handleItemChecked(row: T): void
    {
        // Modify selection state
        if (!this.single)
        {
            this.setChecked(row, !this.isChecked(row));
        }
        else
        {
            this.setChecked(row, true);
        }
    }
}

export class DatatableSelectionChangeSummary<T>
{
    constructor(public type: DatatableSelectionTargetType,
                public target: T,
                public state: boolean,
                public selection: Set<T>)
    {}
}

export enum DatatableSelectionTargetType
{
    ALL, INDIVIDUAL
}

export abstract class DatatableExpansionManager<K, T, ID>
{
    private readonly m_expanded: Map<ID, DatatableExpansion<ID>> = new Map<ID, DatatableExpansion<ID>>();

    public readonly expandedChange        = new EventEmitter<Set<DatatableExpansion<ID>>>();
    public readonly expandedChangeSummary = new EventEmitter<DatatableExpansionChangeSummary<DatatableExpansion<ID>>>();

    constructor(protected table: DatatableManager<K, any, T>,
                public single: boolean,
                public showExpandAll: boolean,
                protected readonly keyConverter: (key: K) => ID,
                protected readonly valueConverter: (value: T) => ID)
    {
    }

    set expanded(expanded: Set<DatatableExpansion<ID>>)
    {
        this.m_expanded.clear();

        if (expanded)
        {
            for (let value of expanded)
            {
                this.m_expanded.set(value.id, value);
                if (this.single) break;
            }
        }

        this.processExpandedItems();
    }

    get expanded(): Set<DatatableExpansion<ID>>
    {
        return new Set<DatatableExpansion<ID>>(this.m_expanded.values());
    }

    update()
    {
        this.processExpandedItems();
    }

    //--//

    get anyExpanded(): boolean
    {
        return this.m_expanded.size > 0;
    }

    private m_allExpanded: boolean = false;
    get allExpanded(): boolean
    {
        return this.m_allExpanded;
    }

    set allExpanded(value: boolean)
    {
        this.m_allExpanded = value;
        this.expandAllItems(value, null);
    }

    //--//

    private m_indeterminateExpanded: boolean = false;
    get indeterminateExpanded(): boolean
    {
        return this.m_indeterminateExpanded;
    }

    set indeterminateExpanded(value: boolean)
    {
        this.m_indeterminateExpanded = value;
    }

    isExpanded(row: T): boolean
    {
        if (this.canExpand(row))
        {
            let id = this.valueConverter(row);
            return this.m_expanded.has(id);
        }

        return false;
    }

    getTrigger(row: T): string
    {
        if (this.isExpanded(row))
        {
            let id = this.valueConverter(row);
            return this.m_expanded.get(id).trigger;
        }

        return null;
    }

    setExpanded(row: T,
                trigger: string,
                expanded: boolean)
    {
        if (this.single)
        {
            this.m_expanded.clear();
        }

        let id = this.valueConverter(row);
        if (expanded)
        {
            let expansion = new DatatableExpansion(id, trigger);
            this.m_expanded.set(id, expansion);
        }
        else
        {
            this.m_expanded.delete(id);
        }
    }

    itemExpanded(row: T,
                 trigger: string)
    {
        this.handleItemExpanded(row, trigger);
        this.processExpandedItems();

        let expansion  = this.m_expanded.get(this.valueConverter(row));
        let expanded   = this.isExpanded(row);
        let expansions = this.expanded;
        let summary    = new DatatableExpansionChangeSummary(DatatableExpansionTargetType.INDIVIDUAL, expansion, expanded, expansions);

        this.expandedChange.emit(expansions);
        this.expandedChangeSummary.emit(summary);
    }

    abstract handleItemExpanded(row: T,
                                trigger: string): void;

    abstract canExpand(row: T): boolean;

    expandAllItems(state: boolean,
                   trigger: string)
    {
        // Empty or fill selection
        if (state)
        {
            for (let item of this.table.slice(0, this.table.count))
            {
                let id        = this.keyConverter(item);
                let expansion = new DatatableExpansion(id, trigger);
                this.m_expanded.set(id, expansion);
            }
        }
        else
        {
            this.m_expanded.clear();
        }

        // Update states
        this.processExpandedItems();

        let expansions = this.expanded;
        let summary    = new DatatableExpansionChangeSummary(DatatableExpansionTargetType.ALL, null, state, expansions);
        this.expandedChange.emit(expansions);
        this.expandedChangeSummary.emit(summary);
    }

    private processExpandedItems()
    {
        let allExpanded = true;
        let anyExpanded = false;
        for (let item of this.table.slice(0, this.table.count))
        {
            if (this.m_expanded.has(this.keyConverter(item)))
            {
                anyExpanded = true;
            }
            else
            {
                allExpanded = false;
            }
        }

        this.m_indeterminateExpanded = anyExpanded && !allExpanded;
        this.m_allExpanded           = anyExpanded;
    }
}

export class SimpleExpansionManager<K, T, ID> extends DatatableExpansionManager<K, T, ID>
{
    public handleItemExpanded(row: T,
                              trigger: string): void
    {
        // Modify selection state
        if (this.canExpand(row))
        {
            this.setExpanded(row, trigger, !this.isExpanded(row));
        }

    }

    public canExpand(row: T): boolean
    {
        return true;
    }
}

export class DatatableExpansionChangeSummary<T>
{
    constructor(public type: DatatableExpansionTargetType,
                public target: T,
                public state: boolean,
                public selection: Set<T>)
    {}
}

export class DatatableExpansion<ID>
{
    constructor(public id: ID,
                public trigger: string)
    {}
}

export enum DatatableExpansionTargetType
{
    ALL, INDIVIDUAL
}

export class ColumnConfiguration
{
    private static readonly resizeGripperWidthPx: number   = 1;
    private static readonly resizeGripperPaddingPx: number = 8;

    public headerElement: HTMLElement;
    public gripperElement: HTMLElement;
    public cellElements: HTMLElement[];

    public hasGripper: boolean = true;

    public leftOffset: number;
    public flexBasis: number;

    public overrideBasis: number;
    public overrideLeft: number;

    public prevColConfig: ColumnConfiguration;
    public nextColConfig: ColumnConfiguration;

    public scratchPadForResize: number;

    constructor(public readonly id: string,
                public enabled: boolean,
                public name?: string)
    {
    }

    updateColumnStyles(sortable: boolean)
    {
        if (this.headerElement && this.cellElements)
        {
            let flexBasis = this.headerElement.style.flexBasis = UtilsService.formatCssValue(this.flexBasis);
            for (let cellElem of this.cellElements) cellElem.style.flexBasis = flexBasis;

            this.updateGripperPosition(sortable);
        }
    }

    updateGripperPosition(sortable: boolean)
    {
        if (this.hasGripper && this.gripperElement)
        {
            this.gripperElement.style.left = UtilsService.formatCssValue(this.gripperOffset(sortable));
        }
    }

    private gripperOffset(sortable: boolean): number
    {
        let offset = ColumnConfiguration.resizeGripperPaddingPx + ColumnConfiguration.resizeGripperWidthPx;
        if (this.headerElement && sortable)
        {
            // sortable header cells have a different structure and an internal element with position relative for the sort arrow
            let dragAdjustment = isNaN(this.overrideBasis) ? 0 : (this.flexBasis - this.overrideBasis) + (this.leftOffset - this.overrideLeft);
            return this.headerElement.clientWidth - UtilsService.parseComputedStyle(getComputedStyle(this.headerElement).paddingLeft, "px") - (dragAdjustment + offset + 1);
        }

        return (this.overrideLeft || this.leftOffset) + (this.overrideBasis || this.flexBasis) - offset;
    }

    copy(other: ColumnConfiguration)
    {
        if (this.id === other.id)
        {
            this.enabled = other.enabled;
            this.name    = other.name;
        }
    }

    clone(): ColumnConfiguration
    {
        return new ColumnConfiguration(this.id, this.enabled, this.name || "");
    }

    isEquivalent(other: ColumnConfiguration)
    {
        if (this.id != other.id) return false;
        if (this.enabled !== other.enabled) return false;
        return !this.name || this.name === other.name;
    }

    moveCursor(toRight: boolean): ColumnConfiguration
    {
        return toRight ? this.nextColConfig : this.prevColConfig;
    }
}

type DatatableRow<T> = T & { rowNumber: number };

//--//

export class FilterDebouncer<T, F>
{
    private m_currentValue: T;
    private m_nextValue: T;
    private m_filters: F;
    private m_filtersExternal: F;
    private m_debouncer: number;

    constructor(private allocateState: () => T,
                private restoreState: () => T,
                private saveState: (val: T) => void,
                private convertToFilter: (state: T,
                                          externalFilters: F) => F,
                private notifyChanges: (filtersChanged: boolean) => void)
    {
    }

    public generateFilter(): F
    {
        if (this.m_nextValue)
        {
            this.m_currentValue = this.m_nextValue;
            this.cancelDebouncer();
        }

        this.ensureState();

        this.saveState(this.m_currentValue);

        return this.convertToFilter(this.m_currentValue, this.m_filtersExternal);
    }

    public getExternalFilter(): F
    {
        return this.m_filtersExternal;
    }

    public setExternalFilter(val: F): void
    {
        if (!UtilsService.compareJson(this.m_filtersExternal, val))
        {
            this.m_filtersExternal = val;
            this.debounce();
        }
    }

    public getProperty<K extends keyof T>(prop: K): T[K]
    {
        this.ensureState();

        let obj = this.m_nextValue || this.m_currentValue;

        return obj[prop];
    }

    public setProperty<K extends keyof T>(prop: K,
                                          val: T[K]): void
    {
        this.ensureState();

        if (!this.m_nextValue)
        {
            this.m_nextValue = Object.assign(this.allocateState(), this.m_currentValue);
        }

        this.m_nextValue[prop] = val;

        this.debounce();
    }

    private debounce()
    {
        this.cancelDebouncer();

        this.m_debouncer = setTimeout(() =>
                                      {
                                          this.m_debouncer = undefined;

                                          let filters = this.generateFilter();
                                          let changed = this.m_filters && !UtilsService.compareJson(filters, this.m_filters);

                                          this.m_filters = filters;
                                          this.notifyChanges(changed);
                                      }, 100);
    }

    private cancelDebouncer()
    {
        if (this.m_debouncer)
        {
            clearTimeout(this.m_debouncer);
        }
    }

    private ensureState()
    {
        if (!this.m_currentValue)
        {
            this.m_currentValue = this.restoreState() || this.allocateState();
        }
    }
}
