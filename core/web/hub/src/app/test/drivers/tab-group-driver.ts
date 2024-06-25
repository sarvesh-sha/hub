import {Injectable} from "@angular/core";

import {querySelectorHelper, TestDriver, waitFor} from "app/test/driver";

import {TabGroupComponent} from "framework/ui/tab-group/tab-group.component";

@Injectable({providedIn: "root"})
export class TabGroupDriver
{
    constructor(private m_driver: TestDriver)
    {
    }

    async changeTab(tabGroup: TabGroupComponent,
                    tabLabel: string): Promise<void>
    {
        const matTabGroupElem = await waitFor(() => tabGroup.tabGroup?.nativeElement, "Could not get mat tab group elem");
        tabLabel              = tabLabel.toLocaleUpperCase();
        const tab             = querySelectorHelper(".mat-tab-label span", (elem) => elem.innerText === tabLabel, matTabGroupElem);
        await this.m_driver.click(tab, "tab " + tabLabel);
        await waitFor(() => tabGroup.activeTabs[tabGroup.selectedIndex].label.toLowerCase() === tabLabel.toLowerCase(),
                      `Tab group did not successfully change tab. On ${tabGroup.activeTabs[tabGroup.selectedIndex].label.toLowerCase()} instead of ${tabLabel.toLowerCase()}`);
    }
}
