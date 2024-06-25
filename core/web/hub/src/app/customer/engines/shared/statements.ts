import * as Base from "app/customer/engines/shared/base";

import * as Models from "app/services/proxy/model/models";
import * as Blockly from "blockly";
import {BlockDef} from "framework/ui/blockly/block";

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_comment",
              model       : Models.EngineStatementComment
          })
export class NopCommentBlock extends Base.StatementBlock<Models.EngineStatementComment>
{
    protected initFields(): void
    {
        let field_text = this.registerStringField("text",
                                                  (model,
                                                   value) => model.text = value,
                                                  (model) => model.text);

        this.appendDummyInput()
            .appendField("note: ")
            .appendField(new Blockly.FieldTextInput(""), field_text);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_comment_section",
              model       : Models.EngineStatementCommentSection
          })
export class NopCommentSectionBlock extends Base.StatementBlock<Models.EngineStatementCommentSection>
{
    private static readonly c_field_DO = "do";

    protected initFields(): void
    {
        let field_text = this.registerStringField("text",
                                                  (model,
                                                   value) => model.text = value,
                                                  (model) => model.text);


        this.appendDummyInput()
            .appendField("section: ")
            .appendField(new Blockly.FieldTextInput(""), field_text);

        this.appendStatementInput(NopCommentSectionBlock.c_field_DO);
    }

    protected initFromModel(model: Models.EngineStatementCommentSection): void
    {
        super.initFromModel(model);

        this.createStatements(NopCommentSectionBlock.c_field_DO, model.statements);

        if (model.statements?.length > 0)
        {
            this.setCollapsed(true);
        }
    }

    public toModel(): Models.EngineStatementCommentSection
    {
        let model        = super.toModel();
        model.statements = this.getStatementsModels(NopCommentSectionBlock.c_field_DO);
        return model;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_thread_start",
              model       : Models.EngineThread
          })
export class ThreadBlock extends Base.ComputationBlock<Models.EngineThread>
{
    protected initFields(): void
    {
        this.appendDummyInput()
            .appendField("Start");
        this.appendStatementInput(Base.ComputationBlock.StatementsInputName);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_thread_end",
              model       : Models.EngineStatementThreadEnd
          })
export class StatementThreadEndBlock extends Base.AppBlock<Models.EngineStatementThreadEnd>
{
    protected initFields(): void
    {
        this.appendDummyInput()
            .appendField("End");
        this.setPreviousStatement(true);
    }

    protected initFromModel(): void
    {
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "repeat_while",
              model       : Models.EngineStatementRepeatWhile
          })
export class StatementRepeatWhileBlock extends Base.StatementBlock<Models.EngineStatementRepeatWhile>
{
    private static readonly c_field_CONDITION = "condition";
    private static readonly c_field_DO        = "do";

    protected initFields(): void
    {
        this.appendTypedValueInput(StatementRepeatWhileBlock.c_field_CONDITION, Models.EngineValuePrimitiveBoolean)
            .appendField("repeat while");
        this.appendStatementInput(StatementRepeatWhileBlock.c_field_DO)
            .appendField("do");
    }

    protected initFromModel(model: Models.EngineStatementRepeatWhile): void
    {
        this.createConnectedBlock(StatementRepeatWhileBlock.c_field_CONDITION, model.condition);
        this.createStatements(StatementRepeatWhileBlock.c_field_DO, model.statements);
    }

    public toModel(): Models.EngineStatementRepeatWhile
    {
        let model        = super.toModel();
        model.condition  = this.getInputModel(StatementRepeatWhileBlock.c_field_CONDITION);
        model.statements = this.getStatementsModels(StatementRepeatWhileBlock.c_field_DO);
        return model;
    }
}
