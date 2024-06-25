import {Component, EventEmitter, Input, Output, SimpleChanges} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {EngineeringUnitsDescriptorExtended} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";

import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-equivalent-units-selector[unitsFactors]",
               templateUrl: "./equivalent-units-selector.component.html",
               styles     : [":host { display: block; width: 100% }"]
           })
export class EquivalentUnitsSelectorComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() units: EngineeringUnitsDescriptorExtended;
    @Output() unitsChange = new EventEmitter<EngineeringUnitsDescriptorExtended>();

    @Input() unitsFactors: Models.EngineeringUnitsFactors;
    @Input() placeholder: string        = "Units";
    @Input() updatePreferred: boolean   = true;
    @Input() standaloneNgModel: boolean = false;

    public ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        if (changes.unitsFactors)
        {
            // should be triggered when unitsFactors changes
            // units also needs to already be set which may not happen even if all updated in same cd cycle
            this.setUpUnits();
        }
    }

    equivalentUnits: ControlOption<EngineeringUnitsDescriptorExtended>[] = [];
    unitsDisplay: string                                                 = "No Units";
    disabled: boolean                                                    = false;

    get ngModelOptions(): { standalone: boolean }
    {
        return this.standaloneNgModel ? {standalone: true} : undefined;
    }

    unitChanged()
    {
        if (this.updatePreferred && this.units) this.app.domain.units.setPreferred(EngineeringUnitsDescriptorExtended.extractFactors(this.units));

        this.unitsChange.emit(this.units);
    }

    private async setUpUnits()
    {
        this.equivalentUnits = await this.app.domain.units.getEquivalentUnits(this.unitsFactors);
        this.disabled        = !this.equivalentUnits.length;

        if (!this.disabled)
        {
            let replaceUnit = true;
            for (let co of this.equivalentUnits)
            {
                if (co.id == this.units)
                {
                    replaceUnit = false;
                    break;
                }
            }

            if (replaceUnit)
            {
                let preferred = await this.app.domain.units.findPreferred(this.unitsFactors);
                this.units    = await this.app.domain.units.resolveDescriptor(preferred, false);
                if (this.units?.noDimensions)
                {
                    this.unitsDisplay = this.equivalentUnits[0]?.id?.model.description?.toLocaleLowerCase() || this.units?.model.displayName;
                }
            }
        }

        if (!this.unitsDisplay) this.unitsDisplay = "No Units";

        this.markForCheck();
    }
}
