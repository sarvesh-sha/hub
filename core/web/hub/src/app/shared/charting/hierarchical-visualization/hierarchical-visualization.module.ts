import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {ContextPaneModule} from "app/dashboard/context-pane";

import {TimeSeriesChartPreviewModule} from "app/shared/assets/chart-preview/time-series-chart-preview.module";
import {DataSourceWizardModule} from "app/shared/charting/data-source-wizard/data-source-wizard.module";
import {HierarchicalVisualizationConfigurationComponent} from "app/shared/charting/hierarchical-visualization/hierarchical-visualization-configuration.component";
import {HierarchicalVisualizationComponent} from "app/shared/charting/hierarchical-visualization/hierarchical-visualization.component";
import {InteractiveTreeModule} from "app/shared/charting/interactive-tree/interactive-tree.module";
import {TimeSeriesChartModule} from "app/shared/charting/time-series-chart/time-series-chart.module";
import {ColorsModule} from "app/shared/colors/colors.module";

import {FrameworkUIModule} from "framework/ui";
import {SkeletonScreenModule} from "framework/ui/shared/skeleton-screen/skeleton-screen.module";

@NgModule({
              declarations: [
                  HierarchicalVisualizationConfigurationComponent,
                  HierarchicalVisualizationComponent
              ],
              imports     : [
                  ColorsModule,
                  CommonModule,
                  ContextPaneModule,
                  DataSourceWizardModule,
                  FrameworkUIModule,
                  InteractiveTreeModule,
                  SkeletonScreenModule,
                  TimeSeriesChartModule,
                  TimeSeriesChartPreviewModule
              ],
              exports     : [
                  HierarchicalVisualizationComponent
              ]
          })
export class HierarchicalVisualizationModule {}
