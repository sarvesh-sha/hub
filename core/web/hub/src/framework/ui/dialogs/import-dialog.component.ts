import {Component, ElementRef, Inject, ViewChild} from "@angular/core";

import {BaseComponent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               templateUrl: "./import-dialog.component.html",
               styleUrls  : ["./import-dialog.component.scss"]
           })
export class ImportDialogComponent<T>
{
    @ViewChild("selectedFiles", {static: true}) selectedFiles: ElementRef;

    validContent: T;
    fileName: string;
    error: string;

    constructor(public dialogRef: OverlayDialogRef<T>,
                @Inject(OVERLAY_DATA) public data: DialogConfig<T>)
    {
    }

    public static open<T>(comp: BaseComponent,
                          title: string,
                          handler: ImportHandler<T>,
                          readType: FileReadType = FileReadType.asText): Promise<T>
    {
        let dialogCfg      = new DialogConfig();
        dialogCfg.title    = title;
        dialogCfg.handler  = handler;
        dialogCfg.readType = readType;

        return OverlayComponent.open(comp, ImportDialogComponent, {
            data  : dialogCfg,
            config: OverlayConfig.importExport()
        });
    }

    handleFiles()
    {
        if (this.selectedFiles?.nativeElement)
        {
            let files = <FileList>this.selectedFiles.nativeElement.files;
            if (files.length > 0)
            {
                let file      = files[0];
                this.fileName = file.name;

                if (this.data.handler.returnRawBlobs())
                {
                    this.validContent = <T><any>file;
                    this.error        = "";
                }
                else
                {
                    let reader    = new FileReader();
                    reader.onload = async (e) =>
                    {
                        try
                        {
                            let content = await this.data.handler.parseFile(reader.result, this.fileName);
                            if (this.data.handler.validator)
                            {
                                this.error = this.data.handler.validator(content) || "";
                            }

                            if (!this.error)
                            {
                                this.validContent = content;
                            }
                        }
                        catch (e)
                        {
                            console.error(e);
                            this.error = e.message;
                        }
                    };

                    this.read(reader, file);
                }
            }
        }
    }

    private read(reader: FileReader,
                 blob: Blob)
    {
        switch (this.data.readType)
        {
            case FileReadType.asText:
                reader.readAsText(blob);
                break;

            case FileReadType.asDataURL:
                reader.readAsDataURL(blob);
                break;
        }
    }
}

export enum FileReadType
{
    asText    = "asText",
    asDataURL = "asDataURL"
}

class DialogConfig<T>
{
    title: string;

    handler: ImportHandler<T>;

    readType: FileReadType;
}

export interface ImportHandler<T>
{
    returnRawBlobs(): boolean;

    parseFile(contents: string | ArrayBuffer,
              fileName: string): Promise<T>;

    // return error message
    validator?(result: T): string;
}
