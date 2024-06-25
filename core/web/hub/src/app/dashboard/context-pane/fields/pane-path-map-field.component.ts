import {ChangeDetectionStrategy, Component, Input} from "@angular/core";

import {PaneFieldComponent} from "app/dashboard/context-pane/fields/pane-field.component";
import {AssetExtended} from "app/services/domain/assets.service";
import * as Models from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

@Component({
               selector       : "o3-pane-path-map-field",
               templateUrl    : "./pane-path-map-field.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class PanePathMapFieldComponent extends PaneFieldComponent
{
    public device: AssetExtended;

    @Input()
    public set deviceId(id: Models.RecordIdentity)
    {
        this.initDevice(id);
    }

    @Input()
    public range = RangeSelectionExtended.newModel();

    public isClickable(): boolean
    {
        return false;
    }

    private async initDevice(id: Models.RecordIdentity)
    {
        this.device = await this.app.domain.assets.getTypedExtendedByIdentity(AssetExtended, id);
        this.markForCheck();
    }
}
