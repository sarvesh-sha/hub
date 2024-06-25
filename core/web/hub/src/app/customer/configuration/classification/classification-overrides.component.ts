import {ChangeDetectionStrategy, Component, Injector, Input, ViewChild} from "@angular/core";
import {ClassificationDetailPageComponent} from "app/customer/configuration/classification/classification-detail-page.component";
import {ClassificationOverrideDialogComponent, ClassificationOverrideDialogConfig} from "app/customer/configuration/classification/classification-override-dialog.component";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {ControlPointsSelectionExtended} from "app/services/domain/report-definitions.service";
import * as Models from "app/services/proxy/model/models";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {inParallel} from "framework/utils/concurrency";

@Component({
               selector       : "o3-classification-overrides",
               templateUrl    : "./classification-overrides.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ClassificationOverridesComponent extends BaseApplicationComponent
{
    overrides: Override[];

    @ViewChild(OverlayComponent, {static: true}) overlay: OverlayComponent;

    overlayConfig = new OverlayConfig();

    controlPoints: ControlPointsSelectionExtended;

    private m_rules: Models.NormalizationRules;

    constructor(inj: Injector,
                public host: ClassificationDetailPageComponent)
    {
        super(inj);

        this.controlPoints = new ControlPointsSelectionExtended(this.app.domain);
    }

    @Input()
    public set rules(rules: Models.NormalizationRules)
    {
        this.m_rules = rules;
        this.refresh();
    }

    public get rules(): Models.NormalizationRules
    {
        return this.m_rules;
    }

    private async refresh()
    {
        let rows: Override[] = [];
        for (let sysId in this.rules.pointOverrides)
        {
            rows.push(new Override(sysId, this.rules.pointOverrides[sysId], this.host));
        }

        await inParallel(rows, async (item) =>
        {
            item.point = await this.app.domain.assets.getTypedExtendedById(DeviceElementExtended, item.sysId);
        });

        this.overrides = rows;
        this.markForCheck();
    }

    async override(row: Override)
    {
        let cfg       = new ClassificationOverrideDialogConfig();
        cfg.sysId     = row.sysId;
        cfg.overrides = row.data;
        cfg.rules     = this.host.normalization.model.rules;
        await ClassificationOverrideDialogComponent.open(this, cfg);
        if (!cfg.overrides)
        {
            // removed
            this.overrides = this.overrides.filter((o) => o !== row);
        }
        else
        {
            row.data = cfg.overrides;
            row.refresh(this.host);
            this.overrides = [...this.overrides];
        }

        this.host.setDirty();
        this.markForCheck();
    }

    async overrideAll()
    {
        let points = await this.controlPoints.getRecords(DeviceElementExtended);
        let sysIds = points.map((p) => p.model.sysId);

        if (sysIds.some((id) => this.host.rules.pointOverrides.hasOwnProperty(id)))
        {
            let confirmed = await this.confirmOperation("Some points already have overrides and will be overwritten.");
            if (!confirmed)
            {
                return;
            }
        }

        this.cancel();

        if (!sysIds.length)
        {
            return;
        }

        let cfg       = new ClassificationOverrideDialogConfig();
        cfg.sysId     = sysIds;
        cfg.overrides = new Models.DeviceElementClassificationOverrides();
        cfg.rules     = this.host.rules;
        await ClassificationOverrideDialogComponent.open(this, cfg);

        if (cfg.overrides)
        {
            for (let point of points)
            {
                let row       = this.overrides.find((r) => r.sysId === point.model.sysId);
                let overrides = Models.DeviceElementClassificationOverrides.newInstance(cfg.overrides);
                if (!row)
                {
                    row       = new Override(point.model.sysId, overrides, this.host);
                    row.point = point;
                    this.overrides.push(row);
                }
                else
                {
                    row.data = overrides;
                    row.refresh(this.host);
                }
            }

            this.overrides = [...this.overrides];
        }
        else
        {
            // Overrides cleared
            this.overrides = this.overrides.filter((override) => sysIds.indexOf(override.sysId) === -1);
        }

        this.host.setDirty();
        this.markForCheck();
    }

    add()
    {
        if (this.overlay)
        {
            this.overlay.toggleOverlay();
            this.markForCheck();
        }
    }

    cancel()
    {
        this.controlPoints = new ControlPointsSelectionExtended(this.app.domain);
        if (this.overlay)
        {
            this.overlay.closeOverlay();
        }
    }

    get canOverride(): boolean
    {
        return this.controlPoints && this.controlPoints.identities.length > 0;
    }
}


class Override
{
    sysId: string;
    data: Models.DeviceElementClassificationOverrides;
    point: DeviceElementExtended;

    equipmentOverride: string  = "";
    pointClassOverride: string = "";
    locationOverride: string   = "";

    constructor(sysId: string,
                override: Models.DeviceElementClassificationOverrides,
                host: ClassificationDetailPageComponent)
    {
        this.data  = override;
        this.sysId = sysId;

        this.refresh(host);
    }

    refresh(host: ClassificationDetailPageComponent)
    {
        if (this.data.equipments)
        {
            this.equipmentOverride = this.data.equipments
                                         .map((eq) => host.formatEquipment(eq))
                                         .join("\n");
        }

        if (this.data.locationsWithType)
        {
            this.locationOverride = host.formatLocations(this.data.locationsWithType);
        }

        if (this.data.pointClassId)
        {
            let pointClass = host.getPointClassOption(this.data.pointClassId);
            if (pointClass)
            {
                this.pointClassOverride = pointClass.label;
            }
        }
    }
}
