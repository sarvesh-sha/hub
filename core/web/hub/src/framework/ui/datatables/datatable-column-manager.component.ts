import {Component, EventEmitter, Output, ViewChild} from "@angular/core";
import {ColumnConfiguration} from "framework/ui/datatables/datatable-manager";
import {DatatableConfiguration} from "framework/ui/datatables/datatable.component";
import {LAYOUT_WIDTH_SM} from "framework/ui/layout";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

// todo: use o3-standard-overlay instead

@Component({
               selector   : "o3-datatable-column-manager",
               templateUrl: "./datatable-column-manager.component.html",
               styleUrls  : ["./datatable-column-manager.component.scss"]
           })
export class DatatableColumnManagerComponent
{
    public title: string;
    public dialogConfig = OverlayConfig.onTopDraggable({
                                                           minWidth : LAYOUT_WIDTH_SM,
                                                           maxHeight: "90vh"
                                                       });

    private datatableConfig: DatatableConfiguration;

    colInfoType = ColumnConfiguration;

    public columns: ColumnConfiguration[];
    public numActiveColumns: number;

    public isDefault: boolean = false;
    public pristine: boolean  = true;

    @Output() columnsUpdated: EventEmitter<ColumnConfiguration[]> = new EventEmitter();

    @ViewChild(OverlayComponent, {static: true}) overlay: OverlayComponent;

    get isOpen(): boolean
    {
        return this.overlay && this.overlay.isOpen;
    }

    public open(datatableConfiguration: DatatableConfiguration,
                title?: string): void
    {
        if (this.overlay && !this.overlay.isOpen)
        {
            this.datatableConfig = datatableConfiguration;
            this.setColumns(this.datatableConfig.columnConfigurations, this.datatableConfig.isDefault, true);

            this.title = title;

            this.overlay.toggleOverlay();
        }
    }

    public close(): void
    {
        if (this.isOpen)
        {
            this.overlay.toggleOverlay();
        }
    }

    public save(): void
    {
        this.columnsUpdated.emit(this.columns);
        this.columns  = this.columns.map((column) => column.clone());
        this.pristine = true;
    }

    public reset()
    {
        this.setColumns(this.datatableConfig.defaultConfigs, true);
    }

    public setColumns(columnConfigs: ColumnConfiguration[],
                      isDefault: boolean,
                      pristine?: boolean)
    {
        this.columns          = columnConfigs;
        this.numActiveColumns = 0;

        for (let currCol of columnConfigs)
        {
            if (currCol.enabled) this.numActiveColumns++;
        }

        this.isDefault = isDefault;
        if (typeof pristine === "boolean")
        {
            this.pristine = pristine;
        }
        else
        {
            this.updatePristineState();
        }
    }

    public handleToggle(colInfo: ColumnConfiguration)
    {
        if (colInfo.enabled && this.numActiveColumns === 1) return;

        colInfo.enabled = !colInfo.enabled;

        if (colInfo.enabled)
        {
            this.numActiveColumns++;
        }
        else
        {
            this.numActiveColumns--;
        }

        this.updatePristineState();
    }

    public updatePristineState(): void
    {
        if (this.datatableConfig && this.columns)
        {
            let activeConfigs = this.datatableConfig.columnConfigurations;
            this.pristine     = activeConfigs.length === this.columns.length && activeConfigs.every((config,
                                                                                                     idx) => config.isEquivalent(this.columns[idx]));
        }
        else
        {
            this.pristine = false;
        }
    }
}
