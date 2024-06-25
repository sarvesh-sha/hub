import {Clipboard} from "@angular/cdk/clipboard";
import {Component, EventEmitter, Injector, Input, Output} from "@angular/core";
import {BaseComponent} from "framework/ui/components";

@Component({
               selector   : "o3-clipboard-copy",
               templateUrl: "./clipboard-copy.component.html",
               styleUrls  : ["./clipboard-copy.component.scss"]
           })
export class ClipboardCopyComponent extends BaseComponent
{
    @Input() copyValue: string;

    @Output() copied = new EventEmitter<void>();

    constructor(inj: Injector,
                private clipboard: Clipboard)
    {
        super(inj);
    }

    copy()
    {
        const pending         = this.clipboard.beginCopy(this.copyValue);
        let remainingAttempts = 3;
        const attempt         = () =>
        {
            const success = pending.copy();
            if (success)
            {
                this.copied.emit();
            }
            if (!success && --remainingAttempts)
            {
                setTimeout(attempt);
            }
            else
            {
                pending.destroy();
            }
        };
        attempt();
    }
}
