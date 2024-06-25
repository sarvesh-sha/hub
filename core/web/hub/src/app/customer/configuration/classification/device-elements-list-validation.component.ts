import {Component, Input} from "@angular/core";
import {DeviceElementFlat, DeviceElementsListBase} from "app/customer/device-elements/device-elements-list-base";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {Lookup} from "framework/services/utils.service";


@Component({
               selector   : "o3-device-elements-list-validation",
               templateUrl: "./device-elements-list-validation.component.html"
           })
export class DeviceElementsListValidationComponent extends DeviceElementsListBase<DeviceElementFlatWithMessage>
{
    @Input() public messages: Lookup<string>;

    public newRow(): DeviceElementFlatWithMessage
    {
        return new DeviceElementFlatWithMessage();
    }

    public getTableConfigId(): string { return "device-elements-validation"; }

    public async transform(rows: DeviceElementExtended[]): Promise<DeviceElementFlatWithMessage[]>
    {
        let transformed = await super.transform(rows);
        for (let row of transformed)
        {
            if (this.messages && this.messages[row.sysId])
            {
                row.message = this.messages[row.sysId];
            }
        }

        return transformed;
    }
}


class DeviceElementFlatWithMessage extends DeviceElementFlat
{
    message: string;
}
