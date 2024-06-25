import {Component, EventEmitter, Input, Output} from "@angular/core";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector: "o3-duration-selector",
               template: `
                   <div class="row">
                       <mat-form-field class="col-sm-6" style="display: inline-block" floatLabel="always">
                           <input matInput type="number" [placeholder]="amountPlaceholder" [(ngModel)]="amount">
                       </mat-form-field>
                       <mat-form-field class="col-sm-6" style="display: inline-block">
                           <mat-select [placeholder]="unitPlaceholder" name="durations" [(ngModel)]="units" [disabled]="readonly">
                               <mat-option *ngIf="excludes.indexOf('SECONDS') === -1" value="SECONDS">Second</mat-option>
                               <mat-option *ngIf="excludes.indexOf('MINUTES') === -1" value="MINUTES">Minute</mat-option>
                               <mat-option *ngIf="excludes.indexOf('HOURS') === -1" value="HOURS">Hour</mat-option>
                               <mat-option *ngIf="excludes.indexOf('DAYS') === -1" value="DAYS">Day</mat-option>
                               <mat-option *ngIf="excludes.indexOf('WEEKS') === -1" value="WEEKS">Weeks</mat-option>
                               <mat-option *ngIf="excludes.indexOf('MONTHS') === -1" value="MONTHS">Month</mat-option>
                               <mat-option *ngIf="excludes.indexOf('YEARS') === -1" value="YEARS">Year</mat-option>
                           </mat-select>
                       </mat-form-field>
                   </div>
               `
           })
export class DurationSelectorComponent
{
    private m_amount = 0;
    private m_units  = Models.ChronoUnit.MINUTES;

    @Input()
    public set amount(val: number)
    {
        this.m_amount = val;
        this.amountChange.emit(val);
    }

    public get amount(): number
    {
        return this.m_amount;
    }

    @Output() amountChange = new EventEmitter();

    @Input()
    public set units(val: Models.ChronoUnit)
    {
        this.m_units = val;
        this.unitsChange.emit(val);
    }

    public get units(): Models.ChronoUnit
    {
        return this.m_units;
    }

    @Input() excludes: string[] = [
        Models.ChronoUnit.SECONDS,
        Models.ChronoUnit.MINUTES,
        Models.ChronoUnit.MONTHS,
        Models.ChronoUnit.YEARS
    ];

    @Input() unitPlaceholder: string = "Time Units";

    @Input() amountPlaceholder: string = "Amount";

    @Input() hintLabel: string;

    @Input() readonly: boolean;

    @Output() unitsChange = new EventEmitter();
}
