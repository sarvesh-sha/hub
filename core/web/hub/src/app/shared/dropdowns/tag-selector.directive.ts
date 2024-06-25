import {Directive, Input} from "@angular/core";

import {AppContext} from "app/app.service";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3TagSelector]"
           })
export class TagSelectorDirective
{
    @Input("o3TagSelector")
    public sink: boolean;

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    async ngOnInit()
    {
        this.selectComponent.placeholder             = this.selectComponent.placeholder === undefined ? "Tag" : this.selectComponent.placeholder;
        this.selectComponent.defaultValueDescription = this.selectComponent.defaultValueDescription || "No Tag Specified";
        this.selectComponent.options                 = await this.app.bindings.getTags();
    }
}
