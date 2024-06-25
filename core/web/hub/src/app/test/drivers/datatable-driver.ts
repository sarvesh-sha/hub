import {Injectable} from "@angular/core";

import {querySelectorHelper, TestDriver, waitFor} from "app/test/driver";

@Injectable({providedIn: "root"})
export class DatatableDriver
{
    constructor(private m_driver: TestDriver)
    {
    }

    async selectControlOption(controlOptionList: HTMLElement,
                              optionLabel: string): Promise<void>
    {
        const targetElem = await waitFor(() => querySelectorHelper("mat-row pre", (elem) => elem.innerText == optionLabel, controlOptionList),
                                         `Could not find element with label "${optionLabel}"`);
        await this.m_driver.click(targetElem, "datatable option: " + optionLabel);
    }
}
