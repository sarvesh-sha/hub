import {CdkDragDrop, moveItemInArray} from "@angular/cdk/drag-drop";
import {ChangeDetectionStrategy, Component, ContentChild, Directive, ElementRef, EventEmitter, Input, Output, TemplateRef, Type, ViewChild} from "@angular/core";

import {BaseComponent} from "framework/ui/components";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";

@Directive({selector: "[o3ModifiableTableRow]"})
export class ModifiableTableRowDirective<T>
{
    // value should be generic but using type = 'any' for nicer template syntax
    constructor(public template: TemplateRef<{ $implicit: T, value: T, index: number }>)
    {}

    @Input("o3ModifiableTableRowType") type: Type<T>;
}

// -- //

@Component({
               selector       : "o3-modifiable-table[rows]",
               templateUrl    : "./modifiable-table.component.html",
               styleUrls      : ["./modifiable-table.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ModifiableTableComponent<T> extends BaseComponent
{
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_allowReordering: boolean | "";
    static ngAcceptInputType_allowAdding: boolean | "";
    static ngAcceptInputType_allowDeleting: boolean | "";

    @Input() rows: T[];
    @Input() limit: number;

    @Input() @CoerceBoolean() allowReordering: boolean = false;
    @Input() @CoerceBoolean() allowAdding: boolean     = false;
    @Input() @CoerceBoolean() allowDeleting: boolean   = false;

    @Input() itemName: string;
    @Input() placeholderHeightPx: number = 45;
    @Input() backgroundColor: string;
    @Input() showRowHover: boolean       = false;
    @Input() noBorder: boolean           = false;

    @Input() canDelete: (row: T) => boolean = () => this.rows.length > 1;
    @Input() addDisabledTooltip: string;

    @ContentChild(ModifiableTableRowDirective, {static: true}) rowTemplate: ModifiableTableRowDirective<T>;

    @ViewChild("test_add", {read: ElementRef}) test_add: ElementRef;

    @Output() itemClicked     = new EventEmitter<T>();
    @Output() rowAddRequested = new EventEmitter<void>();
    @Output() rowsReordered   = new EventEmitter<CdkDragDrop<T>>();
    @Output() rowRemoved      = new EventEmitter<ModifiableTableRowRemove<T>>();

    get showAddButton(): boolean
    {
        if (!this.rows) return false;
        if (!this.allowAdding) return false;
        if (isNaN(this.limit)) return true;

        return this.rows.length < this.limit;
    }

    get rowName(): string
    {
        return this.itemName || "row";
    }

    public updateOrder(event: CdkDragDrop<T>)
    {
        moveItemInArray(this.rows, event.previousIndex, event.currentIndex);
        this.rowsReordered.emit(event);
    }

    public removeRow(idx: number)
    {
        let row = this.rows[idx];
        if (!this.canDelete(row)) return;

        this.rows.splice(idx, 1);
        this.rowRemoved.emit(new ModifiableTableRowRemove(row, idx));
        this.detectChanges();
    }
}

export class ModifiableTableRowRemove<T>
{
    constructor(readonly row: T,
                readonly index: number)
    {}
}
