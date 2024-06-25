import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from "@angular/core";
import * as Models from "app/services/proxy/model/models";
import {DaysOfWeek} from "app/shared/forms/time-range/range-selection-extended";

@Component({
               selector       : "o3-day-picker",
               templateUrl    : "./day-picker.component.html",
               styleUrls      : ["./day-picker.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class DayPickerComponent
{
    daysOfWeek = DaysOfWeek;

    private readonly m_selection = new Set<Models.DayOfWeek>();

    @Input() set selection(selection: Models.DayOfWeek[])
    {
        this.m_selection.clear();
        for (let day of (selection || []))
        {
            this.m_selection.add(day);
        }
    }

    @Output() selectionChange = new EventEmitter<Models.DayOfWeek[]>();

    @Input() singleSelect: boolean;

    isSelected(day: Models.DayOfWeek): boolean
    {
        return this.m_selection.has(day);
    }

    toggle(day: Models.DayOfWeek)
    {
        if (this.singleSelect)
        {
            this.m_selection.clear();
        }

        if (this.isSelected(day))
        {
            this.m_selection.delete(day);
        }
        else
        {
            this.m_selection.add(day);
        }

        this.selectionChange.emit([...this.m_selection]);
    }

    getShortName(day: Models.DayOfWeek): string
    {
        return day.charAt(0);
    }
}
