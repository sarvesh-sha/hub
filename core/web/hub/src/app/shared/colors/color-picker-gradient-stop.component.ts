import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, QueryList, SimpleChanges, ViewChild, ViewChildren} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {ColorPickerConfigurationComponent} from "app/shared/colors/color-picker-configuration.component";
import {ColorPickerFlatComponent} from "app/shared/colors/color-picker-flat.component";

import {UtilsService} from "framework/services/utils.service";
import {ColorSegmentBackground, ColorSegmentInterpolationMode, PaletteId} from "framework/ui/charting/core/colors";
import {ControlOption} from "framework/ui/control-option";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector       : "o3-color-picker-gradient-stop",
               templateUrl    : "./color-picker-gradient-stop.component.html",
               styleUrls      : ["./color-picker-gradient-stop.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ColorPickerGradientStopComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() palette: PaletteId;
    @Input() interpolation: ColorSegmentInterpolationMode = ColorSegmentInterpolationMode.LINEAR;

    private m_stops: Models.ColorSegment[] = ColorConfigurationExtended.newModel().segments;
    @Input() set stops(stops: Models.ColorSegment[])
    {
        if (!stops) return;
        this.m_stops = stops;

        for (let stop of this.m_stops)
        {
            if (stop.stopPoint !== Models.ColorStopPoint.CUSTOM) stop.stopPointValue = undefined;
        }

        if (this.interpolation === ColorSegmentInterpolationMode.STEP) this.m_stops[this.m_stops.length - 1].color = undefined;

        this.stopEditorOverlay.closeOverlay();
        this.regenerate();
    }

    get stops(): Models.ColorSegment[]
    {
        return this.m_stops;
    }

    @Input() maxStops: number;
    @Input() lockNumStops: boolean    = false;
    @Input() lockModes: boolean       = false;
    @Input() lockEndModes: boolean    = false;
    @Input() disableMidpoint: boolean = false;
    @Input() interactable: boolean    = true;

    @Output() paletteChange: EventEmitter<PaletteId>                           = new EventEmitter<PaletteId>();
    @Output() stopsChange: EventEmitter<Models.ColorSegment[]>                 = new EventEmitter<Models.ColorSegment[]>();
    @Output() interpolationChange: EventEmitter<ColorSegmentInterpolationMode> = new EventEmitter<ColorSegmentInterpolationMode>();

    @ViewChild(OverlayComponent, {static: true}) stopEditorOverlay: OverlayComponent;

    @ViewChildren("test_stopChip", {read: ElementRef}) test_stopChips: QueryList<ElementRef>;
    @ViewChildren("test_split", {read: ElementRef}) test_splits: QueryList<ElementRef>;
    @ViewChild("test_colorFlat") test_colorFlat: ColorPickerFlatComponent;
    @ViewChild("test_stopMode", {read: ElementRef}) test_stopMode: ElementRef;
    @ViewChild("test_stopValue", {read: ElementRef}) test_stopValue: ElementRef;

    public segments: ColorSegmentBackground[]                       = [];
    public editingStop: Models.ColorSegment                         = null;
    public editingStopIndex: number;
    public editingStopModes: ControlOption<Models.ColorStopPoint>[] = [];
    public config: OverlayConfig;

    constructor(inj: Injector)
    {
        super(inj);

        this.regenerate();

        this.config = ColorPickerConfigurationComponent.colorOverlayConfig(true);
    }

    public ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        this.regenerate();
    }

    public adjacentToMidpoint(index: number): boolean
    {
        if (this.notEdge(index))
        {
            return this.m_stops[index - 1]?.stopPoint === Models.ColorStopPoint.MIDPOINT ||
                   this.m_stops[index + 1]?.stopPoint === Models.ColorStopPoint.MIDPOINT;
        }

        return false;
    }

    public notEdge(index: number): boolean
    {
        return index !== 0 && index !== this.m_stops.length - 1;
    }

    public isStopColorEditable(index: number): boolean
    {
        return this.interpolation !== ColorSegmentInterpolationMode.STEP || index !== this.m_stops.length - 1;
    }

    public isStopModeEditable(index: number): boolean
    {
        if (this.lockModes) return false;
        return !this.lockEndModes || this.notEdge(index);
    }

    public isStopClickable(index: number): boolean
    {
        if (!this.interactable) return false;
        return this.isStopColorEditable(index) || this.isStopModeEditable(index);
    }

    public isCustomStop(model: Models.ColorSegment)
    {
        return model.stopPoint === Models.ColorStopPoint.CUSTOM;
    }

    public deleteStop(index: number)
    {
        if (!this.interactable) return;

        // Remove the given stop
        this.m_stops.splice(index, 1);

        // Collapse midpoint stops until midpoints are not adjacent
        if (this.m_stops[index - 1].stopPoint === Models.ColorStopPoint.MIDPOINT)
        {
            while (this.m_stops[index].stopPoint === Models.ColorStopPoint.MIDPOINT)
            {
                this.m_stops.splice(index, 1);
            }
        }

        // Trigger stop change event
        this.onStopChange();
    }

    public splitStop(index: number)
    {
        this.m_stops.splice(index + 1, 0, this.newStop(index));

        this.onStopChange();
    }

    public editStop(stop: Models.ColorSegment,
                    index: number)
    {
        if (!this.interactable) return;
        if (!this.isStopClickable(index)) return;
        if (this.editingStop) this.finishStopEditing();

        this.editingStop      = stop;
        this.editingStopIndex = index;

        this.editingStopModes = [];
        if (index === 0) this.editingStopModes.push(new ControlOption<Models.ColorStopPoint>(Models.ColorStopPoint.MIN, "Minimum"));
        if (!this.disableMidpoint && this.notEdge(index) && !this.adjacentToMidpoint(index))
        {
            this.editingStopModes.push(new ControlOption<Models.ColorStopPoint>(Models.ColorStopPoint.MIDPOINT,
                                                                                "Midpoint"));
        }
        if (index === this.m_stops.length - 1) this.editingStopModes.push(new ControlOption<Models.ColorStopPoint>(Models.ColorStopPoint.MAX, "Maximum"));
        this.editingStopModes.push(new ControlOption<Models.ColorStopPoint>(Models.ColorStopPoint.CUSTOM, "Custom"));

        if (!this.stopEditorOverlay.isOpen) this.stopEditorOverlay.toggleOverlay();
    }

    public finishStopEditing()
    {
        if (!this.editingStop) return;

        if (this.editingStop.stopPoint === Models.ColorStopPoint.CUSTOM)
        {
            let changed = false;
            if (this.editingStop.stopPointValue == null)
            {
                if (this.notEdge(this.editingStopIndex))
                {
                    if (!this.disableMidpoint &&
                        this.m_stops[this.editingStopIndex - 1].stopPoint !== Models.ColorStopPoint.MIDPOINT &&
                        this.m_stops[this.editingStopIndex + 1].stopPoint !== Models.ColorStopPoint.MIDPOINT)
                    {
                        this.editingStop.stopPoint = Models.ColorStopPoint.MIDPOINT;
                    }
                    else
                    {
                        this.editingStop.stopPointValue = 0;
                    }
                }
                else if (this.editingStopIndex === 0)
                {
                    this.editingStop.stopPoint = Models.ColorStopPoint.MIN;
                }
                else
                {
                    this.editingStop.stopPoint = Models.ColorStopPoint.MAX;
                }

                changed = true;
            }

            if (this.editingStop.stopPoint === Models.ColorStopPoint.CUSTOM)
            {
                let high = Number.MAX_VALUE;
                let low  = -high;

                let i = this.editingStopIndex - 1;
                while (i >= 0)
                {
                    let stop = this.m_stops[i--];
                    if (stop.stopPoint === Models.ColorStopPoint.CUSTOM)
                    {
                        low = stop.stopPointValue;
                        break;
                    }
                }

                i = this.editingStopIndex + 1;
                while (i < this.m_stops.length)
                {
                    let stop = this.m_stops[i++];
                    if (stop.stopPoint === Models.ColorStopPoint.CUSTOM)
                    {
                        high = stop.stopPointValue;
                        break;
                    }
                }

                this.editingStop.stopPointValue = UtilsService.clamp(low, high, this.editingStop.stopPointValue);

                changed = true;
            }

            if (changed) this.onStopChange();
        }

        this.editingStop      = null;
        this.editingStopIndex = null;
    }

    public stopLabel(stop: Models.ColorSegment): string
    {
        switch (stop.stopPoint)
        {
            case Models.ColorStopPoint.MAX:
                return "Max";
            case Models.ColorStopPoint.MIN:
                return "Min";
            case Models.ColorStopPoint.MIDPOINT:
                return "Midpoint";
            case Models.ColorStopPoint.CUSTOM:
                return `>= ${stop.stopPointValue}`;
        }

        return "Invalid";
    }

    public onStopChange()
    {
        this.regenerate();
        this.stopsChange.emit(this.m_stops);
    }

    public onPaletteChange()
    {
        this.paletteChange.emit(this.palette);
    }

    private newStop(leftIdx: number): Models.ColorSegment
    {
        let start = this.m_stops[leftIdx];
        let end   = this.m_stops[leftIdx + 1];

        if (!this.disableMidpoint && start.stopPoint !== Models.ColorStopPoint.MIDPOINT && end.stopPoint !== Models.ColorStopPoint.MIDPOINT)
        {
            return Models.ColorSegment.newInstance({
                                                       color    : start.color,
                                                       stopPoint: Models.ColorStopPoint.MIDPOINT
                                                   });
        }

        let newIdx = leftIdx + 1;
        let prevCustomValue: number;
        let nextCustomValue: number;
        for (let i = 0; i < this.m_stops.length; i++)
        {
            let stop = this.m_stops[i];
            if (stop.stopPoint === Models.ColorStopPoint.CUSTOM)
            {
                if (i < newIdx)
                {
                    prevCustomValue = stop.stopPointValue;
                }
                else
                {
                    nextCustomValue = stop.stopPointValue;
                    break;
                }
            }
        }

        let stopValue;
        if (!isNaN(prevCustomValue))
        {
            stopValue = !isNaN(nextCustomValue) ? (prevCustomValue + nextCustomValue) / 2 : prevCustomValue;
        }
        else if (!isNaN(nextCustomValue))
        {
            stopValue = nextCustomValue;
        }
        else
        {
            stopValue = 0;
        }

        return Models.ColorSegment.newInstance({
                                                   color         : start.color,
                                                   stopPoint     : Models.ColorStopPoint.CUSTOM,
                                                   stopPointValue: stopValue
                                               });
    }

    private regenerate()
    {
        this.segments = [];

        for (let i = 1; i < this.m_stops.length; i++)
        {
            this.segments.push(new ColorSegmentBackground(this.m_stops[i - 1].color, this.m_stops[i].color, this.interpolation));
        }
    }
}
