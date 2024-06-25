import {Component, ElementRef, Input, SimpleChanges, ViewChild} from "@angular/core";

import {ControlPointsGroupExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {ColorPickerGradientStopComponent} from "app/shared/colors/color-picker-gradient-stop.component";

import {UtilsService} from "framework/services/utils.service";
import {ChartColorUtilities, ColorSegmentInterpolationMode} from "framework/ui/charting/core/colors";
import {OverlayBase} from "framework/ui/overlays/overlay-base";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {TabGroupComponent} from "framework/ui/tab-group/tab-group.component";

@Component({
               selector   : "o3-control-points-group-configurer[groupType][group]",
               templateUrl: "./control-points-group-configurer.component.html",
               styleUrls  : ["./control-points-group-configurer.component.scss"]
           })
export class ControlPointsGroupConfigurerComponent extends OverlayBase
{
    @Input() groupType: ControlPointGroupType;

    private m_group: ControlPointsGroupExtended;
    @Input() set group(group: ControlPointsGroupExtended)
    {
        if (!group) return;
        this.m_group = group;

        if (group.model.valuePrecision)
        {
            this.m_valuePrecision = group.model.valuePrecision - 1;
        }

        let segments = this.m_group.colorConfig?.segments;
        this.setColorIfDefined(segments?.[0]?.color);
    }

    get group(): ControlPointsGroupExtended
    {
        return this.m_group;
    }

    overlayConfig = OverlayConfig.onTopDraggable({width: 500});

    color: string                                = ChartColorUtilities.getDefaultColorById("blue").hex;
    interpolation: ColorSegmentInterpolationMode = ColorSegmentInterpolationMode.STEP;
    mappingSegments: Models.ColorSegment[]; // in use to configure color config - only set color config if modified (when it's unset, any default config can be used instead)

    private m_valuePrecision: number;

    get enableRounding(): boolean
    {
        return this.m_group.model.valuePrecision > 0;
    }

    set enableRounding(enable: boolean)
    {
        if (enable)
        {
            this.valuePrecision = this.m_valuePrecision;
        }
        else
        {
            this.m_group.model.valuePrecision = 0;
        }
    }

    get valuePrecision(): number
    {
        return Math.max(0, this.m_valuePrecision || 0);
    }

    set valuePrecision(precision: number)
    {
        this.m_valuePrecision             = UtilsService.clamp(0, 9, Math.round(precision) || 0);
        this.m_group.model.valuePrecision = this.m_valuePrecision + 1;
    }

    //--//

    colorMappingTooltip: string = "Color mapping based upon aggregation value";
    rangeTooltip: string        = "Only available in 'Table With Bars' visualization mode";
    limitTooltip: string        = "Only available when a limit is selected";

    @ViewChild("test_overlay") test_overlay: OverlayComponent;
    @ViewChild("test_tabGroup") test_tabGroup: TabGroupComponent;
    @ViewChild("test_groupAgg", {read: ElementRef}) test_groupAgg: ElementRef;
    @ViewChild("test_cpAgg", {read: ElementRef}) test_cpAgg: ElementRef;
    @ViewChild("test_gradientStop") test_gradientStop: ColorPickerGradientStopComponent;

    get isAggregationTable(): boolean
    {
        return !this.isAggregationSummary && !this.isAggregationTrend;
    }

    get isAggregationSummary(): boolean
    {
        return this.groupType == ControlPointGroupType.SUMMARY;
    }

    get isAggregationTrend(): boolean
    {
        return this.groupType == ControlPointGroupType.TREND;
    }

    get showColorMapping(): boolean
    {
        switch (this.groupType)
        {
            case ControlPointGroupType.SUMMARY:
            case ControlPointGroupType.TABLE:
            case ControlPointGroupType.TABLE_WITH_BAR:
                return true;
        }

        return false;
    }

    get showGranularityOption(): boolean
    {
        if (this.isAggregationTable) return false;

        switch (this.m_group.model.aggregationType)
        {
            case Models.AggregationTypeId.SUM:
            case Models.AggregationTypeId.DELTA:
            case Models.AggregationTypeId.AVGDELTA:
            case Models.AggregationTypeId.INCREASE:
            case Models.AggregationTypeId.DECREASE:
                return true;

            default:
                return false;
        }
    }

    get showLimitOption(): boolean
    {
        return this.m_group.model.limitMode !== Models.AggregationLimit.None;
    }

    get showRangeOption(): boolean
    {
        return this.groupType == ControlPointGroupType.TABLE_WITH_BAR;
    }

    get showNoneRollupOption(): boolean
    {
        switch (this.groupType)
        {
            case ControlPointGroupType.TABLE_WITH_BAR:
            case ControlPointGroupType.TABLE:
                return true;
            default:
                return false;
        }
    }

    get granularityTooltip(): string
    {
        return this.showGranularityOption ? "Select average per" : "Only available for Sum, Delta, Increase and Decrease control point aggregation modes";
    }

    public ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);
        if (!this.m_group || !this.groupType) return;

        let segments = this.m_group.colorConfig?.segments;
        if (!segments)
        {
            this.m_group.model.colorConfig = ColorConfigurationExtended.emptyModel();
            this.setDefaultColorConfig();
        }
        else if (this.showColorMapping)
        {
            if (segments.length < 2)
            {
                this.color = segments[0]?.color;
                if (this.color)
                {
                    this.setDefaultStops();
                }
                else
                {
                    this.setDefaultColorConfig();
                }
            }
            else
            {
                this.mappingSegments = segments;
            }
        }
        else
        {
            this.setColorIfDefined(segments[0]?.color);
        }
    }

    aggregationTypeChanged()
    {
        if (!this.showGranularityOption)
        {
            this.m_group.model.granularity = Models.AggregationGranularity.None;
        }
    }

    setDefaultColorConfig()
    {
        switch (this.groupType)
        {
            case ControlPointGroupType.TABLE:
                this.color = ChartColorUtilities.getColorById("Gray", "gray4").hex;
                break;

            default:
                this.color = ChartColorUtilities.getDefaultColorById("blue").hex;
                break;
        }

        if (this.showColorMapping) this.setDefaultStops();
    }

    private setDefaultStops()
    {
        this.mappingSegments = [
            Models.ColorSegment.newInstance({
                                                color    : this.color,
                                                stopPoint: Models.ColorStopPoint.MIN
                                            }),
            Models.ColorSegment.newInstance({
                                                color    : this.color,
                                                stopPoint: Models.ColorStopPoint.MAX
                                            })
        ];
    }

    applyColor()
    {
        let segments = this.m_group.colorConfig?.segments?.map((segment) => Models.ColorSegment.newInstance(segment)) || [];
        if (segments.length > 1)
        {
            segments[0] = Models.ColorSegment.newInstance({
                                                              color    : this.color,
                                                              stopPoint: Models.ColorStopPoint.MIN
                                                          });
        }
        else
        {
            segments = [
                Models.ColorSegment.newInstance({
                                                    color    : this.color,
                                                    stopPoint: Models.ColorStopPoint.MIN
                                                }),
                Models.ColorSegment.newInstance({
                                                    color    : this.color,
                                                    stopPoint: Models.ColorStopPoint.MAX
                                                })
            ];
        }

        this.applyStops(segments);
    }

    applyStops(stops: Models.ColorSegment[])
    {
        this.m_group.colorConfig = Models.ColorConfiguration.newInstance({segments: stops});
    }

    private setColorIfDefined(color?: string)
    {
        if (color) this.color = color;
    }
}

export enum ControlPointGroupType
{
    TABLE          = "TABLE",
    TABLE_WITH_BAR = "TABLE_WITH_BAR",
    BUBBLEMAP      = "BUBBLEMAP",
    TREEMAP        = "TREEMAP",
    DONUT          = "DONUT",
    SUNBURST       = "SUNBURST",
    PIE            = "PIE",
    PIEBURST       = "PIEBURST",
    SUMMARY        = "SUMMARY",
    TREND          = "TREND"
}
