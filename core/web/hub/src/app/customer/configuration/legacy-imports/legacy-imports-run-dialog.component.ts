import {Component, Inject, Injector} from "@angular/core";

import {ApiService} from "app/services/domain/api.service";
import * as Models from "app/services/proxy/model/models";

import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./legacy-imports-run-dialog.component.html"
           })
export class LegacyImportsRunDialogComponent extends BaseComponent
{

    constructor(public dialogRef: OverlayDialogRef<void>,
                public apis: ApiService,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: DialogConfig)
    {
        super(inj);
    }

    public static async open(comp: BaseComponent,
                             sysId: string): Promise<void>
    {
        let cfg           = new DialogConfig();
        cfg.dataImportsId = sysId;

        await OverlayComponent.open(comp, LegacyImportsRunDialogComponent, {
            data  : cfg,
            config: OverlayConfig.newInstance({width: "80%"})
        });
        cfg.dialogCancelled = true;
    }

    async ngOnInit()
    {
        super.ngOnInit();

        let run = Models.DataImportRun.newInstance({dataImportsId: this.data.dataImportsId});

        this.data.handle     = await this.apis.dataImports.startImport(run);
        this.data.processing = true;

        this.refresh();
    }

    async refresh()
    {
        if (this.data.dialogCancelled) return;

        let progress = await this.apis.dataImports.checkImport(this.data.handle);
        if (progress)
        {
            this.data.devicesToProcess  = progress.devicesToProcess;
            this.data.devicesProcessed  = progress.devicesProcessed;
            this.data.elementsProcessed = progress.elementsProcessed;
            this.data.elementsModified  = progress.elementsModified;

            switch (progress.status)
            {
                case Models.BackgroundActivityStatus.COMPLETED:
                case Models.BackgroundActivityStatus.FAILED:
                case Models.BackgroundActivityStatus.CANCELLED:
                    this.data.processing = false;
                    break;

                default:
                    setTimeout(() => this.refresh(), 1000);
                    break;
            }
        }
    }
}

class DialogConfig
{
    public dataImportsId: string;

    public dialogCancelled: boolean;

    public processing: boolean;

    //--//

    public handle: string;

    public devicesToProcess: number;
    public devicesProcessed: number;

    public elementsProcessed: number;
    public elementsModified: number;
}
