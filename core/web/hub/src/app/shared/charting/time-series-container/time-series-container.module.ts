import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AngularDraggableModule} from "angular2-draggable";

import {DataSourceWizardModule} from "app/shared/charting/data-source-wizard/data-source-wizard.module";
import {HierarchicalVisualizationModule} from "app/shared/charting/hierarchical-visualization/hierarchical-visualization.module";
import {ScatterPlotModule} from "app/shared/charting/scatter-plot/scatter-plot.module";
import {SourceChipModule} from "app/shared/charting/source-chip/source-chip.module";
import {SourceConfigurerModule} from "app/shared/charting/source-configurer/source-configurer.module";
import {TimeSeriesChartModule} from "app/shared/charting/time-series-chart/time-series-chart.module";
import {TimeSeriesContainerComponent} from "app/shared/charting/time-series-container/time-series-container.component";
import {ColorsModule} from "app/shared/colors/colors.module";
import {GpsMapModule} from "app/shared/mapping/gps-map/gps-map.module";
import {AddWidgetOverlayModule} from "app/shared/overlays/add-widget-overlay.module";

import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [TimeSeriesContainerComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  AngularDraggableModule,
                  AddWidgetOverlayModule,
                  ColorsModule,
                  DataSourceWizardModule,
                  GpsMapModule,
                  HierarchicalVisualizationModule,
                  ScatterPlotModule,
                  SourceConfigurerModule,
                  SourceChipModule,
                  TimeSeriesChartModule
              ],
              exports     : [TimeSeriesContainerComponent]
          })
export class TimeSeriesContainerModule {}
