import {ChangeDetectionStrategy, Component, Input, ViewChild} from "@angular/core";
import {NormalizationRulesBlocklyBlocks} from "app/customer/configuration/classification/normalization-rules-blockly-blocks";
import {BlocklyWorkspaceData} from "app/customer/engines/shared/workspace-data";
import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {NormalizationDefinitionDetailsExtended} from "app/services/domain/normalization.service";
import * as Models from "app/services/proxy/model/models";
import {Block} from "framework/ui/blockly/block";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector       : "o3-logic-diff",
               templateUrl    : "./logic-diff.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class LogicDiffComponent extends BaseApplicationComponent
{
    blocks = NormalizationRulesBlocklyBlocks;

    @Input()
    public rulesA: Models.NormalizationRules;

    @Input()
    public titleA: string;

    @Input()
    public rulesB: Models.NormalizationRules;

    @Input()
    public titleB: string;

    @ViewChild("blocklyWorkspaceA")
    public workspaceA: AppBlocklyWorkspaceComponent;

    @ViewChild("blocklyWorkspaceB")
    public workspaceB: AppBlocklyWorkspaceComponent;

    public currentDetailsA: NormalizationDefinitionDetailsExtended;

    public currentDetailsB: NormalizationDefinitionDetailsExtended;

    public selectedFunction: string;

    public readonly highlightsA = new Set<string>();
    public readonly highlightsB = new Set<string>();

    public functions: ControlOption<string>[] = [];

    ngOnInit()
    {
        let cloneA = Models.NormalizationDefinitionDetails.deepClone(this.rulesA.logic);
        let cloneB = Models.NormalizationDefinitionDetails.deepClone(this.rulesB.logic);

        this.currentDetailsA = NormalizationDefinitionDetailsExtended.newInstance(this.app.domain, cloneA, this.rulesA);
        this.currentDetailsB = NormalizationDefinitionDetailsExtended.newInstance(this.app.domain, cloneB, this.rulesB);

        const functionsA = this.collectFunctions(this.currentDetailsA.data);
        const functionsB = this.collectFunctions(this.currentDetailsB.data);

        const controlOptionShared = new ControlOption<string>("shared", "Shared", [], true);
        const controlOptionA      = new ControlOption<string>("a", this.titleA, [], true);
        const controlOptionB      = new ControlOption<string>("b", this.titleB, [], true);

        for (let [name, id] of functionsA.entries())
        {
            if (functionsB.has(name))
            {
                const idB = functionsB.get(name);
                controlOptionShared.children.push(new ControlOption<string>(`${id}::${idB}`, name));
            }
            else
            {
                controlOptionA.children.push(new ControlOption<string>(`${id}::`, name));
            }
        }

        for (let [name, id] of functionsB.entries())
        {
            if (!functionsA.has(name))
            {
                controlOptionB.children.push(new ControlOption<string>(`::${id}`, name));
            }
        }

        this.functions = [
            controlOptionShared,
            controlOptionA,
            controlOptionB
        ];
    }

    public functionChange()
    {
        if (this.selectedFunction)
        {
            let [idA, idB] = this.selectedFunction.split("::");
            if (idA)
            {
                this.workspaceA.centerOnBlock(idA);
            }

            if (idB)
            {
                this.workspaceB.centerOnBlock(idB);
            }

            if (idA && idB)
            {
                this.highlightsA.clear();
                this.highlightsB.clear();
                this.compareBlocks(this.workspaceA.getBlockById(idA), this.workspaceB.getBlockById(idB), false);
                this.workspaceA.applyHighlights(this.highlightsA);
                this.workspaceB.applyHighlights(this.highlightsB);
            }
        }
    }

    private compareBlocks(a: Block<any>,
                          b: Block<any>,
                          highlightChildren: boolean)
    {
        if (a == b) return;

        if (!a)
        {
            this.highlight(b, this.highlightsB, highlightChildren);
            return;
        }

        if (!b)
        {
            this.highlight(a, this.highlightsA, highlightChildren);
            return;
        }

        if (Object.getPrototypeOf(a) !== Object.getPrototypeOf(b))
        {
            this.highlight(a, this.highlightsA, highlightChildren);
            this.highlight(b, this.highlightsB, highlightChildren);
        }
        else
        {
            let modelA = a.toModel();
            let modelB = b.toModel();

            let keysA = new Set<string>(Object.keys(modelA));
            let keysB = new Set<string>(Object.keys(modelB));

            this.compareBlockProperties(a, modelA, keysA, b, modelB, keysB, highlightChildren);
            this.compareBlockProperties(b, modelB, keysB, a, modelA, keysA, highlightChildren);

            let childrenA = this.getChildBlocks(a);
            let childrenB = this.getChildBlocks(b);

            for (let i = 0; i < Math.max(childrenA.length, childrenB.length); i++)
            {
                this.compareBlocks(childrenA[i], childrenB[i], true);
            }
        }
    }

    private compareBlockProperties(a: Block<any>,
                                   modelA: any,
                                   keysA: Set<string>,
                                   b: Block<any>,
                                   modelB: any,
                                   keysB: Set<string>,
                                   includeChildren: boolean)
    {
        for (let key of keysA)
        {
            switch (key)
            {
                case "id":
                case "x":
                case "y":
                    continue;
            }

            if (typeof modelA[key] === "object") continue;
            if (!keysB.has(key) || modelA[key] != modelB[key])
            {
                this.highlight(a, this.highlightsA, includeChildren);
                this.highlight(b, this.highlightsB, includeChildren);
                break;
            }
        }
    }

    private collectFunctions(data: BlocklyWorkspaceData): Map<string, string>
    {
        const functions = new Map<string, string>();
        data.enumerateAllBlocks((b) =>
                                {
                                    if (b instanceof Models.EngineThread)
                                    {
                                        functions.set("Start", b.id);
                                    }
                                    else if (b instanceof Models.EngineProcedureDeclaration)
                                    {
                                        functions.set(b.name, b.id);
                                    }
                                });

        return functions;
    }

    private getChildBlocks(block: Block<any>): Block<any>[]
    {
        let children = block.getChildren(true) || [];
        let next     = block.getNextBlock();
        children     = children.filter((b) => b !== next);

        let result: Block<any>[] = [];
        for (let child of children)
        {
            result.push(Block.cast(child));
            let next = child.getNextBlock();
            while (next)
            {
                result.push(Block.cast(next));
                next = next.getNextBlock();
            }
        }

        return result;
    }

    private highlight(block: Block<any>,
                      highlights: Set<string>,
                      includeChildren: boolean)
    {
        highlights.add(block.id);
        if (includeChildren)
        {
            let children = this.getChildBlocks(block);
            for (let child of children)
            {
                this.highlight(child, highlights, includeChildren);
            }
        }
    }
}
