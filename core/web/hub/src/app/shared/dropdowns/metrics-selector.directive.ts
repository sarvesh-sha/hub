import {Directive, Input} from "@angular/core";

import {AppContext} from "app/app.service";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3MetricsSelector]"
           })
export class MetricsSelectorDirective
{
    @Input("o3MetricsSelector")
    public sink: boolean;

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    async ngOnInit()
    {
        this.selectComponent.placeholder             = this.selectComponent.placeholder === undefined ? "Metrics" : this.selectComponent.placeholder;
        this.selectComponent.defaultValueDescription = this.selectComponent.defaultValueDescription || "No Metrics Specified";
        this.selectComponent.multiSelect             = false;
        this.selectComponent.singleClick             = true;
        this.selectComponent.navigationFn            = () => this.navigate();
        this.selectComponent.options                 = await this.app.bindings.getMetricsOptions();
    }

    navigate()
    {
        if (this.selectComponent.value) this.app.ui.navigation.go("/configuration/metrics/metric", [this.selectComponent.value]);
    }
}
