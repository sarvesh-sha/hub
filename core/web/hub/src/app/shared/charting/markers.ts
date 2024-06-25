import {ChartMarker} from "framework/ui/charting/core/basics";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {ChartPointSource} from "framework/ui/charting/core/data-sources";

export abstract class MarkerWithChartPointSource<T> extends ChartMarker<T>
{
    private currentColor: string = null;

    protected constructor(source: T,
                          public readonly parent: ChartPointSource<any>)
    {
        super(source);
    }

    get strokeWidthMultiplier(): number
    {
        return 1;
    }

    get color(): string
    {
        if (!this.currentColor)
        {
            this.currentColor = this.computeColor();
            if (!this.currentColor)
            {
                this.currentColor = ChartColorUtilities.safeChroma(this.parent.color)
                                                       .darken(1.5)
                                                       .saturate(1.5)
                                                       .hex();
            }
        }

        return this.currentColor;
    }

    protected computeColor(): string
    {
        return null;
    }
}
