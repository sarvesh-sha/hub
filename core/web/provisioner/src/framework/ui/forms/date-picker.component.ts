import {BreakpointObserver, Breakpoints} from "@angular/cdk/layout";
import {Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";
import {MatDatepicker} from "@angular/material/datepicker";
import {MatSelect} from "@angular/material/select";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-date-picker",
               styleUrls  : ["./date-picker.component.scss"],
               templateUrl: "./date-picker.component.html"
           })
export class DatePickerComponent
{
    /**
     * The label to display with the picker.
     */
    @Input() label: string;

    /**
     * The label to display underneath the picker input.
     */
    @Input() hintLabel: string;

    /**
     * The type of display to show.
     */
    @Input() display: DatePickerDisplayType = DatePickerDisplayType.DateAndTime;

    /**
     * Whether the input is required.
     */
    @Input() required: boolean = false;

    /**
     * Whether the input is readonly
     */
    @Input() readonly: boolean = false;

    /**
     * Whether to increment mintues by 5 rather than 1.
     */
    @Input() fiveMinuteIncrements: boolean = false;

    private m_model: Date;

    /**
     * The model value to bind to (optional).
     */
    @Input() set model(model: Date)
    {
        if (model) model.setSeconds(0, 0);
        this.m_model = model;
    }

    get model(): Date
    {
        return this.m_model;
    }

    /**
     * An event raised when the model value changes.
     */
    @Output() modelChange = new EventEmitter<Date>();

    overlayConfig: OverlayConfig;

    get hour(): number
    {
        if (this.m_model)
        {
            var hour = this.m_model.getHours();
            if (hour == 0)
            {
                return 12;
            }
            else if (hour > 12)
            {
                return hour - 12;
            }
            else
            {
                return hour;
            }
        }
        return null;
    }

    set hour(value: number)
    {
        if (!this.m_model) this.model = new Date();

        var hour = value;
        if (this.isPM && hour < 12)
        {
            hour += 12;
        }
        else if (this.isAM && hour == 12) hour = 0;

        this.m_model.setHours(hour);
        this.m_model.setSeconds(0, 0);
        this.onModelChange(this.m_model);
    }

    get minutes(): number
    {
        if (this.m_model) return this.m_model.getMinutes();
        return null;
    }

    set minutes(value: number)
    {
        if (!this.m_model) this.model = new Date();

        this.m_model.setMinutes(value, 0, 0);
        this.onModelChange(this.m_model);
    }

    get amPM(): string
    {
        if (this.m_model)
        {
            var hour = this.m_model.getHours();
            if (hour < 12)
            {
                return "AM";
            }
            else
            {
                return "PM";
            }
        }
        return null;
    }

    set amPM(value: string)
    {
        if (!this.m_model) this.model = new Date();

        if ((value == "AM" || value == "PM"))
        {
            let hours = this.m_model.getHours();
            if (value == "AM" && hours > 11) hours -= 12;
            if (value == "PM" && hours < 12) hours += 12;

            this.m_model.setHours(hours);
            this.m_model.setSeconds(0, 0);
            this.onModelChange(this.m_model);
        }
    }

    get isAM(): boolean
    {
        if (this.amPM == "AM") return true;
        return false;
    }

    get isPM(): boolean
    {
        if (this.amPM == "PM") return true;
        return false;
    }

    useTouchMode: boolean = false;

    get time(): string
    {
        if (this.m_model)
        {
            let minuteText = "" + this.minutes;
            if (this.minutes < 10) minuteText = "0" + minuteText;
            return `${this.hour}:${minuteText} ${this.amPM}`;
        }

        return null;
    }

    get minuteOptions(): number[]
    {
        let options = [];

        for (let i = 0; i < 60; i++)
        {
            if (this.fiveMinuteIncrements)
            {
                if (i % 5 == 0)
                {
                    options.push(i);
                }
            }
            else
            {
                options.push(i);
            }
        }

        return options;
    }

    get showDateInput(): boolean
    {
        if (this.display == DatePickerDisplayType.Date || this.display == DatePickerDisplayType.DateAndTime) return true;
        return false;
    }

    get showTimeInput(): boolean
    {
        if (this.display == DatePickerDisplayType.Time || this.display == DatePickerDisplayType.DateAndTime) return true;
        return false;
    }

    get clickableClass(): any
    {
        return {
            "clickable"    : !this.readonly,
            "not-clickable": this.readonly
        };
    }

    /**
     * The maximum valid date (optional).
     */
    @Input() maximumDate: Date;

    /**
     * The minimum valid date (optional).
     */
    @Input() minimumDate: Date;

    /**
     * The datepicker.
     */
    @ViewChild("datepicker", {static: true}) datepicker: MatDatepicker<Date>;

    /**
     * The hour selector.
     */
    @ViewChild("hourSelect", {static: true}) hourSelect: MatSelect;

    /**
     * The minute selector.
     */
    @ViewChild("minuteSelect", {static: true}) minuteSelect: MatSelect;

    /**
     * The AM PM selector.
     */
    @ViewChild("amPmSelect", {static: true}) amPmSelect: MatSelect;

    /**
     * The dropdown.
     */
    @ViewChild("dropdown", {static: true}) dropdown: OverlayComponent;

    constructor(breakpointObserver: BreakpointObserver)
    {
        this.overlayConfig = new OverlayConfig();
        this.overlayConfig.setDropdownDefaults();
        this.overlayConfig.coverAnchorWhenDisplayed = false;
        this.overlayConfig.minWidth                 = 220;
        this.overlayConfig.width                    = 220;
        this.overlayConfig.overlayClass             = "overlay-input";
        this.overlayConfig.showCloseButton          = true;

        breakpointObserver.observe([
                                       Breakpoints.XSmall
                                   ])
                          .subscribe((handsetBreakpoint) =>
                                     {
                                         this.useTouchMode = handsetBreakpoint.matches;
                                     });
    }

    /**
     * Raises the model change event.
     */
    onModelChange(model: Date)
    {
        this.modelChange.emit(model);
    }

    placeholderClicked(event: Event)
    {
        if (this.showTimeInput)
        {
            this.toggleTimeDropdown();
        }
        else
        {
            this.activateDatepicker(event);
        }
    }

    activateDatepicker(event: Event)
    {
        if (this.useTouchMode) event.stopPropagation();

        if (this.readonly)
        {
            return;
        }

        this.datepicker.open();
    }

    formatMinute(minute: number): string
    {
        if (minute < 10)
        {
            return "0" + minute;
        }
        else
        {
            return minute.toString();
        }
    }

    toggleTimeDropdown()
    {
        if (this.readonly)
        {
            return;
        }

        this.dropdown.toggleOverlay();
    }
}

const DatePickerDisplayTypePrivate = {
    DateAndTime: "DateAndTime",
    Date       : "Date",
    Time       : "Time"
};

export type DatePickerDisplayType = keyof typeof DatePickerDisplayTypePrivate;

export const DatePickerDisplayType: { [P in DatePickerDisplayType]: P } = <any>DatePickerDisplayTypePrivate;
