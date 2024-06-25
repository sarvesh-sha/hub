import {Component, EventEmitter, Input, Output} from "@angular/core";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector: "o3-unit-editor-wrapper",
               template: `
                   <mat-form-field style="width: 50%">
                       <o3-unit-editor placeholder="Units" [(ngModel)]="units" (ngModelChange)="unitsChange.emit($event)"></o3-unit-editor>
                   </mat-form-field>`
           })
export class UnitEditorWrapperComponent
{
    @Input() public units: Models.EngineeringUnitsFactors;
    @Output() public unitsChange = new EventEmitter<Models.EngineeringUnitsFactors>();
}
