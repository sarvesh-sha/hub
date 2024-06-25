import {Component, Inject, OnInit} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";

import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./simulated-data-dialog.component.html"
           })
export class SimulatedDataDialogComponent implements OnInit
{
    creating: boolean;

    constructor(public dialogRef: OverlayDialogRef<void>,
                public apis: ApiService,
                @Inject(OVERLAY_DATA) public data: DialogConfig)
    {
    }

    public static async open(comp: BaseComponent): Promise<void>
    {
        let cfg                = new DialogConfig();
        cfg.gatewayName        = "Simulated Gateway 1";
        cfg.numberOfDevices    = 1;
        cfg.samplingPeriod     = 300;
        cfg.numberOfDaysOfData = 30;

        await OverlayComponent.open(comp, SimulatedDataDialogComponent, {data: cfg});

        cfg.dialogCancelled = true;
    }

    async ngOnInit()
    {
    }

    async createSimulatedData()
    {
        this.creating = true;

        let simGatewayId = await this.apis.demoTasks.createGateway(this.data.gatewayName, this.data.numberOfDevices, this.data.numberOfDaysOfData, this.data.samplingPeriod);

        this.dialogRef.close();
    }
}

class DialogConfig
{
    gatewayName: string;

    numberOfDevices: number;

    samplingPeriod: number;

    numberOfDaysOfData: number;

    dialogCancelled: boolean;
}
