import {animate, style, transition, trigger} from "@angular/animations";
import {ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import {TimeSeriesAxisConfigurationExtended, TimeSeriesSource, TimeSeriesSourceHost} from "app/customer/visualization/time-series-utils";
import {DeviceElementExtended} from "app/services/domain/assets.service";

import * as SharedSvc from "app/services/domain/base.service";
import {EngineeringUnitsDescriptorExtended} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {ScatterPlotPropertyTuple} from "framework/ui/charting/core/data-sources";

import {ControlOption} from "framework/ui/control-option";
import {OverlayController} from "framework/ui/overlays/overlay-base";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {mapInParallel} from "framework/utils/concurrency";
import {Subject} from "rxjs";
import {debounceTime} from "rxjs/operators";

@Component({
               selector       : "o3-scatter-plot-configuration",
               templateUrl    : "./scatter-plot-configuration.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush,
               animations     : [
                   trigger("expand", [
                       transition(":enter", [
                           style({height: 0}),
                           animate(".2s ease-out", style({height: "*"}))
                       ]),
                       transition(":leave", [
                           animate(".2s ease-out", style({height: "0"}))
                       ])
                   ])
               ]
           })
export class ScatterPlotConfigurationComponent extends SharedSvc.BaseApplicationComponent implements OverlayController
{
    private readonly host: TimeSeriesSourceHost;

    @Input() dimension: string = DeviceElementExtended.PRESENT_VALUE;
    @Input() tuples: Models.ScatterPlotSourceTuple[][];
    @Input() axes: ScatterPlotPropertyTuple<Models.TimeSeriesAxisConfiguration>[];

    private m_editAxes: ScatterPlotPropertyTuple<Models.TimeSeriesAxisConfiguration>[];

    pristine: boolean          = true;
    private hasChange: boolean = false;
    private updateTrigger      = new Subject<void>();

    selectedPanel: ScatterPlotConfigurationPanel;
    panelOptions: ControlOption<ScatterPlotConfigurationPanel>[];

    panels: ScatterPlotConfigurationPanel[];

    overlayConfig = OverlayConfig.onTopDraggable({minWidth: 465});

    secondaryButtonText: "Close" | "Cancel" = "Close";

    @ViewChild(StandardFormOverlayComponent, {static: true}) overlay: StandardFormOverlayComponent;

    @Output() axesEdit   = new EventEmitter<ScatterPlotPropertyTuple<Models.TimeSeriesAxisConfiguration>[]>();
    @Output() axesUpdate = new EventEmitter<ScatterPlotPropertyTuple<Models.TimeSeriesAxisConfiguration>[]>();

    constructor(inj: Injector)
    {
        super(inj);

        this.host = new TimeSeriesSourceHost(this);

        this.subscribeToObservable(this.updateTrigger.pipe(debounceTime(650)), () => this.emitEditAxes());
    }

    private async rebuild()
    {
        if (this.axes)
        {
            this.m_editAxes = this.axes.map((axisTuple) =>
                                            {
                                                if (!axisTuple) return null;

                                                return new ScatterPlotPropertyTuple(axisTuple.timestamp,
                                                                                    Models.TimeSeriesAxisConfiguration.deepClone(axisTuple.valueX),
                                                                                    Models.TimeSeriesAxisConfiguration.deepClone(axisTuple.valueY),
                                                                                    Models.TimeSeriesAxisConfiguration.deepClone(axisTuple.valueZ));
                                            });
            await this.generateConfigurablePanels();

            this.selectedPanel = this.panels.find((panel) => panel.valid) || this.panels[0];
            this.markForCheck();
        }
    }

    private async generateConfigurablePanels()
    {
        let representativeTuples: ScatterPlotPropertyTuple<TimeSeriesSource>[] = await mapInParallel(this.tuples, async (tuplesPerPanel) =>
        {
            let tuple = tuplesPerPanel[0];
            if (!tuple) return null;

            let sourcePromises = [
                TimeSeriesSource.sourceFromId(this.host, tuple.sourceX.deviceElementId),
                TimeSeriesSource.sourceFromId(this.host, tuple.sourceY.deviceElementId)
            ];

            let zId = tuple.sourceZ?.deviceElementId;
            if (zId) sourcePromises.push(TimeSeriesSource.sourceFromId(this.host, tuple.sourceZ.deviceElementId));

            let sources = await Promise.all(sourcePromises);
            return new ScatterPlotPropertyTuple<TimeSeriesSource>(0, sources[0], sources[1], sources[2]);
        });

        this.panels = await mapInParallel(representativeTuples, async (sourceTuple,
                                                                       idx) =>
        {
            if (!sourceTuple) return null;

            let schemaPromises = [
                sourceTuple.valueX.meta.point.getSchemaProperty(this.dimension),
                sourceTuple.valueY.meta.point.getSchemaProperty(this.dimension)
            ];

            if (sourceTuple.valueZ) schemaPromises.push(sourceTuple.valueZ.meta.point.getSchemaProperty(this.dimension));

            let axesTuple   = this.m_editAxes[idx];
            let xAxis       = axesTuple.valueX;
            let yAxis       = axesTuple.valueY;
            let xConfigAxis = new ScatterPlotConfigurationAxis(xAxis, await this.getDescriptor(xAxis.displayFactors));
            let yConfigAxis = new ScatterPlotConfigurationAxis(yAxis, await this.getDescriptor(yAxis.displayFactors));

            let zSource = sourceTuple.valueZ;
            let zAxis   = axesTuple.valueZ;
            let zConfigAxis;
            if (zSource != null && zAxis != null)
            {
                let zUnit = zAxis.displayFactors || await zSource.currentUnits(this.dimension);

                zConfigAxis = new ScatterPlotConfigurationAxis(zAxis, await this.getDescriptor(zUnit));
            }

            let schemas              = await Promise.all(schemaPromises);
            xConfigAxis.unitsFactors = schemas[0].unitsFactors;
            yConfigAxis.unitsFactors = schemas[1].unitsFactors;
            if (zConfigAxis) zConfigAxis.unitsFactors = schemas[2]?.unitsFactors;

            return new ScatterPlotConfigurationPanel(xConfigAxis, yConfigAxis, zConfigAxis);
        });

        this.panelOptions = this.panels.map((panel,
                                             idx) => new ControlOption(panel, `Panel ${idx + 1}`));

        this.detectChanges();
    }

    revertEdits()
    {
        if (!this.m_editAxes) return;
        this.m_editAxes = null;
        this.submitEdits();
    }

    submitEdits()
    {
        this.axesUpdate.emit(this.m_editAxes);
        this.m_editAxes = null;
    }

    private async getDescriptor(factors: Models.EngineeringUnitsFactors): Promise<EngineeringUnitsDescriptorExtended>
    {
        return this.host.app.domain.units.resolveDescriptor(factors, false);
    }

    updated(delay: boolean)
    {
        this.hasChange = true;

        this.updatePristine();
        this.secondaryButtonText = this.pristine ? "Close" : "Cancel";

        if (delay)
        {
            this.updateTrigger.next();
        }
        else
        {
            this.emitEditAxes();
        }
    }

    private updatePristine()
    {
        if (this.axes)
        {
            for (let panelIdx = 0; panelIdx < this.axes.length; panelIdx++)
            {
                if (!this.isPristinePanel(panelIdx))
                {
                    this.pristine = false;
                    return;
                }
            }
        }

        this.pristine = true;
    }

    private isPristinePanel(idx: number): boolean
    {
        let pre  = this.axes[idx];
        let curr = this.m_editAxes[idx];

        if (!TimeSeriesAxisConfigurationExtended.areEquivalent(this.app.domain.units, pre.valueX, curr.valueX, false)) return false;
        if (!TimeSeriesAxisConfigurationExtended.areEquivalent(this.app.domain.units, pre.valueY, curr.valueY, false)) return false;
        if (pre.valueZ && !TimeSeriesAxisConfigurationExtended.areEquivalent(this.app.domain.units, pre.valueZ, curr.valueZ, false)) return false;

        return true;
    }

    private emitEditAxes()
    {
        if (this.hasChange && this.m_editAxes)
        {
            this.hasChange = false;
            this.axesEdit.emit(this.m_editAxes);
        }
    }

    openOverlay()
    {
        this.rebuild();

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

class ScatterPlotConfigurationPanel
{
    get valid(): boolean
    {
        return this.xAxis && !!this.yAxis;
    }

    constructor(public readonly xAxis: ScatterPlotConfigurationAxis,
                public readonly yAxis: ScatterPlotConfigurationAxis,
                public readonly zAxis?: ScatterPlotConfigurationAxis)
    {
    }
}

class ScatterPlotConfigurationAxis
{
    unitsFactors: Models.EngineeringUnitsFactors;

    get selectedFactors()
    {
        return this.m_selectedFactors;
    }

    set selectedFactors(factors: EngineeringUnitsDescriptorExtended)
    {
        this.m_selectedFactors    = factors;
        this.model.displayFactors = EngineeringUnitsDescriptorExtended.extractFactors(factors);
    }

    constructor(public model: Models.TimeSeriesAxisConfiguration,
                private m_selectedFactors: EngineeringUnitsDescriptorExtended)
    {
    }
}
