import {ChangeDetectionStrategy, Component} from "@angular/core";

import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {horizontalAlignmentToRelativeLocation} from "app/shared/options/placement-options";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {ChartFont} from "framework/ui/charting/core/text";
import {relativeLocationToJustifyContentCss} from "framework/ui/utils/relative-location-styles";

@Component({
               selector       : "o3-text-widget",
               templateUrl    : "./widget.template.html",
               styleUrls      : ["./widget.styles.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class TextWidgetComponent extends WidgetBaseComponent<Models.TextWidgetConfiguration, TextWidgetConfigurationExtended>
{
    private static readonly minFontSize: number = 8;
    private static readonly maxFontSize: number = 100;

    public readonly padding: number = 4;

    textFits: boolean;
    textStyles: Lookup<string> = {};
    textContainerClass: string;

    private m_font         = new ChartFont(undefined, "'Open Sans', 'Helvetica Neue', sans-serif");
    private m_textMeasurer = new ChartHelpers();

    get showToolbarTitle(): boolean
    {
        return false;
    }

    async bind()
    {
        await super.bind();

        if (!this.config.name) this.config.name = "Text";

        this.textContainerClass = `o3-text-widget--${this.config.preventWrapping ? "no-wrap" : "wrap"}`;

        this.textStyles["color"]       = this.config.color;
        this.textStyles["font-family"] = this.m_font.family;
        this.textStyles["line-height"] = `${this.m_font.lineHeight}`;

        let alignment                      = this.config.alignment || Models.HorizontalAlignment.Center;
        this.textStyles["text-align"]      = alignment.toLowerCase() || "center";
        this.textStyles["justify-content"] = relativeLocationToJustifyContentCss(horizontalAlignmentToRelativeLocation(alignment), null);

        this.updateText();
    }

    async refreshSize(): Promise<boolean>
    {
        this.updateText();
        return true;
    }

    protected fontSizeUpdated()
    {
        super.fontSizeUpdated();

        this.updateText();
    }

    private updateText()
    {
        let width  = this.widthRaw;
        let height = this.heightRaw;
        if (width && height)
        {
            width -= 2 * this.padding;
            height -= 2 * this.padding;

            let fontSizeFits   = this.m_textMeasurer.computeMaxFontSize(this.config.text, this.m_font, width, height, TextWidgetComponent.maxFontSize, undefined, !this.config.preventWrapping);
            let fontSizeActual = this.manualFontSize || fontSizeFits;
            fontSizeActual     = UtilsService.clamp(TextWidgetComponent.minFontSize, TextWidgetComponent.maxFontSize, fontSizeActual);

            this.textFits                = fontSizeFits >= fontSizeActual;
            this.textStyles["font-size"] = fontSizeActual + "px";
            this.markForCheck();
        }
    }

    protected getClipboardData(): ClipboardEntryData<Models.TextWidgetConfiguration, null>
    {
        let model = Models.TextWidgetConfiguration.deepClone(this.config);

        return new class extends ClipboardEntryData<Models.TextWidgetConfiguration, null>
        {
            constructor()
            {
                super("text");
            }

            public getDashboardWidget(): Models.TextWidgetConfiguration
            {
                return Models.TextWidgetConfiguration.deepClone(model);
            }

            public getReportItem(): null
            {
                return null;
            }
        }();
    }
}

@WidgetDef({
               friendlyName      : "Text",
               typeName          : "TEXT",
               model             : Models.TextWidgetConfiguration,
               component         : TextWidgetComponent,
               dashboardCreatable: false,
               subgroupCreatable : true,
               maximizable       : true,
               defaultWidth      : 6,
               defaultHeight     : 4,
               hostScalableText  : true,
               needsProtector    : false,
               documentation     : {
                   description: "The Text widget allows you to place static text inside a Grouping Widget. You can control color, wrapping, alignment and font size.",
                   examples   : [
                       {
                           file       : "widgets/TEXT/example.png",
                           label      : "Two Text Widgets",
                           description: "Two Text widgets inside of a grouping widget being edited."
                       }
                   ]
               }

           })
export class TextWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.TextWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
        this.model.manualFontScaling = true;
        this.model.fontMultiplier    = 1;
        this.model.color             = ChartColorUtilities.getDefaultColorById("blue").hex;
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        return [];
    }
}
