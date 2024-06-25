import {Injectable} from "@angular/core";

import {TestDriver, waitFor} from "app/test/driver";

import {OverlayComponent} from "framework/ui/overlays/overlay.component";

@Injectable({providedIn: "root"})
export class OverlayDriver
{
    constructor(private m_driver: TestDriver)
    {
    }

    async closeOverlay(overlay: OverlayComponent): Promise<void>
    {
        const closeButton = await waitFor(() => overlay.test_close, "could not get overlay's close button");
        await this.m_driver.click(closeButton, "overlay close button");
        await this.waitForClose(overlay.config.optio3TestId);
    }

    async waitForOpen(overlayTestId: string): Promise<void>
    {
        await waitFor(() => this.overlayPresent(overlayTestId), "Could not find overlay");
    }

    async waitForClose(overlayTestId: string): Promise<void>
    {
        await waitFor(() => !this.overlayPresent(overlayTestId), "Overlay is still present and open");
    }

    overlayPresent(overlayTestId: string): boolean
    {
        return !!document.querySelector(this.m_driver.getO3Selector(overlayTestId));
    }
}
