import {ChangeDetectionStrategy, Component, Input} from "@angular/core";

import {PaneFieldComponent} from "app/dashboard/context-pane/fields/pane-field.component";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector       : "o3-pane-gauge-field",
               templateUrl    : "./pane-gauge-field.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class PaneGaugeFieldComponent extends PaneFieldComponent
{
    @Input()
    public severity: Models.AlertSeverity = Models.AlertSeverity.NORMAL;

    public isClickable(): boolean
    {
        return false;
    }
}
