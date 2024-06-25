import {Component, ViewChild} from "@angular/core";

import {AppService} from "app/app.service";
import {RegistryTaggedImagesListComponent} from "app/customer/registry-tagged-images/registry-tagged-images-list.component";

@Component({
               selector   : "o3-registry-tagged-images-summary-page",
               templateUrl: "./registry-tagged-images-summary-page.component.html"
           })
export class RegistryTaggedImagesSummaryPageComponent
{
    @ViewChild("childList", { static: true })childList: RegistryTaggedImagesListComponent;

    filterImages: string;

    constructor(public appService: AppService)
    {
    }
}
