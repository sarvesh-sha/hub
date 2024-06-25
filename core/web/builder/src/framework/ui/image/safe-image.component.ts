import {ChangeDetectionStrategy, Component, Input} from "@angular/core";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";

@Component({
               selector       : "o3-safe-image[data]",
               templateUrl    : "./safe-image.component.html",
               styleUrls      : ["./safe-image.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class SafeImageComponent
{
    sanitizedData: SafeUrl;

    @Input() set data(data: string)
    {
        this.sanitizedData = data && this.sanitizer.bypassSecurityTrustUrl(data);
    }

    @Input() alt: string;

    constructor(private sanitizer: DomSanitizer)
    {
    }
}
