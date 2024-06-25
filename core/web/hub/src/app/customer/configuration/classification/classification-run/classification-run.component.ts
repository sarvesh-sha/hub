import {ChangeDetectionStrategy, Component, Injector, Input, ViewChild} from "@angular/core";
import {ClassificationDetailPageComponent, formatEquipment} from "app/customer/configuration/classification/classification-detail-page.component";

import {ClassificationOverrideDialogComponent, ClassificationOverrideDialogConfig} from "app/customer/configuration/classification/classification-override-dialog.component";

import {ApiService} from "app/services/domain/api.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {ControlPointPreviewComponent} from "app/shared/assets/chart-preview/control-point-preview.component";
import {PreviewInvokerComponent} from "app/shared/utils/preview-invoker/preview-invoker.component";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {ColumnConfiguration, DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {Unsubscribable} from "rxjs";


@Component({
               selector       : "o3-classification-run",
               styleUrls      : ["../dialog.scss"],
               templateUrl    : "./classification-run.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ClassificationRunComponent extends BaseApplicationComponent implements IDatatableDataProvider<ClassifiedPointGroup, ClassifiedPointGroup, ClassifiedPointGroup>
{
    private m_data: NormalizationDialogConfig;

    viewOptionsConfig = OverlayConfig.dropdown({width: 350});

    public table: DatatableManager<ClassifiedPointGroup, ClassifiedPointGroup, ClassifiedPointGroup>;

    private previews: Map<string, OverlayDialogRef<any>> = new Map<string, OverlayDialogRef<any>>();
    private toggleSubject: Unsubscribable;

    @ViewChild("validationOverlay")
    public validationOverlay: StandardFormOverlayComponent;

    public validationConfig = OverlayConfig.dialog({width: "90vw"});

    constructor(public host: ClassificationDetailPageComponent,
                public apis: ApiService,
                inj: Injector)
    {
        super(inj);

        this.table = new DatatableManager<ClassifiedPointGroup, ClassifiedPointGroup, ClassifiedPointGroup>(this, () => this.getViewState());
        this.table.enableSimpleExpansion((g) => g.key, (g) => g.key, true, false);
    }

    public ngOnInit()
    {
        super.ngOnInit();

        // Listen for any and all toggle events
        this.toggleSubject = PreviewInvokerComponent.onToggle.subscribe((id: string) =>
                                                                        {
                                                                            // Attempt to toggle the preview
                                                                            let preview = ControlPointPreviewComponent.toggle(this, id, false);

                                                                            // Check if it opened or close (null when closed)
                                                                            if (preview)
                                                                            {
                                                                                // Track the preview
                                                                                this.previews.set(id, preview);

                                                                                // If the preview is closed externally, stop tacking it
                                                                                preview.afterClose()
                                                                                       .subscribe(() =>
                                                                                                  {
                                                                                                      this.previews.delete(id);
                                                                                                  });
                                                                            }
                                                                            else
                                                                            {
                                                                                // Stop tracking the preview if we still were
                                                                                this.previews.delete(id);
                                                                            }
                                                                        });
    }

    ngOnDestroy()
    {
        // Stop listening to toggle events
        this.toggleSubject.unsubscribe();

        // Close all previews
        for (let preview of this.previews.values())
        {
            preview.close();
        }
    }

    @Input()
    public set data(data: NormalizationDialogConfig)
    {
        this.m_data = data;
        if (this.m_data)
        {
            if (this.m_data.changes)
            {
                this.refreshVisibility();
            }
            else if (this.m_data.handle)
            {
                this.refresh();
            }
        }
    }

    public get data(): NormalizationDialogConfig
    {
        return this.m_data;
    }

    async start()
    {
        let run = Models.DeviceElementNormalizationRun.newInstance({
                                                                       rules: this.host.rules
                                                                   });

        this.data.handle       = await this.apis.normalization.startNormalization(run, true);
        this.data.processing   = true;
        this.data.refreshDelay = 1000;
        this.data.changes      = null;
        this.data.unknownWords = null;

        const configs = await this.getColumnConfigs();
        this.extractColumns(configs);

        this.refresh();
    }

    validate()
    {
        this.validationOverlay.openOverlay();
    }

    async refresh()
    {
        if (!this.data.handle) return;

        let progress = await this.apis.normalization.checkNormalization(this.data.handle, false);
        if (progress)
        {
            this.data.devicesToProcess = progress.devicesToProcess;

            this.data.devicesProcessed  = progress.devicesProcessed;
            this.data.elementsProcessed = progress.elementsProcessed;
            this.markForCheck();

            switch (progress.status)
            {
                case Models.BackgroundActivityStatus.COMPLETED:
                case Models.BackgroundActivityStatus.FAILED:
                case Models.BackgroundActivityStatus.CANCELLED:
                    progress                         = await this.apis.normalization.checkNormalization(this.data.handle, true);
                    this.data.changes                = progress.details;
                    this.data.unknownWords           = progress.allUnknownWords;
                    this.data.equipmentRelationships = progress.equipmentRelationships;
                    this.data.equipments             = progress.equipments;
                    this.data.workflowOverrides      = progress.workflowOverrides;
                    this.data.onlyChangedEntries     = true;
                    this.data.processing             = false;

                    this.refreshVisibility();
                    break;

                default:
                    this.data.refreshDelay = Math.min(this.data.refreshDelay + 1000, 10000);

                    setTimeout(() => this.refresh(), this.data.refreshDelay);
                    break;
            }
        }
    }

    refreshVisibility()
    {
        if (!this.data.changes)
        {
            return;
        }

        let changes: Lookup<ClassifiedPointGroup> = {};
        let rows: ClassifiedPointGroup[]                     = [];

        this.data.rows = [];

        for (let detail of this.data.changes)
        {
            const output = new ClassifiedPoint(detail, this.data.workflowOverrides, this.host);
            this.data.rows.push(output);

            if (!output.isAboveThreshold && this.data.onlyAboveThreshold) continue;
            if (output.isAboveThreshold && this.data.onlyBelowThreshold) continue;
            if (!output.previouslyUnclassified && this.data.onlyPreviouslyUnclassified) continue;
            if (!output.hasChangedScore && this.data.onlyChangedScores) continue;
            if (!output.hasChangedClass && this.data.onlyChangedClasses) continue;
            if (!output.hasChanged && this.data.onlyChangedEntries) continue;
            if (!output.hasChangedSampling && this.data.onlyChangedSampling) continue;
            if (output.isIgnoredPointClass && this.data.hideIgnored) continue;

            let groupBys = this.data.groupBys;

            let groupKey = groupBys.map((g) => getProperty(output, g))
                                   .join("#");

            if (!groupKey)
            {
                continue;
            }

            let group: ClassifiedPointGroup = changes[groupKey];
            if (!group)
            {
                group             = new ClassifiedPointGroup(groupKey);
                changes[groupKey] = group;

                for (let groupBy of groupBys)
                {
                    setProperty(group, groupBy, getProperty(output, groupBy));
                }

                rows.push(changes[groupKey]);
            }

            group.rows.push(output);
        }

        this.data.groupedRows = rows;

        this.table.refreshData();
        this.markForCheck();
    }

    async debug(row: ClassifiedPoint)
    {
        this.host.sample = await this.apis.normalization.loadSample(row.details.sysId);
        this.host.editLogic();
        await this.host.testLogic();
    }

    async apply()
    {
        let run = Models.DeviceElementNormalizationRun.newInstance({
                                                                       rules: this.host.rules
                                                                   });

        this.data.handle       = await this.apis.normalization.startNormalization(run, false);
        this.data.changes      = null;
        this.data.processing   = true;
        this.data.refreshDelay = 1000;

        this.refresh();
    }

    async override(row: ClassifiedPointGroup,
                   result: ClassifiedPoint)
    {
        let cfg        = new ClassificationOverrideDialogConfig();
        cfg.sysId      = result.details.sysId;
        cfg.overrides  = result.overrides;
        cfg.rules      = this.host.rules;
        const accepted = await ClassificationOverrideDialogComponent.open(this, cfg);

        if (accepted)
        {
            result.overrides = cfg.overrides;

            row.rows = [...row.rows];
            this.markForCheck();
        }
    }

    public getItemName(): string
    {
        return "Results";
    }

    public async getList(): Promise<ClassifiedPointGroup[]>
    {
        const [sort] = this.table.sort || [];
        let rows     = this.data.groupedRows || [];

        if (sort?.prop)
        {
            const prop      = <keyof ClassifiedPointGroup>sort.prop;
            const ascending = sort.dir === "asc";
            rows.sort((a,
                       b) =>
                      {
                          const valA = getProperty(a, prop);
                          const valB = getProperty(b, prop);
                          if (typeof valA === "string" && typeof valB === "string")
                          {
                              return UtilsService.compareStrings(valA, valB, ascending);
                          }
                          else if (typeof valA === "number" && typeof valB === "number")
                          {
                              return UtilsService.compareNumbers(valA, valB, ascending);
                          }

                          return 0;
                      });
        }

        if (this.data.filter)
        {
            const filter = this.data.filter.toLocaleLowerCase();
            rows         = rows.filter((r) =>
                                       {
                                           const serialized = this.data.groupBys.map((g) => getProperty(r, g))
                                                                  .join("")
                                                                  .toLocaleLowerCase();
                                           return this.matchFilter(filter, serialized);
                                       });
        }

        return rows;
    }

    public async getPage(offset: number,
                         limit: number): Promise<ClassifiedPointGroup[]>
    {
        const rows = await this.getList();
        return rows.slice(offset * limit, (offset + 1) * limit);
    }

    public itemClicked(columnId: string,
                       item: ClassifiedPointGroup): void
    {
    }

    public async transform(rows: ClassifiedPointGroup[]): Promise<ClassifiedPointGroup[]>
    {
        return rows;
    }

    public async setColumnConfigs(configs: ColumnConfiguration[]): Promise<boolean>
    {
        this.extractColumns(configs);
        this.refreshVisibility();
        return super.setColumnConfigs(configs);
    }

    public getTableConfigId(): string
    {
        return "classificationResult";
    }

    private extractColumns(configs: ColumnConfiguration[])
    {
        if (!configs) return;

        this.data.groupBys = configs.filter((c) => c.enabled)
                                    .map((c) => <PointKey>c.id);
    }

    public hasColumnGrouped(column: PointKey): boolean
    {
        return this.data.groupBys.indexOf(column) >= 0;
    }
}

export class NormalizationDialogConfig
{
    public onlyChangedEntries: boolean;

    public onlyAboveThreshold: boolean;

    public onlyBelowThreshold: boolean;

    public onlyChangedClasses: boolean;

    public onlyChangedScores: boolean;

    public onlyPreviouslyUnclassified: boolean;

    public onlyChangedSampling: boolean;

    public hideIgnored: boolean;

    public processing: boolean;

    public refreshDelay: number;

    //--//

    public handle: string;

    public devicesToProcess: number;

    public devicesProcessed: number;

    public elementsProcessed: number;

    public changes: Models.ClassificationPointOutput[];

    public rows: ClassifiedPoint[];

    public groupedRows: ClassifiedPointGroup[];

    public groupBys: PointKey[] = [
        "originalName",
        "name",
        "pointClass"
    ];

    public filter: string = "";

    public unknownWords: Lookup<number>;

    public equipmentRelationships: Lookup<string[]>;
    public equipments: Lookup<Models.NormalizationEquipment>;

    public workflowOverrides: Models.WorkflowOverrides;
}

export class ClassifiedPoint
{
    private readonly m_hasChangedName: boolean;

    private readonly m_oldPc: Models.PointClass;

    private readonly m_newPc: Models.PointClass;

    private readonly m_workflowOverrideName: string;

    private m_overrides: Models.DeviceElementClassificationOverrides;

    constructor(public readonly details: Models.ClassificationPointOutput,
                workflowOverrides: Models.WorkflowOverrides,
                host: ClassificationDetailPageComponent)
    {
        this.m_hasChangedName = this.oldName.toLocaleLowerCase() !== this.newName.toLocaleLowerCase();

        this.m_workflowOverrideName = workflowOverrides.pointNames[details.sysId];

        this.m_overrides = host.rules.pointOverrides[details.sysId];

        let newPc    = host.getPointClass(details.currentResult.id);
        this.m_newPc = newPc && newPc.aliasPointClassId ? host.getPointClass(newPc.aliasPointClassId) : newPc;

        let oldPc    = host.getPointClass(details.lastResult.id);
        this.m_oldPc = oldPc && oldPc.aliasPointClassId ? host.getPointClass(oldPc.aliasPointClassId) : oldPc;
    }

    get url(): string
    {
        return `/#/devices/device/${this.details.parentSysId}/element/${this.details.sysId}`;
    }

    get overrides(): Models.DeviceElementClassificationOverrides
    {
        return this.m_overrides;
    }

    set overrides(overrides: Models.DeviceElementClassificationOverrides)
    {
        this.m_overrides = overrides;
    }

    get workflowOverrideName(): string
    {
        return this.m_workflowOverrideName || "";
    }

    get originalName(): string
    {
        return this.workflowOverrideName || this.details.details.objectBackupName || this.details.details.objectName || "";
    }

    get name(): string
    {
        return this.format(this.newName, this.oldName, this.hasChangedName);
    }

    get newName(): string
    {
        return this.details.normalizedName || "";
    }

    get oldName(): string
    {
        return this.details.oldNormalizedName || "";
    }

    get overrideName(): string
    {
        if (this.workflowOverrideName)
        {
            return `${this.workflowOverrideName} (Workflow)`;
        }
        else if (this.overrides?.pointName)
        {
            return `${this.overrides.pointName} (Rules)`;
        }

        return "";
    }

    get hasChangedName(): boolean
    {
        return this.m_hasChangedName;
    }

    get deviceId(): string
    {
        return this.details.details.controllerIdentifier;
    }

    get pointId(): string
    {
        return this.details.details.objectIdentifier;
    }

    get rawPointClass(): Models.PointClass
    {
        return this.m_newPc;
    }

    get pointClass(): string
    {
        return this.format(this.newPointClass, this.oldPointClass, this.hasChangedClass || this.hasChangedScore);
    }

    get newPointClass(): string
    {
        if (this.m_newPc)
        {
            return `${this.m_newPc.pointClassName} - ${this.m_newPc.pointClassDescription} - ${this.currentScore.toFixed(3)}`;
        }
        return "Unclassified";
    }

    get oldPointClass(): string
    {
        if (this.m_oldPc)
        {
            return `${this.m_oldPc.pointClassName} - ${this.m_oldPc.pointClassDescription} - ${this.lastScore.toFixed(3)}`;
        }
        return "Unclassified";
    }

    get units(): string
    {
        return this.format(this.newUnits, this.oldUnits, this.hasChangedUnits);
    }

    get newUnits(): string
    {
        const units = this.details.currentResult.assignedUnits || this.details.details.objectUnits;
        return `${units}`;
    }

    get oldUnits(): string
    {
        const units = this.details.lastResult.assignedUnits || this.details.details.objectUnits;
        return `${units}`;
    }

    get hasChangedUnits(): boolean
    {
        return this.details.currentResult.assignedUnits !== this.details.lastResult.assignedUnits;
    }

    get sampling(): string
    {
        return this.format(this.newSampling, this.oldSampling, this.hasChangedSampling);
    }

    get newSampling(): string
    {
        return this.details.currentResult.noSampling ? "None" : `${this.details.currentResult.samplingPeriod} seconds`;
    }

    get oldSampling(): string
    {
        return this.details.lastResult.noSampling ? "None" : `${this.details.lastResult.samplingPeriod} seconds`;
    }

    get tags(): string
    {
        if (this.details.normalizationTags)
        {
            return `${this.details.normalizationTags.join(", ")}`;
        }

        return "";
    }

    get adtModel(): string
    {
        return this.format(this.newAdtModel, this.oldAdtModel, this.hasChangedAdtModel);
    }

    get newAdtModel(): string
    {
        return this.details.currentResult.azureDigitalTwinModel;
    }

    get oldAdtModel(): string
    {
        return this.details.lastResult.azureDigitalTwinModel;
    }

    get currentScore(): number
    {
        return this.details.currentResult.negativeScore + this.details.currentResult.positiveScore;
    }

    get lastScore(): number
    {
        return this.details.lastResult.negativeScore + this.details.lastResult.positiveScore;
    }

    get previouslyUnclassified(): boolean
    {
        return !this.details.lastResult.id;
    }

    get hasChanged(): boolean
    {
        return this.hasChangedScore || this.hasChangedClass || this.hasChangedName || this.hasChangedAdtModel || this.hasChangedUnits;
    }

    get hasChangedScore(): boolean
    {
        return (this.previouslyUnclassified && !!this.m_newPc) || (!this.previouslyUnclassified && !this.m_newPc) || this.m_newPc && this.lastScore !== this.currentScore;
    }

    get hasChangedClass(): boolean
    {
        return (this.previouslyUnclassified && !!this.m_newPc) || (!this.previouslyUnclassified && !this.m_newPc) || this.m_newPc && this.m_newPc.id !== this.m_oldPc?.id;
    }

    get isAboveThreshold(): boolean
    {
        return this.currentScore > this.details.currentResult.threshold;
    }

    get isIgnoredPointClass(): boolean
    {
        return this.m_newPc?.ignorePointIfMatched;
    }

    get hasChangedSampling(): boolean
    {
        return this.details.currentResult.noSampling != this.details.lastResult.noSampling || this.details.currentResult.samplingPeriod !== this.details.lastResult.samplingPeriod;
    }

    get hasChangedAdtModel(): boolean
    {
        return this.details.currentResult.azureDigitalTwinModel !== this.details.lastResult.azureDigitalTwinModel;
    }

    get equipment(): string
    {
        const formatted = formatEquipment(this.details?.equipments, this.details?.equipmentRelationships);
        return formatted.join("\n");
    }

    get equipments(): Models.NormalizationEquipment[]
    {
        if (this.details?.equipments)
        {
            return Object.keys(this.details.equipments)
                         .map((key) => this.details.equipments[key]);
        }

        return [];
    }

    private format(current: string,
                   last: string,
                   changed: boolean): string
    {
        let result = current;
        if (changed)
        {
            result += ` (Previous: ${last})`;
        }

        return result;
    }

}

export class ClassifiedPointGroup
{
    public rows: ClassifiedPoint[] = [];

    public originalName: string;
    public name: string;
    public pointClass: string;
    public sampling: string;
    public deviceId: string;
    public pointId: string;
    public equipment: string;
    public tags: string;
    public adtModel: string;
    public units: string;

    public get numPoints(): number
    {
        return this.rows?.length;
    }

    public set numPoints(numPoints: number)
    {
    }

    constructor(public key: string)
    {}
}

function getProperty<T, K extends keyof T>(obj: T,
                                           key: K)
{
    return obj[key];
}

function setProperty<T, K extends keyof T>(obj: T,
                                           key: K,
                                           value: T[K])
{
    obj[key] = value;
}

type PointKey = keyof (ClassifiedPointGroup | ClassifiedPoint);
