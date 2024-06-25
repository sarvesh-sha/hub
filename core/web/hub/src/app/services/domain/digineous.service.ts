import {Injectable} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import * as SharedSvc from "app/services/domain/base.service";
import {UsersService} from "app/services/domain/users.service";
import * as Models from "app/services/proxy/model/models";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {inParallel, mapInParallel} from "framework/utils/concurrency";

const endpoint__DEVICE_TEMPLATE_NEW     = "device-template-new";
const endpoint__DEVICE_TEMPLATE_LIST    = "device-template-list";
const endpoint__DEVICE_TEMPLATE_GET     = "device-template-get";
const endpoint__DEVICE_TEMPLATE_DELETE  = "device-template-delete";
const endpoint__DEVICE_TEMPLATE_SET     = "device-template-set";
const endpoint__MACHINE_TEMPLATE_LIST   = "machine-template-list";
const endpoint__MACHINE_TEMPLATE_GET    = "machine-template-get";
const endpoint__MACHINE_TEMPLATE_DELETE = "machine-template-delete";
const endpoint__MACHINE_TEMPLATE_SET    = "machine-template-set";
const endpoint__VIBRATION_LIST          = "vibration-list";
const endpoint__MACHINE_LIST            = "machine-list";
const endpoint__MACHINE_CREATE          = "machine-create";
const endpoint__MACHINE_GET             = "machine-get";
const endpoint__MACHINE_DELETE          = "machine-delete";
const endpoint__MACHINE_SET             = "machine-set";
const endpoint__DEVICE_ACTIVE           = "device-active";

@Injectable()
export class DigineousService
{
    constructor(private api: ApiService,
                private users: UsersService)
    {
    }

    get isEnabled()
    {
        return this.users.instanceConfiguration instanceof Models.InstanceConfigurationForDigineous;
    }

    //--//

    getFlavorOptions(): ControlOption<string>[]
    {
        return [
            new ControlOption<string>(Models.DigineousDeviceFlavor.BlackBox, "BlackBox"),
            new ControlOption<string>(Models.DigineousDeviceFlavor.InfiniteImpulse_Avg, "Vibration Monitor - Average"),
            new ControlOption<string>(Models.DigineousDeviceFlavor.InfiniteImpulse_Max, "Vibration Monitor - Maximum"),
            new ControlOption<string>(Models.DigineousDeviceFlavor.InfiniteImpulse_Min, "Vibration Monitor - Minimum")
        ];
    }

    //--//

    async newDeviceTemplate(flavor: Models.DigineousDeviceFlavor): Promise<Models.DigineousDeviceLibrary>
    {
        let res = await this.api.dataConnections.receiveRaw(endpoint__DEVICE_TEMPLATE_NEW, flavor);
        return res ? Models.DigineousDeviceLibrary.newInstance(res) : null;
    }

    async getDeviceTemplateIds(): Promise<string[]>
    {
        let res = await this.api.dataConnections.receiveRaw(endpoint__DEVICE_TEMPLATE_LIST);
        return <string[]>res;
    }

    async getDeviceTemplates(): Promise<Models.DigineousDeviceLibrary[]>
    {
        let ids = await this.getDeviceTemplateIds();

        return mapInParallel(ids, async (id) => this.getDeviceTemplate(id));
    }

    async getDeviceTemplate(id: string): Promise<Models.DigineousDeviceLibrary>
    {
        let res = await this.api.dataConnections.receiveRaw(endpoint__DEVICE_TEMPLATE_GET, id);
        return res ? Models.DigineousDeviceLibrary.newInstance(res) : null;
    }

    async setDeviceTemplate(id: string,
                            library: Models.DigineousDeviceLibrary): Promise<Models.DigineousDeviceLibrary>
    {
        let res = await this.api.dataConnections.receiveRaw(endpoint__DEVICE_TEMPLATE_SET, id, library);
        return res ? Models.DigineousDeviceLibrary.newInstance(res) : null;
    }

    async deleteDeviceTemplate(id: string): Promise<void>
    {
        await this.api.dataConnections.receiveRaw(endpoint__DEVICE_TEMPLATE_DELETE, id);
    }

    async getDeviceTemplateOptions(): Promise<ControlOption<string>[]>
    {
        let templateList = await this.getDeviceTemplates();

        let options = SharedSvc.BaseService.mapOptions(templateList, (template) =>
        {
            let option   = new ControlOption<string>();
            option.id    = template.id;
            option.label = template.name;
            return option;
        });

        options.sort((a,
                      b) => UtilsService.compareStrings(a.label, b.label, true));

        return options;
    }

    //--//

    async getMachineTemplateIds(): Promise<string[]>
    {
        let res = await this.api.dataConnections.receiveRaw(endpoint__MACHINE_TEMPLATE_LIST);
        return <string[]>res;
    }

    async getMachineTemplates(): Promise<Models.DigineousMachineLibrary[]>
    {
        let ids = await this.getMachineTemplateIds();

        return mapInParallel(ids, async (id) => this.getMachineTemplate(id));
    }

    async getMachineTemplate(id: string): Promise<Models.DigineousMachineLibrary>
    {
        let res = await this.api.dataConnections.receiveRaw(endpoint__MACHINE_TEMPLATE_GET, id);
        return res ? Models.DigineousMachineLibrary.newInstance(res) : null;
    }

    async setMachineTemplate(id: string,
                             library: Models.DigineousMachineLibrary): Promise<Models.DigineousMachineLibrary>
    {
        let res = await this.api.dataConnections.receiveRaw(endpoint__MACHINE_TEMPLATE_SET, id, library);
        return res ? Models.DigineousMachineLibrary.newInstance(res) : null;
    }

    async deleteMachineTemplate(id: string): Promise<void>
    {
        await this.api.dataConnections.receiveRaw(endpoint__MACHINE_TEMPLATE_DELETE, id);
    }

    //--//

    async getMachineIds(): Promise<string[]>
    {
        let res = await this.api.dataConnections.receiveRaw(endpoint__MACHINE_LIST);
        return res ? <string[]>res : [];
    }

    async getMachines(): Promise<Map<string, Models.DigineousMachineConfig>>
    {
        let ids = await this.getMachineIds();

        let map = new Map<string, Models.DigineousMachineConfig>();

        await inParallel(ids, async (id) =>
        {
            let machine = await this.getMachine(id);
            map.set(id, machine);
        });

        return map;
    }

    async createMachine(req: Models.DigineousMachineConfig): Promise<Models.RecordIdentity>
    {
        let res = await this.api.dataConnections.receiveRaw(endpoint__MACHINE_CREATE, null, req);
        return res ? Models.RecordIdentity.newInstance(res) : null;
    }

    async getMachine(id: string): Promise<Models.DigineousMachineConfig>
    {
        let res = await this.api.dataConnections.receiveRaw(endpoint__MACHINE_GET, id);
        return res ? Models.DigineousMachineConfig.newInstance(res) : null;
    }

    async setMachine(id: string,
                     config: Models.DigineousMachineConfig): Promise<Models.DigineousMachineConfig>
    {
        let res = await this.api.dataConnections.receiveRaw(endpoint__MACHINE_SET, id, config);
        return res ? Models.DigineousMachineConfig.newInstance(res) : null;
    }

    async deleteMachine(id: string): Promise<void>
    {
        await this.api.dataConnections.receiveRaw(endpoint__MACHINE_DELETE, id);
    }

    //--//

    async getVibrationMonitorOptions(): Promise<ControlOption<number>[]>
    {
        let monitors = <Models.DigineousVibrationMonitorDetails[]>await this.api.dataConnections.receiveRaw(endpoint__VIBRATION_LIST);

        return SharedSvc.BaseService.mapOptions(monitors, (monitor) =>
        {
            let option   = new ControlOption<number>();
            option.id    = monitor.id;
            option.label = `${monitor.plantId}/${monitor.id}: ${monitor.deviceName || monitor.label}`;
            return option;
        });
    }

    async getActiveDevices(): Promise<{ deviceId: number, lastActivity: Date }[]>
    {
        let res: { deviceId: number, lastActivity: Date }[] = [];

        let map = <Lookup<any>>await this.api.dataConnections.receiveRaw(endpoint__DEVICE_ACTIVE);
        for (let key in map)
        {
            res.push({
                         deviceId    : +key,
                         lastActivity: new Date(map[key] * 1000)
                     });
        }

        res.sort((a,
                  b) => a.deviceId - b.deviceId);

        return res;
    }
}
