import {CommonModule} from "@angular/common";
import {HttpClientModule} from "@angular/common/http";
import {NgModule} from "@angular/core";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatCardModule} from "@angular/material/card";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {MatExpansionModule} from "@angular/material/expansion";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatMenuModule} from "@angular/material/menu";
import {MatSelectModule} from "@angular/material/select";
import {MatTooltipModule} from "@angular/material/tooltip";
import {BrowserModule} from "@angular/platform-browser";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {TestCategoryFilterComponent} from "app/test-filtering/test-category-filter.component";
import {TestResultsComponent} from "app/test-results/test-results.component";
import {TestSelectorComponent} from "app/test-selector/test-selector.component";
import {environment} from "environments/environment";
import {ApiClientConfiguration} from "framework/services/api.client";

import {AppRoutingModule} from "./app-routing.module";
import {AppComponent} from "./app.component";

@NgModule({
              declarations: [
                  AppComponent,
                  TestCategoryFilterComponent,
                  TestResultsComponent,
                  TestSelectorComponent
              ],
              imports     : [
                  BrowserModule,
                  HttpClientModule,
                  FormsModule,
                  CommonModule,
                  BrowserAnimationsModule.withConfig({}),
                  AppRoutingModule,
                  MatFormFieldModule,
                  MatInputModule,
                  MatButtonModule,
                  MatCardModule,
                  MatCheckboxModule,
                  MatIconModule,
                  MatMenuModule,
                  MatSelectModule,
                  MatExpansionModule,
                  ReactiveFormsModule,
                  MatTooltipModule
              ],
              providers   : [
                  {
                      provide : ApiClientConfiguration,
                      useValue: {
                          apiDomain: environment.apiDomain,
                          apiPort  : environment.apiPort
                      }
                  }
              ],
              bootstrap   : [AppComponent]
          })
export class AppModule
{
}
