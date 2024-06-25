import {Component, ElementRef, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";
import {ITreeOptions, TreeComponent, TreeNode} from "@circlon/angular-tree-component";
import {BaseComponent} from "framework/ui/components";

import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-wizard-selection-tree",
               templateUrl: "./wizard-selection-tree.component.html"
           })
export class WizardSelectionTreeComponent extends BaseComponent
{
    private nativeElement: any;

    @ViewChild("treeView", {static: true}) tree: TreeComponent;

    treeOptions: ITreeOptions;

    private m_options: ControlOption<any>[] = [];

    @Input() set options(options: ControlOption<any>[])
    {
        let processedArray = [];
        if (options)
        {
            for (let v of options)
            {
                if (v) processedArray.push(v); // TreeView filter doesn't like nulls.
            }
        }

        this.m_options = processedArray;
        this.bind();
    }

    get options(): ControlOption<any>[]
    {
        return this.m_options;
    }

    private m_value: string[] = [];

    @Input() set value(value: string[])
    {
        this.m_value = value;
        this.bind();
    }

    get value(): string[]
    {
        return this.m_value;
    }

    @Output() valueChange: EventEmitter<string[]> = new EventEmitter<string[]>();

    constructor(inj: Injector,
                public elementRef: ElementRef)
    {
        super(inj);

        this.nativeElement = elementRef.nativeElement;

        this.treeOptions = {
            allowDrop    : false,
            allowDrag    : false,
            animateExpand: false,
            displayField : "label",
            nodeHeight   : 22
        };
    }

    private bind()
    {
        // deselect all
        if (this.tree.treeModel?.nodes?.length)
        {
            this.tree.treeModel.doForAll((node: TreeNode) =>
                                         {
                                             node.setIsActive(false);
                                             node.data.checked = false;
                                         });
        }

        if (this.value)
        {
            // make a local copy of the values
            let value = <string[]>JSON.parse(JSON.stringify(this.value));

            if (value && value.length)
            {
                // bind tree tiems
                for (let item of value) this.select(item);
            }
        }
    }

    private select(id: string)
    {
        setTimeout(() =>
                   {
                       if (id)
                       {
                           let selectedNode: TreeNode = this.tree.treeModel.getNodeById(id);
                           if (selectedNode)
                           {
                               selectedNode.setActiveAndVisible();
                               selectedNode.data.checked = true;
                           }
                       }
                   }, 50);
    }

    load(nodes: ControlOption<any>[],
         value: string[] = null)
    {
        this.options = nodes;
        if (value) this.value = value;
    }

    check(node: any,
          checked: boolean)
    {
        if (node)
        {
            this.performCheck(node, checked);

            this.valueChange.emit(this.value);
        }
    }

    private performCheck(node: any,
                         checked: boolean)
    {
        // update checked node state
        if (node.data && node.data.id)
        {
            node.data.checked = checked;

            if (!this.value) this.value = [];
            if (checked && this.value.indexOf(node.data.id) < 0)
            {
                this.value.push(node.data.id);
            }
            else if (!checked && this.value.indexOf(node.data.id) >= 0)
            {
                this.value.splice(this.value.indexOf(node.data.id), 1);
            }
        }

        // update child node state
        if (node.children)
        {
            node.children.forEach((child: any) =>
                                  {
                                      child.data.checked = checked;
                                      this.performCheck(child, checked);
                                  });
        }
    }
}
