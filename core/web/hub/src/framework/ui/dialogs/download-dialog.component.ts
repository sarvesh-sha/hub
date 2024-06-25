import {Component, Inject, OnInit} from "@angular/core";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";

import {BaseComponent} from "framework/ui/components";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./download-dialog.component.html",
               styleUrls  : ["./download-dialog.component.scss"]
           })
export class DownloadDialogComponent implements OnInit
{
    urlForDownload: SafeUrl;
    hasGenerator: boolean;
    progressMessage: string[];
    progressPercent: number;
    isDeterminate: boolean;
    downloadEmpty: boolean;

    constructor(public dialogRef: OverlayDialogRef<void>,
                private sanitizer: DomSanitizer,
                @Inject(OVERLAY_DATA) public data: DialogConfig<any>)
    {
    }

    public static open<T>(comp: BaseComponent,
                          title: string,
                          fileName: string,
                          data: T): Promise<void>
    {
        let cfg  = new DialogConfig<T>(title, fileName);
        cfg.data = data;

        return this.openDialog(comp, cfg);
    }

    public static openWithUrl<T>(comp: BaseComponent,
                                 title: string,
                                 fileName: string,
                                 url: T): Promise<void>
    {
        let cfg = new DialogConfig<T>(title, fileName);
        cfg.url = url;

        return this.openDialog(comp, cfg);
    }

    public static openWithGenerator<T>(comp: BaseComponent,
                                       title: string,
                                       fileName: string,
                                       generator: DownloadGenerator): Promise<void>
    {
        let cfg       = new DialogConfig<T>(title, fileName);
        cfg.generator = generator;

        return this.openDialog(comp, cfg);
    }

    private static openDialog(comp: BaseComponent,
                              cfg: DialogConfig<any>): Promise<void>
    {
        return OverlayComponent.open(comp, DownloadDialogComponent, {
            data  : cfg,
            config: OverlayConfig.importExport()
        });
    }

    static fileName(prefix: string,
                    extension: FileExtension = ".json"): string
    {
        return `${prefix}__${window.location.hostname}__${MomentHelper.fileNameFormat()}${extension}`;
    }

    async ngOnInit()
    {
        if (this.data.url)
        {
            this.urlForDownload = this.data.url;
        }
        else
        {
            let results: DownloadResults;

            let generator = this.data.generator;
            if (generator)
            {
                this.hasGenerator  = true;
                this.isDeterminate = generator.isDeterminate();
                while (true)
                {
                    let finished = await generator.makeProgress(this);

                    this.progressPercent = generator.getProgressPercent();
                    let progressMessage  = generator.getProgressMessage();
                    if (progressMessage)
                    {
                        this.progressMessage = progressMessage.split("\n");
                    }
                    else
                    {
                        this.progressMessage = null;
                    }

                    if (finished)
                    {
                        this.progressMessage = ["Generating file..."];

                        results = await generator.getResults(this.data.fileName);
                        if (!results)
                        {
                            this.downloadEmpty = true;
                            return;
                        }
                        break;
                    }

                    await generator.sleepForProgress();
                }
            }
            else if (typeof this.data.data === "string")
            {
                results = {text: this.data.data};
            }
            else
            {
                results = {text: JSON.stringify(this.data.data, null, "  ")};
            }

            if (results.url)
            {
                this.urlForDownload = this.sanitizer.bypassSecurityTrustResourceUrl(results.url);
            }
            else
            {
                let blob: Blob;

                if (results.blob)
                {
                    blob = results.blob;
                }
                else if (results.lines)
                {
                    blob = new Blob([results.lines.join("\n")], {type: "text/plain;charset=UTF-8"});
                }
                else
                {
                    blob = new Blob([results.text], {type: "text/plain;charset=UTF-8"});
                }

                this.urlForDownload = this.sanitizer.bypassSecurityTrustResourceUrl(window.URL.createObjectURL(blob));
            }
        }
    }

    isDownloadReady(): boolean
    {
        return this.urlForDownload !== undefined;
    }

    indeterminatePercent(): number
    {
        return this.isDownloadReady() ? 100 : 0;
    }
}

class DialogConfig<T>
{
    constructor(public title: string,
                public fileName: string)
    {
    }

    data: T;

    url: SafeUrl;

    generator: DownloadGenerator;
}

export interface DownloadGenerator
{
    makeProgress(dialog: DownloadDialogComponent): Promise<boolean>;

    sleepForProgress(): Promise<void>;

    getProgressMessage(): string;

    getProgressPercent(): number;

    isDeterminate(): boolean;

    getResults(fileName: string): Promise<DownloadResults>;
}

export interface DownloadResults
{
    text?: string;
    lines?: string[];
    blob?: Blob;
    url?: string;
}

export type FileExtension = ".json" | ".txt" | ".csv" | ".zip" | ".xlsx";
