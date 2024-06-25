import {AfterViewInit, Component, Directive, ElementRef, Injector, Input, Renderer2} from "@angular/core";
import {BaseComponent} from "framework/ui/components";

@Directive({
               selector: "[routableViewSource]"
           })
export class RoutableViewSourceDirective implements AfterViewInit
{
    public element: any;
    public parent: any;
    public activePortal: RoutableViewPortalComponent = null;

    constructor(private ref: ElementRef,
                private renderer: Renderer2)
    {}

    public ngAfterViewInit()
    {
        this.element = this.ref.nativeElement;
        this.parent  = this.ref.nativeElement.parentNode;
    }

    public get isProjected(): boolean
    {
        return !!this.activePortal;
    }

    public project(portal: RoutableViewPortalComponent)
    {
        if (this.isProjected) return;

        this.activePortal = portal;
        this.renderer.removeChild(this.parent, this.element);
        this.renderer.appendChild(this.activePortal.parent, this.element);
    }

    public recall()
    {
        if (!this.isProjected) return;

        this.renderer.removeChild(this.activePortal.parent, this.element);
        this.renderer.appendChild(this.parent, this.element);
        this.activePortal = null;
    }
}

@Component({
               selector: "o3-routable-view-portal",
               template: ""
           })
export class RoutableViewPortalComponent extends BaseComponent
{
    public parent: any;

    @Input() source: RoutableViewSourceDirective = null;

    constructor(private element: ElementRef,
                private renderer: Renderer2,
                inj: Injector)
    {
        super(inj);
        this.parent = element.nativeElement;
    }

    public ngOnDestroy()
    {
        this.release();
    }

    private get isActive(): boolean
    {
        return this.source?.activePortal === this;
    }

    public capture()
    {
        if (this.isActive) return;

        this.source.recall();
        this.source.project(this);
    }

    public release()
    {
        if (!this.isActive) return;

        this.source.recall();
    }
}
