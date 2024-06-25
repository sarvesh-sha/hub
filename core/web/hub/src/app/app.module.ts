import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
// 3rd part dependencies
import {AppRoutingModule} from "app/app-routing.module";
// application
import {AppComponent} from "app/app.component";
// ui component modules
import {ReportingLayoutComponent} from "app/layouts/reporting-layout.component";
// layouts
import {StandardLayoutComponent, StandardLayoutMenuDirective} from "app/layouts/standard-layout.component";
import {BookmarkRedirectComponent} from "app/shared/bookmarks/bookmark-redirect.component";
import {DirectivesModule} from "app/shared/directives/directives.module";
// service layer
// configuration
// framework
import {FrameworkUIModule} from "framework/ui";
import {ErrorListPaneModule} from "framework/ui/errors/error-list-pane.module";

/**
 * Root Module
 *
 * App bootstrap here, add your component (Page) to var [declarations] for load.
 */
@NgModule({
              declarations: [
                  // Page
                  AppComponent,
                  StandardLayoutComponent,
                  StandardLayoutMenuDirective,
                  ReportingLayoutComponent,
                  BookmarkRedirectComponent
              ],
              imports     : [
                  // Angular Imports
                  CommonModule,

                  // framework
                  FrameworkUIModule,

                  AppRoutingModule,
                  DirectivesModule,
                  ErrorListPaneModule
              ],
              providers   : []
          })
export class AppModule {}
