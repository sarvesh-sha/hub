import {Component, EventEmitter, Input, Output, SimpleChanges, ViewChild} from "@angular/core";

import {ImagePreviewTypeMeta} from "app/shared/image/image-preview.component";

import {BaseComponent} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";
import {SelectComponent} from "framework/ui/forms/select.component";

@Component({
               selector   : "o3-image-preview-selector",
               templateUrl: "./image-preview-selector.component.html",
               styleUrls  : [
                   "./image-preview-selector.component.scss"
               ]
           })
export class ImagePreviewSelectorComponent<T> extends BaseComponent
{
    @Input() lookup: (selection: T) => ImagePreviewTypeMeta;
    @Input() options: ControlOption<T>[];
    @Input() selection: T;
    @Input() placeholder: string = "Select an option...";

    @Output() selectionChange: EventEmitter<T> = new EventEmitter();

    @ViewChild("test_selector") test_selector: SelectComponent<T>;

    public meta: ImagePreviewTypeMeta = null;

    public ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);
        this.updateMeta();
    }

    public isReady(): boolean
    {
        return !!this.meta && !!this.selection;
    }

    public onSelection(selection: T)
    {
        this.selection = selection;
        this.selectionChange.emit(selection);
        this.updateMeta();
    }

    public updateMeta()
    {
        this.meta = this.lookup(this.selection) || null;
    }
}
