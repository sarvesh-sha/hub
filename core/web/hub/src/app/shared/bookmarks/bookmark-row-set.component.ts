import {Component, EventEmitter, Input, Output} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {BookmarkSet} from "app/services/domain/bookmark.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-bookmark-row-set[host][bookmarkSet]",
               templateUrl: "./bookmark-row-set.component.html"
           })
export class BookmarkRowSetComponent
{
    @Input() host: SharedSvc.BaseApplicationComponent;
    @Input() bookmarkSet: BookmarkSet;
    @Input() filterText: string;

    @Output() deleted = new EventEmitter<Models.BookmarkConfiguration>();

    bookmarkDeleted(bookmark: Models.BookmarkConfiguration)
    {
        this.bookmarkSet.bookmarkDeleted(bookmark);
        this.deleted.emit(bookmark);
    }
}
