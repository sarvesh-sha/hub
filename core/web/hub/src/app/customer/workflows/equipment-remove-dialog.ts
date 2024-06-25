import {WorkflowDialogBase} from "app/customer/workflows/workflow-dialog-base";
import {LogicalAssetExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";

import * as Models from "app/services/proxy/model/models";
import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {DialogConfirmComponent} from "framework/ui/dialogs/dialog-confirm.component";

export class EquipmentRemoveDialog
{
    public static async open(comp: BaseApplicationComponent,
                             clickedId: string,
                             table: DatatableManager<Models.RecordIdentity, any, any>): Promise<boolean>
    {

        let config = await WorkflowDialogBase.getConfig(LogicalAssetExtended, comp, clickedId, table);

        let remove = await DialogConfirmComponent.execute(comp, "Are you sure?", `Are you sure you want to remove ${config.assets.length} equipment?`);

        if (remove)
        {
            let workflowEquipment = config.assets.map((eq) => Models.WorkflowAsset.newInstance({
                                                                                                   sysId: eq.typedModel.sysId,
                                                                                                   name : eq.typedModel.name
                                                                                               }));
            let workflowExtended  = WorkflowExtended.create(comp.app.domain.events,
                                                            Models.WorkflowType.RemoveEquipment,
                                                            Models.WorkflowDetailsForRemoveEquipment.newInstance({
                                                                                                                     equipments: workflowEquipment
                                                                                                                 }),
                                                            `Removing ${workflowEquipment.length} equipment`);

            await comp.app.domain.workflows.create(workflowExtended.typedModel);

            return true;
        }

        return false;
    }
}
