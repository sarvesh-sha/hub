import {NgModule} from "@angular/core";

import {ControlBindingService} from "app/services/ui/control-binding.service";
import {SearchBindingService} from "app/services/ui/search-binding.service";

@NgModule({
              providers: [
                  ControlBindingService,
                  SearchBindingService
              ]
          })
export class UIModule {}
