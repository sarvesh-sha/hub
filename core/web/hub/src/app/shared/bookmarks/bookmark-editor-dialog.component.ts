import {Component, Inject} from "@angular/core";

import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";
import {LAYOUT_WIDTH_SM} from "framework/ui/layout";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./bookmark-editor-dialog.component.html"
           })
export class BookmarkEditorDialogComponent
{
    private m_name: string;
    get name(): string
    {
        return this.data.staticName || this.m_name;
    }

    set name(name: string)
    {
        this.m_name = name;
    }

    description: string;
    overwriteContent: boolean = false;

    pristine: boolean = true;
    private initialName: string;
    private initialDescription: string;

    get overlayLabel(): string
    {
        return !!this.data.bookmark ? "Edit Bookmark" : "Save Bookmark";
    }

    get secondaryButtonText(): string
    {
        return this.pristine ? "Close" : "Cancel";
    }

    constructor(public dialogRef: OverlayDialogRef<Models.BookmarkConfiguration>,
                @Inject(OVERLAY_DATA) public data: BookmarkConfig)
    {
        let bookmark = this.data.bookmark;
        if (bookmark)
        {
            this.m_name      = bookmark.name;
            this.description = bookmark.description;
        }

        this.initialName        = this.name;
        this.initialDescription = this.description;
    }

    updatePristine()
    {
        this.pristine = false;

        if (!UtilsService.equivalentStrings(this.name, this.initialName)) return;
        if (!UtilsService.equivalentStrings(this.description, this.initialDescription)) return;

        this.pristine = true;
    }

    async save()
    {
        let bookmarkSaved = await this.data.saveFn(this.name, this.description);
        this.dialogRef.close(bookmarkSaved);
    }

    public static open(comp: BaseComponent,
                       editing: boolean,
                       staticName: string,
                       saveFn: BookmarkSaveFn,
                       bookmark?: Models.BookmarkConfiguration): Promise<Models.BookmarkConfiguration>
    {
        let dialogCfg  = new BookmarkConfig(editing, staticName, saveFn, bookmark);
        let overlayCfg = OverlayConfig.newInstance({width: LAYOUT_WIDTH_SM + "px"});

        return OverlayComponent.open(comp, BookmarkEditorDialogComponent, {
            data  : dialogCfg,
            config: overlayCfg
        });
    }
}

class BookmarkConfig
{
    constructor(public readonly editing: boolean,
                public readonly staticName: string,
                public readonly saveFn: BookmarkSaveFn,
                public readonly bookmark?: Models.BookmarkConfiguration)
    {}
}

export type BookmarkSaveFn = (name: string,
                              description: string) => Promise<Models.BookmarkConfiguration>;
