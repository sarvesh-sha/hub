import {NgModule} from "@angular/core";

import {ControlBindingService} from "app/services/ui/control-binding.service";
import {CssConstantsService} from "app/services/ui/css-constants";
import {SearchBindingService} from "app/services/ui/search-binding.service";
import {ViewportService} from "framework/services/viewport.service";

@NgModule({
              providers: [
                  ControlBindingService,
                  SearchBindingService,
                  ViewportService,
                  CssConstantsService
              ]
          })
export class UIModule {}
