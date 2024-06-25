export enum RelativeLocation
{
    Top = 1, Right, Bottom, Left, Center
}

export type AlignItemsCss = "flex-start" | "flex-end" | "center" | "normal" | "stretch";
export type JustifyContentCss = "flex-start" | "flex-end" | "center" | "space-between" | "space-around" | "space-evenly";
export type TextAlignCss = "center" | "left" | "right" | "justify";
export type VerticalAlignCss = "top" | "middle" | "bottom";

export function relativeLocationToTextAlignCss(relativeLocation: RelativeLocation,
                                               defaultStyle: TextAlignCss): TextAlignCss
{
    switch (relativeLocation)
    {
        case RelativeLocation.Left:
            return "left";

        case RelativeLocation.Center:
            return "center";

        case RelativeLocation.Right:
            return "right";
    }

    return defaultStyle;
}

export function relativeLocationToJustifyContentCss(relativeLocation: RelativeLocation,
                                                    defaultStyle: JustifyContentCss): JustifyContentCss
{
    switch (relativeLocation)
    {
        case RelativeLocation.Left:
        case RelativeLocation.Top:
            return "flex-start";

        case RelativeLocation.Center:
            return "center";

        case RelativeLocation.Right:
        case RelativeLocation.Bottom:
            return "flex-end";
    }

    return defaultStyle;
}

export function relativeLocationToAlignItemsCss(relativeLocation: RelativeLocation,
                                                defaultStyle: AlignItemsCss): AlignItemsCss
{
    switch (relativeLocation)
    {
        case RelativeLocation.Top:
        case RelativeLocation.Left:
            return "flex-start";

        case RelativeLocation.Center:
            return "center";

        case RelativeLocation.Bottom:
        case RelativeLocation.Right:
            return "flex-end";
    }

    return defaultStyle;
}

export function relativeLocationToVerticalAlignCss(relativeLocation: RelativeLocation,
                                                   defaultStyle: VerticalAlignCss): VerticalAlignCss
{
    switch (relativeLocation)
    {
        case RelativeLocation.Top:
            return "top";

        case RelativeLocation.Center:
            return "middle";

        case RelativeLocation.Bottom:
            return "bottom";
    }

    return defaultStyle;
}
