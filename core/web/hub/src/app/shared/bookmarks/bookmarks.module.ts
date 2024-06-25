import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {BookmarkModule} from "app/shared/bookmarks/bookmark.module";
import {BookmarksPageComponent} from "app/shared/bookmarks/bookmarks-page.component";
import {BookmarksRoutingModule} from "app/shared/bookmarks/bookmarks-routing.module";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [BookmarksPageComponent],
              imports     : [
                  CommonModule,
                  FrameworkUIModule,
                  BookmarkModule,
                  BookmarksRoutingModule
              ]
          })
export class BookmarksModule {}
