import {Component, Inject, Injector} from "@angular/core";
import {AppContext} from "app/app.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {MetricsDefinitionVersionExtended} from "app/services/domain/metrics-definition-versions.service";
import * as Metrics from "app/services/domain/metrics-definitions.service";
import {MetricsDefinitionDetailsExtended, MetricsDefinitionExtended} from "app/services/domain/metrics-definitions.service";
import * as Models from "app/services/proxy/model/models";
import {WizardDialogComponent, WizardDialogState} from "app/shared/overlays/wizard-dialog.component";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {Future} from "framework/utils/concurrency";

@Component({
               templateUrl: "./metric-wizard-dialog.component.html"
           })
export class MetricWizardDialogComponent extends WizardDialogComponent<MetricsWizardState>
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: MetricsWizardState)
    {
        super(dialogRef, inj, data);
    }

    public static async open(cfg: WizardDialogState,
                             base: BaseApplicationComponent): Promise<boolean>
    {
        return await super.open(cfg, base, MetricWizardDialogComponent);
    }
}

export class MetricsWizardState extends WizardDialogState
{
    metrics: MetricsData;
    id: string;
    versionId: string;
    isLibrary: boolean;
    isCopy: boolean;

    constructor(id: string        = null,
                versionId: string = null,
                library: boolean  = false,
                copy: boolean     = false,
                step: string      = null)
    {
        super(!id || copy, step);

        this.id        = id;
        this.versionId = versionId;
        this.isLibrary = library;
        this.isCopy    = copy;

        this.metrics = new MetricsData();
    }

    public async create(comp: BaseApplicationComponent,
                        goto: boolean): Promise<boolean>
    {
        // Save the model and record the result
        let result = await this.save(comp);

        // If save successful and goto set, navigate to record
        if (result && goto)
        {
            comp.app.ui.navigation.go("/configuration/metrics/metric", [this.metrics.metricsId]);
        }

        // Return save result
        return result;
    }

    public async save(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            this.metrics.metricsDefinition = await this.metrics.metricsDefinition.save();
            this.metrics.metricsId         = this.metrics.metricsDefinition.model.sysId;

            this.metrics.version.setDefinition(this.metrics.metricsDefinition);
            this.metrics.version          = await this.metrics.version.save();
            this.metrics.metricsVersionId = this.metrics.version.model.sysId;

            await this.metrics.metricsDefinition.flush();

            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    public async load(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            await this.metrics.init(this.id, this.versionId, this.isLibrary, this.isCopy, comp.app);
            return true;
        }
        catch (e)
        {
            return false;
        }
    }
}

export class MetricsData
{
    metricsId: string;
    metricsVersionId: string;

    metricsDefinition: Metrics.MetricsDefinitionExtended;
    app: AppContext;

    details: MetricsDefinitionDetailsExtended;
    version: MetricsDefinitionVersionExtended;

    selectedTemplateId: string;

    initialized: Future<void>;

    async init(metricsId: string,
               metricsVersionId: string,
               isLibrary: boolean,
               copy: boolean,
               app: AppContext)
    {
        if (!this.initialized)
        {
            this.initialized = new Future<void>();
        }
        else
        {
            return this.initialized;
        }

        try
        {
            this.metricsId          = metricsId;
            this.metricsVersionId   = metricsVersionId;
            this.app                = app;
            this.selectedTemplateId = null;

            if (this.metricsId && this.metricsId !== "new")
            {
                this.metricsDefinition = await app.domain.metricsDefinitions.getExtendedById(this.metricsId);
                let version: MetricsDefinitionVersionExtended;
                if (this.metricsVersionId)
                {
                    version = await app.domain.metricsDefinitionVersions.getExtendedById(this.metricsVersionId);
                }
                else
                {
                    version = await this.metricsDefinition.getHead();
                }
                this.version = version.getNewVersion();

                if (copy)
                {
                    this.version.model.predecessor     = null;
                    this.metricsDefinition.model.sysId = null;
                    this.metricsDefinition.model.title = `Copy of ${this.metricsDefinition.model.title}`;
                }
            }
            else
            {
                this.metricsDefinition = app.domain.metricsDefinitions.allocateInstance();

                this.version               = app.domain.metricsDefinitionVersions.allocateInstance();
                this.version.model.details = new Models.MetricsDefinitionDetailsForUserProgram();
            }

            await this.initDetails();
        }
        finally
        {
            this.initialized.resolve();
        }
    }

    async initFromTemplate(template: MetricsDefinitionExtended)
    {
        this.version = this.app.domain.metricsDefinitionVersions.allocateInstance();

        if (!template)
        {
            this.version.model.details = new Models.MetricsDefinitionDetailsForUserProgram();
        }
        else
        {
            let release                = await template.getRelease();
            this.version.model.details = release.model.details;
        }

        await this.initDetails();
    }

    get userProgramDetails(): Metrics.MetricsDefinitionDetailsForUserProgramExtended
    {
        return <Metrics.MetricsDefinitionDetailsForUserProgramExtended>this.details;
    }

    async initDetails()
    {
        this.details = await this.version.getDetailsExtended();
    }
}
