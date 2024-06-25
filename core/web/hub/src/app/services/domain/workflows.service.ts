import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {EnumsService} from "app/services/domain/enums.service";
import {EventsService, WorkflowExtended} from "app/services/domain/events.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class WorkflowsService
{
    constructor(private api: ApiService,
                private enums: EnumsService,
                private events: EventsService)
    {
    }

    /**
     * Get the summary of alerts.
     */
    @ReportError
    async getSummary(filters: Models.WorkflowFilterRequest,
                     groupBy?: Models.SummaryFlavor): Promise<Models.SummaryResult[]>
    {
        let result = await this.api.workflows.getSummary(groupBy, filters);

        // Wait and sort the results, to make it nicer for the caller.
        result.sort((a,
                     b) => UtilsService.compareStrings(a.label, b.label, true));

        return result;
    }

    /**
     * Get the list of priorities.
     */
    describePriorities(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("WorkflowPriority", false);
    }

    @Memoizer
    public async getPriorities(): Promise<ControlOption<Models.WorkflowPriority>[]>
    {
        let types = await this.describePriorities();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    /**
     * Get the list of statuses.
     */
    describeStatuses(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("WorkflowStatus", true);
    }

    @Memoizer
    public async getStatuses(): Promise<ControlOption<Models.WorkflowStatus>[]>
    {
        let types = await this.describeStatuses();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    /**
     * Get the list of types.
     */
    describeTypes(): Promise<Models.EnumDescriptor[]>
    {
        return this.enums.getInfos("WorkflowType", true);
    }

    @Memoizer
    public async getTypes(): Promise<ControlOption<Models.WorkflowType>[]>
    {
        let types = await this.describeTypes();

        return SharedSvc.BaseService.mapEnumOptions(types);
    }

    //--//

    getExtendedAll(filters: Models.WorkflowFilterRequest): Promise<WorkflowExtended[]>
    {
        return this.events.getTypedExtendedAll(WorkflowExtended, filters);
    }

    getExtendedBatch(ids: Models.RecordIdentity[]): Promise<WorkflowExtended[]>
    {
        return this.events.getTypedExtendedBatch(WorkflowExtended, ids);
    }

    async create(workflow: Models.Workflow): Promise<WorkflowExtended>
    {
        workflow = await this.api.workflows.create(workflow);
        return WorkflowExtended.newInstance(this.events, workflow);
    }
}

export abstract class WorkflowDetailsExtended<T extends Models.WorkflowDetails>
{
    constructor(protected m_model: T)
    {
    }

    static newInstance(type: Models.WorkflowType,
                       model: Models.WorkflowDetails): WorkflowDetailsExtended<any>
    {
        switch (type)
        {
            case Models.WorkflowType.AssignControlPointsToEquipment:
                return new WorkflowDetailsForAssignControlPointsToEquipmentExtended(<Models.WorkflowDetailsForAssignControlPointsToEquipment>model);

            case Models.WorkflowType.IgnoreDevice:
                return new WorkflowDetailsForIgnoreDeviceExtended(<Models.WorkflowDetailsForIgnoreDevice>model);

            case Models.WorkflowType.MergeEquipments:
                return new WorkflowDetailsForMergeEquipmentsExtended(<Models.WorkflowDetailsForMergeEquipments>model);

            case Models.WorkflowType.NewEquipment:
                return new WorkflowDetailsForNewEquipmentExtended(<Models.WorkflowDetailsForNewEquipment>model);

            case Models.WorkflowType.RemoveEquipment:
                return new WorkflowDetailsForRemoveEquipmentExtended(<Models.WorkflowDetailsForRemoveEquipment>model);

            case Models.WorkflowType.RenameControlPoint:
                return new WorkflowDetailsForRenameControlPointExtended(<Models.WorkflowDetailsForRenameControlPoint>model);

            case Models.WorkflowType.RenameDevice:
                return new WorkflowDetailsForRenameDeviceExtended(<Models.WorkflowDetailsForRenameDevice>model);

            case Models.WorkflowType.RenameEquipment:
                return new WorkflowDetailsForRenameEquipmentExtended(<Models.WorkflowDetailsForRenameEquipment>model);

            case Models.WorkflowType.SetControlPointsClass:
                return new WorkflowDetailsForSetControlPointsClassExtended(<Models.WorkflowDetailsForSetControlPointsClass>model);

            case Models.WorkflowType.SetDeviceLocation:
                return new WorkflowDetailsForSetDeviceLocationExtended(<Models.WorkflowDetailsForSetDeviceLocation>model);

            case Models.WorkflowType.SetEquipmentClass:
                return new WorkflowDetailsForSetEquipmentClassExtended(<Models.WorkflowDetailsForSetEquipmentClass>model);

            case Models.WorkflowType.SetEquipmentLocation:
                return new WorkflowDetailsForSetEquipmentLocationExtended(<Models.WorkflowDetailsForSetEquipmentLocation>model);

            case Models.WorkflowType.SetEquipmentParent:
                return new WorkflowDetailsForSetEquipmentParentExtended(<Models.WorkflowDetailsForSetEquipmentParent>model);

            case Models.WorkflowType.SamplingControlPoint:
                return new WorkflowDetailsForSamplingControlPointExtended(<Models.WorkflowDetailsForSamplingControlPoint>model);

            case Models.WorkflowType.SamplingPeriod:
                return new WorkflowDetailsForSamplingPeriodExtended(<Models.WorkflowDetailsForSamplingPeriod>model);

            case Models.WorkflowType.HidingControlPoint:
                return new WorkflowDetailsForHidingControlPointExtended(<Models.WorkflowDetailsForHidingControlPoint>model);

            case Models.WorkflowType.SetLocationParent:
                return new WorkflowDetailsForSetLocationParentExtended(<Models.WorkflowDetailsForSetLocationParent>model);
        }

        throw Error("Unknown type " + type);
    }

    abstract getTitle(): string;

    public getDeviceList(): WorkflowTarget[]
    {
        return null;
    }

    public getPrimaryEquipment(): WorkflowTarget
    {
        return null;
    }

    public setPrimaryEquipment(id: string,
                               name: string): void
    {
    }

    public getSecondaryEquipment(): WorkflowTarget
    {
        return null;
    }

    public setSecondaryEquipment(id: string,
                                 name: string): void
    {
    }

    public getEquipmentList(): WorkflowTarget[]
    {
        return null;
    }

    public setEquipmentList(equipment: WorkflowTarget[]): void
    {
    }

    public getControlPointSelection(): string[]
    {
        return null;
    }

    public setControlPointSelection(ids: string[])
    {
    }
}

export interface WorkflowTarget
{
    id: string;
    name: string;
    label?: string;
}

export class WorkflowDetailsForAssignControlPointsToEquipmentExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForAssignControlPointsToEquipment>
{
    public getTitle(): string
    {
        return "Assign Control Points To Equipment";
    }

    public getPrimaryEquipment(): WorkflowTarget
    {
        return {
            id  : this.m_model.equipment?.sysId,
            name: this.m_model.equipment?.name
        };
    }

    public setPrimaryEquipment(id: string,
                               name: string): void
    {
        this.m_model.equipment = Models.WorkflowAsset.newInstance({
                                                                      sysId: id,
                                                                      name : name
                                                                  });
    }

    public getControlPointSelection(): string[]
    {
        return this.m_model.controlPoints;
    }

    public setControlPointSelection(ids: string[])
    {
        this.m_model.controlPoints = ids;
    }
}

export class WorkflowDetailsForIgnoreDeviceExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForIgnoreDevice>
{
    public getTitle(): string
    {
        return "Ignore Device";
    }

    public getDeviceList(): WorkflowTarget[]
    {
        return [
            {
                id  : this.m_model.deviceSysId,
                name: this.m_model.deviceName
            }
        ];
    }
}

export class WorkflowDetailsForMergeEquipmentsExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForMergeEquipments>
{
    public getTitle(): string
    {
        return "Merge Equipments";
    }

    public getPrimaryEquipment(): WorkflowTarget
    {
        return {
            id   : this.m_model.equipment1?.sysId,
            name : this.m_model.equipment1?.name,
            label: "Equipment 1"
        };
    }

    public setPrimaryEquipment(id: string,
                               name: string): void
    {
        this.m_model.equipment1 = Models.WorkflowAsset.newInstance({
                                                                       sysId: id,
                                                                       name : name
                                                                   });
    }

    public getSecondaryEquipment(): WorkflowTarget
    {
        return {
            id   : this.m_model.equipment2?.sysId,
            name : this.m_model.equipment2?.name,
            label: "Equipment 2"
        };
    }

    public setSecondaryEquipment(id: string,
                                 name: string): void
    {
        this.m_model.equipment2 = Models.WorkflowAsset.newInstance({
                                                                       sysId: id,
                                                                       name : name
                                                                   });
    }
}

export class WorkflowDetailsForNewEquipmentExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForNewEquipment>
{
    public getTitle(): string
    {
        return "New equipment";
    }

    public getPrimaryEquipment(): WorkflowTarget
    {
        return {
            id   : this.m_model.parentEquipment?.sysId,
            name : this.m_model.parentEquipment?.name,
            label: "Parent Equipment"
        };
    }

    public setPrimaryEquipment(id: string,
                               name: string): void
    {
        this.m_model.parentEquipment = Models.WorkflowAsset.newInstance({
                                                                            sysId: id,
                                                                            name : name
                                                                        });
    }
}

export class WorkflowDetailsForRemoveEquipmentExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForRemoveEquipment>
{
    public getTitle(): string
    {
        return "Remove Equipment";
    }

    public getEquipmentList(): WorkflowTarget[]
    {
        return this.m_model.equipments.map((eq) =>
                                           {
                                               return {
                                                   id  : eq.sysId,
                                                   name: eq.name
                                               };
                                           });
    }

    public setEquipmentList(equipments: WorkflowTarget[]): void
    {
        this.m_model.equipments = equipments.map((eq) => Models.WorkflowAsset.newInstance({
                                                                                              sysId: eq.id,
                                                                                              name : eq.name
                                                                                          }));
    }
}

export class WorkflowDetailsForRenameControlPointExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForRenameControlPoint>
{
    public getTitle(): string
    {
        return "Rename Control Point";
    }

    public getControlPointSelection(): string[]
    {
        return this.m_model.controlPoints;
    }

    public setControlPointSelection(ids: string[])
    {
        this.m_model.controlPoints = ids;
    }
}

export class WorkflowDetailsForRenameDeviceExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForRenameDevice>
{
    public getTitle(): string
    {
        return "Rename Device";
    }

    public getDeviceList(): WorkflowTarget[]
    {
        return this.m_model.devices.map((dev) =>
                                        {
                                            return {
                                                id  : dev.sysId,
                                                name: dev.name
                                            };
                                        });
    }
}

export class WorkflowDetailsForRenameEquipmentExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForRenameEquipment>
{
    public getTitle(): string
    {
        return "Rename Equipment";
    }

    public getEquipmentList(): WorkflowTarget[]
    {
        return this.m_model.equipments.map((eq) =>
                                           {
                                               return {
                                                   id  : eq.sysId,
                                                   name: eq.name
                                               };
                                           });
    }

    public setEquipmentList(equipments: WorkflowTarget[]): void
    {
        this.m_model.equipments = equipments.map((eq) => Models.WorkflowAsset.newInstance({
                                                                                              sysId: eq.id,
                                                                                              name : eq.name
                                                                                          }));
    }
}

export class WorkflowDetailsForSetControlPointsClassExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForSetControlPointsClass>
{
    public getTitle(): string
    {
        return "Set Point Class";
    }

    public getControlPointSelection(): string[]
    {
        return this.m_model.controlPoints;
    }

    public setControlPointSelection(ids: string[])
    {
        this.m_model.controlPoints = ids;
    }
}

export class WorkflowDetailsForSetDeviceLocationExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForSetDeviceLocation>
{
    public getTitle(): string
    {
        return "Set Device Location";
    }

    public getDeviceList(): WorkflowTarget[]
    {
        return this.m_model.devices.map((dev) =>
                                        {
                                            return {
                                                id  : dev.sysId,
                                                name: dev.name
                                            };
                                        });
    }
}

export class WorkflowDetailsForSetEquipmentClassExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForSetEquipmentClass>
{
    public getTitle(): string
    {
        return "Set Equipment Class";
    }

    public getEquipmentList(): WorkflowTarget[]
    {
        return this.m_model.equipments.map((eq) =>
                                           {
                                               return {
                                                   id  : eq.sysId,
                                                   name: eq.name
                                               };
                                           });
    }

    public setEquipmentList(equipments: WorkflowTarget[]): void
    {
        this.m_model.equipments = equipments.map((eq) => Models.WorkflowAsset.newInstance({
                                                                                              sysId: eq.id,
                                                                                              name : eq.name
                                                                                          }));
    }
}

export class WorkflowDetailsForSetEquipmentLocationExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForSetEquipmentLocation>
{
    public getTitle(): string
    {
        return "Set Equipment Location";
    }

    public getEquipmentList(): WorkflowTarget[]
    {
        return this.m_model.equipments.map((eq) =>
                                           {
                                               return {
                                                   id  : eq.sysId,
                                                   name: eq.name
                                               };
                                           });
    }

    public setEquipmentList(equipments: WorkflowTarget[]): void
    {
        this.m_model.equipments = equipments.map((eq) => Models.WorkflowAsset.newInstance({
                                                                                              sysId: eq.id,
                                                                                              name : eq.name
                                                                                          }));
    }
}

export class WorkflowDetailsForSetEquipmentParentExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForSetEquipmentParent>
{
    public getTitle(): string
    {
        return "Set Equipment Parent";
    }

    public getEquipmentList(): WorkflowTarget[]
    {
        return this.m_model.childEquipments.map((eq) =>
                                                {
                                                    return {
                                                        id   : eq.sysId,
                                                        name : eq.name,
                                                        label: "Child Equipment"
                                                    };
                                                });
    }

    public setEquipmentList(equipments: WorkflowTarget[]): void
    {
        this.m_model.childEquipments = equipments.map((eq) => Models.WorkflowAsset.newInstance({
                                                                                                   sysId: eq.id,
                                                                                                   name : eq.name
                                                                                               }));
    }

    public getSecondaryEquipment(): WorkflowTarget
    {
        return {
            id   : this.m_model.parentEquipment?.sysId,
            name : this.m_model.parentEquipment?.name,
            label: "Parent Equipment"
        };
    }

    public setSecondaryEquipment(id: string,
                                 name: string): void
    {
        this.m_model.parentEquipment = Models.WorkflowAsset.newInstance({
                                                                            sysId: id,
                                                                            name : name
                                                                        });
    }
}

export class WorkflowDetailsForSamplingControlPointExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForSamplingControlPoint>
{
    public getTitle(): string
    {
        return this.m_model.enable ? "Enable Sampling" : "Disable Sampling";
    }

    public getControlPointSelection(): string[]
    {
        return this.m_model.controlPoints;
    }

    public setControlPointSelection(ids: string[])
    {
        this.m_model.controlPoints = ids;
    }
}

export class WorkflowDetailsForSamplingPeriodExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForSamplingPeriod>
{
    public getTitle(): string
    {
        return "Set Sampling Period";
    }

    public getControlPointSelection(): string[]
    {
        return this.m_model.controlPoints;
    }

    public setControlPointSelection(ids: string[])
    {
        this.m_model.controlPoints = ids;
    }
}

export class WorkflowDetailsForHidingControlPointExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForHidingControlPoint>
{
    public getTitle(): string
    {
        return this.m_model.hide ? "Hide Control Points" : "Show Control Points";
    }

    public getControlPointSelection(): string[]
    {
        return this.m_model.controlPoints;
    }

    public setControlPointSelection(ids: string[])
    {
        this.m_model.controlPoints = ids;
    }
}

export class WorkflowDetailsForSetLocationParentExtended extends WorkflowDetailsExtended<Models.WorkflowDetailsForSetLocationParent>
{
    public getTitle(): string
    {
        return "Set Location Parent";
    }

    public getChildLocations(): string[]
    {
        return this.m_model.childLocationSysIds;
    }

    public setChildLocations(ids: string[])
    {
        this.m_model.childLocationSysIds = ids;
    }
}
