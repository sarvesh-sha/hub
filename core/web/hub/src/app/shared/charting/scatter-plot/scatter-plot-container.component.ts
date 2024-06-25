import {ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import {InteractableSource, InteractableSourcesChart, TimeSeriesChartConfigurationExtended, TimeSeriesSource, TimeSeriesSourceHost, TimeSeriesSourceTuple, ToggleableNumericRangeExtended} from "app/customer/visualization/time-series-utils";
import {AssetGraphResponseExtended} from "app/services/domain/asset-graph.service";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {EngineeringUnitsDescriptorExtended, UnitsService} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {ScatterPlotConfigurationComponent} from "app/shared/charting/scatter-plot/scatter-plot-configuration.component";
import {TimeSeriesChartingComponent} from "app/shared/charting/time-series-container/common";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";

import {ColorPickerConfigurationComponent} from "app/shared/colors/color-picker-configuration.component";
import {ColorPickerComponent} from "app/shared/colors/color-picker.component";

import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {Lookup, UtilsService} from "framework/services/utils.service";

import {ChartValueRange} from "framework/ui/charting/core/basics";
import {ChartColorUtilities, PaletteId} from "framework/ui/charting/core/colors";
import {DataSourceTuple, isVisible, ScatterPlotPropertyTuple, VisualizationDataSourceState} from "framework/ui/charting/core/data-sources";
import {ChartTimeRange} from "framework/ui/charting/core/time";
import {ScatterPlotAxis, ScatterPlotAxisLabel, ScatterPlotComponent, ScatterPlotInputPanel} from "framework/ui/charting/scatter-plot.component";
import {ControlOption} from "framework/ui/control-option";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {inParallel, mapInParallel} from "framework/utils/concurrency";

import {AsyncDebouncer} from "framework/utils/debouncers";
import {Subscription} from "rxjs";

@Component({
               selector       : "o3-scatter-plot-container",
               templateUrl    : "./scatter-plot-container.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ScatterPlotContainerComponent extends SharedSvc.BaseApplicationComponent implements TimeSeriesChartingComponent,
                                                                                                 InteractableSourcesChart
{
    private readonly host: TimeSeriesSourceHost;

    private m_debouncer = new AsyncDebouncer<void>(50, () => this.onChangeInternal());

    @Input() dimension: string  = DeviceElementExtended.PRESENT_VALUE;
    @Input() allowZoom: boolean = true;

    private m_range: Models.RangeSelection;
    @Input() set range(range: Models.RangeSelection)
    {
        this.m_range = range;
        this.onChange(true);
    }

    private sourceSub: Subscription;
    private m_configExt: TimeSeriesChartConfigurationExtended;
    @Input() set configExt(configExt: TimeSeriesChartConfigurationExtended)
    {
        this.m_configExt = configExt;
        this.onChange(true);

        if (this.sourceSub) this.sourceSub.unsubscribe();
        this.sourceSub = this.subscribeToObservable(this.m_configExt.configChanged, () => this.m_reloadPanels = true);
    }

    get configExt(): TimeSeriesChartConfigurationExtended
    {
        return this.m_configExt;
    }

    @ViewChild(ScatterPlotComponent, {static: true}) scatterPlot: ScatterPlotComponent;
    @ViewChild("colorConfigurer", {static: true}) colorConfigurerContainer: StandardFormOverlayComponent;
    @ViewChild("tupleConfigurer", {static: true}) tupleConfigurerContainer: StandardFormOverlayComponent;
    @ViewChild(ScatterPlotConfigurationComponent, {static: true}) axisConfigurerContainer: ScatterPlotConfigurationComponent;
    @ViewChild("test_colorConfig") test_colorConfig: ColorPickerConfigurationComponent;
    @ViewChild("test_color") test_color: ColorPickerComponent;

    @Output() sourceStatesUpdated = new EventEmitter<Lookup<VisualizationDataSourceState>>();
    @Output() chartUpdated        = new EventEmitter<boolean>();

    @Output() startedFetchingData = new EventEmitter<void>();
    @Output() stoppedFetchingData = new EventEmitter<void>();
    @Output() configExtChange     = new EventEmitter<TimeSeriesChartConfigurationExtended>();

    editColorConfig: OverlayConfig;
    tupleEditConfig: OverlayConfig;

    editTupleModel: Models.ScatterPlotSourceTuple;
    editTupleInteractable: InteractableSource;
    tuplePanelPalette: PaletteId;
    startTupleColorOverride: string;
    tupleColor: string;

    panelConfigs: Models.TimeSeriesPanelConfiguration[];
    panelConfigOptions: ControlOption<Models.TimeSeriesPanelConfiguration>[];
    rememberedTupleColors: string[][];
    activePanel: Models.TimeSeriesPanelConfiguration;
    activePanelHasGradient: boolean;

    generatedTuplesPerPanel: Models.ScatterPlotSourceTuple[][];

    axisTuples: ScatterPlotPropertyTuple<Models.TimeSeriesAxisConfiguration>[];
    private m_editAxisTuples: ScatterPlotPropertyTuple<Models.TimeSeriesAxisConfiguration>[];

    private get activeAxisTuples(): ScatterPlotPropertyTuple<Models.TimeSeriesAxisConfiguration>[]
    {
        return this.m_editAxisTuples || this.axisTuples;
    }

    private sourceTuplesPerPanel: DataSourceTuple[][];

    private m_reloadPanels: boolean = true;

    scatterPlotInputPanels: ScatterPlotInputPanel[] = [];
    timeRange                                       = new ChartTimeRange();

    private idToSource: Lookup<TimeSeriesSource>;

    get colorConfigsAreValid(): boolean
    {
        if (!this.m_configExt) return false;

        return this.m_configExt.model.panels.every((panel) => ColorConfigurationExtended.isValid(panel.colorSettings));
    }

    get colorConfigsArePristine(): boolean
    {
        if (!this.panelConfigs) return false;

        return this.panelConfigs.every((preSaveConfig,
                                        idx) => ColorConfigurationExtended.areEquivalent(preSaveConfig.colorSettings, this.panelConfigOptions[idx].id.colorSettings));
    }

    private get validInputs(): boolean
    {
        if (!this.m_range) return false;
        if (!this.m_configExt) return false;

        switch (this.m_configExt.model.type)
        {
            case Models.TimeSeriesChartType.SCATTER:
            case Models.TimeSeriesChartType.GRAPH_SCATTER:
                return true;

            default:
                return false;
        }
    }

    constructor(inj: Injector)
    {
        super(inj);

        this.host = new TimeSeriesSourceHost(this);

        this.editColorConfig = ColorPickerConfigurationComponent.colorOverlayConfig(true);

        this.tupleEditConfig                 = ColorPickerConfigurationComponent.colorOverlayConfig(true);
        this.tupleEditConfig.showCloseButton = true;
    }

    public onChange(reloadPanels?: boolean)
    {
        if (reloadPanels)
        {
            this.m_reloadPanels = true;
        }

        this.m_debouncer.invoke();
    }

    public getNumSources(): number
    {
        return this.generatedTuplesPerPanel?.reduce((cum,
                                                     tuples) => cum + tuples.length, 0);
    }

    public tupleIdsByPanel()
    {
        return this.generatedTuplesPerPanel.map(
            (generatedTuples,
             panelIdx) =>
            {
                return generatedTuples.map((tuple,
                                            tupleIdx) => `${panelIdx},${tupleIdx}`);
            });
    }

    public toggleConfigurer()
    {
        this.axisConfigurerContainer.toggleOverlay();
    }

    private async onChangeInternal()
    {
        if (!this.validInputs) return;

        if (this.m_reloadPanels)
        {
            // Update time ranges
            let rangeExtended          = new RangeSelectionExtended(this.m_range);
            this.timeRange.minAsMoment = rangeExtended.getMin();
            this.timeRange.maxAsMoment = rangeExtended.getMax();

            await this.generateTuples();
            this.buildPanels();
        }
        else
        {
            this.chartUpdated.emit();
            this.scatterPlot.onChange();
        }
    }

    private async generateTuples()
    {
        let equivalentTuples = await this.processTuples();
        await this.bindSources();
        await this.generateSourceTuples(!equivalentTuples);
    }

    private getRootNodeIdentifier(source: Models.ScatterPlotSource): string
    {
        const graphId = source.binding?.graphId;
        if (graphId)
        {
            const graph  = this.m_configExt.resolvedGraphs.get(source.binding?.graphId);
            const rootId = graph.getRootNodeId(source.binding.nodeId);
            return this.getIdentifier(graphId, rootId);
        }

        return "";
    }

    private getIdentifier(graphId: string,
                          rootId: string): string
    {
        return `${graphId}/${rootId}`;
    }

    private async processTuples(): Promise<boolean>
    {
        let graphModel   = this.m_configExt.model;
        let sourceTuples = graphModel.scatterPlot.sourceTuples;

        // backwards compatibility
        let firstTuple = sourceTuples[0];
        if (firstTuple.panel == null)
        {
            for (let i = 0; i < sourceTuples.length; i++) sourceTuples[i].panel = i;
        }

        let previousTuples           = this.generatedTuplesPerPanel;
        this.generatedTuplesPerPanel = [];
        if (graphModel.type === Models.TimeSeriesChartType.SCATTER)
        {
            for (let tuple of sourceTuples)
            {
                let tuplesPerPanel = this.generatedTuplesPerPanel[tuple.panel];
                if (!tuplesPerPanel) tuplesPerPanel = this.generatedTuplesPerPanel[tuple.panel] = [];
                tuplesPerPanel.push(tuple);
            }
        }
        else
        {
            if (graphModel.type === Models.TimeSeriesChartType.GRAPH_SCATTER)
            {
                let allSelectedGraphs = await this.m_configExt.getAssetGraphResponses();
                let selectedGraphs    = new Map<string, AssetGraphResponseExtended[]>();
                for (let entry of allSelectedGraphs)
                {
                    let id = this.getIdentifier(entry.context.graphId, entry.context.nodeId);
                    let array = selectedGraphs.get(id);
                    if (!array)
                    {
                        array = [];
                        selectedGraphs.set(id, array);
                    }

                    array.push(...entry.responses);
                }

                if (selectedGraphs.size > 0)
                {
                    for (let tuple of sourceTuples)
                    {
                        let tuplesPerPanel = this.generatedTuplesPerPanel[tuple.panel];
                        if (!tuplesPerPanel) tuplesPerPanel = this.generatedTuplesPerPanel[tuple.panel] = [];

                        let rootX = this.getRootNodeIdentifier(tuple.sourceX);
                        let rootY = this.getRootNodeIdentifier(tuple.sourceY);
                        let rootZ = this.getRootNodeIdentifier(tuple.sourceZ);

                        for (let xGraph of selectedGraphs.get(rootX))
                        {
                            let name    = await this.getName(xGraph, tuple.name);
                            let xRecord = this.getAssociatedRecord(xGraph, tuple.sourceX);

                            if (rootY === rootX)
                            {
                                let yRecord = this.getAssociatedRecord(xGraph, tuple.sourceY);
                                if (!rootZ || rootZ === rootX)
                                {
                                    let zRecord = this.getAssociatedRecord(xGraph, tuple.sourceZ);

                                    if (xRecord && yRecord) tuplesPerPanel.push(this.generateTuple(tuple.panel, xRecord.sysId, yRecord.sysId, zRecord && zRecord.sysId, tuple.colorOverride, name));
                                }
                                else
                                {
                                    for (let zGraph of selectedGraphs.get(rootZ))
                                    {
                                        let nameZ   = await this.getName(zGraph, name);
                                        let zRecord = this.getAssociatedRecord(zGraph, tuple.sourceZ);

                                        if (xRecord && yRecord)
                                        {
                                            tuplesPerPanel.push(this.generateTuple(tuple.panel,
                                                                                   xRecord.sysId,
                                                                                   yRecord.sysId,
                                                                                   zRecord && zRecord.sysId,
                                                                                   tuple.colorOverride,
                                                                                   nameZ));
                                        }
                                    }
                                }
                            }
                            else
                            {
                                for (let yGraph of selectedGraphs.get(rootY))
                                {
                                    let nameY   = await this.getName(yGraph, name);
                                    let yRecord = this.getAssociatedRecord(yGraph, tuple.sourceY);

                                    if (rootZ && rootZ !== rootX && rootZ !== rootY)
                                    {
                                        for (let zGraph of selectedGraphs.get(rootZ))
                                        {
                                            let nameZ   = await this.getName(zGraph, nameY);
                                            let zRecord = this.getAssociatedRecord(zGraph, tuple.sourceZ);

                                            if (xRecord && yRecord)
                                            {
                                                tuplesPerPanel.push(this.generateTuple(tuple.panel, xRecord.sysId, yRecord.sysId, zRecord?.sysId, tuple.colorOverride, nameZ));
                                            }
                                        }
                                    }
                                    else
                                    {
                                        let zRecord = this.getAssociatedRecord(rootZ === rootX ? xGraph : yGraph, tuple.sourceZ);

                                        if (xRecord && yRecord)
                                        {
                                            tuplesPerPanel.push(this.generateTuple(tuple.panel,
                                                                                   xRecord.sysId,
                                                                                   yRecord.sysId,
                                                                                   zRecord && zRecord.sysId,
                                                                                   tuple.colorOverride,
                                                                                   nameY));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return this.generatedTuplesAreEquivalent(previousTuples);
    }

    private async bindSources()
    {
        this.idToSource = {};
        await inParallel(this.generatedTuplesPerPanel, async (tupleArr) =>
        {
            let sourcesArr: TimeSeriesSource[][] = await mapInParallel(tupleArr, (tuple) =>
            {
                return Promise.all([
                                       TimeSeriesSource.sourceFromId(this.host, tuple.sourceX.deviceElementId),
                                       TimeSeriesSource.sourceFromId(this.host, tuple.sourceY.deviceElementId),
                                       tuple.sourceZ && TimeSeriesSource.sourceFromId(this.host, tuple.sourceZ.deviceElementId)
                                   ]);
            });

            for (let sources of sourcesArr)
            {
                this.idToSource = UtilsService.extractLookup(sources, this.idToSource);
            }
        });
    }

    private async generateSourceTuples(updateColors: boolean)
    {
        updateColors = updateColors || !this.sourceTuplesPerPanel;
        if (!updateColors) this.rememberCurrentColors();

        if (!this.m_editAxisTuples) this.axisTuples = [];
        this.sourceTuplesPerPanel = await mapInParallel(this.generatedTuplesPerPanel, (generatedTuples,
                                                                                       panelIdx) =>
        {
            let firstTuple = generatedTuples[0];
            if (firstTuple)
            {
                let hasZ      = !!firstTuple?.sourceZ && !!firstTuple.sourceZ.deviceElementId;
                let axesTuple = this.getScatterPlotAxes(panelIdx, hasZ);
                return mapInParallel(generatedTuples,
                                     async (tuple,
                                            tupleIdx) =>
                                     {
                                         let idTuple = new ScatterPlotPropertyTuple(0,
                                                                                    tuple.sourceX.deviceElementId,
                                                                                    tuple.sourceY.deviceElementId,
                                                                                    hasZ && tuple.sourceZ.deviceElementId || undefined);

                                         let timeSeriesSourceTuple = await TimeSeriesSourceTuple.fromTuple(this.host, idTuple, this.idToSource, tuple.name);

                                         let unitsTasks: Promise<Models.EngineeringUnitsFactors>[] = [];

                                         let xAxis = axesTuple.valueX;
                                         unitsTasks.push(xAxis.displayFactors ? Promise.resolve(xAxis.displayFactors) : timeSeriesSourceTuple.sourceX.currentUnits(this.dimension));

                                         let yAxis = axesTuple.valueY;
                                         unitsTasks.push(yAxis.displayFactors ? Promise.resolve(yAxis.displayFactors) : timeSeriesSourceTuple.sourceY.currentUnits(this.dimension));

                                         let sourceZ = timeSeriesSourceTuple.sourceZ;
                                         let zAxis   = axesTuple.valueZ;
                                         if (sourceZ && zAxis)
                                         {
                                             unitsTasks.push(zAxis.displayFactors ? Promise.resolve(zAxis.displayFactors) : sourceZ.currentUnits(this.dimension));
                                         }

                                         let units            = await Promise.all(unitsTasks);
                                         let unitsTuple       = new ScatterPlotPropertyTuple(0, units[0], units[1], units[2]);
                                         xAxis.displayFactors = unitsTuple.valueX;
                                         yAxis.displayFactors = unitsTuple.valueY;
                                         if (sourceZ && zAxis) zAxis.displayFactors = unitsTuple.valueZ;

                                         let dataSourceTuple = await timeSeriesSourceTuple.getDataSource(this.dimension, false, this.timeRange.minAsMoment, this.timeRange.maxAsMoment, unitsTuple);
                                         if (!dataSourceTuple) return null;
                                         dataSourceTuple.tupleId  = `${tuple.panel},${tupleIdx}`;
                                         dataSourceTuple.zoneName = this.m_range.zone;

                                         return dataSourceTuple;
                                     });
            }
            else
            {
                if (!this.m_editAxisTuples) this.axisTuples[panelIdx] = null;
                return Promise.resolve([]);
            }
        });

        this.updateColorInfo(updateColors);
    }

    private generatedTuplesAreEquivalent(prevGeneratedTuples: Models.ScatterPlotSourceTuple[][]): boolean
    {
        if (!prevGeneratedTuples) return !this.generatedTuplesPerPanel;
        if (this.generatedTuplesPerPanel.length !== prevGeneratedTuples.length) return false;

        for (let panelIdx = 0; panelIdx < prevGeneratedTuples.length; panelIdx++)
        {
            let prevTuples = prevGeneratedTuples[panelIdx];
            let currTuples = this.generatedTuplesPerPanel[panelIdx];
            if (prevTuples.length != currTuples.length) return false;

            for (let tupleIdx = 0; tupleIdx < prevTuples.length; tupleIdx++)
            {
                let prevTuple = prevTuples[tupleIdx];
                let currTuple = currTuples[tupleIdx];

                if (prevTuple.sourceX.deviceElementId != currTuple.sourceX.deviceElementId) return false;
                if (prevTuple.sourceY.deviceElementId != currTuple.sourceY.deviceElementId) return false;
                if (prevTuple.sourceZ?.deviceElementId != currTuple.sourceZ?.deviceElementId) return false;
            }
        }

        return true;
    }

    private async getName(graph: AssetGraphResponseExtended,
                          name: string)
    {
        let nameOpt = await graph.getControlOption();
        return [
            name,
            nameOpt.label
        ].filter((s) => !!s)
         .join(" - ");
    }

    private generateTuple(panelIdx: number,
                          xId: string,
                          yId: string,
                          zId?: string,
                          colorOverride?: string,
                          name?: string): Models.ScatterPlotSourceTuple
    {
        let tuple = Models.ScatterPlotSourceTuple.newInstance({
                                                                  panel        : panelIdx,
                                                                  sourceX      : Models.ScatterPlotSource.newInstance({deviceElementId: xId}),
                                                                  sourceY      : Models.ScatterPlotSource.newInstance({deviceElementId: yId}),
                                                                  colorOverride: colorOverride,
                                                                  name         : name
                                                              });

        if (zId) tuple.sourceZ = Models.ScatterPlotSource.newInstance({deviceElementId: zId});

        return tuple;
    }

    private getAssociatedRecord(graph: AssetGraphResponseExtended,
                                source: Models.ScatterPlotSource): Models.RecordIdentity
    {
        const binding = source?.binding;
        if (binding)
        {
            return graph.resolveInputIdentity(binding);
        }

        return null;
    }

    private getScatterPlotAxes(panelIdx: number,
                               withZ: boolean): ScatterPlotPropertyTuple<Models.TimeSeriesAxisConfiguration>
    {
        let axisTuples = this.activeAxisTuples;
        let tuple      = axisTuples[panelIdx];
        if (!tuple)
        {
            let panel = this.m_configExt.model.panels[panelIdx];
            if (!panel) panel = this.m_configExt.model.panels[panelIdx] = ScatterPlotContainerComponent.newPanel();
            tuple = axisTuples[panelIdx] = new ScatterPlotPropertyTuple(0, panel.xAxis, panel.leftAxis, withZ ? panel.rightAxis : undefined);
        }

        return tuple;
    }

    private buildPanels()
    {
        this.scatterPlotInputPanels = this.activeAxisTuples.map(
            (axesTuple,
             idx) =>
            {
                if (!axesTuple) return null;

                let xAxisPre  = axesTuple.valueX;
                let xAxisPost = new ScatterPlotAxis(new ScatterPlotAxisLabel(xAxisPre.label), this.generateChartValueRange(xAxisPre.override) || undefined);

                let yAxisPre  = axesTuple.valueY;
                let yAxisPost = new ScatterPlotAxis(new ScatterPlotAxisLabel(yAxisPre.label), this.generateChartValueRange(yAxisPre.override) || undefined);

                let zAxisPre = axesTuple.valueZ;
                let zAxisPost;
                let gradientStops;
                if (zAxisPre)
                {
                    zAxisPost     = new ScatterPlotAxis(new ScatterPlotAxisLabel(zAxisPre.label), this.generateChartValueRange(zAxisPre.override) || undefined);
                    gradientStops = new ColorConfigurationExtended(this.m_configExt.model.panels[idx].colorSettings).computeStops();
                }

                let scatterPanel = new ScatterPlotInputPanel(this.sourceTuplesPerPanel[idx], xAxisPost, yAxisPost, zAxisPost);

                if (gradientStops)
                {
                    scatterPanel.gradientStops = gradientStops;
                    for (let tuple of scatterPanel.sourceTuples) tuple.colorSegments = gradientStops;
                }

                return scatterPanel;
            });
        this.m_reloadPanels         = false;

        if (this.panelConfigs) this.toggleColorConfigurer();

        this.detectChanges();
        this.chartUpdated.emit();
    }

    onMouseMove(x: number,
                y: number): boolean
    {
        return false;
    }

    onMouseLeave(): void
    {
    }

    async axesChanged(axesTuples: ScatterPlotPropertyTuple<Models.TimeSeriesAxisConfiguration>[],
                      write: boolean)
    {
        axesTuples = axesTuples || this.axisTuples;
        if (write)
        {
            this.m_editAxisTuples = null;
            for (let panelIdx = 0; panelIdx < axesTuples.length; panelIdx++)
            {
                let axesTuple = axesTuples[panelIdx];
                if (!axesTuple) continue;

                let panel      = this.m_configExt.model.panels[panelIdx];
                panel.xAxis    = axesTuple.valueX;
                panel.leftAxis = axesTuple.valueY;
                if (axesTuple.valueZ)
                {
                    axesTuple.valueZ.displayFactors = EngineeringUnitsDescriptorExtended.extractFactors(await this.app.domain.units.resolveDescriptor(axesTuple.valueZ.displayFactors, false));
                    if (UnitsService.areEquivalent(panel.rightAxis.displayFactors, axesTuple.valueZ.displayFactors))
                    {
                        await ColorConfigurationExtended.convertUnits(this.app.domain.units, panel.colorSettings, panel.rightAxis.displayFactors, axesTuple.valueZ.displayFactors);
                    }
                    panel.rightAxis = axesTuple.valueZ;
                }
            }
            this.m_configExt = await TimeSeriesChartConfigurationExtended.newInstance(this.app, this.m_configExt.model);

            this.configExtChange.emit(this.m_configExt);
            this.chartUpdated.emit();
        }
        else
        {
            this.m_editAxisTuples = axesTuples;
        }

        this.onChange(true);
    }

    private updateColorInfo(updateColors: boolean)
    {
        let otherColors: string[] = [];
        let axisTuples            = this.activeAxisTuples;
        if (updateColors)
        {
            for (let panelIdx = 0; panelIdx < axisTuples.length; panelIdx++)
            {
                let axisTuple = axisTuples[panelIdx];
                if (axisTuple && !axisTuple.valueZ) this.addOverrides(panelIdx, otherColors);
            }
        }

        for (let i = 0; i < axisTuples.length; i++) this.updatePanelColorInfo(i, otherColors, updateColors);
    }

    private updatePanelColorInfo(panelIdx: number,
                                 otherColors: string[],
                                 updateColors: boolean)
    {
        let tuples        = this.sourceTuplesPerPanel[panelIdx];
        let panels        = this.m_configExt?.model?.panels || [];
        let colorSettings = panels[panelIdx]?.colorSettings;
        let axisTuple     = this.activeAxisTuples[panelIdx];
        if (axisTuple)
        {
            if (axisTuple.valueZ && colorSettings)
            {
                let gradientStops = new ColorConfigurationExtended(colorSettings).computeStops();

                for (let tuple of this.sourceTuplesPerPanel[panelIdx]) tuple.colorSegments = gradientStops;

                let panel = this.scatterPlotInputPanels && this.scatterPlotInputPanels[panelIdx];
                if (panel) panel.gradientStops = gradientStops;
            }
            else
            {
                const getNextColor = updateColors ?
                    () => ChartColorUtilities.nextBestColor(otherColors, <PaletteId>colorSettings.paletteName) :
                    (tupleIdx: number) => this.rememberedTupleColors[panelIdx][tupleIdx];

                let generatedTuples = this.generatedTuplesPerPanel[panelIdx];
                for (let tupleIdx = 0; tupleIdx < tuples.length; tupleIdx++)
                {
                    tuples[tupleIdx].color = generatedTuples[tupleIdx]?.colorOverride || getNextColor(tupleIdx);
                }
            }
        }
    }

    private addOverrides(panelIdx: number,
                         colorList: string[])
    {
        let generatedTuples = this.generatedTuplesPerPanel[panelIdx];
        for (let tuple of generatedTuples)
        {
            if (tuple.colorOverride) colorList.push(tuple.colorOverride);
        }
    }

    private generateChartValueRange(toggleableRange: Models.ToggleableNumericRange): ChartValueRange
    {
        let rangeExt = new ToggleableNumericRangeExtended(toggleableRange);
        return rangeExt.isActive ? rangeExt.chartRange : null;
    }

    public emitSourceStates()
    {
        this.sourceStatesUpdated.emit(this.m_configExt.getSourcesStates(this));
    }

    public toggleColorConfigurer(panelNum: number = 0)
    {
        if (!this.colorConfigurerContainer.isOpen())
        {
            this.panelConfigs = this.m_configExt.model.panels;
            let copyConfigs   = this.panelConfigs.map((panel) => Models.TimeSeriesPanelConfiguration.deepClone(panel));

            this.m_configExt.model.panels = copyConfigs;

            this.panelConfigOptions = copyConfigs.map((panel,
                                                       idx) => new ControlOption(panel, "Panel " + (idx + 1)));
            this.activePanel        = this.panelConfigOptions[panelNum]?.id;

            this.updateColorConfigurer();

            this.rememberCurrentColors();
        }

        this.colorConfigurerContainer.toggleOverlay();
    }

    private rememberCurrentColors()
    {
        this.rememberedTupleColors = this.scatterPlotInputPanels.map(
            (scatterPanel) => scatterPanel && !scatterPanel.zAxis && scatterPanel.sourceTuples.map((tuple) => tuple.color));
    }

    public updateColorConfigurer()
    {
        let scatterSource;
        if (this.panelConfigOptions.length > 1)
        {
            let panelIdx  = this.panelConfigOptions.findIndex((panelOption) => panelOption.id === this.activePanel);
            scatterSource = this.m_configExt.model.scatterPlot.sourceTuples[panelIdx];
        }
        else
        {
            scatterSource = this.m_configExt.model.scatterPlot.sourceTuples[0];
        }

        let sourceZ                 = scatterSource.sourceZ;
        this.activePanelHasGradient = sourceZ && (!!sourceZ.deviceElementId || sourceZ.binding && !!sourceZ.binding.nodeId);
    }

    public updatePanelColorConfig()
    {
        let idx = this.panelConfigOptions.findIndex((option) => option.id === this.activePanel);
        if (!ColorConfigurationExtended.isValid(this.m_configExt.model.panels[idx].colorSettings)) return;

        let otherColors = [];
        let axisTuples  = this.activeAxisTuples;
        if (!axisTuples[idx].valueZ)
        {
            for (let i = 0; i < this.sourceTuplesPerPanel.length; i++)
            {
                let axisTuple = axisTuples[i];
                if (i === idx || !axisTuple || axisTuple.valueZ) continue;

                for (let tuple of this.sourceTuplesPerPanel[i]) otherColors.push(tuple.color);
            }

            this.addOverrides(idx, otherColors);
        }
        this.updatePanelColorInfo(idx, otherColors, true);

        this.notifyChange();
    }

    public clearColorConfigurationEdits()
    {
        if (!this.panelConfigs) return;
        this.m_configExt.model.panels = this.panelConfigs;
        this.panelConfigs             = null;
        this.updateColorInfo(false);
        this.notifyChange();
    }

    private notifyChange()
    {
        this.scatterPlot.onChange();
        this.chartUpdated.emit();
    }

    //--//

    public refreshSize(): boolean
    {
        return !!this.scatterPlot?.refreshSize();
    }

    public getCanvasPNG(): string
    {
        return this.scatterPlot.getCanvasPNG();
    }

    public getCanvasTitle(): string
    {
        return "scatter-plot";
    }

    //--//

    public async configureSource(sourceId: string)
    {
        if (this.editTupleInteractable) await this.revertTupleEdits();

        this.editTupleInteractable = this.getSource(sourceId);
        let [panelIdx, tupleIdx]   = TimeSeriesChartConfigurationExtended.getScatterTupleIndices(this.editTupleInteractable.identifier);
        let panelTuples            = this.configExt.model.scatterPlot.sourceTuples.filter((tuple) => tuple.panel === panelIdx);
        this.editTupleModel        = panelTuples[tupleIdx];

        if (this.editTupleModel)
        {
            this.tupleColor              = <string>this.editTupleInteractable.color;
            this.startTupleColorOverride = this.editTupleModel.colorOverride;
            this.tuplePanelPalette       = <PaletteId>this.configExt.model.panels[this.editTupleModel.panel].colorSettings.paletteName;

            this.tupleConfigurerContainer.openOverlay();
        }
    }

    public async revertTupleEdits()
    {
        if (!this.editTupleInteractable || <string>this.editTupleInteractable.color === this.tupleColor) return;

        this.editTupleModel.colorOverride               = this.startTupleColorOverride;
        this.editTupleInteractable.getChartData().color = <string>this.editTupleInteractable.color;

        this.onChange(false);

        this.editTupleModel = this.editTupleInteractable = null;
    }

    public updateTupleColor()
    {
        this.editTupleModel.colorOverride               = this.tupleColor;
        this.editTupleInteractable.getChartData().color = this.tupleColor;

        this.onChange(false);
    }

    //--//

    public getSource(sourceId: string): InteractableSource
    {
        let [panelIdx, tupleIdx] = TimeSeriesChartConfigurationExtended.getScatterTupleIndices(sourceId);
        return this.getInteractableSource(panelIdx, tupleIdx);
    }

    public isDeletable(sourceId: string): boolean
    {
        return false;
    }

    private getInteractableSource(panelIdx: number,
                                  tupleIdx: number): InteractableSource
    {
        if (!this.sourceTuplesPerPanel || this.sourceTuplesPerPanel.length <= panelIdx) return null;

        let dataSourceTuple = this.sourceTuplesPerPanel[panelIdx][tupleIdx];
        if (!dataSourceTuple) return null;

        let hasZ       = !!dataSourceTuple.dataSources.valueZ;
        let subSources = dataSourceTuple.subSources;

        let name        = this.generatedTuplesPerPanel[panelIdx][tupleIdx]?.name;
        let description = `X: ${subSources.valueX.name} | Y: ${subSources.valueY.name}`;
        if (name)
        {
            if (hasZ) description += " | Color: " + subSources.valueZ.name;
        }
        else
        {
            name        = description;
            description = hasZ ? "Color: " + subSources.valueZ.name : "";
        }

        let getSource = (tupleId: string) => this.getSourceTuple(tupleId);

        let source: InteractableSource = {
            identifier : dataSourceTuple.tupleId,
            panel      : panelIdx,
            name       : name,
            description: description,
            valid      : true,
            color      : dataSourceTuple.color,
            getChartData() { return getSource(dataSourceTuple.tupleId); }
        };
        if (dataSourceTuple.colorSegments)
        {
            source.colorStops = dataSourceTuple.colorSegments;
        }

        return source;
    }

    private getSourceTuple(tupleId: string): DataSourceTuple
    {
        let [panelIdx, tupleIdx] = TimeSeriesChartConfigurationExtended.getScatterTupleIndices(tupleId);
        return this.sourceTuplesPerPanel[panelIdx][tupleIdx];
    }

    getSourceState(sourceId: string): VisualizationDataSourceState
    {
        let [panelIdx, tupleIdx] = TimeSeriesChartConfigurationExtended.getScatterTupleIndices(sourceId);
        if (!this.sourceTuplesPerPanel) return VisualizationDataSourceState.Active;

        let dataSourceTuple = this.sourceTuplesPerPanel[panelIdx][tupleIdx];
        return dataSourceTuple?.state;
    }

    getSources(panelIdx?: number,
               onlyVisible: boolean = true): InteractableSource[]
    {
        let sources: InteractableSource[] = [];
        if (!this.sourceTuplesPerPanel) return sources;

        if (panelIdx == undefined)
        {
            for (let i = 0; i < this.sourceTuplesPerPanel.length; i++) this.addViewableSources(sources, i, onlyVisible);
        }
        else
        {
            this.addViewableSources(sources, panelIdx, onlyVisible);
        }

        return sources;
    }

    private addViewableSources(sourceList: InteractableSource[],
                               panelIdx: number,
                               onlyVisible: boolean)
    {
        let tuples = this.sourceTuplesPerPanel[panelIdx];
        for (let tupleIdx = 0; tupleIdx < tuples.length; tupleIdx++)
        {
            let state = tuples[tupleIdx]?.state;
            if (!onlyVisible || isVisible(state))
            {
                sourceList.push(this.getInteractableSource(panelIdx, tupleIdx));
            }
        }
    }

    isReady(): boolean
    {
        return !!this.scatterPlotInputPanels;
    }

    toggleTarget(sourceId: string,
                 fromMouseover: boolean): void
    {
        let [panelIdx, tupleIdx] = TimeSeriesChartConfigurationExtended.getScatterTupleIndices(sourceId);
        this.scatterPlot.toggleTarget(panelIdx, this.sourceTuplesPerPanel[panelIdx][tupleIdx], fromMouseover);
    }

    toggleEnabled(sourceId: string): void
    {
        let [panelIdx, tupleIdx] = TimeSeriesChartConfigurationExtended.getScatterTupleIndices(sourceId);
        this.scatterPlot.toggleEnabled(panelIdx, this.sourceTuplesPerPanel[panelIdx][tupleIdx]);
    }

    multiToggleEnabled(originSourceId: string)
    {
        let [panelIdx, tupleIdx] = TimeSeriesChartConfigurationExtended.getScatterTupleIndices(originSourceId);
        this.scatterPlot.multiToggleEnabled(panelIdx, this.sourceTuplesPerPanel[panelIdx][tupleIdx]);
    }

    //--//

    public static newPanel(): Models.TimeSeriesPanelConfiguration
    {
        let newAxis = () => Models.TimeSeriesAxisConfiguration.newInstance({label: ""});
        return Models.TimeSeriesPanelConfiguration.newInstance({
                                                                   colorSettings: ColorConfigurationExtended.newModel(),
                                                                   xAxis        : newAxis(),
                                                                   leftAxis     : newAxis(),
                                                                   rightAxis    : newAxis()
                                                               });
    }
}

export class ScatterPlotSourceTupleExtended
{
    constructor(public model: Models.ScatterPlotSourceTuple)
    {
    }

    static newModel(panelNum: number): Models.ScatterPlotSourceTuple
    {
        return Models.ScatterPlotSourceTuple.newInstance(
            {
                panel  : panelNum,
                sourceX: ScatterPlotSourceTupleExtended.newSource(),
                sourceY: ScatterPlotSourceTupleExtended.newSource(),
                sourceZ: ScatterPlotSourceTupleExtended.newSource()
            }
        );
    }

    private static newSource(): Models.ScatterPlotSource
    {
        return Models.ScatterPlotSource.newInstance({
                                                        deviceElementId: "",
                                                        binding        : Models.AssetGraphBinding.newInstance({
                                                                                                                  graphId: "",
                                                                                                                  nodeId : ""
                                                                                                              })
                                                    });
    }

    static deepCopy(model: Models.ScatterPlotSourceTuple): Models.ScatterPlotSourceTuple
    {
        let newModel     = Models.ScatterPlotSourceTuple.newInstance(model);
        newModel.sourceX = ScatterPlotSourceTupleExtended.deepCopySource(model.sourceX);
        newModel.sourceY = ScatterPlotSourceTupleExtended.deepCopySource(model.sourceY);
        newModel.sourceZ = ScatterPlotSourceTupleExtended.deepCopySource(model.sourceZ);

        return newModel;
    }

    private static deepCopySource(model: Models.ScatterPlotSource): Models.ScatterPlotSource
    {
        if (!model) return model;

        let copySource = Models.ScatterPlotSource.newInstance({deviceElementId: model.deviceElementId});
        if (model.binding)
        {
            copySource.binding = Models.AssetGraphBinding.newInstance({
                                                                          graphId: model.binding.graphId,
                                                                          nodeId : model.binding.nodeId
                                                                      });
        }

        return copySource;
    }

    static getDeviceElementId(source: Models.ScatterPlotSource): string
    {
        return source?.deviceElementId;
    }
}
