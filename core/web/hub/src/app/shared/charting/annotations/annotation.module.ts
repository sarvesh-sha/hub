import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {AnnotationOverlayComponent} from "app/shared/charting/annotations/annotation-overlay.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
    declarations: [AnnotationOverlayComponent],
    imports: [CommonModule, FrameworkUIModule],
    exports: [AnnotationOverlayComponent]
          })
export class AnnotationModule {}
