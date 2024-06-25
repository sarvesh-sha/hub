import {Pipe, PipeTransform} from "@angular/core";

import {AggregationHelper, AggregationResult} from "app/services/domain/aggregation.helper";

@Pipe({name: "o3AggNumFormat"})
export class AggregationNumberFormatPipe implements PipeTransform
{
    transform(num: number,
              units?: string): string
    {
        return AggregationHelper.numberWithUnitDisplay(num, units);
    }
}

@Pipe({name: "o3AggToLabel"})
export class AggregationToLabelPipe implements PipeTransform
{
    public transform(aggResult: AggregationResult): string
    {
        return AggregationHelper.aggregationToLabel(aggResult);
    }
}
