import {ChangeDetectionStrategy, Component, Injector, Input, SimpleChanges, ViewChild} from "@angular/core";

import {AppContext} from "app/app.service";
import * as SharedSvc from "app/services/domain/base.service";

import {ChartGroup, ChartPanel} from "framework/ui/charting/app-charting-utilities";
import {ChartZoomState} from "framework/ui/charting/chart-timeline.component";
import {ChartComponent} from "framework/ui/charting/chart.component";
import {ChartPoint, ChartPointRange, ChartPointSource, ChartPointType} from "framework/ui/charting/core/data-sources";
import {ChartTimeRange} from "framework/ui/charting/core/time";


@Component({
               selector       : "o3-spark-line",
               templateUrl    : "./spark-line.component.html",
               styleUrls      : ["spark-line.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class SparkLineComponent extends SharedSvc.BaseApplicationComponent
{
    private m_panels: ChartPanel[] = [];
    get panels(): ChartPanel[]
    {
        return this.m_panels;
    }

    zoomState: ChartZoomState;

    @Input() data: SparkLinePoint[];
    @Input() unitsAbbreviation: string;
    @Input() color: string;

    public ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);
        this.updateSource();
    }

    @ViewChild(ChartComponent, {static: true}) private chart: ChartComponent;

    constructor(inj: Injector)
    {
        super(inj);

        const panel          = this.m_panels[0] = new ChartPanel();
        panel.hideHighlights = true;
        panel.hideBottomAxis = true;
        panel.hideSideAxes   = true;

        const group              = panel.groups[0] = new ChartGroup();
        group.useAsLeftValueAxis = true;
    }

    private updateSource()
    {
        const source   = new SparkLineSource(this.app, this.data, this.color, this.unitsAbbreviation);
        this.zoomState = new ChartZoomState(null, source.timeRange);

        this.m_panels[0].groups[0].sources[0] = source;
        this.m_panels                         = [this.m_panels[0]];
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();

        this.chart.refreshSize();
    }
}

class SparkLineSource extends ChartPointSource<number>
{
    timeRange: ChartTimeRange = new ChartTimeRange();

    constructor(app: AppContext,
                data: SparkLinePoint[],
                color: string,
                units: string)
    {
        super(app, {
            getTooltip(point: ChartPoint<number>): string
            {
                return ChartPointSource.valueTooltipTemplate(point, units);
            },
            getTooltipText(point: ChartPoint<number>): string
            {
                return ChartPointSource.valueTooltipText(point, units);
            }
        });

        this.fillArea   = false;
        this.showPoints = false;
        this.color      = color;
        this.lineWidth *= 0.8;

        const range = this.ranges[0] = new ChartPointRange(this);
        for (let point of data || [])
        {
            range.addPointRaw(point.x, point.y, point.type);

            this.timeRange.expandToContain(point.x);
        }
    }

    public convertToPoint(timestamp: number,
                          value: number,
                          type: ChartPointType,
                          selected: boolean): ChartPoint<number>
    {
        return new ChartPoint<number>(this, timestamp, value, value, type, selected);
    }

    public getColorMapping(point: ChartPoint<number>): string
    {
        return null;
    }

    public getDisplayValue(value: number): string
    {
        return `${value}`;
    }

    public getEnumeratedRange(): string[]
    {
        return [];
    }

    public getNumericValue(value: number): number
    {
        return value;
    }

    public isDiscrete(): boolean
    {
        return false;
    }
}

export class SparkLinePoint
{
    type: ChartPointType = ChartPointType.Value;

    constructor(public readonly x: number,
                public readonly y: number)
    {}
}
