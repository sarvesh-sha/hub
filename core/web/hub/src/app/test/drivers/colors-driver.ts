import {Injectable} from "@angular/core";

import {ColorPickerFlatComponent} from "app/shared/colors/color-picker-flat.component";
import {ColorPickerComponent} from "app/shared/colors/color-picker.component";
import {TestDriver, waitFor} from "app/test/driver";
import {OverlayDriver} from "app/test/drivers/overlay-driver";

@Injectable({providedIn: "root"})
export class ColorsDriver
{
    constructor(private m_driver: TestDriver,
                private m_overlayDriver: OverlayDriver)
    {
    }

    async selectColor(colorFlat: ColorPickerFlatComponent,
                      colorId: string): Promise<void>
    {
        // todo: support changing of palette to find color of interest

        const colorOptions = await waitFor(() => colorFlat.test_colorPalette?.options?.length && colorFlat.test_colorPalette.options,
                                           "Could not get color options");
        const option       = colorOptions.find((option) => option.id.id === colorId);
        if (!option) throw Error(`Cannot find color "${colorId}" in palette`);

        const colorPalette = await waitFor(() => colorFlat.test_colorPalette, "Could not get color palette");
        await this.m_driver.clickO3Element(colorPalette.getO3TestId(option), "color option: " + colorId);
    }

    async pickColor(color: ColorPickerComponent,
                    colorId: string,
                    colorPurpose: string)
    {
        await this.m_driver.click(color.test_trigger, `${colorPurpose} color picker trigger`);
        await this.m_overlayDriver.waitForOpen(color.overlayConfig.optio3TestId);
        const colorFlat = await waitFor(() => color.test_color, "Could not get color picker flat component");
        await this.selectColor(colorFlat, colorId);
        await this.m_overlayDriver.closeOverlay(color.colorOverlay);
    }
}
