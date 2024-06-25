import {ConnectedPosition, OverlayPositionBuilder} from "@angular/cdk/overlay";
import {ComponentPortal, DomPortalOutlet} from "@angular/cdk/portal";
import {ApplicationRef, Component, ComponentFactoryResolver, Directive, ElementRef, HostListener, Injector, Input, TemplateRef, ViewChild} from "@angular/core";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {Future} from "framework/utils/concurrency";

@Component({
               selector: "o3-tooltip",
               template: `
                   <o3-overlay #tooltip [config]="config">
                       <div (mouseenter)="hovering = true" (mouseleave)="hovering=false">
                           <ng-container [ngTemplateOutlet]="template"></ng-container>
                       </div>
                   </o3-overlay>`
           })
export class TooltipComponent
{
    @Input() template: TemplateRef<any>;
    @Input() config: OverlayConfig;

    public hovering = false;

    @ViewChild(OverlayComponent, {static: true}) overlay: OverlayComponent;
}

@Directive({
               selector: "[o3Tooltip]"
           })
export class TooltipComponentDirective
{
    @Input("o3Tooltip") template: TemplateRef<any>;

    @Input("o3TooltipDisabled") disabled: boolean = false;

    @Input("o3TooltipDelay") delay: number = 0;

    @Input("o3TooltipPosition") position: "top" | "bottom" | "left" | "right" = "bottom";

    private m_overlay: TooltipComponent;
    private m_outlet: DomPortalOutlet;

    private m_open: boolean = false;

    constructor(private m_element: ElementRef,
                private m_positionBuilder: OverlayPositionBuilder,
                private m_cfr: ComponentFactoryResolver,
                private m_appRef: ApplicationRef,
                private m_inj: Injector)
    {}

    public ngOnInit()
    {

        let config    = OverlayConfig.dropdown({
                                                   coverAnchorWhenDisplayed: false,
                                                   showBackdrop            : false,
                                                   positionStrategy        : this.m_positionBuilder.flexibleConnectedTo(this.m_element)
                                                                                 .withPush(false)
                                                                                 .withGrowAfterOpen(true)
                                                                                 .withPositions(this.getPositions())
                                               });
        let portal    = new ComponentPortal(TooltipComponent);
        this.m_outlet = new DomPortalOutlet(this.m_element.nativeElement, this.m_cfr, this.m_appRef, this.m_inj);
        let ref       = portal.attach(this.m_outlet);


        this.m_overlay          = ref.instance;
        this.m_overlay.template = this.template;
        this.m_overlay.config   = config;
    }

    public ngOnDestroy()
    {
        this.m_outlet.dispose();
    }

    @HostListener("mouseenter")
    public async showTooltip()
    {
        if (this.disabled || this.m_open) return;

        this.m_open = true;

        if (this.delay)
        {
            await Future.delayed(this.delay);

            if (!this.m_open) return;
        }

        this.m_overlay.overlay.toggleOverlay();
    }

    @HostListener("mouseleave")
    public hideTooltip()
    {
        if (this.m_overlay.hovering) return;

        this.m_open = false;
        this.m_overlay.overlay.closeOverlay();
    }

    private getPositions(): ConnectedPosition[]
    {
        const bottom: ConnectedPosition = {
            panelClass: "mt-2",
            originX   : "center",
            originY   : "bottom",
            overlayX  : "center",
            overlayY  : "top"
        };

        const top: ConnectedPosition = {
            panelClass: "mb-2",
            originX   : "center",
            originY   : "top",
            overlayX  : "center",
            overlayY  : "bottom"
        };

        const left: ConnectedPosition = {
            panelClass: "mr-2",
            originX   : "start",
            originY   : "center",
            overlayX  : "end",
            overlayY  : "center"
        };

        const right: ConnectedPosition = {
            panelClass: "ml-2",
            originX   : "end",
            originY   : "center",
            overlayX  : "start",
            overlayY  : "center"
        };

        switch (this.position || "bottom")
        {
            case "bottom":
                return [
                    bottom,
                    top
                ];

            case "top":
                return [
                    top,
                    bottom
                ];

            case "left":
                return [
                    left,
                    right
                ];

            case "right":
                return [
                    right,
                    left
                ];
        }
    }
}
