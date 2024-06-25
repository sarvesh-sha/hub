import {ChangeDetectionStrategy, Component, Input} from "@angular/core";

import {DeviceElementsDetailPageComponent} from "app/customer/device-elements/device-elements-detail-page.component";
import {AggregationGroupAggregator, AggregationHelper, AggregationResult} from "app/services/domain/aggregation.helper";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

import {Lookup} from "framework/services/utils.service";
import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ChartFont} from "framework/ui/charting/core/text";

@Component({
               selector       : "o3-aggregation[config]",
               templateUrl    : "./aggregation.component.html",
               styleUrls      : ["./aggregation.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AggregationComponent extends BaseApplicationComponent
{
    private static readonly maxFontSize: number      = 100;
    private static readonly padding: number          = 4;
    private static readonly singleLineHeight: number = 1;
    private static readonly multiLineHeight: number  = 1.2;

    result: AggregationResult;
    valueText: string;
    valueStyles: Lookup<number | string> = {};

    private m_canvasRenderer = new ChartHelpers();
    private m_font           = new ChartFont();

    range: Models.FilterableTimeRange;
    rangeText: string = "";
    private m_rangeSizeCss: string;

    get rangeFontSize(): string
    {
        if (this.m_rangeSizeCss) return this.m_rangeSizeCss;
        if (this.rangeText?.length > 25 && this.responsiveWidth < 150) return "70%";

        return null;
    }

    loading = true;

    get navigable(): boolean
    {
        if (!this.result?.getAggByTimeRange(0).significantElement) return false;

        return this.result.aggType == Models.AggregationTypeId.MAX || this.result.aggType == Models.AggregationTypeId.MIN;
    }


    @Input() config: AggregationConfig;
    @Input() subtextOverride: string = null;
    @Input() fontSize: number;

    private m_responsiveWidth: number;
    @Input() set responsiveWidth(width: number)
    {
        this.m_responsiveWidth = width;
        this.updateFontSize();
    }

    private m_responsiveHeight: number;
    @Input() set responsiveHeight(height: number)
    {
        this.m_responsiveHeight = height;
        this.updateFontSize();
    }

    private m_tooltip: string;
    @Input() set tooltip(tooltip: string)
    {
        this.m_tooltip = tooltip;
    }

    get tooltip(): string
    {
        return this.m_tooltip || AggregationHelper.aggregationToLabel(this.result);
    }

    get subtext(): string
    {
        return this.subtextOverride && this.config.hideRange ? this.subtextOverride : this.rangeText;
    }

    get showSubtext(): boolean
    {
        return !this.config.hideRange || !!this.subtextOverride;
    }

    async bind()
    {
        if (!this.config.controlPointGroup)
        {
            this.config.controlPointGroup = new Models.ControlPointsGroup();
        }
        if (!this.config.controlPointGroup.colorConfig?.segments?.length)
        {
            this.config.controlPointGroup.colorConfig = ColorConfigurationExtended.defaultWidgetModel();
        }

        this.range = Models.FilterableTimeRange.newInstance(this.config.filterableRange);
        if (!this.range.range)
        {
            this.range.range = RangeSelectionExtended.newModel();
        }

        this.valueStyles["font-family"] = this.m_font.family;
        this.valueStyles["padding"]     = AggregationComponent.padding + "px";

        await this.refreshContent();
    }

    async refreshContent()
    {
        let controlPointGroup = this.config.controlPointGroup;
        if (!controlPointGroup) return;

        let filterableRanges: Models.FilterableTimeRange[] = [];

        let overallRange = new RangeSelectionExtended(this.range.range);
        let ranges       = overallRange.splitBasedOnGranularity(controlPointGroup.granularity);

        for (let range of ranges)
        {
            let filterableRange = Models.FilterableTimeRange.newInstance({
                                                                             range          : range,
                                                                             isFilterApplied: this.range.isFilterApplied,
                                                                             filter         : this.range.filter
                                                                         });
            filterableRanges.push(filterableRange);
        }

        if (filterableRanges.length == 1)
        {
            this.result = await AggregationHelper.aggregateControlPointsGroupSingle(this.app, controlPointGroup, filterableRanges);
        }
        else
        {
            //
            // We have to average the results of the individual ranges.
            //
            let aggregationRoot      = AggregationResult.empty();
            aggregationRoot.children = [];

            let filterableRangeSets = [];
            for (let filterableRange of filterableRanges)
            {
                filterableRangeSets.push([filterableRange]);
            }

            let results = await AggregationHelper.aggregateControlPointsGroup(this.app, controlPointGroup, filterableRangeSets, Models.ControlPointDisplayType.NameOnly);

            for (let result of results)
            {
                aggregationRoot.children.push(result);
                aggregationRoot.unitsDisplay = aggregationRoot.unitsDisplay || result.unitsDisplay;
            }

            AggregationGroupAggregator.doGroupAggregations(aggregationRoot,
                                                           Models.AggregationTypeId.MEAN,
                                                           null,
                                                           controlPointGroup,
                                                           0,
                                                           1,
                                                           aggregationRoot.unitsDisplay,
                                                           0);
            aggregationRoot.cpLabel = controlPointGroup.name;

            this.result = aggregationRoot;
        }

        if (this.result)
        {
            this.valueText            = AggregationHelper.numberWithUnitDisplay(this.result.getAggByTimeRange(0).value, this.result.unitsDisplay);
            this.valueStyles["color"] = this.result.getColorByTimeRange(0);
        }
        else
        {
            this.valueText            = "N/A";
            this.valueStyles["color"] = controlPointGroup.colorConfig.segments[0].color;
        }

        this.rangeText = RangeSelectionExtended.getDisplayName(this.range.range);
        if (this.range.name && this.range.isFilterApplied)
        {
            this.rangeText = `${this.range.name} over ${this.rangeText}`;
        }

        this.updateFontSize();

        this.loading = false;
        this.markForCheck();
    }

    navigate()
    {
        let element = this.result?.getAggByTimeRange(0).significantElement;
        if (element instanceof DeviceElementExtended)
        {
            DeviceElementsDetailPageComponent.navigate(this.app, element);
        }
    }

    private updateFontSize()
    {
        if (this.valueText && this.m_responsiveWidth)
        {
            const fontSizeKey = "font-size";
            let currFont      = this.valueStyles[fontSizeKey];

            let width  = this.m_responsiveWidth - AggregationComponent.padding * 2;
            let height = this.m_responsiveHeight - AggregationComponent.padding * 2;
            if (!this.config.hideRange)
            {
                // range gets 32px height
                height -= 32;
            }

            let fontSize = this.fontSize;
            if (!fontSize)
            {
                if (this.m_responsiveHeight != undefined)
                {
                    this.m_font.lineHeight    = AggregationComponent.multiLineHeight;
                    let fontSizeMultipleLines = this.m_canvasRenderer.computeMaxFontSize(this.valueText, this.m_font, width, height, AggregationComponent.maxFontSize);

                    this.m_font.lineHeight = AggregationComponent.singleLineHeight;
                    fontSize               = this.m_canvasRenderer.computeMaxFontSize(this.valueText, this.m_font, width, height, AggregationComponent.maxFontSize, 1);

                    const multiLineThresholdScalar = 1.7;
                    if (fontSizeMultipleLines >= fontSize * multiLineThresholdScalar)
                    {
                        fontSize               = fontSizeMultipleLines;
                        this.m_font.lineHeight = AggregationComponent.multiLineHeight;
                    }
                }
                else
                {
                    this.m_font.lineHeight = AggregationComponent.singleLineHeight;
                    fontSize               = this.m_canvasRenderer.computeMaxFontSize(this.valueText, this.m_font, width, Number.MAX_VALUE, AggregationComponent.maxFontSize, 1);
                }

                this.valueStyles["line-height"] = this.m_font.lineHeight;
            }

            if (currFont !== fontSize)
            {
                const fontSizeCss = fontSize + "px";

                this.valueStyles[fontSizeKey] = fontSizeCss;
                this.m_rangeSizeCss           = fontSize < this.app.css.BaseFontSize.asNumber ? fontSizeCss : null;
            }
        }
    }
}

interface AggregationConfig
{
    controlPointGroup: Models.ControlPointsGroup;
    filterableRange: Models.FilterableTimeRange;
    hideRange: boolean;
}
