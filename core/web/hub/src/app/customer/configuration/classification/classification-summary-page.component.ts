import {Component, Injector, ViewChild} from "@angular/core";
import {ClassificationRulesListComponent} from "app/customer/configuration/classification/classification-rules-list.component";

import * as SharedSvc from "app/services/domain/base.service";

@Component({
               selector   : "o3-classification-summary-page",
               templateUrl: "./classification-summary-page.component.html"
           })
export class ClassificationSummaryPageComponent extends SharedSvc.BaseApplicationComponent
{
    @ViewChild("classificationList", {static: true}) normalizationList: ClassificationRulesListComponent;

    constructor(inj: Injector)
    {
        super(inj);
    }
}


