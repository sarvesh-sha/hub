import {Component, EventEmitter, Input, Output} from "@angular/core";
import * as Models from "app/services/proxy/model/models";
import {ConditionNode, ConditionNodeType, LogicNode} from "app/shared/assets/tag-condition-builder/tag-conditions";
import {DevicesFilterChip, EquipmentClassFilterChip, EquipmentFilterChip, IsClassifiedFilterChip, IsHiddenFilterChip, IsSamplingFilterChip, PointClassFilterChip} from "app/shared/filter/filter-chips";
import {FilterSerializable} from "app/shared/filter/filter-overlay.component";
import {LocationFiltersAdapter} from "app/shared/filter/filters-adapter";
import {UtilsService} from "framework/services/utils.service";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";
import {Memoizer} from "framework/utils/memoizers";

@Component({
               selector   : "o3-device-element-filters-adapter[request]",
               templateUrl: "./device-element-filters-adapter.component.html"
           })
export class DeviceElementFiltersAdapterComponent extends LocationFiltersAdapter<Models.DeviceElementFilterRequest>
{
    samplingOptions   = [...IsSamplingFilterChip.options];
    classifiedOptions = [...IsClassifiedFilterChip.options];
    visibilityOptions = [...IsHiddenFilterChip.options];

    samplingState: Models.FilterPreferenceBoolean;
    classifiedState: Models.FilterPreferenceBoolean;
    visibilityState: Models.FilterPreferenceBoolean;
    pointClassIDs: string[];
    equipmentClassIDs: string[];
    equipmentIDs: string[];


    @Input() isStep: boolean = false;

    @Input() set includeEquipment(includeEquipment: boolean)
    {
        this.m_includeEquipment = includeEquipment;
        if (this.initialized) this.emitNewChips();
    }

    get includeEquipment(): boolean
    {
        return this.m_includeEquipment;
    }

    private m_includeEquipment: boolean = false;

    @Input() set includeDevices(includeDevices: boolean)
    {
        this.m_includeDevices = includeDevices;
        if (this.initialized) this.emitNewChips();
    }

    get includeDevices(): boolean
    {
        return this.m_includeDevices;
    }

    private m_includeDevices: boolean = false;

    @Input() set includeVisibility(includeVisibility: boolean)
    {
        this.m_includeVisibility = includeVisibility;
        if (this.initialized) this.emitNewChips();
    }

    get includeVisibility(): boolean
    {
        return this.m_includeVisibility;
    }

    @Output() submitted = new EventEmitter<Models.DeviceElementFilterRequest>();

    private m_includeVisibility: boolean = false;

    protected resetEditModels()
    {
        this.pointClassIDs     = DeviceElementFiltersAdapterComponent.getClassIds(this.m_editRequest.tagsQuery, ConditionNodeType.POINT);
        this.equipmentClassIDs = DeviceElementFiltersAdapterComponent.getClassIds(this.m_editRequest.parentTagsQuery, ConditionNodeType.EQUIPMENT);
        this.equipmentIDs      = DeviceElementFiltersAdapterComponent.getClassIds(this.m_editRequest.parentTagsQuery, ConditionNodeType.ASSET);
        this.samplingState     = DeviceElementFiltersAdapterComponent.getSamplingInput(this.m_editRequest);
        this.classifiedState   = DeviceElementFiltersAdapterComponent.getClassifiedInput(this.m_editRequest);
        this.visibilityState   = DeviceElementFiltersAdapterComponent.getVisibilityInput(this.m_editRequest);
    }

    protected updateGlobalFilters()
    {
        super.updateGlobalFilters();

        this.filtersSvc.pointClassIDs = DeviceElementFiltersAdapterComponent.getClassIds(this.m_request.tagsQuery, ConditionNodeType.POINT);
        this.filtersSvc.sampling      = DeviceElementFiltersAdapterComponent.getSamplingInput(this.m_request);
        this.filtersSvc.classified    = DeviceElementFiltersAdapterComponent.getClassifiedInput(this.m_request);

        if (this.includeEquipment)
        {
            this.filtersSvc.equipmentClassIDs = DeviceElementFiltersAdapterComponent.getClassIds(this.m_request.parentTagsQuery, ConditionNodeType.EQUIPMENT);
            this.filtersSvc.equipmentIDs      = DeviceElementFiltersAdapterComponent.getClassIds(this.m_request.parentTagsQuery, ConditionNodeType.ASSET);
        }

        if (this.includeDevices)
        {
            this.filtersSvc.deviceIDs = this.m_request.parentIDs;
        }
    }

    protected syncWithGlobalFilters()
    {
        super.syncWithGlobalFilters();

        this.m_request.tagsQuery = DeviceElementFiltersAdapterComponent.generateChildQuery(this.filtersSvc.pointClassIDs, this.filtersSvc.classified);

        if (this.includeEquipment)
        {
            this.m_request.parentTagsQuery = DeviceElementFiltersAdapterComponent.generateParentQuery(this.filtersSvc.equipmentIDs, this.filtersSvc.equipmentClassIDs);
        }

        if (this.includeDevices)
        {
            this.m_request.parentIDs = this.filtersSvc.deviceIDs;
        }

        DeviceElementFiltersAdapterComponent.updateFiltersFromSamplingInput(this.m_request, this.filtersSvc.sampling);
    }

    protected emptyRequestInstance(): Models.DeviceElementFilterRequest
    {
        return new Models.DeviceElementFilterRequest();
    }

    protected newRequestInstance(request?: Models.DeviceElementFilterRequest): Models.DeviceElementFilterRequest
    {
        request = Models.DeviceElementFilterRequest.newInstance(request);

        if (request.hasAnySampling == undefined && request.hasNoSampling == undefined)
        {
            request.hasAnySampling = true;
            request.hasNoSampling  = false;
        }

        return request;
    }

    protected async appendChips(chips: FilterChip[]): Promise<void>
    {
        await super.appendChips(chips);

        chips.push(new PointClassFilterChip(
            () =>
            {
                this.resetEditRequest();
                this.m_editRequest.tagsQuery = DeviceElementFiltersAdapterComponent.generateChildQuery([], this.classifiedState);
                this.applyFilterEdits();
            },
            () => DeviceElementFiltersAdapterComponent.getClassIds(this.m_request.tagsQuery, ConditionNodeType.POINT),
            await this.app.bindings.getPointClasses(true, null)));

        chips.push(new IsSamplingFilterChip(
            () =>
            {
                this.resetEditRequest();
                DeviceElementFiltersAdapterComponent.updateFiltersFromSamplingInput(this.m_editRequest, null);
                this.applyFilterEdits();
            },
            () => DeviceElementFiltersAdapterComponent.getSamplingInput(this.m_request)));

        chips.push(new IsClassifiedFilterChip(
            () =>
            {
                this.resetEditRequest();
                this.m_editRequest.tagsQuery = DeviceElementFiltersAdapterComponent.generateChildQuery(this.pointClassIDs, null);
                this.applyFilterEdits();
            },
            () => DeviceElementFiltersAdapterComponent.getClassifiedInput(this.m_request)));

        if (this.includeDevices)
        {
            chips.push(new DevicesFilterChip(
                () =>
                {
                    this.resetEditRequest();
                    this.m_editRequest.parentIDs = [];
                    this.applyFilterEdits();
                },
                () => this.m_request.parentIDs, null));
        }

        if (this.includeEquipment)
        {
            chips.push(new EquipmentClassFilterChip(
                () =>
                {
                    this.resetEditRequest();
                    this.m_editRequest.parentTagsQuery = DeviceElementFiltersAdapterComponent.generateParentQuery(this.equipmentIDs, []);
                    this.applyFilterEdits();
                },
                () => DeviceElementFiltersAdapterComponent.getClassIds(this.m_request.parentTagsQuery, ConditionNodeType.EQUIPMENT),
                await this.app.bindings.getEquipmentClasses(true, null)));

            chips.push(new EquipmentFilterChip(() =>
                                               {
                                                   this.resetEditRequest();
                                                   this.m_editRequest.parentTagsQuery = DeviceElementFiltersAdapterComponent.generateParentQuery([], this.equipmentClassIDs);
                                                   this.applyFilterEdits();
                                               }, () => DeviceElementFiltersAdapterComponent.getClassIds(this.m_request.parentTagsQuery, ConditionNodeType.ASSET), null));
        }

        if (this.includeVisibility)
        {
            chips.push(new IsHiddenFilterChip(() =>
                                              {
                                                  this.resetEditRequest();
                                                  DeviceElementFiltersAdapterComponent.updateFiltersFromVisibilityInput(this.m_editRequest, null);
                                                  this.applyFilterEdits();
                                              }, () => DeviceElementFiltersAdapterComponent.getVisibilityInput(this.m_request)));
        }
    }

    protected areEquivalent(requestA: Models.DeviceElementFilterRequest,
                            requestB: Models.DeviceElementFilterRequest): boolean
    {
        if (!super.areEquivalent(requestA, requestB)) return false;

        if (!UtilsService.compareArraysAsSets(DeviceElementFiltersAdapterComponent.getClassIds(requestA.tagsQuery, ConditionNodeType.POINT),
                                              DeviceElementFiltersAdapterComponent.getClassIds(requestB.tagsQuery, ConditionNodeType.POINT)))
        {
            return false;
        }

        if (this.includeDevices)
        {
            if (!UtilsService.compareArraysAsSets(requestA.parentIDs, requestB.parentIDs))
            {
                return false;
            }
        }

        if (this.includeEquipment)
        {
            if (!UtilsService.compareArraysAsSets(DeviceElementFiltersAdapterComponent.getClassIds(requestA.parentTagsQuery, ConditionNodeType.EQUIPMENT),
                                                  DeviceElementFiltersAdapterComponent.getClassIds(requestB.parentTagsQuery, ConditionNodeType.EQUIPMENT)))
            {
                return false;
            }

            if (!UtilsService.compareArraysAsSets(DeviceElementFiltersAdapterComponent.getClassIds(requestA.parentTagsQuery, ConditionNodeType.ASSET),
                                                  DeviceElementFiltersAdapterComponent.getClassIds(requestB.parentTagsQuery, ConditionNodeType.ASSET)))
            {
                return false;
            }
        }

        if (DeviceElementFiltersAdapterComponent.getSamplingInput(requestA) !== DeviceElementFiltersAdapterComponent.getSamplingInput(requestB))
        {
            return false;
        }

        if (DeviceElementFiltersAdapterComponent.getClassifiedInput(requestA) !== DeviceElementFiltersAdapterComponent.getClassifiedInput(requestB))
        {
            return false;
        }

        if (this.includeVisibility)
        {
            if (DeviceElementFiltersAdapterComponent.getVisibilityInput(requestA) !== DeviceElementFiltersAdapterComponent.getVisibilityInput(requestB))
            {
                return false;
            }
        }

        return true;
    }

    public updateTagsQuery()
    {
        this.m_editRequest.tagsQuery = DeviceElementFiltersAdapterComponent.generateChildQuery(this.pointClassIDs, this.classifiedState);
        if (this.includeEquipment)
        {
            this.m_editRequest.parentTagsQuery = DeviceElementFiltersAdapterComponent.generateParentQuery(this.equipmentIDs, this.equipmentClassIDs);
        }
        this.updatePristine();
    }

    public updateSampling()
    {
        DeviceElementFiltersAdapterComponent.updateFiltersFromSamplingInput(this.m_editRequest, this.samplingState);
        this.updatePristine();
    }

    public updateVisibility()
    {
        DeviceElementFiltersAdapterComponent.updateFiltersFromVisibilityInput(this.m_editRequest, this.visibilityState);
        this.updatePristine();
    }

    public onSubmit()
    {
        this.submitted.emit(this.m_request);
    }

    @Memoizer
    public getSerializer(): FilterSerializable<Models.DeviceElementFilterRequest>
    {
        return {
            serializer  : (filter) => this.filterSerializer(filter),
            deserializer: (filter) => this.filterDeserializer(filter),
            getter      : () => this.filterGetter(),
            setter      : (filter) => this.filterSetter(filter)
        };
    }

    public filterGetter(): Models.DeviceElementFilterRequest
    {
        return this.m_editRequest;
    }

    public filterSetter(filter: Models.DeviceElementFilterRequest): void
    {
        this.m_editRequest = filter;
        this.applyFilterEdits();
    }

    public filterSerializer(filter: Models.DeviceElementFilterRequest): Models.FilterPreferences
    {
        let serialized = Models.FilterPreferences.newInstance({});
        this.serializeFilter(filter, serialized);

        return serialized;
    }

    public filterDeserializer(filter: Models.FilterPreferences): Models.DeviceElementFilterRequest
    {
        let deserialized = Models.DeviceElementFilterRequest.newInstance({});
        this.deserializeFilter(filter, deserialized);

        return deserialized;
    }

    protected serializeFilter(input: Models.DeviceElementFilterRequest,
                              output: Models.FilterPreferences)
    {
        // Call parent serializer
        super.serializeFilter(input, output);

        // Serialize all relevant data
        output.pointClassIDs = DeviceElementFiltersAdapterComponent.getClassIds(input.tagsQuery, ConditionNodeType.POINT);
        output.isSampling    = DeviceElementFiltersAdapterComponent.getSamplingInput(input);
        output.isClassified  = DeviceElementFiltersAdapterComponent.getClassifiedInput(input);

        if (this.includeEquipment)
        {
            output.equipmentClassIDs = DeviceElementFiltersAdapterComponent.getClassIds(input.parentTagsQuery, ConditionNodeType.EQUIPMENT);
            output.equipmentIDs      = DeviceElementFiltersAdapterComponent.getClassIds(input.parentTagsQuery, ConditionNodeType.ASSET);
        }

        if (this.includeDevices)
        {
            output.deviceIDs = input.parentIDs;
        }
    }

    protected deserializeFilter(input: Models.FilterPreferences,
                                output: Models.DeviceElementFilterRequest)
    {
        // Call parent deserializer
        super.deserializeFilter(input, output);

        // Deserialize all relevant data
        output.tagsQuery = DeviceElementFiltersAdapterComponent.generateChildQuery(input.pointClassIDs, input.isClassified);

        if (this.includeEquipment)
        {
            output.parentTagsQuery = DeviceElementFiltersAdapterComponent.generateParentQuery(input.equipmentIDs, input.equipmentClassIDs);
        }

        if (this.includeDevices)
        {
            output.parentIDs = input.deviceIDs;
        }

        DeviceElementFiltersAdapterComponent.updateFiltersFromSamplingInput(output, input.isSampling);
    }

    public static generateChildQuery(pointClassIDs: string[],
                                     classified: Models.FilterPreferenceBoolean)
    {
        let pointClassCondition = this.generateQuery(pointClassIDs, ConditionNodeType.POINT);
        let classifiedCondition = this.generateClassifiedQuery(classified);
        if (pointClassCondition && classifiedCondition)
        {
            return Models.TagsConditionBinaryLogic.newInstance({
                                                                   op: Models.TagsConditionOperator.And,
                                                                   a : pointClassCondition,
                                                                   b : classifiedCondition
                                                               });
        }
        return pointClassCondition || classifiedCondition;
    }

    public static generateParentQuery(equipmentIDs: string[],
                                      equipmentClassIDs: string[]): Models.TagsCondition
    {
        let equipmentClassCondition = this.generateQuery(equipmentClassIDs, ConditionNodeType.EQUIPMENT);
        let equipmentCondition      = this.generateQuery(equipmentIDs, ConditionNodeType.ASSET);
        if (equipmentClassCondition && equipmentCondition)
        {
            return Models.TagsConditionBinaryLogic.newInstance({
                                                                   op: Models.TagsConditionOperator.And,
                                                                   a : equipmentCondition,
                                                                   b : equipmentClassCondition
                                                               });
        }
        return equipmentClassCondition || equipmentCondition;
    }

    public static generateQuery(classIDs: string[],
                                type: ConditionNodeType): Models.TagsCondition
    {
        let nodes: ConditionNode[] = [];

        for (let classId of classIDs || [])
        {
            let node   = new ConditionNode(type, false);
            node.value = classId;
            nodes.push(node);
        }

        switch (nodes.length)
        {
            case 0:
                return null;

            case 1:
                return nodes[0].toModel();

            default:
                let parentNode = new LogicNode(Models.TagsConditionOperator.Or);
                parentNode.children.push(...nodes);
                return parentNode.toModel();
        }
    }

    public static getClassIds(parentNode: Models.TagsCondition,
                              type: ConditionNodeType): string[]
    {
        if (!parentNode) return null;

        let conditionNode = ConditionNode.fromModel(parentNode);
        if (conditionNode && conditionNode.type === type)
        {
            return [conditionNode.value];
        }
        else if (conditionNode)
        {
            return [];
        }

        let logicNode          = LogicNode.fromModel(parentNode);
        let classIds: string[] = [];
        for (let child of logicNode.children)
        {
            let childIds = DeviceElementFiltersAdapterComponent.getClassIds(child.toModel(), type);
            if (childIds?.length) classIds.push(...childIds);
        }

        return classIds;
    }

    public static updateFiltersFromSamplingInput(filters: Models.DeviceElementFilterRequest,
                                                 sampling: Models.FilterPreferenceBoolean)
    {
        switch (sampling)
        {
            case Models.FilterPreferenceBoolean.Yes:
                filters.hasAnySampling = true;
                filters.hasNoSampling  = false;
                break;

            case Models.FilterPreferenceBoolean.No:
                filters.hasAnySampling = false;
                filters.hasNoSampling  = true;
                break;

            default:
                filters.hasAnySampling = false;
                filters.hasNoSampling  = false;
                break;
        }
    }

    public static getSamplingInput(filters: Models.DeviceElementFilterRequest): Models.FilterPreferenceBoolean
    {
        if (filters.hasAnySampling && !filters.hasNoSampling)
        {
            return Models.FilterPreferenceBoolean.Yes;
        }
        else if (!filters.hasAnySampling && filters.hasNoSampling)
        {
            return Models.FilterPreferenceBoolean.No;
        }

        return null;
    }

    public static updateFiltersFromVisibilityInput(filters: Models.DeviceElementFilterRequest,
                                                   hidden: Models.FilterPreferenceBoolean)
    {
        switch (hidden)
        {
            case Models.FilterPreferenceBoolean.Yes:
                filters.isHidden    = true;
                filters.isNotHidden = false;
                break;

            case Models.FilterPreferenceBoolean.No:
                filters.isNotHidden = true;
                filters.isHidden    = false;
                break;

            default:
                filters.isHidden    = undefined;
                filters.isNotHidden = undefined;
                break;
        }
    }

    public static getVisibilityInput(filters: Models.DeviceElementFilterRequest): Models.FilterPreferenceBoolean
    {
        if (filters.isHidden)
        {
            return Models.FilterPreferenceBoolean.Yes;
        }
        else if (filters.isNotHidden)
        {
            return Models.FilterPreferenceBoolean.No;
        }

        return null;
    }

    public static getClassifiedInput(filters: Models.DeviceElementFilterRequest): Models.FilterPreferenceBoolean
    {
        return this.getClassifiedInputFromNode(filters.tagsQuery);
    }

    private static getClassifiedInputFromNode(parentNode: Models.TagsCondition): Models.FilterPreferenceBoolean
    {
        if (!parentNode) return null;

        let conditionNode = ConditionNode.fromModel(parentNode);
        if (conditionNode && conditionNode.type === ConditionNodeType.CLASSIFIED)
        {
            return conditionNode.negate ? Models.FilterPreferenceBoolean.No : Models.FilterPreferenceBoolean.Yes;
        }
        else if (conditionNode)
        {
            return null;
        }

        let logicNode = LogicNode.fromModel(parentNode);
        for (let child of logicNode.children)
        {
            let state = DeviceElementFiltersAdapterComponent.getClassifiedInputFromNode(child.toModel());
            if (state) return state;
        }

        return null;
    }

    public static generateClassifiedQuery(classified: Models.FilterPreferenceBoolean): Models.TagsCondition
    {
        if (classified)
        {
            let node = new ConditionNode(ConditionNodeType.CLASSIFIED, classified === Models.FilterPreferenceBoolean.No);
            return node.toModel();
        }

        return null;
    }
}
