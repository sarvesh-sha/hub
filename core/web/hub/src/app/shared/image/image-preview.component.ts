import {ChangeDetectionStrategy, Component, ElementRef, Injector, Input, RendererFactory2, SimpleChanges, ViewChild} from "@angular/core";

import {BaseApplicationComponent} from "app/services/domain/base.service";
import {SettingsService} from "app/services/domain/settings.service";

import {Lookup} from "framework/services/utils.service";

@Component({
               selector       : "o3-image-preview",
               templateUrl    : "./image-preview.component.html",
               styleUrls      : ["./image-preview.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ImagePreviewComponent extends BaseApplicationComponent
{
    private m_ready: boolean = false;

    @Input() preview: ImagePreviewMeta;

    @ViewChild("label") label: ElementRef<HTMLElement>;
    @ViewChild("description") description: ElementRef<HTMLElement>;

    public entity: MockEntity;

    constructor(inj: Injector,
                private element: ElementRef,
                private renderer: RendererFactory2)
    {
        super(inj);

        this.renderer.createRenderer(null, null)
            .listen("window", "resize", () => this.detectChanges());
    }

    ngAfterViewInit()
    {
        super.ngAfterViewInit();

        // Flag as ready and renderable
        this.m_ready = true;
    }

    ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        // (Re)initialize mock widget
        if (changes.preview) this.init();
    }

    public imageStyles(): Lookup<string>
    {
        let containerW   = this.element?.nativeElement?.clientWidth | 0;
        let naturalW     = this.entity?.image?.naturalWidth | 0;
        let containerH   = this.element?.nativeElement?.clientHeight | 0;
        let labelH       = this.label?.nativeElement?.clientHeight | 0;
        let descriptionH = this.description?.nativeElement?.clientHeight | 0;
        let naturalH     = this.entity?.image?.naturalHeight | 0;
        let compositeH   = containerH - (labelH + descriptionH);

        let w = Math.min(containerW, (naturalW ? naturalW : containerW));
        let h = Math.min(compositeH, (naturalH ? naturalH : compositeH));

        return {
            "max-width" : (w - 40) + "px",
            "max-height": h + "px"
        };
    }

    public canShowPreview(): boolean
    {
        return this.entity?.loaded && this.entity?.hasImage && this.m_ready;
    }

    private async init()
    {
        this.entity = new MockEntity(this.preview, this.app.domain.settings);
        this.detectChanges();
        await this.entity.load();

        this.detectChanges();
    }
}

export class MockEntity
{
    public url: string       = null;
    public loaded: boolean   = false;
    public hasImage: boolean = false;
    public image: HTMLImageElement;

    constructor(public preview: ImagePreviewMeta,
                public settings: SettingsService)
    {
        // Set the preview image URL
        this.url = "/assets/previews/" + preview.file;
    }

    public async load(): Promise<void>
    {
        // Clear image and reset state
        this.image    = null;
        this.loaded   = false;
        this.hasImage = false;

        return new Promise<void>((resolve,
                                  reject) =>
                                 {
                                     this.loaded = false;
                                     this.image  = new Image();

                                     // Register listeners
                                     this.image.addEventListener("load", (e) =>
                                     {
                                         this.loaded   = true;
                                         this.hasImage = true;
                                         resolve();
                                     });
                                     this.image.addEventListener("error", (e) =>
                                     {
                                         this.loaded   = true;
                                         this.hasImage = false;
                                         resolve();
                                     });

                                     // Start image load
                                     this.image.src = this.url;
                                 });
    }
}

export interface ImagePreviewMeta
{
    file: string;
    label: string;
    description: string;
}

export interface ImagePreviewTypeMeta
{
    description: string;
    examples: ImagePreviewMeta[];
}
