import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output, SimpleChanges, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {TagConditionNodeComponent} from "app/shared/assets/tag-condition-builder/tag-condition-node.component";
import {ConditionNode, LogicNode} from "app/shared/assets/tag-condition-builder/tag-conditions";

@Component({
               selector       : "o3-tag-condition-builder",
               templateUrl    : "./tag-condition-builder.component.html",
               styleUrls      : ["./tag-condition-builder.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class TagConditionBuilderComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() model: Models.TagsCondition;
    @Output() modelChange: EventEmitter<Models.TagsCondition> = new EventEmitter<Models.TagsCondition>();
    @Output() validChange: EventEmitter<boolean>              = new EventEmitter<boolean>();

    @ViewChild("test_conditionNode") test_conditionNode: TagConditionNodeComponent;

    public root: ConditionNode | LogicNode;

    private ignoreNextChange = false;

    async ngOnInit()
    {
        super.ngOnInit();
        this.read();
    }

    ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);
        if (!this.ignoreNextChange) this.read();
        this.ignoreNextChange = false;
    }

    isCondition(): boolean
    {
        return this.root instanceof ConditionNode;
    }

    read()
    {
        if (ConditionNode.isConditionNode(this.model))
        {
            this.root = ConditionNode.fromModel(this.model);
        }
        else if (LogicNode.isLogicNode(this.model))
        {
            this.root = LogicNode.fromModel(this.model);
        }
        else
        {
            this.root = new ConditionNode();
        }

        this.validChange.emit(this.root.isValid());
        this.detectChanges();
    }

    write()
    {
        this.ignoreNextChange = true;

        let model  = this.root.toModel();
        this.model = model;
        this.modelChange.emit(model);
    }

    validate(state: boolean)
    {
        this.validChange.emit(state);
    }
}
