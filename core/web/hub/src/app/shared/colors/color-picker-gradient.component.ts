import {Component, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {ColorPickerGradientStopComponent} from "app/shared/colors/color-picker-gradient-stop.component";
import {Lookup} from "framework/services/utils.service";

import {ChartColorUtilities, ColorScheme} from "framework/ui/charting/core/colors";
import {ControlOption} from "framework/ui/control-option";
import {SelectComponent} from "framework/ui/forms/select.component";

@Component({
               selector   : "o3-color-picker-gradient",
               templateUrl: "./color-picker-gradient.component.html",
               styleUrls  : ["./color-picker-gradient.component.scss"]
           })
export class ColorPickerGradientComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() onlyEmitWhenValid: boolean = false;

    @Input() maxStops: number;
    @Input() lockEndModes: boolean;

    private m_model: Models.ColorSegment[] = ColorConfigurationExtended.newModel().segments;
    @Input() set model(model: Models.ColorSegment[])
    {
        if (!model) return;
        this.m_model = model;

        if (model.length > 0)
        {
            let startStop   = model[0];
            this.startColor = startStop.color;
            this.startValue = startStop.stopPoint === Models.ColorStopPoint.CUSTOM ? startStop.stopPointValue : undefined;
        }

        if (model.length > 1)
        {
            let endStop   = model[1];
            this.endColor = endStop.color;
            this.endValue = endStop.stopPoint === Models.ColorStopPoint.CUSTOM ? endStop.stopPointValue : undefined;
        }
    }

    get model(): Models.ColorSegment[]
    {
        return this.m_model;
    }

    @Output() modelChange: EventEmitter<Models.ColorSegment[]> = new EventEmitter<Models.ColorSegment[]>();

    @ViewChild(ColorPickerGradientStopComponent, {static: true}) gradientStopPicker: ColorPickerGradientStopComponent;
    @ViewChild("test_gradients") test_gradients: SelectComponent<string>;

    public startColor: string = ColorScheme.defaultColors[0];
    public endColor: string   = ColorScheme.defaultColors[1];

    public startValue: number;
    public endValue: number;

    public selectedPreset: string = ChartColorUtilities.defaultGradient;
    public gradientOptions: ControlOption<string>[];
    public gradientPresets: Lookup<Models.ColorSegment[]>;

    constructor(inj: Injector)
    {
        super(inj);

        let gradientPresetNames = ChartColorUtilities.gradientPresetNames();

        this.gradientOptions = gradientPresetNames.map((name) => new ControlOption(name, name));
        this.gradientPresets = {};
        gradientPresetNames.forEach((name) =>
                                    {
                                        let colorScheme            = ChartColorUtilities.getGradientPreset(name);
                                        this.gradientPresets[name] = [
                                            Models.ColorSegment.newInstance(
                                                {
                                                    color    : colorScheme.colors[0],
                                                    stopPoint: Models.ColorStopPoint.MIN
                                                }),
                                            Models.ColorSegment.newInstance({
                                                                                color    : colorScheme.colors[1],
                                                                                stopPoint: Models.ColorStopPoint.MAX
                                                                            })
                                        ];
                                    });
    }

    public updateStops(stops: Models.ColorSegment[])
    {
        this.model = stops;
        this.updatePreset();
        if (!this.onlyEmitWhenValid || ColorPickerGradientComponent.isValidGradient(stops)) this.modelChange.emit(stops);
    }

    private updatePreset()
    {
        if (!this.selectedPreset) return;
        let gradientPreset = ChartColorUtilities.getGradientPreset(this.selectedPreset);
        if (gradientPreset.colors[0] !== this.startColor || gradientPreset.colors[1] !== this.endColor)
        {
            this.selectedPreset = null;
        }
    }

    public setToPreset(presetGradientName: string)
    {
        let presetGradient                          = ChartColorUtilities.getGradientPreset(presetGradientName);
        this.m_model[0].color                       = presetGradient.colors[0];
        this.m_model[this.m_model.length - 1].color = presetGradient.colors[1];
        this.updateStops(this.m_model.map((stop) => stop));
    }

    private static isValidGradient(stops: Models.ColorSegment[]): boolean
    {
        if (!stops || stops.length < 2) return false;

        let [startStop, endStop] = stops;
        if (!startStop || !endStop) return false;

        if (!startStop.color || !endStop.color) return false;

        if (startStop.stopPoint === Models.ColorStopPoint.CUSTOM && typeof startStop.stopPointValue != "number") return false;
        if (endStop.stopPoint === Models.ColorStopPoint.CUSTOM && typeof endStop.stopPointValue != "number") return false;

        if (startStop.stopPoint === Models.ColorStopPoint.CUSTOM && endStop.stopPoint === Models.ColorStopPoint.CUSTOM)
        {
            return startStop.stopPointValue < endStop.stopPointValue;
        }

        return true;
    }
}
