import {Directive, Input} from "@angular/core";

import {AppContext} from "app/app.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";

import {ControlOption} from "framework/ui/control-option";
import {ILazyLoader, ILazyTreeNode, ITreeNode} from "framework/ui/dropdowns/filterable-tree.component";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3DigineousMachineTemplateSelector]"
           })
export class DigineousMachineTemplateSelectorDirective implements ILazyLoader<string>
{
    @Input("o3DigineousMachineTemplateSelector")
    public sink: boolean;

    private m_templates: Models.DigineousMachineLibrary[];

    constructor(private app: AppContext,
                private selectComponent: SelectComponent<string>)
    {
    }

    async ngOnInit()
    {
        if (this.selectComponent.placeholder === undefined) this.selectComponent.placeholder = "Template";
        if (this.selectComponent.defaultValueDescription === "Select Option") this.selectComponent.defaultValueDescription = "No Template Specified";

        this.selectComponent.multiSelect = false;
        this.selectComponent.singleClick = true;
        this.selectComponent.lazyLoader  = this;
    }

    public async getTree(): Promise<ILazyTreeNode<string>[]>
    {
        await this.ensureLoaded();

        let tree = [];

        for (let template of this.m_templates)
        {
            tree.push({
                          id      : template.id,
                          children: []
                      });
        }

        return tree;
    }

    public async loadNodes(nodes: ILazyTreeNode<string>[]): Promise<ITreeNode<string>[]>
    {
        let res: ITreeNode<string>[] = [];

        for (let node of nodes)
        {
            let template = this.m_templates.find((template) => template.id == node.id);
            if (template)
            {
                res.push(new ControlOption<string>(node.id, template.name, null));
            }
        }

        return res;
    }

    public async getLabel(selected: string): Promise<string>
    {
        await this.ensureLoaded();

        let template = this.m_templates.find((template) => template.id == selected);
        return template?.name;
    }

    private async ensureLoaded()
    {
        if (!this.m_templates)
        {
            this.m_templates = await this.app.domain.digineous.getMachineTemplates();
            this.m_templates.sort((a,
                                   b) => UtilsService.compareStrings(a.name, b.name, true));
        }
    }
}
