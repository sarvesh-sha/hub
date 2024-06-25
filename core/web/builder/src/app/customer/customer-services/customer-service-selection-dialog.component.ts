import {Component, Inject, Injector} from "@angular/core";

import {AppDomainContext} from "app/services/domain";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";

import * as Models from "app/services/proxy/model/models";

import {Logger} from "framework/services/logging.service";
import {UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";
import {BaseDialogComponentSingleSelect, BaseDialogConfig, BaseDialogSelection} from "framework/ui/dialogs/base.dialog";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {inParallel} from "framework/utils/concurrency";

@Component({
               templateUrl: "./customer-service-selection-dialog.component.html"
           })
export class CustomerServiceSelectionDialogComponent extends BaseDialogComponentSingleSelect<CustomerServiceSelectionDialogComponent, CustomerServiceDescriptor>
{
    private domain: AppDomainContext;
    private readonly logger: Logger;

    constructor(dialogRef: OverlayDialogRef<CustomerServiceDescriptor>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: DialogConfig)
    {
        super(dialogRef, inj);

        this.domain = this.inject(AppDomainContext);

        this.logger = this.domain.customers.logger.getLogger(CustomerServiceSelectionDialogComponent);
    }

    public static open(comp: BaseComponent,
                       purpose: string,
                       okButton: string,
                       operationalStatus?: Models.DeploymentOperationalStatus): Promise<CustomerServiceDescriptor>
    {
        let cfg               = new DialogConfig();
        cfg.dialogPurpose     = purpose;
        cfg.dialogOkButton    = okButton;
        cfg.operationalStatus = operationalStatus;

        return BaseDialogComponentSingleSelect.openInner(comp, CustomerServiceSelectionDialogComponent, cfg, false);
    }

    protected async loadItems()
    {
        let customers = await this.domain.customers.getExtendedAll();
        await inParallel(customers,
                         async (customer,
                                index) =>
                         {
                             let services = await customer.getServices();
                             for (let service of services)
                             {
                                 if (this.data.operationalStatus && service.model.operationalStatus != this.data.operationalStatus) continue;

                                 let item      = new CustomerServiceDescriptor();
                                 item.svc      = service;
                                 item.customer = customer.model.name;
                                 item.service  = service.model.name;
                                 this.addNewItem(item);
                             }
                         });

        this.sortItems((l,
                        r) =>
                       {
                           let diff = UtilsService.compareStrings(l.customer, r.customer, true);
                           if (diff == 0)
                           {
                               diff = UtilsService.compareStrings(l.service, r.service, true);
                           }

                           return diff;
                       });
    }

    protected async onEmptyFilterResults()
    {
    }

    protected shouldDisplay(pattern: string,
                            item: CustomerServiceDescriptor): boolean
    {
        if (this.containsPattern(pattern, item.customer)) return true;
        if (this.containsPattern(pattern, item.service)) return true;

        return false;
    }
}

class DialogConfig extends BaseDialogConfig
{
    operationalStatus: Models.DeploymentOperationalStatus;
}

export class CustomerServiceDescriptor extends BaseDialogSelection
{
    svc: CustomerServiceExtended;

    customer: string;

    service: string;
}
