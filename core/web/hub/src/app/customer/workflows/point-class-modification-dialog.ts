import {Component, Inject, Injector} from "@angular/core";
import {WorkflowDialogBase, WorkflowDialogConfig} from "app/customer/workflows/workflow-dialog-base";
import {AssetExtended, DeviceElementExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";

import * as Models from "app/services/proxy/model/models";
import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               template: `
                   <o3-standard-form label="Change point class"
                                     primaryButtonText="Save"
                                     secondaryButtonText="Cancel"
                                     (submitted)="save()"
                                     (cancelled)="dialogRef.close(false)">
                       <mat-form-field>
                           <o3-select name="oldPointClass"
                                      placeholder="Old Point Class"
                                      [o3PointClassSelector]="true"
                                      [ngModel]="data.assets[0].typedModel.pointClassId"
                                      [multiSelect]="false" singleClick disabled></o3-select>
                       </mat-form-field>
                       <mat-form-field>
                           <o3-select name="pointClass"
                                      placeholder="New Point Class"
                                      [o3PointClassSelector]="true"
                                      [(ngModel)]="pointClassId"
                                      [multiSelect]="false" singleClick required></o3-select>
                       </mat-form-field>
                   </o3-standard-form>`
           })
export class PointClassModificationDialog extends WorkflowDialogBase
{
    pointClassId: string;

    constructor(inj: Injector,
                public dialogRef: OverlayDialogRef<boolean>,
                @Inject(OVERLAY_DATA) public data: WorkflowDialogConfig<DeviceElementExtended>)
    {
        super(inj);
        this.pointClassId = data.assets[0].typedModel.pointClassId;
    }

    public static open(comp: BaseApplicationComponent,
                       clickedId: string,
                       table: DatatableManager<Models.RecordIdentity, any, any>): Promise<boolean>
    {
        return super.openDialog(DeviceElementExtended, comp, PointClassModificationDialog, clickedId, table);
    }

    async save()
    {
        let points = this.data.assets.map((p) => p.model.sysId);

        let workflowExtended = WorkflowExtended.create(this.app.domain.events,
                                                       Models.WorkflowType.SetControlPointsClass,
                                                       Models.WorkflowDetailsForSetControlPointsClass.newInstance({
                                                                                                                      controlPoints: points,
                                                                                                                      pointClassId : this.pointClassId
                                                                                                                  }),
                                                       `Change point class for ${points.length} control point${points.length && "s" || ""}`);

        await this.app.domain.workflows.create(workflowExtended.typedModel);

        for (let point of this.data.assets)
        {
            point.typedModel.pointClassId = this.pointClassId;
        }

        this.dialogRef.close(true);
    }
}
