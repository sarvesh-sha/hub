import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, SimpleChanges} from "@angular/core";

import {DataExplorerPageComponent} from "app/customer/visualization/data-explorer-page.component";
import {TimeSeriesChartConfigurationExtended, TimeSeriesSourceConfigurationExtended, TimeSeriesSourceHost} from "app/customer/visualization/time-series-utils";
import {AggregationHelper} from "app/services/domain/aggregation.helper";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {UnitsService} from "app/services/domain/units.service";
import {ControlPointsGroupExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {RangeSelection} from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

import {DataSourceBitSet, DataSourceBoolean, DataSourceDecimal, DataSourceEnumerated, DataSourceEnumeratedSet, DataSourceInteger} from "framework/ui/charting/core/data-sources";
import {TimeRangeType} from "framework/ui/charting/core/time";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {AppNavigationService} from "framework/ui/navigation/app-navigation.service";
import {ViewStateService} from "framework/ui/navigation/view-state.service";

@Component({
               selector       : "o3-control-point-summary",
               templateUrl    : "./control-point-summary.component.html",
               styleUrls      : ["./control-point-summary.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ControlPointSummaryComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() id: string = null;
    @Input() showMenu   = true;
    @Output() loaded    = new EventEmitter<boolean>();

    get isStandard(): boolean
    {
        return this.configExt?.model.type === Models.TimeSeriesChartType.STANDARD;
    }

    get isMap(): boolean
    {
        return this.configExt?.model.type === Models.TimeSeriesChartType.COORDINATE;
    }

    public valid: boolean = false;
    public deviceElementExt: DeviceElementExtended;

    public name: string;
    public pointClass: string;
    public location: string;
    public timestamp: string;
    public value: string;
    public units: string;

    public min: string;
    public max: string;
    public avg: string;

    public valueSummaryLoaded = false;
    public hasData            = false;
    public isNumeric          = false;

    get lastValue(): string
    {
        if (!this.valueSummaryLoaded) return "Loading...";

        if (this.hasData) return `${this.value} at ${this.timestamp}`;

        return "No data available";
    }

    public hours: number                    = 168;
    public configExt: TimeSeriesChartConfigurationExtended;
    public range: Models.RangeSelection     = Models.RangeSelection.newInstance({range: Models.TimeRangeId.Last7Days});
    public selection: Models.RangeSelection = Models.RangeSelection.newInstance({range: Models.TimeRangeId.Last7Days});
    public host: TimeSeriesSourceHost       = new TimeSeriesSourceHost(this);

    public ranges = TimeRangeType.Relative;

    private formatter: Intl.NumberFormat;
    private lastDataTimestamp: Date = new Date();
    private unit: Models.EngineeringUnitsFactors;

    constructor(inj: Injector,
                public readonly element: ElementRef,
                public unitsService: UnitsService,
                private navigation: AppNavigationService,
                private vss: ViewStateService)
    {
        super(inj);

        this.formatter = new Intl.NumberFormat("en", {
            style                : "decimal",
            maximumFractionDigits: 2,
            useGrouping          : true
        });
    }

    async ngOnChanges(changes: SimpleChanges)
    {
        this.loaded.emit(false);
        this.valid = false;

        if (this.id)
        {
            this.deviceElementExt = await this.app.domain.assets.getTypedExtendedById(DeviceElementExtended, this.id);
            this.name             = this.deviceElementExt.typedModel.name;

            this.detectChanges();

            let workers = [
                this.fetchPointClassAndLocation(),
                this.fetchChartValues(),
                this.fetchValueSummary()
            ];

            for (let worker of workers)
            {
                try
                {
                    await worker;
                }
                catch (e)
                {
                    // Ignore failures.
                }
            }

            // Mark as valid
            this.valid = true;
        }

        // If valid mark as loaded
        if (this.valid) this.loaded.emit(true);
    }

    private async fetchPointClassAndLocation()
    {
        // Try to fetch the point class name
        let normalization           = await this.app.domain.normalization.getActiveRules();
        let pointClassOptions       = await this.app.bindings.getPointClasses(true, normalization.rules);
        let pointClass              = this.deviceElementExt.model.pointClassId;
        let pointClassControlOption = pointClassOptions.find((option) => option.id === pointClass);

        this.pointClass = pointClassControlOption && pointClassControlOption.label || "No Point Class";

        // Try to fetch the location name
        try
        {
            let locationExtended = await this.deviceElementExt.getLocation();
            this.location        = await locationExtended.getRecursiveName();
        }
        catch (e)
        {
            this.location = "No Location";
        }

        // Force update of view to reflect loaded names
        this.markForCheck();
    }

    private async fetchChartValues()
    {
        let sourceExt = await TimeSeriesSourceConfigurationExtended.resolveFromIdAndDimension(this.host, this.id);

        let chartExt                = TimeSeriesChartConfigurationExtended.emptyInstance(this.app);
        chartExt.model.display.size = 179;

        if (this.deviceElementExt.isCoordinate) chartExt.model.type = Models.TimeSeriesChartType.COORDINATE;

        await chartExt.applySourceChanges([sourceExt], [], true);
        chartExt.setMapSources();
        chartExt.panelsExt[0].leftAxisExtended.hideLabel = true;

        // Assign chart config
        this.configExt = chartExt;

        this.markForCheck();
    }

    private async fetchValueSummary()
    {
        let schema = await this.deviceElementExt.getSchemaProperty(DeviceElementExtended.PRESENT_VALUE);
        let unit   = await this.unitsService.findPreferred(schema?.unitsFactors);
        let units  = (await this.unitsService.getDescriptorFromFactors(unit))?.model.displayName;
        let last   = await this.getCurrValue(unit);
        if (last)
        {
            if (units == UnitsService.noUnitsDisplayName) units = null;

            switch (schema.type)
            {
                case Models.TimeSeriesSampleType.BitSet:
                case Models.TimeSeriesSampleType.EnumeratedSet:
                case Models.TimeSeriesSampleType.Enumerated:
                    units          = null;
                    this.isNumeric = false;
                    break;

                case Models.TimeSeriesSampleType.Decimal:
                case Models.TimeSeriesSampleType.Integer:
                    this.isNumeric = true;
                    break;
            }

            // Save unit data for later use
            this.unit  = unit;
            this.units = units;

            // Adjust range to be from last point
            this.lastDataTimestamp = last.timestamp;
            this.updateRange();

            let time       = MomentHelper.parse(last.timestamp);
            this.timestamp = MomentHelper.friendlyFormatVerboseUS(time);
            this.value     = this.formatLastValue(last, schema);


            // If numeric, load aggregations
            if (this.isNumeric)
            {
                // Perform the aggregation
                await this.aggregate(unit);
            }

            // Flag as having data
            this.hasData = true;
        }

        this.valueSummaryLoaded = true;
        this.markForCheck();
    }

    async viewInDataExplorer()
    {
        if (!this.deviceElementExt) return;

        const acceptableFreshnessFactor = 0.5;
        let timeSinceLastSample         = MomentHelper.now()
                                                      .valueOf() - this.lastDataTimestamp.valueOf();
        let timeRangeDuration           = new RangeSelectionExtended(this.selection).getChartRange().diffAsMs;
        let rangeForDataExplorer        = timeSinceLastSample < timeRangeDuration * acceptableFreshnessFactor ? this.selection : this.range;

        let type = this.deviceElementExt.isCoordinate ? Models.TimeSeriesChartType.COORDINATE : Models.TimeSeriesChartType.STANDARD;

        await DataExplorerPageComponent.visualizeDeviceElement(this, this.navigation, this.vss, this.id, type, rangeForDataExplorer);
    }

    async getCurrValue(unit: Models.EngineeringUnitsFactors): Promise<Models.TimeSeriesLastValueResponse>
    {
        return await this.deviceElementExt.getLastValue(DeviceElementExtended.PRESENT_VALUE, unit);
    }

    rangeChanged(range: RangeSelection)
    {
        let rangeExt = new RangeSelectionExtended(range);
        this.hours   = rangeExt.getMax()
                               .diff(rangeExt.getMin(), "hours");
        this.updateTime();
    }

    updateTime()
    {
        this.updateRange();
        if (this.isNumeric) this.aggregate(this.unit);
    }

    private async aggregate(unit: Models.EngineeringUnitsFactors)
    {
        let base: Partial<Models.ControlPointsGroup> = {
            unitsFactors  : unit,
            selections    : Models.ControlPointsSelection.newInstance({identities: [this.deviceElementExt.getIdentity()]}),
            valuePrecision: 3
        };

        let min = ControlPointsGroupExtended.newModel({
                                                          ...base,
                                                          name                : "Min",
                                                          aggregationType     : Models.AggregationTypeId.MIN,
                                                          groupAggregationType: Models.AggregationTypeId.MIN
                                                      });
        let max = ControlPointsGroupExtended.newModel({
                                                          ...base,
                                                          name                : "Max",
                                                          aggregationType     : Models.AggregationTypeId.MAX,
                                                          groupAggregationType: Models.AggregationTypeId.MAX
                                                      });
        let avg = ControlPointsGroupExtended.newModel({
                                                          ...base,
                                                          name                : "Avg",
                                                          aggregationType     : Models.AggregationTypeId.MEAN,
                                                          groupAggregationType: Models.AggregationTypeId.MEAN
                                                      });

        let filterable = Models.FilterableTimeRange.newInstance({
                                                                    isFilterApplied: false,
                                                                    range          : this.range
                                                                });
        let resultMin  = await AggregationHelper.aggregateControlPointsGroupSingle(this.app, min, [filterable]);
        let resultMax  = await AggregationHelper.aggregateControlPointsGroupSingle(this.app, max, [filterable]);
        let resultAvg  = await AggregationHelper.aggregateControlPointsGroupSingle(this.app, avg, [filterable]);

        this.min = this.formatNumber(resultMin.aggregationsByTimeRange[0].value);
        this.max = this.formatNumber(resultMax.aggregationsByTimeRange[0].value);
        this.avg = this.formatNumber(resultAvg.aggregationsByTimeRange[0].value);
    }

    private updateRange()
    {
        // Adjust range to be from last point
        let buffer   = new RangeSelectionExtended(this.selection).getChartRange().diffAsMs / 400;
        let lastTime = MomentHelper.add(MomentHelper.parse(this.lastDataTimestamp), buffer);
        this.range   = Models.RangeSelection.newInstance({
                                                             start: MomentHelper.subtract(lastTime, this.hours, "hours")
                                                                                .toDate(),
                                                             end  : lastTime.toDate()
                                                         });
    }

    private formatLastValue(last: Models.TimeSeriesLastValueResponse,
                            schema: Models.TimeSeriesPropertyType)
    {
        switch (schema.type)
        {
            case Models.TimeSeriesSampleType.Integer:
                if (schema.isBoolean)
                {
                    return DataSourceBoolean.formatValue(last.value);
                }
                else
                {
                    return this.formatNumber(parseInt(DataSourceInteger.formatValue(last.value)));
                }

            case Models.TimeSeriesSampleType.Decimal:
                return this.formatNumber(parseFloat(DataSourceDecimal.formatValue(last.value)));

            case Models.TimeSeriesSampleType.BitSet:
                return DataSourceBitSet.formatValue(last.value);

            case Models.TimeSeriesSampleType.Enumerated:
                return DataSourceEnumerated.formatValue(last.value, DataSourceEnumerated.mapValuesToNames(schema.values));

            case Models.TimeSeriesSampleType.EnumeratedSet:
                return DataSourceEnumeratedSet.formatValue(last.value);

            default:
                return last.value.toString;
        }
    }

    private formatNumber(value: number): string
    {
        return this.formatter.format(value);
    }
}
