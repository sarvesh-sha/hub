import {Component, EventEmitter, Injector, QueryList, ViewChild, ViewChildren} from "@angular/core";
import {NgForm} from "@angular/forms";
import {ReportError} from "app/app.service";
import {MetricsDefinitionImportExport, MetricsDefinitionImportHandler} from "app/customer/configuration/metrics/metric-list-page.component";
import {MetricBlocklyBlocks} from "app/customer/configuration/metrics/wizard/metric-blockly-blocks";
import {MetricsWizardState, MetricWizardDialogComponent} from "app/customer/configuration/metrics/wizard/metric-wizard-dialog.component";
import {InputParameterExtended, MetricWizardInputFieldComponent} from "app/customer/configuration/metrics/wizard/metric-wizard-input-field.component";
import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";
import * as SharedSvc from "app/services/domain/base.service";
import {MetricsDefinitionVersionExtended} from "app/services/domain/metrics-definition-versions.service";
import {MetricsDefinitionDetailsForUserProgramExtended, MetricsDefinitionExtended} from "app/services/domain/metrics-definitions.service";
import * as Models from "app/services/proxy/model/models";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {debounceTime} from "rxjs/operators";

@Component({
               selector   : "o3-metric-definition-details-page",
               styleUrls  : ["./metric-definition-details-page.component.scss"],
               templateUrl: "./metric-definition-details-page.component.html"
           })
export class MetricDefinitionDetailsPageComponent extends SharedSvc.BaseComponentWithRouter
{
    metricsDefinitionId: string;
    metricsDefinition: MetricsDefinitionExtended;
    metricsVersions: MetricsDefinitionVersionExtended[];
    head: MetricsDefinitionVersionExtended;
    release: MetricsDefinitionVersionExtended;
    selectedVersion: MetricsDefinitionVersionExtended;
    currentDetails: MetricsDefinitionDetailsForUserProgramExtended;

    blocks = MetricBlocklyBlocks;

    blocklyDialogConfig = OverlayConfig.newInstance({
                                                        showCloseButton : true,
                                                        containerClasses: ["dialog-xl"]
                                                    });

    normalization: Models.NormalizationRules;

    private editHead: MetricsDefinitionVersionExtended;
    private editBase: MetricsDefinitionVersionExtended;
    private minEditVersion: number;
    hasEditSession = false;
    needsSave      = false;

    changeListener = new EventEmitter<void>();

    @ViewChild("metricsForm", {static: true}) metricsForm: NgForm;
    @ViewChild("blocklyWorkspace") blocklyWorkspace: AppBlocklyWorkspaceComponent;

    private m_inputFields: QueryList<MetricWizardInputFieldComponent>;
    @ViewChildren(MetricWizardInputFieldComponent) set inputFields(fields: QueryList<MetricWizardInputFieldComponent>)
    {
        this.m_inputFields = fields;
        this.detectChanges();
    }

    get inputFields(): QueryList<MetricWizardInputFieldComponent>
    {
        return this.m_inputFields;
    }

    public get inputs()
    {
        let res = [];

        for (let input of this.currentDetails?.data.getInputParameters() || [])
        {
            if (InputParameterExtended.extract(input))
            {
                res.push(input);
            }
        }

        return res;
    }

    constructor(inj: Injector)
    {
        super(inj);

        this.changeListener
            .pipe(debounceTime(1000))
            .subscribe(() =>
                       {
                           this.save();
                       });
    }

    protected async onNavigationComplete()
    {
        this.metricsDefinitionId = this.getPathParameter("id");

        this.normalization = await this.app.bindings.getActiveNormalizationRules();

        this.initMetricsDefinition();
    }

    async initMetricsDefinition()
    {
        this.metricsDefinition = await this.app.domain.metricsDefinitions.getExtendedById(this.metricsDefinitionId);
        if (!this.metricsDefinition)
        {
            this.exit();
            return;
        }

        this.app.ui.navigation.breadcrumbCurrentLabel = this.metricsDefinition.model.title;

        let allVersions      = await this.metricsDefinition.getAllVersions();
        this.metricsVersions = allVersions.filter((v) => !v.getDetails().temporary);

        // Set edit base to first temp version if it exists
        [this.editBase] = allVersions.filter((v) => v.getDetails().temporary);

        this.release = await this.metricsDefinition.getRelease();
        this.head    = await this.metricsDefinition.getHead();

        if (!!this.editBase && this.head.getDetails().temporary)
        {
            this.hasEditSession = true;
        }

        this.selectedVersion = this.release;
        this.editHead        = this.selectedVersion;

        this.metricsForm.form.markAsPristine();

        await this.initDetails();
    }

    async initDetails()
    {
        this.currentDetails = await this.editHead.getDetailsExtended();
    }

    async versionChange(newId: string)
    {
        this.selectedVersion = await this.app.domain.metricsDefinitionVersions.getExtendedById(newId);
        this.editHead        = this.selectedVersion;
        await this.initDetails();
    }

    async importVersion()
    {
        let result = await ImportDialogComponent.open(this, "Import Metrics Definition", new MetricsDefinitionImportHandler(this.app.domain));
        if (result && result[0] && result[0].details)
        {
            // Only look at first version we see in the import
            this.currentDetails.model = result[0].details;
            this.needsSave            = true;
            await this.createNewVersion();

            // Update details so blockly refreshes
            await this.initDetails();
        }
    }

    async createNewVersion()
    {
        // Save temporary version with any changes
        await this.save();
        // Squash to new permanent version
        await this.saveEditSession();
    }

    async saveEditSession()
    {
        if (this.editHead && this.hasEditSession && this.editBase)
        {
            this.selectedVersion = await this.editHead.squash(this.editBase);

            this.metricsVersions.push(this.selectedVersion);
            this.head           = this.selectedVersion;
            this.minEditVersion = null;
            this.editBase       = null;
            this.hasEditSession = false;
            this.app.framework.errors.success("New version saved", -1);
        }
    }

    @ReportError
    async save()
    {
        if (this.metricsDefinition && !this.isPristine || this.needsSave)
        {
            this.needsSave = false;
            // Save a new version
            let newVersion = this.editHead.getNewVersion();
            if (!this.minEditVersion)
            {
                this.minEditVersion = newVersion.model.version;
                this.hasEditSession = true;
            }

            newVersion.model.details           = this.currentDetails.typedModel;
            newVersion.model.details.temporary = true;
            newVersion                         = await newVersion.save();
            this.editHead                      = newVersion;
            this.selectedVersion               = newVersion;
            this.head                          = newVersion;

            if (!this.editBase)
            {
                this.editBase = newVersion;
            }

            this.blocklyWorkspace.markPristine();
            this.inputFields.forEach((input) => input.markPristine());
            this.currentDetails.markPristine();
        }
    }

    public get isPristine(): boolean
    {
        return this.isGraphPristine && this.isVersionPristine && this.areInputFieldsPristine;
    }

    public get isGraphPristine(): boolean
    {
        return !this.currentDetails || this.currentDetails.isPristine;
    }

    public get isVersionPristine(): boolean
    {
        return !this.blocklyWorkspace || !this.blocklyWorkspace.isDirty();
    }

    public get areInputFieldsPristine(): boolean
    {
        return this.inputFields && !this.inputFields.some((input) => !input.isPristine());
    }

    public get isInEditSession(): boolean
    {
        return this.editBase && this.editHead.model.version === this.head.model.version;
    }

    async configure(step: string)
    {
        let cfg = new MetricsWizardState(this.metricsDefinitionId, this.selectedVersion.model.sysId, false, false, step);
        if (await MetricWizardDialogComponent.open(cfg, this))
        {
            await this.initMetricsDefinition();
            await this.versionChange(cfg.metrics.version.model.sysId);
        }
    }

    async edit()
    {
        this.configure(null);
    }

    async copy()
    {
        await MetricWizardDialogComponent.open(new MetricsWizardState(this.metricsDefinitionId, this.selectedVersion.model.sysId, false, true), this);
    }

    async exportDefinition()
    {
        let name      = `${this.metricsDefinition.model.title}_v${this.selectedVersion.model.version}`;
        let timestamp = MomentHelper.fileNameFormat();

        DownloadDialogComponent.open<MetricsDefinitionImportExport[]>(this, "Download Metrics", `metrics__${name}_${timestamp}.json`, [
            {
                definition: this.metricsDefinition.getExport(),
                details   : this.selectedVersion.getDetails()
            }
        ]);
    }

    async makeRelease()
    {
        this.selectedVersion = await this.selectedVersion.makeRelease();
        this.release         = this.selectedVersion;
        await this.initDetails();
    }

    async undo()
    {
        this.editHead = await this.editHead.undo();
        await this.initDetails();
    }

    async redo()
    {
        this.editHead = await this.editHead.redo();
        await this.initDetails();
    }

    isRelease(id: string)
    {
        return this.release && id === this.release.model.sysId;
    }

    get isReleaseSelected(): boolean
    {
        return this.selectedVersion && this.isRelease(this.selectedVersion.model.sysId);
    }

    get canRedo(): boolean
    {
        return this.editHead && this.editHead.model.successors?.length > 0;
    }

    get canUndo(): boolean
    {
        return this.editHead && !!this.editHead.model.predecessor && this.minEditVersion && this.editHead.model.version >= this.minEditVersion;
    }

    async remove()
    {
        if (await this.confirmOperation("Click Yes to confirm deletion of this Metric."))
        {
            await this.metricsDefinition.remove();
            this.exit();
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
