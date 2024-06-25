import {Component, Injector} from "@angular/core";

import {UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";

import {AppNavigationService, BreadcrumbItem} from "framework/ui/navigation/app-navigation.service";

@Component({
               selector   : "o3-breadcrumbs",
               templateUrl: "./breadcrumbs.component.html"
           })
export class BreadcrumbsComponent extends BaseComponent
{
    label: string = "";

    constructor(inj: Injector,
                public navigation: AppNavigationService,
                public utils: UtilsService)
    {
        super(inj);

        this.subscribeToObservable(this.navigation.breadcrumbLabelChanged, () => this.bind());
    }

    bind()
    {
        this.utils.setTimeoutOutsideAngular(() => { this.label = this.navigation.breadcrumbCurrentLabel; }, 0);
    }

    go(item: BreadcrumbItem)
    {
        this.navigation.breadcrumb(item);
    }
}
