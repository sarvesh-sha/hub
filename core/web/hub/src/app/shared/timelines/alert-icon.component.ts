import {Component, Input} from "@angular/core";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-alert-icon",
               templateUrl: "./alert-icon.component.html"
           })
export class AlertIconComponent
{
    @Input() type: Models.AlertType;

    get isErrorType(): boolean
    {
        switch (this.type)
        {
            case Models.AlertType.DEVICE_FAILURE:
            case Models.AlertType.THRESHOLD_EXCEEDED:
            case Models.AlertType.WARNING:
                return true;
        }

        return false;
    }

    @Input() severity: Models.AlertSeverity;
}
