import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ReportError} from "app/app.service";

import {GatewaySelectionDialogComponent} from "app/customer/data-collection/gateways/gateway-selection-dialog.component";
import {I2CSensorMCP3428Extended, I2CSensorSHT3xExtended} from "app/customer/data-collection/networks/network-detail-editor.component";
import {NetworksWizardDialogComponent, NetworkWizardState} from "app/customer/data-collection/networks/networks-wizard/networks-wizard.component";
import {DevicesListComponent} from "app/customer/devices/devices-list.component";
import {TimeSeriesChartConfigurationExtended, TimeSeriesSourceConfigurationExtended, TimeSeriesSourceHost} from "app/customer/visualization/time-series-utils";

import {DeviceElementExtended, GatewayExtended, LocationExtended, NetworkExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent, ImportHandler} from "framework/ui/dialogs/import-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

@Component({
               selector   : "o3-networks-detail-page",
               templateUrl: "./networks-detail-page.component.html",
               styleUrls  : ["./networks-detail-page.component.scss"]
           })
export class NetworksDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    networkID: string;
    networkLocationID: string;
    networkLocationReady = false;

    networkExtended: NetworkExtended;
    networkRemoveChecks: Models.ValidationResult[];
    networkNoDeleteReason: string;

    gateway: GatewayExtended;
    location: LocationExtended;

    bacnet: Models.ProtocolConfigForBACnet;
    ipn: Models.ProtocolConfigForIpn;
    ipn_i2c_SHT3x: I2CSensorSHT3xExtended[]     = [];
    ipn_i2c_MCP3428: I2CSensorMCP3428Extended[] = [];

    operationalStates: ControlOption<Models.AssetState>[];

    //--//

    range: Models.RangeSelection                 = new Models.RangeSelection();
    config: TimeSeriesChartConfigurationExtended = TimeSeriesChartConfigurationExtended.emptyInstance(this.app);

    elements: ControlOption<string>[] = [];
    deviceElementId: string           = "";
    deviceElement: DeviceElementExtended;

    sources: ControlOption<string>[] = [];
    selectedSource: string           = "";
    secondarySource: string          = "";

    //--//


    @ViewChild("networkForm", {static: true}) networkForm: NgForm;

    @ViewChild("devicesList") devicesList: DevicesListComponent;

    constructor(inj: Injector)
    {
        super(inj);

        // Select default time range
        this.range.range = Models.TimeRangeId.Last60Minutes;

        this.networkExtended = this.app.domain.assets.wrapTypedModel(NetworkExtended, new Models.NetworkAsset());
    }

    protected async onNavigationComplete()
    {
        this.networkID             = this.getPathParameter("id");
        this.networkNoDeleteReason = "<checking...>";

        if (this.networkID)
        {
            await this.loadNetwork();
        }
    }

    protected shouldDelayNotifications(): boolean
    {
        return !this.networkForm.pristine;
    }

    async loadNetwork()
    {
        // load network info
        let network = await this.app.domain.assets.getTypedExtendedById(NetworkExtended, this.networkID);
        if (!network)
        {
            this.exit();
            return;
        }

        this.networkExtended = network;

        this.operationalStates = await this.app.domain.assets.getOperationalStates();

        this.gateway  = await network.getBoundGateway();
        this.location = await network.getLocation();
        if (this.location)
        {
            this.networkLocationID = this.location.model.sysId;
        }
        this.networkLocationReady = true;

        this.networkRemoveChecks   = await network.checkRemove();
        this.networkNoDeleteReason = this.fromValidationToReason("Remove is disabled because:", this.networkRemoveChecks);

        this.bacnet = network.getProtocolConfig(Models.ProtocolConfigForBACnet);
        this.ipn    = network.getProtocolConfig(Models.ProtocolConfigForIpn);
        this.ipn_i2c_SHT3x   = [];
        this.ipn_i2c_MCP3428 = [];

        let rules = await this.app.bindings.getActiveNormalizationRules();

        //--//

        for (let sensor of this.ipn?.i2cSensors || [])
        {
            if (sensor instanceof Models.I2CSensorSHT3x)
            {
                this.ipn_i2c_SHT3x.push(new I2CSensorSHT3xExtended(rules, sensor));
            }
            if (sensor instanceof Models.I2CSensorMCP3428)
            {
                this.ipn_i2c_MCP3428.push(new I2CSensorMCP3428Extended(rules, sensor));
            }
        }

        // set breadcrumbs
        let model                                     = network.typedModel;
        this.app.ui.navigation.breadcrumbCurrentLabel = model.cidr || "New";

        //--//


        let filters = Models.DeviceElementFilterRequest.newInstance({parentIDs: [this.networkID]});

        let response = await this.app.domain.assets.getList(filters);
        let ids      = response.results;
        let points   = await this.app.domain.assets.getTypedExtendedBatch(DeviceElementExtended, ids);

        this.elements      = [];
        this.deviceElement = null;

        for (let point of points)
        {
            this.elements.push(new ControlOption<string>(point.model.sysId, point.model.name || ""));
        }

        this.elements.sort((a,
                            b) =>
                           {
                               let aWeight = this.getWeight(a);
                               let bWeight = this.getWeight(b);

                               if (aWeight != bWeight)
                               {
                                   return aWeight - bWeight;
                               }

                               return UtilsService.compareStrings(a.label, b.label, true);
                           });

        this.networkForm.form.markAsPristine();
        this.detectChanges();
    }

    async refreshDeviceElement()
    {
        this.deviceElement = await this.app.domain.assets.getTypedExtendedById(DeviceElementExtended, this.deviceElementId);

        this.refreshSources();
        this.updateConfig();
    }

    async locationChanged(selectedLocationID: string)
    {
        this.networkExtended.setLocation(selectedLocationID);

        this.location = await this.networkExtended.getLocation();
        this.networkForm.form.markAsDirty();
    }

    async updateConfig()
    {
        if (this.deviceElement)
        {
            let host         = new TimeSeriesSourceHost(this);
            let mainExt      = await TimeSeriesSourceConfigurationExtended.resolveFromIdAndDimension(host, this.deviceElement.typedModel.sysId, this.selectedSource);
            let secondaryExt = await TimeSeriesSourceConfigurationExtended.resolveFromIdAndDimension(host, this.deviceElement.typedModel.sysId, this.secondarySource);

            this.config                                    = await TimeSeriesChartConfigurationExtended.generateNewInstanceFromSources(this.app, mainExt, secondaryExt);
            this.config.model.display.automaticAggregation = true;
        }
        else
        {
            this.config = TimeSeriesChartConfigurationExtended.emptyInstance(this.app);
        }
    }

    exportNetwork()
    {
        let timestamp = MomentHelper.fileNameFormat();

        DownloadDialogComponent.open<Models.NetworkAsset>(this, "Download Network", `network__${this.networkExtended.model.name}_${timestamp}.json`, this.networkExtended.typedModel);
    }

    @ReportError
    async save()
    {
        this.networkExtended = await this.networkExtended.save();

        await this.cancel();
    }

    async cancel()
    {
        await this.networkExtended.refresh();

        await this.loadNetwork();
    }

    @ReportError
    async remove()
    {
        if (this.networkExtended)
        {
            if (await this.confirmOperation("Click Yes to confirm deletion of this Network."))
            {
                let name = this.networkExtended.model.name || this.networkExtended.model.sysId;
                let msg  = this.app.framework.errors.success(`Deleting Network '${name}'...`, -1);

                let promise = this.networkExtended.remove();

                // Navigate away without waiting for deletion, since it can take a long time.
                this.exit();

                if (await promise)
                {
                    this.app.framework.errors.dismiss(msg);
                    this.app.framework.errors.success(`Network '${name}' deleted`, -1);
                }
            }
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    async edit()
    {
        await NetworksWizardDialogComponent.open(new NetworkWizardState(this.app, this.networkID), this);
        this.loadNetwork();
    }

    viewLocation(): void
    {
        if (this.location)
        {
            this.app.ui.navigation.go("/configuration/locations/location", [this.location.model.sysId]);
        }
    }

    viewGateway(): void
    {
        this.app.ui.navigation.go("/gateways/gateway", [
            this.gateway.model.sysId
        ]);
    }

    @ReportError
    async autoConfigure()
    {
        await this.networkExtended.autoConfig();
    }

    async bindGateway()
    {
        let dialogService = await GatewaySelectionDialogComponent.open(this, "select", "Select");
        if (dialogService != null)
        {
            await dialogService.gateway.bindNetwork(this.networkExtended, false, false, false);
            await this.loadNetwork();
        }
    }

    async unbindGateway()
    {
        if (await this.confirmOperation("Click Yes to confirm unbinding of network from gateway."))
        {
            await this.gateway.unbindNetwork(this.networkExtended);
            await this.loadNetwork();
        }
    }

    async enableSamplingWithClassId()
    {
        await this.networkExtended.updateSampling(true, false, false);
    }

    async disableSamplingWithoutClassId()
    {
        await this.networkExtended.updateSampling(false, true, false);
    }

    async refreshSampling()
    {
        await this.networkExtended.updateSampling(false, false, true);
    }

    async rediscover()
    {
        if (await this.confirmDiscoveryOperation())
        {
            await this.gateway.bindNetwork(this.networkExtended, true, false, false);
        }
    }

    async relistObjects()
    {
        if (await this.confirmDiscoveryOperation())
        {
            await this.gateway.bindNetwork(this.networkExtended, false, true, false);
        }
    }

    async rereadObjects()
    {
        if (await this.confirmDiscoveryOperation())
        {
            await this.gateway.bindNetwork(this.networkExtended, false, false, true);
        }
    }

    private async confirmDiscoveryOperation(): Promise<boolean>
    {
        let boundNetworks = await this.gateway.getBoundNetworks();
        if (boundNetworks.length > 1)
        {
            return this.confirmOperation("Starting discovery with other networks bound to the gateway could prevent data collection.<br>Do you wish to continue?");
        }

        return true;
    }

    //--//

    private async refreshSources()
    {
        let sources = [];

        if (this.deviceElement)
        {
            let pointSchema = await this.deviceElement.fetchSchema();
            let keys        = UtilsService.extractKeysFromMap(pointSchema);

            keys.sort((a,
                       b) => UtilsService.compareStrings(a, b, true));

            let oldSource1 = this.selectedSource;
            let oldSource2 = this.secondarySource;

            this.selectedSource  = null;
            this.secondarySource = null;

            for (let prop of keys)
            {
                let schema = pointSchema[prop];

                sources.push(new ControlOption<string>(schema.name, schema.name));

                if (schema.name == oldSource1) this.selectedSource = oldSource1;
                if (schema.name == oldSource2) this.secondarySource = oldSource2;
            }
        }
        else
        {
            this.selectedSource  = null;
            this.secondarySource = null;
        }

        this.sources = sources;
    }

    private getWeight(opt: ControlOption<string>)
    {
        if (opt.label.startsWith("Total")) return 0;
        if (opt.label.startsWith("Transport")) return 1;
        if (opt.label.startsWith("Network")) return 2;
        if (opt.label.startsWith("Device")) return 3;
        return 3;
    }
}

export class OfflineDiscoveryHandler implements ImportHandler<File>
{
    constructor(private domain: AppDomainContext)
    {
    }

    returnRawBlobs(): boolean
    {
        return true;
    }

    async parseFile(result: string): Promise<File>
    {
        return null;
    }
}
