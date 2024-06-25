import {FocusMonitor} from "@angular/cdk/a11y";
import {CdkOverlayOrigin} from "@angular/cdk/overlay";
import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, HostBinding, Injector, Input, Optional, Output, Self, SimpleChanges, ViewChild} from "@angular/core";
import {ControlValueAccessor, NgControl} from "@angular/forms";
import {MatFormFieldControl} from "@angular/material/form-field";

import {UtilsService} from "framework/services/utils.service";
import * as SharedSvc from "framework/ui/components";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {FilterableTreeComponent, ITreeNode, ITreeNodeFilterSubmit} from "framework/ui/dropdowns/filterable-tree.component";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

import {Subject} from "rxjs";

@Component({
               selector       : "o3-string-set",
               templateUrl    : "./string-set.component.html",
               styleUrls      : ["./select.component.scss"],
               providers      : [
                   {
                       provide    : MatFormFieldControl,
                       useExisting: StringSetComponent
                   }
               ],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class StringSetComponent extends SharedSvc.BaseComponent implements ControlValueAccessor,
                                                                           MatFormFieldControl<string[]>
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

    @HostBinding() public readonly id = `o3-string-set-${StringSetComponent.nextId++}`;
    public readonly controlType       = "o3-string-set";
    public focused: boolean;

    @Input() placeholder: string;
    @Input() @CoerceBoolean() disabled: boolean;
    @Input() @CoerceBoolean() required: boolean;
    @Input() @CoerceBoolean() autoSizeDropdown: boolean = true;
    @Input() autoSizeThreshold: number                  = 2;
    @Input() @CoerceBoolean() readonly: boolean         = false;

    //--//

    @Input() filterHint: string;

    private m_values: string[] = [];

    @Input() set value(value: string[])
    {
        if (!value)
        {
            this.m_values = [];
        }
        else
        {
            this.m_values = value;
        }

        this.updateEditState();

        this.bindValueText();
    }

    get value(): string[]
    {
        return this.m_values;
    }

    @Output() valueChange: EventEmitter<string[]> = new EventEmitter<string[]>();

    //--//

    public readonly stateChanges = new Subject<void>();

    //--//

    @ViewChild("dropdown", {static: true}) dropdown: OverlayComponent;

    nodes: StringTreeNode[] = [];

    editValues: string[] = [];

    valueText: string = "";

    overlayConfig = OverlayConfig.dropdown({
                                               coverAnchorWhenDisplayed: false,
                                               overlayClass            : "overlay-input",
                                               minWidth                : 200
                                           });

    //--//

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

    //--//

    public registerOnChange(fn: any): void
    {
    }

    public registerOnTouched(fn: any): void
    {
    }

    public setDisabledState(isDisabled: boolean): void
    {
        this.disabled = isDisabled;
    }

    public writeValue(value: any): void
    {
    }

    //--//

    public get empty(): boolean
    {
        return !(this.m_values?.length > 0);
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

    readonly onNewTextFn: ITreeNodeFilterSubmit<string> = (value: string) =>
    {
        if (!value) return;

        if (this.editValues.indexOf(value) < 0)
        {
            this.editValues.push(value);
            this.editValues.sort((a,
                                  b) => UtilsService.compareStrings(a, b, true));

            this.bindValueText();
        }
    };

    removeTerm(node: StringTreeNode)
    {
        this.editValues = this.editValues.filter((v) => v != node.id);

        this.bindValueText();
    }

    public updateEditState()
    {
        this.editValues = this.m_values.filter((value) => !!value);
    }

    private bindValueText()
    {
        this.nodes = this.editValues.map((v) => new StringTreeNode(this, v, v));

        let numSelected = this.m_values.length;

        if (numSelected > 0 && numSelected < 15)
        {
            this.valueText = this.m_values.join(", ");
        }
        else
        {
            this.valueText = `${numSelected} ${UtilsService.pluralize("Item", numSelected)}`;
        }

        this.markForCheck();
    }

    toggleDropdown(overlayOrigin?: CdkOverlayOrigin)
    {
        if (!this.disabled && !this.readonly)
        {
            // If auto-sizing is enabled, calculate the size
            if (this.autoSizeDropdown)
            {
                // Setting explicit width disables the overlayOrigin width matching behavior of the overlay component
                let minWidth                = overlayOrigin ? overlayOrigin.elementRef.nativeElement.clientWidth : this.elementRef.nativeElement.parentElement?.clientWidth; // grab width from mat-form-field because select could be inline
                let smartWidth              = UtilsService.predictSmartTreeLength(this.nodes, FilterableTreeComponent.OPTION_FONT_SIZE,
                                                                                  FilterableTreeComponent.OFFSET_PER_DEPTH, null, this.autoSizeThreshold);
                this.overlayConfig.width    = Math.max(minWidth, smartWidth);
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
        this.bindValueText();
    }

    onDropdownClose()
    {
        // clear node bindings (to improve memory mgmt / page responsiveness)
        this.nodes = [];
    }

    submit(currentFilter: string)
    {
        this.onNewTextFn(currentFilter);

        this.m_values = this.editValues;

        this.bindValueText();

        this.valueChange.emit(this.m_values);

        this.dropdown.closeOverlay();
    }
}

class StringTreeNode implements ITreeNode<string>
{
    constructor(public readonly comp: StringSetComponent,
                public readonly id: string,
                public readonly label: string)
    {
    }


    public children: StringTreeNode[];

    public get hasChildren(): boolean
    {
        return false;
    }

    public get disableSelection(): boolean
    {
        return true;
    }
}
