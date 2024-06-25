import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {UsersTestsModule} from "app/customer/configuration/users/users.spec";
import {DataExplorerTestsModule} from "app/customer/visualization/data-explorer.spec";
import {DashboardTestsModule} from "app/dashboard/dashboard/dashboard.spec";
import {AggregationTableTestModule} from "app/dashboard/dashboard/widgets/aggregation-table-widget/widget.spec";
import {LoginPageTestsModule} from "app/start/login-page.spec";

import {TestRootComponent} from "app/test/test-root.component";
import {TestRoutingModule} from "app/test/test-routing.module";

@NgModule({
              declarations: [TestRootComponent],
              imports     : [
                  CommonModule,
                  TestRoutingModule,
                  LoginPageTestsModule,
                  DashboardTestsModule,
                  AggregationTableTestModule,
                  DataExplorerTestsModule,
                  UsersTestsModule,
                  UsersTestsModule
              ],
              exports     : []
          })
export class TestModule {}
