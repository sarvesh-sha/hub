import {Component, ElementRef, ViewChild} from "@angular/core";

import {WidgetEditorWizardWidgetPreviewStep} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-widget-preview-step.directive";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {EngineeringUnitsDescriptorExtended, UnitsService} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {getBindingName} from "app/shared/assets/configuration/graph-configuration-host";
import {DataSourceWizardDialogComponent, DataSourceWizardPurpose, DataSourceWizardState} from "app/shared/charting/data-source-wizard/data-source-wizard-dialog.component";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";

import {UtilsService} from "framework/services/utils.service";
import {ColorSegmentInterpolationMode} from "framework/ui/charting/core/colors";
import {ControlOption} from "framework/ui/control-option";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-control-point-step",
               templateUrl: "./widget-editor-wizard-control-point-step.component.html",
               styleUrls  : ["./widget-editor-wizard-control-point-step.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardControlPointStepComponent)]
           })
export class WidgetEditorWizardControlPointStepComponent extends WidgetEditorWizardWidgetPreviewStep<Models.ControlPointWidgetConfiguration>
{
    private static readonly emptyCp = "Select control point";

    private cpWizardState: DataSourceWizardState;

    cpExt: DeviceElementExtended;
    cpName: string = WidgetEditorWizardControlPointStepComponent.emptyCp;
    cpUnits: Models.EngineeringUnitsFactors;

    updatingConfig: boolean;

    displayOptions: ControlOption<Models.ControlPointDisplayType>[] = [];

    mappingSegments: Models.ColorSegment[]       = [];
    interpolation: ColorSegmentInterpolationMode = ColorSegmentInterpolationMode.STEP;
    colorMappingTooltip: string                  = "Color mapping based upon control point's current value";

    private m_defaultFormat: string = "ddd MMM D h:mm:ss a";
    formatterTooltip: string;

    fontMultiplierValid: boolean = true;

    get typedWidget(): Models.ControlPointWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.ControlPointWidgetConfiguration);
    }

    get isEnumeratedSource(): boolean
    {
        return this.cpExt && !this.cpUnits;
    }

    get nameEnabled(): boolean
    {
        return this.typedWidget?.nameEnabled;
    }

    set nameEnabled(show: boolean)
    {
        this.updateWidget((widget) => widget.nameEnabled = show);
    }

    get nameDisplay(): Models.ControlPointDisplayType
    {
        return this.typedWidget?.nameDisplay || Models.ControlPointDisplayType.NameOnly;
    }

    set nameDisplay(display: Models.ControlPointDisplayType)
    {
        this.updateWidget((widget) => widget.nameDisplay = display);
    }

    get nameAlignment(): Models.HorizontalAlignment
    {
        return this.typedWidget?.nameAlignment || Models.HorizontalAlignment.Center;
    }

    set nameAlignment(alignment: Models.HorizontalAlignment)
    {
        this.updateWidget((widget) => widget.nameAlignment = alignment);
    }

    get valueEnabled(): boolean
    {
        return this.typedWidget?.valueEnabled;
    }

    set valueEnabled(show: boolean)
    {
        this.updateWidget((widget) => widget.valueEnabled = show);
    }

    get valuePrecision(): number
    {
        if (this.isEnumeratedSource) return undefined;
        return this.typedWidget?.valuePrecision || 0;
    }

    set valuePrecision(precision: number)
    {
        let valuePrecision = UtilsService.clamp(0, 7, precision);
        this.updateWidget((widget) => widget.valuePrecision = valuePrecision);
    }

    get precisionTooltip(): string
    {
        if (this.isEnumeratedSource) return "Not configurable for enumerated sources";
        return "Number of digits after the decimal";
    }

    private m_unitsExt: EngineeringUnitsDescriptorExtended;
    get unitsExt(): EngineeringUnitsDescriptorExtended
    {
        return this.m_unitsExt;
    }

    set unitsExt(unitsExt: EngineeringUnitsDescriptorExtended)
    {
        this.updateUnitsExt(unitsExt);
    }

    get noUnitText(): string
    {
        if (this.cpExt || this.cpName === WidgetEditorWizardControlPointStepComponent.emptyCp) return "No units";

        return "Unknown units";
    }

    get valueUnitsEnabled(): boolean
    {
        if (this.isEnumeratedSource) return false;
        if (!this.valueEnabled) return false;
        return this.typedWidget?.valueUnitsEnabled;
    }

    set valueUnitsEnabled(show: boolean)
    {
        this.updateWidget((widget) => widget.valueUnitsEnabled = show);
    }

    get valueAlignment(): Models.HorizontalAlignment
    {
        return this.typedWidget?.valueAlignment || Models.HorizontalAlignment.Center;
    }

    set valueAlignment(alignment: Models.HorizontalAlignment)
    {
        this.updateWidget((widget) => widget.valueAlignment = alignment);
    }

    get timestampEnabled(): boolean
    {
        return this.typedWidget?.timestampEnabled;
    }

    set timestampEnabled(show: boolean)
    {
        this.updateWidget((widget) => widget.timestampEnabled = show);
    }

    get timestampFormat(): string
    {
        return this.typedWidget?.timestampFormat || "";
    }

    set timestampFormat(format: string)
    {
        this.updateWidget((widget) => widget.timestampFormat = format);
    }

    get formatterPlaceholder(): string
    {
        if (this.timestampFormat) return "";
        return this.m_defaultFormat;
    }

    get timestampAlignment(): Models.HorizontalAlignment
    {
        return this.typedWidget?.timestampAlignment || Models.HorizontalAlignment.Center;
    }

    set timestampAlignment(alignment: Models.HorizontalAlignment)
    {
        this.updateWidget((widget) => widget.timestampAlignment = alignment);
    }

    get manualFontScaling(): boolean
    {
        return this.typedWidget?.manualFontScaling;
    }

    set manualFontScaling(manual: boolean)
    {
        let fontMultiplier = manual ? this.m_fontMultiplier : 0;
        this.updateWidget((widget) =>
                          {
                              widget.manualFontScaling = manual;
                              widget.fontMultiplier    = fontMultiplier;
                          });
    }

    private m_fontMultiplier: number = 1;
    get fontMultiplier(): number
    {
        return this.m_fontMultiplier;
    }

    set fontMultiplier(multiplier: number)
    {
        this.m_fontMultiplier = multiplier;
        this.updateWidget((widget) => widget.fontMultiplier = multiplier);
    }

    @ViewChild("test_cpTrigger", {read: ElementRef}) test_cpTrigger: ElementRef;
    @ViewChild("test_timestampToggle", {read: ElementRef}) test_timestampToggle: ElementRef;
    @ViewChild("test_fontScalingToggle", {read: ElementRef}) test_fontScalingToggle: ElementRef;

    protected updatePreviewConfig()
    {
        this.previewConfig = Models.ControlPointWidgetConfiguration.newInstance(this.previewConfig);
    }

    public applyColorConfig(stops: Models.ColorSegment[])
    {
        let color = Models.ColorConfiguration.newInstance({segments: stops});
        this.updateWidget((previewWidget) => previewWidget.color = color);
    }

    public getLabel(): string
    {
        return "Control Point";
    }

    public isEnabled(): boolean
    {
        return !!this.typedWidget;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        let widget = this.typedWidget;
        if (!widget) return false;
        if (!widget.pointInput && !widget.pointId) return false;
        if (!isNaN(widget.valuePrecision) && widget.valuePrecision < 0) return false;
        if (!widget.nameEnabled && !widget.valueEnabled && !widget.timestampEnabled) return false;
        if (this.manualFontScaling && !this.fontMultiplierValid) return false;

        return true;
    }

    public onNext(): Promise<boolean>
    {
        return undefined;
    }

    public async onStepSelected(): Promise<void>
    {
        await this.initialize();

        this.widgetPreview?.refresh();
    }

    public async onData(): Promise<any>
    {
        await super.onData();

        this.displayOptions = [
            new ControlOption(Models.ControlPointDisplayType.NameOnly, "Control point"),
            new ControlOption(Models.ControlPointDisplayType.LocationOnly, "Location"),
            new ControlOption(Models.ControlPointDisplayType.FullLocationOnly, "Full location"),
            new ControlOption(Models.ControlPointDisplayType.EquipmentOnly, "Parent equipment")
        ];

        this.m_fontMultiplier = this.data.editor.widget.fontMultiplier || 1;

        await this.initialize();
    }

    public async configureControlPoint()
    {
        if (await DataSourceWizardDialogComponent.open(this.cpWizardState, this.data.host))
        {
            await this.updateConfig();
        }
    }

    private async updateConfig()
    {
        this.updatingConfig = true;

        let cpName;
        let pointId    = null;
        let pointInput = null;

        switch (this.cpWizardState.type)
        {
            case Models.TimeSeriesChartType.STANDARD:
                let id     = this.cpWizardState.ids && this.cpWizardState.ids[0];
                this.cpExt = await this.data.app.domain.assets.getTypedExtendedByIdentity(DeviceElementExtended, id);
                cpName     = this.cpExt?.model.name;

                if (cpName)
                {
                    pointId = id.sysId;
                }
                break;

            case Models.TimeSeriesChartType.GRAPH:
                pointInput = this.cpWizardState.graphBinding;
                if (this.cpWizardState.newSelectorName && this.data.editor.newSelectorNameLookup)
                {
                    this.data.editor.newSelectorNameLookup[pointInput.selectorId] = Models.SharedAssetSelector.newInstance(
                        {
                            id     : pointInput.selectorId,
                            graphId: pointInput.graphId,
                            name   : this.cpWizardState.newSelectorName
                        });
                }

                cpName = getBindingName(this.data.editor.dashboardGraphsHost, pointInput);
                if (cpName)
                {
                    let graphs = await this.data.editor.dashboardGraphsHost.resolveGraphs();
                    let graph  = graphs.get(pointInput.graphId);

                    let identity: Models.RecordIdentity;
                    let context = await this.data.editor.dashboardExt.getGraphContext(pointInput.graphId);
                    if (context?.nodeId)
                    {
                        let resolved = await graph.resolveWithContext([Models.AssetGraphContextAsset.newInstance({sysId: context.nodeId})]);
                        identity     = resolved.resolveIdentities(pointInput)[0];
                    }

                    if (!identity)
                    {
                        let resolved = await graph.resolve();
                        identity     = resolved.resolveIdentities(pointInput)[0];
                    }

                    this.cpExt = await this.data.app.domain.assets.getTypedExtendedByIdentity(DeviceElementExtended, identity);
                }
                break;
        }

        let widget  = this.typedWidget;
        let schema  = await this.cpExt?.getSchemaProperty(DeviceElementExtended.PRESENT_VALUE);
        let cpUnits = schema?.unitsFactors;
        if (cpUnits)
        {
            let units = widget.valueUnits;
            if (!UnitsService.areEquivalent(units, cpUnits))
            {
                units = await this.data.app.domain.units.findPreferred(cpUnits);
            }
            await this.updateUnitsExt(await this.data.app.domain.units.resolveDescriptor(units, false));
        }
        else
        {
            // clear selected units: let units be handled by widget
            this.unitsExt = null;
        }
        this.cpUnits = cpUnits;

        this.cpName = cpName || WidgetEditorWizardControlPointStepComponent.emptyCp;

        widget.pointId    = pointId;
        widget.pointInput = pointInput;

        this.previewConfig         = Models.ControlPointWidgetConfiguration.deepClone(widget);
        this.previewConfig.pointId = this.cpExt?.model.sysId;
        delete this.previewConfig.pointInput;

        this.updatingConfig = false;
    }

    private async updateUnitsExt(unitsExt: EngineeringUnitsDescriptorExtended)
    {
        this.m_unitsExt = unitsExt;

        let currUnits = this.typedWidget.valueUnits;
        let newUnits  = EngineeringUnitsDescriptorExtended.extractFactors(unitsExt);
        if (currUnits && UnitsService.areEquivalent(currUnits, newUnits))
        {
            await ColorConfigurationExtended.convertUnits(this.data.app.domain.units, this.typedWidget.color, this.typedWidget.valueUnits, newUnits);
            this.resetMappingSegments();
        }

        this.updateWidget((widget) => widget.valueUnits = newUnits);
    }

    private async initialize()
    {
        let widget = this.typedWidget;
        if (!this.cpWizardState && widget)
        {
            this.cpWizardState = new DataSourceWizardState(true, DataSourceWizardPurpose.dashboard, null, this.data.editor.dashboardGraphsHost, true);
            this.cpWizardState.updateForControlPointWidget(widget);
            await this.updateConfig();

            if (!widget.color?.segments) widget.color = ColorConfigurationExtended.defaultWidgetModel();
            this.resetMappingSegments();

            let now               = MomentHelper.now();
            let exampleFormats    = [
                "dddd",
                "ddd dddd",
                "h:mm a",
                "YY [insert text by wrapping in square brackets] YYYY",
                "D[, ]Do",
                this.m_defaultFormat
            ];
            this.formatterTooltip = "";
            for (let i = 0; i < exampleFormats.length; i++)
            {
                let exampleFormat = exampleFormats[i];
                this.formatterTooltip += `'${exampleFormat}' : ${now.format(exampleFormat)}`;
                if (i < exampleFormats.length - 1) this.formatterTooltip += "\n";
            }
        }
    }

    private resetMappingSegments()
    {
        if (this.typedWidget?.color?.segments)
        {
            this.mappingSegments = this.typedWidget.color.segments.map((segment) => Models.ColorSegment.newInstance(segment));
        }
    }
}
