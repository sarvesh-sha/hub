import {Component, Inject, Injector} from "@angular/core";
import {WorkflowDialogBase, WorkflowDialogConfig} from "app/customer/workflows/workflow-dialog-base";
import {LogicalAssetExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";
import {EquipmentSelectionExtended} from "app/services/domain/report-definitions.service";

import * as Models from "app/services/proxy/model/models";
import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               template: `
                   <o3-standard-form label="Set equipment parent"
                                     primaryButtonText="Save"
                                     secondaryButtonText="Cancel"
                                     (submitted)="save()"
                                     (cancelled)="dialogRef.close(false)">
                       <o3-equipment-selector *ngIf="equipmentSelection"
                                              [selection]="equipmentSelection"
                                              [limit]="1"></o3-equipment-selector>
                   </o3-standard-form>`
           })
export class EquipmentParentDialog extends WorkflowDialogBase
{
    equipmentSelection: EquipmentSelectionExtended;

    constructor(inj: Injector,
                public dialogRef: OverlayDialogRef<boolean>,
                @Inject(OVERLAY_DATA) public data: WorkflowDialogConfig<LogicalAssetExtended>)
    {
        super(inj);
        this.initialize();
    }

    public static async open(comp: BaseApplicationComponent,
                             clickedId: string,
                             table: DatatableManager<Models.RecordIdentity, any, any>): Promise<boolean>
    {
        return super.openDialog(LogicalAssetExtended, comp, EquipmentParentDialog, clickedId, table);
    }

    private async initialize()
    {
        let parents: Models.RecordIdentity[] = [];
        let firstAsset                       = this.data.assets[0];
        if (firstAsset)
        {
            parents = await firstAsset.getParentsOfRelation(Models.AssetRelationship.controls);
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

        let childEquipments = this.data.assets.map((eq) => Models.WorkflowAsset.newInstance({
                                                                                                sysId: eq.typedModel.sysId,
                                                                                                name : eq.typedModel.name
                                                                                            }));

        let parentEquipment = parentSysId ? Models.WorkflowAsset.newInstance({
                                                                                 sysId: parentSysId,
                                                                                 name : parentName
                                                                             }) : null;

        let description: string;
        if (parentEquipment)
        {
            description = `Setting equipment "${parentName}" as parent for ${childEquipments.length} equipment`;
        }
        else
        {
            description = `Setting ${childEquipments.length} equipment to be root level equipment`;
        }

        let workflowExtended = WorkflowExtended.create(this.app.domain.events,
                                                       Models.WorkflowType.SetEquipmentParent,
                                                       Models.WorkflowDetailsForSetEquipmentParent.newInstance({
                                                                                                                   parentEquipment: parentEquipment,
                                                                                                                   childEquipments: childEquipments
                                                                                                               }),
                                                       description);

        await this.app.domain.workflows.create(workflowExtended.typedModel);

        this.dialogRef.close(true);
    }
}
