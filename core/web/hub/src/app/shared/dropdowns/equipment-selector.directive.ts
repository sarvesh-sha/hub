import {Directive, Input} from "@angular/core";

import {AppContext} from "app/app.service";
import {EquipmentSummary} from "app/services/domain/assets.service";
import {ControlOption} from "framework/ui/control-option";
import {ILazyTreeNode} from "framework/ui/dropdowns/filterable-tree.component";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3EquipmentSelector]"
           })
export class EquipmentSelectorDirective
{
    @Input("o3EquipmentSelector")
    public sink: boolean;

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    private static labelFromEquipmentSummary(eq: EquipmentSummary): string
    {
        return eq ? `${eq.name} - ${eq.locationName}` : "";
    }

    async ngOnInit()
    {
        if (this.selectComponent.placeholder === undefined) this.selectComponent.placeholder = "Equipments";
        this.selectComponent.lazyLoader = {
            getTree  : () => this.app.domain.assets.getEquipmentTree(),
            loadNodes: async (nodes: ILazyTreeNode<string>[]) =>
            {
                let result    = await this.app.domain.assets.getEquipmentSummaries(nodes);
                let processed = result.map((eq) =>
                                           {
                                               let label        = EquipmentSelectorDirective.labelFromEquipmentSummary(eq);
                                               let opt          = new ControlOption<string>(eq.id, label, null);
                                               opt.hasChildren  = eq.children.length > 0;
                                               opt.lazyChildren = eq.children;
                                               return opt;
                                           });


                return processed;
            },
            getLabel : async (selectedId: string) =>
            {
                let node: ILazyTreeNode<string> = {
                    id      : selectedId,
                    children: null
                };
                let summaryHolder               = await this.app.domain.assets.getEquipmentSummaries([node]);
                return EquipmentSelectorDirective.labelFromEquipmentSummary(summaryHolder[0]);
            }
        };
    }
}
