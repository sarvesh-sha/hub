import {BreakpointObserver} from "@angular/cdk/layout";
import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {CanvasZoneSelection, CanvasZoneSelectionType} from "framework/ui/charting/app-charting-utilities";
import {BaseComponent} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";
import {LAYOUT_WIDTH_SM} from "framework/ui/layout";
import {OverlayController} from "framework/ui/overlays/overlay-base";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {ModifiableTableComponent, ModifiableTableRowRemove} from "framework/ui/shared/modifiable-table.component";

@Component({
               selector       : "o3-annotation-overlay[annotations]",
               templateUrl    : "./annotation-overlay.component.html",
               styleUrls      : ["./annotation-overlay.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AnnotationOverlayComponent extends BaseComponent implements OverlayController
{
    @Input() annotations: CanvasZoneSelection[];
    @Input() range: Models.RangeSelection;

    @Input() set addAnnotationSelection(selection: CanvasZoneSelection)
    {
        if (selection && this.m_editAnnotation !== selection) this.modifyAnnotation(selection, true);
    }

    @Input() onlyPointAnnotations = false;
    @Input() readonly             = false;

    get editAnnotation(): CanvasZoneSelection
    {
        return this.m_editAnnotation;
    }

    get editOverlayLabel(): string
    {
        let editAnnotation = this.editAnnotation;

        let label = this.isNew ? "New: " : "Edit: ";
        label += editAnnotation.title || "";
        if (label === "New: ")
        {
            switch (editAnnotation.type)
            {
                case CanvasZoneSelectionType.Area:
                    label += "Area Annotation";
                    break;

                case CanvasZoneSelectionType.AreaInverted:
                    label += "Area Annotation (Inverted)";
                    break;

                case CanvasZoneSelectionType.Point:
                    label += "Point Annotation";
                    break;

                case CanvasZoneSelectionType.XRange:
                    label += "Time Range Annotation";
                    break;

                case CanvasZoneSelectionType.XRangeInverted:
                    label += "Time Range Annotation (Inverted)";
                    break;

                case CanvasZoneSelectionType.YRange:
                    label += "Value Range Annotation";
                    break;

                case CanvasZoneSelectionType.YRangeInverted:
                    label += "Value Range Annotation (Inverted)";
                    break;
            }
        }

        return label;
    }

    get errorMessage(): string
    {
        if (!this.editTitle) return "Enter title";

        let id = this.editTitle.trim();
        if (this.editDescription) id += this.editDescription.trim();

        let edit = this.editAnnotation;
        for (let annotation of this.annotations)
        {
            if (annotation !== edit && id == annotation.id) return "Identical annotation already exists";
        }

        return "";
    }

    get isPristine(): boolean
    {
        let edit = this.editAnnotation;

        if (this.isNew) return false;
        if (!UtilsService.equivalentStrings(this.editTitle, edit.title)) return false;
        if (!UtilsService.equivalentStrings(this.editDescription, edit.description)) return false;
        return !this.editShowTooltip === edit.hideTooltip;
    }

    deletableFn = () => true;

    allowAdding: boolean = true;
    annotationTypeOptions: ControlOption<CanvasZoneSelectionType>[];

    private isNew: boolean;
    private m_editAnnotation: CanvasZoneSelection;

    editShowTooltip: boolean;
    editTitle: string;
    editDescription: string;

    @Output() newAnnotationRequest = new EventEmitter<CanvasZoneSelectionType>();
    @Output() annotationUpdated    = new EventEmitter<CanvasZoneSelection>();
    @Output() annotationToggled    = new EventEmitter<CanvasZoneSelection>();
    @Output() annotationDeleted    = new EventEmitter<ModifiableTableRowRemove<CanvasZoneSelection>>();

    @ViewChild(OverlayComponent, {static: true}) overlay: OverlayComponent;
    @ViewChild(StandardFormOverlayComponent, {static: false}) editOverlay: StandardFormOverlayComponent;
    @ViewChild("test_table") test_table: ModifiableTableComponent<CanvasZoneSelection>;
    @ViewChild("test_name", {read: ElementRef}) test_name: ElementRef;
    @ViewChild("test_description", {read: ElementRef}) test_description: ElementRef;

    overlayConfig     = OverlayConfig.onTopDraggable({width: 425});
    editOverlayConfig = OverlayConfig.onTopDraggable({width: 425});

    constructor(inj: Injector,
                breakpointObserver: BreakpointObserver)
    {
        super(inj);

        this.annotationTypeOptions = [
            new ControlOption(CanvasZoneSelectionType.Point, "Point"),
            new ControlOption(CanvasZoneSelectionType.XRange, "Time Range"),
            new ControlOption(CanvasZoneSelectionType.XRangeInverted, "Time Range (Inverted)"),
            new ControlOption(CanvasZoneSelectionType.YRange, "Value Range"),
            new ControlOption(CanvasZoneSelectionType.YRangeInverted, "Value Range (Inverted)"),
            new ControlOption(CanvasZoneSelectionType.Area, "Area"),
            new ControlOption(CanvasZoneSelectionType.AreaInverted, "Area (Inverted)")
        ];

        this.subscribeToObservable(breakpointObserver.observe(`(min-width: ${LAYOUT_WIDTH_SM}px)`),
                                   (handsetBreakpoint) => this.allowAdding = handsetBreakpoint.matches);
    }

    public toggleAnnotation(annotation: CanvasZoneSelection)
    {
        annotation.enabled = !annotation.enabled;
        this.annotationToggled.emit(annotation);
    }

    public modifyAnnotation(selection: CanvasZoneSelection,
                            isNew: boolean = false)
    {
        this.isNew            = isNew;
        this.m_editAnnotation = selection;
        this.editTitle        = selection.title || "";
        this.editDescription  = selection.description || "";
        this.editShowTooltip  = !selection.hideTooltip;
        this.detectChanges();

        setTimeout(() => this.editOverlay?.openOverlay());
    }

    public saveAnnotation()
    {
        let editAnnotation         = this.editAnnotation;
        editAnnotation.title       = this.editTitle.trim();
        editAnnotation.description = this.editDescription.trim();
        editAnnotation.hideTooltip = !this.editShowTooltip;

        this.annotationUpdated.emit(editAnnotation);
        this.clearEdits();

        if (this.isNew) this.overlay.resetPosition();
    }

    public clearEdits()
    {
        this.editTitle = this.editDescription = this.m_editAnnotation = undefined;

        // notify that annotations should no longer be creatable
        if (this.isNew) this.newAnnotationRequest.emit(undefined);
    }

    public requestNewAnnotation(type: CanvasZoneSelectionType)
    {
        this.newAnnotationRequest.emit(type);
        this.toggleOverlay();
    }

    public closeOverlay(): void
    {
        this.overlay.closeOverlay();
    }

    public isOpen(): boolean
    {
        return !!this.overlay.isOpen;
    }

    public openOverlay(): void
    {
        if (!this.overlay.isOpen) this.overlay.toggleOverlay();
    }

    public toggleOverlay(open?: boolean): void
    {
        if (open === undefined || open !== this.overlay.isOpen) this.overlay.toggleOverlay();
    }
}
