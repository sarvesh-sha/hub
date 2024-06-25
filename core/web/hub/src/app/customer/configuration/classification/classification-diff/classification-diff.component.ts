import {ChangeDetectionStrategy, Component, Inject, Injector, Input} from "@angular/core";
import {DomSanitizer, SafeHtml} from "@angular/platform-browser";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {NormalizationExtended} from "app/services/domain/normalization.service";

import * as Models from "app/services/proxy/model/models";
import {Change, Lookup, UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";
import {DatatableSort} from "framework/ui/datatables/datatable-manager";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               template       : `
                   <o3-standard-form label="Pick versions" primaryButtonText="Compare" (submitted)="submit()" (cancelled)="dialogRef.close()" [primaryButtonDisabled]="!pickedVersion">
                       <mat-form-field>
                           <o3-select placeholder="Saved Version" [multiSelect]="false" singleClick [disabled]="!versions" [options]="versions" [(ngModel)]="pickedVersion" required></o3-select>
                       </mat-form-field>
                   </o3-standard-form>`,
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ClassificationDiffPickerDialogComponent extends BaseApplicationComponent
{
    versions: ControlOption<NormalizationExtended>[];

    pickedVersion: NormalizationExtended;

    public static open(comp: BaseComponent,
                       rules: Models.NormalizationRules): Promise<void>
    {
        let overlayCfg = OverlayConfig.dialog();

        return OverlayComponent.open(comp, ClassificationDiffPickerDialogComponent, {
            data  : {rules: rules},
            config: overlayCfg
        });
    }

    constructor(inj: Injector,
                public dialogRef: OverlayDialogRef<void>,
                @Inject(OVERLAY_DATA) public data: DiffPickerConfig)
    {
        super(inj);
    }

    async ngOnInit()
    {
        let versions  = await this.app.domain.normalization.getExtendedAll();
        this.versions = versions.map((v) => new ControlOption<NormalizationExtended>(v, "v" + v.model.version));
        this.markForCheck();
    }

    submit()
    {
        this.dialogRef.close();
        ClassificationDiffComponent.open(this, this.data.rules, "Current", this.pickedVersion.model.rules, "v" + this.pickedVersion.model.version);
    }
}

interface DiffPickerConfig
{
    rules: Models.NormalizationRules
}

@Component({
               selector   : "o3-classification-diff",
               templateUrl: "./classification-diff.component.html"
           })
export class ClassificationDiffComponent
{
    public readonly a: Models.NormalizationRules;

    public readonly b: Models.NormalizationRules;

    public readonly titleA: string;

    public readonly titleB: string;

    disableAnimations = true;

    abbreviationRows: FormattedDiff[]    = [];
    disambiguationsRows: FormattedDiff[] = [];
    startsWithRows: FormattedDiff[]      = [];
    endsWithRows: FormattedDiff[]        = [];
    containsRows: FormattedDiff[]        = [];
    termScoringRows: FormattedDiff[]     = [];
    pointClassRows: FormattedDiff[]      = [];
    equipmentClassRows: FormattedDiff[]  = [];
    locationClassRows: FormattedDiff[]   = [];

    public static open(comp: BaseComponent,
                       a: Models.NormalizationRules,
                       titleA: string,
                       b: Models.NormalizationRules,
                       titleB: string): Promise<void>
    {
        let dialogCfg  = new DiffConfig(a, titleA, b, titleB);
        let overlayCfg = OverlayConfig.onTopDraggable({width: "90vw"});

        return OverlayComponent.open(comp, ClassificationDiffComponent, {
            data  : dialogCfg,
            config: overlayCfg
        });
    }

    constructor(public dialogRef: OverlayDialogRef<void>,
                private sanitizer: DomSanitizer,
                @Inject(OVERLAY_DATA) public data: DiffConfig)
    {
        this.a      = data.a;
        this.b      = data.b;
        this.titleA = data.titleA;
        this.titleB = data.titleB;
    }

    public ngAfterViewInit()
    {
        // HACK: https://github.com/angular/components/issues/13870#issuecomment-502071712
        setTimeout(() => this.disableAnimations = false);
    }

    ngOnInit()
    {
        this.abbreviationRows = this.getDiffs(this.a.abbreviations, this.b.abbreviations, (val) => [...val], (val) => val.join(", "));

        this.disambiguationsRows = this.getDiffs(this.a.disambiguations, this.b.disambiguations, (data) => data, (data) => data);
        this.startsWithRows      = this.getDiffs(this.a.startsWith, this.b.startsWith, (data) => data, (data) => data);
        this.endsWithRows        = this.getDiffs(this.a.endsWith, this.b.endsWith, (data) => data, (data) => data);
        this.containsRows        = this.getDiffs(this.a.contains, this.b.contains, (data) => data, (data) => data);
        this.termScoringRows     = this.getDiffs(this.a.knownTerms, this.b.knownTerms, (data) =>
                                                 {
                                                     return {
                                                         "positiveWeight": `+${(data.positiveWeight || 1).toFixed(1)}`,
                                                         "negativeWeight": `-${(data.negativeWeight || 0).toFixed(1)}`,
                                                         "acronym"       : data.acronym || "",
                                                         "synonyms"      : data.synonyms?.join(", ") || ""
                                                     };
                                                 },
                                                 (data) =>
                                                 {
                                                     let result: string[] = [`Weights: ${data.positiveWeight}/${data.negativeWeight}`];
                                                     if (data.acronym)
                                                     {
                                                         result.push(`Acronym: ${data.acronym}`);
                                                     }

                                                     if (data.synonyms)
                                                     {
                                                         result.push(`Synonyms: ${data.synonyms}`);
                                                     }

                                                     return result.join(" | ");
                                                 });

        this.pointClassRows = this.getDiffs(UtilsService.extractMappedLookup(this.a.pointClasses, (a) => a),
                                            UtilsService.extractMappedLookup(this.b.pointClasses, (a) => a),
                                            (pc) =>
                                            {
                                                return {
                                                    "pointClassName"       : pc.pointClassName,
                                                    "pointClassDescription": pc.pointClassDescription,
                                                    "type"                 : pc.type,
                                                    "tags"                 : pc.tags?.join(",") || "",
                                                    "azureDigitalTwin"     : pc.azureDigitalTwin,
                                                    "aliasPointClassId"    : pc.aliasPointClassId
                                                };
                                            },
                                            (pc) =>
                                            {
                                                let result: string[] = [`${pc.pointClassName} - ${pc.pointClassDescription}`];

                                                if (pc.type)
                                                {
                                                    result.push(`Type: ${pc.type}`);
                                                }

                                                if (pc.tags)
                                                {
                                                    result.push(`Tags: ${pc.tags}`);
                                                }

                                                if (pc.azureDigitalTwin)
                                                {
                                                    result.push(`ADT: ${pc.azureDigitalTwin}`);
                                                }

                                                if (pc.aliasPointClassId)
                                                {
                                                    result.push(`Alias: ${pc.aliasPointClassId}`);
                                                }

                                                return result.join(" | ");
                                            });

        this.equipmentClassRows = this.getDiffs(UtilsService.extractMappedLookup(this.a.equipmentClasses, (a) => a),
                                                UtilsService.extractMappedLookup(this.b.equipmentClasses, (a) => a),
                                                (ec) =>
                                                {
                                                    return {
                                                        "equipClassName"  : ec.equipClassName,
                                                        "description"     : ec.description,
                                                        "tags"            : ec.tags?.join(",") || "",
                                                        "azureDigitalTwin": ec.azureDigitalTwin
                                                    };
                                                },
                                                (ec) =>
                                                {
                                                    let result: string[] = [`${ec.equipClassName} - ${ec.description}`];

                                                    if (ec.tags)
                                                    {
                                                        result.push(`Tags: ${ec.tags}`);
                                                    }

                                                    if (ec.azureDigitalTwin)
                                                    {
                                                        result.push(`ADT: ${ec.azureDigitalTwin}`);
                                                    }

                                                    return result.join(" | ");
                                                });

        this.locationClassRows = this.getDiffs(UtilsService.extractMappedLookup(this.a.locationClasses, (a) => a),
                                               UtilsService.extractMappedLookup(this.b.locationClasses, (a) => a),
                                               (lc) =>
                                               {
                                                   return {
                                                       "description"     : lc.description,
                                                       "tags"            : lc.tags?.join(",") || "",
                                                       "azureDigitalTwin": lc.azureDigitalTwin
                                                   };
                                               },
                                               (lc) =>
                                               {
                                                   let result: string[] = [`${lc.description}`];

                                                   if (lc.tags)
                                                   {
                                                       result.push(`Tags: ${lc.tags}`);
                                                   }

                                                   if (lc.azureDigitalTwin)
                                                   {
                                                       result.push(`ADT: ${lc.azureDigitalTwin}`);
                                                   }

                                                   return result.join(" | ");
                                               });
    }

    private formatMissing(text: string): string
    {
        return this.formatSpan(text, "#fc7465");
    }

    private formatAdded(text: string): string
    {
        return this.formatSpan(text, "#65b352");
    }

    private formatChange(text: string): string
    {
        return this.formatSpan(text, "#fffa99");
    }

    private formatSpan(text: string,
                       color: string): string
    {
        if (!text) return "";
        return `<span style="background: ${color}">${text}</span>`;
    }

    private getDiffs<T>(a: Lookup<T>,
                        b: Lookup<T>,
                        getProperties: (d: T) => { [key in keyof T]?: string },
                        formatProperties: (d: { [key in keyof T]?: string }) => string)
    {
        let changes         = UtilsService.diffJson(a, b);
        let prevKey: string = null;
        let diff: Diff<T>   = null;
        let rows: Diff<T>[] = [];
        for (let change of changes)
        {
            let key = change.path.shift();

            if (key == prevKey)
            {
                diff.changes.push(change);
                continue;
            }

            diff = new Diff(key, a[key], b[key]);
            diff.changes.push(change);
            rows.push(diff);

            prevKey = key;
        }

        return rows.map((diff) =>
                        {
                            let aHtml      = ``;
                            let bHtml      = ``;
                            let propertiesA = diff.a ? getProperties(diff.a) : null;
                            let propertiesB = diff.b ? getProperties(diff.b) : null;

                            if (!diff.a)
                            {
                                aHtml = this.formatMissing("&lt;missing&gt;");
                                bHtml = this.formatAdded(formatProperties(propertiesB));
                            }
                            else if (!diff.b)
                            {
                                aHtml = this.formatAdded(formatProperties(propertiesA));
                                bHtml = this.formatMissing("&lt;missing&gt;");
                            }
                            else
                            {
                                for (let change of diff.changes)
                                {
                                    let prop: keyof T = <any>change.path.shift();
                                    let valA          = propertiesA[prop];
                                    let valB          = propertiesB[prop];
                                    if (!valA && !valB) continue;

                                    if (!valA)
                                    {
                                        propertiesB[prop] = this.formatAdded(valB);
                                    }
                                    else if (!valB)
                                    {
                                        propertiesA[prop] = this.formatAdded(valA);
                                    }
                                    else
                                    {
                                        propertiesA[prop] = this.formatChange(valA);
                                        propertiesB[prop] = this.formatChange(valB);
                                    }
                                }

                                aHtml = formatProperties(propertiesA);
                                bHtml = formatProperties(propertiesB);
                            }

                            return new FormattedDiff(diff.key, this.sanitizer.bypassSecurityTrustHtml(aHtml), this.sanitizer.bypassSecurityTrustHtml(bHtml));
                        });
    }
}

@Component({
               selector: "o3-classification-diff-table",
               styles  : [
                   ".missing { background: red; }",
                   ".added { background: green; }",
                   ".change {background: yellow }"
               ],
               template: `
                   <o3-datatable [rows]="rows" [showRowNumbers]="false" (sort)="sort($event)">
                       <o3-datatable-column id="key" [name]="titleKey" sortId="key" prop="key"></o3-datatable-column>
                       <o3-datatable-column id="a" [name]="titleA" prop="a" [grow]="2">
                           <ng-template o3-datatable-cell-template let-value="value">
                               <span [innerHTML]="value"></span>
                           </ng-template>
                       </o3-datatable-column>
                       <o3-datatable-column id="b" [name]="titleB" prop="b" [grow]="2">
                           <ng-template o3-datatable-cell-template let-value="value">
                               <span [innerHTML]="value"></span>
                           </ng-template>
                       </o3-datatable-column>
                   </o3-datatable>
               `
           })
export class ClassificationDiffTableComponent
{
    @Input() rows: FormattedDiff[];

    @Input() titleKey: string;

    @Input() titleA: string;

    @Input() titleB: string;

    public sort(event: DatatableSort[])
    {
        let [sort] = event;
        if (!sort) return;
        const ascending = sort.dir === "asc";
        this.rows       = [
            ...this.rows.sort((a,
                               b) =>
                              {
                                  switch (sort.prop)
                                  {
                                      case "key":
                                          return UtilsService.compareStrings(a.key, b.key, ascending);
                                  }
                                  return 0;
                              })
        ];
    }

}

class Diff<T>
{
    public readonly changes: Change[] = [];

    constructor(public readonly key: string,
                public readonly a: T,
                public readonly b: T)
    {}
}

class FormattedDiff
{
    constructor(public readonly key: string,
                public readonly a: SafeHtml,
                public readonly b: SafeHtml)
    {}
}

class DiffConfig
{
    constructor(public readonly a: Models.NormalizationRules,
                public readonly titleA: string,
                public readonly b: Models.NormalizationRules,
                public readonly titleB: string)
    {}
}

