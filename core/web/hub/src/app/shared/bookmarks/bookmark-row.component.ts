import {Component, EventEmitter, Input, OnDestroy, Output} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";
import {BookmarkSet} from "app/services/domain/bookmark.service";

import * as Models from "app/services/proxy/model/models";
import {BookmarkEditorDialogComponent} from "app/shared/bookmarks/bookmark-editor-dialog.component";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

@Component({
               selector   : "o3-bookmark-row[host][bookmarkSet]",
               templateUrl: "./bookmark-row.component.html",
               styleUrls  : ["./bookmark-row.component.scss"]
           })
export class BookmarkRowComponent implements OnDestroy
{
    private static readonly cancelDeletionWindowMs: number = 7500;

    @Input() host: SharedSvc.BaseApplicationComponent;

    private m_bookmarkSet: BookmarkSet;
    @Input() set bookmarkSet(set: BookmarkSet)
    {
        if (!set) return;

        let bookmark = set.bookmark;
        switch (bookmark.type)
        {
            case Models.BookmarkType.ALERT:
            case Models.BookmarkType.DEVICE:
            case Models.BookmarkType.DEVICE_ELEMENT:
            case Models.BookmarkType.EQUIPMENT:
                this.staticName = bookmark.name;
                break;

            default:
                this.staticName = null;
                break;
        }

        this.m_bookmarkSet = set;
        if (!this.bookmark)
        {
            this.bookmark = bookmark;
            this.updateBookmarkText();
        }
    }

    bookmark: Models.BookmarkConfiguration;
    staticName: string;
    private bookmarkText: string;
    inView: boolean = true;

    menuOpen: boolean = false;
    isView: boolean   = false;

    deleting: boolean = false;
    private deletionTimer: number;

    @Input() set view(view: Models.BookmarkConfiguration)
    {
        this.isView   = !!view;
        this.bookmark = view;
        this.updateBookmarkText();
    }

    private m_filterText: string = "";
    @Input() set filterText(text: string)
    {
        text = text || "";
        if (this.m_filterText != text)
        {
            this.m_filterText = text;
            this.updateInView();
        }
    }

    @Output() deleted = new EventEmitter<Models.BookmarkConfiguration>();

    ngOnDestroy(): void
    {
        if (this.deletionTimer)
        {
            clearTimeout(this.deletionTimer);
            this.deleteBookmark();
        }
    }

    handleRowClick()
    {
        if (this.deleting)
        {
            this.cancelDeletion();
        }
        else
        {
            this.viewBookmark();
        }
    }

    private cancelDeletion()
    {
        this.deleting = false;
        if (this.deletionTimer)
        {
            clearTimeout(this.deletionTimer);
            this.deletionTimer = null;
        }
    }

    private viewBookmark()
    {
        this.host.app.domain.bookmarks.navigateTo(this.bookmark);
    }

    openMenu(click: MouseEvent)
    {
        this.menuOpen = true;
        click.stopPropagation();
    }

    editBookmark()
    {
        BookmarkEditorDialogComponent.open(this.host, true, this.staticName,
                                           (name,
                                            description) => this.updateBookmark(name, description),
                                           this.bookmark);
    }

    private async updateBookmark(name: string,
                                 description: string): Promise<Models.BookmarkConfiguration>
    {
        this.bookmark             = Models.BookmarkConfiguration.newInstance(this.bookmark);
        this.bookmark.name        = name;
        this.bookmark.description = description;

        await this.host.app.domain.bookmarks.updateBookmark(this.bookmark);

        return this.bookmark;
    }

    exportBookmark()
    {
        let data = this.m_bookmarkSet;
        if (this.isView)
        {
            data = new BookmarkSet(this.m_bookmarkSet.bookmark);
            data.addView(this.bookmark);
        }

        DownloadDialogComponent.open(this.host, "Bookmark Export", DownloadDialogComponent.fileName("bookmark"), [data]);
    }

    markForDeletion()
    {
        this.deletionTimer = setTimeout(() => this.deleteBookmark(), BookmarkRowComponent.cancelDeletionWindowMs);
        this.deleting      = true;
    }

    private async deleteBookmark()
    {
        this.deletionTimer = null;

        await this.host.app.domain.bookmarks.deleteBookmark(this.bookmark);

        this.deleted.emit(this.bookmark);
    }

    private updateBookmarkText(updateInView: boolean = true)
    {
        let bookmarkText = this.bookmark.name.toLocaleLowerCase() + (this.bookmark.description?.toLocaleLowerCase() || "");
        if (this.bookmark.createdOn)
        {
            let createdMoment = MomentHelper.parse(this.bookmark.createdOn);
            bookmarkText += createdMoment.format("l") + createdMoment.format("LT");
        }
        this.bookmarkText = bookmarkText || "";

        if (updateInView) this.updateInView();
    }

    private updateInView()
    {
        if (!this.bookmarkText) this.updateBookmarkText(false);

        let filterText = this.m_filterText || "";
        this.inView    = this.bookmark && this.bookmarkText.includes(filterText.toLocaleLowerCase());
    }
}
