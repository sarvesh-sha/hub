import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {ImagePreviewSelectorComponent} from "app/shared/dropdowns/image-preview-selector/image-preview-selector.component";
import {AppImageModule} from "app/shared/image/app-image.module";
import {FrameworkUIModule} from "framework/ui";
import {CarouselModule} from "framework/ui/carousel/carousel.module";

@NgModule({
              declarations: [ImagePreviewSelectorComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  AppImageModule,
                  CarouselModule
              ],
              exports     : [ImagePreviewSelectorComponent]
          })
export class ImagePreviewSelectorModule {}
