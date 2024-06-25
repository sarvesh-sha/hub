import {Component, Inject, Injector} from "@angular/core";

import {AppContext} from "app/app.service";
import {I2CSensorMCP3428Extended, I2CSensorSHT3xExtended} from "app/customer/data-collection/networks/network-detail-editor.component";

import {LocationExtended, NetworkExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {WizardDialogComponent, WizardDialogState} from "app/shared/overlays/wizard-dialog.component";

import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               templateUrl: "./networks-wizard.component.html"
           })
export class NetworksWizardDialogComponent extends WizardDialogComponent<NetworkWizardState>
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: NetworkWizardState)
    {
        super(dialogRef, inj, data);
    }

    public static async open(cfg: WizardDialogState,
                             base: BaseApplicationComponent): Promise<boolean>
    {
        return await super.open(cfg, base, NetworksWizardDialogComponent);
    }
}

export class NetworkWizardState extends WizardDialogState
{
    network: NetworkExtended;
    networkModel: Models.NetworkAsset;

    locationId: string;
    location: LocationExtended;

    bacnetEnabled: boolean;
    bacnet: Models.ProtocolConfigForBACnet;
    bacnetLimitScanLow: number;
    bacnetLimitScanHigh: number;

    ipnEnabled: boolean;
    ipn: Models.ProtocolConfigForIpn;
    ipn_i2c_SHT3x: I2CSensorSHT3xExtended[]     = [];
    ipn_i2c_MCP3428: I2CSensorMCP3428Extended[] = [];


    constructor(public app: AppContext,
                public networkId: string)
    {
        super(!networkId);
    }

    public async create(comp: BaseApplicationComponent,
                        goto: boolean): Promise<boolean>
    {
        // Save the model and record the result
        let result = await this.save(comp);

        // If save successful and goto set, navigate to record
        if (result && goto)
        {
            comp.app.ui.navigation.go("/networks/network", [this.networkId]);
        }

        // Return save result
        return result;
    }

    public async save(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            let cfg: Models.ProtocolConfig[] = [];

            if (this.bacnetEnabled)
            {
                if (this.bacnetLimitScanLow || this.bacnetLimitScanHigh)
                {
                    this.bacnet.limitScan = Models.WhoIsRange.newInstance({
                                                                              low : this.bacnetLimitScanLow || 0,
                                                                              high: this.bacnetLimitScanHigh || ((1 << 22) - 1)
                                                                          });
                }
                else
                {
                    this.bacnet.limitScan = null;
                }

                cfg.push(this.bacnet);
            }

            if (this.ipnEnabled)
            {
                let sensors = [
                    ...this.ipn_i2c_SHT3x.map(o => o.model),
                    ...this.ipn_i2c_MCP3428.map(o => o.model)
                ];

                if (sensors.length > 0)
                {
                    this.ipn.i2cSensors = sensors;
                }
                else
                {
                    this.ipn.i2cSensors = null;
                }

                cfg.push(this.ipn);
            }

            this.network.typedModel.protocolsConfiguration = cfg;

            let isNew = !this.network.model.sysId;

            this.network   = await this.network.save();
            this.networkId = this.network.model.sysId;

            this.app.framework.errors.success(isNew ? "Network added" : "Network updated", -1);
            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    public async load(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            await this.initNetwork();
            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    async initNetwork()
    {
        if (this.networkId)
        {
            this.network      = await this.app.domain.assets.getTypedExtendedById(NetworkExtended, this.networkId);
            this.networkModel = this.network.typedModel;
        }

        if (!this.network)
        {
            this.network                     = this.app.domain.assets.wrapTypedModel(NetworkExtended, new Models.NetworkAsset());
            this.networkModel                = this.network.typedModel;
            this.networkModel.state          = Models.AssetState.operational;
            this.networkModel.cidr           = "0.0.0.0/0";
            this.networkModel.samplingPeriod = 900;
        }

        this.bacnet              = this.network.getProtocolConfig(Models.ProtocolConfigForBACnet);
        this.bacnetEnabled       = this.bacnet !== null;
        this.bacnetLimitScanLow  = this.bacnet?.limitScan?.low;
        this.bacnetLimitScanHigh = this.bacnet?.limitScan?.high;

        this.ipn             = this.network.getProtocolConfig(Models.ProtocolConfigForIpn);
        this.ipnEnabled      = this.ipn !== null;
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

        this.location = await this.network.getLocation();
        if (this.location)
        {
            this.locationId = this.location.model.sysId;
        }
    }

    ensureProtocolConfig()
    {
        if (this.bacnetEnabled && !this.bacnet)
        {
            this.bacnet = Models.ProtocolConfigForBACnet.newInstance({
                                                                         bbmds                    : [],
                                                                         scanSubnets              : [],
                                                                         nonDiscoverableDevices   : [],
                                                                         nonDiscoverableMstpTrunks: [],
                                                                         skippedDevices           : [],
                                                                         filterSubnets            : []
                                                                     });
        }

        if (this.ipnEnabled && !this.ipn)
        {
            this.ipn = Models.ProtocolConfigForIpn.newInstance({
                                                                   ipnPort    : "/optio3-dev/optio3_RS485",
                                                                   ipnBaudrate: 33400,
                                                                   gpsPort    : "/optio3-dev/optio3_gps",
                                                                   canPort    : "can0"
                                                               });
        }
    }

    async initLocation()
    {
        if (this.locationId)
        {
            this.network.setLocation(this.locationId);

            this.location = await this.network.getLocation();
        }
    }
}
