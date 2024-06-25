import {TemplatePortal} from "@angular/cdk/portal";
import {AfterContentInit, ContentChild, Directive, EventEmitter, Injector, OnDestroy, Output, TemplateRef, ViewContainerRef} from "@angular/core";
import {CarouselComponent} from "framework/ui/carousel/carousel.component";
import {BaseComponent} from "framework/ui/components";
import {Subscription} from "rxjs";
import {map, startWith, switchMap} from "rxjs/operators";

@Directive({
               selector: "ng-template[slideContent]"
           })
export class CarouselSlideContent
{
    constructor(public template: TemplateRef<any>) {}
}

@Directive({
               selector: "[o3-carousel-slide]"
           })
export class CarouselSlideDirective extends BaseComponent implements AfterContentInit,
                                                                     OnDestroy
{
    private static nextId = 0;
    private m_isSelected  = Subscription.EMPTY;

    public id: number;
    public interacted: boolean = false;
    public portal: TemplatePortal;

    @Output("interacted") readonly interactedStream: EventEmitter<CarouselSlideDirective> = new EventEmitter<CarouselSlideDirective>();

    @ContentChild(CarouselSlideContent, {static: false}) lazyContent: CarouselSlideContent;

    constructor(inj: Injector,
                public carousel: CarouselComponent,
                public content: TemplateRef<any>,
                private m_viewContainerRef: ViewContainerRef)
    {
        super(inj);
        this.id = CarouselSlideDirective.nextId++;
    }

    ngAfterContentInit()
    {
        let slideChanges  = this.carousel.slides.changes.pipe(switchMap(() => this.carousel.selectionChange.pipe(map(
            event => event.selected === this), startWith(this.carousel.selected === this))));
        this.m_isSelected = slideChanges.subscribe(
            isSelected =>
            {
                if (isSelected && this.lazyContent && !this.portal)
                {
                    this.portal = new TemplatePortal(this.lazyContent.template, this.m_viewContainerRef!);
                }
            });
    }

    ngOnChanges()
    {
        this.carousel.stateChanged();
    }

    ngOnDestroy()
    {
        this.m_isSelected.unsubscribe();
    }

    select(): void
    {
        this.carousel.selected = this;
    }

    reset(): void
    {
        this.interacted = false;
    }

    markAsInteracted()
    {
        if (!this.interacted)
        {
            this.interacted = true;
            this.interactedStream.emit(this);
        }
    }
}
