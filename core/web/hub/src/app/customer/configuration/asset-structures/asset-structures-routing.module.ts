import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AppNavigationResolver} from "app/app-navigation.resolver";
import {AssetStructureDetailsPageComponent} from "app/customer/configuration/asset-structures/asset-structure-details-page.component";
import {AssetStructureListPageComponent} from "app/customer/configuration/asset-structures/asset-structure-list-page.component";

const routes: Routes = [
    {
        path     : "",
        component: AssetStructureListPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title: "Asset Structures"
        }
    },
    {
        path     : ":id",
        component: AssetStructureDetailsPageComponent,
        resolve  : {breadcrumbs: AppNavigationResolver},
        data     : {
            title      : "Asset Structures",
            breadcrumbs: [
                {
                    title: "Asset Structures",
                    url  : "configuration/asset-structures"
                }
            ]
        }
    }
];

@NgModule({
              imports: [RouterModule.forChild(routes)],
              exports: [RouterModule]
          })
export class AssetStructuresRoutingModule {}
