import {Component, ContentChild, ContentChildren, Directive, Input, QueryList, TemplateRef, ViewChild} from "@angular/core";
import {AppContext} from "app/app.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {DatatableColumnDirective, DatatableComponent, DatatableRowActivateEvent} from "framework/ui/datatables/datatable.component";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";

@Directive()
export abstract class NetworkDetailEditorBase<T>
{
    @Input() rows: T[];

    @Input() readonly: boolean;

    public abstract newInstance(model: T): T;
}

@Component({
               selector : "o3-network-detail-list-editor",
               styleUrls: ["./network-detail-editor.component.scss"],
               template : `
                   <o3-collapsible-filter-button *ngIf="!hideFilter" [(model)]="filter"></o3-collapsible-filter-button>
                   <o3-datatable [rows]="host.rows" [filter]="filter" [useExpandToggle]="false" [showRowNumbers]="false" [showViewIcons]="false" [clickableRows]="!host.readonly"
                                 (activate)="edit($event)">
                       <o3-datatable-column *ngFor="let col of columns" [id]="col.id" [sortId]="col.sortId" [width]="col.width" [name]="col.name" [prop]="col.prop"
                                            [shrink]="col.shrink" [grow]="col.grow"></o3-datatable-column>
                   </o3-datatable>

                   <div class="row mt-2" *ngIf="!host.readonly">
                       <div class="col-sm-1">
                           <button mat-raised-button type="button" (click)="add()">
                               Add {{name}}
                           </button>
                       </div>
                   </div>

                   <o3-standard-form-overlay #overlay label="Configure {{name}}" [overlayConfig]="overlayConfig" (submitted)="saveEdit()"
                                             extraButtonText="Remove {{name}}" [showExtraButton]="!!existing" (extraButtonPressed)="remove()"
                                             [confirmSecondaryButton]="!isPristine" [secondaryButtonText]="isPristine ? 'Close' : 'Cancel'" [showSecondary]="true"
                                             (cancelled)="cancelEdit()">
                       <ng-container *ngIf="model">
                           <ng-template [ngTemplateOutlet]="form" [ngTemplateOutletContext]="{$implicit: model}"></ng-template>
                       </ng-container>
                   </o3-standard-form-overlay>`
           })
export class NetworkDetailListEditorComponent extends BaseApplicationComponent
{
    @ViewChild(StandardFormOverlayComponent, {static: true}) overlay: StandardFormOverlayComponent;
    @ViewChild(DatatableComponent, {static: true}) table: DatatableComponent<any, any, any>;

    @ContentChildren(DatatableColumnDirective)
    public set columnDirectives(columns: QueryList<DatatableColumnDirective>)
    {
        this.columns = columns.toArray();
    }

    @ContentChild("form") form: TemplateRef<any>;

    @Input() host: NetworkDetailEditorBase<any>;

    @Input() name: string;

    model: any;

    existing: any;

    filter: string = "";
    @Input() hideFilter: boolean;

    overlayConfig = OverlayConfig.dialog({width: "500px"});

    columns: DatatableColumnDirective[] = [];

    public add(): void
    {
        if (this.host.readonly) return;
        this.model = this.host.newInstance({});
        this.overlay.openOverlay();
    }

    public edit(event: DatatableRowActivateEvent<any>): void
    {
        if (this.host.readonly) return;
        this.existing = event.row;
        this.model    = this.host.newInstance(this.existing);
        this.overlay.openOverlay();
    }

    public saveEdit(): void
    {
        if (this.existing)
        {
            Object.assign(this.existing, this.model);
        }
        else
        {
            this.host.rows.push(this.model);

            // Force refresh
            this.table.rows = this.host.rows;
        }

        this.existing = null;
        this.model    = null;
    }

    public cancelEdit(): void
    {
        this.existing = null;
        this.model    = null;
    }

    public async remove(): Promise<void>
    {
        if (!this.existing)
        {
            return;
        }

        if (!await this.confirmOperation("Are you sure you want to remove this row?"))
        {
            return;
        }

        this.host.rows.splice(this.host.rows.indexOf(this.existing), 1);

        // Force refresh
        this.table.rows = this.host.rows;

        this.existing = null;
        this.model    = null;
        this.overlay.closeOverlay();
    }

    public get isPristine(): boolean
    {
        if (!this.model) return true;
        if (!this.existing) return false;

        return UtilsService.compareJson(this.model, this.existing);
    }
}

@Component({
               selector: "o3-non-discoverable-device-table",
               template: `
                   <o3-network-detail-list-editor [host]="this" name="Device">
                       <o3-datatable-column id="networkId" sortId="networkId" name="Network ID" prop="networkNumber" [width]="125" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="instanceId" sortId="instanceId" name="Instance ID" prop="instanceNumber" [width]="125" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="mstpAddress" sortId="mstpAddress" name="MS/TP Address" prop="mstpAddress" [width]="125" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="ipAddress" sortId="ipAddress" name="IP Address" prop="networkAddress" [width]="150" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="port" sortId="port" name="Port" prop="networkPort" [width]="125" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="notes" sortId="notes" name="Notes" prop="notes" [grow]="3"></o3-datatable-column>

                       <ng-template #form let-model>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Network ID" [(ngModel)]="model.networkNumber" name="networkNumber" required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Instance ID" [(ngModel)]="model.instanceNumber" name="instanceNumber" required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="MS/TP address" [(ngModel)]="model.mstpAddress" name="mstpAddress">
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="text" placeholder="IP Address" [(ngModel)]="model.networkAddress" name="networkAddress" ipAddressRequired required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Port" [(ngModel)]="model.networkPort" name="networkPort" required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="text" placeholder="Notes" [(ngModel)]="model.notes" name="notes">
                           </mat-form-field>
                       </ng-template>
                   </o3-network-detail-list-editor>
               `
           })
export class NonDiscoverableDeviceTableComponent extends NetworkDetailEditorBase<Models.NonDiscoverableBACnetDevice>
{
    public newInstance(model: Models.NonDiscoverableBACnetDevice): Models.NonDiscoverableBACnetDevice
    {
        return Models.NonDiscoverableBACnetDevice.newInstance(model);
    }
}


@Component({
               selector: "o3-skipped-device-table",
               template: `
                   <o3-network-detail-list-editor [host]="this" name="Device">
                       <o3-datatable-column id="networkId" sortId="networkId" name="Network ID" prop="networkNumber" [width]="125" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="instanceId" sortId="instanceId" name="Instance ID" prop="instanceNumber" [width]="125" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="ipAddress" sortId="ipAddress" name="IP Address" prop="transportAddress" [width]="150" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="port" sortId="port" name="Port" prop="transportPort" [width]="125" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="notes" sortId="notes" name="Notes" prop="notes" [grow]="3"></o3-datatable-column>
                       <ng-template #form let-model>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Network ID" [(ngModel)]="model.networkNumber" name="networkNumber" required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Instance ID" [(ngModel)]="model.instanceNumber" name="instanceNumber" required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="text" placeholder="IP Address" [(ngModel)]="model.transportAddress" name="networkAddress" ipAddressRequired required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Port" [(ngModel)]="model.transportPort" name="networkPort" required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="text" placeholder="Notes" [(ngModel)]="model.notes" name="notes">
                           </mat-form-field>
                       </ng-template>
                   </o3-network-detail-list-editor>
               `
           })
export class SkippedDeviceTableComponent extends NetworkDetailEditorBase<Models.SkippedBACnetDevice>
{
    public newInstance(model: Models.SkippedBACnetDevice): Models.SkippedBACnetDevice
    {
        return Models.SkippedBACnetDevice.newInstance(model);
    }
}


@Component({
               selector: "o3-non-discoverable-mstp-table",
               template: `
                   <o3-network-detail-list-editor [host]="this" name="MS/TP Trunk">
                       <o3-datatable-column id="ipAddress" sortId="ipAddress" name="IP Address" prop="networkAddress" [width]="150" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="port" sortId="port" name="Port" prop="networkPort" [width]="125" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="networkId" sortId="networkId" name="Network ID" prop="networkNumber" [width]="125" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="notes" sortId="notes" name="Notes" prop="notes" [grow]="3"></o3-datatable-column>
                       <ng-template #form let-model>
                           <mat-form-field>
                               <input matInput type="text" placeholder="IP Address" [(ngModel)]="model.networkAddress" name="mstp_networkAddress" required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Port" [(ngModel)]="model.networkPort" name="mstp_networkPort" required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Network ID" [(ngModel)]="model.networkNumber" name="mstp_networkNumber" required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="text" placeholder="Notes" [(ngModel)]="model.notes" name="mstp_notes">
                           </mat-form-field>
                       </ng-template>
                   </o3-network-detail-list-editor>
               `
           })
export class NonDiscoverableMSTPTableComponent extends NetworkDetailEditorBase<Models.NonDiscoverableMstpTrunk>
{
    public newInstance(model: Models.NonDiscoverableMstpTrunk): Models.NonDiscoverableMstpTrunk
    {
        return Models.NonDiscoverableMstpTrunk.newInstance(model);
    }
}

@Component({
               selector: "o3-bacnet-bbmd-table",
               template: `
                   <o3-network-detail-list-editor [host]="this" name="BBMD">
                       <o3-datatable-column id="ipAddress" sortId="ipAddress" name="IP Address" prop="networkAddress" [width]="150" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="port" sortId="port" name="Port" prop="networkPort" [width]="125" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="notes" sortId="notes" name="Notes" prop="notes" [grow]="3"></o3-datatable-column>
                       <ng-template #form let-model>
                           <mat-form-field>
                               <input matInput type="text" placeholder="IP Address" [(ngModel)]="model.networkAddress" name="bbmd_networkAddress" required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Port" [(ngModel)]="model.networkPort" name="bbmd_networkPort" required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="text" placeholder="Notes" [(ngModel)]="model.notes" name="bbmd_notes">
                           </mat-form-field>
                       </ng-template>
                   </o3-network-detail-list-editor>
               `
           })
export class BACnetBBMDTableComponent extends NetworkDetailEditorBase<Models.BACnetBBMD>
{
    public newInstance(model: Models.BACnetBBMD): Models.BACnetBBMD
    {
        return Models.BACnetBBMD.newInstance(model);
    }
}


@Component({
               selector: "o3-bacnet-subnet-table",
               template: `
                   <o3-network-detail-list-editor [host]="this" name="Subnet">
                       <o3-datatable-column id="ipAddress" sortId="ipAddress" name="IP Address" prop="cidr" [width]="150" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="notes" sortId="notes" name="Notes" prop="notes" [grow]="3"></o3-datatable-column>
                       <ng-template #form let-model>
                           <mat-form-field>
                               <input matInput type="text" placeholder="IP Address" [(ngModel)]="model.cidr" name="cidr" required>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="text" placeholder="Notes" [(ngModel)]="model.notes" name="notes">
                           </mat-form-field>
                       </ng-template>
                   </o3-network-detail-list-editor>
               `
           })
export class BACnetSubnetTableComponent extends NetworkDetailEditorBase<Models.FilteredSubnet>
{
    public newInstance(model: Models.FilteredSubnet): Models.FilteredSubnet
    {
        return Models.FilteredSubnet.newInstance(model);
    }
}


@Component({
               selector: "o3-i2c-SHT30x-table",
               template: `
                   <o3-network-detail-list-editor [hideFilter]="true" [host]="this" name="Sensor">
                       <o3-datatable-column id="bus" name="I2C Bus" prop="model.bus" [width]="150" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="samplingPeriod" name="Sampling Period" prop="model.samplingPeriod" [width]="150" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="instanceSelector" name="Instance Selector" prop="model.instanceSelector" [grow]="3"></o3-datatable-column>
                       <o3-datatable-column id="equipmentClass" name="Equipment Class" prop="equipmentClassDisplay" [grow]="3"></o3-datatable-column>

                       <ng-template #form let-model>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Bus" [(ngModel)]="model.model.bus" name="bus">
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Sampling Period" [(ngModel)]="model.model.samplingPeriod" name="samplingPeriod">
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="text" placeholder="Instance Selector" [(ngModel)]="model.model.instanceSelector" name="instanceSelector">
                           </mat-form-field>
                           <mat-form-field>
                               <o3-select name="equipmentClass" [o3EquipmentClassSelector]="true" [(ngModel)]="model.equipmentClass" required></o3-select>
                           </mat-form-field>
                       </ng-template>
                   </o3-network-detail-list-editor>
               `
           })
export class I2C_SHT30x_TableComponent extends NetworkDetailEditorBase<I2CSensorSHT3xExtended>
{
    rules: Models.NormalizationRules;

    constructor(public app: AppContext)
    {
        super();

        this.app.bindings.getActiveNormalizationRules()
            .then((rules) => this.rules = rules);
    }

    public newInstance(ext: I2CSensorSHT3xExtended): I2CSensorSHT3xExtended
    {
        let model = Models.I2CSensorSHT3x.newInstance(ext.model);
        return new I2CSensorSHT3xExtended(this.rules, model);
    }
}

@Component({
               selector: "o3-i2c-MCP3428-table",
               template: `
                   <o3-network-detail-list-editor [hideFilter]="true" [host]="this" name="Sensor">
                       <o3-datatable-column id="samplingPeriod" name="Sampling Period" prop="model.samplingPeriod" [width]="150" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="instanceSelector" name="Instance Selector" prop="model.instanceSelector" [grow]="3"></o3-datatable-column>
                       <o3-datatable-column id="equipmentClass" name="Equipment Class" prop="equipmentClassDisplay" [grow]="3"></o3-datatable-column>
                       <o3-datatable-column id="channel" name="ADC Channel" prop="model.channel" [width]="150" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="gain" name="ADC Gain" prop="model.gain" [width]="150" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="conversionOffsetPre" name="Pre-conversion Offset" prop="model.conversionOffsetPre" [width]="200" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="conversionScale" name="Conversion (Volts per unit)" prop="model.conversionScale" [width]="250" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="conversionOffsetPost" name="Post-conversion Offset" prop="model.conversionOffsetPost" [width]="200" [grow]="0"></o3-datatable-column>
                       <o3-datatable-column id="PointClass" name="Point Class" prop="pointClassDisplay" [grow]="3"></o3-datatable-column>

                       <ng-template #form let-model>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Sampling Period" [(ngModel)]="model.model.samplingPeriod" name="samplingPeriod">
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="text" placeholder="Instance Selector" [(ngModel)]="model.model.instanceSelector" name="instanceSelector">
                           </mat-form-field>
                           <mat-form-field>
                               <o3-select name="equipmentClass" [o3EquipmentClassSelector]="true" [(ngModel)]="model.equipmentClass" required></o3-select>
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="ADC Channel" [(ngModel)]="model.model.channel" name="channel">
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="ADC Gain" [(ngModel)]="model.model.gain" name="gain">
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Pre-conversion offset" [(ngModel)]="model.model.conversionOffsetPre" name="conversionOffsetPre">
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Conversion (Volts per unit)" [(ngModel)]="model.model.conversionScale" name="conversionScale">
                           </mat-form-field>
                           <mat-form-field>
                               <input matInput type="number" placeholder="Post-conversion offset" [(ngModel)]="model.model.conversionOffsetPost" name="conversionOffsetPost">
                           </mat-form-field>
                           <mat-form-field>
                               <o3-select [o3PointClassSelector]="true" [rules]="model.rules" [multiSelect]="false" [(ngModel)]="model.pointClass"></o3-select>
                           </mat-form-field>
                       </ng-template>
                   </o3-network-detail-list-editor>
               `
           })
export class I2C_MCP3428_TableComponent extends NetworkDetailEditorBase<I2CSensorMCP3428Extended>
{
    rules: Models.NormalizationRules;

    constructor(public app: AppContext)
    {
        super();

        this.app.bindings.getActiveNormalizationRules()
            .then((rules) => this.rules = rules);
    }

    public newInstance(ext: I2CSensorMCP3428Extended): I2CSensorMCP3428Extended
    {
        let model = Models.I2CSensorMCP3428.newInstance(ext.model);
        if (model.gain == 0)
        {
            model.gain = 1;
        }

        if (model.conversionScale == 0)
        {
            model.conversionScale = 1;
        }

        return new I2CSensorMCP3428Extended(this.rules, model);
    }
}

export abstract class I2CSensorExtendedBase
{
    constructor(public readonly rules: Models.NormalizationRules)
    {
    }

    setEquipmentClass(id: number): Models.WellKnownEquipmentClassOrCustom
    {
        let ecMatch = this.rules?.equipmentClasses?.find((ec) => ec.id == id);
        if (ecMatch)
        {
            let ec = new Models.WellKnownEquipmentClassOrCustom();
            if (ecMatch.wellKnown)
            {
                ec.known = ecMatch.wellKnown;
            }
            else
            {
                ec.custom = ecMatch.id;
            }

            return ec;
        }

        return undefined;
    }

    getEquipmentClass(equipmentClass: Models.WellKnownEquipmentClassOrCustom): number
    {
        if (equipmentClass?.custom)
        {
            return equipmentClass.custom;
        }

        let ecMatch = this.rules?.equipmentClasses?.find((ec) => ec.wellKnown == equipmentClass?.known);
        if (ecMatch) return ecMatch.id;

        return undefined;
    }

    getEquipmentClassName(equipmentClass: Models.WellKnownEquipmentClassOrCustom): string
    {
        let id = this.getEquipmentClass(equipmentClass);

        let ecMatch = this.rules?.equipmentClasses?.find((ec) => ec.id == id);
        if (ecMatch) return ecMatch.description;

        return undefined;
    }

    setPointClass(id: number): Models.WellKnownPointClassOrCustom
    {
        let ecMatch = this.rules?.pointClasses?.find((ec) => ec.id == id);
        if (ecMatch)
        {
            let ec = new Models.WellKnownPointClassOrCustom();
            if (ecMatch.wellKnown)
            {
                ec.known = ecMatch.wellKnown;
            }
            else
            {
                ec.custom = ecMatch.id;
            }

            return ec;
        }

        return undefined;
    }

    getPointClass(pointClass: Models.WellKnownPointClassOrCustom): number
    {
        if (pointClass?.custom)
        {
            return pointClass.custom;
        }

        let ecMatch = this.rules?.pointClasses?.find((ec) => ec.wellKnown == pointClass?.known);
        if (ecMatch) return ecMatch.id;

        return undefined;
    }

    getPointClassName(pointClass: Models.WellKnownPointClassOrCustom): string
    {
        let id = this.getPointClass(pointClass);

        let ecMatch = this.rules?.pointClasses?.find((ec) => ec.id == id);
        if (ecMatch) return ecMatch.pointClassDescription;

        return undefined;
    }
}

export class I2CSensorSHT3xExtended extends I2CSensorExtendedBase
{
    constructor(rules: Models.NormalizationRules,
                public readonly model: Models.I2CSensorSHT3x)
    {
        super(rules);
    }

    set equipmentClass(id: number)
    {
        this.model.equipmentClass = this.setEquipmentClass(id);
    }

    get equipmentClass(): number
    {
        return this.getEquipmentClass(this.model.equipmentClass);
    }

    get equipmentClassDisplay(): string
    {
        return this.getEquipmentClassName(this.model.equipmentClass);
    }
}


export class I2CSensorMCP3428Extended extends I2CSensorExtendedBase
{
    constructor(rules: Models.NormalizationRules,
                public readonly model: Models.I2CSensorMCP3428)
    {
        super(rules);
    }

    set equipmentClass(id: number)
    {
        this.model.equipmentClass = this.setEquipmentClass(id);
    }

    get equipmentClass(): number
    {
        return this.getEquipmentClass(this.model.equipmentClass);
    }

    get equipmentClassDisplay(): string
    {
        return this.getEquipmentClassName(this.model.equipmentClass);
    }

    set pointClass(id: number)
    {
        this.model.pointClass = this.setPointClass(id);
    }

    get pointClass(): number
    {
        return this.getPointClass(this.model.pointClass);
    }

    get pointClassDisplay(): string
    {
        return this.getPointClassName(this.model.pointClass);
    }
}
