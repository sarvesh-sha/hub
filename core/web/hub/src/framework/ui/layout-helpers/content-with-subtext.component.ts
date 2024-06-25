import {ChangeDetectionStrategy, Component, Input, SimpleChanges} from "@angular/core";

import {Lookup} from "framework/services/utils.service";
import * as SharedSvc from "framework/ui/components";

@Component({
               selector       : "o3-content-with-subtext",
               templateUrl    : "./content-with-subtext.component.html",
               styleUrls      : ["./content-with-subtext.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ContentWithSubtextComponent extends SharedSvc.BaseComponent
{
    @Input() text: string;
    @Input() fontSize: string;
    @Input() fontColor: string;
    @Input() showText: boolean;

    fontStyles: Lookup<string> = {};

    public ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        if (changes.fontSize || changes.fontColor)
        {
            this.fontStyles = {};

            if (this.fontColor) this.fontStyles["color"] = this.fontColor;
            if (this.fontSize) this.fontStyles["font-size"] = this.fontSize;
        }
    }
}
