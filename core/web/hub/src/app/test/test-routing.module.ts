import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";

import {TestRootComponent} from "app/test/test-root.component";

const routes: Routes = [
    {
        path     : "",
        component: TestRootComponent
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class TestRoutingModule {}
