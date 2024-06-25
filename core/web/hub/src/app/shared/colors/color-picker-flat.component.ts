import {Component, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {ColorPaletteComponent} from "app/shared/colors/color-palette.component";

import {ChartColorUtilities, PaletteId} from "framework/ui/charting/core/colors";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-color-picker-flat",
               templateUrl: "./color-picker-flat.component.html",
               styleUrls  : ["./color-picker-flat.component.scss"]
           })
export class ColorPickerFlatComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() disabled: boolean = false;
    @Input() maxNumSelections: number;
    @Input() placeholder: string;

    @Input() onlyPalette: boolean = false;
    @Input() hidePalette: boolean = false;

    private m_colorPalette: PaletteId;
    private m_selectedPalette: PaletteId;

    @Input() set palette(palette: PaletteId)
    {
        this.m_selectedPalette = palette;
    }

    get palette(): PaletteId
    {
        return this.m_selectedPalette ?? this.m_colorPalette ?? ChartColorUtilities.defaultPalette;
    }

    @Output() paletteChange: EventEmitter<PaletteId> = new EventEmitter();

    private m_color: string;
    @Input() set color(color: string)
    {
        this.m_color        = color;
        this.m_colorPalette = ChartColorUtilities.getPaletteFromColor(color) || this.m_colorPalette;
    }

    get color(): string
    {
        return this.m_color;
    }

    @Output() colorChange: EventEmitter<string> = new EventEmitter();

    @ViewChild("test_colorPalette") test_colorPalette: ColorPaletteComponent;

    public paletteOptions: ControlOption<string>[];

    constructor(inj: Injector)
    {
        super(inj);

        this.paletteOptions = ChartColorUtilities.paletteOptions();
    }

    public updateColor(color: string)
    {
        if (color && this.color !== color)
        {
            this.colorChange.emit(this.color = color);
        }
    }

    // calculates the number of rows necessary for palette of width 5 color blocks
    public getPaletteRows(paletteName: PaletteId): number
    {
        return Math.ceil(ChartColorUtilities.getPaletteOptions(paletteName).length / 5);
    }
}
