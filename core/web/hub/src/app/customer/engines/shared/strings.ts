import {Type} from "@angular/core";
import * as Base from "app/customer/engines/shared/base";
import {ConnectedBlockHandler} from "app/customer/engines/shared/base";
import {LookupTableComponent} from "app/customer/engines/shared/lookup-table.component";
import {RegexReplaceTableComponent} from "app/customer/engines/shared/regex-replace-table.component";

import * as Models from "app/services/proxy/model/models";

import * as Blockly from "blockly";

import {Block, BlockDef} from "framework/ui/blockly/block";
import {ModalDialogField} from "framework/ui/blockly/modal-dialog-field";

@BlockDef({
              blockContext: "Shared",
              blockName   : "string_literal",
              model       : Models.EngineLiteralString,
              outputType  : Models.EngineValuePrimitiveString
          })
export class LiteralStringBlock extends Base.ExpressionBlock<Models.EngineLiteralString>
{
    protected initFields(): void
    {
        super.initFields();

        let field_val = this.registerStringField("val",
                                                 (model,
                                                  value) =>
                                                 {
                                                     if (value) this.setShadow(false);
                                                     model.value = value;
                                                 },
                                                 (model) => model.value);

        this.appendDummyInput()
            .appendField("\"")
            .appendField(new Blockly.FieldTextInput(""), field_val)
            .appendField("\"");
    }

    protected initForToolbox(model: Models.EngineLiteralString): void
    {
        model.value = "";
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_format_text",
              model       : Models.EngineExpressionFormatText,
              outputType  : Models.EngineValuePrimitiveString
          })
export class FormatTextBlock extends Base.ExpressionBlock<Models.EngineExpressionFormatText> implements Base.ListHelperHost
{
    private m_format: ConnectedBlockHandler<Models.EngineExpression, Models.EngineExpressionFormatText>;
    private m_helper: Base.ListHelper<FormatTextBlock, Models.EngineExpressionFormatText>;

    protected initFields(): void
    {
        super.initFields();

        this.m_format = this.registerConnectedBlock<Models.EngineExpression>("FORMAT",
                                                                             false,
                                                                             (model,
                                                                              value) => model.format = value,
                                                                             (model) => model.format,
                                                                             Models.EngineValuePrimitiveString);

        this.m_helper = new Base.ListHelper(this);
        this.m_helper.init();

        this.setWarningText("To substitute a parameter, use ${<index>}\n" +
                            " \n" +
                            "The substitution can be customized using |<key>=<value> syntax:\n" +
                            " \n" +
                            "For numbers:  ${0|precision=8.2}\n" +
                            "For lists: ${0|separator=#}\n" +
                            "For dates: ${0|pattern=yyyy-MM-dd HH:mm:ss}\n");
    }

    protected initFromModel(model: Models.EngineExpressionFormatText): void
    {
        this.m_helper.setInputs(model.arguments);

        super.initFromModel(model);
    }

    public toModel(): Models.EngineExpressionFormatText
    {
        let model       = super.toModel();
        model.arguments = this.m_helper.collectInputs();

        return model;
    }

    public regenerateShape()
    {
        if (!this.getInput("FORMAT"))
        {
            this.appendConnectedBlock(this.m_format)
                .appendField("format");
        }

        this.m_helper.updateShapeForInputs();
    }

    public handleNewInput(index: number,
                          input: Blockly.Input): Type<Models.EngineValue>
    {
        if (index == 0)
        {
            input.appendField("with")
                 .setAlign(Blockly.ALIGN_RIGHT);
        }

        return null;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_log_text",
              model       : Models.EngineStatementLog
          })
export class LogTextBlock extends Base.StatementBlock<Models.EngineStatementLog> implements Base.ListHelperHost
{
    private m_format: ConnectedBlockHandler<Models.EngineExpression, Models.EngineStatementLog>;
    private m_helper: Base.ListHelper<LogTextBlock, Models.EngineStatementLog>;

    protected initFields(): void
    {
        this.m_format = this.registerConnectedBlock<Models.EngineExpression>("FORMAT",
                                                                             false,
                                                                             (model,
                                                                              value) => model.format = value,
                                                                             (model) => model.format,
                                                                             Models.EngineValuePrimitiveString);

        this.m_helper = new Base.ListHelper(this);
        this.m_helper.init();

        this.setPreviousStatement(true);
        this.setNextStatement(true);

        this.setWarningText("To substitute a parameter, use ${<index>}\n" +
                            " \n" +
                            "The substitution can be customized using <key>=<value> syntax:\n" +
                            " \n" +
                            "For numbers:  ${0|precision=8.2}\n" +
                            "For lists: ${0|separator=#}\n" +
                            "For dates: ${0|pattern=yyyy-MM-dd HH:mm:ss}\n");
    }

    protected initFromModel(model: Models.EngineStatementLog): void
    {
        this.m_helper.setInputs(model.arguments);

        super.initFromModel(model);
    }

    public toModel(): Models.EngineStatementLog
    {
        let model       = super.toModel();
        model.arguments = this.m_helper.collectInputs();

        return model;
    }

    public regenerateShape()
    {
        if (!this.getInput("FORMAT"))
        {
            this.appendConnectedBlock(this.m_format)
                .appendField("log");
        }

        this.m_helper.updateShapeForInputs();
    }

    public handleNewInput(index: number,
                          input: Blockly.Input): Type<Models.EngineValue>
    {
        if (index == 0)
        {
            input.appendField("with")
                 .setAlign(Blockly.ALIGN_RIGHT);
        }

        return null;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_string_concat",
              model       : Models.EngineExpressionBinaryStringConcat,
              outputType  : Models.EngineValuePrimitiveString
          })
export class StringConcatBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryStringConcat>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitive, Models.EngineValuePrimitive);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_a);

        this.appendConnectedBlock(this.block_b)
            .appendField("concat");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryStringConcat)
    {
        model.a = Models.EngineLiteralString.newInstance({value: ""});
        model.b = Models.EngineLiteralString.newInstance({value: ""});

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_string_starts_with",
              model       : Models.EngineExpressionBinaryStringStartsWith,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class StringStartsWithBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryStringStartsWith>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString, Models.EngineValuePrimitiveString);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_a);

        this.appendConnectedBlock(this.block_b)
            .appendField("starts with");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryStringStartsWith)
    {
        model.a = Models.EngineLiteralString.newInstance({value: ""});
        model.b = Models.EngineLiteralString.newInstance({value: ""});

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_string_ends_with",
              model       : Models.EngineExpressionBinaryStringEndsWith,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class StringEndsWithBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryStringEndsWith>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString, Models.EngineValuePrimitiveString);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_a);

        this.appendConnectedBlock(this.block_b)
            .appendField("ends with");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryStringEndsWith)
    {
        model.a = Models.EngineLiteralString.newInstance({value: ""});
        model.b = Models.EngineLiteralString.newInstance({value: ""});

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_string_indexof",
              model       : Models.EngineExpressionBinaryStringIndexOf,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class StringIndexOfBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryStringIndexOf>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString, Models.EngineValuePrimitiveString);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_a);

        this.appendConnectedBlock(this.block_b)
            .appendField("index of");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryStringIndexOf)
    {
        model.a = Models.EngineLiteralString.newInstance({value: ""});
        model.b = Models.EngineLiteralString.newInstance({value: ""});

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_string_tolowercase",
              model       : Models.EngineOperatorUnaryStringToLowerCase,
              outputType  : Models.EngineValuePrimitiveString
          })
export class StringToLowerCaseBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryStringToLowerCase>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueList, Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField("to lowercase");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_string_touppercase",
              model       : Models.EngineOperatorUnaryStringToUpperCase,
              outputType  : Models.EngineValuePrimitiveString
          })
export class StringToUpperCaseBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryStringToUpperCase>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueList, Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField("to uppercase");

        this.setInputsInline(true);
    }
}


@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_string_toNumber",
              model       : Models.EngineOperatorUnaryStringToNumber,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class StringToNumberBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryStringToNumber>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField("to number");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_string__split",
              model       : Models.EngineExpressionBinaryStringSplit,
              outputType  : Models.EngineValueList
          })
export class StringSplitBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryStringSplit>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString, Models.EngineValuePrimitiveString);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_a)
            .appendField("split");

        this.appendConnectedBlock(this.block_b)
            .appendField("with");
        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryStringSplit)
    {
        model.a = Models.EngineLiteralString.newInstance({value: ""});
        model.b = Models.EngineLiteralString.newInstance({value: ""});

        super.initForToolbox(model);
    }
}

//--//

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_regex_match",
              model       : Models.EngineExpressionBinaryRegexMatch,
              outputType  : Models.EngineValueRegexMatch
          })
export class RegexMatchBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryRegexMatch>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString, Models.EngineValuePrimitiveString);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_a)
            .appendField("match RegEx");

        this.appendConnectedBlock(this.block_b)
            .appendField("with");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryRegexMatch)
    {
        model.a = Models.EngineLiteralString.newInstance({value: ""});
        model.b = Models.EngineLiteralString.newInstance({value: ""});

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_regex_match_case_sensitive",
              model       : Models.EngineExpressionBinaryRegexMatchCaseSensitive,
              outputType  : Models.EngineValueRegexMatch
          })
export class RegexMatchCaseSensitiveBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryRegexMatchCaseSensitive>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString, Models.EngineValuePrimitiveString);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_a)
            .appendField("match case-sensitive RegEx");

        this.appendConnectedBlock(this.block_b)
            .appendField("with");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryRegexMatchCaseSensitive)
    {
        model.a = Models.EngineLiteralString.newInstance({value: ""});
        model.b = Models.EngineLiteralString.newInstance({value: ""});

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_regex_get_group",
              model       : Models.EngineOperatorUnaryRegexGetGroup,
              outputType  : Models.EngineValuePrimitiveString
          })
export class RegexGetGroupBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryRegexGetGroup>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueRegexMatch);

        let field_group = this.registerIntegerField("group",
                                                    (model,
                                                     value) => model.group = value,
                                                    (model) => model.group);

        this.appendConnectedBlock(this.block_a)
            .appendField("get group")
            .appendField(new Blockly.FieldNumber(0), field_group)
            .appendField("from");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineOperatorUnaryRegexGetGroup)
    {
        model.group = 0;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_regex_replace",
              model       : Models.EngineExpressionBinaryRegexReplace,
              outputType  : Models.EngineValuePrimitiveString
          })
export class RegexReplaceBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryRegexReplace>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueRegexMatch, Models.EngineValuePrimitiveString);

        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_a)
            .appendField("replace match in");

        this.appendConnectedBlock(this.block_b)
            .appendField("with");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryRegexReplace)
    {
        model.b = Models.EngineLiteralString.newInstance({value: ""});

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_regex_is_match",
              model       : Models.EngineExpressionBinaryStringIsMatch,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class RegexIsMatchBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryStringIsMatch>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString, Models.EngineValuePrimitiveString);

        this.block_a.shadow = true;
        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_a)
            .appendField("is match");

        this.appendConnectedBlock(this.block_b)
            .appendField("with");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryStringIsMatch)
    {
        model.a = Models.EngineLiteralString.newInstance({value: ""});
        model.b = Models.EngineLiteralString.newInstance({value: ""});

        super.initForToolbox(model);
    }
}

//--//

export class RegexReplaceTableField extends ModalDialogField<RegexReplaceTableComponent, typeof RegexReplaceTableComponent, Models.RegexReplacement[]>
{
    private m_replacements: Models.RegexReplacement[];

    constructor(block: Block<any>)
    {
        super(block, RegexReplaceTableComponent, "Setup Regex Replacements");
    }

    protected initData(data: Models.RegexReplacement[]): void
    {
        if (data)
        {
            this.m_replacements = data;
        }
        else if (!this.m_replacements)
        {
            this.m_replacements = [];
        }
    }

    protected initComponent(component: RegexReplaceTableComponent): void
    {
        component.data = this.m_replacements;
    }

    public getModel(): Models.RegexReplacement[]
    {
        return this.m_replacements.map((r) => Models.RegexReplacement.newInstance(r));
    }

    protected getText(): string
    {
        return `${this.m_replacements.length} replacements`;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_regex_replace_table_literal",
              model       : Models.EngineLiteralRegexReplaceTable,
              outputType  : Models.EngineValueRegexReplaceTable
          })
export class LiteralRegexReplaceTableBlock extends Base.ExpressionBlock<Models.EngineLiteralRegexReplaceTable>
{
    private m_regexReplaceField: RegexReplaceTableField;

    protected initFields(): void
    {
        super.initFields();

        this.m_regexReplaceField = new RegexReplaceTableField(this);

        this.registerModalField(this.m_regexReplaceField,
                                (model,
                                 value) => model.replacements = value,
                                (model) => model.replacements);

        this.appendDummyInput()
            .appendField(this.m_regexReplaceField.field);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_regex_replace_table",
              model       : Models.EngineExpressionBinaryRegexTableReplace,
              outputType  : Models.EngineValuePrimitiveString
          })
export class RegexTableReplaceBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryRegexTableReplace>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueRegexReplaceTable, Models.EngineValuePrimitiveString);

        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_a)
            .appendField("run replacements");

        this.appendConnectedBlock(this.block_b)
            .appendField("on");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryRegexTableReplace)
    {
        model.b = Models.EngineLiteralString.newInstance({value: ""});

        super.initForToolbox(model);
    }
}

//--//

export class LookupTableField extends ModalDialogField<LookupTableComponent, typeof LookupTableComponent, Models.LookupEntry[]>
{
    private m_entries: Models.LookupEntry[];

    constructor(block: Block<any>)
    {
        super(block, LookupTableComponent, "Setup Lookup Entries");
    }

    protected initData(data: Models.LookupEntry[]): void
    {
        if (data)
        {
            this.m_entries = data;
        }
        else if (!this.m_entries)
        {
            this.m_entries = [];
        }
    }

    protected initComponent(component: LookupTableComponent): void
    {
        component.data = this.m_entries;
    }

    public getModel(): Models.LookupEntry[]
    {
        return this.m_entries.map((r) => Models.LookupEntry.newInstance(r));
    }

    protected getText(): string
    {
        return `${this.m_entries.length} lookup entries`;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_lookup_table_literal",
              model       : Models.EngineLiteralLookupTable,
              outputType  : Models.EngineValueLookupTable
          })
export class LiteralLookupTableBlock extends Base.ExpressionBlock<Models.EngineLiteralLookupTable>
{
    private m_lookupField: LookupTableField;

    protected initFields(): void
    {
        super.initFields();

        this.m_lookupField = new LookupTableField(this);

        this.registerModalField(this.m_lookupField,
                                (model,
                                 value) => model.entries = value,
                                (model) => model.entries);

        this.appendDummyInput()
            .appendField(this.m_lookupField.field);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_lookup_table_lookup",
              model       : Models.EngineExpressionBinaryLookupTableLookup,
              outputType  : Models.EngineValuePrimitiveString
          })
export class LookupTableLookupBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryLookupTableReplace>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueLookupTable, Models.EngineValuePrimitiveString);

        this.block_b.shadow = true;

        this.appendConnectedBlock(this.block_b)
            .appendField("lookup key");

        this.appendConnectedBlock(this.block_a)
            .appendField("in table");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryLookupTableReplace)
    {
        model.b = Models.EngineLiteralString.newInstance({value: ""});

        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_lookup_table_filter",
              model       : Models.EngineExpressionBinaryLookupTableFilter,
              outputType  : Models.EngineValueList
          })
export class LookupTableFilterBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryLookupTableFilter>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueLookupTable, Models.EngineValueList);

        this.appendConnectedBlock(this.block_b)
            .appendField("filter list");

        this.appendConnectedBlock(this.block_a)
            .appendField("with keys in table");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_lookup_table_replace",
              model       : Models.EngineExpressionBinaryLookupTableReplace,
              outputType  : Models.EngineValueList
          })
export class LookupTableReplaceBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryLookupTableReplace>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueLookupTable, Models.EngineValueList);

        this.appendConnectedBlock(this.block_b)
            .appendField("map values");

        this.appendConnectedBlock(this.block_a)
            .appendField("using table");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_lookup_table_put",
              model       : Models.EngineStatementLookupTablePut
          })
export class LookupTablePutBlock extends Base.StatementBlock<Models.EngineStatementLookupTablePut>
{
    protected initFields(): void
    {
        let block_table = this.registerConnectedBlock<Models.EngineExpression>("table",
                                                                               false,
                                                                               (model,
                                                                                value) => model.table = value,
                                                                               (model) => model.table,
                                                                               Models.EngineValueLookupTable);

        let block_key   = this.registerConnectedBlock<Models.EngineExpression>("key",
                                                                               true,
                                                                               (model,
                                                                                value) => model.key = value,
                                                                               (model) => model.key,
                                                                               Models.EngineValuePrimitiveString);
        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               true,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValuePrimitiveString);

        let caseInsensitive = this.registerBooleanField("caseInsensitive",
                                                        (model,
                                                         value) => model.caseInsensitive = value,
                                                        (model) => model.caseInsensitive || true);

        this.appendConnectedBlock(block_key)
            .appendField("put key");

        this.appendConnectedBlock(block_value)
            .appendField("value");

        this.appendConnectedBlock(block_table)
            .appendField("into table");

        this.appendDummyInput()
            .appendField("case insensitive")
            .appendField(new Blockly.FieldCheckbox(), caseInsensitive);

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.EngineStatementLookupTablePut): void
    {
        model.key = Models.EngineLiteralString.newInstance({ value: "" });
        model.value = Models.EngineLiteralString.newInstance({ value: "" });
        super.initForToolbox(model);
    }
}

