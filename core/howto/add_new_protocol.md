To add a new gateway protocol, several files need to be touched:

1) Define models for the different messages as subclasses of IpnObjectModel, annotating fields with proper metadata to classify the various properties of a sensor, for example.
2) Add protocol configuration parameters in the ProtocolConfigForIpn class. For example:
```java
   public String montageBluetoothGatewayPort;
```

3) Add similar configuration parameters in the ProberOperationForIpn class.
4) Link values in ProberOperationForIpn to values in ProtocolConfigForIpn in ProberForIpn class. For example:
```java
        cfg.montageBluetoothGatewayPort = input.montageBluetoothGatewayPort;
```
5) Add a new subclass of ServiceWorker or ServiceWorkerWithWatchdog, to host the logic, decoders, etc. and integrate with the IpnManager. 
6) Instantiate the worker in IpnManager based on the presence of configuration parameters. For example: 
```java
   if (StringUtils.isNotBlank(cfg.montageBluetoothGatewayPort))
   {
       addWorker(new WorkerForMontageBluetoothGateway(this, cfg.montageBluetoothGatewayPort));
   }
```
7) Add a new enum in GatewayAutoDiscovery. For example:
```java
    public enum Flavor
    {
        ...
        MontageBluetoothGateway,

```

8) Create probe code in GatewayStateImpl. For example, insert checks in checkRS232:
```java
        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.montageBluetoothGatewayPort = port;

            foundSomething = await(state.checkSensor(GatewayAutoDiscovery.Flavor.MontageBluetoothGateway, cfg.montageBluetoothGatewayPort, false, BaseBluetoothGatewayObjectModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }
        }

```
9) Handle discovery notifications in the TaskForTransportationAutoConfiguration class, to automatically populate network configurations. For example:
```java
                    case MontageBluetoothGateway:
                        cfg.montageBluetoothGatewayPort = cfgFound.montageBluetoothGatewayPort;

                        addEntry(discovered, name_gateway, "Found Montage Bluetooth Gateway at %s", cfg.montageBluetoothGatewayPort);
                        got = true;
                        break;
```
10) Write the Sensor detection logic in the Waypoint product.
- A subclass of SensorConfig and write detection logic
```java
@JsonTypeName("SensorConfigForMontageBluetoothGateway")
public class SensorConfigForMontageBluetoothGateway extends SensorConfig
{
```
- A subclass of SensorResult
```java
@JsonTypeName("SensorResultForMontageBluetoothGateway")
public class SensorResultForMontageBluetoothGateway extends SensorResult
{
```

11) Rebuild Hub and Waypoint SDKs:
```
o3 sdk hub
o3 sdk waypoint
```
12) In the Hub Angular project, update the three pages used to configure networks:
- app/customer/data-collection/networks/networks-detail-page.component.html
```angular2html
                        <div class="row">
                            <div class="col-sm-3">
                                <mat-form-field>
                                    <input matInput type="text" placeholder="Montage Bluetooth Gateway Port" [value]="ipn.montageBluetoothGatewayPort" name="ipn_montageBluetoothGatewayPort" readonly>
                                </mat-form-field>
                            </div>
                        </div>
```
- app/customer/data-collection/networks/networks-wizard/networks-wizard-ipn-step.component.html
```angular2html
    <div class="row">
        <div class="col-sm-3">
            <mat-form-field>
                <input matInput type="text" placeholder="Montage Bluetooth Gateway Port" [(ngModel)]="data.ipn.montageBluetoothGatewayPort" name="ipn_montageBluetoothGatewayPort">
            </mat-form-field>
        </div>
    </div>
```
- app/customer/data-collection/networks/networks-wizard/networks-wizard-confirm-step.component.html
```angular2html
            <div class="row">
                <div class="col-sm-3">
                    <mat-form-field>
                        <input matInput type="text" placeholder="Montage Bluetooth Gateway Port" [value]="data.ipn.montageBluetoothGatewayPort" name="ipn_montageBluetoothGatewayPort" readonly>
                    </mat-form-field>
                </div>
            </div>
```
13) In the Waypoint Angular project, update the detection logic. For example, in the checkRS232 method:
```typescript
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
```
