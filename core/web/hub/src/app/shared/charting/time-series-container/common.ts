import {EventReceiver} from "framework/ui/charting/charting-interaction";

export interface TimeSeriesChartingComponent extends EventReceiver
{
    refreshSize(): boolean;

    getCanvasPNG(): string;

    getCanvasTitle(): string;

    toggleConfigurer(): void;

    toggleColorConfigurer(): void;
}

export enum ChartChangeAnnotationImpact
{
    None,
    UnitsChanged,
    Invalidated
}
