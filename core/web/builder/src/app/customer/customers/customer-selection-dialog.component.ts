import {Component, Inject, Injector} from "@angular/core";

import {AppDomainContext} from "app/services/domain";
import {CustomerExtended} from "app/services/domain/customers.service";

import {Logger} from "framework/services/logging.service";
import {UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";
import {BaseDialogComponentSingleSelect, BaseDialogConfig, BaseDialogSelection} from "framework/ui/dialogs/base.dialog";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               templateUrl: "./customer-selection-dialog.component.html"
           })
export class CustomerSelectionDialogComponent extends BaseDialogComponentSingleSelect<CustomerSelectionDialogComponent, CustomerDescriptor>
{
    private domain: AppDomainContext;
    private readonly logger: Logger;

    constructor(dialogRef: OverlayDialogRef<CustomerDescriptor>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: DialogConfig)
    {
        super(dialogRef, inj);

        this.domain = this.inject(AppDomainContext);

        this.logger = this.domain.customers.logger.getLogger(CustomerSelectionDialogComponent);
    }

    public static open(comp: BaseComponent,
                       purpose: string,
                       okButton: string): Promise<CustomerDescriptor>
    {
        let cfg            = new DialogConfig();
        cfg.dialogPurpose  = purpose;
        cfg.dialogOkButton = okButton;

        return BaseDialogComponentSingleSelect.openInner(comp, CustomerSelectionDialogComponent, cfg, false);
    }

    protected async loadItems()
    {
        let customers = await this.domain.customers.getExtendedAll();
        for (let customer of customers)
        {
            let item      = new CustomerDescriptor();
            item.ext      = customer;
            item.customer = customer.model.name;
            this.addNewItem(item);
        }

        this.sortItems((l,
                        r) => UtilsService.compareStrings(l.customer, r.customer, true));
    }

    protected async onEmptyFilterResults()
    {
    }

    protected shouldDisplay(pattern: string,
                            item: CustomerDescriptor): boolean
    {
        if (this.containsPattern(pattern, item.customer)) return true;

        return false;
    }
}

class DialogConfig extends BaseDialogConfig
{
}

export class CustomerDescriptor extends BaseDialogSelection
{
    ext: CustomerExtended;

    customer: string;
}
