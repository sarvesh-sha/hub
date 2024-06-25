import {Component, ContentChildren, EventEmitter, Input, Output, QueryList, ViewChild} from "@angular/core";
import {MatMenu} from "@angular/material/menu";

import {TabActionDirective} from "framework/ui/shared/tab-action.directive";

@Component({
               selector   : "o3-menu",
               templateUrl: "./menu.component.html"
           })
export class MenuComponent
{
    private m_hierarchicalOptions: HierarchicalMenuOption[] = [];
    get hierarchicalOptions(): HierarchicalMenuOption[]
    {
        return this.m_hierarchicalOptions;
    }

    private m_inputOptions: TabActionDirective[];
    @Input("options") set inputOptions(options: TabActionDirective[])
    {
        if (options)
        {
            this.m_inputOptions = options;
            this.updateOptions();
        }
    }

    private m_contentChildrenOptions: TabActionDirective[];
    @ContentChildren(TabActionDirective) set contentChildrenOptions(options: QueryList<TabActionDirective>)
    {
        if (options)
        {
            this.m_contentChildrenOptions = options.toArray();
            this.updateOptions();
        }
    }

    @ViewChild("menu",
               {
                   read  : MatMenu,
                   static: true
               }) menu: MatMenu;

    @Output() closed = new EventEmitter<void>();

    allChildrenDisabled(option: HierarchicalMenuOption): boolean
    {
        return option.subOptions.every((subOption) => subOption.option.disabled);
    }

    private updateOptions()
    {
        let beginningOptions: TabActionDirective[] = [];
        let endOptions: TabActionDirective[]       = [];
        const appendOption                         = (option: TabActionDirective) =>
        {
            if (option.toBeginning)
            {
                beginningOptions.push(option);
            }
            else
            {
                endOptions.push(option);
            }
        };

        for (let option of this.m_contentChildrenOptions || []) appendOption(option);
        for (let option of this.m_inputOptions || []) appendOption(option);

        this.m_hierarchicalOptions = HierarchicalMenuOption.fromTabActionDirectives(beginningOptions);
        HierarchicalMenuOption.fromTabActionDirectives(endOptions, this.m_hierarchicalOptions);
    }
}

class HierarchicalMenuOption
{
    public readonly subOptions: HierarchicalMenuOption[] = [];

    get tooltip(): string
    {
        if (this.option?.tooltip) return this.option.tooltip;

        for (let subOption of this.subOptions)
        {
            const subOptionTooltip = subOption.tooltip;
            if (subOptionTooltip) return subOptionTooltip;
        }

        return null;
    }

    constructor(public readonly label?: string,
                public readonly option?: TabActionDirective)
    {
    }

    public static ensureLevel(options: HierarchicalMenuOption[],
                              label: string): HierarchicalMenuOption
    {
        for (let option of options)
        {
            if (option.label == label) return option;
        }

        let newOption = new HierarchicalMenuOption(label);
        options.push(newOption);
        return newOption;
    }

    public static fromTabActionDirectives(options: TabActionDirective[],
                                          firstLevelOptions: HierarchicalMenuOption[] = []): HierarchicalMenuOption[]
    {
        for (let option of options || [])
        {
            if (option.labelFirstLevel)
            {
                let firstLevel = HierarchicalMenuOption.ensureLevel(firstLevelOptions, option.labelFirstLevel);
                if (option.labelSecondLevel)
                {
                    let secondLevel = HierarchicalMenuOption.ensureLevel(firstLevel.subOptions, option.labelSecondLevel);
                    secondLevel.subOptions.push(new HierarchicalMenuOption(undefined, option));
                }
                else
                {
                    firstLevel.subOptions.push(new HierarchicalMenuOption(undefined, option));
                }
            }
            else
            {
                firstLevelOptions.push(new HierarchicalMenuOption(undefined, option));
            }
        }

        return firstLevelOptions;
    }
}
