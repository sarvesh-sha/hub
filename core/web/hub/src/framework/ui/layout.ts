import {Injectable} from "@angular/core";

import {Subject} from "rxjs";

// keep synced with app.variables.scss
export const LAYOUT_WIDTH_SM: number = 600;
export const LAYOUT_WIDTH_MD: number = 960;
export const LAYOUT_WIDTH_LG: number = 1280;

@Injectable({providedIn: "root"})
export class CommonLayout
{
    contentSizeChanged = new Subject<void>();
}

export enum MatIconSize
{
    tiny   = "tiny",
    small  = "small",
    medium = "medium",
    normal = "normal"
}

export function getMatIconClass(size: MatIconSize): string
{
    switch (size)
    {
        case MatIconSize.tiny:
            return "mat-icon-tiny";

        case MatIconSize.small:
            return "mat-icon-small";

        case MatIconSize.medium:
            return "mat-icon-medium";

        default:
            return null;
    }
}

export const TABLE_CELL_BORDER_SPACING: number = 1;
