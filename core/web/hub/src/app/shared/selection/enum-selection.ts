import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {ControlOption} from "framework/ui/control-option";
import {DatatableSelectionChangeSummary, DatatableSelectionTargetType} from "framework/ui/datatables/datatable-manager";

export const ALL_ENUMS_ID: string = "All";

export function triStateChange(change: DatatableSelectionChangeSummary<string>,
                               set: Set<string>): Set<string>
{
    if (change.target === ALL_ENUMS_ID && change.state === true)
    {
        // Clear all entries
        set = new Set<string>();
        // Add only the all state selector
        set.add(ALL_ENUMS_ID);
    }
    else if (change.target !== ALL_ENUMS_ID && change.type !== DatatableSelectionTargetType.ALL && change.state === true)
    {
        // Add all values back
        set = new Set<string>(set.values());
        // Remove the all state value
        set.delete(ALL_ENUMS_ID);
    }

    return set;
}

export function enumControlOptionsWithAll(enums: Models.EnumDescriptor[],
                                          allLabel: string): ControlOption<string>[]
{
    let options = SharedSvc.BaseService.mapEnumOptions<string>(enums);
    options.unshift(new ControlOption<string>(ALL_ENUMS_ID, allLabel));

    return options;
}
