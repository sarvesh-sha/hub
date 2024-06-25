import {ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, Output} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {Lookup} from "framework/services/utils.service";
import {SyncDebouncer} from "framework/utils/debouncers";

@Component({
               selector       : "o3-range-override",
               templateUrl    : "./range-override.component.html",
               styleUrls      : ["./range-override.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class RangeOverrideComponent extends SharedSvc.BaseApplicationComponent
{
    private m_model: Models.ToggleableNumericRange;
    @Input() set model(model: Models.ToggleableNumericRange)
    {
        if (model)
        {
            model.minInvalid = model.minInvalid || model.minInvalid == null;
            model.maxInvalid = model.maxInvalid || model.maxInvalid == null;
            if (model.minInvalid) model.min = undefined;
            if (model.maxInvalid) model.max = undefined;

            this.m_model = model;
        }
    }

    get model(): Models.ToggleableNumericRange
    {
        return this.m_model;
    }

    @Input() toggleLabel: string = "";
    @Input() vertical: boolean   = false;
    @Input() hide: boolean       = false;
    @Input() disabled: boolean   = false;

    textEmissionDebouncer = new SyncDebouncer(750, () => this.emitIfChanged());

    @Output() modelChange = new EventEmitter<Models.ToggleableNumericRange>();

    private changeOccurred: boolean = false;

    constructor(inj: Injector)
    {
        super(inj);

        this.model = new Models.ToggleableNumericRange();
    }

    classes(): Lookup<boolean>
    {
        return {
            "vertical"  : this.vertical,
            "horizontal": !this.vertical,
            "inactive"  : !this.m_model.active,
            "disabled"  : this.disabled
        };
    }

    activeToggled()
    {
        if (this.m_model.active)
        {
            this.m_model.min        = 0;
            this.m_model.minInvalid = false;
        }

        this.emitModel(true);
    }

    emitIfChanged()
    {
        if (this.changeOccurred) this.emitModel(true);
    }

    emitModel(forceEmission: boolean = false): void
    {
        if (forceEmission)
        {
            this.modelChange.emit(Models.ToggleableNumericRange.newInstance(this.m_model));
            this.changeOccurred = false;
        }
        else
        {
            this.changeOccurred = true;
        }
    }

    minChanged(value: number)
    {
        if (this.m_model.min != value)
        {
            this.m_model.min = value;
            this.limitChanged();
        }
    }

    maxChanged(value: number)
    {
        if (this.m_model.max != value)
        {
            this.m_model.max = value;
            this.limitChanged();
        }
    }

    private limitChanged()
    {
        this.changeOccurred     = true;
        this.m_model.maxInvalid = this.m_model.max == null;
        this.m_model.minInvalid = this.m_model.min == null;
        this.textEmissionDebouncer.invoke();
    }
}
