import {Directive, Input} from "@angular/core";

import {AppContext} from "app/app.service";
import * as Models from "app/services/proxy/model/models";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3PointClassSelector]"
           })
export class PointClassSelectorDirective
{
    @Input("o3PointClassSelector")
    public sink: boolean;

    @Input() rules: Models.NormalizationRules;
    @Input() onlyUsed: boolean = false;

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    async ngOnInit()
    {
        if (this.selectComponent.placeholder === undefined) this.selectComponent.placeholder = "Point Class";
        if (this.selectComponent.defaultValueDescription === undefined) this.selectComponent.defaultValueDescription = "No Point Class Specified";
        this.selectComponent.options = await this.app.bindings.getPointClasses(this.onlyUsed, this.rules);
    }
}
