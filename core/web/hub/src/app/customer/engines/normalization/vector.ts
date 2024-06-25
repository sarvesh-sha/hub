import {NormalizationRulesBlocklyWorkspaceData} from "app/customer/configuration/classification/normalization-rules-blockly-workspace.data";
import {DocumentTableComponent} from "app/customer/engines/normalization/document-table.component";
import {TermFrequencyInverseDocumentFrequencyConfigurationComponent, TermFrequencyInverseDocumentFrequencyParameters} from "app/customer/engines/normalization/term-frequency-inverse-document-frequency-configuration.component";
import * as Base from "app/customer/engines/shared/base";
import {ExportableStringSetField} from "app/customer/engines/shared/exportable-string-set.component";
import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";
import * as Models from "app/services/proxy/model/models";
import {FieldNumber} from "blockly/blockly";
import {Block, BlockDef} from "framework/ui/blockly/block";
import {ModalDialogField} from "framework/ui/blockly/modal-dialog-field";

class DocumentTableField extends ModalDialogField<DocumentTableComponent, typeof DocumentTableComponent, Models.NormalizationEngineValueDocument[]>
{
    private m_assignments: Models.NormalizationEngineValueDocument[];

    constructor(block: Block<any>)
    {
        super(block, DocumentTableComponent, "Setup Documents");
    }

    protected initData(data: Models.NormalizationEngineValueDocument[]): void
    {
        if (data)
        {
            this.m_assignments = data;
        }
        else if (!this.m_assignments)
        {
            this.m_assignments = [];
        }
    }

    protected initComponent(component: DocumentTableComponent): void
    {
        let workspace   = this.m_block.getComponent<AppBlocklyWorkspaceComponent>();
        let data        = <NormalizationRulesBlocklyWorkspaceData>workspace.data;
        component.rules = data && data.rules;
        component.data  = this.m_assignments;
    }

    public getModel(): Models.NormalizationEngineValueDocument[]
    {
        return this.m_assignments.map((r) => Models.NormalizationEngineValueDocument.deepClone(r));
    }

    protected getText(): string
    {
        return `${this.m_assignments.length} documents`;
    }
}

@BlockDef({
              blockContext     : "NormalizationRules",
              blockName        : "o3_literal_document_table",
              model            : Models.NormalizationEngineLiteralDocumentSet,
              outputType       : Models.EngineValueList,
              outputElementType: Models.NormalizationEngineValueDocument
          })
export class LiteralDocumentSetBlock extends Base.ExpressionBlock<Models.NormalizationEngineLiteralDocumentSet>
{
    private m_documentField: DocumentTableField;

    protected initFields(): void
    {
        super.initFields();

        this.m_documentField = new DocumentTableField(this);

        this.registerModalField(this.m_documentField,
                                (model,
                                 value) => model.value = value,
                                (model) => model.value);


        this.appendDummyInput()
            .appendField(this.m_documentField.field);
    }

    protected initForToolbox(model: Models.NormalizationEngineLiteralDocumentSet): void
    {
        model.value = [];
        super.initForToolbox(model);
    }
}

class TermFrequencyInverseDocumentFrequencyParameterField extends ModalDialogField<TermFrequencyInverseDocumentFrequencyConfigurationComponent, typeof TermFrequencyInverseDocumentFrequencyConfigurationComponent, TermFrequencyInverseDocumentFrequencyParameters>
{
    private m_model: TermFrequencyInverseDocumentFrequencyParameters;

    constructor(block: Block<any>)
    {
        super(block, TermFrequencyInverseDocumentFrequencyConfigurationComponent, "TF-IDF Parameters");
    }

    protected initData(data: TermFrequencyInverseDocumentFrequencyParameters): void
    {
        if (data)
        {
            this.m_model = data;
        }
        else if (!this.m_model)
        {
            this.m_model = {
                minNgram       : 1,
                maxNgram       : 1,
                minDocFrequency: 1
            };
        }
    }

    protected initComponent(component: TermFrequencyInverseDocumentFrequencyConfigurationComponent): void
    {
        component.model = this.m_model;
    }

    public getModel(): TermFrequencyInverseDocumentFrequencyParameters
    {
        return {...this.m_model};
    }

    protected getText(): string
    {
        return `ngram: (${this.m_model.minNgram}, ${this.m_model.maxNgram}), minDF: ${this.m_model.minDocFrequency}`;
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "o3_score_top_document",
              model       : Models.NormalizationEngineOperatorBinaryScoreTopDocument,
              outputType  : Models.NormalizationEngineValueDocument
          })
export class ScoreTopDocumentBlock extends Base.BinaryOperatorBlock<Models.NormalizationEngineOperatorBinaryScoreTopDocument>
{
    private m_tfidfField: TermFrequencyInverseDocumentFrequencyParameterField;

    protected initFields(): void
    {
        this.initOperatorFields(Models.EngineValuePrimitiveString, Models.EngineValueList);

        this.appendConnectedBlock(this.block_b)
            .appendField("score top document from");

        this.appendConnectedBlock(this.block_a)
            .appendField("with text");

        this.m_tfidfField = new TermFrequencyInverseDocumentFrequencyParameterField(this);
        this.registerModalField(this.m_tfidfField,
                                (model,
                                 value) =>
                                {
                                    model.minNgram        = value.minNgram;
                                    model.maxNgram        = value.maxNgram;
                                    model.minDocFrequency = value.minDocFrequency;
                                },
                                (model) =>
                                {
                                    return {
                                        minNgram       : model.minNgram || 1,
                                        maxNgram       : model.maxNgram || 1,
                                        minDocFrequency: model.minDocFrequency || 1
                                    };
                                });
        this.appendDummyInput()
            .appendField("with parameters")
            .appendField(this.m_tfidfField.field);

        this.setInputsInline(false);
    }

    protected initForToolbox(model: Models.NormalizationEngineOperatorBinaryScoreTopDocument)
    {
        model.minNgram        = 1;
        model.maxNgram        = 1;
        model.minDocFrequency = 1;
        super.initForToolbox(model);
    }
}


@BlockDef({
              blockContext     : "NormalizationRules",
              blockName        : "o3_score_documents",
              model            : Models.NormalizationEngineOperatorBinaryScoreDocuments,
              outputType       : Models.EngineValueList,
              outputElementType: Models.NormalizationEngineValueDocument
          })
export class ScoreDocumentsBlock extends Base.BinaryOperatorBlock<Models.NormalizationEngineOperatorBinaryScoreDocuments>
{
    private m_tfidfField: TermFrequencyInverseDocumentFrequencyParameterField;

    protected initFields(): void
    {
        this.initOperatorFields(Models.EngineValuePrimitiveString, Models.EngineValueList);

        this.appendConnectedBlock(this.block_b)
            .appendField("score documents from");

        this.appendConnectedBlock(this.block_a)
            .appendField("with text");

        let threshold = this.registerFloatField("threshold",
                                                (model,
                                                 value) => model.minScore = value,
                                                (model) => model.minScore);

        this.m_tfidfField = new TermFrequencyInverseDocumentFrequencyParameterField(this);
        this.registerModalField(this.m_tfidfField,
                                (model,
                                 value) =>
                                {
                                    model.minNgram        = value.minNgram;
                                    model.maxNgram        = value.maxNgram;
                                    model.minDocFrequency = value.minDocFrequency;
                                },
                                (model) =>
                                {
                                    return {
                                        minNgram       : model.minNgram || 1,
                                        maxNgram       : model.maxNgram || 1,
                                        minDocFrequency: model.minDocFrequency || 1
                                    };
                                });


        this.appendDummyInput()
            .appendField("above score threshold")
            .appendField(new FieldNumber(.5, 0, 1), threshold);

        this.appendDummyInput()
            .appendField("with parameters")
            .appendField(this.m_tfidfField.field);

        this.setInputsInline(false);
    }

    protected initForToolbox(model: Models.NormalizationEngineOperatorBinaryScoreDocuments)
    {
        model.minNgram        = 1;
        model.maxNgram        = 1;
        model.minDocFrequency = 1;
        super.initForToolbox(model);
    }
}


@BlockDef({
              blockContext     : "NormalizationRules",
              blockName        : "o3_filter_documents",
              model            : Models.NormalizationEngineOperatorBinaryFilterDocumentSet,
              outputType       : Models.EngineValueList,
              outputElementType: Models.NormalizationEngineValueDocument
          })
export class FilterDocumentsBlock extends Base.BinaryOperatorBlock<Models.NormalizationEngineOperatorBinaryFilterDocumentSet>
{
    private m_termsField: ExportableStringSetField;

    protected initFields(): void
    {
        this.initOperatorFields(Models.EngineValueList, Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(this.block_a)
            .appendField("filter documents from");

        this.appendConnectedBlock(this.block_b)
            .appendField("against text");

        this.m_termsField = new ExportableStringSetField(this, "Negative Terms");
        this.registerModalField(this.m_termsField,
                                (model,
                                 value) => model.negativeTerms = value,
                                (model) => model.negativeTerms);

        this.appendDummyInput()
            .appendField("with negative terms")
            .appendField(this.m_termsField.field);

        this.setInputsInline(false);
    }

    protected initForToolbox(model: Models.NormalizationEngineOperatorBinaryFilterDocumentSet)
    {
        model.negativeTerms = [];
        super.initForToolbox(model);
    }
}


@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "o3_get_document_jaccard_index",
              model       : Models.NormalizationEngineOperatorBinaryGetJaccardIndex,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class GetJaccardIndexBlock extends Base.BinaryOperatorBlock<Models.NormalizationEngineOperatorBinaryGetJaccardIndex>
{
    protected initFields(): void
    {
        this.initOperatorFields(Models.EngineValuePrimitiveString, Models.NormalizationEngineValueDocument);

        this.appendConnectedBlock(this.block_b)
            .appendField("get jaccard index between document");

        this.appendConnectedBlock(this.block_a)
            .appendField("and text");

    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "o3_get_document_score",
              model       : Models.NormalizationEngineOperatorUnaryGetDocumentScore,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class GetDocumentScoreBlock extends Base.UnaryOperatorBlock<Models.NormalizationEngineOperatorBinaryGetJaccardIndex>
{
    protected initFields(): void
    {
        this.initOperatorFields(Models.NormalizationEngineValueDocument);

        this.appendConnectedBlock(this.block_a)
            .appendField("get score from document");
    }
}


@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "o3_set_point_class_from_document",
              model       : Models.NormalizationEngineStatementSetPointClassFromDocument
          })
export class SetPointClassFromDocumentBlock extends Base.StatementBlock<Models.NormalizationEngineStatementSetPointClassFromDocument>
{
    protected initFields(): void
    {
        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               false,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.NormalizationEngineValueDocument);

        this.appendConnectedBlock(block_value)
            .appendField("set point class from document");
    }
}
