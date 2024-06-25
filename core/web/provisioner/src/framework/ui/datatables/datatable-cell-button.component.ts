import {Component, Input} from "@angular/core";

@Component({
               selector   : "o3-datatable-cell-button[icon]",
               templateUrl: "./datatable-cell-button.component.html",
               styleUrls  : ["./datatable-cell-button.component.scss"]
           })
export class DatatableCellButtonComponent<T>
{
    @Input() contextMenuRow: T;
    @Input() icon: string;
}
