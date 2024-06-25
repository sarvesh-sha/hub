import {Injectable} from "@angular/core";

import {StandardLayoutComponent} from "app/layouts/standard-layout.component";
import {assertIsDefined, TestDriver, waitFor} from "app/test/driver";

@Injectable({providedIn: "root"})
export class SidebarDriver
{
    constructor(private m_driver: TestDriver)
    {
    }

    async ensureSidebarOpen(standardLayout: StandardLayoutComponent): Promise<void>
    {
        assertIsDefined(standardLayout, "standard layout");
        if (!standardLayout.sidenavOpen)
        {
            const hasOpenSidebar = () => !!document.querySelector(".mat-drawer-opened");
            await waitFor(() => !hasOpenSidebar(), "sidenav should be closed", 500);
            await this.m_driver.click(standardLayout.test_sidenavToggle, "sidenav toggle");
            await waitFor(() => standardLayout.sidenavOpen && hasOpenSidebar(), "sidenav did not properly open", 500);
        }
    }
}
