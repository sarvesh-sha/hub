import {InjectionToken} from "@angular/core";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {Observable, Subject, Subscription} from "rxjs";

export const OVERLAY_DATA = new InjectionToken<any>("OVERLAY_DATA");

export interface OverlayDialogConfig<T>
{
    config?: OverlayConfig;
    data: T;
}

export class OverlayDialogRef<R>
{
    private m_afterClose = new Subject<R>();
    private closeSub: Subscription;

    constructor(public component: OverlayComponent)
    {
        this.closeSub = this.component.close.subscribe(undefined, undefined, () => this.closed());
    }

    afterClose(): Observable<R>
    {
        return this.m_afterClose.asObservable();
    }

    close(result?: R)
    {
        this.component.closeOverlay();
        this.closed(result);
    }

    closed(result?: R)
    {
        this.closeSub.unsubscribe();
        this.m_afterClose.next(result);
        this.m_afterClose.complete();
    }
}
