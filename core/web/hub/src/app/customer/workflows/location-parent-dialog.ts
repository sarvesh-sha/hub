import {Component, Inject, Injector} from "@angular/core";

import {WorkflowDialogBase, WorkflowDialogConfig} from "app/customer/workflows/workflow-dialog-base";
import {AssetExtended, LocationExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               template: `
                   <o3-standard-form label="Set Parent Location" primaryButtonText="Save" (submitted)="save()" [showSecondary]="false">
                       <mat-form-field>
                           <o3-select name="location" [o3LocationSelector]="true" placeholder="Parent" [(ngModel)]="locationSysId" [multiSelect]="false">
                           </o3-select>
                       </mat-form-field>
                   </o3-standard-form>`
           })
export class LocationParentDialog extends WorkflowDialogBase
{
    locationSysId: string;

    constructor(inj: Injector,
                public dialogRef: OverlayDialogRef<boolean>,
                @Inject(OVERLAY_DATA) public data: WorkflowDialogConfig<AssetExtended>)
    {
        super(inj);
    }

    public static open(comp: BaseApplicationComponent,
                       clickedId: string,
                       table: DatatableManager<Models.RecordIdentity, any, any>): Promise<boolean>
    {
        return super.openDialog(LocationExtended, comp, LocationParentDialog, clickedId, table);
    }

    async save()
    {
        let workflowDetails = Models.WorkflowDetailsForSetLocationParent.newInstance({
                                                                                         parentLocationSysId: this.locationSysId,
                                                                                         childLocationSysIds: this.data.assets.map((device) => device.typedModel.sysId)
                                                                                     });

        let workflowExtended = WorkflowExtended.create(this.app.domain.events,
                                                       Models.WorkflowType.SetLocationParent,
                                                       workflowDetails);

        await this.app.domain.workflows.create(workflowExtended.typedModel);

        this.dialogRef.close(true);
    }
}
