import {Component, EventEmitter, Injector, Input, Output} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";

import {DatatableManager, DatatableSelectionChangeSummary, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-control-option-list",
               templateUrl: "./control-option-list.component.html",
               styleUrls  : ["./control-option-list.component.scss"]
           })
export class ControlOptionListComponent<T> extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<ControlOptionItem<T>, ControlOptionItem<T>, SelectableControlOptionItem<T>>
{
    @Output() selectionChange: EventEmitter<Set<T>>;
    @Output() selectionChangeSummary: EventEmitter<DatatableSelectionChangeSummary<T>>;

    //--//

    private m_options: ControlOptionItem<T>[] = [];
    @Input() set options(options: ControlOptionItem<T>[])
    {
        if (this.m_options != options)
        {
            this.m_options = options;

            if (!this.showFooter) this.table.limit = this.m_options.length;

            this.table.refreshData();
        }
    }

    //--//

    @Input() set selection(value: Set<T>)
    {
        this.table.selectionManager.selection = value;
    }

    //--//

    @Input() showSelectAll: boolean  = false;
    @Input() showRowNumbers: boolean = false;
    @Input() sortable: boolean       = false;
    @Input() filterable: boolean     = false;
    @Input() single: boolean         = false;
    @Input() showHeader: boolean     = false;

    @Input() set showFooter(showFooter: boolean)
    {
        this.m_showFooter = showFooter;
        if (this.m_showFooter) this.table.limit = this.table.defaultLimit;
    }

    private m_showFooter: boolean = false;

    get showFooter(): boolean
    {
        return this.m_showFooter;
    }

    private m_filterText: string;
    get filterText(): string
    {
        return this.m_filterText;
    }

    set filterText(value: string)
    {
        if (this.m_filterText != value)
        {
            this.m_filterText = value;
            this.table.refreshData();
        }
    }

    //--//


    table: DatatableManager<ControlOptionItem<T>, ControlOptionItem<T>, SelectableControlOptionItem<T>>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = new DatatableManager(this, () => this.getViewState());

        let manager                 = this.table.enableSimpleSelection((key) => key.id, (value) => value.option.id, this.single, this.showSelectAll);
        this.selectionChange        = manager.selectionChange;
        this.selectionChangeSummary = manager.selectionChangeSummary;
    }

    public ngOnInit(): void
    {
        super.ngOnInit();
        this.table.selectionManager.single        = this.single;
        this.table.selectionManager.showSelectAll = this.showSelectAll;
    }

    ngAfterViewInit()
    {
        // Refresh table
        this.table.refreshData();
    }

    getItemName(): string { return "Options"; }

    getTableConfigId(): string { return "options"; }

    async getList(): Promise<ControlOptionItem<T>[]>
    {
        // Get all location identities
        let all = this.m_options;

        // Check if we need to sort or filter
        if (this.sortable && this.table.sort.length > 0 || this.filterable && this.filterText)
        {
            // Apply filter
            if (this.filterText)
            {
                let lower = this.filterText.toLowerCase();
                all       = all.filter((option) =>
                                       {
                                           return option.label.toLowerCase()
                                                        .indexOf(lower) >= 0;
                                       });
            }

            // Apply sorting
            if (this.sortable && this.table.sort.length > 0)
            {
                let sort = this.table.sort[0];
                let dir  = sort.dir === "asc";
                all.sort((a,
                          b) => dir ? a.label.localeCompare(b.label) : b.label.localeCompare(a.label));
            }
        }

        return all;
    }

    async getPage(offset: number,
                  limit: number): Promise<ControlOptionItem<T>[]>
    {
        return this.table.slicePage(offset, limit);
    }

    async transform(rows: ControlOptionItem<T>[]): Promise<SelectableControlOptionItem<T>[]>
    {
        return rows.map((row) => new SelectableControlOptionItem<T>(row, false));
    }

    itemClicked(columnId: string,
                item: SelectableControlOptionItem<T>): void
    {
        this.table.selectionManager.itemChecked(item);
    }
}

class SelectableControlOptionItem<T>
{
    constructor(public readonly option: ControlOptionItem<T>,
                public readonly checked: boolean)
    {}
}

export interface ControlOptionItem<T>
{
    id: T;
    label: string;
    description?: string;
}
