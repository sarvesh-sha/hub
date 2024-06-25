import {Component, Input} from "@angular/core";

import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";

@Component({
               selector   : "o3-dynamic-pane-field",
               templateUrl: "./dynamic-pane-field.component.html"
           })
export class DynamicPaneFieldComponent
{
    @Input() field: Models.PaneField;
    @Input() aggRange: Models.RangeSelection;

    asAggregatedValueField(field: Models.PaneField): Models.PaneFieldAggregatedValue
    {
        return UtilsService.asTyped(field, Models.PaneFieldAggregatedValue);
    }

    asAlertCountField(field: Models.PaneField): Models.PaneFieldAlertCount
    {
        return UtilsService.asTyped(field, Models.PaneFieldAlertCount);
    }

    asAlertFeedField(field: Models.PaneField): Models.PaneFieldAlertFeed
    {
        return UtilsService.asTyped(field, Models.PaneFieldAlertFeed);
    }

    asChartField(field: Models.PaneField): Models.PaneFieldChart
    {
        return UtilsService.asTyped(field, Models.PaneFieldChart);
    }

    asCurrentValueField(field: Models.PaneField): Models.PaneFieldCurrentValue
    {
        return UtilsService.asTyped(field, Models.PaneFieldCurrentValue);
    }

    asGaugeField(field: Models.PaneField): Models.PaneFieldGauge
    {
        return UtilsService.asTyped(field, Models.PaneFieldGauge);
    }

    asNumberField(field: Models.PaneField): Models.PaneFieldNumber
    {
        return UtilsService.asTyped(field, Models.PaneFieldNumber);
    }

    asPathMapField(field: Models.PaneField): Models.PaneFieldPathMap
    {
        return UtilsService.asTyped(field, Models.PaneFieldPathMap);
    }

    asStringField(field: Models.PaneField): Models.PaneFieldString
    {
        return UtilsService.asTyped(field, Models.PaneFieldString);
    }
}
