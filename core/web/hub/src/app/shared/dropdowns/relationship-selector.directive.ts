import {Directive, Input} from "@angular/core";

import {AppContext} from "app/app.service";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3RelationshipSelector]"
           })
export class RelationshipSelectorDirective
{
    @Input("o3RelationshipSelector")
    public sink: boolean;

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    async ngOnInit()
    {
        this.selectComponent.placeholder             = this.selectComponent.placeholder === undefined ? "Relationship" : this.selectComponent.placeholder;
        this.selectComponent.defaultValueDescription = "No Relationship Specified";
        this.selectComponent.multiSelect             = false;
        this.selectComponent.options                 = await this.app.domain.assets.getRelationships();
        this.selectComponent.singleClick             = true;
        this.selectComponent.preventDeselection      = true;
    }
}
