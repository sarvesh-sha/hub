import {Directive, Input} from "@angular/core";

import {AppContext} from "app/app.service";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3LocationSelector]"
           })
export class LocationSelectorDirective
{
    @Input("o3LocationSelector")
    public sink: boolean;

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    async ngOnInit()
    {
        if (this.selectComponent.placeholder === undefined) this.selectComponent.placeholder = "Location";
        if (this.selectComponent.defaultValueDescription === "Select Option") this.selectComponent.defaultValueDescription = "No Location Specified";

        this.selectComponent.singleClick  = true;
        this.selectComponent.navigationFn = () => this.navigate();
        this.selectComponent.options      = await this.app.bindings.getLocationsOptions();
    }

    navigate()
    {
        if (this.selectComponent.value) this.app.ui.navigation.go("/configuration/locations/location", [this.selectComponent.value]);
    }
}
