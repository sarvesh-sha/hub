import {animate, query, stagger, style, transition, trigger} from "@angular/animations";
import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, QueryList, SimpleChanges, ViewChildren} from "@angular/core";

import {TimeSeriesChartConfigurationExtended} from "app/customer/visualization/time-series-utils";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {TimeSeriesChartType} from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {TimeSeriesContainerComponent} from "app/shared/charting/time-series-container/time-series-container.component";
import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {Subscription} from "rxjs";

/* Firefox has bugs with animation module: https://github.com/angular/angular/issues/20489 */
@Component({
               selector       : "o3-time-series-set",
               templateUrl    : "./time-series-set.component.html",
               styleUrls      : ["./time-series-set.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush,
               animations     : [
                   trigger("change", [
                       transition("* => *", [
                           query(":enter", [
                               style({
                                         "margin-top"   : 0,
                                         "margin-right" : 0,
                                         "margin-bottom": 0,
                                         "margin-left"  : 0,
                                         "height"       : 0,
                                         "opacity"      : 0,
                                         "transform"    : "scale(0.15,0.85)"
                                     }),
                               animate("0.2s ease-out", style({
                                                                  "margin-top"   : "*",
                                                                  "margin-right" : "*",
                                                                  "margin-bottom": "*",
                                                                  "margin-left"  : "*",
                                                                  "height"       : "*",
                                                                  "opacity"      : 1,
                                                                  "transform"    : "scale(1,1)"
                                                              }))
                           ], {optional: true}),
                           query(":leave", [
                               stagger(50, [
                                   animate("0.2s ease-out", style({
                                                                      "margin-top"   : 0,
                                                                      "margin-right" : 0,
                                                                      "margin-bottom": 0,
                                                                      "margin-left"  : 0,
                                                                      "height"       : 0,
                                                                      "opacity"      : 0,
                                                                      "transform"    : "scale(0.25,0.65)"
                                                                  }))
                               ])
                           ], {optional: true})
                       ])
                   ])
               ]
           })
export class TimeSeriesSetComponent extends SharedSvc.BaseApplicationComponent
{
    private static readonly CONTAINER_PADDING_BOTTOM = 11;
    private static readonly EMBEDDED_BORDER_BOTTOM   = 1;

    // Readonly flag for charts
    @Input() readonly: boolean            = false;
    @Input() canDeleteAllCharts: boolean  = false;
    @Input() canDeleteAllSources: boolean = true;
    @Input() handleInteractions           = true;
    @Input() resizable: boolean           = true;
    @Input() zoomable: boolean            = true;
    @Input() printable: boolean           = false;
    @Input() embedded: boolean            = false;
    @Input() allowDashboardAdd: boolean   = true;

    private m_allowConsolidatedSources: boolean = true;
    @Input() set allowConsolidatedSources(allow: boolean)
    {
        if (this.m_allowConsolidatedSources !== allow)
        {
            this.m_allowConsolidatedSources = allow;
            this.updateConfigChangeListeners();
        }
    }


    chartHeights: number[] = [];

    private m_viewWindow: VerticalViewWindow;
    @Input() set viewWindow(viewWindow: VerticalViewWindow)
    {
        this.m_viewWindow = viewWindow;
        this.updateViewWindows();
    }

    @Input() externalGraphsHost: GraphConfigurationHost;
    @Input() range: Models.RangeSelection;

    private m_numValidCharts: number                             = 0;
    private m_configChangeListeners: Subscription[]              = [];
    private m_configExts: TimeSeriesChartConfigurationExtended[] = [];
    @Input() set configExts(configExts: TimeSeriesChartConfigurationExtended[])
    {
        if (configExts)
        {
            this.m_configExts = configExts;

            this.updateConfigChangeListeners();
            this.recountValidCharts();
        }
    }

    get configExts(): TimeSeriesChartConfigurationExtended[]
    {
        return this.m_configExts;
    }

    @Output() configExtsChange           = new EventEmitter<TimeSeriesChartConfigurationExtended[]>();
    @Output() chartUpdated               = new EventEmitter<boolean>();
    @Output() sourcesConsolidatedUpdated = new EventEmitter<void>();
    @Output() startedFetchingData        = new EventEmitter<void>();
    @Output() stoppedFetchingData        = new EventEmitter<void>();

    getChartId: (index: number,
                 chart: TimeSeriesChartConfigurationExtended) => string = (index,
                                                                           chart) => chart.id;

    private m_sourcesConsolidated: boolean[]                       = [];
    viewWindows: VerticalViewWindow[];
    private m_timeSeriesContainers: TimeSeriesContainerComponent[] = [];

    private m_prevHoverHit: TimeSeriesContainerComponent;

    private m_fetching: boolean = false;
    private m_stoppedFetching   = new Set<TimeSeriesChartConfigurationExtended>();

    // Get reference to all timeseries container components
    @ViewChildren(TimeSeriesContainerComponent) set timeSeriesSet(set: QueryList<TimeSeriesContainerComponent>)
    {
        this.m_timeSeriesContainers = set?.toArray() || [];
        this.updateViewWindows();
    }

    get timeSeriesContainers(): TimeSeriesContainerComponent[]
    {
        return this.m_timeSeriesContainers;
    }

    private get inBetweenPadding(): number
    {
        let padding = TimeSeriesSetComponent.CONTAINER_PADDING_BOTTOM;
        if (this.embedded) padding += TimeSeriesSetComponent.EMBEDDED_BORDER_BOTTOM;
        return padding;
    }

    private m_added: boolean = false;

    constructor(inj: Injector,
                private element: ElementRef)
    {
        super(inj);
    }

    ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        if (changes.configExts)
        {
            for (let series of this.m_stoppedFetching)
            {
                if (this.m_configExts.indexOf(series) === -1) this.m_stoppedFetching.delete(series);
            }
        }
    }

    private updateConfigChangeListeners()
    {
        for (let configChangeListener of this.m_configChangeListeners) configChangeListener.unsubscribe();
        this.m_configChangeListeners = [];

        if (!this.m_allowConsolidatedSources)
        {
            for (let i = 0; i < this.m_configExts.length; i++)
            {
                let configExt = this.m_configExts[i];
                // trigger re-check of whether sources have enough space to be completely shown
                this.m_configChangeListeners.push(this.subscribeToObservable(configExt.configChanged, () => this.m_sourcesConsolidated[i] = false));
            }
        }
    }

    private recountValidCharts()
    {
        this.m_numValidCharts = 0;
        for (let configExt of this.m_configExts)
        {
            if (configExt.chartHandler.hasSources()) this.m_numValidCharts++;
        }
    }

    chartAdded()
    {
        this.markForCheck();
        this.m_added = true;
        this.reportConfigurationChanges();
    }

    afterConfigurationChanges()
    {
        super.afterConfigurationChanges();

        if (this.m_added)
        {
            // Unflag as having a new chart added
            this.m_added = false;

            // Open the source picking dialog if config is empty
            let last = this.m_timeSeriesContainers[this.m_timeSeriesContainers.length - 1];
            if (last.isEmpty()) last.configureSources();
        }
    }

    sourcesConsolidatedChanged(chartIdx: number,
                               consolidated: boolean)
    {
        this.m_sourcesConsolidated[chartIdx] = consolidated;
        this.sourcesConsolidatedUpdated.emit();
    }

    showSourceBar(index: number): boolean
    {
        if (this.m_allowConsolidatedSources) return true;

        return !this.m_sourcesConsolidated[index];
    }

    onMouseMove(event: MouseEvent,
                scrollTop: number)
    {
        const inBetweenPadding = this.inBetweenPadding;

        let offsetY    = event.offsetY + scrollTop;
        let currHeight = 0;
        const hoverHit = this.m_timeSeriesContainers.find((container) =>
                                                          {
                                                              currHeight += container.componentHeight;
                                                              const containerY = container.componentHeight - (currHeight - offsetY);
                                                              const hovered    = offsetY < currHeight && container.onMouseMove(event.offsetX, containerY);
                                                              currHeight += inBetweenPadding;
                                                              return hovered;
                                                          });

        if (hoverHit !== this.m_prevHoverHit)
        {
            this.onMouseLeave();
            this.m_prevHoverHit = hoverHit;
        }
    }

    onMouseLeave()
    {
        if (this.m_prevHoverHit)
        {
            this.m_prevHoverHit.onMouseLeave();
            this.m_prevHoverHit = null;
        }
    }

    refreshConsolidated()
    {
        this.m_sourcesConsolidated.fill(false);
    }

    refreshHeight(targetWithHeight: number,
                  retries: number = 0): boolean
    {
        let numCharts     = this.m_configExts.length;
        this.chartHeights = [];

        const sourceBarHeight = 60;
        let someAreZero       = false;
        let cumHeight         = 0;
        let cumPadding        = 0;
        for (let i = 0; i < numCharts; i++)
        {
            let chartExt = this.m_configExts[i];
            let size     = chartExt.model.display.size;

            this.chartHeights.push(size);
            cumHeight += size;
            if (!size) someAreZero = true;

            if (chartExt.chartHandler.showSourcePills())
            {
                if (this.m_allowConsolidatedSources || !this.m_timeSeriesContainers[i]?.sourcesConsolidated) cumPadding += sourceBarHeight;
            }

            if (!this.readonly)
            {
                switch (chartExt.model.type)
                {
                    case TimeSeriesChartType.COORDINATE:
                    case TimeSeriesChartType.GRAPH:
                    case TimeSeriesChartType.GRAPH_SCATTER:
                        cumPadding += 59; // context selector increases mat-card-title height to 59
                        break;

                    default:
                        cumPadding += 47;
                        break;
                }
            }
        }

        cumPadding += this.inBetweenPadding * (numCharts - 1);

        if (someAreZero || numCharts < 2)
        {
            const minHeight = 200;
            this.chartHeights.fill(Math.max(minHeight, (targetWithHeight - cumPadding) / numCharts));
        }
        else
        {
            let diff = targetWithHeight - cumPadding - cumHeight;
            if (diff > 0)
            {
                for (let i = 0; i < numCharts; i++)
                {
                    let height = this.chartHeights[i];
                    this.chartHeights[i] += diff * height / cumHeight;
                }
            }
        }

        this.detectChanges();

        let refreshed = true;
        for (let timeSeriesContainer of this.m_timeSeriesContainers)
        {
            if (!timeSeriesContainer.refreshSize(retries)) refreshed = false;
        }

        return refreshed;
    }

    configExtChanged(chartIdx: number,
                     newChart: TimeSeriesChartConfigurationExtended)
    {
        let oldChart = this.m_configExts.splice(chartIdx, 1, newChart)[0];
        this.recountValidCharts();

        if (oldChart.model !== newChart.model) this.configExtsChange.emit(this.m_configExts);
        this.chartUpdated.emit();
    }

    refreshZoomability()
    {
        for (let timeSeriesContainer of this.m_timeSeriesContainers)
        {
            timeSeriesContainer.refreshZoomability();
        }
    }

    deleteVisualization(index: number)
    {
        this.m_configExts.splice(index, 1);
        this.recountValidCharts();

        this.configExtsChange.emit(this.m_configExts);
    }

    startedFetching(index: number)
    {
        this.m_stoppedFetching.delete(this.m_configExts[index]);
        if (!this.m_fetching)
        {
            this.m_fetching = true;
            this.startedFetchingData.emit();
        }
    }

    stoppedFetching(index: number)
    {
        this.m_stoppedFetching.add(this.m_configExts[index]);
        if (this.m_stoppedFetching.size === this.m_numValidCharts)
        {
            this.m_fetching = false;
            this.stoppedFetchingData.emit();
        }
    }

    private updateViewWindows()
    {
        if (!this.m_timeSeriesContainers || !this.m_viewWindow) return;

        let offsetTop    = this.element.nativeElement.offsetTop || 0;
        let viewTop      = this.m_viewWindow.viewTop - offsetTop;
        let cumHeight    = 0;
        this.viewWindows = [];
        for (let timeSeriesContainer of this.m_timeSeriesContainers)
        {
            this.viewWindows.push(new VerticalViewWindow(viewTop - cumHeight, this.m_viewWindow.viewHeight));
            cumHeight += timeSeriesContainer.componentHeight;
        }
    }
}
