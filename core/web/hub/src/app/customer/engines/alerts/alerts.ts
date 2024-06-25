import * as Base from "app/customer/engines/shared/base";

import * as Models from "app/services/proxy/model/models";

import * as Blockly from "blockly";
import {UtilsService} from "framework/services/utils.service";
import {BlockDef} from "framework/ui/blockly/block";

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "alert_create",
              model       : Models.AlertEngineOperatorUnaryCreateAlert,
              outputType  : Models.AlertEngineValueAlert
          })
export class CreateAlertBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryCreateAlert>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint);

        let field_alertType = this.registerStringField("alertType",
                                                       (model,
                                                        value) => model.type = UtilsService.getEnumValue(Models.AlertType, value),
                                                       (model) => model.type);


        this.appendConnectedBlock(this.block_a)
            .appendField("create")
            .appendField(new Blockly.FieldDropdown(this.getDropdownFromEnum(Models.AlertType, false)), field_alertType)
            .appendField("alert for");
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "alert_get",
              model       : Models.AlertEngineOperatorUnaryGetAlert,
              outputType  : Models.AlertEngineValueAlert
          })
export class GetAlertBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryGetAlert>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint);

        let field_alertType = this.registerStringField("alertType",
                                                       (model,
                                                        value) => model.type = UtilsService.getEnumValue(Models.AlertType, value),
                                                       (model) => model.type);

        this.appendConnectedBlock(this.block_a)
            .appendField("get")
            .appendField(new Blockly.FieldDropdown(this.getDropdownFromEnum(Models.AlertType, false)), field_alertType)
            .appendField("alert on");
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "alert_set_description",
              model       : Models.AlertEngineStatementSetAlertDescription
          })
export class SetAlertDescriptionBlock extends Base.StatementBlock<Models.AlertEngineStatementSetAlertDescription>
{
    protected initFields(): void
    {
        let block_alert = this.registerConnectedBlock<Models.EngineExpression>("alert",
                                                                               false,
                                                                               (model,
                                                                                value) => model.alert = value,
                                                                               (model) => model.alert,
                                                                               Models.AlertEngineValueAlert);

        let block_description = this.registerConnectedBlock<Models.EngineExpression>("description",
                                                                                     true,
                                                                                     (model,
                                                                                      value) => model.description = value,
                                                                                     (model) => model.description,
                                                                                     Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(block_alert)
            .appendField("set description of alert");

        this.appendConnectedBlock(block_description)
            .appendField("to");
        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.AlertEngineStatementSetAlertDescription): void
    {
        model.description = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "alert_set_text",
              model       : Models.AlertEngineStatementSetAlertText
          })
export class SetAlertTextBlock extends Base.StatementBlock<Models.AlertEngineStatementSetAlertText>
{
    protected initFields(): void
    {
        let block_alert = this.registerConnectedBlock<Models.EngineExpression>("alert",
                                                                               false,
                                                                               (model,
                                                                                value) => model.alert = value,
                                                                               (model) => model.alert,
                                                                               Models.AlertEngineValueAlert);

        let block_description = this.registerConnectedBlock<Models.EngineExpression>("text",
                                                                                     true,
                                                                                     (model,
                                                                                      value) => model.text = value,
                                                                                     (model) => model.text,
                                                                                     Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(block_alert)
            .appendField("set text of alert");

        this.appendConnectedBlock(block_description)
            .appendField("to");
        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.AlertEngineStatementSetAlertText): void
    {
        model.text = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "alert_set_timestamp",
              model       : Models.AlertEngineStatementSetAlertTimestamp
          })
export class SetAlertTimestampBlock extends Base.StatementBlock<Models.AlertEngineStatementSetAlertTimestamp>
{
    protected initFields(): void
    {
        let block_alert = this.registerConnectedBlock<Models.EngineExpression>("alert",
                                                                               false,
                                                                               (model,
                                                                                value) => model.alert = value,
                                                                               (model) => model.alert,
                                                                               Models.AlertEngineValueAlert);

        let block_description = this.registerConnectedBlock<Models.EngineExpression>("sample",
                                                                                     false,
                                                                                     (model,
                                                                                      value) => model.sample = value,
                                                                                     (model) => model.sample,
                                                                                     Models.EngineValue);

        this.appendConnectedBlock(block_alert)
            .appendField("set timestamp of alert");

        this.appendConnectedBlock(block_description)
            .appendField("to");
        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "alert_set_severity",
              model       : Models.AlertEngineStatementSetAlertSeverity
          })
export class SetAlertSeverityBlock extends Base.StatementBlock<Models.AlertEngineStatementSetAlertSeverity>
{
    protected initFields(): void
    {
        let block_alert = this.registerConnectedBlock<Models.EngineExpression>("alert",
                                                                               false,
                                                                               (model,
                                                                                value) => model.alert = value,
                                                                               (model) => model.alert,
                                                                               Models.AlertEngineValueAlert);

        let block_severity = this.registerConnectedBlock<Models.EngineExpression>("severity",
                                                                                  true,
                                                                                  (model,
                                                                                   value) => model.severity = value,
                                                                                  (model) => model.severity,
                                                                                  Models.AlertEngineValueAlertSeverity);

        this.appendConnectedBlock(block_alert)
            .appendField("set severity of alert");

        this.appendConnectedBlock(block_severity)
            .appendField("to");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.AlertEngineStatementSetAlertSeverity): void
    {
        model.severity = Models.AlertEngineLiteralAlertSeverity.newInstance({value: Models.AlertSeverity.NORMAL});
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "alert_set_status",
              model       : Models.AlertEngineStatementSetAlertStatus
          })
export class SetAlertSetStatusBlock extends Base.StatementBlock<Models.AlertEngineStatementSetAlertStatus>
{
    protected initFields(): void
    {
        let block_alert = this.registerConnectedBlock<Models.EngineExpression>("alert",
                                                                               false,
                                                                               (model,
                                                                                value) => model.alert = value,
                                                                               (model) => model.alert,
                                                                               Models.AlertEngineValueAlert);

        let block_status = this.registerConnectedBlock<Models.EngineExpression>("status",
                                                                                true,
                                                                                (model,
                                                                                 value) => model.status = value,
                                                                                (model) => model.status,
                                                                                Models.AlertEngineValueAlertStatus);

        this.appendConnectedBlock(block_alert)
            .appendField("set status of alert");

        this.appendConnectedBlock(block_status)
            .appendField("to");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.AlertEngineStatementSetAlertStatus): void
    {
        model.status = Models.AlertEngineLiteralAlertStatus.newInstance({value: Models.AlertStatus.active});
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "alert_get_severity",
              model       : Models.AlertEngineOperatorUnaryGetAlertSeverity,
              outputType  : Models.AlertEngineLiteralAlertSeverity
          })
export class GetAlertSeverityBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryGetAlertSeverity>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAlert);

        this.appendConnectedBlock(this.block_a)
            .appendField("get severity of alert");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "alert_get_status",
              model       : Models.AlertEngineOperatorUnaryGetAlertStatus,
              outputType  : Models.AlertEngineValueAlertStatus
          })
export class GetAlertStatusBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryGetAlertStatus>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAlert);

        this.appendConnectedBlock(this.block_a)
            .appendField("get status of alert");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "alert_has_changed",
              model       : Models.AlertEngineOperatorUnaryHasAlertChanged,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class HasAlertChangedBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryHasAlertChanged>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAlert);

        this.appendConnectedBlock(this.block_a)
            .appendField("has alert");

        this.appendDummyInput()
            .appendField("changed");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "literal_alert_status",
              model       : Models.AlertEngineLiteralAlertStatus,
              outputType  : Models.AlertEngineValueAlertStatus
          })
export class LiteralAlertStatusBlock extends Base.ExpressionBlock<Models.AlertEngineLiteralAlertStatus>
{
    protected initFields(): void
    {
        super.initFields();

        let field_val = this.registerStringField("val",
                                                 (model,
                                                  value) => model.value = UtilsService.getEnumValue(Models.AlertStatus, value),
                                                 (model) => model.value);

        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown(this.getDropdownFromEnum(Models.AlertStatus, false)), field_val);
    }

    protected initForToolbox(model: Models.AlertEngineLiteralAlertStatus): void
    {
        model.value = Models.AlertStatus.active;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "literal_alert_severity",
              model       : Models.AlertEngineLiteralAlertSeverity,
              outputType  : Models.AlertEngineValueAlertSeverity
          })
export class LiteralAlertSeverityBlock extends Base.ExpressionBlock<Models.AlertEngineLiteralAlertSeverity>
{
    protected initFields(): void
    {
        super.initFields();

        let field_val = this.registerStringField("val",
                                                 (model,
                                                  value) => model.value = UtilsService.getEnumValue(Models.AlertSeverity, value),
                                                 (model) => model.value);
        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown(this.getDropdownFromEnum(Models.AlertSeverity, false)), field_val);
    }

    protected initForToolbox(model: Models.AlertEngineLiteralAlertSeverity): void
    {
        model.value = Models.AlertSeverity.NORMAL;
        super.initForToolbox(model);
    }
}

//--//

export abstract class AlertActionBlock<T extends Models.AlertEngineExpressionAction> extends Base.ExpressionBlock<T>
{
    protected initFields(): void
    {
        let block_alert = this.registerConnectedBlock<Models.EngineExpression>("alert",
                                                                               false,
                                                                               (model,
                                                                                value) => model.alert = value,
                                                                               (model) => model.alert,
                                                                               Models.AlertEngineValueAlert);

        let block_deliveryOptions = this.registerConnectedBlock<Models.EngineExpression>("deliveryOptions",
                                                                                         false,
                                                                                         (model,
                                                                                          value) => model.deliveryOptions = value,
                                                                                         (model) => model.deliveryOptions,
                                                                                         Models.AlertEngineValueDeliveryOptions);

        this.appendConnectedBlock(block_alert)
            .appendField(`prepare ${this.getPrefix()} for`);

        this.appendConnectedBlock(block_deliveryOptions)
            .appendField("to");

        this.setInputsInline(true);

        super.initFields();
    }

    protected abstract getPrefix(): string;
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "prepare_email",
              model       : Models.AlertEngineExpressionActionNewEmail,
              outputType  : Models.AlertEngineValueEmail
          })
export class NewEmailBlock extends AlertActionBlock<Models.AlertEngineExpressionActionNewEmail>
{
    protected getPrefix()
    {
        return "email";
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "prepare_sms",
              model       : Models.AlertEngineExpressionActionNewSms,
              outputType  : Models.AlertEngineValueSms
          })
export class NewSmsBlock extends AlertActionBlock<Models.AlertEngineExpressionActionNewSms>
{
    protected getPrefix()
    {
        return "SMS";
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "prepare_ticket",
              model       : Models.AlertEngineExpressionActionNewTicket,
              outputType  : Models.AlertEngineValueTicket
          })
export class NewTicketBlock extends AlertActionBlock<Models.AlertEngineExpressionActionNewTicket>
{
    protected getPrefix()
    {
        return "ticket";
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "commit_action",
              model       : Models.AlertEngineStatementCommitAction
          })
export class CommitActionBlock extends Base.StatementBlock<Models.AlertEngineStatementCommitAction>
{
    protected initFields(): void
    {
        let block_action = this.registerConnectedBlock<Models.EngineExpression>("action",
                                                                                false,
                                                                                (model,
                                                                                 value) => model.action = value,
                                                                                (model) => model.action,
                                                                                Models.AlertEngineValueAction);

        this.appendConnectedBlock(block_action)
            .appendField("commit action");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "sms_set_sender",
              model       : Models.AlertEngineStatementSetSmsSender
          })
export class SetSmsSenderBlock extends Base.StatementBlock<Models.AlertEngineStatementSetSmsSender>
{
    protected initFields(): void
    {
        let block_action = this.registerConnectedBlock<Models.EngineExpression>("action",
                                                                                false,
                                                                                (model,
                                                                                 value) => model.sms = value,
                                                                                (model) => model.sms,
                                                                                Models.AlertEngineValueAction);

        let block_subject = this.registerConnectedBlock<Models.EngineExpression>("sender",
                                                                                 true,
                                                                                 (model,
                                                                                  value) => model.sender = value,
                                                                                 (model) => model.sender,
                                                                                 Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(block_action)
            .appendField("set sender of SMS");

        this.appendConnectedBlock(block_subject)
            .appendField("to");
        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.AlertEngineStatementSetSmsSender): void
    {
        model.sender = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "email_set_subject",
              model       : Models.AlertEngineStatementSetEmailSubject
          })
export class SetEmailSubjectBlock extends Base.StatementBlock<Models.AlertEngineStatementSetEmailSubject>
{
    protected initFields(): void
    {
        let block_action = this.registerConnectedBlock<Models.EngineExpression>("action",
                                                                                false,
                                                                                (model,
                                                                                 value) => model.email = value,
                                                                                (model) => model.email,
                                                                                Models.AlertEngineValueAction);

        let block_subject = this.registerConnectedBlock<Models.EngineExpression>("subject",
                                                                                 true,
                                                                                 (model,
                                                                                  value) => model.subject = value,
                                                                                 (model) => model.subject,
                                                                                 Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(block_action)
            .appendField("set subject of email");

        this.appendConnectedBlock(block_subject)
            .appendField("to");
        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.AlertEngineStatementSetEmailSubject): void
    {
        model.subject = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "email_add_line",
              model       : Models.AlertEngineStatementAddEmailLine
          })
export class AddEmailLineBlock extends Base.StatementBlock<Models.AlertEngineStatementAddEmailLine>
{
    protected initFields(): void
    {
        let block_action = this.registerConnectedBlock<Models.EngineExpression>("action",
                                                                                false,
                                                                                (model,
                                                                                 value) => model.email = value,
                                                                                (model) => model.email,
                                                                                Models.AlertEngineValueAction);

        let block_text = this.registerConnectedBlock<Models.EngineExpression>("line",
                                                                              true,
                                                                              (model,
                                                                               value) => model.text = value,
                                                                              (model) => model.text,
                                                                              Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(block_text)
            .appendField("add line");

        this.appendConnectedBlock(block_action)
            .appendField("to email");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.AlertEngineStatementAddEmailLine): void
    {
        model.text = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "sms_add_line",
              model       : Models.AlertEngineStatementAddSmsLine
          })
export class AddSmsLineBlock extends Base.StatementBlock<Models.AlertEngineStatementAddSmsLine>
{
    protected initFields(): void
    {
        let block_action = this.registerConnectedBlock<Models.EngineExpression>("action",
                                                                                false,
                                                                                (model,
                                                                                 value) => model.sms = value,
                                                                                (model) => model.sms,
                                                                                Models.AlertEngineValueAction);

        let block_text = this.registerConnectedBlock<Models.EngineExpression>("line",
                                                                              true,
                                                                              (model,
                                                                               value) => model.text = value,
                                                                              (model) => model.text,
                                                                              Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(block_text)
            .appendField("add line");

        this.appendConnectedBlock(block_action)
            .appendField("to SMS");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.AlertEngineStatementAddSmsLine): void
    {
        model.text = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}
