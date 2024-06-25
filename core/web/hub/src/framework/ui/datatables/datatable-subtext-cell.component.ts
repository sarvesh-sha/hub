import {Component, Input} from "@angular/core";

import {Lookup} from "framework/services/utils.service";

@Component({
               selector   : "o3-datatable-subtext-cell",
               templateUrl: "./datatable-subtext-cell.component.html"
           })
export class DatatableSubtextCellComponent
{
    @Input() primary: any;
    @Input() secondary: any;

    @Input() stylesFn: (primary: any) => Lookup<string | number>;
    @Input() secondaryStylesFn: (secondary: any) => Lookup<string | number>;

    getStyles(primary: boolean,
              value: any): Lookup<string | number>
    {
        const stylesFn = primary ? this.stylesFn : this.secondaryStylesFn;

        let styles = stylesFn?.(value) || {};
        delete styles["display"];

        return styles;
    }
}
