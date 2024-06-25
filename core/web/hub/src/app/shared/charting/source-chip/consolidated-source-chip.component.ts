import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, QueryList, ViewChild, ViewChildren} from "@angular/core";

import {InteractableSource, InteractableSourcesChart} from "app/customer/visualization/time-series-utils";
import * as SharedSvc from "app/services/domain/base.service";
import {SourceAction, SourceChipComponent} from "app/shared/charting/source-chip/source-chip.component";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ColorGradientContinuous, ColorGradientStop} from "framework/ui/charting/core/colors";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {Future} from "framework/utils/concurrency";
import {Debouncer} from "framework/utils/debouncers";

import {Subscription} from "rxjs";

@Component({
               selector       : "o3-consolidated-source-chip[chart], o3-consolidated-source-chip[charts]",
               templateUrl    : "./consolidated-source-chip.component.html",
               styleUrls      : [
                   "./source-chip.component.scss",
                   "./consolidated-source-chip.component.scss"
               ],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ConsolidatedSourceChipComponent extends SharedSvc.BaseApplicationComponent
{
    static ngAcceptInputType_disableConsolidatedRipple: boolean | "";

    private static readonly maxUnconsolidatedSources: number = 10;

    private m_updateThrottler = new Debouncer(30, () => this.update());

    sourceIdentifier: (index: number,
                       item: InteractableSourceExtended) => string = (idx,
                                                                      source) => source.source.identifier;

    get noActiveSources(): boolean
    {
        return !this.consolidated && this.sourceExts.every((sourceExt) => !this.showSource(sourceExt, false));
    }

    needsUpdate: boolean;
    sourceExts: InteractableSourceExtended[] = [];
    sourceFilter: string                     = "";
    numShowingSources: number                = 0;

    consolidated: boolean;
    reflowing: boolean = false;

    sourceIndicatorStyle: string;

    private m_chartUpdatedSubs: Subscription[];

    private m_charts: InteractableSourcesChart[];

    @Input() set chart(chart: InteractableSourcesChart)
    {
        if (chart)
        {
            this.m_charts = [chart];
            this.queueUpdate();
            this.updateSubs();
        }
    }

    @Input() set charts(charts: InteractableSourcesChart[])
    {
        charts = charts?.filter((chart) => !!chart);
        if (charts?.length)
        {
            this.m_charts = charts;
            this.queueUpdate();
            this.updateSubs();
        }
    }

    @Input() actions: SourceAction[]         = [];
    @Input() deletable: boolean              = true;
    @Input() hideSecondaryText: boolean      = false;
    @Input() sortSources: boolean            = true;
    @Input() embedded: boolean               = false;
    @Input() @CoerceBoolean() disableConsolidatedRipple: boolean;
    @Input() catchClicks: boolean            = false;
    @Input() consolidatedChipTooltip: string;
    @Input() dataSourceType: string          = "Data Source";
    @Input() showSingleChipTooltips: boolean = true;

    @Output() opened             = new EventEmitter<void>();
    @Output() consolidatedChange = new EventEmitter<boolean>();
    @Output() deleteTriggered    = new EventEmitter<InteractableSource>();
    @Output() deleteCancelled    = new EventEmitter<InteractableSource>();

    @ViewChild(OverlayComponent, {static: true}) sourcesOverlay: OverlayComponent;
    @ViewChildren("chip") sourceChips: QueryList<SourceChipComponent>;
    @ViewChildren("test_overlayChip") test_overlayChips: QueryList<SourceChipComponent>;
    @ViewChild("test_consolidated", {read: ElementRef}) test_consolidated: ElementRef;

    sourcesOverlayConfig = OverlayConfig.onTopDraggable({
                                                            width    : 300,
                                                            maxHeight: 500
                                                        });

    constructor(inj: Injector,
                private m_element: ElementRef<HTMLElement>)
    {
        super(inj);
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();

        this.updateConsolidated();
    }

    isDeletable(source: InteractableSourceExtended): boolean
    {
        return this.deletable && source.containingChart.isDeletable(source.source.identifier);
    }

    private queueUpdate()
    {
        this.m_updateThrottler.invoke();
    }

    private updateSubs()
    {
        if (this.m_chartUpdatedSubs)
        {
            this.m_chartUpdatedSubs.forEach((sub) => sub.unsubscribe());
            this.m_chartUpdatedSubs = null;
        }

        if (this.m_charts)
        {
            this.m_chartUpdatedSubs = this.m_charts.map((chart) => this.subscribeToObservable(chart.chartUpdated, (sourcesChanged: boolean) =>
            {
                this.needsUpdate ||= !!sourcesChanged;
                this.queueUpdate();
            }));
        }
    }

    showSource(source: InteractableSourceExtended,
               inOverlay: boolean): boolean
    {
        if (!source.source.valid) return false;
        if (source.deleted) return false;
        if (inOverlay) return source.matchesFilter;
        if (this.reflowing) return true;

        return !this.consolidated;
    }

    async toggleSourcesDropdown()
    {
        let overlayOpen = this.sourcesOverlay.isOpen;
        if (!overlayOpen)
        {
            this.sourceFilter = "";
            this.updateFilteredSources();
        }

        this.sourcesOverlay.toggleOverlay();

        // mat-chip catches click events by default. Create new click if catchClicks isn't set
        if (!this.catchClicks) this.m_element.nativeElement.click();
    }

    triggerDeletion(source: InteractableSourceExtended)
    {
        if (this.deletable) this.deleteTriggered.emit(source.source);
    }

    getPrimaryDataSourcesMessage(includeCount: boolean): string
    {
        if (!this.m_charts) return "";

        let numSources = this.sourceExts.length;
        return UtilsService.pluralize(`${includeCount ? numSources + " " : ""}${this.dataSourceType}`, numSources);
    }

    async update()
    {
        let sources                                       = [];
        let chartLookup: Lookup<InteractableSourcesChart> = {};
        for (let chart of this.m_charts)
        {
            for (let source of chart.getSources(null, false))
            {
                if (source.valid)
                {
                    sources.push(source);
                    chartLookup[source.identifier] = chart;
                }
            }
        }

        let currSources = this.sourceExts.map((sourceExt) => sourceExt.source);
        if (this.needsUpdate || !UtilsService.compareArraysAsSets(currSources, sources))
        {
            this.needsUpdate  = false;
            this.sourceFilter = "";

            await this.updateSources(sources, chartLookup);
            this.updateFilteredSources();
        }
        else
        {
            this.updateIndicatorBackground();
        }
    }

    private async updateSources(sources: InteractableSource[],
                                chartLookup: Lookup<InteractableSourcesChart>)
    {
        let sourceExts = sources.map((source) => new InteractableSourceExtended(source, chartLookup[source.identifier]));
        if (this.sortSources)
        {
            sourceExts.sort((sourceA,
                             sourceB) => UtilsService.compareStrings(sourceA.filterString, sourceB.filterString, true));

        }

        this.sourceExts = sourceExts;

        this.updateIndicatorBackground();

        this.measureSourceChips();
    }

    private updateIndicatorBackground()
    {
        if (!this.sourceExts.length) return;

        let sourceColors = this.sourceExts.map((sourceExt) => sourceExt.source.colorStops || sourceExt.source.color);
        let colors: string[];
        let colorSegments: ColorGradientStop[][];
        if (typeof sourceColors[0] == "string")
        {
            colors = <string[]>sourceColors;
        }
        else if (sourceColors[0]?.length === 1)
        {
            colors = (<ColorGradientStop[][]>sourceColors).map((color) => color[0].color);
        }
        else
        {
            colorSegments = <ColorGradientStop[][]>sourceColors;
        }

        let edgeBuffer = 100 * (1 - SourceChipComponent.sourceIndicatorDiameterDecimal) / 2;
        if (colors)
        {
            let style = colors[0] || "grey";
            if (colors.length > 1)
            {
                colors         = colors.slice(0, 10);
                let step       = (100 - edgeBuffer * 2) / colors.length;
                let colorSteps = colors.map((color,
                                             index) => `${color} ${index * step + edgeBuffer}%, ${color} ${((index + 1) * step) - 0.01 + edgeBuffer}%`);

                colorSteps.unshift(`${colors[0]} 0%`);
                colorSteps.push(`${colors[colors.length - 1]} 100%`);

                style = `linear-gradient(-45deg, ${colorSteps.join(", ")})`;
            }

            this.sourceIndicatorStyle = style;
        }
        else
        {
            colorSegments             = colorSegments.slice(0, 3);
            let step                  = (100 - edgeBuffer * 2) / colorSegments.length;
            let gradientSteps         = colorSegments.map((segments,
                                                           index) =>
                                                          {
                                                              let colors       = ColorGradientContinuous.generateGradientColors(segments, false);
                                                              let start        = edgeBuffer + index * step;
                                                              let lastColorIdx = colors.length - 1;
                                                              let innerStep    = step / lastColorIdx;
                                                              return colors.map((color,
                                                                                 innerIndex) =>
                                                                                {
                                                                                    let percent = innerIndex * innerStep + start;
                                                                                    if (innerIndex === lastColorIdx) percent -= 0.01;
                                                                                    return `${color} ${percent}%`;
                                                                                })
                                                                           .join(", ");
                                                          });
            this.sourceIndicatorStyle = `linear-gradient(45deg, ${gradientSteps.join(", ")})`;
        }
    }

    updateFilteredSources()
    {
        let filter = (this.sourceFilter || "").toLocaleLowerCase();

        let numSources = 0;
        for (let sourceExt of this.sourceExts)
        {
            sourceExt.matchesFilter = !filter || sourceExt.filterString.indexOf(filter) >= 0;
            if (sourceExt.matchesFilter) numSources++;
        }

        if (numSources !== this.numShowingSources)
        {
            if (this.sourcesOverlay.isOpen && numSources > this.numShowingSources) this.sourcesOverlay.pullIntoView(0);

            this.numShowingSources = numSources;
        }

        this.markForCheck();
        if (!this.reflowing) this.updateConsolidated();
    }

    private async measureSourceChips()
    {
        if (!this.embedded && this.sourceExts.length <= ConsolidatedSourceChipComponent.maxUnconsolidatedSources)
        {
            this.reflowing = true;
            this.detectChanges();

            await Future.delayed(0);

            for (let sourceChip of this.sourceChips)
            {
                let source = sourceChip.source;
                if (source)
                {
                    let sourceExt = this.sourceExts.find((sourceExt) => sourceExt?.source?.identifier === source.identifier);
                    if (sourceExt)
                    {
                        sourceExt.chipWidth = sourceChip.element.nativeElement.offsetWidth;
                    }
                }
            }

            this.reflowing = false;

            this.updateConsolidated();
        }
    }

    public updateConsolidated()
    {
        let consolidated = this.sourceExts.length > 1;
        if (!this.embedded && this.sourceExts.length <= ConsolidatedSourceChipComponent.maxUnconsolidatedSources)
        {
            const availableWidth = this.m_element.nativeElement.clientWidth;
            let requiredWidth    = 0;
            const rightPadding   = 8;
            for (let sourceExt of this.sourceExts)
            {
                if (sourceExt.source.valid)
                {
                    requiredWidth += sourceExt.chipWidth + rightPadding;
                    if (requiredWidth - rightPadding > availableWidth) break;
                }
            }

            consolidated = requiredWidth - rightPadding > availableWidth;
        }

        if (this.consolidated !== consolidated)
        {
            this.consolidated = consolidated;
            this.consolidatedChange.emit(consolidated);
        }

        this.markForCheck();
    }

    notifyOpen(): void
    {
        this.opened.emit();
    }
}

class InteractableSourceExtended
{
    public readonly filterString: string;

    public matchesFilter: boolean = true;
    public chipWidth: number;

    public deleted: boolean = false;

    constructor(public readonly source: InteractableSource,
                public readonly containingChart: InteractableSourcesChart,
                public readonly primaryTextOverride?: string)
    {
        let primary       = this.primaryTextOverride || source.name || "";
        let secondary     = source.description || "";
        this.filterString = (primary + secondary).toLocaleLowerCase();
    }
}
