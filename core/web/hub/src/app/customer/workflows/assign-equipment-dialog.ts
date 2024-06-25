import {Component, Inject, Injector} from "@angular/core";
import {WorkflowDialogBase, WorkflowDialogConfig} from "app/customer/workflows/workflow-dialog-base";
import {DeviceElementExtended, LogicalAssetExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";
import {EquipmentSelectionExtended} from "app/services/domain/report-definitions.service";

import * as Models from "app/services/proxy/model/models";
import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               template: `
                   <o3-standard-form label="Assign points to equipment"
                                     primaryButtonText="Save"
                                     secondaryButtonText="Cancel"
                                     (submitted)="save()"
                                     (cancelled)="dialogRef.close(false)">
                       <o3-equipment-selector *ngIf="equipmentSelection"
                                              [selection]="equipmentSelection"
                                              [limit]="1"></o3-equipment-selector>
                   </o3-standard-form>`
           })
export class AssignEquipmentDialog extends WorkflowDialogBase
{
    equipmentSelection: EquipmentSelectionExtended;

    constructor(inj: Injector,
                public dialogRef: OverlayDialogRef<boolean>,
                @Inject(OVERLAY_DATA) public data: WorkflowDialogConfig<DeviceElementExtended>)
    {
        super(inj);
        this.initialize();
    }

    public static async open(comp: BaseApplicationComponent,
                             clickedId: string,
                             table: DatatableManager<Models.RecordIdentity, any, any>): Promise<boolean>
    {
        return super.openDialog(DeviceElementExtended, comp, AssignEquipmentDialog, clickedId, table);
    }

    public static getOverlayConfig(): OverlayConfig
    {
        const cfg = super.getOverlayConfig();
        cfg.width = "700px";
        return cfg;
    }

    private async initialize()
    {
        let parents: Models.RecordIdentity[] = [];
        let firstPoint                       = this.data.assets[0];
        if (firstPoint)
        {
            parents = await firstPoint.getParentsOfRelation(Models.AssetRelationship.controls);
        }
        this.equipmentSelection = new EquipmentSelectionExtended(this.app.domain, parents || []);
    }

    async save()
    {
        let parentName: string;
        let parentSysId: string;
        if (this.equipmentSelection.identities && this.equipmentSelection.identities.length === 1)
        {
            parentSysId = this.equipmentSelection.identities[0].sysId;
            let equip   = await this.app.domain.assets.getTypedExtendedById(LogicalAssetExtended, parentSysId);
            parentName  = equip.model.name;
        }

        let points = this.data.assets.map((p) => p.model.sysId);

        let parentEquipment = parentSysId ? Models.WorkflowAsset.newInstance({
                                                                                 sysId: parentSysId,
                                                                                 name : parentName
                                                                             }) : null;

        let description = `Assigning ${points.length} control point${points.length && "s" || ""} to `;
        if (parentEquipment)
        {
            description += `equipment "${parentName}"`;
        }
        else
        {
            description += `no equipment`;
        }

        let workflowExtended = WorkflowExtended.create(this.app.domain.events,
                                                       Models.WorkflowType.AssignControlPointsToEquipment,
                                                       Models.WorkflowDetailsForAssignControlPointsToEquipment.newInstance({
                                                                                                                               equipment    : parentEquipment,
                                                                                                                               controlPoints: points
                                                                                                                           }),
                                                       description);

        await this.app.domain.workflows.create(workflowExtended.typedModel);

        this.dialogRef.close(true);
    }
}
