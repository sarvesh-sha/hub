import {Component, Inject, Injector} from "@angular/core";

import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";

import {BaseDialogComponentMultiSelect, BaseDialogConfig, BaseDialogSelectionWithState} from "framework/ui/dialogs/base.dialog";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               templateUrl: "./service-role-selection-dialog.component.html"
           })
export class ServiceRoleSelectionDialogComponent extends BaseDialogComponentMultiSelect<ServiceRoleSelectionDialogComponent, RoleDescriptor>
{
    constructor(dialogRef: OverlayDialogRef<RoleDescriptor>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: DialogConfig)
    {
        super(dialogRef, inj);
    }

    public static open(comp: BaseComponent,
                       roles: Map<Models.DeploymentRole, boolean>,
                       roleText: string,
                       okButton: string): Promise<RoleDescriptor[]>
    {
        let cfg            = new DialogConfig();
        cfg.roles          = roles;
        cfg.dialogPurpose  = roleText;
        cfg.dialogOkButton = okButton;

        return BaseDialogComponentMultiSelect.openInner(comp, undefined, undefined, ServiceRoleSelectionDialogComponent, cfg);
    }

    protected async loadItems()
    {
        this.data.roles.forEach((selected,
                                 role) =>
                                {
                                    let item      = new RoleDescriptor();
                                    item.role     = role;
                                    item.selected = selected;
                                    this.addNewItem(item);
                                });

        this.sortItems((l,
                        r) =>
                       {
                           return UtilsService.compareStrings(l.role, r.role, true);
                       });
    }

    protected async onEmptyFilterResults()
    {
    }

    protected shouldDisplay(pattern: string,
                            item: RoleDescriptor): boolean
    {
        if (this.containsPattern(pattern, item.role.toString())) return true;

        return false;
    }

}

class DialogConfig extends BaseDialogConfig
{
    roles: Map<Models.DeploymentRole, boolean>;
}

export class RoleDescriptor extends BaseDialogSelectionWithState
{
    role: Models.DeploymentRole;
}
