import {ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges} from "@angular/core";

import {AlignItemsCss, JustifyContentCss, RelativeLocation, relativeLocationToAlignItemsCss, relativeLocationToJustifyContentCss, relativeLocationToTextAlignCss, relativeLocationToVerticalAlignCss, TextAlignCss, VerticalAlignCss} from "framework/ui/utils/relative-location-styles";

@Component({
               selector       : "o3-image-frame",
               templateUrl    : "./image-frame.component.html",
               styleUrls      : ["./image-frame.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ImageFrameComponent implements OnChanges
{
    @Input() imageBase64: string;
    @Input() horizontalAlignment: RelativeLocation;
    @Input() verticalAlignment: RelativeLocation;

    justifyContentStyle: JustifyContentCss;
    alignItemsStyle: AlignItemsCss;
    textAlignStyle: TextAlignCss;
    verticalAlignStyle: VerticalAlignCss;

    ngOnChanges(changes: SimpleChanges)
    {
        this.justifyContentStyle = relativeLocationToJustifyContentCss(this.horizontalAlignment, "center");
        this.alignItemsStyle     = relativeLocationToAlignItemsCss(this.verticalAlignment, "center");
        this.textAlignStyle      = relativeLocationToTextAlignCss(this.horizontalAlignment, "center");
        this.verticalAlignStyle  = relativeLocationToVerticalAlignCss(this.verticalAlignment, "middle");
    }
}
