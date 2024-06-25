import {Component, Input} from "@angular/core";

import {DeliveryOptionsExtended} from "app/customer/configuration/common/delivery-options";
import * as SharedSvc from "app/services/domain/base.service";

import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-delivery-options",
               templateUrl: "./delivery-options.component.html"
           })
export class DeliveryOptionsComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() deliveryOptions: DeliveryOptionsExtended;

    userOptions: ControlOption<string>[];
    groupOptions: ControlOption<string>[];
    roleOptions: ControlOption<string>[];

    async ngOnInit()
    {
        super.ngOnInit();

        this.userOptions  = await this.app.bindings.getUsers(true);
        this.groupOptions = await this.app.bindings.getGroups();
        this.roleOptions  = await this.app.bindings.getRoles();
    }

    getUserName(sysId: string)
    {
        return this.getOptionLabel(this.userOptions, sysId);
    }

    getGroupName(sysId: string)
    {
        return this.getOptionLabel(this.groupOptions, sysId);
    }

    getRoleName(sysId: string)
    {
        return this.getOptionLabel(this.roleOptions, sysId);
    }

    private getOptionLabel(options: ControlOption<string>[],
                           id: string)
    {
        let option = options.find((u) => u.id === id);
        if (option)
        {
            return option.label;
        }
        else
        {
            // For LDAP users this is the email address
            return id;
        }
    }
}
