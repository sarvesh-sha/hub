import {Component, Inject, Injector} from "@angular/core";
import {GatewayExtended} from "app/services/domain/assets.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {UsersService} from "app/services/domain/users.service";
import * as Models from "app/services/proxy/model/models";

import {Logger} from "framework/services/logging.service";
import {BaseComponent} from "framework/ui/components";
import {BaseDialogComponentSingleSelect, BaseDialogConfig, BaseDialogSelection} from "framework/ui/dialogs/base.dialog";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               templateUrl: "./gateway-selection-dialog.component.html"
           })
export class GatewaySelectionDialogComponent extends BaseDialogComponentSingleSelect<GatewaySelectionDialogComponent, GatewayDescriptor>
{
    private readonly logger: Logger;
    private domain: AppDomainContext;

    includeBound: boolean;
    private includeBoundLast: boolean;
    private isCRE: boolean;

    constructor(dialogRef: OverlayDialogRef<GatewayDescriptor>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: DialogConfig)
    {
        super(dialogRef, inj);

        this.domain = this.inj.get(AppDomainContext);
        this.logger = this.domain.assets.logger.getLogger(GatewaySelectionDialogComponent);

        this.isCRE = this.inject(UsersService).instanceConfiguration instanceof Models.InstanceConfigurationForCRE;
    }

    public static open(comp: BaseComponent,
                       purpose: string,
                       okButton: string): Promise<GatewayDescriptor>
    {
        let cfg            = new DialogConfig();
        cfg.dialogPurpose  = purpose;
        cfg.dialogOkButton = okButton;

        return BaseDialogComponentSingleSelect.openInner(comp, GatewaySelectionDialogComponent, cfg, false);
    }

    protected async loadItems()
    {
        for (let gateway of await GatewayExtended.listAll(this.domain, true))
        {
            let item     = new GatewayDescriptor();
            item.gateway = gateway;
            item.isBound = (await gateway.getBoundNetworks() || []).length > 0;
            this.addNewItem(item);
        }
    }

    protected async onEmptyFilterResults()
    {
    }

    protected shouldRefreshFiltered(): boolean
    {
        return super.shouldRefreshFiltered() || (this.includeBoundLast != this.includeBound);
    }

    protected doneRefreshFiltered()
    {
        super.doneRefreshFiltered();

        this.includeBoundLast = this.includeBound;
    }

    protected shouldDisplay(pattern: string,
                            item: GatewayDescriptor): boolean
    {
        if (!this.includeBound && item.isBound) return false;

        if (this.containsPattern(pattern, item.gateway.model.name)) return true;
        if (this.containsPattern(pattern, item.gateway.typedModel.instanceId)) return true;

        return false;
    }

    get showBoundChoice(): boolean
    {
        return this.isCRE || (this.items && this.items.length > 5);
    }
}

class DialogConfig extends BaseDialogConfig
{
}

export class GatewayDescriptor extends BaseDialogSelection
{
    gateway: GatewayExtended;
    isBound: boolean;

    get name(): string
    {
        return this.gateway.model.name || "<unnamed>";
    }
}
