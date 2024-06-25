import {ChartBox, ChartPixel} from "framework/ui/charting/core/basics";

export const INTERACTIVE_TREE_HEADER_FONT = "'Open Sans', 'Helvetica Neue', sans-serif";
export const DEFAULT_FONT                 = "'Helvetica Neue', 'Helvetica', 'Arial', sans-serif";

export class ChartFont
{
    constructor(public color: string      = "#666",
                public family: string     = DEFAULT_FONT,
                public size: number       = 12,
                public style: string      = "normal",
                public lineHeight: number = 1.25)
    {
    }

    toFontName(): string
    {
        return `${this.style} ${this.size}px ${this.family}`;
    }

    clone(): ChartFont
    {
        let font = new ChartFont();

        font.color      = this.color;
        font.family     = this.family;
        font.size       = this.size;
        font.style      = this.style;
        font.lineHeight = this.lineHeight;

        return font;
    }
}

export enum TextPlacement
{
    Left,
    Center,
    Right,
}

export enum TextOrientation
{
    Horizontal,
    VerticalAscending,
    VerticalDescending,
}

export class ChartTextBox extends ChartBox
{
    constructor(x: number,
                y: number,
                width: number,
                height: number,
                public lines: ChartTextLine[],
                public angle: number)
    {
        super(x, y, width, height);
    }
}

export class ChartTextLine extends ChartPixel
{
    constructor(x: number,
                y: number,
                public readonly width: number,
                public readonly height: number,
                public readonly text: string)
    {
        super(x, y);
    }
}

//--//

export class ChartTextUtilities
{
    static truncateLabel(label: string)
    {
        // truncate the middle of a lengthy string
        if (label.length > 15)
        {
            let first = label.substring(0, 6); // first six characters
            let last  = label.substring(label.length - 6); // last six characters
            return `${first}...${last}`;
        }
        return label;
    }

    static setPercentageOnLabel(tooltipItem: any,
                                data: any)
    {
        let allData      = data.datasets[tooltipItem.datasetIndex].data;
        let tooltipLabel = data.labels[tooltipItem.index];
        let tooltipData  = allData[tooltipItem.index];
        let total        = 0;
        for (let i in allData)
        {
            total += allData[i];
        }
        let tooltipPercentage = Math.round((tooltipData / total) * 100);
        return tooltipLabel + ": " + tooltipData + " (" + tooltipPercentage + "%)";
    }
}
