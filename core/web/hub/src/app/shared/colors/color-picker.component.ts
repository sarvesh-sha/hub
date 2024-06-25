import {FocusMonitor} from "@angular/cdk/a11y";
import {Component, ElementRef, EventEmitter, HostBinding, Injector, Input, Optional, Output, Self, SimpleChanges, ViewChild} from "@angular/core";
import {ControlValueAccessor, NgControl} from "@angular/forms";
import {MatFormFieldControl} from "@angular/material/form-field";

import {ColorPickerConfigurationComponent} from "app/shared/colors/color-picker-configuration.component";
import {ColorPickerFlatComponent} from "app/shared/colors/color-picker-flat.component";
import {PaletteId} from "framework/ui/charting/core/colors";
import * as SharedSvc from "framework/ui/components";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {Subject} from "rxjs";

@Component({
               selector   : "o3-color-picker",
               templateUrl: "./color-picker.component.html",
               styleUrls  : ["./color-picker.component.scss"],
               providers  : [
                   {
                       provide    : MatFormFieldControl,
                       useExisting: ColorPickerComponent
                   }
               ]
           })
export class ColorPickerComponent extends SharedSvc.BaseComponent implements ControlValueAccessor,
                                                                             MatFormFieldControl<string>
{
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_disabled: boolean | "";
    static ngAcceptInputType_required: boolean | "";

    public static nextId: number      = 0;
    @HostBinding() public readonly id = `o3-color-picker-${ColorPickerComponent.nextId++}`;

    // -- //

    private m_value: string;
    get value(): string
    {
        return this.m_value;
    }

    @Input()
    set value(value: string)
    {
        this.m_value = value;
    }

    // -- //

    @Input() palette: PaletteId;
    @Input() placeholder: string;
    @Input() @CoerceBoolean() required: boolean;
    @Input() @CoerceBoolean() disabled: boolean;

    // -- //

    get empty(): boolean
    {
        return !this.m_value;
    }

    get errorState(): boolean
    {
        return this.ngControl && this.ngControl.errors && this.ngControl.errors.length > 0;
    }

    // -- //

    @ViewChild(OverlayComponent, {static: true}) colorOverlay: OverlayComponent;
    @ViewChild("test_trigger", {read: ElementRef}) test_trigger: ElementRef;
    @ViewChild("test_color") test_color: ColorPickerFlatComponent;

    public focused: boolean;
    public readonly shouldLabelFloat: boolean = true;
    public readonly controlType: string       = "o3-color-picker";

    // -- //

    public readonly stateChanges: Subject<void>      = new Subject();
    @Output() valueChange: EventEmitter<string>      = new EventEmitter();
    @Output() paletteChange: EventEmitter<PaletteId> = new EventEmitter();

    public overlayConfig: OverlayConfig;

    private m_onTouched: () => void;
    private m_onChange: (value: string) => void;

    constructor(inj: Injector,
                @Optional() @Self() public ngControl: NgControl,
                private fm: FocusMonitor,
                private elementRef: ElementRef)
    {
        super(inj);

        if (this.ngControl) this.ngControl.valueAccessor = this;

        fm.monitor(elementRef.nativeElement, true)
          .subscribe((origin) =>
                     {
                         this.focused = this.colorOverlay?.isOpen || !!origin;
                         this.stateChanges.next();
                     });

        this.overlayConfig = ColorPickerConfigurationComponent.colorOverlayConfig(true);
    }

    public ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        this.stateChanges.next();
    }

    public ngOnDestroy(): void
    {
        super.ngOnDestroy();

        this.fm.stopMonitoring(this.elementRef.nativeElement);
        this.stateChanges.complete();
    }

    public submitChange()
    {
        this.valueChange.emit(this.m_value);
        if (this.m_onChange) this.m_onChange(this.value);
    }

    public onContainerClick(event: MouseEvent): void
    {
        if ((event.target as Element).tagName.toLowerCase() != "button")
        {
            this.toggleColorOverlay();
        }
    }

    public toggleColorOverlay(): void
    {
        if (!this.disabled || this.colorOverlay && this.colorOverlay.isOpen) this.colorOverlay.toggleOverlay();
    }

    public registerOnChange(fn: any): void
    {
        this.m_onChange = fn;
    }

    public registerOnTouched(fn: any): void
    {
        this.m_onTouched = fn;
    }

    @HostBinding("attr.aria-describedby") describedBy = "";

    public setDescribedByIds(ids: string[])
    {
        this.describedBy = ids.join(" ");
    }

    public setDisabledState(isDisabled: boolean): void
    {
        this.disabled = isDisabled;
        this.stateChanges.next();
    }

    public writeValue(value: string): void
    {
        this.value = value;
        this.stateChanges.next();
    }
}
