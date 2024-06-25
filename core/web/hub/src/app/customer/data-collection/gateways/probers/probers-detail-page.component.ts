import {Component, Injector, Type, ViewChild} from "@angular/core";

import {ProbersDetailPageDialogComponent} from "app/customer/data-collection/gateways/probers/probers-detail-page-dialog.component";

import {GatewayExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {GatewayProberOperationExtended} from "app/services/domain/gateway-prober-operations.service";
import * as Models from "app/services/proxy/model/models";
import {IpnDeviceDescriptor, ProberObjectCANbus, ProberObjectIpn} from "app/services/proxy/model/models";

import {convertLogFilters} from "app/shared/logging/application-log";
import {MACAddressDirective} from "framework/directives/mac-address.directive";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ApplicationLogFilter, IApplicationLogRange, IConsoleLogEntry, IConsoleLogProvider} from "framework/ui/consoles/console-log";
import {ConsoleLogComponent} from "framework/ui/consoles/console-log.component";
import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent, ImportHandler} from "framework/ui/dialogs/import-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {Future} from "framework/utils/concurrency";

@Component({
               selector   : "o3-gateway-probers-detail-page",
               templateUrl: "./probers-detail-page.component.html",
               styleUrls  : ["./probers-detail-page.component.scss"]
           })
export class ProbersDetailPageComponent extends SharedSvc.BaseComponentWithRouter implements IConsoleLogProvider
{
    gatewayId: string;
    gatewayExt: GatewayExtended;
    networkStatus: Models.ProberNetworkStatus;

    operationExt: GatewayProberOperationExtended;
    operationExecuting: boolean;

    nextOpCfg: ProberState;

    @ViewChild("log", {static: true}) log: ConsoleLogComponent;
    logLockScroll: boolean;
    private scrollToEnd: boolean;

    //--//

    discoveredDevices: ProberDeviceDetails[]  = [];
    discoveredObjects: ProberObjectDetails[]  = [];
    discoveredBDTEntries: ProbersBDTDetails[] = [];
    discoveredFDTEntries: ProberFDTDetails[]  = [];

    pendingDevicesSort: boolean = false;
    pendingObjectsSort: boolean = false;
    pendingBDTSort: boolean     = false;
    pendingFDTSort: boolean     = false;

    selectedObject: ProberObjectDetails;

    //--//

    sampledFrames: ProberFrameDetails[] = [];

    selectedFrame: ProberFrameDetails;

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        this.nextOpCfg  = new ProberState();
        this.gatewayExt = GatewayExtended.newInstance(this.app.domain.assets, Models.GatewayAsset.newInstance({}));
    }

    ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.log.bind(this);
    }

    protected async onNavigationComplete()
    {
        this.gatewayId = this.getPathParameter("id");

        this.loadData();
    }

    //--//

    async loadData()
    {
        if (this.gatewayId)
        {
            let gatewayExt = await this.app.domain.assets.getTypedExtendedById(GatewayExtended, this.gatewayId);
            if (!gatewayExt)
            {
                this.exit();
                return;
            }

            this.gatewayExt = gatewayExt;

            this.nextOpCfg.proberComponent = this;

            //--//

            this.removeAllDbSubscriptions();

            this.subscribeOneShot(gatewayExt,
                                  async (ext,
                                         action) =>
                                  {
                                      this.loadData();
                                  });

            this.refreshLog();

            this.networkStatus = await gatewayExt.checkNetworkStatus();
        }
    }

    public ngOnDestroy()
    {
        super.ngOnDestroy();

        this.closeOperation();
    }

    async exit()
    {
        this.app.ui.navigation.pop();
    }

    selectObject(obj: ProberObjectDetails)
    {
        if (this.selectedObject == obj)
        {
            this.selectedObject = null;
        }
        else
        {
            this.selectedObject = obj;
        }
    }

    selectFrame(obj: ProberFrameDetails)
    {
        if (this.selectedFrame == obj)
        {
            this.selectedFrame = null;
        }
        else
        {
            this.selectedFrame = obj;
        }
    }

    async closeOperation()
    {
        if (this.operationExt)
        {
            try
            {
                await this.operationExt.remove();
            }
            catch (e)
            {
            }

            this.operationExt = null;
        }
    }

    async newOperation()
    {
        if (await ProbersDetailPageDialogComponent.open(this, this.nextOpCfg))
        {
            this.nextOpCfg.previousOperation = this.nextOpCfg.operation;

            if (this.nextOpCfg.configuringDevice)
            {
                this.addDevice(this.nextOpCfg.newDevice, (details) =>
                {
                    details.manual = true;
                });
            }
            else if (this.nextOpCfg.configuringOperation)
            {
                await this.closeOperation();

                this.operationExt       = await this.gatewayExt.startOperation(this.nextOpCfg.operation);
                this.operationExecuting = true;

                this.scrollToEnd   = true;
                this.logLockScroll = true;

                this.log.log.reset();
                this.refreshLog();
            }
        }
    }

    private async refreshLog()
    {
        if (this.operationExt)
        {
            let activity = await this.operationExt.getCurrentActivity();
            if (!activity || activity.isDone())
            {
                if (this.operationExecuting)
                {
                    this.operationExecuting = false;

                    // Ensure we get the final results
                    this.operationExt = await this.operationExt.refresh();

                    let output = this.operationExt.model.outputDetails;

                    if (output instanceof Models.ProberOperationForBACnetToDiscoverBBMDsResults)
                    {
                        this.addDevices(output.discoveredDevices,
                                        (details,
                                         isNew) =>
                                        {
                                            details.manual = false;
                                            details.isBBMD = true;
                                        });
                    }

                    if (output instanceof Models.ProberOperationForBACnetToDiscoverDevicesResults)
                    {
                        this.addDevices(output.discoveredDevices,
                                        (details,
                                         isNew) =>
                                        {
                                            details.manual = false;
                                        });
                    }

                    if (output instanceof Models.ProberOperationForBACnetToDiscoverRoutersResults)
                    {
                        for (let router of (output.discoveredRouters || []))
                        {
                            this.addDevice(router.device,
                                           (details,
                                            isNew) =>
                                           {
                                               details.isRouter       = true;
                                               details.routedNetworks = router.networks;
                                           });
                        }
                    }

                    if (output instanceof Models.ProberOperationForBACnetToScanMstpTrunkForDevicesResults)
                    {
                        this.addDevices(output.discoveredDevices,
                                        (details,
                                         isNew) =>
                                        {
                                            if (isNew)
                                            {
                                                details.foundInMstpScan = true;
                                            }

                                            details.manual = false;
                                        });
                    }

                    if (output instanceof Models.ProberOperationForBACnetToAutoDiscoveryResults)
                    {
                        this.addDevices(output.discoveredDevices,
                                        (details,
                                         isNew) =>
                                        {
                                            details.manual = false;
                                        });
                    }

                    if (output instanceof Models.ProberOperationForBACnetToScanSubnetForDevicesResults)
                    {
                        this.addDevices(output.discoveredDevices,
                                        (details,
                                         isNew) =>
                                        {
                                            if (isNew)
                                            {
                                                details.foundInSubnetScan = true;
                                            }
                                            details.manual = false;
                                        });
                    }

                    if (output instanceof Models.ProberOperationForBACnetToReadDevicesResults)
                    {
                        for (let obj of (output.objects || []))
                        {
                            let device = this.addDevice(obj.device, null);

                            let props = obj.properties;
                            this.addObject(device, obj.objectId, props);

                            if (props && props.object_list)
                            {
                                for (let objId of props.object_list)
                                {
                                    this.addObject(device, objId);
                                }
                            }
                        }
                    }

                    if (output instanceof Models.ProberOperationForBACnetToReadBBMDsResults)
                    {
                        for (let bbmd of (output.bbmds || []))
                        {
                            for (let bdtEntry of (bbmd.bdt || []))
                            {
                                this.addBDT(bbmd.descriptor, bdtEntry);
                            }

                            for (let fdtEntry of (bbmd.fdt || []))
                            {
                                this.addFDT(bbmd.descriptor, fdtEntry);
                            }
                        }
                    }

                    if (output instanceof Models.ProberOperationForBACnetToReadObjectNamesResults)
                    {
                        for (let obj of (output.objects || []))
                        {
                            let device = this.addDevice(obj.device, null);

                            this.addObject(device, obj.objectId, obj.properties);
                        }
                    }

                    if (output instanceof Models.ProberOperationForBACnetToReadObjectsResults)
                    {
                        for (let obj of (output.objects || []))
                        {
                            let device = this.addDevice(obj.device, null);

                            this.addObject(device, obj.objectId, obj.properties);
                        }
                    }

                    //--//

                    if (output instanceof Models.ProberOperationForIpnToDecodedReadResults)
                    {
                        for (let frame of (output.frames || []))
                        {
                            let newFrame = new ProberFrameDetails();
                            newFrame.fromObjectIpn(frame);

                            this.sampledFrames.push(newFrame);
                        }

                        // Clone array to trigger Angular change detection
                        this.sampledFrames = [...this.sampledFrames];
                    }

                    if (output instanceof Models.ProberOperationForIpnToObdiiReadResults)
                    {
                        for (let frame of (output.frames || []))
                        {
                            let newFrame = new ProberFrameDetails();
                            newFrame.fromObjectIpn(frame);

                            this.sampledFrames.push(newFrame);
                        }

                        // Clone array to trigger Angular change detection
                        this.sampledFrames = [...this.sampledFrames];
                    }

                    //--//

                    if (output instanceof Models.ProberOperationForCANbusToRawReadResults)
                    {
                        for (let frame of (output.frames || []))
                        {
                            let newFrame = new ProberFrameDetails();
                            newFrame.fromObjectCANbus(frame);

                            this.sampledFrames.push(newFrame);
                        }

                        // Clone array to trigger Angular change detection
                        this.sampledFrames = [...this.sampledFrames];
                    }

                    if (output instanceof Models.ProberOperationForCANbusToDecodedReadResults)
                    {
                        for (let frame of (output.frames || []))
                        {
                            let newFrame = new ProberFrameDetails();
                            newFrame.fromObjectCANbus(frame);

                            this.sampledFrames.push(newFrame);
                        }

                        // Clone array to trigger Angular change detection
                        this.sampledFrames = [...this.sampledFrames];
                    }

                    //--//

                    this.flushSorting();
                }
            }

            this.log.refresh(this.scrollToEnd);

            if (this.operationExecuting)
            {
                this.subscribeOneShot(this.operationExt,
                                      async (ext,
                                             action) =>
                                      {
                                          this.operationExt = await this.operationExt.refresh();
                                          this.refreshLog();
                                      });
            }
        }
    }

    //--//

    exportState()
    {
        let exportState     = new ProberExport();
        exportState.version = ExportVersion;

        for (let device of this.discoveredDevices)
        {
            let exportDevice            = new ProberDeviceExport();
            exportDevice.descriptor     = device.descriptor;
            exportDevice.isBBMD         = device.isBBMD;
            exportDevice.isRouter       = device.isRouter;
            exportDevice.routedNetworks = device.routedNetworks;

            exportDevice.foundInMstpScan   = device.foundInMstpScan;
            exportDevice.foundInSubnetScan = device.foundInSubnetScan;

            exportDevice.manual = device.manual;

            for (let key in device.objects)
            {
                let obj                 = device.objects[key];
                let exportObject        = new ProberObjectExport();
                exportObject.objectId   = obj.objectId;
                exportObject.properties = obj.properties;

                exportDevice.objects[key] = exportObject;
            }

            exportState.devices.push(exportDevice);
        }

        for (let frame of this.sampledFrames)
        {
            let exportFrame        = new ProberFrameExport();
            exportFrame.timestamp  = frame.timestamp;
            exportFrame.id         = frame.id;
            exportFrame.properties = frame.properties;

            exportState.frames.push(exportFrame);
        }

        let timestamp = MomentHelper.fileNameFormat();
        DownloadDialogComponent.open(this, "Prober State Export", `ProberState__${timestamp}.json`, exportState);
    }

    async importState()
    {
        let importState = await ImportDialogComponent.open(this, "Prober State Import", new ProberImportHandler());
        if (importState)
        {
            this.discoveredDevices = [];
            this.discoveredObjects = [];
            this.sampledFrames     = [];

            for (let device of importState.devices || [])
            {
                let importDevice = new ProberDeviceDetails();
                Models.BaseAssetDescriptor.fixupPrototype(device.descriptor);
                importDevice.descriptor     = device.descriptor;
                importDevice.isBBMD         = device.isBBMD;
                importDevice.isRouter       = device.isRouter;
                importDevice.routedNetworks = device.routedNetworks;

                importDevice.foundInMstpScan   = device.foundInMstpScan;
                importDevice.foundInSubnetScan = device.foundInSubnetScan;

                importDevice.manual = device.manual;

                for (let key in device.objects)
                {
                    let obj                 = device.objects[key];
                    let importObject        = new ProberObjectDetails();
                    importObject.device     = importDevice;
                    importObject.objectId   = obj.objectId;
                    importObject.properties = obj.properties;

                    importDevice.objects[key] = importObject;

                    this.discoveredObjects.push(importObject);
                }

                this.discoveredDevices.push(importDevice);
            }

            for (let frame of importState.frames || [])
            {
                let importFrame        = new ProberFrameDetails();
                importFrame.timestamp  = frame.timestamp;
                importFrame.id         = frame.id;
                importFrame.properties = frame.properties;

                this.sampledFrames.push(importFrame);
            }

            this.pendingDevicesSort = true;
            this.pendingObjectsSort = true;
            this.flushSorting();
        }
    }

    //--//

    get networkInterfaceKeys(): string[]
    {
        return UtilsService.extractKeysFromMap(this.networkStatus ? this.networkStatus.networkInterfaces : null, true);
    }

    getNetworkInterfaceValue(key: string): string
    {
        return this.networkStatus.networkInterfaces[key];
    }

    //--//

    getLogCount(): number
    {
        return this.operationExt ? this.operationExt.model.lastOffset : 0;
    }

    async getLogPage(start: number,
                     end: number): Promise<IConsoleLogEntry[]>
    {
        let logEntries = [];

        let lines = await this.operationExt.getLog(start, end, null) || [];
        for (let line of lines)
        {
            logEntries.push(this.log.newLogEntry(line));
        }

        return logEntries;
    }

    async performFilter(filters: ApplicationLogFilter): Promise<IApplicationLogRange[]>
    {
        return await this.operationExt.filterLog(convertLogFilters(filters)) || [];
    }

    async prepareDownload()
    {
        if (this.operationExt)
        {
            DownloadDialogComponent.openWithGenerator(this, "Prober Log", DownloadDialogComponent.fileName("prober", ".txt"), new OperationLogDownloader(this.operationExt));
        }
    }

    //--//

    private flushSorting()
    {
        if (this.pendingDevicesSort)
        {
            // Clone array to trigger Angular change detection
            let discoveredDevices = [...this.discoveredDevices];

            discoveredDevices.sort((a,
                                    b) => ProberDeviceDetails.compareDescriptor(a.descriptor, b.descriptor));

            this.discoveredDevices  = discoveredDevices;
            this.pendingDevicesSort = false;
        }

        if (this.pendingObjectsSort)
        {
            // Clone array to trigger Angular change detection
            let discoveredObjects = [...this.discoveredObjects];

            discoveredObjects.sort((a,
                                    b) => ProberObjectDetails.compare(a, b));

            this.discoveredObjects  = discoveredObjects;
            this.pendingObjectsSort = false;
        }

        if (this.pendingBDTSort)
        {
            // Clone array to trigger Angular change detection
            let discoveredBDT = [...this.discoveredBDTEntries];

            discoveredBDT.sort((a,
                                b) => ProbersBDTDetails.compare(a, b));

            this.discoveredBDTEntries = discoveredBDT;
            this.pendingBDTSort       = false;
        }

        if (this.pendingFDTSort)
        {
            // Clone array to trigger Angular change detection
            let discoveredFDT = [...this.discoveredFDTEntries];

            discoveredFDT.sort((a,
                                b) => ProberFDTDetails.compare(a, b));

            this.discoveredFDTEntries = discoveredFDT;
            this.pendingFDTSort       = false;
        }

        this.detectChanges();
    }

    //--//

    private addDevices(lst: Models.BaseAssetDescriptor[],
                       callback: (details: ProberDeviceDetails,
                                  isNew: boolean) => void): ProberDeviceDetails[]
    {
        let res = [];

        if (lst)
        {
            for (let d of lst)
            {
                res.push(this.addDevice(d, callback));
            }
        }

        return res;
    }

    private addDevice(desc: Models.BaseAssetDescriptor,
                      callback: (details: ProberDeviceDetails,
                                 isNew: boolean) => void): ProberDeviceDetails
    {
        let newDevice        = new ProberDeviceDetails();
        newDevice.descriptor = desc;

        for (let oldDevice of this.discoveredDevices)
        {
            if (oldDevice.compare(newDevice) == 0)
            {
                if (callback)
                {
                    callback(oldDevice, false);
                }
                return oldDevice;
            }
        }

        if (callback)
        {
            callback(newDevice, true);
        }

        this.discoveredDevices  = [
            ...this.discoveredDevices,
            newDevice
        ];
        this.pendingDevicesSort = true;

        return newDevice;
    }

    //--//

    private addObject(device: ProberDeviceDetails,
                      objectId: string,
                      properties?: any)
    {
        let oldObject = device.objects[objectId];
        if (oldObject)
        {
            if (properties != undefined)
            {
                oldObject.properties = properties;
            }

            return oldObject;
        }

        let newObject        = new ProberObjectDetails();
        newObject.device     = device;
        newObject.objectId   = objectId;
        newObject.properties = properties;

        device.objects[objectId] = newObject;

        this.discoveredObjects.push(newObject);
        this.pendingObjectsSort = true;

        return newObject;
    }

    private addBDT(bbmd: Models.BACnetBBMD,
                   bdt: Models.ProberBroadcastDistributionTableEntry): ProbersBDTDetails
    {
        let newBDT        = new ProbersBDTDetails();
        newBDT.descriptor = bbmd;
        newBDT.entry      = bdt;

        for (let oldBDT of this.discoveredBDTEntries)
        {
            if (oldBDT.compare(newBDT) == 0)
            {
                return oldBDT;
            }
        }

        this.discoveredBDTEntries = [
            ...this.discoveredBDTEntries,
            newBDT
        ];

        this.pendingBDTSort = true;

        return newBDT;
    }

    private addFDT(bbmd: Models.BACnetBBMD,
                   bdt: Models.ProberForeignDeviceTableEntry): ProberFDTDetails
    {
        let newFDT        = new ProberFDTDetails();
        newFDT.descriptor = bbmd;
        newFDT.entry      = bdt;

        for (let oldFDT of this.discoveredFDTEntries)
        {
            if (oldFDT.compare(newFDT) == 0)
            {
                return oldFDT;
            }
        }

        this.discoveredFDTEntries = [
            ...this.discoveredFDTEntries,
            newFDT
        ];

        this.pendingFDTSort = true;

        return newFDT;
    }
}

export class ProberState
{
    public proberComponent: ProbersDetailPageComponent;

    public action: "operation" | "add-device";

    public protocolType: Type<Models.ProberOperation>;
    public operationType: Type<Models.ProberOperation>;

    public newDevice: Models.BACnetDeviceDescriptor;
    public operation: Models.ProberOperation;
    public previousOperation: Models.ProberOperation;

    public get configuringOperation()
    {
        return this.action == "operation";
    }

    public get configuringDevice()
    {
        return this.action == "add-device";
    }
}

// Bump version whenever we make a breaking change to the schema.
const ExportVersion = "v1";

export class ProberExport
{
    version: string;

    devices: ProberDeviceExport[] = [];

    frames: ProberFrameExport[] = [];
}

export class ProberDeviceExport
{
    descriptor: Models.BaseAssetDescriptor;

    isBBMD: boolean;
    isRouter: boolean;
    routedNetworks: number[];

    foundInMstpScan: boolean;
    foundInSubnetScan: boolean;

    manual: boolean;

    objects: Lookup<ProberObjectExport> = {};
}

export class ProberDeviceDetails
{
    descriptor: Models.BaseAssetDescriptor;

    isBBMD: boolean;
    isRouter: boolean;
    routedNetworks: number[];

    foundInMstpScan: boolean;
    foundInSubnetScan: boolean;

    manual: boolean;

    objects: Lookup<ProberObjectDetails> = {};

    //--//

    get textForTransport(): string
    {
        return ProberDeviceDetails.descriptorAsTransport(this.descriptor);
    }

    get textForIdentity(): string
    {
        if (this.descriptor instanceof Models.BACnetDeviceDescriptor)
        {
            if (this.descriptor.address)
            {
                return `${this.descriptor.address.networkNumber}/${this.descriptor.address.instanceNumber}`;
            }
        }

        if (this.descriptor instanceof Models.IpnDeviceDescriptor)
        {
            return this.descriptor.name;
        }

        return "";
    }

    get textForMAC(): string
    {
        let res = [];

        if (this.descriptor instanceof Models.BACnetDeviceDescriptor)
        {
            if (this.descriptor.bacnetAddress)
            {
                return `${this.descriptor.bacnetAddress.network_number}/${this.descriptor.bacnetAddress.mac_address}`;
            }
        }

        return "";
    }

    get annotations(): string
    {
        let res = [];

        if (this.isRouter)
        {
            res.push(`Router for networks [${this.routedNetworks.join(",")}]`);
        }

        if (this.isBBMD)
        {
            res.push("BBMD");
        }

        if (this.foundInMstpScan)
        {
            res.push("MS/TP Scan");
        }

        if (this.foundInSubnetScan)
        {
            res.push("Subnet Scan");
        }

        return res.join(" - ");
    }

    compare(other: ProberDeviceDetails)
    {
        return ProberDeviceDetails.compareDescriptor(this.descriptor, other.descriptor);
    }

    static compareDescriptor(a: Models.BaseAssetDescriptor,
                             b: Models.BaseAssetDescriptor): number
    {
        let diff: number;

        if (a instanceof Models.BACnetDeviceDescriptor)
        {
            if (b instanceof Models.BACnetDeviceDescriptor)
            {
                if ((diff = ProberDeviceDetails.compareTransport(a.transport, b.transport)) != 0) return diff;
                if ((diff = ProberDeviceDetails.compareId(a.address, b.address)) != 0) return diff;
                if ((diff = ProberDeviceDetails.compareMAC(a.bacnetAddress, b.bacnetAddress)) != 0) return diff;
            }
            else if (b == null)
            {
                return -1;
            }
        }
        else
        {
            if (b != null)
            {
                return 1;
            }
        }

        return 0;
    }

    static compareTransport(a: Models.TransportAddress,
                            b: Models.TransportAddress): number
    {
        let diff: number;

        if (a instanceof Models.UdpTransportAddress)
        {
            if (b instanceof Models.UdpTransportAddress)
            {
                if ((diff = ProberDeviceDetails.compareHost(a.host, b.host)) != 0) return diff;
                if ((diff = a.port - b.port) != 0) return diff;
            }
            else if (b instanceof Models.EthernetTransportAddress)
            {
                return -1;
            }
            else if (b == null)
            {
                return -1;
            }
        }
        else if (a instanceof Models.EthernetTransportAddress)
        {
            if (b instanceof Models.EthernetTransportAddress)
            {
                if ((diff = a.d1 - b.d1) != 0) return diff;
                if ((diff = a.d2 - b.d2) != 0) return diff;
                if ((diff = a.d3 - b.d3) != 0) return diff;
                if ((diff = a.d4 - b.d4) != 0) return diff;
                if ((diff = a.d5 - b.d5) != 0) return diff;
                if ((diff = a.d6 - b.d6) != 0) return diff;
            }
            else if (b == null)
            {
                return -1;
            }
        }
        else if (a == null)
        {
            if (b != null)
            {
                return 1;
            }
        }

        return 0;
    }

    static compareHost(a: string,
                       b: string): number
    {
        let regExp = /^(\d+)\.(\d+)\.(\d+)\.(\d+)$/;

        let aReg = regExp.exec(a);
        let bReg = regExp.exec(b);

        if (aReg && bReg)
        {
            for (let pos = 1; pos < 5; pos++)
            {
                let aNum = +aReg[pos];
                let bNum = +bReg[pos];

                let diff = aNum - bNum;
                if (diff != 0) return diff;
            }

            return 0;
        }

        return UtilsService.compareStrings(a, b, true);
    }

    static compareId(a: Models.BACnetDeviceAddress,
                     b: Models.BACnetDeviceAddress): number
    {
        let diff: number;

        if (a != null)
        {
            if (b != null)
            {
                if ((diff = a.networkNumber - b.networkNumber) != 0) return diff;
                if ((diff = a.instanceNumber - b.instanceNumber) != 0) return diff;
            }
            else
            {
                return -1;
            }
        }
        else
        {
            if (b != null)
            {
                return 1;
            }
        }

        return 0;
    }

    static compareMAC(a: Models.BACnetAddress,
                      b: Models.BACnetAddress): number
    {
        let diff: number;

        if (a != null)
        {
            if (b != null)
            {
                if ((diff = a.network_number - b.network_number) != 0) return diff;

                let a1 = a.mac_address || "";
                let b1 = b.mac_address || "";

                if ((diff = UtilsService.compareStrings(a1, b1, true)) != 0) return diff;
            }
            else
            {
                return -1;
            }
        }
        else
        {
            if (b != null)
            {
                return 1;
            }
        }

        return 0;
    }

    public static descriptorAsTransport(descriptor: Models.BaseAssetDescriptor): string
    {
        if (descriptor instanceof Models.BACnetDeviceDescriptor)
        {
            if (descriptor.transport instanceof Models.UdpTransportAddress)
            {
                return `${descriptor.transport.host}:${descriptor.transport.port}`;
            }
            else if (descriptor.transport instanceof Models.EthernetTransportAddress)
            {
                let t = descriptor.transport;
                return MACAddressDirective.toMACString(t.d1, t.d2, t.d3, t.d4, t.d5, t.d6);
            }
        }

        return "";
    }
}

export class ProberObjectExport
{
    objectId: string;

    properties: any;
}

export class ProberObjectDetails
{
    device: ProberDeviceDetails;

    objectId: string;

    properties: any;

    private expandedProperties: Lookup<boolean> = {};

    private get propertiesNotNull(): any
    {
        return this.properties || {};
    }

    //--//

    compare(other: ProberObjectDetails)
    {
        return ProberObjectDetails.compare(this, other);
    }

    public static compare(a: ProberObjectDetails,
                          b: ProberObjectDetails)
    {
        let diff = ProberDeviceDetails.compareDescriptor(a.device.descriptor, b.device.descriptor);
        if (diff != null)
        {
            diff = UtilsService.compareStrings(a.objectId, b.objectId, true);
        }

        return diff;
    }

    //--//

    get propertyKeys(): string[]
    {
        return UtilsService.extractKeysFromMap(this.propertiesNotNull, true, "__type", "__type_bacnet", "__type_ipn");
    }

    getPropertyValue(key: string): string
    {
        let value = this.propertiesNotNull[key];
        switch (typeof value)
        {
            case "boolean":
            case "number":
            case "string":
                return <string>value;
        }

        return JSON.stringify(value, null, "  ");
    }

    getPropertyClass(key: string): string
    {
        return this.expandedProperties[key] ? "propertyValue-wide" : "propertyValue";
    }

    toggleProperty(key: string)
    {
        this.expandedProperties[key] = !this.expandedProperties[key];
    }
}

export class ProbersBDTDetails
{
    descriptor: Models.BACnetBBMD;

    entry: Models.ProberBroadcastDistributionTableEntry;

    get bbmdTransport(): string
    {
        return `${this.descriptor.networkAddress}:${this.descriptor.networkPort}`;
    }

    compare(other: ProbersBDTDetails): number
    {
        return ProbersBDTDetails.compare(this, other);
    }

    public static compare(a: ProbersBDTDetails,
                          b: ProbersBDTDetails): number
    {
        let diff = UtilsService.compareStrings(a.bbmdTransport, b.bbmdTransport, true);
        if (diff == 0)
        {
            diff = UtilsService.compareStrings(a.entry.address, b.entry.address, true);
            if (diff == 0)
            {
                diff = UtilsService.compareStrings(a.entry.mask, b.entry.mask, true);
            }
        }

        return diff;
    }
}

export class ProberFDTDetails
{
    descriptor: Models.BACnetBBMD;

    entry: Models.ProberForeignDeviceTableEntry;

    get bbmdTransport(): string
    {
        return `${this.descriptor.networkAddress}:${this.descriptor.networkPort}`;
    }

    compare(other: ProberFDTDetails): number
    {
        return ProberFDTDetails.compare(this, other);
    }

    public static compare(a: ProberFDTDetails,
                          b: ProberFDTDetails): number
    {
        let diff = UtilsService.compareStrings(a.bbmdTransport, b.bbmdTransport, true);
        if (diff == 0)
        {
            diff = UtilsService.compareStrings(a.entry.address, b.entry.address, true);
        }

        return diff;
    }
}

export class ProberFrameExport
{
    timestamp: Date;

    id: string;

    properties: any;
}

export class ProberFrameDetails
{
    timestamp: Date;

    id: string;

    properties: any;

    private expandedProperties: Lookup<boolean> = {};

    private get propertiesNotNull(): any
    {
        return this.properties || {};
    }

    //--//

    get propertyKeys(): string[]
    {
        return UtilsService.extractKeysFromMap(this.propertiesNotNull, true);
    }

    getPropertyValue(key: string): string
    {
        let value = this.propertiesNotNull[key];
        switch (typeof value)
        {
            case "boolean":
            case "number":
            case "string":
                return <string>value;
        }

        return JSON.stringify(value, null, "  ");
    }

    getPropertyClass(key: string): string
    {
        return this.expandedProperties[key] ? "propertyValue-wide" : "propertyValue";
    }

    toggleProperty(key: string)
    {
        this.expandedProperties[key] = !this.expandedProperties[key];
    }

    fromObjectIpn(obj: ProberObjectIpn)
    {
        this.timestamp  = obj.timestamp;
        this.id         = (obj.device as IpnDeviceDescriptor).name;
        this.properties = obj.properties;
    }

    fromObjectCANbus(obj: ProberObjectCANbus)
    {
        this.timestamp  = obj.timestamp;
        this.id         = (obj.device as IpnDeviceDescriptor).name;
        this.properties = obj.properties;
    }
}

class OperationLogDownloader implements DownloadGenerator
{
    logEntries: string[] = [];
    lastOffset: number   = 0;

    constructor(private extended: GatewayProberOperationExtended)
    {
    }

    public getProgressPercent()
    {
        return NaN;
    }

    public getProgressMessage()
    {
        return "Lines in log: " + this.logEntries.length;
    }

    public async makeProgress(dialog: DownloadDialogComponent): Promise<boolean>
    {
        let lines = await this.extended.getLog(this.lastOffset, null, 4357); // Random-looking number, better for the UI, less flickering
        if (!lines || lines.length == 0)
        {
            return true;
        }

        for (let line of lines)
        {
            let text      = line.line.replace("\n", "");
            let timestamp = MomentHelper.parse(line.timestamp);
            let log       = `${timestamp.format("YYYY-MM-DD HH:mm:ss.SSS")}: ${text}\n`;

            this.lastOffset = line.lineNumber + 1;

            this.logEntries.push(log);
        }

        return false;
    }

    public async sleepForProgress(): Promise<void>
    {
        // We don't need to sleep.
    }

    public isDeterminate()
    {
        return false;
    }

    public async getResults(): Promise<DownloadResults>
    {
        return {lines: this.logEntries};
    }
}

class ProberImportHandler implements ImportHandler<ProberExport>
{
    public returnRawBlobs(): boolean
    {
        return false;
    }

    public async parseFile(contents: string): Promise<ProberExport>
    {
        let future = new Future<ProberExport>();

        let json = JSON.parse(contents) as ProberExport;
        if (json.version != ExportVersion)
        {
            json = null;
        }

        return json;
    }

}
