import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {DataExplorerPageComponent} from "app/customer/visualization/data-explorer-page.component";
import {VisualizationRoutingModule} from "app/customer/visualization/visualization-routing.module";
import {WidgetModule} from "app/dashboard/dashboard/widgets/widget.module";
import {BookmarkModule} from "app/shared/bookmarks/bookmark.module";
import {ChartingModule} from "app/shared/charting/charting.module";
import {TimeSeriesSetModule} from "app/shared/charting/time-series-set/time-series-set.module";
import {AddWidgetOverlayModule} from "app/shared/overlays/add-widget-overlay.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [DataExplorerPageComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  BookmarkModule,
                  VisualizationRoutingModule,
                  ChartingModule,
                  TimeSeriesSetModule,
                  AddWidgetOverlayModule,
                  WidgetModule
              ]
          })
export class VisualizationModule {}
