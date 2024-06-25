import {NgModule} from "@angular/core";

import {RoutableViewPortalComponent, RoutableViewSourceDirective} from "framework/ui/routable-view/routable-view-portal.component";

@NgModule({
              declarations: [
                  RoutableViewPortalComponent,
                  RoutableViewSourceDirective
              ],
              imports     : [],
              exports     : [
                  RoutableViewPortalComponent,
                  RoutableViewSourceDirective
              ]
          })
export class RoutableViewModule {}
