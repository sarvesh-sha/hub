import * as Models from "app/services/proxy/model/models";

export class WidgetExportInfo<T extends Models.WidgetConfiguration>
{
    constructor(public readonly config: T,
                public outline?: Models.WidgetOutline,
                public readonly selectors: Models.SharedAssetSelector[] = [],
                public readonly graphs: Models.SharedAssetGraph[] = [])
    {
    }
}
