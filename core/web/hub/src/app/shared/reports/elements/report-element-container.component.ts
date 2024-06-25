import {Component, ComponentFactoryResolver, ElementRef, EventEmitter, Input, Output, Type, ViewChild, ViewContainerRef} from "@angular/core";
import {ReportElementAggregatedValueComponent} from "app/shared/reports/elements/report-element-aggregated-value.component";
import {ReportElementAggregationTableComponent} from "app/shared/reports/elements/report-element-aggregation-table.component";
import {ReportElementAggregationTrendComponent} from "app/shared/reports/elements/report-element-aggregation-trend.component";
import {ReportElementAlertExecutionSummaryComponent} from "app/shared/reports/elements/report-element-alert-execution-summary.component";
import {ReportElementAlertFeedComponent} from "app/shared/reports/elements/report-element-alert-feed.component";
import {ReportElementAlertsListComponent} from "app/shared/reports/elements/report-element-alerts-list.component";
import {ReportElementAlertsSummaryComponent} from "app/shared/reports/elements/report-element-alerts-summary.component";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";
import {ReportElementChartSetComponent} from "app/shared/reports/elements/report-element-chart-set.component";
import {ReportElementControlPointsListComponent} from "app/shared/reports/elements/report-element-control-points-list.component";
import {ReportElementDevicesListComponent} from "app/shared/reports/elements/report-element-devices-list.component";
import {ReportElementDevicesSummaryComponent} from "app/shared/reports/elements/report-element-devices-summary.component";
import {ReportElementEquipmentListComponent} from "app/shared/reports/elements/report-element-equipment-list.component";
import {ReportElementHeaderComponent} from "app/shared/reports/elements/report-element-header.component";
import {ReportElementPageBreakComponent} from "app/shared/reports/elements/report-element-page-break.component";
import {ReportElementRichTextComponent} from "app/shared/reports/elements/report-element-rich-text.component";
import {ReportElementAlertTableComponent} from "./report-element-alert-table.component";

@Component({
               selector: "o3-report-element-container",
               template: `
                   <div #reportElementContainer></div>
               `
           })
export class ReportElementContainerComponent
{
    @ViewChild("reportElementContainer",
               {
                   read  : ViewContainerRef,
                   static: true
               })
    reportElementContainer: ViewContainerRef;

    @Output() renderComplete = new EventEmitter<ReportElementConfigurationBase>();

    constructor(private componentFactoryResolver: ComponentFactoryResolver,
                private element: ElementRef)
    {
    }

    @Input() set model(model: ReportElementModel)
    {
        if (model)
        {
            let componentType: Type<ReportElementBaseComponent<ReportElementDataBase, ReportElementConfigurationBase>>;
            switch (model.type)
            {
                case ReportElementType.AlertExecutionSummary:
                    componentType = ReportElementAlertExecutionSummaryComponent;
                    break;

                case ReportElementType.AlertFeed:
                    componentType = ReportElementAlertFeedComponent;
                    break;

                case ReportElementType.AlertTable:
                    componentType = ReportElementAlertTableComponent;
                    break;

                case ReportElementType.AlertsList:
                    componentType = ReportElementAlertsListComponent;
                    break;

                case ReportElementType.AlertsSummary:
                    componentType = ReportElementAlertsSummaryComponent;
                    break;

                case ReportElementType.ControlPointList:
                    componentType = ReportElementControlPointsListComponent;
                    break;

                case ReportElementType.DevicesList:
                    componentType = ReportElementDevicesListComponent;
                    break;

                case ReportElementType.DevicesSummary:
                    componentType = ReportElementDevicesSummaryComponent;
                    break;

                case ReportElementType.EquipmentList:
                    componentType = ReportElementEquipmentListComponent;
                    break;

                case ReportElementType.Header:
                    componentType = ReportElementHeaderComponent;
                    break;

                case ReportElementType.AggregatedValue:
                    componentType = ReportElementAggregatedValueComponent;
                    break;

                case ReportElementType.AggregationTable:
                    componentType = ReportElementAggregationTableComponent;
                    break;

                case ReportElementType.AggregationTrend:
                    componentType = ReportElementAggregationTrendComponent;
                    break;

                case ReportElementType.ChartSet:
                    componentType = ReportElementChartSetComponent;
                    break;

                case ReportElementType.RichText:
                    componentType = ReportElementRichTextComponent;
                    break;

                case ReportElementType.PageBreak:
                    componentType = ReportElementPageBreakComponent;
                    break;
            }

            // We create the component using the factory and the injector
            this.element.nativeElement.id = `report-element-${model.id}`;

            let factory = this.componentFactoryResolver.resolveComponentFactory(componentType);

            this.reportElementContainer.clear();

            let elementComponent    = <ReportElementBaseComponent<any, any>>this.reportElementContainer.createComponent(factory).instance;
            elementComponent.id     = model.id || "";
            elementComponent.config = model.configuration;
            elementComponent.data   = model.data;

            elementComponent.subscribeToCompleted((config) => this.renderComplete.emit(config));
        }
    }
}
