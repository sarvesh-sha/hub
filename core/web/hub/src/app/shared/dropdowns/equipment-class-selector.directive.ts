import {Directive, Input} from "@angular/core";

import {AppContext} from "app/app.service";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3EquipmentClassSelector]"
           })
export class EquipmentClassSelectorDirective
{
    @Input("o3EquipmentClassSelector")
    public sink: boolean;

    @Input() onlyUsed: boolean = false;

    @Input() allowMultipleClasses: boolean = false;

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    async ngOnInit()
    {
        if (this.selectComponent.placeholder === undefined) this.selectComponent.placeholder = "Equipment Classes";
        if (this.selectComponent.defaultValueDescription === undefined) this.selectComponent.defaultValueDescription = "No Equipment Class Specified";
        this.selectComponent.multiSelect = this.allowMultipleClasses;
        this.selectComponent.singleClick = true;
        this.selectComponent.options     = await this.app.bindings.getEquipmentClasses(this.onlyUsed, null);
    }
}
