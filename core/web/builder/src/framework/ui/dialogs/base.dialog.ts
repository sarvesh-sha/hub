import {ComponentType} from "@angular/cdk/overlay";
import {ChangeDetectorRef, Directive, ElementRef, Injector, OnDestroy, OnInit, Type, ViewChild} from "@angular/core";

import {BaseComponent} from "framework/ui/components";
import {OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Directive()
export abstract class BaseDialogComponent<T extends BaseDialogComponent<T, V>, V extends BaseDialogSelection> implements OnInit,
                                                                                                                         OnDestroy
{
    cdr: ChangeDetectorRef;

    loading: boolean = false;

    @ViewChild("dialogContent", {static: true}) dialogContent: ElementRef;
    @ViewChild("itemGroup", {static: true}) itemGroup: ElementRef;
    @ViewChild("itemRow", {static: true}) itemRow: ElementRef;

    items: V[];
    filterItems: string = null;
    private filterItemsLast: string;
    private filteredItemsLast: V[];

    constructor(public dialogRef: OverlayDialogRef<V>,
                protected inj: Injector)
    {
        this.cdr = inj.get(ChangeDetectorRef);
    }

    ngOnInit()
    {
        this.loadData();
    }

    ngOnDestroy(): void
    {
        this.dialogRef = null;
        this.cdr       = null;
    }

    protected detectChanges(): void
    {
        if (this.cdr) this.cdr.detectChanges();
    }

    protected async loadData()
    {
        this.loading = true;
        this.detectChanges();

        try
        {
            this.items             = [];
            this.filteredItemsLast = null;

            await this.loadItems();

            // lock the dimensions of the dialog
            // (avoiding odd shifting behavior on filtering)
            this.lockDialogDimensions();
        }
        finally
        {
            this.loading = false;
            this.detectChanges();
        }
    }

    protected addNewItem(item: V): void
    {
        item.index = this.items.length;
        this.items.push(item);
        this.filteredItemsLast = null;
    }

    protected sortItems(callback: (left: V,
                                   right: V) => number)
    {
        this.items.sort(callback);

        let index = 0;
        for (let item of this.items)
        {
            item.index = index++;
        }
    }

    protected getLastItem()
    {
        return this.items && this.items.length > 0 ? this.items[this.items.length - 1] : null;
    }

    filteredItems(): V[]
    {
        if (!this.filteredItemsLast || this.shouldRefreshFiltered())
        {
            let pattern = (this.filterItems || "").toLowerCase();

            let results = (this.items || []).filter((item) => this.shouldDisplay(pattern, item));
            if (results.length == 0)
            {
                this.handleEmptyFilteredResults();
            }

            this.filteredItemsLast = results;

            this.doneRefreshFiltered();
        }

        return this.filteredItemsLast;
    }

    protected shouldRefreshFiltered(): boolean
    {
        return this.filterItems != this.filterItemsLast;
    }

    protected doneRefreshFiltered()
    {
        this.filterItemsLast = this.filterItems;
    }

    noResults(): boolean
    {
        let items = this.filteredItems();
        return (!items || items.length == 0);
    }

    protected abstract loadItems(): Promise<void>;

    protected abstract onEmptyFilterResults(): Promise<void>;

    protected abstract shouldDisplay(pattern: string,
                                     item: V): boolean;

    protected containsPattern(pattern: string,
                              target: string): boolean

    {
        // For a short pattern, include all items.
        if (!pattern || pattern.length < 2) return true;

        return pattern && target && target.toLowerCase()
                                          .indexOf(pattern) >= 0;
    }

    //--//

    private lockDialogDimensions()
    {
        if (!this.itemRow || !this.itemGroup)
        {
            return;
        }

        let row = this.itemRow.nativeElement;
        if (row)
        {
            let maxRowsToShow = Math.min(this.items.length + 2, 8);

            let element          = this.itemGroup.nativeElement;
            element.style.height = (row.clientHeight * maxRowsToShow) + "px";
        }
    }

    private async handleEmptyFilteredResults()
    {
        if (!this.loading)
        {
            try
            {
                this.loading = true;
                await this.onEmptyFilterResults();

                this.lockDialogDimensions();
            }
            finally
            {
                this.loading = false;
            }
        }
    }
}

@Directive()
export abstract class BaseDialogComponentSingleSelect<T extends BaseDialogComponentSingleSelect<T, V>, V extends BaseDialogSelection> extends BaseDialogComponent<T, V>
{
    selectedItemIndex: number;

    constructor(dialogRef: OverlayDialogRef<V>,
                inj: Injector)
    {
        super(dialogRef, inj);
    }

    inject<T>(token: Type<T>): T
    {
        return this.inj.get(token);
    }

    protected static async openInner<T extends BaseDialogComponentSingleSelect<T, V>, V extends BaseDialogSelection>(comp: BaseComponent,
                                                                                                                     constructor: ComponentType<T>,
                                                                                                                     config: BaseDialogConfig,
                                                                                                                     closableViaBackdrop?: boolean,
                                                                                                                     width?: string,
                                                                                                                     height?: string): Promise<V>
    {
        let overlayConfig = OverlayConfig.newInstance({
                                                          width,
                                                          height,
                                                          closableViaBackdrop
                                                      });

        let result = await OverlayComponent.open<BaseDialogConfig, V>(comp, constructor, {
            data  : config,
            config: overlayConfig
        });

        config.dialogCancelled = true;

        if (result instanceof BaseDialogSelection)
        {
            return result;
        }

        return null;
    }

    clicked(): void
    {
        this.dialogRef.close(this.items[this.selectedItemIndex]);
    }

    protected selectItem(index: number)
    {
        if (index >= 0 && index < this.items.length)
        {
            this.selectedItemIndex = index;
        }
    }
}

@Directive()
export abstract class BaseDialogComponentMultiSelect<T extends BaseDialogComponentMultiSelect<T, V>, V extends BaseDialogSelectionWithState> extends BaseDialogComponent<T, V>
{
    constructor(dialogRef: OverlayDialogRef<V>,
                inj: Injector)
    {
        super(dialogRef, inj);
    }

    protected static async openInner<T extends BaseDialogComponentMultiSelect<T, V>, V extends BaseDialogSelectionWithState>(comp: BaseComponent,
                                                                                                                             width: string,
                                                                                                                             height: string,
                                                                                                                             constructor: ComponentType<T>,
                                                                                                                             config: BaseDialogConfig): Promise<V[]>
    {
        let overlayConfig = OverlayConfig.newInstance({
                                                          width : width,
                                                          height: height
                                                      });

        let result = await OverlayComponent.open(comp, constructor, {
            data  : config,
            config: overlayConfig
        });

        config.dialogCancelled = true;

        if (result instanceof BaseDialogSelections)
        {
            return <V[]>result.items;
        }
        else
        {
            return null;
        }
    }

    clicked(): void
    {
        let res   = new BaseDialogSelections();
        res.items = [];

        for (let item of this.items)
        {
            if (item.selected)
            {
                res.items.push(item);
            }
        }

        this.dialogRef.close(<any>res);
    }

    get anySelected(): boolean
    {
        for (let item of this.items || [])
        {
            if (item.selected)
            {
                return true;
            }
        }

        return false;
    }
}

export abstract class BaseDialogConfig
{
    dialogPurpose: string;

    dialogOkButton: string;

    dialogCancelled: boolean;
}

export abstract class BaseDialogSelection
{
    index: number;
}

export abstract class BaseDialogSelectionWithState extends BaseDialogSelection
{
    selected: boolean;
}

export class BaseDialogSelections
{
    items: BaseDialogSelection[];
}
