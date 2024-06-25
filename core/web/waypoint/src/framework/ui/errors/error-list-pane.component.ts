import {Component} from "@angular/core";

import {Error, ErrorAction, ErrorLevel, ErrorService} from "framework/services/error.service";
import {AppFabsService} from "framework/ui/fabs/app-fabs.service";

@Component({
               selector   : "o3-error-list-pane",
               templateUrl: "./error-list-pane.component.html",
               styleUrls  : ["./error-list-pane.component.scss"]
           })
export class ErrorListPaneComponent
{

    constructor(public errors: ErrorService,
                public fabs: AppFabsService)
    {

    }

    messageClass(error: Error): string
    {
        switch (error.level)
        {
            case ErrorLevel.Error:
                return "error";
            case ErrorLevel.Fatal:
                return "error";
            case ErrorLevel.Success:
                return "success";
            case ErrorLevel.Warn:
                return "warn";
        }
    }

    handleAction(error: Error,
                 action: ErrorAction)
    {
        if (action.func && action.func(error))
        {
            this.dismiss(error);
        }
    }

    dismiss(error: Error)
    {
        this.errors.dismiss(error);
    }

    get hasFab(): boolean
    {
        let fabs = this.fabs.get();
        if (fabs && fabs.length) return true;
        return false;
    }
}
