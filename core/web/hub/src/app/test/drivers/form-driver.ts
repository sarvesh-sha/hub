import {ElementRef, Injectable} from "@angular/core";

import {TestDriver, waitFor} from "app/test/driver";
import {OverlayDriver} from "app/test/drivers/overlay-driver";

import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {StandardFormComponent} from "framework/ui/overlays/standard-form.component";

@Injectable({providedIn: "root"})
export class FormDriver
{
    constructor(private m_driver: TestDriver,
                private m_overlayDriver: OverlayDriver)
    {
    }

    async submitForm(standardForm: StandardFormComponent,
                     label: string)
    {
        const internalTab      = await waitFor(() => standardForm.test_tabGroup.activeTabs[0], "Could not get internal tab");
        const actionOfInterest = internalTab.actions.find((action) => action.label === label);
        await this.m_driver.clickO3Element(actionOfInterest.optio3TestId, "submit form button");
    }

    getFormFieldElement(formFieldDescendant: ElementRef<HTMLElement> | HTMLElement,
                        formFieldDescendantName: string): Promise<HTMLElement>
    {
        if (formFieldDescendant instanceof ElementRef) formFieldDescendant = formFieldDescendant.nativeElement;

        return waitFor(() =>
                       {
                           let element = <HTMLElement>formFieldDescendant;
                           while (element)
                           {
                               let parent = element.parentElement;
                               if (parent?.nodeName == "MAT-FORM-FIELD") return parent;
                               element = parent;
                           }

                           return null;
                       }, `could not get mat-form-field element of ${formFieldDescendantName}`);
    }

    async submitOverlayForm(standardOverlayForm: StandardFormOverlayComponent,
                            label: string)
    {
        const form = await this.m_driver.getComponentValue(standardOverlayForm, (overlay) => overlay.form, "standard form");
        await this.submitForm(form, label);
        await this.m_overlayDriver.waitForClose(standardOverlayForm.overlayConfig.optio3TestId);
    }
}
