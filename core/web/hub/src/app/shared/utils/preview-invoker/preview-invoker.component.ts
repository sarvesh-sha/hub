import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {Observable, Subject} from "rxjs";

@Component({
               selector       : "o3-preview-invoker",
               templateUrl    : "./preview-invoker.component.html",
               styleUrls      : ["./preview-invoker.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class PreviewInvokerComponent extends SharedSvc.BaseApplicationComponent
{
    private static m_onToggle: Subject<string> = new Subject<string>();
    public static onToggle: Observable<string> = PreviewInvokerComponent.m_onToggle.asObservable();

    @Input() id: string                    = null;
    @Output() toggle: EventEmitter<string> = new EventEmitter<string>();

    doToggle(event: MouseEvent)
    {
        event.stopPropagation();
        event.preventDefault();

        this.toggle.emit(this.id);
        PreviewInvokerComponent.m_onToggle.next(this.id);
    }
}
