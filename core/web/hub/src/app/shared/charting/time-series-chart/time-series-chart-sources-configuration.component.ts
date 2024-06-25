import {animate, style, transition, trigger} from "@angular/animations";
import {CdkDragDrop, moveItemInArray} from "@angular/cdk/drag-drop";
import {CdkScrollable} from "@angular/cdk/overlay";
import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, Output, QueryList, ViewChild, ViewChildren} from "@angular/core";

import {InteractableSource, TimeSeriesAxisConfigurationExtended, TimeSeriesPanelConfigurationExtended, TimeSeriesSourceConfigurationExtended, TimeSeriesSourceHost} from "app/customer/visualization/time-series-utils";
import * as SharedSvc from "app/services/domain/base.service";
import {EngineeringUnitsDescriptorExtended, UnitsService} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {SourceAction, SourceChipComponent} from "app/shared/charting/source-chip/source-chip.component";
import {TimeSeriesChartComponent} from "app/shared/charting/time-series-chart/time-series-chart.component";
import {ChartChangeAnnotationImpact} from "app/shared/charting/time-series-container/common";

import {UtilsService} from "framework/services/utils.service";
import {DragScroller, ScrollerConfig, ScrollState} from "framework/ui/utils/drag-scroller";

@Component({
               selector       : "o3-time-series-chart-sources-configuration",
               templateUrl    : "./time-series-chart-sources-configuration.component.html",
               styleUrls      : ["./time-series-chart-sources-configuration.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush,
               animations     : [
                   trigger("expand", [
                       transition(":enter", [
                           style({height: 0}),
                           animate(".2s ease-out", style({height: "*"}))
                       ]),
                       transition(":leave",
                                  [animate(".2s ease-out", style({height: "0"}))])
                   ])
               ]
           })
export class TimeSeriesChartSourcesConfigurationComponent extends SharedSvc.BaseApplicationComponent
{
    private static ids: number    = 0;
    public readonly id: number    = TimeSeriesChartSourcesConfigurationComponent.ids++;
    public readonly addNewPanelId = `add-new-${this.id}`;

    @Input() host: TimeSeriesSourceHost;
    @Input() chartElement: TimeSeriesChartComponent;
    @Input() panels: PanelEntry[] = [];
    @Input() annotations: AnnotationEntry[];

    @ViewChild(CdkScrollable, {static: true}) scrollContainer: CdkScrollable;

    @ViewChildren("test_panel", {read: ElementRef}) test_panels: QueryList<ElementRef>;
    @ViewChildren("test_source") test_sources: QueryList<SourceChipComponent>;
    @ViewChildren("test_sourceDrag", {read: ElementRef}) test_sourceDrags: QueryList<ElementRef>;
    @ViewChildren("test_toRightAxis", {read: ElementRef}) test_toRightAxis: QueryList<ElementRef>;
    @ViewChild("test_newPanel", {
        read  : ElementRef,
        static: true
    }) test_newPanel: ElementRef;

    @Output() updated         = new EventEmitter<boolean>();
    @Output() configureSource = new EventEmitter<InteractableSource>();

    sourceActions: SourceAction[] = [new SourceAction("settings", (source) => this.configureSource.emit(source))];

    scroller: DragScroller;
    draggingSource: boolean;

    public ngOnInit()
    {
        super.ngOnInit();

        this.scroller = new DragScroller(new ScrollerConfig(0.5, this.scrollContainer));
    }

    //--//

    getConnectedDropLists(): string[]
    {
        let connectedDropLists = this.panels.map((panel) => this.computeDropListId(panel));
        connectedDropLists.push(this.addNewPanelId);
        return connectedDropLists;
    }

    computeDropListId(panel: PanelEntry): string
    {
        return `sources-${this.id}-${panel.idx}`;
    }

    //--//

    isOnAxis(axis: AxisEntry,
             source: TimeSeriesSourceConfigurationExtended): boolean
    {
        return axis.id === source.model.axis;
    }

    movePanel(event: CdkDragDrop<PanelEntry[]>)
    {
        moveItemInArray(this.panels, event.previousIndex, event.currentIndex);
        this.reindexPanels();
    }

    reindexPanels(): void
    {
        //
        // First, mark all the annotation as not belonging to a valid panel.
        //
        for (let annotationEntry of this.annotations)
        {
            annotationEntry.panel.idx = -1;
        }

        for (let i = 0; i < this.panels.length; i++)
        {
            this.panels[i].setIdx(i);
        }

        //
        // Then fetch the idx from the panel. If the panel is still valid, it will have a correct index.
        //
        for (let annotationEntry of this.annotations)
        {
            annotationEntry.model.panel = annotationEntry.panel.idx;
        }

        this.updated.emit();
    }

    //--//

    onSourceDragStart()
    {
        this.draggingSource = true;
        this.scroller.setUpForScroll();
    }

    onSourceDragMove(cursorY: number)
    {
        this.scroller.processState(cursorY);
    }

    onSourceDragEnd()
    {
        this.draggingSource = false;
        this.scroller.updateState(ScrollState.Stationary);
    }

    private getSourceDragInfo(event: CdkDragDrop<PanelEntry>): SourceDragInfo
    {
        return new SourceDragInfo(event.previousContainer.data, event.container.data, event.item.data);
    }

    async assignToAxis(panel: PanelEntry,
                       axisId: number,
                       sourceExt: TimeSeriesSourceConfigurationExtended)
    {
        let targetAxis = axisId === 0 ? panel.left : panel.right;
        await panel.assignToAxis(sourceExt, targetAxis);
        this.updated.emit();
    }

    async moveSource(event: CdkDragDrop<PanelEntry>)
    {
        let dragInfo = this.getSourceDragInfo(event);
        if (dragInfo.previousPanel !== dragInfo.panel)
        {
            await this.moveSourceToPanel(dragInfo);

            this.markForCheck();
        }
        else
        {
            dragInfo.panel.moveSource(event);

            this.updated.emit();
        }
    }

    async addNewPanel(event: CdkDragDrop<PanelEntry>)
    {
        let dragInfo = this.getSourceDragInfo(event);

        let prevAxis          = dragInfo.sourceExt.model.axis === 0 ? dragInfo.previousPanel.left : dragInfo.previousPanel.right;
        let prevGroup         = await prevAxis.getEquivalentUnitsGroup(dragInfo.sourceExt);
        let prevGroupSelected = prevGroup?.keyFactors == prevAxis.factors;

        await this.removeSourceFromPanel(dragInfo.previousPanel, dragInfo.sourceExt);

        // Make a new panel, configure and add it
        let panel = new PanelEntry(this.host);
        await panel.addSource(dragInfo.sourceExt);
        let newGroup = dragInfo.sourceExt.model.axis === 0 ? panel.left.groups[0] : panel.right.groups[0];
        newGroup.copyState(prevGroup, prevGroupSelected);
        this.panels.push(panel);

        this.reindexPanels();
    }

    private async moveSourceToPanel(dragInfo: SourceDragInfo)
    {
        // Remove the source from the original panel
        await this.removeSourceFromPanel(dragInfo.previousPanel, dragInfo.sourceExt);

        // Add the source to the new panel
        await dragInfo.panel.addSource(dragInfo.sourceExt);

        this.reindexPanels();
    }

    private async removeSourceFromPanel(panel: PanelEntry,
                                        source: TimeSeriesSourceConfigurationExtended)
    {
        // Remove the source from the panel
        await panel.removeSource(source);

        // Remove the panel if it is empty
        if (panel.sources.length === 0) this.panels.splice(this.panels.indexOf(panel), 1);
    }
}

class SourceDragInfo
{
    constructor(public readonly previousPanel: PanelEntry,
                public readonly panel: PanelEntry,
                public readonly sourceExt: TimeSeriesSourceConfigurationExtended)
    {}
}

export class PanelEntry
{
    left: AxisEntry;
    right: AxisEntry;
    idx: number = 0;

    sources: TimeSeriesSourceConfigurationExtended[] = [];

    constructor(host: TimeSeriesSourceHost)
    {
        this.left  = new AxisEntry(host, 0, this);
        this.right = new AxisEntry(host, 1, this);
    }

    setIdx(idx: number): void
    {
        this.idx = idx;
        for (let source of this.sources) source.model.panel = this.idx;
    }

    async assignToAxis(source: TimeSeriesSourceConfigurationExtended,
                       destinationAxis: AxisEntry)
    {
        if (destinationAxis)
        {
            let sourceAxis = destinationAxis === this.left ? this.right : this.left;
            await sourceAxis.removeSource(source);
            await destinationAxis.addSource(source);
        }
    }

    private findSourceIndex(source: TimeSeriesSourceConfigurationExtended): number
    {
        return this.sources.findIndex((sourceExt) => sourceExt.model === source.model);
    }

    moveSource(event: CdkDragDrop<PanelEntry>)
    {
        moveItemInArray(this.sources, event.previousIndex, event.currentIndex);

        this.reindexSources();
    }

    reindexSources()
    {
        for (let i = 0; i < this.sources.length; i++) this.sources[i].panelSourceIndex = i;
    }

    async addSource(sourceExt: TimeSeriesSourceConfigurationExtended)
    {
        // Add to sources if not already added
        if (this.findSourceIndex(sourceExt) < 0)
        {
            // Add to the list of sources for this panel
            sourceExt.panelSourceIndex = this.sources.length;
            this.sources.push(sourceExt);

            let source = sourceExt.model;

            // Update the source's panel
            source.panel = this.idx;

            // Add to an axis if one is assigned
            let axisIdx = source.axis;
            if (axisIdx === 0)
            {
                await this.left.addSource(sourceExt);
            }
            else if (axisIdx === 1)
            {
                await this.right.addSource(sourceExt);
            }
        }
    }

    async removeSource(source: TimeSeriesSourceConfigurationExtended)
    {
        // Remove from sources if it exists
        let index = this.findSourceIndex(source);
        if (index >= 0)
        {
            this.sources.splice(index, 1);
            this.reindexSources();

            // Remove from axis if one was assigned
            let axisIdx = source.model.axis;
            if (axisIdx === 0)
            {
                await this.left.removeSource(source);
            }
            else if (axisIdx === 1)
            {
                await this.right.removeSource(source);
            }
        }
    }

    generateModel(): Models.TimeSeriesPanelConfiguration
    {
        let panel       = TimeSeriesPanelConfigurationExtended.newModel();
        panel.leftAxis  = this.left.generateModel();
        panel.rightAxis = this.right.generateModel();
        return panel;
    }
}

export class AxisEntry
{
    label: string;
    factors: EngineeringUnitsDescriptorExtended      = null;
    sources: TimeSeriesSourceConfigurationExtended[] = [];
    groups: GroupEntry[]                             = [];
    override: Models.ToggleableNumericRange          = new Models.ToggleableNumericRange();

    get displayFactors(): EngineeringUnitsDescriptorExtended
    {
        let relevantGroup = this.groups.find((group) => this.factors === group.keyFactors);
        return relevantGroup?.selectedFactors;
    }

    constructor(private host: TimeSeriesSourceHost,
                public id: number,
                public panel: PanelEntry)
    {
    }

    private findSourceIndex(source: TimeSeriesSourceConfigurationExtended): number
    {
        return this.sources.findIndex((sourceExt) => sourceExt.model === source.model);
    }

    async getEquivalentUnitsGroup(sourceExt: TimeSeriesSourceConfigurationExtended): Promise<GroupEntry>
    {
        const rootFactors = await this.host.app.domain.units.resolveRootFactors(sourceExt.unitsFactors);
        return this.groups.find((group) => UnitsService.areEquivalent(EngineeringUnitsDescriptorExtended.extractFactors(group.keyFactors), rootFactors));
    }

    async addSource(source: TimeSeriesSourceConfigurationExtended)
    {
        // Only add the source if it is not already added
        if (this.findSourceIndex(source) < 0)
        {
            // Add the source to the axis
            this.sources.push(source);
            // Update the source axis
            source.model.axis = this.id;

            // Check if the unit factors is new for this axis
            let unitsFactors = source.unitsFactors;
            let sourceGroup  = await this.getEquivalentUnitsGroup(source);
            if (!sourceGroup)
            {
                // Add a new factors
                let keyExt      = await this.host.app.domain.units.resolveRootDescriptor(unitsFactors);
                let selectedExt = await this.host.app.domain.units.resolveDescriptor(unitsFactors, false);
                let group       = new GroupEntry(this, keyExt, selectedExt, null);
                group.sources.push(source);
                this.groups.push(group);

                // If this is the only factors, select it
                if (this.groups.length === 1) this.factors = this.groups[0].keyFactors;
            }
            else
            {
                // Get the correct factors and add the source to it
                sourceGroup.sources.push(source);
            }
        }
    }

    async removeSource(source: TimeSeriesSourceConfigurationExtended)
    {
        let sourceIndex = this.findSourceIndex(source);
        if (sourceIndex >= 0)
        {
            this.sources.splice(sourceIndex, 1);

            // update group of removed source
            const group = await this.getEquivalentUnitsGroup(source);
            if (group)
            {
                group.removeSource(source);

                if (group.sources.length == 0)
                {
                    // Remove this now-empty group
                    this.groups.splice(this.groups.indexOf(group), 1);

                    // find new group to associate with axis
                    let newGroup = this.groups.find((group) => group.keyFactors) || this.groups[0];
                    if (newGroup) this.factors = newGroup.keyFactors;
                }
            }
        }
    }

    isEquivalent(model: Models.TimeSeriesAxisConfiguration): boolean
    {
        if (!UtilsService.equivalentStrings(this.label, model.label)) return false;

        if (model.groupedFactors.length !== this.groups.length) return false;

        for (let i = 0; i < this.groups.length; i++)
        {
            let group = this.groups[i];

            if (!group.isEquivalent(model.groupedFactors[i])) return false;
            if (GroupEntry.sameFactorsHelper(this.factors, EngineeringUnitsDescriptorExtended.extractFactors(group.keyFactors)) &&
                !GroupEntry.sameFactorsHelper(group.selectedFactors, model.displayFactors))
            {
                return false;
            }
        }

        return true;
    }

    generateModel(): Models.TimeSeriesAxisConfiguration
    {
        let axis = TimeSeriesAxisConfigurationExtended.newModel();

        axis.label = this.label;

        for (let group of this.groups)
        {
            if (group.keyFactors == this.factors)
            {
                axis.displayFactors = EngineeringUnitsDescriptorExtended.extractFactors(group.selectedFactors);
            }

            axis.groupedFactors.push(group.generateModel());
        }

        return axis;
    }
}

export class GroupEntry
{
    sources: TimeSeriesSourceConfigurationExtended[] = [];

    readonly label: string;

    unitsFactors: Models.EngineeringUnitsFactors;

    get dimensionlessUnits(): boolean
    {
        return this.keyFactors?.noDimensions;
    }

    constructor(public axis: AxisEntry,
                public readonly keyFactors: EngineeringUnitsDescriptorExtended,
                public selectedFactors: EngineeringUnitsDescriptorExtended,
                public override: Models.ToggleableNumericRange)
    {
        let labelStart    = keyFactors?.model.family || "Unspecified";
        this.label        = labelStart + " units";
        this.unitsFactors = EngineeringUnitsDescriptorExtended.extractFactors(this.keyFactors);
    }

    copyState(other: GroupEntry,
              includeLabel: boolean)
    {
        if (other)
        {
            this.override = other.override;
            if (includeLabel) this.axis.label = other.axis.label;
        }
    }

    removeSource(source: TimeSeriesSourceConfigurationExtended): void
    {
        let index = this.sources.findIndex((sourceExt) => sourceExt.model === source.model);
        if (index >= 0)
        {
            this.sources.splice(index, 1);
        }
    }

    isEquivalent(model: Models.TimeSeriesAxisGroupConfiguration): boolean
    {
        if (!GroupEntry.sameFactorsHelper(this.keyFactors, model.keyFactors)) return false;
        if (!GroupEntry.sameFactorsHelper(this.selectedFactors, model.selectedFactors)) return false;
        return UtilsService.compareJson(this.override, model.override);
    }

    generateModel(): Models.TimeSeriesAxisGroupConfiguration
    {
        return Models.TimeSeriesAxisGroupConfiguration.newInstance({
                                                                       keyFactors     : EngineeringUnitsDescriptorExtended.extractFactors(this.keyFactors),
                                                                       selectedFactors: EngineeringUnitsDescriptorExtended.extractFactors(this.selectedFactors),
                                                                       override       : Models.ToggleableNumericRange.deepClone(this.override)
                                                                   });
    }

    public static sameFactorsHelper(unitsExt: EngineeringUnitsDescriptorExtended,
                                    engineeringFactors: Models.EngineeringUnitsFactors): boolean
    {
        if (!unitsExt && !engineeringFactors) return true;
        if (!unitsExt || !engineeringFactors) return false;
        return unitsExt.sameFactors(engineeringFactors);
    }
}

export class AnnotationEntry
{
    impact: ChartChangeAnnotationImpact;

    constructor(readonly panel: PanelEntry,
                readonly model: Models.TimeSeriesAnnotationConfiguration,
                readonly unitsFactors: Models.EngineeringUnitsFactors)
    {
        this.refreshValidity();
    }

    refreshValidity(): void
    {
        this.impact = this.computeValidity();
    }

    private computeValidity(): ChartChangeAnnotationImpact
    {
        let panelEntry = this.panel;
        if (panelEntry.idx < 0) return ChartChangeAnnotationImpact.Invalidated;

        let sourceId = this.model.sourceId;
        if (this.model.type === Models.TimeSeriesAnnotationType.Point && sourceId)
        {
            let relevantSource = panelEntry.sources.find((sourceExt) => sourceExt.identifier === sourceId);
            if (!relevantSource) return ChartChangeAnnotationImpact.Invalidated;

            let axisEntry  = relevantSource.model.axis === 0 ? panelEntry.left : panelEntry.right;
            let groupEntry = axisEntry.groups.find((group) =>
                                                   {
                                                       let groupUnits = EngineeringUnitsDescriptorExtended.extractFactors(group.selectedFactors);
                                                       return UnitsService.areEquivalent(groupUnits, this.unitsFactors);
                                                   });
            if (groupEntry)
            {
                if (!groupEntry.selectedFactors.sameFactors(this.unitsFactors)) return ChartChangeAnnotationImpact.UnitsChanged;
            }
            else
            {
                return ChartChangeAnnotationImpact.Invalidated;
            }
        }
        else
        {
            let axisEntry      = panelEntry.left;
            let displayFactors = axisEntry.displayFactors;
            let newFactors     = EngineeringUnitsDescriptorExtended.extractFactors(displayFactors);

            let relevantSources = axisEntry.sources.filter((source) => UnitsService.areEquivalent(this.unitsFactors, source.unitsFactors));
            if (relevantSources.length === 0) return ChartChangeAnnotationImpact.Invalidated;

            if (UnitsService.areEquivalent(newFactors, this.unitsFactors))
            {
                if (!displayFactors || !displayFactors.sameFactors(this.unitsFactors)) return ChartChangeAnnotationImpact.UnitsChanged;
            }
            else
            {
                return ChartChangeAnnotationImpact.Invalidated;
            }
        }

        return ChartChangeAnnotationImpact.None;
    }
}
