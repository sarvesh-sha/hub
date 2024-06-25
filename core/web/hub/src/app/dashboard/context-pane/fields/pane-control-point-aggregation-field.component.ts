import {ChangeDetectionStrategy, Component, Injector, Input} from "@angular/core";

import {DeviceElementsDetailPageComponent} from "app/customer/device-elements/device-elements-detail-page.component";
import {PaneFieldComponent} from "app/dashboard/context-pane/fields/pane-field.component";
import {AggregationHelper, AggregationResult} from "app/services/domain/aggregation.helper";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector       : "o3-pane-control-point-aggregation-field",
               templateUrl    : "./pane-control-point-aggregation-field.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class PaneControlPointAggregationFieldComponent extends PaneFieldComponent
{
    private m_controlPoint: Models.ControlPointsGroup;
    private m_range: Models.RangeSelection;

    public result: AggregationResult;
    public color: string;

    @Input()
    public set range(range: Models.RangeSelection)
    {
        this.m_range = range;
        this.getValue();
    }

    @Input()
    public set controlPointGroup(controlPointGroup: Models.ControlPointsGroup)
    {
        this.m_controlPoint = controlPointGroup;
        this.getValue();
    }

    constructor(inj: Injector)
    {
        super(inj);
    }

    private async getValue()
    {
        if (!this.m_controlPoint || !this.m_range)
        {
            return;
        }

        this.isLoading = true;
        this.detectChanges();

        let range      = Models.FilterableTimeRange.newInstance({range: this.m_range});
        this.result    = await AggregationHelper.aggregateControlPointsGroupSingle(this.app, this.m_controlPoint, [range]);
        this.color     = this.result?.getColorByTimeRange(0);
        this.isLoading = false;
        this.detectChanges();
    }

    public isClickable(): boolean
    {
        return this.result && !!this.result.getAggByTimeRange(0).significantElement;
    }

    attemptNavigation()
    {
        if (this.result)
        {
            let childResult = this.result.getAggByTimeRange(0);
            if (childResult.significantElement)
            {
                DeviceElementsDetailPageComponent.navigate(this.app, childResult.significantElement);
            }
        }
    }
}
