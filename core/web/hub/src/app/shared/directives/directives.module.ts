import {NgModule} from "@angular/core";

import {HasAccessDirective} from "app/shared/directives/has-access.directive";
import {IfSystemPreferenceDirective} from "app/shared/directives/if-system-preference.directive";

@NgModule({
              declarations: [
                  HasAccessDirective,
                  IfSystemPreferenceDirective
              ],
              exports     : [
                  HasAccessDirective,
                  IfSystemPreferenceDirective
              ]
          })
export class DirectivesModule {}
