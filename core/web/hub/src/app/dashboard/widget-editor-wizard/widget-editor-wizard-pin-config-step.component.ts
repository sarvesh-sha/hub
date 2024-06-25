import {Component, ElementRef, QueryList, ViewChildren} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {MapPinIcon} from "app/services/proxy/model/models";
import {AppMappingUtilities, IconOption} from "app/shared/mapping/app-mapping.utilities";

import {UtilsService} from "framework/services/utils.service";
import {ChartColorUtilities, ColorSegmentInterpolationMode, PaletteId} from "framework/ui/charting/core/colors";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-pin-config-step",
               templateUrl: "./widget-editor-wizard-pin-config-step.component.html",
               styleUrls  : [
                   "./widget-editor-wizard-dialog.component.scss",
                   "./widget-editor-wizard-pin-config-step.component.scss"
               ],
               providers  : [WizardStep.createProvider(WidgetEditorWizardPinConfigStepComponent)]
           })
export class WidgetEditorWizardPinConfigStepComponent extends WizardStep<WidgetEditorWizardState>
{
    AlertSeverity = Models.AlertSeverity;
    ToggleModes   = ToggleMode;

    readonly interpolationMode  = ColorSegmentInterpolationMode.STEP;
    readonly palette: PaletteId = "Map Colors";

    pinSize: number                          = 36;
    pinIcon: MapPinIcon                      = MapPinIcon.Pin;
    icons: IconOption[]                      = AppMappingUtilities.iconOptions();
    toggleMode: ToggleMode                   = ToggleMode.Count;
    colorMode: Models.AlertMapPinColorMode   = Models.AlertMapPinColorMode.Dynamic;
    dataSource: Models.AlertMapPinDataSource = Models.AlertMapPinDataSource.AlertCount;
    staticColor: string                      = ChartColorUtilities.getColorById("Map Colors", "mapblue").hex;

    severityColors: Models.AlertMapSeverityColor[];
    severityColorsMap: Map<Models.AlertSeverity, Models.AlertMapSeverityColor> = this.mapSeverityColors([]);
    countColors: Models.ColorSegment[]                                         = this.defaultCountColors();

    alertSeverityLabels: Map<Models.AlertSeverity, string> = new Map<Models.AlertSeverity, string>();

    @ViewChildren("test_iconTypeRadio", {read: ElementRef}) test_iconTypes: QueryList<ElementRef>;

    get typedWidget(): Models.AlertMapWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.AlertMapWidgetConfiguration);
    }

    public async onData()
    {
        await super.onData();

        // Get all alert severity enum options
        let alertSeverities = await this.data.app.domain.alerts.getSeverities();

        // Map alert severity enums to labels
        this.alertSeverityLabels = new Map();
        for (let severity of alertSeverities)
        {
            this.alertSeverityLabels.set(severity.id, severity.label);
        }

        let cfg = this.typedWidget;
        if (cfg)
        {
            // Restore pin settings
            if (cfg.pin)
            {
                // Restore any saved severity color mapping
                if (cfg.pin.severityColors)
                {
                    this.severityColorsMap = this.mapSeverityColors(cfg.pin.severityColors);
                    this.onSeverityColorChange();
                }

                // Restore and saved color stops
                this.countColors = cfg.pin.countColors || this.defaultCountColors();

                // Restore any other state
                if (cfg.pin.colorMode) this.colorMode = cfg.pin.colorMode;
                if (cfg.pin.dataSource) this.dataSource = cfg.pin.dataSource;
                if (cfg.pin.staticColor) this.staticColor = cfg.pin.staticColor;
                if (cfg.pin.pinSize) this.pinSize = cfg.pin.pinSize;
                if (cfg.pin.pinIcon) this.pinIcon = cfg.pin.pinIcon;

                this.determineToggleMode();
            }
            else
            {
                // Initialize pin
                cfg.pin = Models.AlertMapPinConfig.newInstance({});
            }
        }
    }

    public getLabel() { return "Pin"; }

    public isEnabled(): boolean
    {
        return this.data.editor.allowWidgetTypes(Models.AlertMapWidgetConfiguration);
    }

    public isValid(): boolean
    {
        return true;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }

    public severityLabel(severity: Models.AlertSeverity): string
    {
        return `${this.alertSeverityLabels.get(severity)} Pin Color`;
    }

    public getExample(icon: IconOption)
    {
        let size = this.pinSize || 64;

        return this.bypassSecurityTrustHtml(icon.renderer(`${size}`, size, ChartColorUtilities.getColorById("Map Colors", "mapred").hex));
    }

    onSeverityColorChange()
    {
        this.severityColors = this.flattenSeverityColors(this.severityColorsMap);
    }

    colorToggleChange(mode: ToggleMode)
    {
        // Set value
        this.toggleMode = mode;
        // Set related modes
        this.applyToggleMode(mode);
        // Update config
        this.updatePinConfig();
    }

    updatePinConfig()
    {
        if (this.data.editor.widget instanceof Models.AlertMapWidgetConfiguration)
        {
            if (!this.data.editor.widget.pin) this.data.editor.widget.pin = Models.AlertMapPinConfig.newInstance({});

            // Save configuration
            let pin            = this.data.editor.widget.pin;
            pin.pinIcon        = this.pinIcon;
            pin.pinSize        = this.pinSize;
            pin.staticColor    = this.staticColor;
            pin.colorMode      = this.colorMode;
            pin.dataSource     = this.dataSource;
            pin.countColors    = this.countColors;
            pin.severityColors = this.severityColors;
        }
    }

    private mapSeverityColors(flat: Models.AlertMapSeverityColor[]): Map<Models.AlertSeverity, Models.AlertMapSeverityColor>
    {
        let map = new Map<Models.AlertSeverity, Models.AlertMapSeverityColor>([
                                                                                  [
                                                                                      Models.AlertSeverity.LOW,
                                                                                      Models.AlertMapSeverityColor.newInstance({
                                                                                                                                   severity: Models.AlertSeverity.LOW,
                                                                                                                                   color   : ChartColorUtilities.getColorById("Map Colors",
                                                                                                                                                                              "mapgreen").hex
                                                                                                                               })
                                                                                  ],
                                                                                  [
                                                                                      Models.AlertSeverity.NORMAL,
                                                                                      Models.AlertMapSeverityColor.newInstance({
                                                                                                                                   severity: Models.AlertSeverity.NORMAL,
                                                                                                                                   color   : ChartColorUtilities.getColorById("Map Colors",
                                                                                                                                                                              "mapyellow").hex
                                                                                                                               })
                                                                                  ],
                                                                                  [
                                                                                      Models.AlertSeverity.SIGNIFICANT,
                                                                                      Models.AlertMapSeverityColor.newInstance({
                                                                                                                                   severity: Models.AlertSeverity.SIGNIFICANT,
                                                                                                                                   color   : ChartColorUtilities.getColorById("Map Colors",
                                                                                                                                                                              "maporange").hex
                                                                                                                               })
                                                                                  ],
                                                                                  [
                                                                                      Models.AlertSeverity.CRITICAL,
                                                                                      Models.AlertMapSeverityColor.newInstance({
                                                                                                                                   severity: Models.AlertSeverity.CRITICAL,
                                                                                                                                   color   : ChartColorUtilities.getColorById("Map Colors",
                                                                                                                                                                              "mapred").hex
                                                                                                                               })
                                                                                  ]
                                                                              ]);

        // Overwrite defaults with values given
        for (let entry of flat)
        {
            map.set(entry.severity, entry);
        }

        // Return merged map
        return map;
    }

    private flattenSeverityColors(map: Map<Models.AlertSeverity, Models.AlertMapSeverityColor>): Models.AlertMapSeverityColor[]
    {
        return [
            map.get(Models.AlertSeverity.LOW),
            map.get(Models.AlertSeverity.NORMAL),
            map.get(Models.AlertSeverity.SIGNIFICANT),
            map.get(Models.AlertSeverity.CRITICAL)
        ];
    }

    private defaultCountColors(): Models.ColorSegment[]
    {
        return [
            Models.ColorSegment.newInstance({
                                                color    : ChartColorUtilities.getColorById("Map Colors", "mapgreen").hex,
                                                stopPoint: Models.ColorStopPoint.MIN
                                            }),
            Models.ColorSegment.newInstance({
                                                color         : ChartColorUtilities.getColorById("Map Colors", "mapyellow").hex,
                                                stopPoint     : Models.ColorStopPoint.CUSTOM,
                                                stopPointValue: 1
                                            }),
            Models.ColorSegment.newInstance({
                                                color         : ChartColorUtilities.getColorById("Map Colors", "maporange").hex,
                                                stopPoint     : Models.ColorStopPoint.CUSTOM,
                                                stopPointValue: 7
                                            }),
            Models.ColorSegment.newInstance({
                                                color         : ChartColorUtilities.getColorById("Map Colors", "mapred").hex,
                                                stopPoint     : Models.ColorStopPoint.CUSTOM,
                                                stopPointValue: 10
                                            }),
            Models.ColorSegment.newInstance({
                                                color    : ChartColorUtilities.getColorById("Map Colors", "mapred").hex,
                                                stopPoint: Models.ColorStopPoint.MAX
                                            })
        ];
    }

    private applyToggleMode(mode: ToggleMode)
    {
        switch (mode)
        {
            case ToggleMode.Fixed:
                this.colorMode = Models.AlertMapPinColorMode.Fixed;
                break;

            case ToggleMode.Count:
                this.dataSource = Models.AlertMapPinDataSource.AlertCount;
                this.colorMode  = Models.AlertMapPinColorMode.Dynamic;
                break;

            case ToggleMode.Severity:
                this.dataSource = Models.AlertMapPinDataSource.Severity;
                this.colorMode  = Models.AlertMapPinColorMode.Dynamic;
                break;
        }
    }

    private determineToggleMode()
    {
        if (this.colorMode === Models.AlertMapPinColorMode.Fixed)
        {
            this.toggleMode = ToggleMode.Fixed;
        }
        else
        {
            if (this.dataSource === Models.AlertMapPinDataSource.AlertCount)
            {
                this.toggleMode = ToggleMode.Count;
            }
            else
            {
                this.toggleMode = ToggleMode.Severity;
            }
        }
    }
}

enum ToggleMode
{
    Severity,
    Count,
    Fixed
}
