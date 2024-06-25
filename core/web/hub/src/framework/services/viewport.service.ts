import {BreakpointObserver} from "@angular/cdk/layout";
import {Injectable} from "@angular/core";
import {CommonLayout, LAYOUT_WIDTH_SM} from "framework/ui/layout";
import {BehaviorSubject, Observable} from "rxjs";

/**
 * Viewport service
 */
@Injectable({ providedIn: "root" })
export class ViewportService
{
    private m_width: number;
    private m_height: number;
    private m_changed: boolean = true;

    private readonly mobileBreakpoint: number = LAYOUT_WIDTH_SM;

    public isMobile: Observable<boolean>;

    constructor(private layout: CommonLayout,
                private breakpointObserver: BreakpointObserver)
    {
        // Read initial size
        this.read();

        // Subscribe to all future window changes
        this.layout.contentSizeChanged.subscribe(() => this.m_changed = true);

        // Set up mobile breakpoint observer
        let subject   = new BehaviorSubject<boolean>(this.isMobileSized());
        this.isMobile = subject.asObservable();
        this.breakpointObserver.observe(`(max-width: ${this.mobileBreakpoint}px)`)
            .subscribe((handsetBreakpoint) => subject.next(handsetBreakpoint.matches));
    }

    get width(): number
    {
        if (this.m_changed) this.read();
        return this.m_width;
    }

    get height(): number
    {
        if (this.m_changed) this.read();
        return this.m_height;
    }

    private read()
    {
        this.m_width   = window.document.body.clientWidth;
        this.m_height  = window.document.body.clientHeight;
        this.m_changed = false;
    }

    private isMobileSized(): boolean
    {
        return this.width <= this.mobileBreakpoint;
    }
}
