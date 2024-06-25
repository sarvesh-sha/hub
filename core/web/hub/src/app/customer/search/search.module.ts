import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {SearchResultsPageComponent} from "app/customer/search/search-results-page.component";
import {SearchRoutingModule} from "app/customer/search/search-routing.module";
import {SearchModule as SharedSearchModule} from "app/shared/search/search.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [SearchResultsPageComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  SearchRoutingModule,
                  SharedSearchModule
              ]
          })
export class SearchModule {}
