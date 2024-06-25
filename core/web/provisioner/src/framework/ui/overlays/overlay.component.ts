import {animate, state, style, transition, trigger} from "@angular/animations";
import {CDK_DRAG_CONFIG, CdkDrag, CdkDragHandle} from "@angular/cdk/drag-drop";
import {Point} from "@angular/cdk/drag-drop/drag-ref";
import {CdkOverlayOrigin, ComponentType, ConnectedPosition, Overlay, OverlayConfig as CdkOverlayConfig, OverlayRef, PositionStrategy} from "@angular/cdk/overlay";
import {CdkPortal, ComponentPortal, Portal, TemplatePortal} from "@angular/cdk/portal";
import {Component, ContentChild, ElementRef, EventEmitter, Injector, Input, OnInit, Output, TemplateRef, Type, ViewChild, ViewContainerRef} from "@angular/core";

import {UtilsService} from "framework/services/utils.service";
import {BaseComponent, fromEvent} from "framework/ui/components";
import {OVERLAY_DATA, OverlayDialogConfig, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {Future} from "framework/utils/concurrency";

@Component({
               selector   : "o3-overlay",
               styleUrls  : ["./overlay.component.scss"],
               templateUrl: "./overlay.component.html",
               providers  : [
                   {
                       provide : CDK_DRAG_CONFIG,
                       useValue: {
                           dragStartThreshold             : 0,
                           pointerDirectionChangeThreshold: 5
                       }
                   }
               ],
               animations : [
                   trigger("transformDropdown", [
                       state("void", style({
                                               transform: "scaleY(0)",
                                               minWidth : "100%",
                                               opacity  : 0
                                           })),
                       state("enter", style({
                                                opacity  : 1,
                                                minWidth : "100%",
                                                transform: "scaleY(1)"
                                            })),
                       state("leave", style({
                                                transform: "scaleY(0)",
                                                minWidth : "100%",
                                                opacity  : 0
                                            })),
                       transition("* => enter", animate("100ms ease-out")),
                       transition("enter => *", animate("100ms ease-in"))
                   ]),
                   trigger("transformDialog", [
                       state("void", style({
                                               transform: "translateY(25%) scale(0.9)",
                                               opacity  : 0
                                           })),
                       state("enter", style({
                                                transform: "translateY(0) scale(1)",
                                                opacity  : 1
                                            })),
                       state("leave", style({
                                                transform: "translateY(25%)",
                                                opacity  : 0
                                            })),
                       transition("* => enter", animate("100ms ease-out")),
                       transition("enter => *", animate("100ms ease-in"))
                   ])
               ]
           })
export class OverlayComponent extends BaseComponent implements OnInit
{
    static readonly gripperHeight = 16;
    static readonly gripperWidth  = 32;

    private m_cdkConfig: CdkOverlayConfig;
    private get cdkConfig(): CdkOverlayConfig
    {
        if (!this.m_cdkConfig) this.buildConfig();

        return this.m_cdkConfig;
    }

    private m_config: OverlayConfig;
    @Input() set config(config: OverlayConfig)
    {
        this.m_config = config;
        this.buildConfig();
    }

    get config(): OverlayConfig
    {
        return this.m_config;
    }

    @Input() overlayOrigin: CdkOverlayOrigin;

    @Output() open  = new EventEmitter<void>();
    @Output() close = new EventEmitter<void>();

    @ViewChild(TemplateRef, {static: true}) templateRef: TemplateRef<any>;
    @ViewChild(CdkDrag) overlayElement: CdkDrag;
    @ViewChild(CdkDragHandle, {read: ElementRef}) handle: ElementRef;

    @ContentChild(CdkPortal, {static: true}) set contentPortal(portal: CdkPortal)
    {
        this.portal = portal;
    }

    portal: Portal<any>;

    overlay: OverlayRef;

    animationState: "void" | "enter" | "leave" = "void";

    overlayId: string;

    trapFocus = false;

    private m_isDialog: boolean;

    private m_portal: TemplatePortal;

    get isOpen(): boolean
    {
        return this.overlay?.hasAttached();
    }

    private dragOffsetX: number;
    private dragOffsetY: number;
    dragConstrainFn = (point: Point) =>
    {
        point.x = UtilsService.clamp(this.dragOffsetX, window.innerWidth - (OverlayComponent.gripperWidth - this.dragOffsetX), point.x);
        point.y = UtilsService.clamp(this.dragOffsetY, window.innerHeight - (OverlayComponent.gripperHeight - this.dragOffsetY), point.y);

        return point;
    };

    dragPosition = {
        x: 0,
        y: 0
    };

    constructor(private viewContainerRef: ViewContainerRef,
                private overlayService: Overlay,
                inj: Injector)
    {
        super(inj);
    }

    public static openRef<D, R>(base: BaseComponent,
                                componentRef: ComponentType<any>,
                                config: OverlayDialogConfig<D>): OverlayDialogRef<R>
    {
        if (!config.config)
        {
            config.config = new OverlayConfig();
        }

        let overlayComponent = OverlayComponent.createOverlayComponent(base.injector, config);

        let overlayDialogRef = new OverlayDialogRef<R>(overlayComponent);
        overlayComponent.attachPortal(config, overlayDialogRef, componentRef);

        return overlayDialogRef;
    }

    public static open<D, R>(base: BaseComponent,
                             componentRef: ComponentType<any>,
                             config: OverlayDialogConfig<D>): Promise<R>
    {
        let ref = this.openRef<D, R>(base, componentRef, config);
        return this.waitForClose(ref);
    }

    public static waitForClose<R>(ref: OverlayDialogRef<R>): Promise<R>
    {
        let result = new Future<R>();
        ref.afterClose()
           .subscribe((value) =>
                      {
                          result.resolve(value);
                      });
        return result;
    }

    private static createOverlayComponent(injector: Injector,
                                          config: OverlayDialogConfig<any>): OverlayComponent
    {
        let overlay: Overlay = injector.get(Overlay);

        let overlayPortal    = new ComponentPortal(OverlayComponent, null, injector);
        let overlayRef       = overlay.create();
        let overlayComponent = overlayRef.attach(overlayPortal).instance;

        overlayComponent.config     = config.config;
        overlayComponent.m_isDialog = true;
        overlayComponent.close.subscribe(() =>
                                         {
                                             overlayRef.detach();
                                         });

        return overlayComponent;
    }

    private attachPortal(config: OverlayDialogConfig<any>,
                         overlayDialogRef: OverlayDialogRef<any>,
                         componentRef: Type<any>): void
    {
        let componentInjector = Injector.create({
                                                    providers: [
                                                        {
                                                            provide : OVERLAY_DATA,
                                                            useValue: config.data
                                                        },
                                                        {
                                                            provide : OverlayDialogRef,
                                                            useValue: overlayDialogRef
                                                        },
                                                        {
                                                            provide : OverlayComponent,
                                                            useValue: this
                                                        }
                                                    ],
                                                    parent   : this.injector
                                                });

        this.portal = new ComponentPortal(componentRef, null, componentInjector);
    }

    ngOnInit()
    {
        super.ngOnInit();

        this.m_portal = new TemplatePortal(this.templateRef, this.viewContainerRef);

        if (this.m_isDialog)
        {
            this.toggleOverlay();
        }
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();

        this.pullIntoView();
    }

    ngOnDestroy()
    {
        super.ngOnDestroy();
        this.destroyOverlay();
    }

    onAnimationDone()
    {
        if (this.animationState === "leave")
        {
            this.detachOverlay();
        }
    }

    toggleOverlay(overlayOrigin: CdkOverlayOrigin = null)
    {
        if (!this.overlay) this.createOverlay();

        // reset the origin if one is provided
        if (overlayOrigin)
        {
            this.overlayOrigin = overlayOrigin;
            this.buildConfig();
            this.overlay.updatePositionStrategy(this.cdkConfig.positionStrategy);
        }

        if (!this.overlay.hasAttached())
        {
            this.attachOverlay();
        }
        else
        {
            this.detachOverlay();
        }
    }

    closeOverlay()
    {
        if (!this.overlay) return;
        this.detachOverlay();
    }

    setDragConstraints(event: MouseEvent)
    {
        let handleRect   = this.handle.nativeElement.getBoundingClientRect();
        this.dragOffsetX = event.clientX - handleRect.left;
        this.dragOffsetY = event.clientY - handleRect.top;
    }

    resetPosition()
    {
        this.overlayElement?.reset();
    }

    pullIntoView(timeoutDelay?: number)
    {
        if (this.isOpen && this.handle)
        {
            if (isNaN(timeoutDelay))
            {
                this.pullIntoViewHelper();
            }
            else
            {
                setTimeout(this.pullIntoViewHelper.bind(this), timeoutDelay);
            }
        }
    }

    private pullIntoViewHelper()
    {
        let handleRect = this.handle.nativeElement.getBoundingClientRect();

        let needsAdjustment = false;
        let currentFreeDrag = this.overlayElement.getFreeDragPosition();
        let position        = {
            x: currentFreeDrag.x,
            y: currentFreeDrag.y
        };

        if (handleRect.left < 0)
        {
            needsAdjustment = true;
            position.x      = -(window.innerWidth - OverlayComponent.gripperWidth) / 2;
        }
        else if (handleRect.right > window.innerWidth)
        {
            needsAdjustment = true;
            position.x      = (window.innerWidth - OverlayComponent.gripperWidth) / 2;
        }

        let overlayHeight = this.overlayElement.element.nativeElement.clientHeight;
        if (handleRect.top < 0)
        {
            needsAdjustment = true;
            position.y      = (overlayHeight + OverlayComponent.gripperHeight - window.innerHeight) / 2;
        }
        else if (handleRect.bottom > window.innerHeight)
        {
            needsAdjustment = true;
            position.y      = (window.innerHeight + overlayHeight - OverlayComponent.gripperHeight) / 2;
        }

        if (needsAdjustment)
        {
            this.dragPosition = position;
            this.markForCheck();
        }
    }

    private buildConfig()
    {
        if (!this.m_config)
        {
            this.m_config = new OverlayConfig();
            if (this.overlayOrigin) this.m_config.setDropdownDefaults();
        }

        if (this.overlayOrigin)
        {
            this.m_config.positionStrategy = this.overlayService.position()
                                                 .flexibleConnectedTo(this.overlayOrigin.elementRef)
                                                 .withPositions(this.getDropdownPositions())
                                                 .withTransformOriginOn(".o3-overlay--container");
        }
        else if (!this.m_config.positionStrategy)
        {
            this.m_config.positionStrategy = this.overlayService.position()
                                                 .global()
                                                 .centerHorizontally()
                                                 .centerVertically();
        }

        let cdkConfig = new CdkOverlayConfig({
                                                 panelClass      : "overlay-dialog-panel",
                                                 hasBackdrop     : this.m_config.showBackdrop,
                                                 width           : this.m_config.width,
                                                 minWidth        : this.m_config.minWidth,
                                                 maxWidth        : this.m_config.maxWidth,
                                                 height          : this.m_config.height,
                                                 minHeight       : this.m_config.minHeight,
                                                 maxHeight       : this.m_config.maxHeight,
                                                 positionStrategy: this.m_config.positionStrategy
                                             });

        if (this.m_config.isDraggable)
        {
            cdkConfig.hasBackdrop    = false;
            cdkConfig.scrollStrategy = this.overlayService.scrollStrategies.noop();
        }
        else
        {
            cdkConfig.scrollStrategy = this.overlayService.scrollStrategies.block();
        }

        cdkConfig.backdropClass = this.m_config.backdropClass || "cdk-overlay-dark-backdrop";

        this.m_cdkConfig = cdkConfig;
    }

    private getDropdownPositions(): ConnectedPosition[]
    {
        if (this.m_config.coverAnchorWhenDisplayed)
        {
            return [
                // Overlay opens over the anchor
                {
                    originX : "start",
                    originY : "top",
                    overlayX: "start",
                    overlayY: "top"
                },
                {
                    originX : "start",
                    originY : "bottom",
                    overlayX: "start",
                    overlayY: "bottom"
                },
                {
                    originX : "end",
                    originY : "bottom",
                    overlayX: "end",
                    overlayY: "top"
                },
                {
                    originX : "end",
                    originY : "top",
                    overlayX: "end",
                    overlayY: "bottom"
                }
            ];
        }
        else
        {
            return [
                // Overlay opens under the anchor
                {
                    originX : "start",
                    originY : "bottom",
                    overlayX: "start",
                    overlayY: "top"
                },
                {
                    originX : "start",
                    originY : "top",
                    overlayX: "start",
                    overlayY: "bottom"
                },
                {
                    originX : "end",
                    originY : "bottom",
                    overlayX: "end",
                    overlayY: "top"
                },
                {
                    originX : "end",
                    originY : "top",
                    overlayX: "end",
                    overlayY: "bottom"
                }
            ];
        }
    }

    private getOverlayId(): string
    {
        if (this.overlay?.overlayElement?.id) return "#" + this.overlay.overlayElement.id;

        return "";
    }

    private updateSize()
    {
        // Overlay will default to the optimal width for its content.
        let width: string | number;
        let minWidth: string | number = 0;

        if (this.m_config.width) width = this.m_config.width;
        if (this.m_config.minWidth) minWidth = this.m_config.minWidth;

        if (this.overlayOrigin && this.overlayOrigin.elementRef && !minWidth && !width)
        {
            let rect = this.overlayOrigin.elementRef.nativeElement.getBoundingClientRect();
            if (rect && rect.width > 0)
            {
                minWidth = rect.width;
            }
        }

        this.overlay.updateSize({
                                    width   : width,
                                    minWidth: minWidth
                                });
    }

    private attachOverlay()
    {
        if (this.overlayOrigin)
        {
            this.updateSize();
        }

        this.overlay.attach(this.m_portal);

        this.animationState = "enter";

        setTimeout(() =>
                   {
                       this.trapFocus = true;
                       this.detectChanges();
                   });

        if (this.overlay.backdropElement)
        {
            this.subscribeToObservable(fromEvent(this.overlay.backdropElement, "contextmenu"), (event) =>
            {
                if (this.m_config.dismissOnRightClick)
                {
                    //cancel the event
                    event.preventDefault();
                    event.stopPropagation();

                    // close overlay
                    this.closeOverlay();
                }
            });
        }

        this.open.emit();
    }

    private detachOverlay(animate = true)
    {
        if (this.overlay?.hasAttached())
        {
            if (animate && this.m_config.isAnimationsEnabled && this.animationState !== "leave")
            {
                // Once animation is finished we'll detach
                this.animationState = "leave";
            }
            else
            {
                this.trapFocus = false;
                this.overlay.detach();
                this.close.emit();
                this.animationState = "void";
                this.overlay.dispose();
                this.overlay = null;
            }

            this.markForCheck();
        }
    }

    private createOverlay()
    {
        this.overlay   = this.overlayService.create(this.cdkConfig);
        this.overlayId = this.getOverlayId();

        this.subscribeToObservable(this.overlay.keydownEvents(), (event: KeyboardEvent) =>
        {
            if (event.key === "Escape") this.closeOverlay();
        });

        this.subscribeToObservable(this.overlay.backdropClick(), (event: MouseEvent) =>
        {
            if (this.m_config.closableViaBackdrop) this.closeOverlay();
        });
    }

    private destroyOverlay()
    {
        this.detachOverlay(false);

        this.close.complete();
        this.open.complete();
    }
}

export class OverlayConfig
{
    showBackdrop: boolean;

    closableViaBackdrop: boolean = true;

    showCloseButton: boolean;

    isAnimationsEnabled: boolean;

    isDraggable: boolean;

    coverAnchorWhenDisplayed: boolean;

    // applies to dropdown only
    dismissOnRightClick: boolean;

    overlayClass: string;

    width: number | string;

    minWidth: number | string;

    maxWidth: number | string;

    height: number | string;

    minHeight: number | string;

    maxHeight: number | string;

    positionStrategy: PositionStrategy;

    backdropClass: string;

    m_containerClasses: string[] = [];
    set containerClasses(classes: string[])
    {
        if (classes)
        {
            this.m_containerClasses = classes;
        }
    }

    get containerClasses(): string[]
    {
        return this.m_containerClasses;
    }

    constructor()
    {
        this.setDialogDefaults();
    }

    onTopDraggable()
    {
        this.showCloseButton = true;
        this.isDraggable     = true;
        this.m_containerClasses.push("on-top-grip");
    }

    setDialogDefaults()
    {
        this.showBackdrop             = true;
        this.showCloseButton          = false;
        this.isAnimationsEnabled      = true;
        this.isDraggable              = false;
        this.coverAnchorWhenDisplayed = false;
        this.overlayClass             = "";
    }

    setDropdownDefaults()
    {
        this.showBackdrop             = true;
        this.closableViaBackdrop      = true;
        this.showCloseButton          = false;
        this.isAnimationsEnabled      = true;
        this.coverAnchorWhenDisplayed = true;
        this.overlayClass             = "";
        this.backdropClass            = "cdk-overlay-transparent-backdrop";
    }
}
