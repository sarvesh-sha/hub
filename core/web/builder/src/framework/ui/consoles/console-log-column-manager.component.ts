import {Component, EventEmitter, Output, ViewChild} from "@angular/core";
import {BaseComponent} from "framework/ui/components";
import {ColumnConfig, DefaultColumns} from "framework/ui/consoles/console-log";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";

@Component({
               selector   : "o3-console-log-column-manager",
               templateUrl: "./console-log-column-manager.component.html",
               styleUrls  : ["./console-log-column-manager.component.scss"]
           })
export class ConsoleLogColumnManagerComponent extends BaseComponent
{
    public dialogConfig = OverlayConfig.onTopDraggable({
                                                           minWidth : 600,
                                                           maxHeight: "90vh"
                                                       });

    colInfoType = ColumnConfig;

    public columns: ColumnConfig[];
    public numActiveColumns: number;
    public isDefault: boolean = false;
    public pristine: boolean  = true;

    @Output() columnsUpdated = new EventEmitter<ColumnConfig[]>();

    @ViewChild(StandardFormOverlayComponent, {static: true}) overlay: StandardFormOverlayComponent;

    private m_defaultConfig = DefaultColumns;
    private m_activeConfig: ColumnConfig[];

    get isOpen(): boolean
    {
        return this.overlay && this.overlay.isOpen();
    }

    public open(columns: ColumnConfig[]): void
    {
        if (!this.isOpen)
        {
            this.m_activeConfig = columns;
            this.setColumns(columns);
            this.detectChanges();

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
        this.m_activeConfig = this.columns;
        this.setColumns(this.columns);
    }

    public reset()
    {
        this.setColumns(this.m_defaultConfig);
    }

    public setColumns(columnConfigs: ColumnConfig[])
    {
        this.columns          = columnConfigs.map((c) => c.clone());
        this.numActiveColumns = 0;

        for (let currCol of columnConfigs)
        {
            if (currCol.enabled) this.numActiveColumns++;
        }

        this.updatePristineState();
    }

    public handleToggle(colInfo: ColumnConfig)
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
        if (this.columns)
        {
            this.pristine = this.m_activeConfig.length === this.columns.length && this.m_activeConfig.every((config,
                                                                                                             idx) => config.equals(this.columns[idx]));

            this.isDefault = this.m_defaultConfig.length === this.columns.length && this.m_defaultConfig.every((config,
                                                                                                                idx) => config.equals(this.columns[idx]));
        }
        else
        {
            this.pristine  = false;
            this.isDefault = false;
        }
    }
}
