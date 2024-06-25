import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from "@angular/core";
import * as chroma from "chroma-js";

@Component({
               selector   : "o3-color-builder",
               templateUrl: "./color-builder.component.html",
               styleUrls  : ["./color-builder.component.scss"]
           })
export class ColorBuilderComponent
{
    public hexInput: string   = "#";
    public alphaInput: number = 100;

    private m_chroma: chroma.Color;

    public alpha: number = 1;
    public red: number   = 0;
    public blue: number  = 0;
    public green: number = 0;

    @Input() editableOpacity: boolean = true;

    private m_color: string;
    @Input() set color(color: string)
    {
        let chromaColor = ColorBuilderComponent.safeChroma(color);
        if (chromaColor)
        {
            this.m_chroma = chromaColor;
            this.alpha    = <number>this.m_chroma.alpha();

            if (this.hexElem && this.hexElem.nativeElement !== document.activeElement) this.hexInput = this.hex;

            if (this.alphaElem && this.alphaElem.nativeElement !== document.activeElement)
            {
                this.alphaInput = parseInt(((<number>this.m_chroma.alpha()) * 100).toFixed(0));
            }

            [
                this.red,
                this.green,
                this.blue
            ] = this.m_chroma.rgb();

            this.colorChange.emit(this.m_color = color);
        }
    }

    @Input() disabled: boolean = false;

    get color(): string
    {
        return this.m_color;
    }

    get hex(): string
    {
        return this.m_chroma && this.m_chroma.hex("rgb");
    }

    get rgba(): string
    {
        return this.m_chroma && this.m_chroma.css() || "black";
    }

    @ViewChild("hexInputElement", {static: true}) hexElem: ElementRef;
    @ViewChild("alphaInputElement") alphaElem: ElementRef;

    @ViewChild("defaultColorTrigger", {static: true}) defaultColorTrigger: ElementRef;

    @Output() colorChange: EventEmitter<string> = new EventEmitter();

    processHex(event: KeyboardEvent)
    {
        if (this.hexInput.length >= 7 &&
            !(event.getModifierState("Control") || event.getModifierState("Meta")) &&
            !(event.key === "Backspace" || event.key === "Delete"))
        {
            event.preventDefault();
        }
    }

    processHexInput(input: string)
    {
        if (input && input.length >= 6) this.updateColor(input);
    }

    processAlphaInput()
    {
        if (this.alphaInput >= 0 && this.alphaInput <= 100) this.alpha = this.alphaInput / 100;
    }

    updateFromColorComponent()
    {
        this.color = "rgba(" + [
            this.red,
            this.green,
            this.blue,
            this.alpha
        ].join(",") + ")";
    }

    triggerDefaultColorPicker()
    {
        if (this.defaultColorTrigger) this.defaultColorTrigger.nativeElement.click();
    }

    private updateColor(opaqueColor: string)
    {
        let chroma = ColorBuilderComponent.safeChroma(opaqueColor);
        if (chroma)
        {
            this.color = chroma.alpha(this.alpha)
                               .css();
        }
    }

    private static safeChroma(color: string): chroma.Color
    {
        try
        {
            return chroma(color);
        }
        catch
        {
            return null;
        }
    }
}
