import {Component, Injector, ViewChild} from "@angular/core";

import {ReportError} from "app/app.service";
import {TimeSeriesChartConfigurationExtended, TimeSeriesSourceConfigurationExtended, TimeSeriesSourceHost} from "app/customer/visualization/time-series-utils";
import {NAVBAR_HEIGHT} from "app/layouts/standard-layout.component";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {BookmarkSet} from "app/services/domain/bookmark.service";
import * as Models from "app/services/proxy/model/models";
import {BookmarkEditorDialogComponent} from "app/shared/bookmarks/bookmark-editor-dialog.component";
import {BookmarksOverlayComponent} from "app/shared/bookmarks/bookmarks-overlay.component";
import {ChartSetComponent} from "app/shared/charting/chart-set.component";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

import {BaseComponent, PersistViewState} from "framework/ui/components";
import {AppNavigationService} from "framework/ui/navigation/app-navigation.service";
import {ViewState, ViewStateService} from "framework/ui/navigation/view-state.service";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {mapInParallelNoNulls} from "framework/utils/concurrency";

const BOOKMARK_NAME: string = "Data Explorer";

@Component({
               selector   : "o3-data-explorer-page",
               templateUrl: "./data-explorer-page.component.html",
               styleUrls  : ["./data-explorer-page.component.scss"]
           })
export class DataExplorerPageComponent extends SharedSvc.BaseComponentWithRouter
{
    private static readonly url = "/visualization/explorer";

    @PersistViewState() resizeChart: boolean         = false;
    @PersistViewState() range: Models.RangeSelection = RangeSelectionExtended.newModel();
    @PersistViewState(undefined,
                      (instance: BaseComponent,
                       charts) => charts.forEach((chart: Models.TimeSeriesChartConfiguration) => Models.TimeSeriesChartConfiguration.fixupPrototype(chart)))
    charts: Models.TimeSeriesChartConfiguration[]    = [];

    @ViewChild(BookmarksOverlayComponent, {static: true}) bookmarksOverlay: BookmarksOverlayComponent;

    private m_chartSetComponent: ChartSetComponent;
    @ViewChild(ChartSetComponent) set chartSetComponent(chartSet: ChartSetComponent)
    {
        if (chartSet && this.charts.length === 1 && this.resizeChart)
        {
            const verticalPaddingPx = 24;
            let chartHeight         = Math.max(this.charts[0].display.size, window.innerHeight - NAVBAR_HEIGHT - verticalPaddingPx);

            chartSet.refreshHeight(chartHeight, 2);
            this.resizeChart = false;
        }
        this.m_chartSetComponent = chartSet;
    }

    get chartSetComponent(): ChartSetComponent
    {
        return this.m_chartSetComponent;
    }

    bookmarkSets: BookmarkSet[]                           = [];
    private bookmarkViews: Models.BookmarkConfiguration[] = [];

    widgetOverlayConfig = OverlayConfig.newInstance({minWidth: "400px"});

    get emptyVisualizations(): boolean
    {
        return this.charts.every((chart) =>
                                     !TimeSeriesChartConfigurationExtended.hasDataSources(chart) &&
                                     !chart.scatterPlot?.sourceTuples.length &&
                                     !chart.graph?.sharedGraphs?.length && !chart.hierarchy?.bindings?.length);
    }

    constructor(inj: Injector)
    {
        super(inj);
        this.persistViewStateReady(); // Needed to properly enable @PersistViewState
    }

    protected onNavigationComplete(): void
    {
        if (!this.charts) this.charts = [];
        if (!this.range) this.range = RangeSelectionExtended.newModel();
    }

    ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.loadBookmarkViews();
    }

    async loadBookmarkViews()
    {
        let bookmarks      = await this.app.domain.bookmarks.getBookmarksOfType(Models.BookmarkType.DATA_EXPLORER);
        let bookmark       = bookmarks[0];
        this.bookmarkViews = bookmark && await this.app.domain.bookmarks.getBookmarkViews(bookmark) || [];
        if (this.bookmarkViews.length)
        {
            let bookmarkSet = new BookmarkSet(bookmark);
            let numViews    = this.bookmarkViews.length;
            for (let i = 0; i < numViews; i++) bookmarkSet.addView(this.bookmarkViews[i], i === numViews - 1);

            this.bookmarkSets = [bookmarkSet];
            this.detectChanges();
        }
    }

    openBookmarkOverlay()
    {
        BookmarkEditorDialogComponent.open(this, false, null,
                                           (name,
                                            description) => this.saveBookmarkView(name, description));
    }

    @ReportError
    private async saveBookmarkView(name: string,
                                   description: string): Promise<Models.BookmarkConfiguration>
    {
        let bookmark = await this.app.domain.bookmarks.generateBookmark(BOOKMARK_NAME, null, Models.BookmarkType.DATA_EXPLORER, null, null);
        if (bookmark && name)
        {
            let view = await this.app.domain.bookmarks.generateBookmarkView(bookmark, name, description);
            if (view)
            {
                this.app.framework.errors.success("Bookmark view saved", -1);
                this.bookmarkViews.push(view);

                let bookmarkSet = this.bookmarkSets.length ? this.bookmarkSets[0] : new BookmarkSet(bookmark);
                bookmarkSet.addView(view);
                this.bookmarkSets = [bookmarkSet];
            }
        }

        return bookmark;
    }

    async addWidget(title: string)
    {
        let config = Models.TimeSeriesWidgetConfiguration.newInstance({
                                                                          name  : title,
                                                                          range : this.range,
                                                                          charts: this.charts
                                                                      });

        let cfg = await this.app.domain.dashboard.getActive();
        await cfg.addTimeSeriesWidget(config);
    }

    public static async visualizeDeviceElement(sourcePage: BaseApplicationComponent,
                                               navigation: AppNavigationService,
                                               vss: ViewStateService,
                                               deviceElementId: string,
                                               chartType?: Models.TimeSeriesChartType,
                                               range?: Models.RangeSelection)
    {
        await this.visualizeDeviceElements(sourcePage, navigation, vss, [deviceElementId], chartType, range);
    }

    public static async visualizeDeviceElements(sourcePage: BaseApplicationComponent,
                                                navigation: AppNavigationService,
                                                vss: ViewStateService,
                                                deviceElementIds: string[],
                                                chartType?: Models.TimeSeriesChartType,
                                                range?: Models.RangeSelection)
    {
        let sourceExts = await mapInParallelNoNulls(deviceElementIds, async (deviceElementId) =>
        {
            let sourceExt = await TimeSeriesSourceConfigurationExtended.resolveFromIdAndDimension(new TimeSeriesSourceHost(sourcePage), deviceElementId);
            if (!sourceExt) return null;

            let deviceElemExt = await sourcePage.app.domain.assets.getTypedExtendedById(DeviceElementExtended, sourceExt.model.id);
            if (!deviceElemExt) return null;

            if (!chartType) chartType = deviceElemExt.isCoordinate ? Models.TimeSeriesChartType.COORDINATE : Models.TimeSeriesChartType.STANDARD;
            return sourceExt;
        });

        let chartExt        = TimeSeriesChartConfigurationExtended.emptyInstance(sourcePage.app);
        chartExt.model.type = chartType;

        await chartExt.applySourceChanges(sourceExts, []);
        chartExt.setMapSources();

        await this.visualizeCharts(navigation, vss, [chartExt], range);
    }

    public static async visualizeCharts(navigation: AppNavigationService,
                                        vss: ViewStateService,
                                        chartExts: TimeSeriesChartConfigurationExtended[],
                                        range?: Models.RangeSelection,
                                        appendCharts?: boolean)
    {

        let chartModels = chartExts.map((chartExt) => chartExt.model);

        let viewState = new ViewState(vss);
        if (appendCharts)
        {
            let existingCharts: Models.TimeSeriesChartConfiguration[] = vss.restore()
                                                                           .get("charts");

            chartModels = existingCharts.concat(chartModels);
        }
        viewState.set("charts", chartModels, false, false);
        viewState.set("resizeChart", true, false, false);
        if (range) viewState.set("range", range, false, false);

        vss.setForUrl(viewState, this.url);
        await navigation.go(this.url);
    }
}

