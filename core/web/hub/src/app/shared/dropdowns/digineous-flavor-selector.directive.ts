import {Directive, Input} from "@angular/core";

import {AppContext} from "app/app.service";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3DigineousFlavorSelector]"
           })
export class DigineousFlavorSelectorDirective
{
    @Input("o3DigineousFlavorSelector")
    public sink: boolean;

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    async ngOnInit()
    {
        if (this.selectComponent.placeholder === undefined) this.selectComponent.placeholder = "Flavor";
        if (this.selectComponent.defaultValueDescription === "Select Option") this.selectComponent.defaultValueDescription = "No Flavor Specified";

        this.selectComponent.multiSelect = false;
        this.selectComponent.singleClick = true;
        this.selectComponent.options     = this.app.domain.digineous.getFlavorOptions();
    }
}
