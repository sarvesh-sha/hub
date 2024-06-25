import {Component, EventEmitter, Input, Output, TemplateRef, ViewChild} from "@angular/core";

import * as Models from "app/services/proxy/model/models";

import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";

@Component({
               selector   : "o3-custom-report-builder-row-configurer",
               templateUrl: "./custom-report-builder-row-configurer.component.html",
               styleUrls  : [
                   "./custom-report-builder.component.scss",
                   "./custom-report-builder-row-configurer.component.scss"
               ]
           })
export class CustomReportBuilderRowConfigurerComponent
{
    @Input() layoutTemplate: TemplateRef<any>;

    public rowLayoutConfig = OverlayConfig.onTopDraggable({
                                                              maxWidth: "95vw",
                                                              width   : 1000
                                                          });

    @ViewChild(StandardFormOverlayComponent, {static: true}) rowLayoutOverlay: StandardFormOverlayComponent;

    @Output() rowUpdated = new EventEmitter<void>();

    private m_row: Models.ReportLayoutRow;
    get row(): Models.ReportLayoutRow
    {
        return this.m_row;
    }

    public colWidths: number[] = [];


    get colWidthsPristine(): boolean
    {
        if (!this.colWidths) return false;
        if (!this.m_row) return false;

        for (let i = 0; i < this.m_row.children.length; i++)
        {
            if (this.m_row.children[i].widthRatio !== this.colWidths[i]) return false;
        }

        return true;
    }

    public configureRow(row: Models.ReportLayoutRow)
    {
        this.m_row = row;
        if (this.m_row)
        {
            this.colWidths = this.m_row.children.map((child) => child.widthRatio || 1);
            this.rowLayoutOverlay.toggleOverlay(true);
        }
    }

    public updateWidthRatios()
    {
        for (let i = 0; i < this.m_row.children.length; i++)
        {
            this.m_row.children[i].widthRatio = this.colWidths[i];
        }
        this.colWidths = [];

        this.rowUpdated.emit();
    }
}
