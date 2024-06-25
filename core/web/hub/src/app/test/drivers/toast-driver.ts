import {Injectable} from "@angular/core";

import {TestDriver, waitFor} from "app/test/driver";

import {ErrorListPaneComponent} from "framework/ui/errors/error-list-pane.component";

@Injectable({providedIn: "root"})
export class ToastDriver
{
    constructor(private m_driver: TestDriver)
    {
    }

    async clearAll(): Promise<void>
    {
        const standardLayout                 = await this.m_driver.getStandardLayoutComponent();
        const toasts: ErrorListPaneComponent = await waitFor(() => standardLayout.test_toasts, "Unable to get toast pane component");
        for (let toast of toasts.test_toasts)
        {
            try
            {
                await this.m_driver.click(toast, "toast");
            }
            catch (e)
            {
                // do nothing: the toast probably just expired on its own
            }
        }
    }
}
