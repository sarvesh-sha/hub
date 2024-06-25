import * as Base from "app/customer/engines/shared/base";
import * as DateTime from "app/customer/engines/shared/datetime";

import * as Models from "app/services/proxy/model/models";
import * as Blockly from "blockly";

import {BlockDef} from "framework/ui/blockly/block";

export abstract class InputParameterBlock<T extends Models.EngineInputParameter> extends Base.ExpressionBlock<T>
{
    protected initFields(): void
    {
        super.initFields();

        let field_title = this.registerStringField("title",
                                                   (model,
                                                    value) => model.title = value,
                                                   (model) => model.title);

        let field_description = this.registerStringField("description",
                                                         (model,
                                                          value) => model.description = value,
                                                         (model) => model.description);
        this.appendDummyInput()
            .appendField("Title:")
            .appendField(new Blockly.FieldTextInput(""), field_title);

        this.appendDummyInput()
            .appendField("Description:")
            .appendField(new Blockly.FieldTextInput(""), field_description);

        this.appendValueField(this.appendDummyInput()
                                  .appendField("Value:"));
    }

    protected abstract appendValueField(input: Blockly.Input): void;

    protected initForToolbox(model: T): void
    {
        model.title       = "Input Parameter";
        model.description = "Description of Input Parameter";
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "duration_input",
              model       : Models.EngineInputParameterDuration,
              outputType  : Models.EngineValueDuration
          })
export class InputParameterDurationBlock extends InputParameterBlock<Models.EngineInputParameterDuration>
{
    private durationField: DateTime.DurationField;

    protected appendValueField(input: Blockly.Input)
    {
        this.durationField = new DateTime.DurationField(this);
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
        input.appendField(this.durationField.field);
    }

    protected initForToolbox(model: Models.EngineInputParameterDuration): void
    {
        model.amount = 10;
        model.unit   = Models.ChronoUnit.MINUTES;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "datetime_input",
              model       : Models.EngineInputParameterDateTime,
              outputType  : Models.EngineValueDateTime
          })
export class InputParameterDateTimeBlock extends InputParameterBlock<Models.EngineInputParameterDateTime>
{
    private datePickerField: DateTime.DateTimeField;

    protected appendValueField(input: Blockly.Input)
    {
        this.datePickerField = new DateTime.DateTimeField(this);
        this.registerModalField(this.datePickerField,
                                (model,
                                 value) => model.value = value,
                                (model) => model.value);

        input.appendField(this.datePickerField.field);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_logic_input",
              model       : Models.EngineInputParameterBoolean,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class InputParameterBooleanBlock extends InputParameterBlock<Models.EngineInputParameterBoolean>
{
    protected appendValueField(input: Blockly.Input)
    {
        let field_val = this.registerStringField("val",
                                                 (model,
                                                  value) => model.value = value == "true",
                                                 (model) => model.value ? "true" : "false");

        input.appendField(new Blockly.FieldDropdown([
                                                        [
                                                            "true",
                                                            "true"
                                                        ],
                                                        [
                                                            "false",
                                                            "false"
                                                        ]
                                                    ]), field_val);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "number_input",
              model       : Models.EngineInputParameterNumber,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class InputParameterNumberBlock extends InputParameterBlock<Models.EngineInputParameterNumber>
{
    protected appendValueField(input: Blockly.Input)
    {
        let field_val = this.registerFloatField("val",
                                                (model,
                                                 value) => model.value = value,
                                                (model) => model.value);

        input.appendField(new Blockly.FieldNumber(0), field_val);
    }

    protected initForToolbox(model: Models.EngineInputParameterNumber): void
    {
        model.value = 0;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "string_input",
              model       : Models.EngineInputParameterString,
              outputType  : Models.EngineValuePrimitiveString
          })
export class InputParameterStringBlock extends InputParameterBlock<Models.EngineInputParameterString>
{
    protected appendValueField(input: Blockly.Input)
    {
        let field_val = this.registerStringField("val",
                                                 (model,
                                                  value) => model.value = value,
                                                 (model) => model.value);

        input.appendField("\"")
             .appendField(new Blockly.FieldTextInput(""), field_val)
             .appendField("\"");
    }

    protected initForToolbox(model: Models.EngineInputParameterString): void
    {
        model.value = "";
        super.initForToolbox(model);
    }
}
