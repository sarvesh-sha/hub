import {Component, EventEmitter, Input, Output} from "@angular/core";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";

@Component({
               selector: "o3-bound-select",
               template: `
                   <mat-form-field>
                       <mat-select [placeholder]="placeholder" [(ngModel)]="model" (ngModelChange)="onModelChange($event)" [required]="required">
                           <mat-option *ngFor="let item of items" [value]="value(item)">{{ label(item) }}</mat-option>
                       </mat-select>
                   </mat-form-field>`
           })
export class BoundSelectComponent
{
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_required: boolean | "";

    /**
     * Placeholder text for the select.
     */
    @Input() placeholder: string;

    /**
     * Whether the input is required.
     */
    @Input() @CoerceBoolean() required: boolean;

    /**
     * The items to display.
     */
    @Input() items: any[];

    /**
     * The property to use for the value of an item. Default "id".
     */
    @Input() valueProperty: string = "id";

    /**
     * The property to use for the label of an item. Default "name".
     */
    @Input() labelProperty: string = "name";

    /**
     * The model value to bind to (optional).
     */
    @Input() model: any;

    /**
     * An event raised when the model value changes.
     */
    @Output() modelChange = new EventEmitter<any>();

    /**
     * Raises the model change event.
     */
    onModelChange(event: any)
    {
        this.modelChange.emit(event);
    }

    /**
     * Returns the value of a specific item.
     */
    value(item: any)
    {
        return this.getPart(this.valueProperty, item);
    }

    /**
     * Returns the label of a specific item.
     */
    label(item: any)
    {
        return this.getPart(this.labelProperty, item);
    }

    private getPart(path: string,
                    item: any)
    {
        let parts = path.split(".");
        for (let part of parts)
        {
            if (!item)
            {
                break;
            }

            item = item[part];
        }

        return item;
    }
}
