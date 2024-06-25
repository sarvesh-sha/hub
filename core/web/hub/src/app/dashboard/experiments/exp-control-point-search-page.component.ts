import {Component} from "@angular/core";

import {ExperimentsBasePageComponent} from "app/dashboard/experiments/exp-base-page.component";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-experiments-control-point-search-page",
               templateUrl: "./exp-control-point-search-page.component.html",
               styleUrls  : ["./exp-control-point-search-page.component.scss"]
           })
export class ExperimentsControlPointSearchPageComponent extends ExperimentsBasePageComponent
{
    builderTest: Models.TagsCondition = null;
}
