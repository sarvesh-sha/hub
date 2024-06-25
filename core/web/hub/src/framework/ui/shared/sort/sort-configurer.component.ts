import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from "@angular/core";

import {BaseComponent} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";

@Component({
               selector       : "o3-sort-configurer",
               templateUrl    : "./sort-configurer.component.html",
               styleUrls      : ["./sort-configurer.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class SortConfigurerComponent extends BaseComponent
{
    static ngAcceptInputType_hideColumn: boolean | "";
    static ngAcceptInputType_hideDirection: boolean | "";

    @Input() sortColumn: string;
    @Input() sortAscending: boolean                  = true;
    @Input() sortPlaceholder: string                 = "Initial Sort Column";
    @Input() sortOptions: ControlOption<string>[]    = [];
    @Input() @CoerceBoolean() hideId: boolean        = false;
    @Input() @CoerceBoolean() hideDirection: boolean = false;

    @Output() sortColumnChange    = new EventEmitter<string>();
    @Output() sortAscendingChange = new EventEmitter<boolean>();
}
