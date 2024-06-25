import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {SamplingDetailPageComponent} from "app/customer/data-collection/sampling/sampling-detail-page.component";
import {SamplingRoutingModule} from "app/customer/data-collection/sampling/sampling-routing.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [SamplingDetailPageComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  SamplingRoutingModule
              ]
          })
export class SamplingModule {}
