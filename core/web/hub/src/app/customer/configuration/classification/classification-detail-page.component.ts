import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ReportError} from "app/app.service";

import {ClassificationDataExporter} from "app/customer/configuration/classification/classification-data-exporter";
import {ClassificationDiffPickerDialogComponent} from "app/customer/configuration/classification/classification-diff/classification-diff.component";
import {ClassifiedPoint, NormalizationDialogConfig} from "app/customer/configuration/classification/classification-run/classification-run.component";
import {NormalizationRulesBlocklyBlocks} from "app/customer/configuration/classification/normalization-rules-blockly-blocks";
import {ValidationConfig} from "app/customer/configuration/classification/validation-run.component";
import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";

import * as SharedSvc from "app/services/domain/base.service";
import {NormalizationDefinitionDetailsExtended, NormalizationExtended} from "app/services/domain/normalization.service";
import * as Models from "app/services/proxy/model/models";
import {IProviderForMapHost} from "app/shared/tables/provider-for-map";
import {ExcelExporter} from "app/shared/utils/excel-exporter";
import {Lookup} from "framework/services/utils.service";

import {ControlOption} from "framework/ui/control-option";

import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {Future} from "framework/utils/concurrency";

@Component({
               selector   : "o3-classification-detail-page",
               templateUrl: "./classification-detail-page.component.html",
               styleUrls  : ["./classification-detail-page.component.scss"]
           })
export class ClassificationDetailPageComponent extends SharedSvc.BaseComponentWithRouter implements IProviderForMapHost
{
    normalizationId: string;

    normalization: NormalizationExtended;

    normalizationValid: boolean;
    normalizationPristine: boolean;

    @ViewChild("normalizationDetailsForm") normalizationDetailsForm: NgForm;

    @ViewChild("blocklyWorkspace")
    set blocklyWorkspace(blocklyWorkspace: AppBlocklyWorkspaceComponent)
    {
        this.m_blocklyWorkspace = blocklyWorkspace;
        if (blocklyWorkspace)
        {
            blocklyWorkspace.refreshSize();
        }
    }

    get blocklyWorkspace(): AppBlocklyWorkspaceComponent
    {
        return this.m_blocklyWorkspace;
    }

    private m_blocklyWorkspace: AppBlocklyWorkspaceComponent;

    blocks = NormalizationRulesBlocklyBlocks;

    @ViewChild("blocklyDialog", {static: true}) blocklyDialog: OverlayComponent;
    blocklyDialogConfig = OverlayConfig.newInstance({
                                                        showCloseButton : true,
                                                        containerClasses: ["dialog-xl"]
                                                    });
    currentDetails: NormalizationDefinitionDetailsExtended;

    @ViewChild("testResultsDialog", {static: true}) testResultsDialog: OverlayComponent;
    testResultsDialogConfig = OverlayConfig.onTopDraggable({containerClasses: ["dialog-md"]});
    testResults: Models.NormalizationEngineExecutionStep[];
    testLogs: Models.LogLine[];

    testFormDialogConfig = OverlayConfig.onTopDraggable({containerClasses: ["dialog-md"]});


    //--//

    units: ControlOption<Models.EngineeringUnits>[] = [];
    locations: ControlOption<string>[]              = [];
    locationTypes: ControlOption<string>[]          = [];
    private m_pointClassOptions: ControlOption<string>[];
    private m_equipmentClassOptions: ControlOption<string>[];

    //--//

    public sample = Models.DeviceElementNormalizationSample.newInstance({details: new Models.ClassificationPointInputDetails()});

    normalizationState: NormalizationDialogConfig;
    validationState: ValidationConfig;

    loaded = new Future<void>();

    constructor(inj: Injector)
    {
        super(inj);

        //--//

        this.normalizationState = new NormalizationDialogConfig();
        this.validationState    = new ValidationConfig();
    }

    protected async onNavigationComplete()
    {
        this.normalizationId = this.getPathParameter("id");

        if (this.normalizationId)
        {
            // load normalization info
            this.normalization = await this.app.domain.normalization.getExtendedById(this.normalizationId);
            if (!this.normalization)
            {
                this.exit();
                return;
            }

            if (!this.normalization.model.rules) this.normalization.model.rules = new Models.NormalizationRules();
            if (!this.normalization.model.rules.validation) this.normalization.model.rules.validation = new Models.ValidationRules();
            if (!this.normalization.model.rules.validation.equipmentRules) this.normalization.model.rules.validation.equipmentRules = [];
            if (!this.normalization.model.rules.validation.pointClassRules) this.normalization.model.rules.validation.pointClassRules = [];

            await this.initClassOptions();

            this.normalizationPristine = true;
            this.normalizationValid    = true;

            this.bind();

            this.loaded.resolve();
        }
    }

    private bind()
    {
        // set breadcrumbs
        let model                                     = this.normalization.model;
        this.app.ui.navigation.breadcrumbCurrentLabel = `Version ${model.version}`;
    }

    @ReportError
    async save()
    {
        let model = await this.app.domain.apis.normalization.create(this.normalization.model);
        this.exit();
    }

    @ReportError
    async makeActive()
    {
        await this.app.domain.apis.normalization.makeActive(this.normalization.model.sysId);
        this.exit();
    }

    @ReportError
    async remove()
    {
        if (await this.confirmOperation("Click Yes to confirm deletion of this Normalization Rule."))
        {
            await this.app.domain.apis.normalization.remove(this.normalization.model.sysId);
            this.exit();
        }
    }

    get rules(): Models.NormalizationRules
    {
        return this.normalization && this.normalization.model.rules;
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    getUnit(unit: Models.EngineeringUnits): ControlOption<Models.EngineeringUnits>
    {
        return this.units.find((u) => u.id === unit);
    }

    private readonly m_pointClassOptionMap = new Map<string, { raw: Models.PointClass, option: ControlOption<string> }>();

    get pointClassOptions(): ControlOption<string>[]
    {
        return this.m_pointClassOptions;
    }

    set pointClassOptions(options: ControlOption<string>[])
    {
        this.m_pointClassOptions = options;
        this.m_pointClassOptionMap.clear();

        for (let option of options)
        {
            this.m_pointClassOptionMap.set(option.id,
                                           {
                                               raw   : null,
                                               option: option
                                           });
        }

        for (let cls of this.normalization.model.rules.pointClasses)
        {
            const id  = cls.id + "";
            const val = this.m_pointClassOptionMap.get(id);
            if (val)
            {
                val.raw = cls;
                this.m_pointClassOptionMap.set(id, val);
            }
        }
    }

    getPointClassOption(id: string): ControlOption<string>
    {
        return this.m_pointClassOptionMap.get(id)?.option;
    }

    private readonly m_equipClassOptionMap = new Map<string, { raw: Models.EquipmentClass, option: ControlOption<string> }>();

    get equipmentClassOptions(): ControlOption<string>[]
    {
        return this.m_equipmentClassOptions;
    }

    set equipmentClassOptions(options: ControlOption<string>[])
    {
        this.m_equipmentClassOptions = options;
        this.m_equipClassOptionMap.clear();
        for (let option of options)
        {
            this.m_equipClassOptionMap.set(option.id,
                                           {
                                               raw   : null,
                                               option: option
                                           });
        }

        for (let cls of this.normalization.model.rules.equipmentClasses)
        {
            const id  = cls.id + "";
            const val = this.m_equipClassOptionMap.get(id);
            if (val)
            {
                val.raw = cls;
                this.m_equipClassOptionMap.set(id, val);
            }
        }
    }

    getEquipmentClassOption(id: string): ControlOption<string>
    {
        return this.m_equipClassOptionMap.get(id)?.option;
    }

    async initClassOptions()
    {
        this.pointClassOptions     = await this.app.bindings.getPointClasses(false, this.normalization.model.rules, false);
        this.equipmentClassOptions = await this.app.bindings.getEquipmentClasses(false, this.normalization.model.rules);

        this.units         = await this.app.domain.units.getEngineeringUnits();
        this.locations     = await this.app.bindings.getLocationsOptions();
        this.locationTypes = await this.app.bindings.getLocationTypeOptions();
    }

    compare()
    {
        ClassificationDiffPickerDialogComponent.open(this, this.normalization.model.rules);
    }

    exportRules()
    {
        this.saveLogic(false);
        DownloadDialogComponent.open(this,
                                     "Normalization Export",
                                     DownloadDialogComponent.fileName("normalization_v" + this.normalization.model.version),
                                     this.normalization.model.rules);
    }

    async importRules()
    {
        let result = await ImportDialogComponent.open(this, "Normalization Import", {
            returnRawBlobs: () => false,
            parseFile     : (contents: string) => this.app.domain.normalization.parseImportedRules(contents)
        });

        if (result != null)
        {
            this.normalization.model.rules = result;
            this.setDirty();
            this.bind();
            await this.initClassOptions();
        }
    }

    public async importSection(title: string,
                               parser: (contents: string) => string)
    {
        let result = await ImportDialogComponent.open(this, title, {
            returnRawBlobs: () => false,
            parseFile     : (contents: string) => this.app.domain.normalization.parseImportedRules(parser(contents))
        });

        if (result != null)
        {
            this.normalization.model.rules = result;
            this.setDirty();
            this.bind();
        }
    }

    public exportSection(title: string,
                         fileNamePrefix: string,
                         data: any)
    {
        DownloadDialogComponent.open(this, title, DownloadDialogComponent.fileName(`normalization_${fileNamePrefix}_${this.normalization.model.version}`), data);
    }

    exportData()
    {
        DownloadDialogComponent.openWithGenerator(this,
                                                  "Normalization Data Export",
                                                  DownloadDialogComponent.fileName("normalization_data_v" + this.normalization.model.version),
                                                  new ClassificationDataExporter(this.app.domain, this.normalization.model.rules));
    }

    //--//

    async exportChangesToExcel(onlyChanges = false)
    {
        let exporter = new ExcelExporter(this.app.domain.apis.exports, "Normalization Changes");
        exporter.addColumnHeader("SysId");
        exporter.addColumnHeader("DeviceId");
        exporter.addColumnHeader("ObjectId");
        exporter.addColumnHeader("DeviceName");
        exporter.addColumnHeader("DeviceDescription");
        exporter.addColumnHeader("BackupName");
        exporter.addColumnHeader("Original Units");
        exporter.addColumnHeader("Assigned Units");
        exporter.addColumnHeader("Output");
        exporter.addColumnHeader("Location");
        exporter.addColumnHeader("Equipment");
        exporter.addColumnHeader("EquipmentClassId");
        exporter.addColumnHeader("EquipmentClassName");
        exporter.addColumnHeader("EquipmentClassDescription");
        exporter.addColumnHeader("EquipmentHierarchy");
        exporter.addColumnHeader("pointClassId");
        exporter.addColumnHeader("pointClassName");
        exporter.addColumnHeader("pointClassDescription");
        exporter.addColumnHeader("aliasPointClassId");
        exporter.addColumnHeader("aliasPointClassName");
        exporter.addColumnHeader("aliasPointClassDescription");
        exporter.addColumnHeader("overridePointClassId");
        exporter.addColumnHeader("overridePointClassName");
        exporter.addColumnHeader("overridePointClassDescription");
        exporter.addColumnHeader("score");
        exporter.addColumnHeader("positiveScore");
        exporter.addColumnHeader("negativeScore");
        exporter.addColumnHeader("threshold");

        let fileName = DownloadDialogComponent.fileName("normalization_results", ".xlsx");
        DownloadDialogComponent.openWithGenerator(this, "Normalization Changes", fileName, exporter);

        for (let change of this.normalizationState.changes)
        {
            if (onlyChanges)
            {
                const output = new ClassifiedPoint(change, this.normalizationState.workflowOverrides, this);
                if (!output.hasChanged) continue;
            }

            let row = await exporter.addRow();

            let locations     = (change.locations || []).map((loc) => loc.name);
            let equipmentKeys = Object.keys(change.equipments);
            let nonLeaves     = Object.keys(change.equipmentRelationships);
            let leaves        = equipmentKeys.filter((key) => !nonLeaves.find((k) => k === key));
            let eq            = leaves.length > 0 ? change.equipments[leaves[0]] : null;
            let ec            = this.getEquipmentClass(eq ? eq.equipmentClassId : null);
            let allEquip      = formatEquipment(change.equipments, change.equipmentRelationships);
            let pc            = this.getPointClass(change.currentResult.id);
            let aliasPc       = pc && pc.aliasPointClassId ? this.getPointClass(pc.aliasPointClassId) : null;
            let override      = this.normalizationState.workflowOverrides.pointClasses[change.sysId];
            let overridePc    = override ? this.getPointClass(override) : null;

            row.push(change.sysId,
                     change.details.controllerIdentifier,
                     change.details.objectIdentifier,
                     change.details.objectName,
                     change.details.objectDescription,
                     change.details.objectBackupName,
                     change.details.objectUnits,
                     change.currentResult?.assignedUnits,
                     change.normalizedName,
                     locations.join("/"),
                     eq?.name,
                     ec?.id,
                     ec?.equipClassName,
                     ec?.description,
                     allEquip.join(", "),
                     pc?.id,
                     pc?.pointClassName,
                     pc?.pointClassDescription,
                     aliasPc?.id,
                     aliasPc?.pointClassName,
                     aliasPc?.pointClassDescription,
                     overridePc?.id,
                     overridePc?.pointClassName,
                     overridePc?.pointClassDescription,
                     change.currentResult?.positiveScore + change.currentResult?.negativeScore,
                     change.currentResult?.positiveScore,
                     change.currentResult?.negativeScore,
                     change.currentResult?.threshold);
        }

        exporter.finish();
    }

    async exportTermsToExcel()
    {
        let exporter = new ExcelExporter(this.app.domain.apis.exports, "Unknown Terms");
        exporter.addColumnHeader("Term");
        exporter.addColumnHeader("Count");

        let fileName = DownloadDialogComponent.fileName("unknown_terms", ".xlsx");
        DownloadDialogComponent.openWithGenerator(this, "Unknown Terms", fileName, exporter);

        for (let word in this.normalizationState.unknownWords)
        {
            if (word.length > 0)
            {
                let row = await exporter.addRow();
                row.push(word, this.normalizationState.unknownWords[word]);
            }
        }

        exporter.finish();
    }


    async importOverrides()
    {
        let result = await ImportDialogComponent.open(this, "Overrides Import", {
            returnRawBlobs: () => false,
            parseFile     : (contents: string) => this.app.domain.normalization.parseImportedOverrides(contents)
        });

        if (result != null)
        {
            for (let override of result)
            {
                this.normalization.model.rules.pointOverrides[override.sysId] = override.overrides;
            }
        }
    }

    //--//

    editLogic()
    {
        this.currentDetails = this.normalization.getDetailsExtended();
        this.blocklyDialog.toggleOverlay();
    }

    saveLogic(cleanUp = true)
    {
        if (this.currentDetails && this.normalization)
        {
            this.normalization.setDetailsExtended(this.currentDetails);
        }

        if (cleanUp)
        {
            this.currentDetails = null;
        }
    }

    @ReportError
    async testLogic()
    {
        this.resetTestResults();
        this.saveLogic(false);
        this.detectChanges();
        try
        {
            this.testResultsDialog.toggleOverlay();

            this.updateRules();

            let results      = await this.normalization.evaluate(this.sample, 1000000, true);
            this.testResults = results.steps;
            this.testLogs    = results.logEntries;
            this.detectChanges();
        }
        catch (err)
        {
            this.testResultsDialog.toggleOverlay();
        }
    }

    public updateRules(): void
    {
        if (this.normalizationPristine)
        {
            this.sample.rulesId = this.normalization.model.sysId;
            this.sample.rules   = null;
        }
        else
        {
            this.sample.rulesId = null;
            this.sample.rules   = this.normalization.model.rules;
        }
    }

    formatLocations(locations: Models.NormalizationEquipmentLocation[] = []): string
    {
        return locations.map((l) => l.name)
                        .join(" - ");
    }

    formatEquipment(equipment: Models.NormalizationEquipment): string
    {
        if (equipment)
        {
            let ec = this.getEquipmentClass(equipment.equipmentClassId);
            if (ec)
            {
                return `${equipment.name}: ${ec.equipClassName} - ${ec.description}`;
            }

            return `${equipment.name}`;
        }

        return "";
    }

    getPointClass(id: string): Models.PointClass
    {
        return this.m_pointClassOptionMap.get(id)?.raw;
    }

    getEquipmentClass(id: string): Models.EquipmentClass
    {
        return this.m_equipClassOptionMap.get(id)?.raw;
    }

    public resetTestResults()
    {
        this.testResults = null;
        this.testLogs    = null;

        if (this.testResultsDialog.isOpen)
        {
            this.testResultsDialog.closeOverlay();
        }
    }

    //--//

    public setDirty()
    {
        this.normalizationPristine = false;
    }
}

export function formatEquipment(equipments: Lookup<Models.NormalizationEquipment>,
                                equipmentRelationships: Lookup<string[]>)
{
    const output: string[] = [];
    if (equipments && equipmentRelationships)
    {
        const parents = new Map<string, string>();

        for (let key in equipmentRelationships)
        {
            const children = equipmentRelationships[key];
            for (let child of children)
            {
                parents.set(child, key);
            }
        }

        for (let key in equipments)
        {
            // It's a leaf
            if (!equipmentRelationships[key])
            {
                let path  = "";
                let child = key;
                while (child)
                {
                    path  = `${equipments[child].name}${path ? " \u279d " + path : ""}`;
                    child = parents.get(child);
                }
                output.push(path);
            }
        }
    }

    return output;
}
