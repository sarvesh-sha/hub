import {FocusMonitor} from "@angular/cdk/a11y";
import {CdkOverlayOrigin} from "@angular/cdk/overlay";
import {ChangeDetectionStrategy, Component, ContentChild, ElementRef, EventEmitter, HostBinding, Injector, Input, Optional, Output, Self, SimpleChanges, TemplateRef, ViewChild} from "@angular/core";
import {ControlValueAccessor, NgControl} from "@angular/forms";
import {MatFormFieldControl} from "@angular/material/form-field";

import {UtilsService} from "framework/services/utils.service";
import * as SharedSvc from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {ILazyLoader, ITreeNode, ITreeNodeFilter, ITreeNodeFilterSubmit} from "framework/ui/dropdowns/filterable-tree.component";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

import {Subject} from "rxjs";

@Component({
               selector       : "o3-select",
               templateUrl    : "./select.component.html",
               styleUrls      : ["./select.component.scss"],
               providers      : [
                   {
                       provide    : MatFormFieldControl,
                       useExisting: SelectComponent
                   }
               ],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class SelectComponent<T> extends SharedSvc.BaseComponent implements ControlValueAccessor,
                                                                           MatFormFieldControl<T | T[]>
{
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_disabled: boolean | "";
    static ngAcceptInputType_required: boolean | "";
    static ngAcceptInputType_preventDeselection: boolean | "";
    static ngAcceptInputType_enableNavigation: boolean | "";
    static ngAcceptInputType_singleClick: boolean | "";
    static ngAcceptInputType_autoExpandAll: boolean | "";
    static ngAcceptInputType_readonly: boolean | "";
    static ngAcceptInputType_searchMode: boolean | "";

    private static nextId = 0;

    @HostBinding() public readonly id = `o3-select-${SelectComponent.nextId++}`;
    public readonly controlType       = "o3-select";
    public focused: boolean;

    @Input() placeholder: string;
    @Input() @CoerceBoolean() disabled: boolean;
    @Input() @CoerceBoolean() required: boolean;
    @Input() loadingValueDescription: string              = "Loading...";
    @Input() defaultValueDescription: string              = "Select Option";
    @Input() toggleableSelectPlurality: boolean           = false;
    @Input() @CoerceBoolean() preventDeselection: boolean = false;
    @Input() @CoerceBoolean() enableNavigation: boolean;
    @Input() navigationFn: (value: T) => void;
    @Input() hierarchicalContext: boolean                 = false;
    @Input() @CoerceBoolean() singleClick: boolean;
    @Input() @CoerceBoolean() autoExpandAll: boolean;
    @Input() @CoerceBoolean() autoSizeDropdown: boolean   = true;
    @Input() autoSizeThreshold: number                    = 2;
    @Input() @CoerceBoolean() readonly: boolean           = false;
    @Input() @CoerceBoolean() searchMode: boolean         = false;
    @Input() filterFn: ITreeNodeFilter<T>;
    @Input() filterSubmitFn: ITreeNodeFilterSubmit<T>;

    private wasMultiSelect: boolean = true;

    selectionPluralityOn: boolean  = true;
    private m_multiSelect: boolean = true;
    @Input() set multiSelect(multiSelect: boolean)
    {
        multiSelect = !!multiSelect;
        if (multiSelect !== this.m_multiSelect)
        {
            this.wasMultiSelect       = multiSelect;
            this.selectionPluralityOn = multiSelect;
            this.m_multiSelect        = multiSelect;
        }
    }

    get multiSelectBehavior(): boolean
    {
        return this.toggleableSelectPlurality ? this.selectionPluralityOn : this.m_multiSelect;
    }

    @ContentChild("buttonTemplate", {static: true}) buttonTemplate: TemplateRef<{ value: T | T[], text: string }>;
    @ContentChild("nodeTemplate", {static: true}) nodeTemplate: TemplateRef<any>;
    @ContentChild("nodePostTemplate", {static: true}) nodePostTemplate: TemplateRef<any>;

    //--//
    private m_options: ITreeNode<T>[] = [];
    @Input() set options(options: ITreeNode<T>[])
    {
        this.m_options = options;
        this.m_loaded  = true;
        setTimeout(() => this.bindValueText(), 50);
    }

    get options(): ITreeNode<T>[]
    {
        return this.m_options;
    }

    @Input() set lazyLoader(loader: ILazyLoader<T>)
    {
        this.m_lazyLoader = loader;
        setTimeout(() => this.bindValueText(), 50);
    }

    get lazyLoader(): ILazyLoader<T>
    {
        return this.m_lazyLoader;
    }

    private m_lazyLoader: ILazyLoader<T>;

    //--//

    private m_value: T;
    private m_values: T[] = [];

    @Input() set value(value: T | T[])
    {
        if (value == null)
        {
            if (this.multiSelectBehavior)
            {
                this.m_values   = [];
                this.editValues = [];
            }
            else
            {
                this.m_value = undefined;
            }
        }
        else if (this.multiSelectBehavior)
        {
            if (!Array.isArray(value)) return;

            this.m_values   = value;
            this.editValues = UtilsService.arrayCopy(value);
        }
        else
        {
            let singularValue: T;
            if (this.toggleableSelectPlurality)
            {
                if (!Array.isArray(value)) return;

                singularValue = value[0];
            }
            else
            {
                singularValue = <T>value;
            }

            if (this.m_value != singularValue)
            {
                this.m_value    = singularValue;
                this.editValues = this.m_value ? [this.m_value] : [];
            }
        }

        this.bindValueText();
    }

    get value(): T | T[]
    {
        return this.multiSelectBehavior ? this.m_values : this.m_value;
    }

    get hasValue(): boolean
    {
        return this.multiSelectBehavior ? !!this.m_values?.length : !!this.m_value;
    }

    get showOkButton(): boolean
    {
        if (!this.singleClick || this.multiSelectBehavior) return true;

        return this.wasMultiSelect !== this.multiSelectBehavior;
    }

    get areAcceptableChanges(): boolean
    {
        return !this.preventDeselection || !!this.editValues.length;
    }

    @Output() valueChange: EventEmitter<T | T[]> = new EventEmitter<T | T[]>();

    //--//

    public readonly stateChanges = new Subject<void>();

    //--//

    @ViewChild("dropdown", {static: true}) dropdown: OverlayComponent;

    nodes: ITreeNode<T>[] = [];

    editValues: T[] = [];

    valueText: string = "";

    overlayConfig: OverlayConfig;

    //--//

    private m_onTouched: () => void;
    private m_onChange: (value: T | T[]) => void;
    private m_loaded: boolean = false;

    //--//

    private m_ready: boolean = true;

    @Input()
    public set ready(value: boolean)
    {
        this.m_ready = value;

        this.bindValueText();
    }

    public get ready(): boolean
    {
        return this.m_ready;
    }

    constructor(inj: Injector,
                @Optional() @Self() public ngControl: NgControl,
                private fm: FocusMonitor,
                public elementRef: ElementRef)
    {
        super(inj);

        if (this.ngControl)
        {
            this.ngControl.valueAccessor = this;
        }

        fm.monitor(elementRef.nativeElement, true)
          .subscribe((origin) =>
                     {
                         this.focused = !this.readonly && !!origin;
                         this.stateChanges.next();
                     });

        this.overlayConfig = new OverlayConfig();
        this.overlayConfig.setDropdownDefaults();
        this.overlayConfig.coverAnchorWhenDisplayed = false;
        this.overlayConfig.overlayClass             = "overlay-input";
        this.overlayConfig.minWidth                 = 200;
    }

    //--//

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

    public writeValue(value: any): void
    {
        this.value = value;
    }

    //--//

    public get empty(): boolean
    {
        return !this.m_value && (!this.m_values || !this.m_values.length);
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
        // If they click the container and don't hit the button, open the dropdown
        if ((event.target as Element).tagName.toLowerCase() != "button")
        {
            if (!this.dropdown.isOpen)
            {
                this.toggleDropdown();
            }
        }
    }

    @HostBinding("attr.aria-describedby") describedBy = "";

    public setDescribedByIds(ids: string[]): void
    {
        this.describedBy = ids.join(" ");
    }

    //--//

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

    //--//

    public updateEditState()
    {
        let editValues: T[];
        if (this.multiSelectBehavior)
        {
            editValues = !!this.m_values?.length ? this.m_values : [this.m_value];
        }
        else
        {
            editValues = [this.m_value || this.m_values[0]];
        }

        this.editValues = editValues.filter((value) => !!value);
    }

    private async bindValueText()
    {
        let text = this.loadingValueDescription;

        if (this.m_ready)
        {
            if (this.m_loaded || this.m_lazyLoader)
            {
                if (this.multiSelectBehavior && !!this.m_values?.length || !this.multiSelectBehavior && this.m_value)
                {
                    let numSelected = this.multiSelectBehavior ? this.m_values.length : 1;
                    if (numSelected === 1)
                    {
                        let selected = this.multiSelectBehavior ? this.m_values[0] : this.m_value;
                        text         = this.m_lazyLoader ? await this.m_lazyLoader.getLabel(selected) : ControlOption.getLabel(selected, this.options, this.hierarchicalContext);
                        text         = text || this.defaultValueDescription;
                    }
                    else
                    {
                        text = `${numSelected} ${UtilsService.pluralize("Item", numSelected)} Selected`;
                    }
                }
                else
                {
                    text = this.defaultValueDescription;
                }
            }
        }

        this.valueText = text;
        this.markForCheck();
    }

    toggleDropdown(overlayOrigin?: CdkOverlayOrigin)
    {
        if (!this.disabled && !this.readonly)
        {
            // If auto-sizing is enabled, calculate the size
            if (this.autoSizeDropdown)
            {
                let minWidth = overlayOrigin ?
                    overlayOrigin.elementRef.nativeElement.clientWidth :
                    this.elementRef.nativeElement.parentElement?.clientWidth; // grab width from mat-form-field because select could be inline

                // Setting explicit width disables the overlayOrigin width matching behavior of the overlay component
                this.overlayConfig.width    = Math.max(minWidth, smartTreeWidth(this.m_options, this.autoSizeThreshold));
                this.overlayConfig.maxWidth = "50%";
            }
            else
            {
                // Clear width and max width to enable size matching
                delete this.overlayConfig.width;
                delete this.overlayConfig.maxWidth;
            }

            // Toggle the overlay open/closed
            this.dropdown.toggleOverlay(overlayOrigin);
        }
    }

    async onDropdownOpen()
    {
        // bind on dropdown (to improve memory mgmt / page responsiveness)
        this.updateEditState();
        this.nodes = this.options;
    }

    onDropdownClose()
    {
        // clear node bindings (to improve memory mgmt / page responsiveness)
        this.nodes = [];
    }

    navigate()
    {
        if (this.navigationFn) this.navigationFn(this.m_value);
    }

    selectedNodesChanged()
    {
        if (!this.multiSelectBehavior && this.singleClick && !this.wasMultiSelect)
        {
            this.submit();
        }
    }

    submit()
    {
        let values = this.editValues;

        if (this.multiSelectBehavior)
        {
            this.value = values;
        }
        else
        {
            if (values.length > 0)
            {
                this.value = this.toggleableSelectPlurality ? [values[0]] : values[0];
            }
            else
            {
                this.value = null;
            }
        }

        if (this.toggleableSelectPlurality)
        {
            this.wasMultiSelect = this.multiSelectBehavior;
            this.bindValueText();
        }

        let emitValue = this.toggleableSelectPlurality && !this.multiSelectBehavior ? [<T>this.value] : this.value;

        //
        // First update the model, then fire the event.
        //
        if (this.m_onChange) this.m_onChange(emitValue);
        this.valueChange.emit(emitValue);

        this.dropdown.closeOverlay();
    }
}

//--//

export function smartTreeWidth<T>(nodes: ITreeNode<T>[],
                                  autoSizeThreshold: number): number
{
    // Convert to flattened measurable nodes
    let flat = flatten(nodes);

    // Find mean length
    let mean = 0;
    for (let node of flat) mean += node.size;
    mean = mean / flat.length;

    // Find mean of squares of differences
    let squaredMean = 0;
    for (let node of flat) squaredMean += (node.size - mean) * (node.size - mean);
    squaredMean = squaredMean / flat.length;

    // Find standard deviation
    let stdDev = Math.sqrt(squaredMean);

    // Find an array of z scores
    for (let node of flat) node.zScore = (node.size - mean) / stdDev;

    // Filter nodes with z score outside of threshold
    let filtered = flat.filter((node: MeasurableNode<T>) => { return Math.abs(node.zScore) <= autoSizeThreshold; });

    // Find the largest size
    let largest = 0;
    for (let node of filtered)
    {
        if (node.size > largest) largest = node.size;
    }

    // Return the largest size
    return largest;
}

function flatten<T>(nodes: ITreeNode<T>[],
                    depth: number = 0): MeasurableNode<T>[]
{
    let measurable: MeasurableNode<T>[] = [];
    for (let i = 0; i < nodes?.length; i++)
    {
        let processed = new MeasurableNode<T>(nodes[i], depth);
        measurable.push(processed);

        if (nodes[i].children?.length > 0)
        {
            measurable = measurable.concat(flatten(nodes[i].children, depth + 1));
        }
    }

    return measurable;
}

class MeasurableNode<T>
{
    public size: number = 0;
    public zScore: number;

    constructor(public node: ITreeNode<T>,
                public depth: number)
    {
        // Perhaps in the future we can be a little smarter about sizing
        // One idea would be to measure common character sizes using SVG and
        // cache in a lookup table indexed by ascii code and calculate size
        // based on the actual characters in the label

        // Current formula is:
        // (label length * 7.5px) + (nesting depth * nesting indent of 44px) + (50px of padding and checkbox space)
        this.size = ((node.label || "").length * 7.5) + (depth * 44) + 50;
    }
}

