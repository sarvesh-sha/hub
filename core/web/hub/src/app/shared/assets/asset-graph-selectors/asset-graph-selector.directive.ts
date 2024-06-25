import {Directive} from "@angular/core";

import {AppContext} from "app/app.service";
import {ControlOption} from "framework/ui/control-option";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3AssetGraphSelector]"
           })
export class AssetGraphSelectorDirective
{
    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    async ngOnInit()
    {
        this.selectComponent.placeholder        = this.selectComponent.placeholder || "Select asset structure";
        this.selectComponent.multiSelect        = false;
        this.selectComponent.singleClick        = true;
        this.selectComponent.preventDeselection = true;

        let ids                      = await this.app.domain.assetGraphs.getGraphIds();
        let graphs                   = await this.app.domain.assetGraphs.getConfigBatch(ids);
        this.selectComponent.options = graphs.map((g) => new ControlOption(g.id, g.name));
    }
}
