import {Component, EventEmitter, Input, Output} from "@angular/core";
import {ChartColorUtilities, ColorOption, PaletteId} from "framework/ui/charting/core/colors";
import {ControlOption} from "framework/ui/control-option";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";

@Component({
               selector   : "o3-color-palette",
               templateUrl: "./color-palette.component.html",
               styleUrls  : ["./color-palette.component.scss"]
           })
export class ColorPaletteComponent
{
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_interactable: boolean | "";

    private static id    = 0;
    private m_id: number = ColorPaletteComponent.id++;

    private m_paletteId: PaletteId = ChartColorUtilities.defaultPalette;
    @Input() set paletteId(paletteName: PaletteId)
    {
        this.m_paletteId = paletteName;
        this.updateColors();
        this.updateSelectedColor();
    }

    @Input() @CoerceBoolean() interactable: boolean = true;

    private m_selectedOption: ColorSelection;
    private m_selectedColor: string;
    @Input() set selectedColor(color: string)
    {
        this.m_selectedColor = color;
        this.updateSelectedColor();
    }

    @Input() options: ColorSelection[];

    @Output() colorSelection = new EventEmitter<string>();

    constructor()
    {
        this.updateColors();
    }

    public getO3TestId(option: ColorSelection): string
    {
        return `${this.m_id}-${this.m_paletteId}-${option.id.id}-color`;
    }

    public select(option: ColorSelection)
    {
        if (this.interactable)
        {
            let hex            = option.id.hex;
            this.selectedColor = hex;
            this.colorSelection.emit(hex);
        }
    }

    private updateSelectedColor()
    {
        let chromaColor = this.m_selectedColor && ChartColorUtilities.safeChroma(this.m_selectedColor);
        if (chromaColor)
        {
            let hex       = chromaColor.hex();
            let selection = this.options.find((option) => option.id.hex === hex);
            if (this.m_selectedOption)
            {
                this.m_selectedOption.selected = false;
                this.m_selectedOption          = null;
            }

            if (selection)
            {
                this.m_selectedOption          = selection;
                this.m_selectedOption.selected = true;
            }
        }
    }

    public contrast(color: string): string
    {
        return ChartColorUtilities.bestContrastBetween(color, "white", "black", 0.035).css();
    }

    private updateColors()
    {
        this.options = ChartColorUtilities.getColorOptions(this.m_paletteId)
                                          .map((option) => new ColorSelection(option));
    }
}

export class ColorSelection
{
    public selected: boolean = false;

    constructor(private option: ControlOption<ColorOption>)
    {}

    public get id(): ColorOption
    {
        return this.option.id;
    }

    public get label(): string
    {
        return this.option.label;
    }
}
