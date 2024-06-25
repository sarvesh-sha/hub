import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {Future} from "framework/utils/concurrency";

@Component({
               selector   : "o3-sensors-page",
               templateUrl: "./sensors-page.component.html",
               styleUrls  : ["./sensors-page.component.scss"]
           })
export class SensorsPageComponent extends SharedSvc.BaseApplicationComponent
{
    checkedPorts: SensorDetails[] = [];
    doneChecking: boolean;

    constructor(inj: Injector)
    {
        super(inj);
    }

    ngAfterViewInit()
    {
        this.checkPorts();
    }

    async checkPorts()
    {
        this.checkedPorts = [];
        this.doneChecking = false;

        let promiseCAN0 = this.checkPort("CAN0", (details) => this.checkCAN("can0", details));
        let promiseCAN1 = this.checkPort("CAN1", (details) => this.checkCAN("can1", details));

        let promiseGPS = this.checkPort("GPS", async (details) => this.checkGPS("/optio3-dev/optio3_gps", details));

        let promiseObdiiExt = this.checkPort("OBD-II (External)", async (details) => this.checkOBDII("/optio3-dev/optio3_obdii", details));

        let promiseRS232    = this.checkPort("RS-232", async (details) => this.checkRS232("/optio3-dev/optio3_RS232", details));
        let promiseRS232b   = this.checkPort("RS-232b", async (details) => this.checkRS232("/optio3-dev/optio3_RS232b", details));
        let promiseRS232ext = this.checkPort("RS-232ext", async (details) => this.checkRS232("/optio3-dev/optio3_RS232ext", details));

        let promiseRS485  = this.checkPort("RS-485", async (details) => this.checkRS485("/optio3-dev/optio3_RS485", true, details));
        let promiseRS485b = this.checkPort("RS-485b", async (details) => this.checkRS485("/optio3-dev/optio3_RS485b", false, details));

        let promiseI2CHub = this.checkPort("I2C Hat", async (details) => this.checkI2CHub(details));

        await Promise.all([
                              promiseCAN0,
                              promiseCAN1,
                              promiseGPS,
                              promiseObdiiExt,
                              promiseRS232,
                              promiseRS232b,
                              promiseRS232ext,
                              promiseRS485,
                              promiseRS485b,
                              promiseI2CHub
                          ]);

        this.doneChecking = true;
    }

    private async checkCAN(port: string,
                           details: SensorDetails)
    {
        details.info("Looking for Palfinger liftgate...");

        let cfgPalfinger = Models.SensorConfigForPalfinger.newInstance({
                                                                           seconds     : 5,
                                                                           canPort     : port,
                                                                           canFrequency: 250000
                                                                       });

        let resultPalfinger = await this.checkSensor(details, cfgPalfinger);
        if (resultPalfinger?.success)
        {
            details.detected("Detected Palfinger liftgate!");
            return;
        }

        cfgPalfinger.canInvert = true;
        resultPalfinger        = await this.checkSensor(details, cfgPalfinger);
        if (resultPalfinger?.success)
        {
            details.detected("Detected Palfinger liftgate! (inverted polarity)");
            return;
        }

        //--//

        details.info("Looking for Hendrickson Watchman...");

        let cfgHendricksonWatchman = Models.SensorConfigForHendricksonWatchman.newInstance({
                                                                                               seconds         : 12,
                                                                                               canPort         : port,
                                                                                               canFrequency    : 500000,
                                                                                               canNoTermination: true
                                                                                           });

        let resultHendricksonWatchman = await this.checkSensor(details, cfgHendricksonWatchman);
        if (resultHendricksonWatchman?.success)
        {
            details.detected("Detected Hendrickson Watchman!");
            return;
        }

        cfgHendricksonWatchman.canInvert = true;
        resultHendricksonWatchman        = await this.checkSensor(details, cfgHendricksonWatchman);
        if (resultHendricksonWatchman?.success)
        {
            details.detected("Detected Hendrickson Watchman! (inverted polarity)");
            return;
        }

        //--//

        if (false) // Disabled for now, no customer demand.
        {
            details.info("Looking for Bergstrom HVAC...");

            let cfgBergstrom = Models.SensorConfigForBergstrom.newInstance({
                                                                               seconds     : 5,
                                                                               canPort     : port,
                                                                               canFrequency: 250000
                                                                           });

            let resultBergstrom = await this.checkSensor(details, cfgBergstrom);
            if (resultBergstrom?.success)
            {
                details.detected("Detected Bergstrom HVAC! (bitrate: 250000)");
                return;
            }

            cfgBergstrom.canInvert = true;

            resultBergstrom = await this.checkSensor(details, cfgBergstrom);
            if (resultBergstrom?.success)
            {
                details.detected("Detected Bergstrom HVAC! (bitrate: 250000, inverted polarity)");
                return;
            }

            cfgBergstrom.canFrequency = 500000;
            cfgBergstrom.canInvert    = false;

            resultBergstrom = await this.checkSensor(details, cfgBergstrom);
            if (resultBergstrom?.success)
            {
                details.detected("Detected Bergstrom HVAC! (bitrate: 500000)");
                return;
            }

            cfgBergstrom.canInvert = true;
            resultBergstrom        = await this.checkSensor(details, cfgBergstrom);
            if (resultBergstrom?.success)
            {
                details.detected("Detected Bergstrom HVAC! (bitrate: 500000, inverted polarity)");
                return;
            }
        }

        //--//

        details.info("Looking for J1939 traffic...");

        let cfgJ1939 = Models.SensorConfigForJ1939.newInstance({
                                                                   seconds       : 5,
                                                                   obdiiPort     : port,
                                                                   obdiiFrequency: 250000
                                                               });

        let resultJ1939 = await this.checkSensor(details, cfgJ1939);
        if (resultJ1939?.success)
        {
            details.detected("Detected J1939 traffic! (bitrate: 250000)");
            return;
        }

        cfgJ1939.obdiiInvert = true;

        resultJ1939 = await this.checkSensor(details, cfgJ1939);
        if (resultJ1939?.success)
        {
            details.detected("Detected J1939 traffic! (bitrate: 250000, inverted polarity)");
            return;
        }


        cfgJ1939.obdiiFrequency = 500000;
        cfgJ1939.obdiiInvert    = false;

        resultJ1939 = await this.checkSensor(details, cfgJ1939);
        if (resultJ1939?.success)
        {
            details.detected("Detected J1939 traffic! (bitrate: 500000)");
            return;
        }

        cfgJ1939.obdiiInvert = true;

        resultJ1939 = await this.checkSensor(details, cfgJ1939);
        if (resultJ1939?.success)
        {
            details.detected("Detected J1939 traffic! (bitrate: 500000, inverted polarity)");
            return;
        }

        //--//

        details.info("Looking for raw CANbus traffic...");

        let cfgRaw = Models.SensorConfigForPalfinger.newInstance({
                                                                     seconds     : 5,
                                                                     canPort     : port,
                                                                     canFrequency: 250000
                                                                 });

        let resultRaw = await this.checkSensor(details, cfgRaw);
        if (resultRaw?.success)
        {
            details.detected("Detected unknown CANbus traffic! (bitrate: 250000)");
            return;
        }

        cfgRaw.canFrequency = 500000;

        resultRaw = await this.checkSensor(details, cfgRaw);
        if (resultRaw?.success)
        {
            details.detected("Detected unknown CANbus traffic! (bitrate: 500000)");
            return;
        }

        details.failure();
    }

    private async checkGPS(port: string,
                           details: SensorDetails)
    {
        details.info("Looking for GPS unit...");

        let cfg = Models.SensorConfigForGps.newInstance({
                                                            gpsPort: port
                                                        });

        let result = <Models.SensorResultForGps>await this.checkSensor(details, cfg);
        if (result?.success)
        {
            details.detected("Detected GPS unit!");
            return;
        }

        if (result?.hasFix)
        {
            details.failure(`GPS has fix with ${result.satellitesInFix.length} satellites`);
            return;
        }

        if (result?.satellitesInView.length > 0)
        {
            details.failure(`GPS has ${result.satellitesInView.length} satellites in view, still no fix`);
            return;
        }

        details.failure();
    }

    private async checkOBDII(port: string,
                             details: SensorDetails)
    {
        details.info("Looking for OBD-II dongle...");

        let cfgOBDII = Models.SensorConfigForJ1939.newInstance({
                                                                   seconds       : 10,
                                                                   obdiiPort     : port,
                                                                   obdiiFrequency: 115200
                                                               });

        let resultOBDII = await this.checkSensor(details, cfgOBDII);
        if (resultOBDII?.success)
        {
            details.detected("Detected OBD-II traffic! (bitrate: 115200)");
            return;
        }

        cfgOBDII.obdiiFrequency = 19200;

        resultOBDII = await this.checkSensor(details, cfgOBDII);
        if (resultOBDII?.success)
        {
            details.detected("Detected OBD-II traffic! (bitrate: 19200)");
            return;
        }

        details.failure();
    }

    private async checkRS485(port: string,
                             checkAsRS232: boolean,
                             details: SensorDetails)
    {
        details.info("Looking for BlueSky charge controller...");

        let cfgBluesky = Models.SensorConfigForBluesky.newInstance({
                                                                       seconds: 5,
                                                                       ipnPort: port
                                                                   });

        let resultBluesky = await this.checkSensor(details, cfgBluesky);
        if (resultBluesky?.success)
        {
            details.detected("Detected BlueSky charge controller! (normal polarity)");
            return;
        }

        cfgBluesky.ipnInvert = true;

        resultBluesky = await this.checkSensor(details, cfgBluesky);
        if (resultBluesky?.success)
        {
            details.detected("Detected BlueSky charge controller! (inverted polarity)");
            return;
        }

        //--//

        details.info("Looking for EpSolar charge controller...");

        let cfgEpsolar = Models.SensorConfigForEpSolar.newInstance({
                                                                       seconds    : 5,
                                                                       epsolarPort: port
                                                                   });

        let resultEpsolar = await this.checkSensor(details, cfgEpsolar);
        if (resultEpsolar?.success)
        {
            details.detected("Detected EpSolar charge controller! (normal polarity)");
            return;
        }

        cfgEpsolar.epsolarInvert = true;

        resultEpsolar = await this.checkSensor(details, cfgEpsolar);
        if (resultEpsolar?.success)
        {
            details.detected("Detected EpSolar charge controller! (inverted polarity)");
            return;
        }

        //--//

        details.info("Looking for Holykell level sensor...");

        let cfgHolykell = Models.SensorConfigForHolykell.newInstance({
                                                                         seconds     : 5,
                                                                         holykellPort: port
                                                                     });

        let resultHolykell = await this.checkSensor(details, cfgHolykell);
        if (resultHolykell?.success)
        {
            details.detected("Detected Holykell level sensor! (normal polarity)");
            return;
        }

        cfgHolykell.holykellInvert = true;

        resultHolykell = await this.checkSensor(details, cfgHolykell);
        if (resultHolykell?.success)
        {
            details.detected("Detected Holykell level sensor! (inverted polarity)");
            return;
        }

        if (checkAsRS232)
        {
            details.info("Looking for MorningStar charge controller...");

            let cfgTristar = Models.SensorConfigForTriStar.newInstance({
                                                                           seconds    : 5,
                                                                           tristarPort: port
                                                                       });

            let resultTristar = await this.checkSensor(details, cfgTristar);
            if (resultTristar?.success)
            {
                details.detected("Detected MorningStar charge controller!");
                return;
            }

            details.info("Looking for Victron charge controller...");

            let cfgVictron = Models.SensorConfigForVictron.newInstance({
                                                                           seconds    : 5,
                                                                           victronPort: port
                                                                       });

            let resultVictron = await this.checkSensor(details, cfgVictron);
            if (resultVictron?.success)
            {
                details.detected("Detected Victron charge controller!");
                return;
            }
        }

        details.failure();
    }

    private async checkRS232(port: string,
                             details: SensorDetails)
    {
        details.info("Looking for ArgoHytos sensors...");

        let cfgArgoHytos = Models.SensorConfigForArgoHytos.newInstance({
                                                                           seconds      : 5,
                                                                           argohytosPort: port
                                                                       });

        let resultArgoHytos = await this.checkSensor(details, cfgArgoHytos);
        if (resultArgoHytos?.success)
        {
            details.detected("Detected ArgoHytos sensors!");
            return;
        }

        let cfgMontageBluetoothGateway = Models.SensorConfigForMontageBluetoothGateway.newInstance({
                                                                                                       seconds                    : 20,
                                                                                                       montageBluetoothGatewayPort: port
                                                                                                   });

        let resultMontageBluetoothGateway = await this.checkSensor(details, cfgMontageBluetoothGateway);
        if (resultMontageBluetoothGateway?.success)
        {
            details.detected("Detected Montage Bluetooth Gateway!");
            return;
        }

        if (false)
        {
            details.info("Looking for StealthPower No-Idle controller...");

            let cfgStealthPower = Models.SensorConfigForStealthPower.newInstance({
                                                                                     seconds         : 5,
                                                                                     stealthpowerPort: port
                                                                                 });

            let resultStealthPower = await this.checkSensor(details, cfgStealthPower);
            if (resultStealthPower?.success)
            {
                details.detected("Detected StealthPower No-Idle controller!");
                return;
            }
        }

        details.info("Looking for Victron charge controller...");

        let cfgVictron = Models.SensorConfigForVictron.newInstance({
                                                                       seconds    : 5,
                                                                       victronPort: port
                                                                   });

        let resultVictron = await this.checkSensor(details, cfgVictron);
        if (resultVictron?.success)
        {
            details.detected("Detected Victron charge controller!");
            return;
        }

        details.info("Looking for MorningStar charge controller...");

        let cfgTristar = Models.SensorConfigForTriStar.newInstance({
                                                                       seconds    : 5,
                                                                       tristarPort: port
                                                                   });

        let resultTristar = await this.checkSensor(details, cfgTristar);
        if (resultTristar?.success)
        {
            details.detected("Detected MorningStar charge controller!");
            return;
        }

        details.failure();
    }

    async checkPort(id: string,
                    callback: (details: SensorDetails) => Promise<void>)
    {
        if (this.wasDestroyed()) return;

        let details = new SensorDetails(this);
        details.id  = id;

        this.checkedPorts.push(details);

        try
        {
            details.info("Checking...");

            await callback(details);
        }
        catch (e)
        {
            details.info(`Failed due to ${e}`);
        }
    }

    private async checkI2CHub(details: SensorDetails)
    {
        details.info("Looking for I2C Hub sensors...");

        let cfg = Models.SensorConfigForI2CHub.newInstance({
                                                               seconds: 5
                                                           });

        let result = await this.checkSensor(details, cfg);
        if (result?.success && result instanceof Models.SensorResultForI2CHub)
        {
            let resultText = `Detected ${result.busScan.length} I2C sensors!`;

            for (let entry of result.busScan || [])
            {
                if (entry.bus < 0)
                {
                    resultText += ` [${entry.device} on board]`;
                }
                else
                {
                    resultText += ` [${entry.device} on port ${entry.bus + 1}]`;
                }
            }

            details.detected(resultText);
            return;
        }

        details.failure();
    }

    refreshSensors()
    {
        this.checkedPorts = [...this.checkedPorts];
        this.detectChanges();
    }

    private async checkSensor(details: SensorDetails,
                              cfg: Models.SensorConfig): Promise<Models.SensorResult>
    {
        let token = await this.app.domain.apis.sensors.startCheckStatus(cfg);

        for (let i = 0; i < 60 + cfg.seconds; i++)
        {
            await Future.delayed(1000);

            let res = await this.app.domain.apis.sensors.checkStatus(token);
            if (res)
            {
                if (res.portDetected)
                {
                    details.refreshPort();
                }
                return res;
            }
        }

        return null;
    }
}

class SensorDetails
{
    id: string;
    status: string;
    portDetected: boolean;
    sensorDetected: boolean;
    failed: boolean;

    constructor(private readonly m_comp: SensorsPageComponent)
    {
    }

    get color(): string
    {
        if (this.sensorDetected) return "#0f0";
        if (this.portDetected) return "#ff0";
        if (this.failed) return "#f00";
        return "#fff";
    }

    info(newStatus: string)
    {
        this.status = newStatus;
        this.m_comp.refreshSensors();
    }

    failure(status?: string)
    {
        this.failed = true;
        this.status = status || (this.portDetected ? "Port detected, no activity" : "Port not detected");
        this.m_comp.refreshSensors();
    }

    detected(newStatus: string)
    {
        this.status         = newStatus;
        this.sensorDetected = true;
        this.m_comp.refreshSensors();
    }

    refreshPort()
    {
        if (!this.portDetected)
        {
            this.portDetected = true;
            this.m_comp.refreshSensors();
        }
    }
}
