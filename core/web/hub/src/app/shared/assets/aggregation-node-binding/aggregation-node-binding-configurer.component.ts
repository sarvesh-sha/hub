import {CdkDragDrop, moveItemInArray} from "@angular/cdk/drag-drop";
import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, Output, QueryList, ViewChild, ViewChildren} from "@angular/core";
import {AbstractControl, NgForm} from "@angular/forms";
import {UUID} from "angular2-uuid";

import {AssetGraphExtended} from "app/services/domain/asset-graph.service";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {EngineeringUnitsDescriptorExtended, UnitsService} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";

import {UtilsService} from "framework/services/utils.service";
import {ChartColorUtilities, ColorSegmentInterpolationMode} from "framework/ui/charting/core/colors";
import {ControlOption} from "framework/ui/control-option";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {SelectComponent} from "framework/ui/forms/select.component";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {ModifiableTableComponent} from "framework/ui/shared/modifiable-table.component";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector       : "o3-aggregation-node-binding-configurer",
               templateUrl    : "./aggregation-node-binding-configurer.component.html",
               styleUrls      : ["./aggregation-node-binding-configurer.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AggregationNodeBindingConfigurerComponent extends BaseApplicationComponent
{
    static ngAcceptInputType_configurableInitialSort: boolean | "";

    public static readonly FIRST_COLUMN_OPTION: string      = "Contexts";
    private static readonly COL_BINDING_NAME_PREFIX: string = "col-name-";

    get colNamePrefix(): string
    {
        return AggregationNodeBindingConfigurerComponent.COL_BINDING_NAME_PREFIX;
    }

    graphExt: AssetGraphExtended;
    nodeExts: AggregationNodeBindingExtended[] = [];

    AggregationNodeBindingExtended = AggregationNodeBindingExtended;

    isUniqueFn = (control: AbstractControl) => this.isUniqueBindingName(control);

    private get colNameFormControls(): AbstractControl[]
    {
        let controls = [];
        for (let name in this.form.controls)
        {
            if (name.startsWith(this.colNamePrefix)) controls.push(this.form.controls[name]);
        }

        return controls;
    }

    sortColumnOptions: ControlOption<string>[]                             = [AggregationNodeBindingConfigurerComponent.firstSortOption()];
    contextDisplayOptions: ControlOption<Models.ControlPointDisplayType>[] = [];

    displayOverlayConfig: OverlayConfig = OverlayConfig.onTopDraggable({width: 500});
    readonly colorInterpolation         = ColorSegmentInterpolationMode.STEP;
    mappingSegments: Models.ColorSegment[];

    editNodeExt: AggregationNodeBindingExtended;

    @Input() nodeName: string                                  = "Column";
    @Input() @CoerceBoolean() configurableInitialSort: boolean = false;

    @Output() displayTypeChange = new EventEmitter<Models.ControlPointDisplayType>();
    @Input() displayType        = Models.ControlPointDisplayType.NameOnly;

    private m_graph: Models.AssetGraph;
    @Input() set graph(graph: Models.AssetGraph)
    {
        this.m_graph = graph;
        if (!this.graphExt || !UtilsService.compareJson(this.graphExt.model, this.m_graph))
        {
            this.graphExt = new AssetGraphExtended(this.app.domain, this.m_graph);
            this.updateColExts();
        }
    }

    @Output() nodesChange = new EventEmitter<Models.AggregationNodeBinding[]>();
    private m_nodes: Models.AggregationNodeBinding[];
    @Input() set nodes(nodes: Models.AggregationNodeBinding[])
    {
        if (nodes) this.m_nodes = nodes;
        if (!UtilsService.compareArraysAsSets(this.nodeExts.map((colExt) => colExt.model), nodes)) this.updateColExts();
    }

    @Output() initialSortChange               = new EventEmitter<Models.SortCriteria>();
    @Input() initialSort: Models.SortCriteria = Models.SortCriteria.newInstance({ascending: true});

    get initiallyAscendingSort(): boolean
    {
        return this.initialSort.ascending;
    }

    set initiallyAscendingSort(ascending: boolean)
    {
        this.initialSort.ascending = ascending;
        this.initialSortChanged();
    }

    get initialSortColumn(): string
    {
        return this.nodeExts.find((nodeExt) => nodeExt.model.name === this.initialSort.column)?.id ||
               AggregationNodeBindingConfigurerComponent.FIRST_COLUMN_OPTION;
    }

    set initialSortColumn(col: string)
    {
        this.initialSort.column = this.nodeExts.find((nodeExt) => nodeExt.id === col)?.model.name;
        this.initialSortChanged();
    }

    @Output() visualizationModeChange                                = new EventEmitter<Models.HierarchicalVisualizationType>();
    @Input() visualizationMode: Models.HierarchicalVisualizationType = Models.HierarchicalVisualizationType.TABLE;

    get barMode(): boolean
    {
        return this.visualizationMode === Models.HierarchicalVisualizationType.TABLE_WITH_BAR;
    }

    set barMode(bar: boolean)
    {
        this.visualizationMode = bar ? Models.HierarchicalVisualizationType.TABLE_WITH_BAR : Models.HierarchicalVisualizationType.TABLE;
        this.visualizationModeChange.emit(this.visualizationMode);
    }

    @ViewChild(OverlayComponent, {static: true}) displayConfigurer: OverlayComponent;
    @ViewChild(NgForm, {static: true}) form: NgForm;

    @ViewChild("test_table") test_table: ModifiableTableComponent<AggregationNodeBindingExtended>;
    @ViewChildren("test_binding") test_bindings: QueryList<SelectComponent<string>>;
    @ViewChildren("test_name", {read: ElementRef}) test_names: QueryList<ElementRef>;
    @ViewChildren("test_aggType", {read: ElementRef}) test_aggTypes: QueryList<ElementRef>;

    public async ngOnInit()
    {
        await super.ngOnInit();

        let displayTypes           = await this.app.domain.enums.getInfos("ControlPointDisplayType", false);
        this.contextDisplayOptions = displayTypes.filter((descriptor) => (<string>descriptor.id).indexOf("Equipment") === -1)
                                                 .map((descriptor) => new ControlOption(<Models.ControlPointDisplayType>descriptor.id, descriptor.displayName));
    }

    private initialSortChanged()
    {
        this.initialSort = Models.SortCriteria.newInstance(this.initialSort);
        this.initialSortChange.emit(this.initialSort);
    }

    private async updateColExts()
    {
        if (this.m_nodes && this.graphExt)
        {
            this.nodeExts = await mapInParallel(this.m_nodes, async (node) =>
            {
                let colExt        = new AggregationNodeBindingExtended(this, node, null);
                colExt.nameEdited = true;
                await colExt.updateBinding(this.app.domain, this.graphExt);
                return colExt;
            });

            this.evaluateNameFormControls();
            this.rebuildSortOptions();

            this.markForCheck();
        }
    }

    private evaluateNameFormControls()
    {
        this.markForCheck();
        // let form controls get updated values
        setTimeout(() =>
                   {
                       for (let control of this.colNameFormControls)
                       {
                           control.markAsTouched();
                           control.updateValueAndValidity();
                       }
                   });
    }

    private isUniqueBindingName(control: AbstractControl): boolean
    {
        for (let c of this.colNameFormControls)
        {
            if (c !== control && c.value == control.value) return false;
        }

        return true;
    }

    private rebuildSortOptions()
    {
        this.sortColumnOptions = [AggregationNodeBindingConfigurerComponent.firstSortOption()];
        for (let nodeExt of this.nodeExts)
        {
            this.sortColumnOptions.push(new ControlOption(nodeExt.id, nodeExt.model.name || "Unnamed Column"));
        }
    }

    private static firstSortOption(): ControlOption<string>
    {
        return new ControlOption(AggregationNodeBindingConfigurerComponent.FIRST_COLUMN_OPTION, AggregationNodeBindingConfigurerComponent.FIRST_COLUMN_OPTION);
    }

    nodesReordered(event: CdkDragDrop<AggregationNodeBindingExtended>)
    {
        moveItemInArray(this.m_nodes, event.previousIndex, event.currentIndex);

        this.rebuildSortOptions();

        this.nodesChange.emit(this.m_nodes);
    }

    nodeRemoved(colIdx: number)
    {
        let node = this.m_nodes.splice(colIdx, 1)[0];
        if (this.initialSort.column === node.name) this.initialSort.column = null;

        this.rebuildSortOptions();

        this.nodesChange.emit(this.m_nodes);
    }

    addNode()
    {
        let col = Models.AggregationNodeBinding.newInstance({
                                                                aggregationType: Models.AggregationTypeId.MAX,
                                                                barRange       : Models.ToggleableNumericRange.newInstance({active: false})
                                                            });
        this.m_nodes.push(col);
        this.nodeExts.push(new AggregationNodeBindingExtended(this, col, null));
        this.nodeExts = UtilsService.arrayCopy(this.nodeExts);
        this.rebuildSortOptions();

        this.nodesChange.emit(this.m_nodes);
    }

    async selectedNodeChanged(nodeExt: AggregationNodeBindingExtended)
    {
        await nodeExt.updateBinding(this.app.domain, this.graphExt);
        this.updateSortOption(nodeExt);
        this.evaluateNameFormControls();
        this.markForCheck();
    }

    async unitsUpdated(nodeExt: AggregationNodeBindingExtended)
    {
        await nodeExt.updateUnits();
        if (this.editNodeExt === nodeExt) this.resetMappingSegments(nodeExt);
        this.markForCheck();
    }

    nameEdited(nodeExt: AggregationNodeBindingExtended,
               newName: string)
    {
        let prevName       = nodeExt.model.name;
        nodeExt.model.name = newName;
        nodeExt.nameEdited = true;

        if (prevName === this.initialSort.column) this.initialSortColumn = nodeExt.id;
        this.updateSortOption(nodeExt);

        this.evaluateNameFormControls();
    }

    private updateSortOption(nodeExt: AggregationNodeBindingExtended)
    {
        this.sortColumnOptions[this.nodeExts.indexOf(nodeExt) + 1].label = nodeExt.model.name;
        this.sortColumnOptions                                           = UtilsService.arrayCopy(this.sortColumnOptions);
    }

    configureDisplay(nodeExt: AggregationNodeBindingExtended)
    {
        if (this.editNodeExt !== nodeExt)
        {
            if (nodeExt.model.color?.segments.length)
            {
                this.resetMappingSegments(nodeExt);
            }
            else
            {
                let defaultColor = this.visualizationMode === Models.HierarchicalVisualizationType.TABLE_WITH_BAR ?
                    ChartColorUtilities.getDefaultColorById("blue").hex :
                    ChartColorUtilities.getColorById("Gray", "gray4").hex;

                this.mappingSegments = [
                    Models.ColorSegment.newInstance({
                                                        color    : defaultColor,
                                                        stopPoint: Models.ColorStopPoint.MIN
                                                    }),
                    Models.ColorSegment.newInstance({
                                                        color    : defaultColor,
                                                        stopPoint: Models.ColorStopPoint.MAX
                                                    })
                ];
            }
            this.editNodeExt = nodeExt;

            if (!this.displayConfigurer.isOpen) this.displayConfigurer.toggleOverlay();
            this.markForCheck();
        }
    }

    private resetMappingSegments(nodeExt: AggregationNodeBindingExtended)
    {
        this.mappingSegments = nodeExt.model.color.segments.map((segment) => Models.ColorSegment.newInstance(segment));
    }

    applyStops(stops: Models.ColorSegment[])
    {
        this.editNodeExt.model.color = Models.ColorConfiguration.newInstance({segments: stops});
    }
}

export class AggregationNodeBindingExtended
{
    nameEdited: boolean = false;

    desc: EngineeringUnitsDescriptorExtended;

    constructor(public readonly host: AggregationNodeBindingConfigurerComponent,
                public readonly model: Models.AggregationNodeBinding,
                public readonly id: string)
    {
        if (!this.id) this.id = UUID.UUID();
    }

    async updateBinding(domain: AppDomainContext,
                        graphExt: AssetGraphExtended)
    {
        const response = await graphExt.resolve();
        const results  = await response.resolveControlPoints(domain, [Models.AssetGraphBinding.newInstance({nodeId: this.model.nodeId})]);
        for (let deviceElem of results)
        {
            let element = await domain.assets.getTypedExtendedById(DeviceElementExtended, deviceElem.sysId);
            if (element)
            {
                let propSchema = await element.getSchemaProperty(DeviceElementExtended.PRESENT_VALUE);
                this.desc      = await domain.units.resolveDescriptor(propSchema?.unitsFactors, false);
                if (this.desc)
                {
                    if (!UnitsService.areEquivalent(this.desc.model.factors, this.model.units)) await this.updateUnits();
                    break;
                }
            }
        }

        if (!this.nameEdited || !this.model.name)
        {
            this.nameEdited = false;
            this.model.name = graphExt.getNodeName(this.model.nodeId);
        }
    }

    async updateUnits()
    {
        if (this.desc)
        {
            let unitsResolved = await this.host.app.domain.units.resolveDescriptor(this.desc.rawFactors ?? this.desc.model.factors, false);
            let newUnits      = EngineeringUnitsDescriptorExtended.extractFactors(unitsResolved);
            if (this.model.units && UnitsService.areEquivalent(this.model.units, newUnits))
            {
                await ColorConfigurationExtended.convertUnits(this.host.app.domain.units, this.model.color, this.model.units, newUnits);
                this.host.markForCheck();
            }

            this.model.units = newUnits;
        }
    }

    copy(): AggregationNodeBindingExtended
    {
        let copy        = new AggregationNodeBindingExtended(this.host, this.model, this.id);
        copy.desc       = this.desc;
        copy.nameEdited = this.nameEdited;
        return copy;
    }

    public static isValid(model: Models.AggregationNodeBinding): boolean
    {
        if (!model?.name) return false;
        if (model.name === AggregationNodeBindingConfigurerComponent.FIRST_COLUMN_OPTION) return false;
        if (!model.nodeId) return false;
        return !!model.aggregationType;
    }
}
