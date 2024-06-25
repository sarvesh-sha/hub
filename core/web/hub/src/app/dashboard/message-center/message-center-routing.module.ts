import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {MessageCenterPageComponent} from "app/dashboard/message-center/message-center-page.component";

const routes: Routes = [
    {
        path     : "",
        component: MessageCenterPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Notification Center",
            breadcrumbs: []
        }
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class MessageCenterRoutingModule {}
