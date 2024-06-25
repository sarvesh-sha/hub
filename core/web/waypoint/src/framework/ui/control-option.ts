import {SafeHtml} from "@angular/platform-browser";
import {ITreeNode, ILazyTreeNode} from "framework/ui/dropdowns/filterable-tree.component";

export class ControlOption<T> implements ITreeNode<T>
{
    id: T;
    label: string;
    safeLabel?: SafeHtml;
    isDivider: boolean;
    lazyChildren: ILazyTreeNode<T>[];

    constructor(id?: T,
                label?: string,
                public children: ControlOption<T>[] = [],
                public disableSelection: boolean    = false)
    {
        this.label = label;
        this.id    = id;
    }

    set hasChildren(hasChildren: boolean)
    {
        this.m_hasChildren = hasChildren;
    }

    get hasChildren(): boolean
    {
        return this.children?.length > 0 || this.m_hasChildren;
    }

    private m_hasChildren: boolean;

    static getLabels<T>(ids: T[],
                        options: ITreeNode<T>[]): string
    {
        let text: string = "";
        if (ids && ids.length > 0)
        {
            // compose string of selected filter items
            for (let id of ids)
            {
                let itemText = this.getLabel(id, options);
                if (itemText) text += itemText + ", ";
            }

            // remove last comma
            if (text && text.length)
            {
                text = text.substring(0, text.length - 2);
            }
        }
        return text;
    }

    static getLabel<T>(id: T,
                       options: ITreeNode<T>[],
                       withHierarchicalContext: boolean = false): string
    {
        if (id && options && options.length)
        {
            for (let option of options)
            {
                if (option.id == id) return option.label;

                if (option.children && option.children.length)
                {
                    let candidateName = ControlOption.getLabel(id, option.children, withHierarchicalContext);
                    if (candidateName) return withHierarchicalContext ? option.label + " -- " + candidateName : candidateName;
                }
            }
        }

        return null;
    }

    static getDivider<T>(label?: string): ControlOption<T>
    {
        let newOption       = new ControlOption(null, label || "---------------------");
        newOption.isDivider = true;
        return newOption;
    }
}
