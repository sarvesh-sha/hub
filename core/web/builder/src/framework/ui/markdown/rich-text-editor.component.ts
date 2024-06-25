import {Component, ElementRef, EventEmitter, Injector, Input, OnDestroy, OnInit, Output, ViewChild, ViewEncapsulation} from "@angular/core";

import {BaseComponent} from "framework/ui/components";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {FileReadType, ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {ComponentBlotDefinition, createComponentBlot} from "framework/ui/markdown/formats/basic-component";

import Quill from "quill";
import * as ImageResizeModule from "quill-image-resize";

class o3Image extends Quill.import("formats/image")
{
    static match()
    {
        return true;
    }
}

Quill.register(o3Image, true);
Quill.register("modules/imageResize", ImageResizeModule.default);

let fontClass       = Quill.import("attributors/class/size");
fontClass.whitelist = [
    "small",
    "12",
    "16",
    "large",
    "20",
    "24",
    "28",
    "huge",
    "36"
];
Quill.register(fontClass, true);

@Component({
               selector     : "o3-rich-text-editor",
               templateUrl  : "./rich-text-editor.component.html",
               styleUrls    : ["./rich-text-editor.component.scss"],
               encapsulation: ViewEncapsulation.None
           })
export class RichTextEditorComponent implements OnInit,
                                                OnDestroy
{
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_hideToolbar: boolean | "";

    private static componentBlotDefinitions = new Set<ComponentBlotDefinition<any, any>>();

    public static RegisterComponentBlot(blot: ComponentBlotDefinition<any, any>)
    {
        this.componentBlotDefinitions.add(blot);
    }

    @ViewChild("toolbar", {static: true}) toolbar: ElementRef<HTMLElement>;
    @ViewChild("container", {static: true}) container: ElementRef<HTMLElement>;

    private m_quill: any;

    @Input() host: BaseComponent;

    private m_backgroundColor: string;
    @Input() set backgroundColor(color: string)
    {
        this.m_backgroundColor = color;
    }

    get backgroundColor(): string
    {
        return this.m_backgroundColor || "#ffffff";
    }

    @Input() public data: any[];
    @Output() public dataChange = new EventEmitter<any[]>();

    @Input() @CoerceBoolean() hideToolbar: boolean;

    @Input() set disabled(disabled: boolean)
    {
        this.m_disabled = disabled;
        if (this.m_quill)
        {
            if (disabled)
            {
                this.m_quill.disable();
            }
            else
            {
                this.m_quill.enable();
            }
        }
    }

    get disabled(): boolean
    {
        return this.m_disabled;
    }

    private m_disabled: boolean;

    private m_changeHandler = () =>
    {
        this.data = this.m_quill.getContents().ops;
        this.dataChange.emit(this.data);
    };

    constructor(private inj: Injector) {}

    ngOnInit()
    {
        this.render();
    }

    ngOnDestroy()
    {
        this.m_quill.off("text-change", this.m_changeHandler);

        // Delete contents of editor, character by character, so all embeds get properly destroyed
        let length = this.m_quill.getLength();
        for (let i = 0; i < length; i++)
        {
            this.m_quill.deleteText(0, 1);
        }

        this.m_quill = null;
    }

    insertComponentBlot<V>(blot: ComponentBlotDefinition<any, V>,
                           data?: V)
    {
        let range = this.m_quill.getSelection(true);
        if (range)
        {
            this.m_quill.insertEmbed(range.index, blot.blotName, data || true, Quill.sources.USER);
            this.m_quill.setSelection(range.index + 1, Quill.sources.SILENT);
        }
    }

    private render()
    {
        for (let componentBlot of RichTextEditorComponent.componentBlotDefinitions)
        {
            Quill.register(createComponentBlot(this.inj, componentBlot), true);
        }

        this.m_quill = new Quill(this.container.nativeElement, {
            theme  : "snow",
            modules: {
                toolbar    : this.hideToolbar ? false : this.toolbar.nativeElement,
                imageResize: {
                    modules: [
                        "Resize",
                        "DisplaySize"
                    ]
                }
            }
        });

        if (this.host)
        {
            this.m_quill.getModule("toolbar")
                .addHandler("image", () => this.uploadImage());
        }

        if (this.data) this.m_quill.setContents(this.data);

        this.m_quill.on("text-change", this.m_changeHandler);

        if (this.m_disabled)
        {
            this.m_quill.disable();
        }
    }

    private async uploadImage()
    {
        let imgSrc = await ImportDialogComponent.open(this.host, "Import Image", {
            returnRawBlobs: () => false,
            parseFile     : async (base64Logo: string) => typeof base64Logo === "string" ? base64Logo : null
        }, FileReadType.asDataURL);

        if (imgSrc)
        {
            let selection = this.m_quill.getSelection();
            let index     = selection ? selection.index : this.m_quill.getLength();
            this.m_quill.insertEmbed(index, "image", imgSrc);
        }
    }
}
