import {Component} from "@angular/core";
import {WorkflowWizardData} from "app/customer/workflows/wizard/workflow-wizard.component";
import {DeviceElementExtended, DeviceExtended, LogicalAssetExtended} from "app/services/domain/assets.service";
import {WorkflowExtended} from "app/services/domain/events.service";
import * as Models from "app/services/proxy/model/models";
import {ControlOption} from "framework/ui/control-option";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-workflow-wizard-type-step",
               templateUrl: "./workflow-wizard-type-step.component.html",
               providers  : [WizardStep.createProvider(WorkflowWizardTypeStepComponent)]
           })
export class WorkflowWizardTypeStepComponent extends WizardStep<WorkflowWizardData>
{
    selectedType: Models.WorkflowType;
    types: ControlOption<Models.WorkflowType>[] = [];

    device: DeviceExtended;
    deviceElement: DeviceElementExtended;
    equipment: LogicalAssetExtended;

    public async onData()
    {
        await super.onData();

        this.device        = this.data.deviceContext;
        this.deviceElement = this.data.deviceElementContext;
        this.equipment     = this.data.equipmentContext;

        let types = await this.data.app.bindings.getWorkflowTypes();
        if (this.equipment)
        {
            types = types.filter((type) =>
                                 {
                                     switch (type.id)
                                     {
                                         case Models.WorkflowType.AssignControlPointsToEquipment:
                                         case Models.WorkflowType.MergeEquipments:
                                         case Models.WorkflowType.RemoveEquipment:
                                         case Models.WorkflowType.RenameEquipment:
                                         case Models.WorkflowType.SetEquipmentClass:
                                         case Models.WorkflowType.SetEquipmentLocation:
                                         case Models.WorkflowType.SetEquipmentParent:
                                             return true;

                                         default:
                                             return false;
                                     }
                                 });

            this.selectedType = Models.WorkflowType.RenameEquipment;
        }
        else if (this.device)
        {
            types = types.filter((type) =>
                                 {
                                     switch (type.id)
                                     {
                                         case Models.WorkflowType.RenameDevice:
                                         case Models.WorkflowType.SetDeviceLocation:
                                             return true;

                                         default:
                                             return false;
                                     }
                                 });

            this.selectedType = Models.WorkflowType.RenameDevice;
        }
        else if (this.deviceElement)
        {
            types = types.filter((type) =>
                                 {
                                     switch (type.id)
                                     {
                                         case Models.WorkflowType.SetControlPointsClass:
                                         case Models.WorkflowType.RenameControlPoint:
                                         case Models.WorkflowType.AssignControlPointsToEquipment:
                                             return true;

                                         default:
                                             return false;
                                     }
                                 });

            this.selectedType = Models.WorkflowType.RenameControlPoint;
        }
        else
        {
            types             = types.filter((type) => type.id === Models.WorkflowType.NewEquipment);
            this.selectedType = Models.WorkflowType.NewEquipment;
        }

        this.types = types;
        this.typeChange(this.selectedType);
    }

    public getLabel(): string
    {
        return "Type";
    }

    public isEnabled(): boolean
    {
        return this.types && this.types.length > 1;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }

    public typeChange(type: Models.WorkflowType)
    {
        this.selectedType = type;
        switch (this.selectedType)
        {
            case Models.WorkflowType.AssignControlPointsToEquipment:
                this.assignPointsToEquipment();
                break;

            case Models.WorkflowType.IgnoreDevice:
                this.ignoreDevice();
                break;

            case Models.WorkflowType.MergeEquipments:
                this.mergeEquipment();
                break;

            case Models.WorkflowType.NewEquipment:
                this.newEquipment();
                break;

            case Models.WorkflowType.RemoveEquipment:
                this.removeEquipment();
                break;

            case Models.WorkflowType.RenameControlPoint:
                this.renameControlPoint();
                break;

            case Models.WorkflowType.RenameDevice:
                this.renameDevice();
                break;

            case Models.WorkflowType.RenameEquipment:
                this.renameEquipment();
                break;

            case Models.WorkflowType.SetControlPointsClass:
                this.changeControlPointClass();
                break;

            case Models.WorkflowType.SetDeviceLocation:
                this.changeDeviceLocation();
                break;

            case Models.WorkflowType.SetEquipmentClass:
                this.changeEquipClass();
                break;

            case Models.WorkflowType.SetEquipmentLocation:
                this.changeEquipLocation();
                break;

            case Models.WorkflowType.SetEquipmentParent:
                this.changeEquipParent();
                break;
        }
    }

    renameDevice()
    {
        this.createWorkflow(`Rename ${this.device.typedModel.name}`,
                            Models.WorkflowDetailsForRenameDevice.newInstance({
                                                                                  devices      : [
                                                                                      Models.WorkflowAsset.newInstance({
                                                                                                                           sysId: this.device.typedModel.sysId,
                                                                                                                           name : this.device.typedModel.name
                                                                                                                       })
                                                                                  ],
                                                                                  deviceNewName: ""
                                                                              }));
    }

    ignoreDevice()
    {
        this.createWorkflow(`Ignore ${this.device.typedModel.name}`,
                            Models.WorkflowDetailsForIgnoreDevice.newInstance({
                                                                                  deviceName : this.device.typedModel.name,
                                                                                  deviceSysId: this.device.typedModel.sysId
                                                                              }));
    }

    changeDeviceLocation()
    {
        this.createWorkflow(`Change location for ${this.device.typedModel.name}`,
                            Models.WorkflowDetailsForSetDeviceLocation.newInstance({
                                                                                       devices: [
                                                                                           Models.WorkflowAsset.newInstance({
                                                                                                                                sysId: this.device.typedModel.sysId,
                                                                                                                                name : this.device.typedModel.name
                                                                                                                            })
                                                                                       ]
                                                                                   }));
    }

    renameEquipment()
    {
        this.createWorkflow(`Rename ${this.equipment.typedModel.name}`,
                            Models.WorkflowDetailsForRenameEquipment.newInstance({
                                                                                     equipments      : [
                                                                                         Models.WorkflowAsset.newInstance({
                                                                                                                              name : this.equipment.typedModel.name,
                                                                                                                              sysId: this.equipment.typedModel.sysId
                                                                                                                          })
                                                                                     ],
                                                                                     equipmentNewName: ""
                                                                                 }));
    }

    changeEquipClass()
    {
        this.createWorkflow(`Change Equipment Class for ${this.equipment.typedModel.name}`,
                            Models.WorkflowDetailsForSetEquipmentClass.newInstance({
                                                                                       equipments      : [
                                                                                           Models.WorkflowAsset.newInstance({
                                                                                                                                name : this.equipment.typedModel.name,
                                                                                                                                sysId: this.equipment.typedModel.sysId
                                                                                                                            })
                                                                                       ],
                                                                                       equipmentClassId: this.equipment.typedModel.equipmentClassId
                                                                                   }));
    }

    changeEquipLocation()
    {
        this.createWorkflow(`Change location for ${this.equipment.typedModel.name}`,
                            Models.WorkflowDetailsForSetEquipmentLocation.newInstance({
                                                                                          equipments: [
                                                                                              Models.WorkflowAsset.newInstance({
                                                                                                                                   name : this.equipment.typedModel.name,
                                                                                                                                   sysId: this.equipment.typedModel.sysId
                                                                                                                               })
                                                                                          ]
                                                                                      }));
    }

    changeEquipParent()
    {
        this.createWorkflow(`Change parent for ${this.equipment.typedModel.name}`,
                            Models.WorkflowDetailsForSetEquipmentParent.newInstance({
                                                                                        childEquipments: [
                                                                                            Models.WorkflowAsset.newInstance({
                                                                                                                                 name : this.equipment.typedModel.name,
                                                                                                                                 sysId: this.equipment.typedModel.sysId
                                                                                                                             })
                                                                                        ]
                                                                                    }));

    }

    removeEquipment()
    {
        this.createWorkflow(`Remove equipment ${this.equipment.typedModel.name}`,
                            Models.WorkflowDetailsForRemoveEquipment.newInstance({
                                                                                     equipments: [
                                                                                         Models.WorkflowAsset.newInstance({
                                                                                                                              name : this.equipment.typedModel.name,
                                                                                                                              sysId: this.equipment.typedModel.sysId
                                                                                                                          })
                                                                                     ]
                                                                                 }));
    }

    mergeEquipment()
    {
        this.createWorkflow(`Merge equipment ${this.equipment.typedModel.name}`,
                            Models.WorkflowDetailsForMergeEquipments.newInstance({
                                                                                     equipment1: Models.WorkflowAsset.newInstance({
                                                                                                                                      name : this.equipment.typedModel.name,
                                                                                                                                      sysId: this.equipment.typedModel.sysId
                                                                                                                                  })
                                                                                 }));
    }

    assignPointsToEquipment()
    {
        if (this.equipment)
        {
            this.createWorkflow(`Assign points to equipment ${this.equipment.typedModel.name}`,
                                Models.WorkflowDetailsForAssignControlPointsToEquipment.newInstance({
                                                                                                        equipment    : Models.WorkflowAsset.newInstance({
                                                                                                                                                            name : this.equipment.typedModel.name,
                                                                                                                                                            sysId: this.equipment.typedModel.sysId
                                                                                                                                                        }),
                                                                                                        controlPoints: []
                                                                                                    }));
        }

        if (this.deviceElement)
        {
            this.createWorkflow(`Assign point ${this.deviceElement.typedModel.name} to equipment`,
                                Models.WorkflowDetailsForAssignControlPointsToEquipment.newInstance({
                                                                                                        controlPoints: [this.deviceElement.typedModel.sysId]
                                                                                                    }));
        }
    }

    renameControlPoint()
    {
        this.createWorkflow(`Rename point ${this.deviceElement.typedModel.name}`,
                            Models.WorkflowDetailsForRenameControlPoint.newInstance({
                                                                                        controlPoints      : [this.deviceElement.typedModel.sysId],
                                                                                        controlPointNewName: ""
                                                                                    }));
    }

    changeControlPointClass()
    {
        this.createWorkflow(`Change point class for ${this.deviceElement.typedModel.name}`,
                            Models.WorkflowDetailsForSetControlPointsClass.newInstance({
                                                                                           pointClassId : this.deviceElement.typedModel.pointClassId,
                                                                                           controlPoints: [this.deviceElement.typedModel.sysId]
                                                                                       }));
    }

    newEquipment()
    {
        this.createWorkflow(`Add new equipment`, Models.WorkflowDetailsForNewEquipment.newInstance({}));
    }

    private createWorkflow(description: string,
                           details: Models.WorkflowDetails)
    {
        this.data.workflow = WorkflowExtended.create(this.data.app.domain.events,
                                                     this.selectedType,
                                                     details,
                                                     description);
    }
}
