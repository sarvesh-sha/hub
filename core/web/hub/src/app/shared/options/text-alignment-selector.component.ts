import {Component, EventEmitter, Input, Output} from "@angular/core";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-text-alignment-selector[alignment]",
               templateUrl: "./text-alignment-selector.component.html",
               styleUrls  : ["./text-alignment-selector.component.scss"]
           })
export class TextAlignmentSelectorComponent
{
    @Input() alignment: Models.HorizontalAlignment;
    @Input() disabled: boolean = false;

    @Output() alignmentChange = new EventEmitter<Models.HorizontalAlignment>();
}
