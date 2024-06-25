import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";

import {BookmarkEditorDialogComponent} from "app/shared/bookmarks/bookmark-editor-dialog.component";
import {BookmarkRowSetComponent} from "app/shared/bookmarks/bookmark-row-set.component";
import {BookmarkRowComponent} from "app/shared/bookmarks/bookmark-row.component";
import {BookmarksOverlayComponent} from "app/shared/bookmarks/bookmarks-overlay.component";
import {FrameworkUIModule} from "framework/ui";

@NgModule({
              declarations: [
                  BookmarkEditorDialogComponent,
                  BookmarkRowComponent,
                  BookmarkRowSetComponent,
                  BookmarksOverlayComponent
              ],
              imports     : [
                  CommonModule,
                  FrameworkUIModule
              ],
              exports     : [
                  BookmarkEditorDialogComponent,
                  BookmarksOverlayComponent,
                  BookmarkRowSetComponent
              ]
          })
export class BookmarkModule {}
