import {Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {ColorPickerGradientComponent} from "app/shared/colors/color-picker-gradient.component";

import {ChartColorUtilities, PaletteId} from "framework/ui/charting/core/colors";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-color-picker-configuration",
               templateUrl: "./color-picker-configuration.component.html",
               styleUrls  : ["./color-picker-configuration.component.scss"]
           })
export class ColorPickerConfigurationComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() forFlat: boolean     = false;
    @Input() forGradient: boolean = false;

    @Input() onlyPalette: boolean = false;

    @Input() maxStops: number;
    @Input() lockEndModes: boolean;

    @Input() onlyEmitWhenValid: boolean = false;

    private m_model: Models.ColorConfiguration = ColorConfigurationExtended.newModel();
    @Input() set model(model: Models.ColorConfiguration)
    {
        if (!model) return;
        this.m_model  = model;
        this.palette  = <PaletteId>model.paletteName;
        this.segments = model.segments;
        this.single   = model.segments?.[0]?.color;
    }

    @Output() modelChange = new EventEmitter<Models.ColorConfiguration>();

    @ViewChild("test_gradient") test_gradient: ColorPickerGradientComponent;

    public palette: PaletteId = ChartColorUtilities.defaultPalette;

    public single: string;
    public segments: Models.ColorSegment[] = null;

    public updated(fromGradient: boolean)
    {
        this.m_model.paletteName = this.palette;

        if (fromGradient)
        {
            this.m_model.segments = this.segments;
        }
        else if (!this.onlyPalette)
        {
            if (!this.m_model.segments) this.m_model.segments = [];
            this.m_model.segments[0] = Models.ColorSegment.newInstance({color: this.single});
        }

        if (!this.onlyEmitWhenValid || ColorConfigurationExtended.isValid(this.m_model)) this.modelChange.emit(this.m_model);
    }

    public static colorOverlayConfig(draggable: boolean,
                                     overrideWidth?: number): OverlayConfig
    {
        const newOverlayConfig = draggable ? OverlayConfig.onTopDraggable : OverlayConfig.newInstance;
        return newOverlayConfig({
                                    width   : overrideWidth ?? 350,
                                    maxWidth: "95%"
                                });
    }
}
