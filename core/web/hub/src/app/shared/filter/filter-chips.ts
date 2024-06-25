import * as Models from "app/services/proxy/model/models";

import {ControlOption} from "framework/ui/control-option";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";

export class IsSamplingFilterChip extends FilterChip
{
    static readonly options: ControlOption<Models.FilterPreferenceBoolean>[] = [
        new ControlOption(Models.FilterPreferenceBoolean.Yes, "Sampled"),
        new ControlOption(Models.FilterPreferenceBoolean.No, "Not Sampled")
    ];

    constructor(filterClear: () => void,
                filterGetter: () => string)
    {
        super("Sampling", filterClear, () => filterGetter() ? [filterGetter()] : [], IsSamplingFilterChip.options, undefined, false, true);
    }
}

export class IsClassifiedFilterChip extends FilterChip
{
    static readonly options: ControlOption<Models.FilterPreferenceBoolean>[] = [
        new ControlOption(Models.FilterPreferenceBoolean.Yes, "Classified"),
        new ControlOption(Models.FilterPreferenceBoolean.No, "Unclassified")
    ];

    constructor(filterClear: () => void,
                filterGetter: () => string)
    {
        super("Classified", filterClear, () => filterGetter() ? [filterGetter()] : [], IsClassifiedFilterChip.options, undefined, false, true);
    }
}

export class EquipmentClassFilterChip extends FilterChip
{
    constructor(filterClear: () => void,
                filterGetter: () => string[],
                public readonly options: ControlOption<string>[])
    {
        super("Equipment Class", filterClear, filterGetter, options);
    }

    getClassName(id: string): string
    {
        return this.idToLabel[id];
    }
}

export class EquipmentFilterChip extends FilterChip
{
    constructor(filterClear: () => void,
                filterGetter: () => string[],
                public readonly options: ControlOption<string>[])
    {
        super("Equipment", filterClear, filterGetter, options);
    }
}

export class DevicesFilterChip extends FilterChip
{
    constructor(filterClear: () => void,
                filterGetter: () => string[],
                public readonly options: ControlOption<string>[])
    {
        super("Device", filterClear, filterGetter, options);
    }
}

export class PointClassFilterChip extends FilterChip
{
    constructor(filterClear: () => void,
                filterGetter: () => string[],
                public readonly options: ControlOption<string>[])
    {
        super("Point Class", filterClear, filterGetter, options);
    }

    getClassName(id: string): string
    {
        return this.idToLabel[id];
    }
}

export class IsHiddenFilterChip extends FilterChip
{
    static readonly options: ControlOption<Models.FilterPreferenceBoolean>[] = [
        new ControlOption(Models.FilterPreferenceBoolean.No, "Visible"),
        new ControlOption(Models.FilterPreferenceBoolean.Yes, "Hidden")
    ];

    constructor(filterClear: () => void,
                filterGetter: () => string)
    {
        super("Hidden", filterClear, () => filterGetter() ? [filterGetter()] : [], IsHiddenFilterChip.options, undefined, false, true);
    }
}
