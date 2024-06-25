import * as Models from "app/services/proxy/model/models";

import {ControlOption} from "framework/ui/control-option";
import {JustifyContentCss, RelativeLocation, relativeLocationToJustifyContentCss, relativeLocationToTextAlignCss, TextAlignCss} from "framework/ui/utils/relative-location-styles";

export class VerticalHorizontalPlacement
{
    constructor(public readonly vertical: Models.VerticalAlignment,
                public readonly horizontal: Models.HorizontalAlignment)
    {
    }

    matches(vertical: Models.VerticalAlignment,
            horizontal: Models.HorizontalAlignment): boolean
    {
        return this.vertical === vertical && this.horizontal === horizontal;
    }
}

export const CenteredVerticalHorizontalPlacement = new VerticalHorizontalPlacement(Models.VerticalAlignment.Middle, Models.HorizontalAlignment.Center);

export const PlacementOptions: ControlOption<VerticalHorizontalPlacement>[] = [
    new ControlOption(new VerticalHorizontalPlacement(Models.VerticalAlignment.Top, Models.HorizontalAlignment.Left), "Top-left"),
    new ControlOption(new VerticalHorizontalPlacement(Models.VerticalAlignment.Top, Models.HorizontalAlignment.Center), "Top"),
    new ControlOption(new VerticalHorizontalPlacement(Models.VerticalAlignment.Top, Models.HorizontalAlignment.Right), "Top-right"),
    new ControlOption(new VerticalHorizontalPlacement(Models.VerticalAlignment.Middle, Models.HorizontalAlignment.Left), "Left"),
    new ControlOption(CenteredVerticalHorizontalPlacement, "Center"),
    new ControlOption(new VerticalHorizontalPlacement(Models.VerticalAlignment.Middle, Models.HorizontalAlignment.Right), "Right"),
    new ControlOption(new VerticalHorizontalPlacement(Models.VerticalAlignment.Bottom, Models.HorizontalAlignment.Left), "Bottom-left"),
    new ControlOption(new VerticalHorizontalPlacement(Models.VerticalAlignment.Bottom, Models.HorizontalAlignment.Center), "Bottom"),
    new ControlOption(new VerticalHorizontalPlacement(Models.VerticalAlignment.Bottom, Models.HorizontalAlignment.Right), "Bottom-right")
];

export function getVerticalHorizontalPlacement(vertical: Models.VerticalAlignment,
                                               horizontal: Models.HorizontalAlignment): VerticalHorizontalPlacement
{
    return PlacementOptions.find((placement) => placement.id.matches(vertical, horizontal))?.id;
}

//--//

export function horizontalAlignmentToRelativeLocation(alignment: Models.HorizontalAlignment): RelativeLocation
{
    switch (alignment)
    {
        case Models.HorizontalAlignment.Left:
            return RelativeLocation.Left;

        case Models.HorizontalAlignment.Center:
            return RelativeLocation.Center;

        case Models.HorizontalAlignment.Right:
            return RelativeLocation.Right;
    }

    return null;
}

export function verticalAlignmentToRelativeLocation(alignment: Models.VerticalAlignment): RelativeLocation
{
    switch (alignment)
    {
        case Models.VerticalAlignment.Top:
            return RelativeLocation.Top;

        case Models.VerticalAlignment.Middle:
            return RelativeLocation.Center;

        case Models.VerticalAlignment.Bottom:
            return RelativeLocation.Bottom;
    }

    return null;
}
