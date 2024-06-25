import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {CarouselSlideDirective} from "./carousel-slide.directive";
import {CarouselComponent} from "./carousel.component";

@NgModule({
              declarations: [
                  CarouselComponent,
                  CarouselSlideDirective
              ],
              imports     : [
                  CommonModule
              ],
              exports     : [
                  CarouselComponent,
                  CarouselSlideDirective
              ]
          })
export class CarouselModule {}
