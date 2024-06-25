import {FocusMonitor} from "@angular/cdk/a11y";
import {CdkOverlayOrigin} from "@angular/cdk/overlay";
import {ChangeDetectionStrategy, Component, ElementRef, HostBinding, Injector, Input, Optional, Self, SimpleChanges, ViewChild} from "@angular/core";
import {ControlValueAccessor, NgControl} from "@angular/forms";
import {MatFormFieldControl} from "@angular/material/form-field";
import {SafeHtml} from "@angular/platform-browser";

import {BaseApplicationComponent} from "app/services/domain/base.service";
import {EngineeringUnitsDescriptorExtended} from "app/services/domain/units.service";

import * as Models from "app/services/proxy/model/models";
import {ControlOption} from "framework/ui/control-option";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {SelectComponent} from "framework/ui/forms/select.component";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {Subject} from "rxjs";

@Component({
               selector       : "o3-unit-editor",
               styleUrls      : ["./unit-editor.component.scss"],
               templateUrl    : "./unit-editor.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush,
               providers      : [
                   {
                       provide    : MatFormFieldControl,
                       useExisting: UnitEditorComponent
                   }
               ]
           })
export class UnitEditorComponent extends BaseApplicationComponent implements ControlValueAccessor,
                                                                             MatFormFieldControl<Models.EngineeringUnitsFactors>
{
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_disabled: boolean | "";
    static ngAcceptInputType_required: boolean | "";
    static ngAcceptInputType_readonly: boolean | "";

    private static nextId = 0;

    @HostBinding()
    public readonly id = `o3-unit-editor-${UnitEditorComponent.nextId++}`;

    @HostBinding("attr.aria-describedby")
    public describedBy = "";

    public readonly controlType = "o3-unit-editor";

    public readonly stateChanges = new Subject<void>();

    public focused: boolean;

    private m_value: Models.EngineeringUnitsFactors;
    @Input() set value(unit: Models.EngineeringUnitsFactors)
    {
        if (unit)
        {
            this.m_value = unit;
        }
        else
        {
            this.m_value = Models.EngineeringUnitsFactors.newInstance({
                                                                          numeratorUnits  : [],
                                                                          denominatorUnits: []
                                                                      });
        }

        this.updateDisplay();
    }

    get value(): Models.EngineeringUnitsFactors
    {
        return this.m_value;
    }

    @Input() @CoerceBoolean() disabled: boolean;
    @Input() @CoerceBoolean() required: boolean;
    @Input() @CoerceBoolean() readonly: boolean;
    @Input() placeholder: string;

    @ViewChild(StandardFormOverlayComponent, {static: true}) overlay: StandardFormOverlayComponent;
    @ViewChild(SelectComponent, {static: true}) selector: SelectComponent<string>;

    constructor(inj: Injector,
                @Self() @Optional() public ngControl: NgControl,
                private fm: FocusMonitor,
                private elementRef: ElementRef)
    {
        super(inj);

        if (this.ngControl)
        {
            this.ngControl.valueAccessor = this;
        }

        fm.monitor(elementRef.nativeElement, true)
          .subscribe((origin) =>
                     {
                         let focused = !!origin;
                         if (this.overlay && this.overlay.isOpen())
                         {
                             this.focused = true;
                         }
                         else
                         {
                             this.focused = focused;
                         }

                         this.stateChanges.next();
                     });
    }

    public async ngOnInit()
    {
        super.ngOnInit();

        let units        = await this.app.domain.units.describeEngineeringUnits();
        this.unitOptions = units.map((unit) => new ControlOption(unit.model.units, unit.controlPointWithDescription.label));
        this.unitsMap    = await this.app.domain.units.mapEngineeringUnits();
        this.detectChanges();
    }

    public ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);
        this.stateChanges.next();
    }

    public ngOnDestroy(): void
    {
        super.ngOnDestroy();
        this.stateChanges.complete();
        this.fm.stopMonitoring(this.elementRef.nativeElement);
    }

    public get empty(): boolean
    {
        return !this.m_value ||
               ((!this.m_value.numeratorUnits || !this.m_value.numeratorUnits.length) &&
                (!this.m_value.denominatorUnits || !this.m_value.denominatorUnits.length));
    }

    public get shouldLabelFloat(): boolean
    {
        return true;
    }

    public get errorState(): boolean
    {
        return this.ngControl && !!this.ngControl.errors;
    }

    public onContainerClick(event: MouseEvent): void
    {
        if (this.disabled || this.readonly)
        {
            return;
        }

        // If they click the container and don't hit the button, open the dropdown
        if ((event.target as Element).tagName.toLowerCase() != "button")
        {
            if (!this.overlay.isOpen())
            {
                this.overlay.openOverlay();
            }
        }
    }

    public setDescribedByIds(ids: string[]): void
    {
        this.describedBy = ids.join(" ");
    }

    //--//
    private m_onChange: (change: any) => void;
    private m_onTouched: () => void;

    public registerOnChange(fn: any): void
    {
        this.m_onChange = fn;
    }

    public registerOnTouched(fn: any): void
    {
        this.m_onTouched = fn;
    }

    public setDisabledState(isDisabled: boolean): void
    {
        this.disabled = isDisabled;
    }

    public writeValue(obj: any): void
    {
        this.value = obj;
    }

    //--//

    overlayConfig                                         = OverlayConfig.dropdown({coverAnchorWhenDisplayed: false});
    editUnit: Models.EngineeringUnits                     = null;
    editList: Models.EngineeringUnits[]                   = null;
    editIndex: number;
    unitOptions: ControlOption<Models.EngineeringUnits>[] = [];
    unitsMap                                              = new Map<Models.EngineeringUnits, EngineeringUnitsDescriptorExtended>();
    unitDisplay: SafeHtml;

    isEditing(unit: Models.EngineeringUnits,
              index: number,
              list: Models.EngineeringUnits[])
    {
        return unit === this.editUnit && index === this.editIndex && list === this.editList;
    }

    edit(unit: Models.EngineeringUnits,
         index: number,
         list: Models.EngineeringUnits[],
         origin: CdkOverlayOrigin)
    {
        this.editUnit  = unit;
        this.editIndex = index;
        this.editList  = list;
        this.selector.toggleDropdown(origin);
    }

    addUnit()
    {
        // Detect changes now so we will correctly reset the components
        this.detectChanges();

        this.editList = this.editList || this.numerators;

        if (!isNaN(this.editIndex))
        {
            if (this.editUnit)
            {
                this.editList.splice(this.editIndex, 1, this.editUnit);
            }
            else
            {
                this.editList.splice(this.editIndex, 1);
            }
        }
        else if (this.editUnit)
        {
            this.editList.push(this.editUnit);
        }

        this.editUnit  = null;
        this.editList  = null;
        this.editIndex = undefined;

        this.emitChange();
        this.updateDisplay();
    }

    get numerators(): Models.EngineeringUnits[]
    {
        return this.m_value.numeratorUnits;
    }

    get denominators(): Models.EngineeringUnits[]
    {
        return this.m_value.denominatorUnits;
    }

    getUnitLabel(unit: Models.EngineeringUnits): string
    {
        let descriptor = this.unitsMap.get(unit);
        if (descriptor)
        {
            return descriptor.model.displayName;
        }

        return "";
    }

    async simplifyAfterSubmit()
    {
        if (this.m_value)
        {
            this.m_value = await this.app.domain.units.compact(this.m_value);
            this.emitChange();
            this.updateDisplay();
        }
    }

    private emitChange()
    {
        this.stateChanges.next();
        if (this.m_onChange)
        {
            this.m_onChange(this.m_value);
        }
    }

    private async updateDisplay()
    {
        this.unitDisplay = await this.app.domain.units.getDisplayHtmlString(this.m_value);
        this.detectChanges();
    }
}
