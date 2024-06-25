import {Component, Inject, Injector} from "@angular/core";
import {WorkflowDialogBase, WorkflowDialogConfig} from "app/customer/workflows/workflow-dialog-base";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";
import * as Models from "app/services/proxy/model/models";
import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               template: `
                   <o3-standard-form label="Set Sampling Period"
                                     primaryButtonText="Save"
                                     secondaryButtonText="Cancel"
                                     (submitted)="save()"
                                     (cancelled)="dialogRef.close(false)">
                       <mat-form-field>
                           <input name="oldSamplingPeriod" type="number" placeholder="Old Sampling Period" matInput [ngModel]="oldSamplingPeriod" readonly required/>
                       </mat-form-field>
                       <mat-form-field>
                           <input name="samplingPeriod" type="number" placeholder="New Sampling Period" matInput [(ngModel)]="samplingPeriod" required/>
                       </mat-form-field>
                   </o3-standard-form>`
           })
export class AssetSamplingPeriodDialog extends WorkflowDialogBase
{
    oldSamplingPeriod: number;
    samplingPeriod: number;

    constructor(inj: Injector,
                public dialogRef: OverlayDialogRef<boolean>,
                @Inject(OVERLAY_DATA) public data: WorkflowDialogConfig<DeviceElementExtended>)
    {
        super(inj);
        let firstElement = this.data.assets[0];
        this.oldSamplingPeriod = firstElement.typedModel.samplingSettings[0]?.samplingPeriod || 900;
        this.samplingPeriod = this.oldSamplingPeriod;
    }

    public static open(comp: BaseApplicationComponent,
                       clickedId: string,
                       table: DatatableManager<Models.RecordIdentity, any, any>): Promise<boolean>
    {
        return super.openDialog(DeviceElementExtended, comp, AssetSamplingPeriodDialog, clickedId, table);
    }

    async save()
    {
        let workflowDetails = Models.WorkflowDetailsForSamplingPeriod.newInstance({
                                                                                      controlPoints : this.data.assets.map((asset) => asset.model.sysId),
                                                                                      samplingPeriod: this.samplingPeriod
                                                                                  });
        let workflowType    = Models.WorkflowType.SamplingPeriod;


        let workflowExtended = WorkflowExtended.create(this.app.domain.events,
                                                       workflowType,
                                                       workflowDetails,
                                                       `Setting sampling period to ${this.samplingPeriod} seconds for ${this.data.assets.length} control point${this.data.assets.length && "s" || ""}`);

        await this.app.domain.workflows.create(workflowExtended.typedModel);

        this.dialogRef.close(true);
    }
}
