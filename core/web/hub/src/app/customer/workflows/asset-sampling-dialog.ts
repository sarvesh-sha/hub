import {WorkflowDialogBase} from "app/customer/workflows/workflow-dialog-base";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {WorkflowExtended} from "app/services/domain/events.service";

import * as Models from "app/services/proxy/model/models";
import {DatatableManager} from "framework/ui/datatables/datatable-manager";
import {DialogConfirmComponent} from "framework/ui/dialogs/dialog-confirm.component";

export class AssetSamplingDialog
{
    public static async open(comp: BaseApplicationComponent,
                             clickedId: string,
                             table: DatatableManager<Models.RecordIdentity, any, any>,
                             enable: boolean): Promise<boolean>
    {

        let config = await WorkflowDialogBase.getConfig(DeviceElementExtended, comp, clickedId, table);

        let confirm = await DialogConfirmComponent.execute(comp, "Are you sure?", `Are you sure you want to ${enable ? "enable" : "disable"} sampling for ${config.assets.length} points?`);

        if (confirm)
        {
            let points           = config.assets.map((point) => point.model.sysId);
            let workflowExtended = WorkflowExtended.create(comp.app.domain.events,
                                                           Models.WorkflowType.SamplingControlPoint,
                                                           Models.WorkflowDetailsForSamplingControlPoint.newInstance({
                                                                                                                         controlPoints: points,
                                                                                                                         enable       : enable
                                                                                                                     }),
                                                           `${enable ? "Enabling" : "Disabling"} sampling for ${points.length} points`);

            await comp.app.domain.workflows.create(workflowExtended.typedModel);

            return true;
        }

        return false;
    }
}
