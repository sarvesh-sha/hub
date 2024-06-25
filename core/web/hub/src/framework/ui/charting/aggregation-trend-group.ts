import {UtilsService} from "framework/services/utils.service";

export class AggregationTrendGroup
{
    private static m_id: number = 0;
    public readonly id: number  = AggregationTrendGroup.m_id++;

    constructor(public readonly name: string,
                public readonly aggType: string,
                public readonly aggregations: AggregationTrendGroupAggregation[],
                public readonly valuePrecision: number,
                public color?: string)
    {
        this.valuePrecision = this.valuePrecision ?? 1;
        for (let aggregation of aggregations) aggregation.group = this;
    }
}

export class AggregationTrendGroupAggregation
{
    group: AggregationTrendGroup;

    valueLength: number;

    m_roundedValue: number;

    get value(): number
    {
        return this.m_roundedValue ?? this.m_value;
    }

    get rawValue(): number
    {
        return this.m_value;
    }

    constructor(private readonly m_value: number,
                public readonly formattedValue: string,
                public rangeLabel: string)
    {
    }

    roundValue(numDecimals: number)
    {
        this.m_roundedValue = numDecimals ? UtilsService.getRoundedValue(this.m_value, numDecimals - 1) : this.m_value;
    }
}
