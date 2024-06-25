import {Injectable} from "@angular/core";

class CssConstant
{
    protected m_string: string;
    protected m_number: number;

    constructor(public readonly name: string)
    {}

    public get asString(): string
    {
        if (this.m_string === undefined)
        {
            this.m_string = getComputedStyle(document.documentElement)
                .getPropertyValue(this.name);
        }

        return this.m_string;
    }

    public get asNumber(): number
    {
        if (this.m_number === undefined)
        {
            this.m_number = parseInt(this.asString);
        }

        return this.m_number;
    }
}

@Injectable()
export class CssConstantsService
{
    public readonly BaseFontSize           = new CssConstant("--base-font-size");
    public readonly DashboardWidgetPadding = new CssConstant("--widget-padding");
    public readonly WidgetToolbarHeight    = new CssConstant("--widget-toolbar-height");
}
