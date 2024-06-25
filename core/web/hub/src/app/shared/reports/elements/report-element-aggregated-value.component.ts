import {Component, ElementRef, Injector, ViewChild} from "@angular/core";

import * as Models from "app/services/proxy/model/models";
import {AggregationComponent} from "app/shared/aggregation/aggregation.component";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";

@Component({
               selector   : "o3-report-element-aggregated-value",
               styles     : [
                   ":host { display: block; width: 100%; }",
                   "h2 { margin-bottom: 0 !important; }",
                   "o3-aggregation { height: 100px; }"
               ],
               templateUrl: "./report-element-aggregated-value.component.html"
           })
export class ReportElementAggregatedValueComponent extends ReportElementBaseComponent<ReportElementAggregatedValueData, ReportElementAggregatedValueConfiguration>
{
    @ViewChild(AggregationComponent, {static: true}) aggregation: AggregationComponent;

    constructor(inj: Injector,
                private element: ElementRef)
    {
        super(inj);
    }

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        this.updateWidth();
    }

    width: number;

    async afterConfigurationChanges()
    {
        if (this.data && this.data.controlPointGroup && this.data.filterableRange)
        {
            await this.aggregation.bind();
            this.markAsComplete();
        }
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();

        this.updateWidth();
    }

    private updateWidth()
    {
        this.width = this.element.nativeElement?.clientWidth;
        this.detectChanges();
    }
}

export class ReportElementAggregatedValueConfiguration extends ReportElementConfigurationBase
{
    public static newReportModel()
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.AggregatedValue;
        model.configuration = new ReportElementAggregatedValueConfiguration();
        return model;
    }
}

export class ReportElementAggregatedValueData extends ReportElementDataBase
{
    label: string;
    controlPointGroup: Models.ControlPointsGroup;
    filterableRange: Models.FilterableTimeRange;
    hideRange: boolean;
}
