import {FocusMonitor} from "@angular/cdk/a11y";
import {CdkOverlayOrigin} from "@angular/cdk/overlay";
import {ChangeDetectionStrategy, Component, ContentChild, ElementRef, EventEmitter, HostBinding, HostListener, Injector, Input, Optional, Output, Self, SimpleChanges, TemplateRef, ViewChild} from "@angular/core";
import {ControlValueAccessor, NgControl} from "@angular/forms";
import {MatFormFieldControl} from "@angular/material/form-field";

import {UtilsService} from "framework/services/utils.service";
import * as SharedSvc from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {FilterableTreeComponent, ILazyLoader, ITreeNode, ITreeNodeFilter, ITreeNodeFilterSubmit} from "framework/ui/dropdowns/filterable-tree.component";
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
    static ngAcceptInputType_selectChildren: boolean | "";
    static ngAcceptInputType_explicitValueDescription: boolean | "";
    static ngAcceptInputType_allowSingleOrMulti: boolean | "";

    private static nextId = 0;

    @HostBinding() public readonly id = `o3-select-${SelectComponent.nextId++}`;
    public readonly controlType       = "o3-select";
    public focused: boolean;

    @Input() placeholder: string;
    @Input() @CoerceBoolean() disabled: boolean;
    @Input() @CoerceBoolean() required: boolean;
    @Input() loadingValueDescription: string                    = "Loading...";
    @Input() defaultValueDescription: string                    = "Select Option";
    @Input() @CoerceBoolean() explicitValueDescription: boolean = false;
    @Input() @CoerceBoolean() allowSingleOrMulti: boolean       = false;
    @Input() @CoerceBoolean() preventDeselection: boolean       = false;
    @Input() @CoerceBoolean() enableNavigation: boolean;
    @Input() navigationFn: (value: T) => void;
    @Input() hierarchicalContext: boolean                       = false;
    @Input() @CoerceBoolean() singleClick: boolean;
    @Input() @CoerceBoolean() autoExpandAll: boolean;
    @Input() @CoerceBoolean() autoSizeDropdown: boolean         = true;
    @Input() minDropdownWidth: number;
    @Input() autoSizeThreshold: number                          = 2;
    @Input() showFilterThreshold: number                        = 5;
    @Input() @CoerceBoolean() readonly: boolean                 = false;
    @Input() @CoerceBoolean() searchMode: boolean               = false;
    @Input() @CoerceBoolean() selectChildren: boolean           = true;
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
        return this.allowSingleOrMulti ? this.selectionPluralityOn : this.m_multiSelect;
    }

    @ContentChild("buttonTemplate", {static: true}) buttonTemplate: TemplateRef<{ value: T | T[], text: string }>;
    @ContentChild("nodeTemplate", {static: true}) nodeTemplate: TemplateRef<any>;
    @ContentChild("nodePostTemplate", {static: true}) nodePostTemplate: TemplateRef<any>;

    //--//
    private m_options: ITreeNode<T>[]     = [];
    private m_optionsFlat: ITreeNode<T>[] = [];
    private m_numOptions: number          = 0;

    @Input() set options(options: ITreeNode<T>[])
    {
        this.m_options         = options;
        const flattenedOptions = UtilsService.flatten(options);
        this.m_numOptions      = flattenedOptions.length;
        this.m_optionsFlat     = flattenedOptions.filter((n) => !n.disableSelection);
        this.m_loaded          = true;
        this.computeOverlaySize();

        if (this.dropdownOpen)
        {
            this.nodes = this.m_options;
            this.dropdown.resize({
                                     width   : this.overlayConfig.width,
                                     maxWidth: this.overlayConfig.maxWidth
                                 });
        }

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
            if (this.allowSingleOrMulti)
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

    get showFilter(): boolean
    {
        if (this.m_lazyLoader) return true;
        return this.m_numOptions >= this.showFilterThreshold;
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
    @ViewChild("test_plurality", {read: ElementRef}) test_plurality: ElementRef;
    @ViewChild("test_filterableTree") test_filterableTree: FilterableTreeComponent<T>;
    @ViewChild("test_submit", {read: ElementRef}) test_submit: ElementRef;

    nodes: ITreeNode<T>[] = [];

    editValues: T[] = [];

    valueText: string = "";

    overlayConfig = OverlayConfig.dropdown({
                                               coverAnchorWhenDisplayed: false,
                                               overlayClass            : "overlay-input",
                                               minWidth                : 200
                                           });

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
                         this.focused = this.dropdown?.isOpen || (!this.readonly && !!origin);
                         this.stateChanges.next();
                     });
    }

    @HostListener("document:keydown", ["$event"])
    onKeyDown(event: KeyboardEvent)
    {
        if (!this.focused || this.multiSelectBehavior || !this.m_optionsFlat.length)
        {
            return;
        }
        const down = event.key === "ArrowDown";
        const up   = event.key === "ArrowUp";

        if (!down && !up)
        {
            return;
        }

        event.preventDefault();
        if (!this.m_value)
        {
            this.editValues = [this.m_optionsFlat[0].id];
            this.submit();
        }
        else
        {
            let index = this.m_optionsFlat.findIndex((opt) => opt.id === this.m_value);
            if (index >= 0)
            {
                index           = up ? Math.max(0, index - 1) : Math.min(this.m_optionsFlat.length - 1, index + 1);
                this.editValues = [this.m_optionsFlat[index].id];
                this.submit();
            }
        }
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
                if (this.multiSelectBehavior && !!this.m_values?.length || !this.multiSelectBehavior && this.m_value != null || this.readonly)
                {
                    let numSelected = this.multiSelectBehavior ? this.m_values.length : 1;
                    if (numSelected === 1)
                    {
                        let selected = this.multiSelectBehavior ? this.m_values[0] : this.m_value;
                        text         = this.m_lazyLoader ? await this.m_lazyLoader.getLabel(selected) : ControlOption.getLabel(selected, this.options, this.hierarchicalContext);
                        text         = text || this.defaultValueDescription;
                    }
                    else if (numSelected === 0)
                    {
                        text = this.readonly ? "Nothing Selected" : this.defaultValueDescription;
                    }
                    else
                    {
                        if (!this.explicitValueDescription && !this.readonly)
                        {
                            text = `${numSelected} ${UtilsService.pluralize("Item", numSelected)} Selected`;
                        }
                        else
                        {
                            let explicit = [];
                            for (let value of this.m_values)
                            {
                                explicit.push(this.m_lazyLoader ? await this.m_lazyLoader.getLabel(value) : ControlOption.getLabel(value, this.options, this.hierarchicalContext));
                            }

                            text = explicit.join(", ");
                        }
                    }
                }
                else
                {
                    text = this.defaultValueDescription;
                }
            }
        }

        if (this.valueText !== text)
        {
            this.valueText = text;
            this.markForCheck();
        }
    }

    tooltip(): string
    {
        return this.readonly ? this.valueText : null;
    }

    get dropdownOpen(): boolean
    {
        return this.dropdown.isOpening || this.dropdown.isOpen;
    }

    toggleDropdown(overlayOrigin?: CdkOverlayOrigin)
    {
        if (!this.disabled && !this.readonly)
        {
            if (overlayOrigin)
            {
                this.computeOverlaySize(overlayOrigin);
            }

            // Toggle the overlay open/closed
            this.dropdown.toggleOverlay(overlayOrigin);
        }
    }

    private computeOverlaySize(overlayOrigin?: CdkOverlayOrigin)
    {
        // If auto-sizing is enabled, calculate the size
        if (this.autoSizeDropdown)
        {
            const horizontalPadding = this.multiSelectBehavior ? 50 : 30; // provide extra padding if we need a checkbox
            const smartWidth        = UtilsService.predictSmartTreeLength(this.m_options, FilterableTreeComponent.OPTION_FONT_SIZE,
                                                                          FilterableTreeComponent.OFFSET_PER_DEPTH, horizontalPadding, this.autoSizeThreshold);
            const minWidth          = Math.max(this.minDropdownWidth || 0,
                                               overlayOrigin?.elementRef.nativeElement.clientWidth || this.elementRef.nativeElement.parentElement?.clientWidth || 0);

            // Setting explicit width disables the overlayOrigin width matching behavior of the overlay component
            this.overlayConfig.width    = Math.max(minWidth, smartWidth);
            this.overlayConfig.maxWidth = "50%";
        }
        else
        {
            // Clear width and max width to enable size matching
            delete this.overlayConfig.width;
            delete this.overlayConfig.maxWidth;
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
                this.value = this.allowSingleOrMulti ? [values[0]] : values[0];
            }
            else
            {
                this.value = null;
            }
        }

        if (this.allowSingleOrMulti)
        {
            this.wasMultiSelect = this.multiSelectBehavior;
            this.bindValueText();
        }

        let emitValue = this.allowSingleOrMulti && !this.multiSelectBehavior ? [<T>this.value] : this.value;

        //
        // First update the model, then fire the event.
        //
        if (this.m_onChange) this.m_onChange(emitValue);
        this.valueChange.emit(emitValue);

        this.dropdown.closeOverlay();
    }

    clearSelections()
    {
        if (this.multiSelectBehavior)
        {
            this.editValues = [];
        }
    }

    selectAll()
    {
        if (this.multiSelectBehavior)
        {
            this.editValues = this.m_optionsFlat.map((v) => v.id);
        }
    }

    reposition()
    {
        this.dropdown?.overlay?.updatePosition();
    }

    //--//

    test_isNodeExpanded(nodeId: T): boolean
    {
        return this.test_filterableTree.getNode(nodeId).isExpanded;
    }
}
