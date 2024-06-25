import {Component, Injector} from "@angular/core";
import {ReportError} from "app/app.service";

import {ExperimentsBasePageComponent} from "app/dashboard/experiments/exp-base-page.component";
import * as Models from "app/services/proxy/model/models";

import {DismissErrorAction, Error, ErrorAction, NavigationErrorAction} from "framework/services/error.service";

@Component({
               selector   : "o3-experiments-messages-page",
               templateUrl: "./exp-messages-page.component.html",
               styleUrls  : ["./exp-messages-page.component.scss"]
           })
export class ExperimentsMessagesPageComponent extends ExperimentsBasePageComponent
{
    constructor(inj: Injector)
    {
        super(inj);
    }

    fillCache()
    {
        for (let i = 0; i < 5000; i++)
        {
            let value = this.randomString(1000);
            this.app.framework.cache.set("TEST_" + i, value);
        }
    }

    error()
    {
        this.app.framework.errors.error("ERROR_CODE", "This is an error message (should auto dismiss).");
    }

    success()
    {
        this.app.framework.errors.success("This is a success message (should auto dismiss).", -1);
    }

    notify()
    {
        this.app.framework.errors.notify("This is a notification message (should NOT auto dismiss).");
    }

    notifyWithAction()
    {
        let action = new ErrorAction("Click Me", (e: Error) =>
        {
            alert("clicked notify button");
            return true;
        });

        this.app.framework.errors.notify("This is a notification message with actions (should NOT auto dismiss).", [action]);
    }

    notifyWithNavigationAction()
    {
        let action  = new NavigationErrorAction(this.app.routing.router, "Go", "/devices/catalog");
        let dismiss = new DismissErrorAction();
        this.app.framework.errors.notify("This is a notification message with navigation actions (should NOT auto dismiss).", [
            action,
            dismiss
        ]);
    }

    @ReportError
    async exception()
    {
        // get a null widget and attempt to access its properties
        let widget: Models.WidgetConfiguration = null;
        let description                        = widget.description;
        console.log(description);
    }

    randomString(length: number)
    {
        let text     = "";
        let possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (let i = 0; i < length; i++)
        {
            text += possible.charAt(Math.floor(Math.random() * possible.length));
        }
        return text;
    }
}
