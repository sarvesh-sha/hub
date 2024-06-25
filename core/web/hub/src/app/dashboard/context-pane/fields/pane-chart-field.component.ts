import {ChangeDetectionStrategy, Component, Input} from "@angular/core";

import {TimeSeriesChartConfigurationExtended} from "app/customer/visualization/time-series-utils";
import {PaneFieldComponent} from "app/dashboard/context-pane/fields/pane-field.component";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector       : "o3-context-pane-chart-field",
               templateUrl    : "./pane-chart-field.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class PaneChartFieldComponent extends PaneFieldComponent
{
    @Input() public range: Models.RangeSelection;

    @Input() public chartExt: TimeSeriesChartConfigurationExtended;

    @Input()
    public set chart(chart: Models.TimeSeriesChartConfiguration)
    {
        this.initChart(chart);
    }

    hasData(): boolean
    {
        return this.range !== undefined && this.chartExt !== undefined;
    }

    public isClickable(): boolean
    {
        return false;
    }

    private async initChart(chart: Models.TimeSeriesChartConfiguration)
    {
        let chartExt = await TimeSeriesChartConfigurationExtended.newInstance(this.app, chart);
        if (!chartExt.resolvedGraphs.size)
        {
            await chartExt.loadGraphs();
            await chartExt.applyStandardGraphSourceChanges(chartExt.standardGraphBindingSet());
        }

        this.chartExt = chartExt;
        this.detectChanges();
    }
}
