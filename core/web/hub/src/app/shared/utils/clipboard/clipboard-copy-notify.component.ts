import {Component, Input} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";

@Component({
               selector: "o3-clipboard-copy-notify[copyValue]",
               template: `
                   <o3-clipboard-copy [copyValue]="copyValue" (copied)="notify()"></o3-clipboard-copy>`
           })
export class ClipboardCopyNotifyComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() copyValue: string;

    @Input() showNotification: boolean = true;

    @Input() notificationMessage: string;

    notify()
    {
        if (this.showNotification)
        {
            this.app.framework.errors.success(this.notificationMessage || this.copyValue + " copied to clipboard.", -1);
        }
    }
}
