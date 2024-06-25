import {Component, Inject, Injector} from "@angular/core";

import {ProberState} from "app/customer/data-collection/gateways/probers/probers-detail-page.component";

import {ApiService} from "app/services/domain/api.service";

import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./probers-detail-page-dialog.component.html"
           })
export class ProbersDetailPageDialogComponent extends BaseComponent
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                public apis: ApiService,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: ProberState)
    {
        super(inj);
    }

    public static open(comp: BaseComponent,
                       cfg: ProberState): Promise<boolean>
    {
        return OverlayComponent.open(comp, ProbersDetailPageDialogComponent, {
            data  : cfg,
            config: OverlayConfig.newInstance({containerClasses: ["dialog-xl"]})
        });
    }

    async ngOnInit()
    {
        super.ngOnInit();
    }

    wizardCancel()
    {
        this.dialogRef.close(null);
    }

    async wizardCommit()
    {
        this.dialogRef.close(true);
    }
}
