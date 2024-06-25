import {animate, AnimationEvent, AnimationTriggerMetadata, state, style, transition, trigger} from "@angular/animations";
import {AfterContentInit, ChangeDetectionStrategy, Component, ContentChildren, ElementRef, EventEmitter, Injector, Input, Output, QueryList} from "@angular/core";
import {CarouselSlideDirective} from "framework/ui/carousel/carousel-slide.directive";
import {BaseComponent} from "framework/ui/components";
import {Subject} from "rxjs";
import {distinctUntilChanged, startWith, takeUntil} from "rxjs/operators";

// Slide animation definitions
export const CarouselAnimations: {
    readonly slideTransition: AnimationTriggerMetadata;
} = {
    slideTransition: trigger("slideTransition", [
        state("previous",
              style({
                        transform : "translate3d(-100%, 0, 0)",
                        visibility: "hidden"
                    })),
        state("current",
              style({
                        transform : "none",
                        visibility: "inherit"
                    })),
        state("next",
              style({
                        transform : "translate3d(100%, 0, 0)",
                        visibility: "hidden"
                    })),
        transition(":leave", style({display: "none"})),
        transition("* => *", animate("500ms cubic-bezier(0.35, 0, 0.25, 1)"))
    ])
};

// Slide animation states
export type CarouselSlidePositionState = "previous" | "current" | "next";

// Slide change event metadata
export class CarouselSelectionEvent
{
    selectedIndex: number;
    previouslySelectedIndex: number;
    selected: CarouselSlideDirective;
    previouslySelected: CarouselSlideDirective;
}


@Component({
               selector       : "o3-carousel",
               templateUrl    : "carousel.component.html",
               styleUrls      : ["carousel.component.scss"],
               animations     : [CarouselAnimations.slideTransition],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class CarouselComponent extends BaseComponent implements AfterContentInit
{
    private static nextId = 0;

    private m_previousIndex: number    = 0;
    private m_selectedIndex: number    = 0;
    private m_id: number;
    private m_onDestroyed              = new Subject<void>();
    private m_animationPaused: boolean = false;
    private m_cycleTimer: number       = null;

    public animationDoneInner                        = new Subject<AnimationEvent>();
    public slides: QueryList<CarouselSlideDirective> = new QueryList<CarouselSlideDirective>();

    @ContentChildren(CarouselSlideDirective, {descendants: true}) allSlides: QueryList<CarouselSlideDirective>;

    @Input() autoCycle: boolean        = false;
    @Input() autoCycleTime: number     = 5000;
    @Input() disableAnimation: boolean = false;

    @Input()
    get selectedIndex()
    {
        return this.m_selectedIndex;
    }

    set selectedIndex(index: number)
    {
        // If slides exist, perform additional validations
        if (this.slides && this.allSlides)
        {
            // Only allow selection of indices in bounds
            if (!this.isValidIndex(index))
            {
                throw Error("o3-carousel: Cannot assign out-of-bounds value to `selectedIndex`.");
            }

            // Mark the currently selected slide as interacted with
            this.selected?.markAsInteracted();

            // Only actually change selection if it is a new index
            if (this.m_selectedIndex !== index)
            {
                this.updateSelectedItemIndex(index);
            }
        }
        else
        {
            // If no slides, just update the index value
            this.m_selectedIndex = index;
        }
    }

    @Input()
    get selected(): CarouselSlideDirective | undefined
    {
        return this.slides ? this.slides.toArray()[this.selectedIndex] : undefined;
    }

    set selected(slide: CarouselSlideDirective | undefined)
    {
        this.selectedIndex = (slide && this.slides) ? this.slides.toArray()
                                                          .indexOf(slide) : -1;
    }

    @Output() readonly selectionChange                   = new EventEmitter<CarouselSelectionEvent>();
    @Output() readonly animationDone: EventEmitter<void> = new EventEmitter<void>();

    constructor(inj: Injector,
                public elementRef: ElementRef<HTMLElement>)
    {
        super(inj);

        // Assign an id to the carousel instance
        this.m_id = CarouselComponent.nextId++;
    }

    ngAfterContentInit()
    {
        // Reset all slides whenever the slide list changes
        this.allSlides.changes
            .pipe(startWith(this.allSlides), takeUntil(this.m_onDestroyed))
            .subscribe((e) => this.onAllSlidesChanges(e));

        // Reset the carousel whenever the available slides changes
        this.slides.changes
            .pipe(takeUntil(this.m_onDestroyed))
            .subscribe(() => this.reset());

        // Trigger animationDone when animation state changes
        this.animationDoneInner
            .pipe(distinctUntilChanged(this.animationStateIsSame), takeUntil(this.m_onDestroyed))
            .subscribe((e) => this.onAnimationDone(e));
    }

    ngAfterViewInit()
    {
        // No need to `takeUntil` here, because we're the ones destroying `steps`.
        this.slides.changes.subscribe(this.ensureSlideIsSelected);

        // If the currently selected index is invalid, force selection to first slide
        if (!this.isValidIndex(this.m_selectedIndex))
        {
            this.m_selectedIndex = 0;
        }
    }

    ngOnDestroy()
    {
        this.stopAutoCycle();
        this.slides.destroy();
        this.m_onDestroyed.next();
        this.m_onDestroyed.complete();
    }

    isCurrent(index: number)
    {
        return this.m_selectedIndex === index;
    }

    next(): void
    {
        this.selectedIndex = this.atEnd() ? 0 : this.m_selectedIndex + 1;
    }

    previous(): void
    {
        this.selectedIndex = this.atStart() ? this.slides.length - 1 : this.m_selectedIndex - 1;
    }

    goto(index: number)
    {
        this.selectedIndex = index;
    }

    reset(): void
    {
        this.updateSelectedItemIndex(0);
        this.slides.forEach(
            step => step.reset());
        this.restartAutoCycle();
        this.stateChanged();
    }

    stopAutoCycle()
    {
        if (this.m_cycleTimer !== null)
        {
            clearInterval(this.m_cycleTimer);
            this.m_cycleTimer = null;
        }
    }

    restartAutoCycle()
    {
        // Stop any existing auto-cycle
        this.stopAutoCycle();

        // If auto-cycle is on, set it up again
        if (this.autoCycle)
        {
            // Set up a new auto-cycle
            this.m_cycleTimer = setInterval(() => this.next(), this.autoCycleTime);
        }
    }

    getSlideLabelId(i: number): string
    {
        return `o3-slide-label-${this.m_id}-${i}`;
    }

    getSlideContentId(i: number): string
    {
        return `o3-slide-content-${this.m_id}-${i}`;
    }

    stateChanged(skipAnimation: boolean = true)
    {
        if (skipAnimation)
        {
            // Set animation as paused
            this.m_animationPaused = true;
            // Un-pause it after the next change detection cycle
            setTimeout(() => this.m_animationPaused = false);
        }

        // Notify of changes
        this.markForCheck();
    }


    getAnimationState(index: number): CarouselSlidePositionState
    {
        if (index < this.m_selectedIndex)
        {
            return "previous";
        }
        else if (index > this.m_selectedIndex)
        {
            return "next";
        }

        return "current";
    }

    isAnimationDisabled(): boolean
    {
        return this.m_animationPaused || this.disableAnimation;
    }

    private updateSelectedItemIndex(index: number): void
    {
        // Commit current index to previous index, update current index
        this.m_previousIndex = this.m_selectedIndex;
        this.m_selectedIndex = index;

        // Get all slides
        let slides = this.slides.toArray();

        // Emit selection change event
        this.selectionChange.emit({
                                      selectedIndex          : this.m_selectedIndex,
                                      previouslySelectedIndex: this.m_previousIndex,
                                      selected               : slides[this.m_selectedIndex],
                                      previouslySelected     : slides[this.m_previousIndex]
                                  });

        // Signal that state changed
        this.stateChanged(false);
    }


    private isValidIndex(index: number): boolean
    {
        return index > -1 && (!this.slides || index < this.slides.length);
    }

    private atStart(): boolean
    {
        return this.selectedIndex === 0;
    }

    private atEnd(): boolean
    {
        return this.selectedIndex === this.slides.length - 1;
    }

    private onAllSlidesChanges(steps: QueryList<CarouselSlideDirective>)
    {
        let ownedSlides = steps ? steps.filter(
            step => step.carousel === this) : [];

        this.slides?.reset(ownedSlides);
        this.slides?.notifyOnChanges();
    }

    private animationStateIsSame(x: AnimationEvent,
                                 y: AnimationEvent)
    {
        return x.fromState === y.fromState && x.toState === y.toState;
    }

    private onAnimationDone(event: AnimationEvent)
    {
        if ((event.toState as CarouselSlidePositionState) === "current") this.animationDone.emit();
    }

    private ensureSlideIsSelected()
    {
        if (!this.selected)
        {
            // Default to last valid slide index
            this.m_selectedIndex = Math.max(this.m_selectedIndex - 1, 0);
        }
    }
}
