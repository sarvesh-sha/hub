import {Component, Input} from "@angular/core";

@Component({
               selector   : "o3-key-value-item[key][value]",
               templateUrl: "./key-value-item.component.html",
               styleUrls  : ["./key-value-item.component.scss"]
           })
export class KeyValueItem
{
    @Input() key: string;
    @Input() value: string;
}
