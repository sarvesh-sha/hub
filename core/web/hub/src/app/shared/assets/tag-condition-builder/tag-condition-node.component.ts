import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, QueryList, ViewChild, ViewChildren} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {ConditionNode, ConditionNodeOptions, ConditionNodeType, LogicNode} from "app/shared/assets/tag-condition-builder/tag-conditions";
import {SelectComponent} from "framework/ui/forms/select.component";

@Component({
               selector       : "o3-tag-condition-node",
               templateUrl    : "./tag-condition-node.component.html",
               styleUrls      : ["./tag-condition-node.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class TagConditionNodeComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() model: LogicNode | ConditionNode;

    @ViewChild("test_split", {read: ElementRef}) test_split: ElementRef;
    @ViewChild("test_type") test_type: SelectComponent<string>;

    private m_conditionSelector: SelectComponent<string>;
    @ViewChild("conditionSelector")
    public set conditionSelector(condition: SelectComponent<string>)
    {
        this.m_conditionSelector = condition;
        if (condition && this.isCondition() && !this.asCondition.value)
        {
            this.m_conditionSelector.toggleDropdown();
        }
    }

    public get conditionSelector(): SelectComponent<string>
    {
        return this.m_conditionSelector;
    }

    @ViewChild("test_logic", {read: ElementRef}) test_logic: ElementRef;
    @ViewChildren("test_childNode") test_childNodes: QueryList<TagConditionNodeComponent>;

    @Output() modelChange: EventEmitter<LogicNode | ConditionNode> = new EventEmitter<LogicNode | ConditionNode>();
    @Output() validChange: EventEmitter<boolean>                   = new EventEmitter<boolean>();

    public readonly conditionTypeOptions = ConditionNodeOptions.filter((cond) =>
                                                                       {
                                                                           switch (cond.type)
                                                                           {
                                                                               case ConditionNodeType.ASSET:
                                                                               case ConditionNodeType.CLASSIFIED:
                                                                                   // Don't show in the dropdown.
                                                                                   return false;

                                                                               default:
                                                                                   return true;
                                                                           }
                                                                       });

    private childrenValid: boolean = true;

    constructor(inj: Injector)
    {
        super(inj);
    }

    get asLogic(): LogicNode
    {
        return <LogicNode>this.model;
    }

    get asCondition(): ConditionNode
    {
        return <ConditionNode>this.model;
    }

    isLogic()
    {
        return this.model instanceof LogicNode;
    }

    isCondition()
    {
        return this.model instanceof ConditionNode;
    }

    private isConditionMatch(type: ConditionNodeType): boolean
    {
        return this.isCondition() && this.asCondition.type === type;
    }

    isAnythingCondition(): boolean
    {
        return this.isConditionMatch(ConditionNodeType.ANYTHING);
    }

    isEquipmentClassCondition(): boolean
    {
        return this.isConditionMatch(ConditionNodeType.EQUIPMENT);
    }

    isPointClassCondition(): boolean
    {
        return this.isConditionMatch(ConditionNodeType.POINT);
    }

    isTagCondition(): boolean
    {
        return this.isConditionMatch(ConditionNodeType.TAG);
    }

    isLocationCondition(): boolean
    {
        return this.isConditionMatch(ConditionNodeType.LOCATION);
    }

    isMetricsCondition(): boolean
    {
        return this.isConditionMatch(ConditionNodeType.METRICS);
    }

    isMetricsOutputCondition(): boolean
    {
        return this.isConditionMatch(ConditionNodeType.METRICSOUTPUT);
    }

    isAssetCondition(): boolean
    {
        return this.isConditionMatch(ConditionNodeType.ASSET);
    }

    isValid(): boolean
    {
        return this.childrenValid && this.model.isValid();
    }

    split()
    {
        if (this.isCondition())
        {
            let original = this.asCondition;
            let logic    = new LogicNode();
            let extra    = new ConditionNode(ConditionNodeType.ANYTHING);

            // Add old and extra child to a new logic node
            logic.children.push(original, extra);

            // Swap condition node for new logic node
            this.model = logic;

            // Trigger model update
            this.write();
        }
    }

    addChild()
    {
        if (this.isLogic())
        {
            // Add a new child node
            this.asLogic.children.push(new ConditionNode());

            // Trigger model update
            this.write();
        }
    }

    removeChild(index: number)
    {
        if (this.isLogic())
        {
            // Remove the child
            this.asLogic.children.splice(index, 1);

            // If only one left, convert to just a condition node
            if (this.asLogic.children.length <= 1) this.model = this.asLogic.children[0];

            // Trigger model update
            this.write();
        }
    }

    validateChildren()
    {
        this.childrenValid = true;

        if (this.model instanceof LogicNode)
        {
            for (let child of this.asLogic.children)
            {
                if (!child.isValid()) this.childrenValid = false;
            }
        }

        this.detectChanges();
    }

    write()
    {
        this.validateChildren();
        this.validChange.emit(this.isValid());
        this.modelChange.emit(this.model);
        this.detectChanges();
    }
}
