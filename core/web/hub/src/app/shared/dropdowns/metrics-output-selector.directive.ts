import {Directive, Input} from "@angular/core";
import {AppContext} from "app/app.service";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3MetricsOutputSelector]"
           })
export class MetricsOutputSelectorDirective
{
    @Input("o3MetricsOutputSelector")
    public sink: boolean;

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    async ngOnInit()
    {
        this.selectComponent.placeholder             = this.selectComponent.placeholder === undefined ? "Metrics Output" : this.selectComponent.placeholder;
        this.selectComponent.defaultValueDescription = this.selectComponent.defaultValueDescription || "No Metrics Specified";
        this.selectComponent.multiSelect             = false;
        this.selectComponent.singleClick             = true;
        this.selectComponent.options                 = await this.app.bindings.getMetricsNamedOutput();
    }
}
