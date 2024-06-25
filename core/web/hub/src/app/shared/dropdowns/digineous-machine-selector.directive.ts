import {Directive, Input} from "@angular/core";

import {AppContext} from "app/app.service";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {ILazyLoader, ILazyTreeNode, ITreeNode} from "framework/ui/dropdowns/filterable-tree.component";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3DigineousMachineSelector]"
           })
export class DigineousMachineSelectorDirective implements ILazyLoader<string>
{
    @Input("o3DigineousMachineSelector")
    public sink: boolean;

    private m_machines: Map<string, Models.DigineousMachineConfig>;

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    async ngOnInit()
    {
        if (this.selectComponent.placeholder === undefined) this.selectComponent.placeholder = "Machine";
        if (this.selectComponent.defaultValueDescription === "Select Option") this.selectComponent.defaultValueDescription = "No Machine Specified";

        this.selectComponent.multiSelect = false;
        this.selectComponent.singleClick = true;
        this.selectComponent.lazyLoader  = this;
    }

    public async getTree(): Promise<ILazyTreeNode<string>[]>
    {
        await this.ensureLoaded();

        let tree = [];

        for (let key of this.m_machines.keys())
        {
            tree.push({
                          id      : key,
                          children: []
                      });
        }

        tree.sort((a,
                   b) => UtilsService.compareStrings(this.m_machines.get(a.id).machineName, this.m_machines.get(b.id).machineName, true));

        return tree;
    }

    public async loadNodes(nodes: ILazyTreeNode<string>[]): Promise<ITreeNode<string>[]>
    {
        let res: ITreeNode<string>[] = [];

        for (let node of nodes)
        {
            let machine = this.m_machines.get(node.id);
            if (machine)
            {
                res.push(new ControlOption<string>(node.id, machine.machineName, null));
            }
        }

        return res;
    }

    public async getLabel(selected: string): Promise<string>
    {
        await this.ensureLoaded();

        let machine = this.m_machines.get(selected);
        return machine?.machineName;
    }

    private async ensureLoaded()
    {
        if (!this.m_machines)
        {
            this.m_machines = await this.app.domain.digineous.getMachines();
        }
    }
}
