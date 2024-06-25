import {ChangeDetectionStrategy, Component, ViewChild} from "@angular/core";

import {ControlPointMetadata} from "app/customer/visualization/time-series-utils";
import {WidgetBaseComponent} from "app/dashboard/dashboard/widgets/widget-base.component";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {ClipboardEntryData} from "app/services/domain/clipboard.service";
import {AssetContextSubscriptionPayload} from "app/services/domain/dashboard-management.service";
import {WidgetConfigurationExtended, WidgetDef} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {horizontalAlignmentToRelativeLocation} from "app/shared/options/placement-options";

import {Lookup} from "framework/services/utils.service";
import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ChartTooltipComponent} from "framework/ui/charting/chart-tooltip.component";
import {StepwiseColorMapper} from "framework/ui/charting/core/colors";
import {ChartPointSource} from "framework/ui/charting/core/data-sources";
import {ChartFont} from "framework/ui/charting/core/text";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {relativeLocationToJustifyContentCss} from "framework/ui/utils/relative-location-styles";

@Component({
               selector       : "o3-control-point-widget",
               templateUrl    : "./widget.template.html",
               styleUrls      : ["./widget.styles.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ControlPointWidgetComponent extends WidgetBaseComponent<Models.ControlPointWidgetConfiguration, ControlPointWidgetConfigurationExtended>
{
    private m_dbSub: SharedSvc.DbChangeSubscription<Models.Asset>;
    private m_cpData: ControlPointMetadata;
    private m_unitsFactors: Models.EngineeringUnitsFactors;

    get hasPoint(): boolean
    {
        return !!this.m_cpData?.point;
    }

    name: string;
    valueDisplay: string;
    timestampDisplay: string;
    timestampFontSize: string;

    entryStyling: Lookup<number | string> = {};
    entryColor: string                    = null;
    entryPadding: number                  = 2;
    entryJustifyContents: string[]        = [];

    private m_textMeasurer: ChartHelpers = new ChartHelpers;
    private m_font                       = new ChartFont(undefined, "'Open Sans', 'Helvetica Neue', sans-serif");

    @ViewChild("tooltip", {static: true}) public tooltip: ChartTooltipComponent;

    private m_tooltipBase: string;
    private m_tooltip: string;
    private m_tooltipRenderEvent: MouseEvent;

    public ngOnDestroy()
    {
        super.ngOnDestroy();

        this.clearDbSub();
    }

    public async bind(): Promise<void>
    {
        await super.bind();

        if (!this.config.name) this.config.name = "Current Value";

        if (!this.config.pointInput)
        {
            await this.setupValueUpdater(this.config.pointId);
        }

        this.entryJustifyContents = [
            relativeLocationToJustifyContentCss(horizontalAlignmentToRelativeLocation(this.config.nameAlignment), null),
            relativeLocationToJustifyContentCss(horizontalAlignmentToRelativeLocation(this.config.valueAlignment), null),
            relativeLocationToJustifyContentCss(horizontalAlignmentToRelativeLocation(this.config.timestampAlignment), null)
        ];

        this.entryStyling["line-height"] = this.m_font.lineHeight;
        this.entryStyling["font-family"] = this.m_font.family;
    }

    protected async dashboardUpdated(): Promise<any>
    {
        await super.dashboardUpdated();

        let input = this.config.pointInput;
        if (input)
        {
            let currContextId: string;
            await this.registerContextSubscriptions(new AssetContextSubscriptionPayload(input.selectorId, async (context) =>
            {
                if (context != null && context.sysId !== currContextId)
                {
                    currContextId = context.sysId;
                    let graph     = await this.dashboard.getResolvedGraph(input.graphId);
                    if (!graph) return;

                    let response   = await graph.resolveWithContext([context]);
                    let identities = response.resolveIdentities(input);
                    await this.setupValueUpdater(identities[0]?.sysId);
                }
            }));
        }
    }

    private async setupValueUpdater(sysId: string)
    {
        if (this.m_cpData?.id !== sysId)
        {
            this.clearDbSub();

            this.m_cpData = await ControlPointMetadata.fromId(this.app, sysId);
            if (this.hasPoint)
            {
                this.m_unitsFactors = this.config.valueUnits || (await this.m_cpData.point.getSchemaProperty(DeviceElementExtended.PRESENT_VALUE))?.unitsFactors;
                this.m_dbSub        = this.subscribe(this.m_cpData.point, async () =>
                {
                    await this.executeWithLoading(() => this.updateValues());
                });

                let locationName        = this.m_cpData.locationName;
                let parentEquipments    = await this.m_cpData.point.getParentsOfRelation(Models.AssetRelationship.controls) || [];
                let parentEquipment     = await this.app.domain.assets.getExtendedByIdentity(parentEquipments[0]);
                let parentEquipmentName = parentEquipment?.model.name;
                let controlPointName    = this.m_cpData.name;

                this.m_tooltipBase = "";
                this.m_tooltipBase += ChartPointSource.generateTooltipEntry("Control Point", controlPointName);
                this.m_tooltipBase += ChartPointSource.generateTooltipEntry("Parent Equipment", parentEquipmentName);
                this.m_tooltipBase += ChartPointSource.generateTooltipEntry("Location", locationName);
                await this.updateValues();

                switch (this.config.nameDisplay)
                {
                    case Models.ControlPointDisplayType.LocationOnly:
                        this.name = locationName;
                        break;

                    case Models.ControlPointDisplayType.FullLocationOnly:
                        this.name = this.m_cpData.fullLocationName;
                        break;

                    case Models.ControlPointDisplayType.EquipmentOnly:
                        this.name = parentEquipmentName;
                        break;

                    default:
                        this.name = controlPointName;
                        break;
                }
            }
            this.renderTooltip(this.m_tooltipRenderEvent);

            this.markForCheck();
        }
    }

    private clearDbSub()
    {
        if (this.m_dbSub)
        {
            this.removeSubscription(this.m_dbSub);
            this.m_dbSub = null;
        }
    }

    private async updateValues()
    {
        let tooltip = "";
        if (this.hasPoint)
        {
            let lastValue = await this.m_cpData.point.getLastValue(DeviceElementExtended.PRESENT_VALUE, this.m_unitsFactors);
            if (lastValue)
            {
                let value        = lastValue.value;
                let valueDisplay = await this.m_cpData.point.getEnumValueDisplay(value, DeviceElementExtended.PRESENT_VALUE);
                if (!valueDisplay)
                {
                    let unitsExt     = await this.app.domain.units.resolveDescriptor(this.m_unitsFactors, false);
                    let unitsDisplay = unitsExt?.model.displayName || "";

                    let colorConfigExt        = new ColorConfigurationExtended(this.config.color);
                    let stepwiseColorComputer = new StepwiseColorMapper(colorConfigExt.computeStops());
                    this.entryColor           = stepwiseColorComputer.getColor(value);

                    if (typeof value == "number")
                    {
                        let precision = this.config.valuePrecision || 0;
                        value         = isNaN(value) ? "N/A" : value.toLocaleString(undefined, {
                            minimumFractionDigits: precision,
                            maximumFractionDigits: precision
                        });
                    }

                    unitsDisplay      = `${value} ${unitsDisplay}`;
                    tooltip += ChartPointSource.generateTooltipEntry("Value", unitsDisplay);
                    this.valueDisplay = this.config.valueUnitsEnabled ? unitsDisplay : value;
                }
                else
                {
                    tooltip += ChartPointSource.generateTooltipEntry("Value", valueDisplay);
                    this.valueDisplay = valueDisplay;
                }

                let moment            = MomentHelper.parse(lastValue.timestamp);
                let defaultFormat     = MomentHelper.friendlyFormatVerboseUS(moment);
                this.timestampDisplay = this.config.timestampFormat ? moment.format(this.config.timestampFormat) : defaultFormat;
                tooltip += ChartPointSource.generateTooltipEntry("Timestamp", defaultFormat, false);
            }
        }

        this.updateFontSize();

        this.m_tooltip = this.m_tooltipBase + tooltip;
        this.renderTooltip(this.m_tooltipRenderEvent);

        this.markForCheck();
    }

    protected fontSizeUpdated()
    {
        super.fontSizeUpdated();

        this.updateFontSize();
    }

    private updateFontSize()
    {
        let height = this.heightRaw;
        let width  = this.widthRaw;
        if (height > 0 && width > 0)
        {
            const textValues: string[] = [];
            if (this.config.nameEnabled) textValues.push(this.name);
            if (this.config.valueEnabled) textValues.push(this.valueDisplay);

            this.entryPadding = Math.max(height, width) / 100;

            let fontSize = this.manualFontSize;
            if (!fontSize)
            {
                let longest = 0;
                for (let i = 1; i < textValues.length; i++)
                {
                    if (textValues[i]?.length > textValues[longest]?.length) longest = i;
                }

                width             = width - 2 * this.entryPadding;
                const entryHeight = height / textValues.length - 2 * this.entryPadding;
                fontSize          = this.m_textMeasurer.computeMaxFontSize(textValues[longest], this.m_font, width, entryHeight, undefined, 2);
                fontSize          = Math.min(entryHeight / this.m_font.lineHeight, fontSize - 1); // subtracting because ellipsis application is a little greedy
            }
            const fontSizeCss = fontSize + "px";

            this.timestampFontSize         = fontSize < this.app.css.BaseFontSize.asNumber ? fontSizeCss : null;
            this.entryStyling["font-size"] = fontSizeCss;
        }
    }

    public renderTooltip(event: MouseEvent)
    {
        if (event && this.hasPoint)
        {
            let widgetRect = this.element.nativeElement.getBoundingClientRect();
            this.tooltip.render(event.x - widgetRect.x, event.y - widgetRect.y, this.m_tooltip);
        }
        else
        {
            this.tooltip.remove();
        }

        this.m_tooltipRenderEvent = event;
    }

    public async refreshSize(): Promise<boolean>
    {
        this.updateFontSize();
        this.markForCheck();

        return true;
    }

    protected getClipboardData(): ClipboardEntryData<Models.ControlPointWidgetConfiguration, null>
    {
        let model = Models.ControlPointWidgetConfiguration.deepClone(this.config);

        return new class extends ClipboardEntryData<Models.ControlPointWidgetConfiguration, null>
        {
            constructor()
            {
                super("control point");
            }

            public getDashboardWidget(): Models.ControlPointWidgetConfiguration
            {
                return Models.ControlPointWidgetConfiguration.deepClone(model);
            }

            public getReportItem(): null
            {
                return null;
            }
        }();
    }
}

@WidgetDef({
               friendlyName      : "Control Point",
               typeName          : "CONTROL_POINT",
               model             : Models.ControlPointWidgetConfiguration,
               component         : ControlPointWidgetComponent,
               dashboardCreatable: true,
               subgroupCreatable : true,
               maximizable       : true,
               defaultWidth      : 6,
               defaultHeight     : 3,
               hostScalableText  : true,
               needsProtector    : false,
               documentation     : {
                   description: "The Control Point widget allows the selection and display of a single control point's current (most recent) value with fine-grained control on how the value is displayed.",
                   examples   : [
                       {
                           file       : "widgets/CONTROL_POINT/value.png",
                           label      : "Value Only",
                           description: "A control point widget showcasing the current value, displayed as a 2x2 widget."
                       },
                       {
                           file       : "widgets/CONTROL_POINT/details.png",
                           label      : "Name, Value and Timestamp",
                           description: "A control point widget showcasing name, current value and timestamp, displayed as a 2x3 widget."
                       }
                   ]
               }

           })
export class ControlPointWidgetConfigurationExtended extends WidgetConfigurationExtended<Models.ControlPointWidgetConfiguration>
{
    protected initializeForWizardInner()
    {
        this.model.manualFontScaling = true;
        this.model.fontMultiplier    = 1;
        this.model.nameEnabled       = true;
        this.model.valueEnabled      = true;
        this.model.valueUnitsEnabled = true;
        this.model.valuePrecision    = 2;
        this.model.timestampEnabled  = true;
        this.model.nameAlignment     = this.model.valueAlignment = this.model.timestampAlignment = Models.HorizontalAlignment.Center;
    }

    public getBindings(): Models.AssetGraphBinding[]
    {
        let pointInput = this.model.pointInput;
        return pointInput ? [Models.AssetGraphBinding.deepClone(pointInput)] : [];
    }
}
