import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {HealthGaugeComponent} from "app/shared/gauge/gauge-health.component";

@NgModule({
              declarations: [HealthGaugeComponent],
              imports     : [CommonModule],
              exports     : [HealthGaugeComponent]
          })
export class GaugeModule {}
