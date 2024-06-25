import {Injectable} from "@angular/core";

import {TestDriver, waitFor} from "app/test/driver";
import {OverlayDriver} from "app/test/drivers/overlay-driver";

import {DialogConfirmComponent} from "framework/ui/dialogs/dialog-confirm.component";

@Injectable({providedIn: "root"})
export class ConfirmationDriver
{
    constructor(private m_driver: TestDriver,
                private m_overlayDriver: OverlayDriver)
    {
    }

    async handleConfirmationDialog(deny?: boolean)
    {
        const confirmComponent = await this.m_driver.getComponent(DialogConfirmComponent);
        const internalTab      = await waitFor(() => confirmComponent.test_standardForm?.test_tabGroup?.activeTabs[0], "Could not get confirm component's internal tab");

        let textOfInterest     = deny ? confirmComponent.denyText : confirmComponent.confirmText;
        const actionOfInterest = await waitFor(() => internalTab.actions.find((tabAction) => tabAction.label === textOfInterest),
                                               `Could not find confirm component's ${textOfInterest} button`);

        await this.m_driver.clickO3Element(actionOfInterest.optio3TestId, deny ? "deny confirmation button" : "accept confirmation button");
        await this.m_overlayDriver.waitForClose(confirmComponent.dialogRef.component.config.optio3TestId);
    }
}
