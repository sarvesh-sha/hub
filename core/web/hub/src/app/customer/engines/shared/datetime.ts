import * as Base from "app/customer/engines/shared/base";

import * as Models from "app/services/proxy/model/models";
import {DurationSelectorComponent} from "app/shared/forms/time-range/duration-selector.component";
import {ScheduleSelectorComponent} from "app/shared/forms/time-range/schedule-selector.component";
import {TimeZoneSelectorComponent} from "app/shared/forms/time-range/time-zone-selector.component";

import * as Blockly from "blockly";
import {UtilsService} from "framework/services/utils.service";

import {Block, BlockDef} from "framework/ui/blockly/block";
import {ModalDialogField} from "framework/ui/blockly/modal-dialog-field";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {DatePickerComponent} from "framework/ui/forms/date-picker.component";

export class DateTimeField extends ModalDialogField<DatePickerComponent, typeof DatePickerComponent, Date>
{
    private m_currentDate: Date;

    constructor(block: Block<any>)
    {
        super(block, DatePickerComponent, "Pick date and time");
    }

    protected initData(data: Date): void
    {
        if (data)
        {
            this.m_currentDate = new Date(data);
        }
        else if (!this.m_currentDate)
        {
            this.m_currentDate = new Date();
            this.m_currentDate.setSeconds(0);
        }
    }

    protected initComponent(component: DatePickerComponent): void
    {
        component.model = this.m_currentDate;
        component.modelChange.subscribe((d: Date) => this.m_currentDate = d);
    }

    public getModel(): Date
    {
        return this.m_currentDate;
    }

    protected getText(): string
    {
        let moment = MomentHelper.parse(this.m_currentDate);

        // Short date followed by short time
        return `${moment.format("l")} ${moment.format("LT")}`;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "datetime_literal",
              model       : Models.EngineLiteralDateTime,
              outputType  : Models.EngineValueDateTime
          })
export class LiteralDateTimeBlock extends Base.ExpressionBlock<Models.EngineLiteralDateTime>
{
    private datePickerField: DateTimeField;

    protected initFields(): void
    {
        super.initFields();

        this.datePickerField = new DateTimeField(this);

        this.registerModalField(this.datePickerField,
                                (model,
                                 value) =>
                                {
                                    model.zoneCreated = MomentHelper.getLocalZone();
                                    model.value       = value;
                                },
                                (model) =>
                                {
                                    // Legacy fixup
                                    if (!model.zoneCreated)
                                    {
                                        model.zoneCreated = MomentHelper.getLocalZone();
                                    }
                                    return model.value;
                                });


        this.appendDummyInput()
            .appendField(this.datePickerField.field);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "datetimerange",
              model       : Models.EngineExpressionBinaryDateTimeRange,
              outputType  : Models.EngineValueDateTimeRange
          })
export class DateTimeRangeBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryDateTimeRange>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueDateTime, Models.EngineValueDateTime);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_a);

        this.appendConnectedBlock(this.block_b)
            .appendField("to");
        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryDateTimeRange)
    {
        model.a = Models.EngineLiteralDateTime.newInstance({
                                                               value      : new Date(),
                                                               zoneCreated: MomentHelper.getLocalZone()
                                                           });
        model.b = Models.EngineLiteralDateTime.newInstance({
                                                               value      : new Date(),
                                                               zoneCreated: MomentHelper.getLocalZone()
                                                           });

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "datetime_current",
              model       : Models.EngineExpressionCurrentDateTime,
              outputType  : Models.EngineValueDateTime
          })
export class CurrentDateTimeBlock extends Base.ExpressionBlock<Models.EngineExpressionCurrentDateTime>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("now");
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "datetime_range_from_current",
              model       : Models.EngineOperatorUnaryDateTimeRangeFromCurrentTime,
              outputType  : Models.EngineValueDateTimeRange
          })
export class DateTimeRangeFromCurrentTimeBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryDateTimeRangeFromCurrentTime>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueDuration);

        this.block_a.shadow = true;

        this.appendConnectedBlock(this.block_a)
            .appendField("get range for last");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineOperatorUnaryDateTimeRangeFromCurrentTime)
    {
        model.a = Models.EngineLiteralDuration.newInstance({
                                                               amount: 10,
                                                               unit  : Models.ChronoUnit.MINUTES
                                                           });

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "datetime_range_from_time",
              model       : Models.EngineExpressionBinaryDateTimeRangeFromTime,
              outputType  : Models.EngineValueDateTimeRange
          })
export class DateTimeRangeFromTimeBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryDateTimeRangeFromTime>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueDateTime, Models.EngineValueDuration);

        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_b)
            .appendField("get range for last");

        this.appendConnectedBlock(this.block_a)
            .appendField("from");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryDateTimeRangeFromTime)
    {
        model.b = Models.EngineLiteralDuration.newInstance({
                                                               amount: 10,
                                                               unit  : Models.ChronoUnit.MINUTES
                                                           });

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "datetime_modify",
              model       : Models.EngineExpressionBinaryDateTimeModify,
              outputType  : Models.EngineValueDateTime
          })
export class DateTimeModifyBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryDateTimeModify>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueDateTime, Models.EngineValueDuration);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        let field_operation = this.registerStringField("operation",
                                                       (model,
                                                        value) => model.operation = UtilsService.getEnumValue(Models.CommonEngineDateTimeOperation, value),
                                                       (model) => model.operation);

        this.appendConnectedBlock(this.block_a);

        this.appendConnectedBlock(this.block_b)
            .appendField(new Blockly.FieldDropdown(DateTimeModifyBlock.getOperations()), field_operation);

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryDateTimeModify)
    {
        model.a = Models.EngineLiteralDateTime.newInstance({
                                                               value      : new Date(),
                                                               zoneCreated: MomentHelper.getLocalZone()
                                                           });
        model.b = Models.EngineLiteralDuration.newInstance({
                                                               amount: 10,
                                                               unit  : Models.ChronoUnit.MINUTES
                                                           });

        super.initForToolbox(model);
    }

    private static getOperations()
    {
        return [
            [
                "+",
                Models.CommonEngineDateTimeOperation.Add
            ],
            [
                "-",
                Models.CommonEngineDateTimeOperation.Subtract
            ]
        ];
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "datetime_getpart",
              model       : Models.EngineOperatorUnaryDateTimeGetField,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class DateTimeGetFieldBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryDateTimeGetField>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueDateTime);

        let field_unit = this.registerStringField("unit",
                                                  (model,
                                                   value) => model.unit = UtilsService.getEnumValue(Models.TimeUnit, value),
                                                  (model) => model.unit);

        this.appendConnectedBlock(this.block_a)
            .appendField("get")
            .appendField(new Blockly.FieldDropdown(DateTimeGetFieldBlock.getUnits()), field_unit)
            .appendField("of");
    }

    private static getUnits()
    {
        return [
            [
                "day",
                Models.ChronoUnit.DAYS
            ],
            [
                "month",
                Models.ChronoUnit.MONTHS
            ],
            [
                "year",
                Models.ChronoUnit.YEARS
            ],
            [
                "hours",
                Models.ChronoUnit.HOURS
            ],
            [
                "minutes",
                Models.ChronoUnit.MINUTES
            ]
        ];
    }
}

export class DurationValue
{
    amount: number;

    unit: Models.ChronoUnit;
}

export class DurationField extends ModalDialogField<DurationSelectorComponent, typeof DurationSelectorComponent, DurationValue>
{
    private m_duration: DurationValue;

    constructor(block: Block<any>)
    {
        super(block, DurationSelectorComponent, "Choose duration");
    }

    protected initData(data: DurationValue): void
    {
        if (data)
        {
            this.m_duration = data;
        }
        else if (!this.m_duration)
        {
            this.m_duration        = new DurationValue();
            this.m_duration.amount = 0;
            this.m_duration.unit   = Models.ChronoUnit.SECONDS;
        }
    }

    protected initComponent(component: DurationSelectorComponent): void
    {
        component.excludes = [];

        component.units = this.m_duration.unit;
        component.unitsChange.subscribe((u: Models.ChronoUnit) => this.m_duration.unit = u);

        component.amount = this.m_duration.amount;
        component.amountChange.subscribe((v: number) => this.m_duration.amount = v);
    }

    public getModel(): DurationValue
    {
        return this.m_duration;
    }

    protected getText(): string
    {
        return `${this.m_duration.amount} ${(this.m_duration.unit + "").toLowerCase()}`;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "duration_literal",
              model       : Models.EngineLiteralDuration,
              outputType  : Models.EngineValueDuration
          })
export class DurationLiteralBlock extends Base.ExpressionBlock<Models.EngineLiteralDuration>
{
    private durationField: DurationField;

    protected initFields(): void
    {
        super.initFields();

        this.durationField = new DurationField(this);

        this.registerModalField(this.durationField,
                                (model,
                                 value) =>
                                {
                                    model.amount = value.amount;
                                    model.unit   = value.unit;
                                },
                                (model) =>
                                {
                                    return {
                                        amount: model.amount,
                                        unit  : model.unit
                                    };
                                });

        this.appendDummyInput()
            .appendField(this.durationField.field);
    }

    protected initForToolbox(model: Models.EngineLiteralDuration): void
    {
        model.amount = 0;
        model.unit   = Models.ChronoUnit.SECONDS;
        super.initForToolbox(model);
    }
}

//--//

export class WeeklyScheduleField extends ModalDialogField<ScheduleSelectorComponent, typeof ScheduleSelectorComponent, Models.RecurringWeeklySchedule>
{
    private m_currentSchedule: Models.RecurringWeeklySchedule;

    constructor(block: Block<any>)
    {
        super(block, ScheduleSelectorComponent, "Define Weekly Schedule");
    }

    protected initData(data: Models.RecurringWeeklySchedule): void
    {
        if (data)
        {
            this.m_currentSchedule = Models.RecurringWeeklySchedule.newInstance(data);
        }
        else if (!this.m_currentSchedule)
        {
            this.m_currentSchedule = new Models.RecurringWeeklySchedule();
        }
    }

    protected initComponent(component: ScheduleSelectorComponent): void
    {
        component.model = this.m_currentSchedule;
        component.selectionChange.subscribe((d: Models.RecurringWeeklySchedule) => this.m_currentSchedule = d);
    }

    public getModel(): Models.RecurringWeeklySchedule
    {
        return this.m_currentSchedule;
    }

    protected getText(): string
    {
        let days = 0;

        for (let day of this.m_currentSchedule?.days || [])
        {
            if (day.dailySchedule?.ranges?.length > 0)
            {
                days++;
            }
        }

        if (!days) return "<not configured>";

        return `${days} days configured`;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "weeklyschedule_literal",
              model       : Models.EngineLiteralWeeklySchedule,
              outputType  : Models.EngineValueWeeklySchedule
          })
export class LiteralWeeklyScheduleBlock extends Base.ExpressionBlock<Models.EngineLiteralWeeklySchedule>
{
    private weeklyScheduleField: WeeklyScheduleField;

    protected initFields(): void
    {
        super.initFields();

        this.weeklyScheduleField = new WeeklyScheduleField(this);
        this.registerModalField(this.weeklyScheduleField,
                                (model,
                                 value) => model.value = value,
                                (model) => model.value);


        this.appendDummyInput()
            .appendField("weekly schedule")
            .appendField(this.weeklyScheduleField.field);
    }
}

//--//

export class TimeZoneField extends ModalDialogField<TimeZoneSelectorComponent, typeof TimeZoneSelectorComponent, string>
{
    private m_currentTimeZone: string;

    constructor(block: Block<any>)
    {
        super(block, TimeZoneSelectorComponent, "Pick time zone");
    }

    protected initData(data: string): void
    {
        this.m_currentTimeZone = data;
    }

    protected initComponent(component: TimeZoneSelectorComponent): void
    {
        component.zone = this.m_currentTimeZone;
        component.zoneChange.subscribe((v: string) => this.m_currentTimeZone = v);
    }

    public getModel(): string
    {
        return this.m_currentTimeZone;
    }

    protected getText(): string
    {
        return this.m_currentTimeZone;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "timezone_literal",
              model       : Models.EngineLiteralTimeZone,
              outputType  : Models.EngineValueTimeZone
          })
export class LiteralTimeZoneBlock extends Base.ExpressionBlock<Models.EngineLiteralTimeZone>
{
    private timezonePickerField: TimeZoneField;

    protected initFields(): void
    {
        super.initFields();

        this.timezonePickerField = new TimeZoneField(this);

        this.registerModalField(this.timezonePickerField,
                                (model,
                                 value) => model.value = value,
                                (model) => model.value || MomentHelper.getLocalZone());


        this.appendDummyInput()
            .appendField(this.timezonePickerField.field);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "datetime_set_timezone",
              model       : Models.EngineExpressionBinaryDateTimeSetTimeZone,
              outputType  : Models.EngineValueDateTime
          })
export class DateTimeSetTimeZoneBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryDateTimeSetTimeZone>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueDateTime, Models.EngineValueTimeZone);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        this.appendDummyInput();

        this.appendConnectedBlock(this.block_b)
            .appendField("set time zone");

        this.appendConnectedBlock(this.block_a)
            .appendField("on");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryDateTimeSetTimeZone)
    {
        model.a = Models.EngineLiteralDateTime.newInstance({
                                                               value      : new Date(),
                                                               zoneCreated: MomentHelper.getLocalZone()
                                                           });
        model.b = Models.EngineLiteralTimeZone.newInstance({
                                                               value: MomentHelper.getLocalZone()
                                                           });

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "weeklyschedule_set_timezone",
              model       : Models.EngineExpressionBinaryWeeklyScheduleSetTimeZone,
              outputType  : Models.EngineValueWeeklySchedule
          })
export class WeeklyScheduleSetTimeZoneBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryWeeklyScheduleSetTimeZone>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueWeeklySchedule, Models.EngineValueTimeZone);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        this.appendDummyInput();

        this.appendConnectedBlock(this.block_b)
            .appendField("set time zone");

        this.appendConnectedBlock(this.block_a)
            .appendField("on");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryDateTimeSetTimeZone)
    {
        model.a = Models.EngineLiteralWeeklySchedule.newInstance({
                                                                     value: new Models.RecurringWeeklySchedule()
                                                                 });
        model.b = Models.EngineLiteralTimeZone.newInstance({
                                                               value: MomentHelper.getLocalZone()
                                                           });

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "weeklyschedule_is_included",
              model       : Models.EngineExpressionBinaryWeeklyScheduleIsIncluded,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class WeeklyScheduleIsIncludedBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryWeeklyScheduleIsIncluded>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueWeeklySchedule, Models.EngineValueDateTime);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        this.appendDummyInput();

        this.appendConnectedBlock(this.block_b);

        this.appendConnectedBlock(this.block_a)
            .appendField("is included in schedule");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryDateTimeSetTimeZone)
    {
        model.a = Models.EngineLiteralWeeklySchedule.newInstance({
                                                                     value: new Models.RecurringWeeklySchedule()
                                                                 });
        model.b = Models.EngineLiteralDateTime.newInstance({
                                                               value      : new Date(),
                                                               zoneCreated: MomentHelper.getLocalZone()
                                                           });

        super.initForToolbox(model);
    }
}
