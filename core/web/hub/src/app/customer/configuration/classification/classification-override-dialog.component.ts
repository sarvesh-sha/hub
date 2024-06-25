import {ChangeDetectionStrategy, Component, Inject, Injector} from "@angular/core";
import {ApiService} from "app/services/domain/api.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl    : "./classification-override-dialog.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ClassificationOverrideDialogComponent extends BaseApplicationComponent
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                public apis: ApiService,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: ClassificationOverrideDialogConfig)
    {
        super(inj);
        if (!this.data.overrides)
        {
            this.data.overrides = new Models.DeviceElementClassificationOverrides();
        }
    }

    public static open(comp: BaseComponent,
                       cfg: ClassificationOverrideDialogConfig)
    {
        return OverlayComponent.open(comp, ClassificationOverrideDialogComponent, {data: cfg});
    }

    get model(): Models.DeviceElementClassificationOverrides
    {
        return this.data.overrides;
    }

    get locations(): string
    {
        return this.model.locationsWithType ? this.model.locationsWithType.map((loc) => loc.name)
                                                  .join(" | ") : null;
    }

    set locations(locationRaw: string)
    {
        this.model.locationsWithType = locationRaw.split("|")
                                                  .map((s) => s.trim())
                                                  .filter((s) => !!s)
                                                  .map((name) =>
                                                           Models.NormalizationEquipmentLocation.newInstance({
                                                                                                                 name: name,
                                                                                                                 type: Models.LocationType.OTHER
                                                                                                             }));
    }

    get equipments(): string
    {
        return this.model.equipments ? this.model.equipments.map((eq) => eq.name)
                                           .join(" | ") : null;
    }

    set equipments(equipmentsRaw: string)
    {
        this.model.equipments = equipmentsRaw.split("|")
                                             .map((s) => s.trim())
                                             .filter((s) => !!s)
                                             .map((name) =>
                                                      Models.NormalizationEquipment.newInstance({
                                                                                                    name: name
                                                                                                }));
    }

    async override()
    {
        this.model.equipments        = this.model.equipments && this.model.equipments.length ? this.model.equipments : null;
        this.model.pointName         = this.model.pointName || null;
        this.model.locationsWithType = this.model.locationsWithType && this.model.locationsWithType.length ? this.model.locationsWithType : null;
        this.model.pointClassId      = this.model.pointClassId || null;
        await this.applyOverride(this.model);
    }

    async clearOverrides()
    {
        this.data.overrides = null;
        await this.applyOverride(null);
    }

    private async applyOverride(overrides: Models.DeviceElementClassificationOverrides)
    {
        let sysIds: string[];
        if (Array.isArray(this.data.sysId))
        {
            sysIds = this.data.sysId;
        }
        else
        {
            sysIds = [this.data.sysId];
        }

        for (let sysId of sysIds)
        {
            this.data.rules.pointOverrides[sysId] = overrides;
        }

        this.dialogRef.close(true);
    }
}

export class ClassificationOverrideDialogConfig
{
    sysId: string | string[];
    overrides: Models.DeviceElementClassificationOverrides;
    rules: Models.NormalizationRules;
}
