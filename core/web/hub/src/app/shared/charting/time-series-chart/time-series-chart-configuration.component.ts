import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import {InteractableSource, TimeSeriesChartConfigurationExtended, TimeSeriesSourceConfigurationExtended, TimeSeriesSourceHost} from "app/customer/visualization/time-series-utils";
import * as SharedSvc from "app/services/domain/base.service";
import {EngineeringUnitsDescriptorExtended, UnitsService} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {AddTimeSeriesSourceEvent} from "app/shared/charting/add-related-source/add-related-source.component";
import {SourceConfigurerComponent} from "app/shared/charting/source-configurer/source-configurer.component";
import {AnnotationEntry, AxisEntry, GroupEntry, PanelEntry, TimeSeriesChartSourcesConfigurationComponent} from "app/shared/charting/time-series-chart/time-series-chart-sources-configuration.component";
import {TimeSeriesChartComponent} from "app/shared/charting/time-series-chart/time-series-chart.component";
import {ChartChangeAnnotationImpact} from "app/shared/charting/time-series-container/common";

import {UtilsService} from "framework/services/utils.service";
import {OverlayController} from "framework/ui/overlays/overlay-base";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {TabGroupComponent} from "framework/ui/tab-group/tab-group.component";
import {mapInParallel, mapInParallelNoNulls} from "framework/utils/concurrency";

import {Subject, Subscription} from "rxjs";
import {debounceTime} from "rxjs/operators";

@Component({
               selector       : "o3-time-series-chart-configuration",
               templateUrl    : "./time-series-chart-configuration.component.html",
               styleUrls      : ["./time-series-chart-configuration.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class TimeSeriesChartConfigurationComponent extends SharedSvc.BaseApplicationComponent implements OverlayController
{
    private sourceSub: Subscription;

    editConfigExt: TimeSeriesChartConfigurationExtended;

    private m_configExt: TimeSeriesChartConfigurationExtended;
    @Input() set configExt(chartExt: TimeSeriesChartConfigurationExtended)
    {
        this.m_configExt = chartExt;

        if (this.sourceSub) this.sourceSub.unsubscribe();
        this.sourceSub = this.subscribeToObservable(this.m_configExt.configChanged, () => this.rebuild());

        this.rebuild();
    }

    get configExt(): TimeSeriesChartConfigurationExtended
    {
        return this.m_configExt;
    }

    @Input() chartElement: TimeSeriesChartComponent;

    @Output() configEdit   = new EventEmitter<TimeSeriesChartConfigurationExtended>();
    @Output() configUpdate = new EventEmitter<TimeSeriesChartConfigurationExtended>();

    @ViewChild(StandardFormOverlayComponent, {static: true}) overlay: StandardFormOverlayComponent;
    @ViewChild(TabGroupComponent, {read: ElementRef}) tabGroup: ElementRef;
    @ViewChild(SourceConfigurerComponent, {static: false}) sourceConfigurer: SourceConfigurerComponent;

    @ViewChild("test_tabGroup") test_tabGroup: TabGroupComponent;
    @ViewChild("test_sources") test_sources: TimeSeriesChartSourcesConfigurationComponent;

    get showDecimation(): boolean
    {
        return !this.editConfigExt.model.display.hideDecimation;
    }

    set showDecimation(show: boolean)
    {
        this.editConfigExt.model.display.hideDecimation = !show;
    }

    get showSources(): boolean
    {
        return !this.editConfigExt.model.display.hideSources;
    }

    set showSources(show: boolean)
    {
        this.editConfigExt.model.display.hideSources = !show;
    }

    pristine            = true;
    private editEmitted = false;

    annotationImpactMessage: string;

    annotations: AnnotationEntry[] = [];
    panels: PanelEntry[]           = [];

    sourceToConfigure: TimeSeriesSourceConfigurationExtended;

    overlayConfig = OverlayConfig.onTopDraggable({
                                                     showCloseButton: false,
                                                     width          : "850px",
                                                     maxWidth       : "95vw",
                                                     height         : "75vh"
                                                 });

    private hasChangeToEmit = false;
    private updateTrigger   = new Subject<void>();

    public readonly host = new TimeSeriesSourceHost(this);

    constructor(inj: Injector)
    {
        super(inj);

        this.subscribeToObservable(this.updateTrigger.pipe(debounceTime(650)), () => this.emitEditExt());
    }

    //--//

    updated(withDelay: boolean)
    {
        this.hasChangeToEmit = true;

        this.updatePristine();

        if (!this.pristine) this.checkAnnotationImpact();

        if (withDelay)
        {
            this.updateTrigger.next();
        }
        else
        {
            this.emitEditExt();
        }
    }

    private emitEditExt()
    {
        if (this.editConfigExt && this.hasChangeToEmit)
        {
            this.editConfigExt.model.panels = this.panels.map((panel: PanelEntry) => panel.generateModel());

            this.configEdit.emit(TimeSeriesChartConfigurationExtended.constructFrom(this.editConfigExt));
            this.editEmitted     = true;
            this.hasChangeToEmit = false;
        }
    }

    private updatePristine()
    {
        this.pristine = false;
        if (!this.equivalentAxes()) return;
        if (!this.equivalentSourceOrder()) return;

        this.editConfigExt.model.dataSources = this.editConfigExt.model.dataSources.filter((source) => source.pointBinding);
        for (let panel of this.panels)
        {
            for (let sourceExt of panel.sources)
            {
                if (!sourceExt.model.pointBinding) this.editConfigExt.model.dataSources.unshift(sourceExt.model);
            }
        }

        this.pristine = UtilsService.compareJson(TimeSeriesChartConfigurationExtended.cleanForComparison(this.m_configExt.model, true, true),
                                                 TimeSeriesChartConfigurationExtended.cleanForComparison(this.editConfigExt.model, true, true));
    }

    private equivalentAxes(): boolean
    {
        if (this.m_configExt.model.panels.length !== this.panels.length) return false;
        for (let i = 0; i < this.panels.length; i++)
        {
            let panel       = this.panels[i];
            let panelConfig = this.m_configExt.model.panels[i];
            if (!panel.left.isEquivalent(panelConfig.leftAxis)) return false;
            if (!panel.right.isEquivalent(panelConfig.rightAxis)) return false;
        }

        return true;
    }

    private equivalentSourceOrder(): boolean
    {
        let editPanelsExt = this.editConfigExt?.panelsExt;
        if (!editPanelsExt) return false;

        for (let i = 0; i < this.m_configExt.panelsExt.length; i++)
        {
            let panelExt     = this.m_configExt.panelsExt[i];
            let editPanelExt = editPanelsExt[i];
            if (panelExt?.sources?.length !== editPanelExt?.sources?.length) return false;

            for (let source of editPanelExt.sources)
            {
                let oldSource = this.m_configExt.getSourceByIdentifier(source.identifier);
                if (!oldSource || oldSource.panelSourceIndex !== source.panelSourceIndex) return false;
            }
        }

        return true;
    }

    private checkAnnotationImpact()
    {
        for (let annotationEntry of this.annotations)
        {
            annotationEntry.refreshValidity();
        }

        let allValid                 = this.annotations.every((annotationEntry) => annotationEntry.impact !== ChartChangeAnnotationImpact.Invalidated);
        this.annotationImpactMessage = allValid ? null : "These changes will invalidate some of your annotations.";
        this.markForCheck();
    }

    async write()
    {
        const editConfigExt = this.editConfigExt;

        // update panel models and then rebuild ext
        editConfigExt.model.panels      = this.panels.map((panel: PanelEntry) => panel.generateModel());
        editConfigExt.model.annotations = await mapInParallelNoNulls(this.annotations, async (annotationEntry) =>
        {
            let annotationModel = annotationEntry.model;
            switch (annotationEntry.impact)
            {
                case ChartChangeAnnotationImpact.Invalidated:
                    return null;

                case ChartChangeAnnotationImpact.UnitsChanged:
                    let sourceId   = annotationModel.sourceId;
                    let type       = annotationModel.type;
                    let oldUnits   = annotationEntry.unitsFactors;
                    let panelEntry = annotationEntry.panel;
                    if (type === Models.TimeSeriesAnnotationType.Point && sourceId)
                    {
                        let sourceExt = panelEntry.sources.find((source) => source.identifier === sourceId);
                        let axisEntry = sourceExt.model.axis === 0 ? panelEntry.left : panelEntry.right;
                        let newUnits  = axisEntry.groups.find((group) =>
                                                              {
                                                                  let units = EngineeringUnitsDescriptorExtended.extractFactors(group.selectedFactors);
                                                                  return UnitsService.areEquivalent(units, oldUnits);
                                                              })?.selectedFactors;
                        if (!newUnits) return null;

                        annotationModel.minY = await this.app.domain.units.convert(annotationModel.minY, oldUnits, EngineeringUnitsDescriptorExtended.extractFactors(newUnits));
                    }
                    else
                    {
                        let newUnits = EngineeringUnitsDescriptorExtended.extractFactors(panelEntry.left.displayFactors);

                        annotationModel.minY = await this.app.domain.units.convert(annotationModel.minY, oldUnits, newUnits);
                        if (type !== Models.TimeSeriesAnnotationType.Point)
                        {
                            annotationModel.maxY = await this.app.domain.units.convert(annotationModel.maxY, oldUnits, newUnits);
                        }
                    }

                    if (isNaN(annotationModel.minY)) return null;
                    if (type !== Models.TimeSeriesAnnotationType.Point && isNaN(annotationModel.maxY)) return null;
                    break;
            }

            return annotationModel;
        });

        this.editConfigExt = TimeSeriesChartConfigurationExtended.constructFrom(editConfigExt);
        this.configUpdate.emit(this.editConfigExt);
        this.revert(false);
    }

    revert(rebuild: boolean)
    {
        if (this.editConfigExt)
        {
            this.editConfigExt = undefined;
            if (this.editEmitted) this.configEdit.emit();

            if (rebuild) this.rebuild();
        }
    }

    async addSource(sourceInfo: AddTimeSeriesSourceEvent)
    {
        let source = await TimeSeriesSourceConfigurationExtended.resolveFromIdAndDimension(this.host, sourceInfo.sysId);

        source.model.timeOffset = sourceInfo.duration;

        await this.editConfigExt.applySourceChanges([source], [], true);

        this.sourceToConfigure = source;

        this.updated(false);
    }

    configureSource(source: InteractableSource)
    {
        this.sourceToConfigure = this.editConfigExt.sourcesExt.find((sourceExt) => sourceExt.identifier === source?.identifier);
        if (this.sourceToConfigure) this.sourceConfigurer.openOverlay();
    }

    async rebuild()
    {
        let configModel    = Models.TimeSeriesChartConfiguration.deepClone(this.m_configExt.model);
        this.editConfigExt = await TimeSeriesChartConfigurationExtended.newInstance(this.app, configModel, this.m_configExt.id, this.m_configExt.externalGraph);
        this.syncSources();

        this.panels = await mapInParallel(this.editConfigExt.panelsExt, async (panelExtended) =>
        {
            let panel     = new PanelEntry(this.host);
            panel.idx     = panelExtended.index;
            panel.sources = panelExtended.sources;

            let axes = [];
            for (let axisIndex = 0; axisIndex < 2; axisIndex++)
            {
                let axisExtended = panelExtended.axisByIndexExtended(axisIndex);
                let axisConfig   = axisExtended.model;
                let axis         = new AxisEntry(this.host, axisIndex, panel);
                axis.label       = axisConfig.label;
                axis.sources     = await panelExtended.filterSources(this.app.domain.units, axisExtended, undefined);

                for (let groupConfig of axisConfig.groupedFactors)
                {
                    let keyExt      = await this.app.domain.units.resolveRootDescriptor(groupConfig.keyFactors);
                    let selectedExt = await this.app.domain.units.resolveDescriptor(groupConfig.selectedFactors, false);
                    let group       = new GroupEntry(axis, keyExt, selectedExt, groupConfig.override);
                    if (groupConfig.keyFactors)
                    {
                        let groupSources = await panelExtended.filterSources(this.app.domain.units, axisExtended, groupConfig.keyFactors);
                        for (let source of groupSources) group.sources.push(source);
                    }

                    axis.groups.push(group);

                    if (UnitsService.areEquivalent(axisConfig.displayFactors, EngineeringUnitsDescriptorExtended.extractFactors(selectedExt)))
                    {
                        axis.factors = keyExt;
                    }
                }

                axes.push(axis);
            }

            panel.left  = axes[0];
            panel.right = axes[1];

            return panel;
        });

        let annotations = [];
        for (let annotationModel of this.m_configExt.model.annotations || [])
        {
            let panelIdx   = annotationModel.panel;
            let panelEntry = this.panels[panelIdx];
            if (panelEntry)
            {
                let units;
                if (annotationModel.type === Models.TimeSeriesAnnotationType.Point)
                {
                    let sourceId  = annotationModel.sourceId;
                    let sourceExt = this.m_configExt.sourcesExt.find((source) => source.identifier === sourceId);
                    if (sourceExt)
                    {
                        let axis  = sourceExt.model.axis === 0 ? panelEntry.left : panelEntry.right;
                        let group = axis.groups.find((group) => group.sources.some((source) => source.identifier === sourceId));
                        if (group) units = EngineeringUnitsDescriptorExtended.extractFactors(group.selectedFactors);
                    }
                }

                if (!units) units = EngineeringUnitsDescriptorExtended.extractFactors(panelEntry.left.displayFactors);

                annotations.push(new AnnotationEntry(panelEntry, annotationModel, units));
            }
        }

        this.annotations = annotations;
        this.pristine    = true;

        this.markForCheck();
    }

    private syncSources()
    {
        for (let formerSourceExt of this.m_configExt.sourcesExt)
        {
            let editSourceExt = this.editConfigExt?.getSourceByIdentifier(formerSourceExt.identifier);
            if (editSourceExt)
            {
                editSourceExt.model.color = formerSourceExt.model.color;
                editSourceExt.copySourceData(formerSourceExt);
            }
        }
    }

    //--//

    openOverlay()
    {
        this.editEmitted = false;
        this.syncSources();
        this.overlay.openOverlay();
    }

    closeOverlay(): void
    {
        this.overlay.closeOverlay();
    }

    toggleOverlay(open?: boolean): void
    {
        if (open === undefined) open = !this.isOpen();

        if (open)
        {
            this.openOverlay();
        }
        else
        {
            this.closeOverlay();
        }
    }

    isOpen(): boolean
    {
        return this.overlay.isOpen();
    }
}
