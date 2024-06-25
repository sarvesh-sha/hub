import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {ImagePreviewComponent} from "app/shared/image/image-preview.component";

import {ImageModule} from "framework/ui/image/image.module";

@NgModule({
              declarations: [
                  ImagePreviewComponent
              ],
              imports     : [
                  CommonModule,
                  ImageModule
              ],
              exports     : [
                  ImagePreviewComponent
              ]
          })
export class AppImageModule {}
