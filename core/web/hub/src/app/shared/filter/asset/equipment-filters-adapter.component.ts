import {Component} from "@angular/core";
import * as Models from "app/services/proxy/model/models";
import {ConditionNode, ConditionNodeType, LogicNode} from "app/shared/assets/tag-condition-builder/tag-conditions";
import {EquipmentClassFilterChip} from "app/shared/filter/filter-chips";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";
import {LocationFiltersAdapter} from "app/shared/filter/filters-adapter";
import {UtilsService} from "framework/services/utils.service";

@Component({
               selector   : "o3-equipment-filters-adapter[request]",
               templateUrl: "./equipment-filters-adapter.component.html"
           })
export class EquipmentFiltersAdapterComponent extends LocationFiltersAdapter<Models.AssetFilterRequest>
{
    equipmentClassIDs: string[];

    protected resetEditModels()
    {
        this.equipmentClassIDs = EquipmentFiltersAdapterComponent.getEquipmentClassIDs(this.m_editRequest.tagsQuery);
    }

    protected updateGlobalFilters()
    {
        super.updateGlobalFilters();

        this.filtersSvc.equipmentClassIDs = EquipmentFiltersAdapterComponent.getEquipmentClassIDs((this.m_request.tagsQuery));
    }

    protected syncWithGlobalFilters()
    {
        super.syncWithGlobalFilters();

        this.m_request.tagsQuery = EquipmentFiltersAdapterComponent.generateEquipmentClassIDsQuery(this.filtersSvc.equipmentClassIDs);
    }

    protected emptyRequestInstance(): Models.AssetFilterRequest
    {
        return new Models.AssetFilterRequest();
    }

    protected newRequestInstance(request?: Models.AssetFilterRequest): Models.AssetFilterRequest
    {
        return Models.AssetFilterRequest.newInstance(request);
    }

    protected async appendChips(chips: FilterChip[]): Promise<void>
    {
        await super.appendChips(chips);

        chips.push(new EquipmentClassFilterChip(() =>
                                                {
                                                    this.resetEditRequest();
                                                    this.m_editRequest.tagsQuery = EquipmentFiltersAdapterComponent.generateEquipmentClassIDsQuery([]);
                                                    this.applyFilterEdits();
                                                },
                                                () => EquipmentFiltersAdapterComponent.getEquipmentClassIDs(this.m_request.tagsQuery),
                                                await this.app.bindings.getEquipmentClasses(true, null)));
    }

    protected areEquivalent(requestA: Models.DeviceElementFilterRequest,
                            requestB: Models.DeviceElementFilterRequest): boolean
    {
        if (!super.areEquivalent(requestA, requestB)) return false;

        return UtilsService.compareArraysAsSets(EquipmentFiltersAdapterComponent.getEquipmentClassIDs(requestA.tagsQuery),
                                                EquipmentFiltersAdapterComponent.getEquipmentClassIDs(requestB.tagsQuery));
    }

    updateTagsQuery()
    {
        this.m_editRequest.tagsQuery = EquipmentFiltersAdapterComponent.generateEquipmentClassIDsQuery(this.equipmentClassIDs);
        this.updatePristine();
    }

    static generateEquipmentClassIDsQuery(equipmentClassIDs: string[]): Models.TagsCondition
    {
        let nodes: ConditionNode[] = [];

        for (let equipmentClassId of equipmentClassIDs || [])
        {
            let node   = new ConditionNode(ConditionNodeType.EQUIPMENT, false);
            node.value = equipmentClassId;
            nodes.push(node);
        }

        let isEquipment = new Models.TagsConditionIsEquipment();
        let childNode: Models.TagsCondition;

        switch (nodes.length)
        {
            case 0:
                childNode = null;
                break;

            case 1:
                childNode = nodes[0].toModel();
                break;

            default:
                let parentNode = new LogicNode(Models.TagsConditionOperator.Or);
                parentNode.children.push(...nodes);
                childNode = parentNode.toModel();
        }

        if (childNode)
        {
            return Models.TagsConditionBinaryLogic.newInstance({
                                                                   a : isEquipment,
                                                                   b : childNode,
                                                                   op: Models.TagsConditionOperator.And
                                                               });
        }

        return isEquipment;
    }

    static getEquipmentClassIDs(parentNode: Models.TagsCondition): string[]
    {
        if (!parentNode) return null;
        if (!(parentNode instanceof Models.TagsConditionBinaryLogic)) return [];

        let conditionNode = ConditionNode.fromModel(parentNode.b);
        if (conditionNode) return [conditionNode.value];

        let logicNode         = LogicNode.fromModel(parentNode.b);
        let conditionChildren = <ConditionNode[]>logicNode.children.filter(
            (child) => child instanceof ConditionNode && child.type === ConditionNodeType.EQUIPMENT);
        return conditionChildren.map((condition) => condition.value)
                                .filter((value) => !!value);
    }
}
