import {CdkVirtualScrollViewport} from "@angular/cdk/scrolling";
import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import {AppContext} from "app/app.service";
import {LocationExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {TABLE_CELL_BORDER_SPACING} from "framework/ui/layout";
import {getSortDirection, SortArrowDirection} from "framework/ui/shared/sort/sort-arrow.component";

@Component({
               selector       : "o3-alert-table",
               templateUrl    : "./alert-table.component.html",
               styleUrls      : ["./alert-table.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AlertTableComponent extends BaseApplicationComponent
{
    private static readonly ITEM_SIZE: number = 31;

    get itemSize(): number
    {
        return AlertTableComponent.ITEM_SIZE + TABLE_CELL_BORDER_SPACING * 2;
    }

    private m_config: AlertTableConfig;
    @Input() set config(config: AlertTableConfig)
    {
        this.m_config = config;
        if (this.m_config && !this.m_config.groupBy) this.m_config.groupBy = Models.SummaryFlavor.location;

        this.refreshContent();
    }

    private m_ranges: Models.RangeSelection[];
    @Input() set ranges(ranges: Models.RangeSelection[])
    {
        this.m_ranges          = ranges;
        this.rangeDescriptions = this.m_ranges?.map((range) => RangeSelectionExtended.getDisplayName(range)) || [];
        this.refreshContent();
    }

    get ranges(): Models.RangeSelection[]
    {
        return this.m_ranges;
    }

    get groupByLabel(): string
    {
        switch (this.m_config.groupBy)
        {
            case Models.SummaryFlavor.rule:
                return "Alert Rule";

            case Models.SummaryFlavor.severity:
                return "Alert Severity";

            case Models.SummaryFlavor.status:
                return "Alert Status";

            case Models.SummaryFlavor.type:
                return "Alert Type";

            default:
                return "Location";
        }
    }

    alertSummaries: AlertSummaryResult[] = [];
    rangeDescriptions: string[];

    sortColumnIdx: number;
    currSortAscending: boolean;

    @ViewChild(CdkVirtualScrollViewport) scrollViewport: CdkVirtualScrollViewport;

    @Output() rowSelected = new EventEmitter<Models.SummaryResult>();

    get virtualScrollTranslationInverse(): number
    {
        let contentOffset = this.scrollViewport && this.scrollViewport["_renderedContentOffset"];
        if (isNaN(contentOffset)) return 0;

        // bug in chrome where borders aren't rendered correctly which leads to flickering effect as
        // content goes by and is seen through the gap created from the border not being rendered --
        // workaround: subtract by 1 to hide this gap
        return -contentOffset - 1;
    }

    async refreshSize(): Promise<boolean>
    {
        if (this.scrollViewport)
        {
            this.scrollViewport.checkViewportSize();
            return true;
        }

        return false;
    }

    async refreshContent()
    {
        if (!this.m_config || !this.m_ranges?.length) return;

        let filters = Models.AlertFilterRequest.newInstance({
                                                                locationIDs      : this.m_config.locations,
                                                                locationInclusive: true,
                                                                alertStatusIDs   : this.m_config.alertStatusIDs,
                                                                alertTypeIDs     : this.m_config.alertTypeIDs,
                                                                alertSeverityIDs : this.m_config.alertSeverityIDs,
                                                                alertRules       : this.m_config.alertRules,
                                                                evaluateUpdatedOn: true
                                                            });

        let labelGenerator                            = await AlertSummaryLabelGenerator.newGenerator(this.app, this.m_config.groupBy);
        let resultsLookup: Lookup<AlertSummaryResult> = {};
        let results: AlertSummaryResult[]             = [];

        let highestValue = 0;
        for (let rangeIndex = 0; rangeIndex < this.ranges.length; rangeIndex++)
        {
            let overallRange   = new RangeSelectionExtended(this.ranges[rangeIndex]);
            filters.rangeStart = overallRange.getMin()
                                             .toDate();
            filters.rangeEnd   = overallRange.getMax()
                                             .toDate();

            let values = await this.app.domain.alerts.getSummary(filters, this.m_config.rollupType, this.m_config.groupBy);
            for (let value of values)
            {
                if (value.count > 0)
                {
                    let result = await this.ensureExt(resultsLookup, labelGenerator, results, value);

                    result.numAlerts[rangeIndex] = value.count;

                    if (highestValue < value.count) highestValue = value.count;
                }
            }

            // sort by severity to ensure the most relevant alarming color is selected
            let colorMap = new Map<Models.AlertSeverity, Models.AlertMapSeverityColor>();
            for (let severityColor of this.m_config.severityColors) colorMap.set(severityColor.severity, severityColor);
            let severityColors = [
                colorMap.get(Models.AlertSeverity.CRITICAL),
                colorMap.get(Models.AlertSeverity.SIGNIFICANT),
                colorMap.get(Models.AlertSeverity.NORMAL),
                colorMap.get(Models.AlertSeverity.LOW)
            ];

            // Color according to configuration
            let severityIDs = this.m_config.alertSeverityIDs || [];
            for (let severity of severityColors)
            {
                if (!severityIDs.length || severityIDs.indexOf(severity.severity) >= 0)
                {
                    await this.updateColor(resultsLookup, labelGenerator, results, filters, rangeIndex, severity.color, severity.severity);
                }
            }
        }

        this.alertSummaries = results;

        let col       = this.sortColumnIdx;
        let ascending = this.currSortAscending;
        if (ascending == null)
        {
            col       = this.ranges.length;
            ascending = false;
        }
        this.updateSort(col, getSortDirection(ascending));
        this.markForCheck();
    }

    private async ensureExt(resultsLookup: Lookup<AlertSummaryResult>,
                            labelGenerator: AlertSummaryLabelGenerator,
                            results: AlertSummaryResult[],
                            value: Models.SummaryResult)
    {
        let result = resultsLookup[value.id];
        if (!result)
        {
            result                  = new AlertSummaryResult(value, await labelGenerator.getLabel(value));
            resultsLookup[value.id] = result;
            results.push(result);
        }

        return result;
    }

    private async updateColor(resultsLookup: Lookup<AlertSummaryResult>,
                              labelGenerator: AlertSummaryLabelGenerator,
                              results: AlertSummaryResult[],
                              filters: Models.AlertFilterRequest,
                              rangeIndex: number,
                              color: string,
                              ...severity: Models.AlertSeverity[])
    {
        let filtersCloned              = Models.AlertFilterRequest.newInstance(filters);
        filtersCloned.alertSeverityIDs = severity;

        let values = await this.app.domain.alerts.getSummary(filtersCloned, this.m_config.rollupType, this.m_config.groupBy);
        for (let value of values)
        {
            if (value.count > 0)
            {
                let result = await this.ensureExt(resultsLookup, labelGenerator, results, value);
                if (result.colors[rangeIndex] === undefined)
                {
                    result.colors[rangeIndex] = color;
                }
            }
        }
    }

    public updateSort(colIdx: number,
                      direction: SortArrowDirection)
    {
        let ascending          = direction === "asc";
        this.sortColumnIdx     = colIdx;
        this.currSortAscending = ascending;

        AlertSummaryResult.sort(this.alertSummaries, colIdx, ascending);

        if (this.scrollViewport)
        {
            this.alertSummaries = [...this.alertSummaries];
            this.markForCheck();
        }
    }

    public static defaultSeverityColors(): Models.AlertMapSeverityColor[]
    {
        return [
            Models.AlertMapSeverityColor.newInstance({
                                                         color   : "#ffffff",
                                                         severity: Models.AlertSeverity.LOW
                                                     }),
            Models.AlertMapSeverityColor.newInstance({
                                                         color   : ChartColorUtilities.getColorById("Map Colors", "maporange").hex,
                                                         severity: Models.AlertSeverity.NORMAL
                                                     }),
            Models.AlertMapSeverityColor.newInstance({
                                                         color   : ChartColorUtilities.getColorById("Map Colors", "maporange").hex,
                                                         severity: Models.AlertSeverity.SIGNIFICANT
                                                     }),
            Models.AlertMapSeverityColor.newInstance({
                                                         color   : ChartColorUtilities.getColorById("Map Colors", "mapred").hex,
                                                         severity: Models.AlertSeverity.CRITICAL
                                                     })
        ];
    }
}

export interface AlertTableConfig
{
    groupBy: Models.SummaryFlavor;
    locations: string[];
    rollupType: Models.LocationType;
    alertStatusIDs: Models.AlertStatus[];
    alertTypeIDs: Models.AlertType[];
    alertSeverityIDs: Models.AlertSeverity[];
    severityColors: Models.AlertMapSeverityColor[];
    alertRules: Models.RecordIdentity[];
}

class AlertSummaryLabelGenerator
{
    private readonly m_locationLookup: Lookup<LocationExtended> = {};

    private constructor(public readonly groupBy: Models.SummaryFlavor)
    {
        if (!this.groupBy) this.groupBy = Models.SummaryFlavor.location;
    }

    private async init(app: AppContext)
    {
        if (this.groupBy === Models.SummaryFlavor.location)
        {
            let locations = await app.domain.locations.getLocations();
            for (let location of locations) this.m_locationLookup[location.model.sysId] = location;
        }
    }

    public static async newGenerator(app: AppContext,
                                     groupBy: Models.SummaryFlavor)
    {
        let generator = new AlertSummaryLabelGenerator(groupBy);
        await generator.init(app);
        return generator;
    }

    public async getLabel(summary: Models.SummaryResult): Promise<string>
    {
        if (!summary) return "";

        switch (this.groupBy)
        {
            case Models.SummaryFlavor.location:
                return this.m_locationLookup[summary.id]?.getRecursiveName();

            default:
                return summary.label;
        }
    }
}

class AlertSummaryResult
{
    numAlerts: number[] = [];
    colors: string[]    = [];

    constructor(public readonly summary: Models.SummaryResult,
                public label: string)
    {
    }

    public static sort(results: AlertSummaryResult[],
                       columnIdx: number,
                       ascending: boolean)
    {
        results.sort(AlertSummaryResult.getSortFn(columnIdx, ascending));
    }

    private static getSortFn(columnIdx: number,
                             ascending: boolean): (a: AlertSummaryResult,
                                                   b: AlertSummaryResult) => number
    {
        let timeRangeIdx = columnIdx - 1;
        if (timeRangeIdx === -1)
        {
            return (a: AlertSummaryResult,
                    b: AlertSummaryResult) => UtilsService.compareStrings(a.label, b.label, ascending);
        }

        return (a: AlertSummaryResult,
                b: AlertSummaryResult) => UtilsService.compareNumbers(a.numAlerts[timeRangeIdx], b.numAlerts[timeRangeIdx], ascending);
    }
}
