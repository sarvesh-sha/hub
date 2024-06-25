import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {ImageFrameComponent} from "framework/ui/image/image-frame.component";
import {SafeImageComponent} from "framework/ui/image/safe-image.component";

@NgModule({
              declarations: [
                  ImageFrameComponent,
                  SafeImageComponent
              ],
              imports     : [
                  CommonModule
              ],
              exports     : [
                  ImageFrameComponent,
                  SafeImageComponent
              ]
          })
export class ImageModule {}
