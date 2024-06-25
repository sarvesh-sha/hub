import {Component, EventEmitter, Input, Output} from "@angular/core";

import * as Models from "app/services/proxy/model/models";
import {DaysOfWeek, RecurringWeeklyScheduleExtended} from "app/shared/forms/time-range/range-selection-extended";

@Component({
               selector   : "o3-schedule-selector",
               templateUrl: "./schedule-selector.component.html",
               styleUrls  : ["./schedule-selector.component.scss"]
           })
export class ScheduleSelectorComponent
{
    private static hoursOfDay: number[] = [...new Array(24).keys()];

    @Input() showLegend: boolean = true;

    @Input() onWord: string  = "Enabled";
    @Input() offWord: string = "Disabled";

    zoomInTarget: number        = null;
    private targetShade: number = null;

    private m_state: DaySchedule[] = [];
    private m_model: Models.RecurringWeeklySchedule;

    get state(): DaySchedule[]
    {
        if (!this.m_state.length)
        {
            this.m_model = RecurringWeeklyScheduleExtended.generateWeeklySchedule([]);
            this.m_state = this.generateSchedule();
        }
        return this.m_state;
    }

    @Input() set model(model: Models.RecurringWeeklySchedule)
    {
        if (RecurringWeeklyScheduleExtended.scheduleIsValid(model))
        {
            let prevModel = this.m_model;
            this.m_model  = model;
            if (!prevModel || !new RecurringWeeklyScheduleExtended(prevModel).equals(this.m_model)) this.m_state = this.generateSchedule();
        }
    }

    get model(): Models.RecurringWeeklySchedule
    {
        return this.m_model;
    }

    @Output() selectionChange: EventEmitter<Models.RecurringWeeklySchedule> = new EventEmitter();

    /**
     *
     * @param schedule - if not passed in, then returns updated this.m_model; otherwise generates new model based on schedule
     */
    private generateModel(schedule: DaySchedule[] = this.m_state): Models.RecurringWeeklySchedule
    {
        let model = schedule === this.m_state ? this.m_model : RecurringWeeklyScheduleExtended.generateWeeklySchedule([]);

        for (let dayIdx = 0; dayIdx < schedule.length; dayIdx++)
        {
            let currRanges: Models.RelativeTimeRange[] = [];
            model.days[dayIdx].dailySchedule.ranges    = currRanges;
            let currDaySchedule                        = schedule[dayIdx].schedule;
            let start: number                          = null;
            for (let hourIdx = 0; hourIdx < currDaySchedule.length; hourIdx++)
            {
                for (let quarterHourIdx = 0; quarterHourIdx < currDaySchedule[hourIdx].length; quarterHourIdx++)
                {
                    let curr = currDaySchedule[hourIdx][quarterHourIdx];
                    if (curr)
                    {
                        if (start === null)
                        {
                            start = (4 * hourIdx + quarterHourIdx) * RecurringWeeklyScheduleExtended.secPerQuarterHour;
                        }
                    }
                    else if (start !== null)
                    {
                        currRanges.push(Models.RelativeTimeRange.newInstance(
                            {
                                offsetSeconds  : start,
                                durationSeconds: (4 * hourIdx + quarterHourIdx) * RecurringWeeklyScheduleExtended.secPerQuarterHour - start
                            }));
                        start = null;
                    }
                }
            }
            if (start !== null)
            {
                currRanges.push(Models.RelativeTimeRange.newInstance(
                    {
                        offsetSeconds  : start,
                        durationSeconds: RecurringWeeklyScheduleExtended.quarterHourPerDay * RecurringWeeklyScheduleExtended.secPerQuarterHour - start
                    }));
            }
        }

        return model;
    }

    private generateSchedule(model: Models.RecurringWeeklySchedule = this.m_model): DaySchedule[]
    {
        let schedule: DaySchedule[] = [];
        for (let dayIndex = 0; dayIndex < model.days.length; dayIndex++)
        {
            schedule.push({
                              name    : <any>model.days[dayIndex].dayOfWeek,
                              schedule: []
                          });
            for (let hourIndex = 0; hourIndex < 24; hourIndex++)
            {
                schedule[dayIndex].schedule.push([]);
                for (let quarterHourIndex = 0; quarterHourIndex < 4; quarterHourIndex++)
                {
                    schedule[dayIndex].schedule[hourIndex][quarterHourIndex] = 0;
                }
            }
        }

        this.fillSchedule(schedule, model);
        return schedule;
    }

    private fillSchedule(daySchedules: DaySchedule[],
                         model: Models.RecurringWeeklySchedule)
    {
        model.days.forEach((day) => this.fillDay(daySchedules, day));
    }

    private fillDay(daySchedules: DaySchedule[],
                    targetDay: Models.DailyScheduleWithDayOfWeek)
    {
        targetDay.dailySchedule.ranges.forEach((range) => this.setDayOverRange(daySchedules.find((currDay) => <any>currDay.name === targetDay.dayOfWeek),
                                                                               range.offsetSeconds / RecurringWeeklyScheduleExtended.secPerQuarterHour,
                                                                               (range.offsetSeconds + range.durationSeconds) / RecurringWeeklyScheduleExtended.secPerQuarterHour,
                                                                               1));
    }

    public getTimeLabel(hour: number): string
    {
        if (this.zoomInTarget !== null)
        {
            if (hour % 4 === 0)
            {
                if (this.zoomInTarget < 3)
                {
                    hour /= 4;
                }
                else if (this.zoomInTarget > 21)
                {
                    hour = hour / 4 + 6;
                }
                else
                {
                    hour = this.zoomInTarget + (hour - 12) / 4;
                }
            }
            else
            {
                return "";
            }
        }

        return `${hour % 12 === 0 ? 12 : hour % 12}`;
    }

    public setZoom(hour: number): void
    {
        this.zoomInTarget === null ? this.zoomInTarget = hour : this.zoomInTarget = null;
    }

    public zoomOut(): void
    {
        this.zoomInTarget = null;
    }

    public setUpShader(row: number,
                       col: number): void
    {
        let coords = this.getQuarterHourCoords(row, col);
        this.setShade(this.getState(coords));
        this.setSquare(coords);
    }

    public getZoomedState(row: number,
                          col: number): number
    {
        if (this.zoomInTarget === null) return null;
        return this.getState(this.getQuarterHourCoords(row, col));
    }

    private getQuarterHourCoords(row: number,
                                 col: number): QuarterHourCoords
    {
        if (this.zoomInTarget === null)
        {
            return {
                day        : row,
                hour       : col,
                quarterHour: null
            };
        }
        else
        {
            let hour: number;
            if (this.zoomInTarget < 3)
            {
                hour = Math.trunc(col / 4);
            }
            else if (this.zoomInTarget > 21)
            {
                hour = Math.trunc(col / 4) + 18;
            }
            else
            {
                hour = this.zoomInTarget - 3 + Math.trunc(col / 4);
            }

            return {
                day        : row,
                hour       : hour,
                quarterHour: col % 4
            };
        }
    }

    private getState(coords: QuarterHourCoords): number
    {
        return this.m_state[coords.day].schedule[coords.hour][this.zoomInTarget === null ? 0 : coords.quarterHour];
    }

    public deconstructShader(): void
    {
        this.targetShade = null;
    }

    public shadeSquare(row: number,
                       col: number): void
    {
        if (this.targetShade !== null) this.setSquare(this.getQuarterHourCoords(row, col));
    }

    /**
     * set (row, col) to targetShade - if zoomed in this is a 15 min interval, else it's an hour interval
     *
     * @param row
     * @param col
     */
    private setSquare(coords: QuarterHourCoords): void
    {
        if (coords.quarterHour === null)
        {
            this.m_state[coords.day].schedule[coords.hour].fill(this.targetShade);
        }
        else
        {
            this.m_state[coords.day].schedule[coords.hour][this.zoomInTarget === null ? 0 : coords.quarterHour] = this.targetShade;
        }

        this.generateModel();
        this.selectionChange.emit(this.m_model);
    }

    private setShade(shade: number): void
    {
        this.targetShade = (shade + 1) % 2;
    }

    private static getWeekdays(): Models.DayOfWeek[]
    {
        return DaysOfWeek.filter((day,
                                  dayIdx) => dayIdx >= 1 && dayIdx <= 5);
    }

    @Input() otherSchedules: Models.FilterableTimeRange[];

    public setToCustom(schedule: Models.RecurringWeeklySchedule): void
    {
        this.m_state = this.generateSchedule(schedule);
        this.generateModel();
        this.selectionChange.emit(this.m_model);
    }

    public setToPreset(presetId: string)
    {
        switch (presetId)
        {
            case "none":
                this.setAll(0);
                break;

            case "all":
                this.setAll(1);
                break;

            case "business hours":
                this.setAll(0);
                ScheduleSelectorComponent
                    .getWeekdays()
                    .forEach((dayName) =>
                                 this.setDayOverRange(this.m_state.find((day) => <any>day.name === dayName), 4 * 9, 4 * 17, 1));
                break;

            case "weekdays":
                this.setAll(0);
                ScheduleSelectorComponent
                    .getWeekdays()
                    .forEach((dayName) =>
                                 this.setDayOverRange(this.m_state.find((day) => <any>day.name === dayName), 0, 4 * 24, 1));
                break;

            case "weekend":
                this.setAll(0);
                this.setDayOverRange(this.m_state.find((day) => <any>day.name == "SATURDAY"), 0, 4 * 24, 1);
                this.setDayOverRange(this.m_state.find((day) => <any>day.name == "SUNDAY"), 0, 4 * 24, 1);
                break;

            case "nights":
                this.setAll(1);
                this.m_state.forEach((day) => this.setDayOverRange(day, 5 * 4, 4 * 21, 0));
                break;

            case "days":
                this.setAll(0);
                this.m_state.forEach((day) => this.setDayOverRange(day, 5 * 4, 4 * 21, 1));
        }
        this.generateModel();
        this.selectionChange.emit(this.m_model);
    }

    private setHeaviestFilter(day: DaySchedule): void
    {
        for (let low = 0; low < 4 * 24 - 1; low += 2) this.setDayOverRange(day, low, low + 1, 1);
    }

    private setDayOverRange(day: DaySchedule,
                            qhStartIndex: number,
                            qhEndIndex: number,
                            value: number)
    {
        if (day &&
            qhStartIndex >= 0 && qhStartIndex < 4 * 24 &&
            qhEndIndex >= 0 && qhEndIndex <= 4 * 24)
        {

            let target   = day.schedule;
            let currHour = Math.trunc(qhStartIndex / 4);
            let endHour  = Math.trunc(qhEndIndex / 4);

            // fill first partial hour
            let qhCurrIndex = qhStartIndex % 4;
            if (qhCurrIndex !== 0)
            {
                qhStartIndex = qhStartIndex - qhCurrIndex;
                while (qhCurrIndex !== 4 && qhStartIndex + qhCurrIndex < qhEndIndex)
                {
                    target[currHour][qhCurrIndex++] = value;
                }
                qhStartIndex += qhCurrIndex;
                qhCurrIndex = 0;
            }

            if (qhStartIndex === qhEndIndex) return;

            // truthy: qhStartIndex % 4 === 0

            // fill bulk
            currHour = qhStartIndex / 4;
            while (target.length && currHour < endHour)
            {
                target[currHour++].fill(value);
            }

            // fill last partial hour
            if (currHour === endHour)
            {
                let qhFinal = qhEndIndex % 4;
                while (qhCurrIndex !== qhFinal)
                {
                    target[endHour][qhCurrIndex++] = value;
                }
            }
        }
    }

    private setAll(value: number): void
    {
        for (let dayIdx = 0; dayIdx < this.m_state.length; dayIdx++)
        {
            for (let hourIdx = 0; hourIdx < this.m_state[dayIdx].schedule.length; hourIdx++)
            {
                this.m_state[dayIdx].schedule[hourIdx].fill(value);
            }
        }
    }

    get hoursOfDay(): number[]
    {
        return ScheduleSelectorComponent.hoursOfDay;
    }
}

interface QuarterHourCoords
{
    day: number;
    hour: number;
    quarterHour: number;
}

interface DaySchedule
{
    name: string;
    schedule: number[][];
}
