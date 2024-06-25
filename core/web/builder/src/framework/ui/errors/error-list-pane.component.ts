import {Component, ElementRef, QueryList, ViewChildren} from "@angular/core";

import {Error, ErrorAction, ErrorLevel, ErrorService} from "framework/services/error.service";

@Component({
               selector   : "o3-error-list-pane",
               templateUrl: "./error-list-pane.component.html",
               styleUrls  : ["./error-list-pane.component.scss"]
           })
export class ErrorListPaneComponent
{
    @ViewChildren("test_toast", {read: ElementRef}) test_toasts: QueryList<ElementRef>;

    constructor(public readonly errors: ErrorService)
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
}
