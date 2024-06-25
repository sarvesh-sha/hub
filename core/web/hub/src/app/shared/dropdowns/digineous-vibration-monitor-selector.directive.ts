import {Directive, Input} from "@angular/core";

import {AppContext} from "app/app.service";
import {UtilsService} from "framework/services/utils.service";

import {ControlOption} from "framework/ui/control-option";
import {ILazyLoader, ILazyTreeNode, ITreeNode} from "framework/ui/dropdowns/filterable-tree.component";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3DigineousVibrationMonitorSelector]"
           })
export class DigineousVibrationMonitorSelectorDirective implements ILazyLoader<number>
{
    @Input("o3DigineousVibrationMonitorSelector")
    public sink: boolean;

    private m_monitorOptions: ControlOption<number>[];

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<number>)
    {
    }

    async ngOnInit()
    {
        if (this.selectComponent.placeholder === undefined) this.selectComponent.placeholder = "Vibration Monitor";
        if (this.selectComponent.defaultValueDescription === "Select Option") this.selectComponent.defaultValueDescription = "No Vibration Monitor Specified";

        this.selectComponent.multiSelect = false;
        this.selectComponent.singleClick = true;
        this.selectComponent.lazyLoader  = this;
    }

    public async getTree(): Promise<ILazyTreeNode<number>[]>
    {
        await this.ensureLoaded();

        let tree = [];

        for (let monitorOption of this.m_monitorOptions)
        {
            tree.push({
                          id      : monitorOption.id,
                          children: []
                      });
        }

        return tree;
    }

    public async loadNodes(nodes: ILazyTreeNode<number>[]): Promise<ITreeNode<number>[]>
    {
        let res: ITreeNode<number>[] = [];

        for (let node of nodes)
        {
            let monitorOption = this.m_monitorOptions.find((monitorOption) => monitorOption.id == node.id);
            if (monitorOption)
            {
                res.push(monitorOption);
            }
        }

        return res;
    }

    public async getLabel(selected: number): Promise<string>
    {
        await this.ensureLoaded();

        let monitorOption = this.m_monitorOptions.find((monitorOption) => monitorOption.id == selected);
        return monitorOption?.label;
    }

    private async ensureLoaded()
    {
        if (!this.m_monitorOptions)
        {
            this.m_monitorOptions = await this.app.domain.digineous.getVibrationMonitorOptions();
            this.m_monitorOptions.sort((a,
                                   b) => UtilsService.compareStrings(a.label, b.label, true));
        }
    }
}
