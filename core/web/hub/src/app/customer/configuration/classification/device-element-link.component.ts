import {ChangeDetectionStrategy, Component, Input} from "@angular/core";

@Component({
               selector       : "o3-device-element-link",
               template       : `
                   <a [href]="getUrl()" target="_blank">
                       <mat-icon>chevron_right</mat-icon>
                   </a>`,
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class DeviceElementLinkComponent
{
    @Input()
    public sysId: string;

    @Input()
    public parentSysId: string;

    getUrl(): string
    {
        return `/#/devices/device/${this.parentSysId}/element/${this.sysId}`;
    }
}
