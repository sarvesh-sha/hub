import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {SkeletonScreenComponent} from "framework/ui/shared/skeleton-screen/skeleton-screen.component";

@NgModule({
              declarations: [SkeletonScreenComponent],
              imports     : [CommonModule],
              exports     : [SkeletonScreenComponent]
          })
export class SkeletonScreenModule {}
