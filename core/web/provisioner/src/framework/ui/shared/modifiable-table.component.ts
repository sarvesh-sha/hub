import {CdkDragDrop, moveItemInArray} from "@angular/cdk/drag-drop";
import {ChangeDetectionStrategy, Component, ContentChild, Directive, EventEmitter, Input, Output, TemplateRef} from "@angular/core";

import {BaseComponent} from "framework/ui/components";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";

@Directive({selector: "[o3ModifiableTableRow]"})
export class ModifiableTableRowDirective
{
    // value should be generic but using type = 'any' for nicer template syntax
    constructor(public template: TemplateRef<{ $implicit: any, value: any, index: number }>)
    {}
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

    @ContentChild(ModifiableTableRowDirective, {static: true}) rowTemplate: ModifiableTableRowDirective;

    @Output() onItemClick     = new EventEmitter<T>();
    @Output() onRowAddRequest = new EventEmitter<void>();
    @Output() onRowReorder    = new EventEmitter<CdkDragDrop<T>>();
    @Output() onRowRemove     = new EventEmitter<ModifiableTableRowRemove<T>>();

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
        this.onRowReorder.emit(event);
    }

    public removeRow(idx: number)
    {
        let row = this.rows[idx];
        if (!this.canDelete(row)) return;

        this.rows.splice(idx, 1);
        this.onRowRemove.emit(new ModifiableTableRowRemove(row, idx));
        this.detectChanges();
    }
}

export class ModifiableTableRowRemove<T>
{
    constructor(readonly row: T,
                readonly index: number)
    {}
}
