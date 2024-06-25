import {Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {BookmarkSet} from "app/services/domain/bookmark.service";
import * as Models from "app/services/proxy/model/models";
import {OverlayController} from "framework/ui/overlays/overlay-base";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-bookmarks-overlay[bookmarks]",
               templateUrl: "./bookmarks-overlay.component.html",
               styleUrls  : ["./bookmarks-overlay.component.scss"]
           })
export class BookmarksOverlayComponent extends SharedSvc.BaseApplicationComponent implements OverlayController
{
    private m_bookmarks: BookmarkSet[];
    @Input() set bookmarks(sets: BookmarkSet[])
    {
        this.m_bookmarks = sets;

        let numBookmarks = sets.reduce((cum,
                                        set) => cum + set.numBookmarks, 0);
        if (numBookmarks > this.m_numBookmarks && this.isOpen()) this.overlay.pullIntoView(0);
        this.m_numBookmarks = numBookmarks;
    }

    get bookmarks(): BookmarkSet[]
    {
        return this.m_bookmarks;
    }

    @Input() label: string = "Bookmarks";

    @ViewChild(OverlayComponent, {static: true}) overlay: OverlayComponent;

    @Output() bookmarkDeleted = new EventEmitter<Models.BookmarkConfiguration>();

    filterText: string = "";

    private m_numBookmarks: number = 0;
    get numBookmarks(): number
    {
        return this.m_numBookmarks;
    }

    overlayConfig = OverlayConfig.onTopDraggable({
                                                     width    : 500,
                                                     maxHeight: 600,
                                                     maxWidth : "95vw"
                                                 });

    public closeOverlay(): void
    {
        if (this.overlay.isOpen) this.overlay.toggleOverlay();
    }

    public isOpen(): boolean
    {
        return this.overlay?.isOpen;
    }

    public openOverlay(): void
    {
        if (!this.overlay.isOpen)
        {
            this.overlay.toggleOverlay();
            this.overlay.resetPosition();
        }
    }

    public toggleOverlay(open?: boolean): void
    {
        if (open === undefined) open = !this.isOpen();

        if (open)
        {
            this.openOverlay();
        }
        else
        {
            this.closeOverlay();
        }
    }
}
