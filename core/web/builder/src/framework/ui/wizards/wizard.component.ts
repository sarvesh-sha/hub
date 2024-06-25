import {ComponentType} from "@angular/cdk/portal";
import {Component, ContentChildren, ElementRef, EventEmitter, HostListener, Injector, Input, Output, QueryList, Renderer2, ViewChild, ViewChildren} from "@angular/core";
import {NgForm} from "@angular/forms";
import {MatTabGroup} from "@angular/material/tabs";

import {ViewportService} from "framework/services/viewport.service";
import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {BaseComponent} from "framework/ui/components";
import {WizardStep} from "framework/ui/wizards/wizard-step";
import {Future} from "framework/utils/concurrency";

@Component({
               selector   : "o3-wizard",
               templateUrl: "./wizard.component.html",
               styleUrls  : ["./wizard.component.scss"]
           })
export class WizardComponent<T> extends BaseComponent
{
    private static readonly ANIMATION_DURATION = 450;

    @Input() label: string;
    @Input() modalMode: boolean       = false;
    @Input() finishButtonText: string = "FINISH";

    @Input() data: T;

    @Output() wizardCancelled   = new EventEmitter<void>();
    @Output() wizardCommitted   = new EventEmitter<void>();
    @Output() viewWindowUpdated = new EventEmitter<VerticalViewWindow>();

    private m_selectedIndex: number               = 0;
    private m_selectedStep: InternalWizardStep<T> = null;
    private m_scrollListener: () => void          = null;

    public loading: boolean = true;

    public isMobile: boolean;

    public get selectedIndex(): number
    {
        return this.m_selectedIndex;
    }

    public set selectedIndex(value: number)
    {
        if (this.m_selectedIndex !== value && this.availableSteps[value])
        {
            this.m_selectedIndex = value;
        }
    }

    private m_firstStepLoaded = false;

    stepsLoaded                    = new Future<void>();
    loadingSteps: boolean          = true;
    steps: InternalWizardStep<T>[] = [];

    /**
     * Step templates gathered from `ContentChildren` if described in your markup.
     */
    @ContentChildren(WizardStep)
    public set stepTemplates(stepTemplates: QueryList<WizardStep<T>>)
    {
        this.steps = [];
        this.translateTemplates(stepTemplates ? stepTemplates.toArray() : []);
    }

    @ViewChild("test_next", {read: ElementRef}) test_next: ElementRef;
    @ViewChild("test_finish", {read: ElementRef}) test_finish: ElementRef;

    @ViewChild(MatTabGroup) tabGroup: MatTabGroup;

    private m_stepForm: NgForm;
    @ViewChild("stepForm", {static: false}) set stepForm(form: NgForm)
    {
        if (form !== this.m_stepForm)
        {
            this.m_stepForm = form;
            this.detectChanges();
        }
    }

    get stepForm(): NgForm
    {
        return this.m_stepForm;
    }

    get animationDuration(): number
    {
        return this.m_firstStepLoaded ? WizardComponent.ANIMATION_DURATION : 0;
    }

    @ViewChildren("tabContent", {read: ElementRef}) tabContents: QueryList<ElementRef<HTMLElement>>;

    /**
     * Constructor
     * @param inj
     * @param elementRef
     * @param renderer
     * @param viewport
     */
    constructor(inj: Injector,
                private elementRef: ElementRef,
                private renderer: Renderer2,
                viewport: ViewportService)
    {
        super(inj);

        // Subscribe to mobile state changes
        viewport.isMobile.subscribe((value) => this.isMobile = value);
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();

        if (!this.loadingSteps && this.tabGroup)
        {
            this.tabGroup.realignInkBar();
        }
    }

    private switchSelectedStep(): void
    {
        if (this.m_selectedStep)
        {
            this.m_selectedStep.component.stepSelected = false;
        }

        let available = this.availableSteps;
        let index     = this.selectedIndex;

        this.m_selectedStep = index >= 0 && index < available.length ? available[index] : null;
        if (this.m_selectedStep)
        {
            this.m_selectedStep.component.stepSelected = true;
        }
    }

    ngOnDestroy(): void
    {
        // If there is any scroll listener, destroy it
        this.clearScrollListener();
    }

    @HostListener("window:keyup", ["$event"])
    public onKey(event: KeyboardEvent): void
    {
        // Only capture left/right when not focused on a text input
        let tagName = event.target && (<any>event.target).tagName || "";
        tagName     = tagName.toLowerCase();
        if (tagName === "input" || tagName === "textarea")
        {
            return;
        }

        if (event.key === "ArrowRight")
        {
            this.next();
        }

        if (event.key === "ArrowLeft")
        {
            this.prev();
        }
    }

    /**
     * Create object model from directives.
     * @param templates
     */
    private async translateTemplates(templates: WizardStep<T>[]): Promise<void>
    {
        await this.executeAsyncOperation(async () =>
                                         {
                                             for (const template of templates)
                                             {
                                                 // Bind data
                                                 await template.setData(this.data);
                                                 template.wizard = this;

                                                 if (template instanceof WizardStep)
                                                 {
                                                     this.translateStepTemplate(template);
                                                 }
                                             }

                                             this.switchSelectedStep();
                                             this.loadingSteps = false;
                                             this.stepsLoaded.resolve();
                                             this.detectChanges();
                                         });
        this.onStepSelected();
    }

    private translateStepTemplate(temp: WizardStep<T>): void
    {
        let step       = new InternalWizardStep<T>();
        step.id        = this.steps.length;
        step.name      = temp.name;
        step.component = temp;

        this.steps.push(step);
    }

    /**
     * Returns the available steps.
     * @readonly
     * @type {WizardStep[]}
     */
    get availableSteps(): InternalWizardStep<T>[]
    {
        if (!this.data) return [];

        return this.steps.filter((step) => step.component.isEnabled());
    }

    /**
     * True if the user can jump to step at specified index
     * @param index
     */
    canJumpToStep(index: number): boolean
    {
        return this.selectedIndex >= index || this.arePreviousStepsValid(index);
    }

    /**
     * Go to a named step (identified by its name property).
     * @param name
     */
    goToStep(name: string): void
    {
        if (name)
        {
            let idx = this.availableSteps.findIndex((step) => step.name == name);
            if (this.canJumpToStep(idx)) this.selectedIndex = idx;
        }
    }

    getStepId(index: number,
              item: InternalWizardStep<any>): number
    {
        return item.id;
    }

    hasPrev(): boolean
    {
        return this.selectedIndex > 0;
    }

    prev(): void
    {
        if (this.selectedIndex <= 0)
        {
            this.selectedIndex = 0;
        }
        else
        {
            this.selectedIndex = this.selectedIndex - 1;
        }
    }

    hasNext(): boolean
    {
        return this.selectedIndex < this.availableSteps.length - 1;
    }

    async next(): Promise<void>
    {
        await this.executeAsyncOperation(async () =>
                                         {
                                             if (this.hasNext() && this.isValid())
                                             {
                                                 // if selected step's onNext returns true, cancel next operation
                                                 if (await this.m_selectedStep?.component.onNext()) return;

                                                 this.selectedIndex = this.selectedIndex + 1;
                                                 this.detectChanges();
                                             }
                                         });
    }

    private async executeAsyncOperation(fn: () => Promise<void>): Promise<void>
    {
        this.loading = true;
        await fn();
        this.loading = false;
    }

    /**
     * Returns true if the current step is valid.
     */
    isValid(): boolean
    {
        return this.m_stepForm?.valid && this.isStepValid(this.m_selectedStep);
    }

    /**
     * Returns true if the specified step is valid.
     * @param step
     */
    private isStepValid(step: InternalWizardStep<T>): boolean
    {
        if (step != null)
        {
            return step.component.isValid();
        }

        return false;
    }

    /**
     * Returns true if all steps are valid.
     */
    allStepsValid(): boolean
    {
        for (let step of this.availableSteps)
        {
            if (!this.isStepValid(step)) return false;
        }

        return true;
    }

    /**
     * Returns tooltip to be displayed in the case that next/finish button is disabled
     */
    getInvalidExplanation(): string
    {
        return this.invalidExplanation(this.m_selectedStep);
    }

    private invalidExplanation(step: InternalWizardStep<T>): string
    {
        if (step != null && !step.component.isValid())
        {
            return step.component.invalidExplanation();
        }

        return null;
    }

    /**
     * True if all previous steps of the specified index are valid.
     * @param index
     */
    arePreviousStepsValid(index: number): boolean
    {
        // if this is the first step, there are no preceding steps to be invalid
        if (index <= 0)
        {
            return true;
        }

        // check if the proceeding step is valid
        let previousStepIndex = index - 1;
        let previousStep      = this.availableSteps[previousStepIndex];

        if (this.isStepValid(previousStep))
        {
            // ensure that if the step has a "next" handler, that it is jumpable
            if (previousStep.component.isNextJumpable())
            {
                // if it is, check all prior steps are valid as well
                return this.arePreviousStepsValid(previousStepIndex);
            }
        }

        return false;
    }

    async onStepSelected(): Promise<void>
    {
        await this.executeAsyncOperation(async () =>
                                         {
                                             this.switchSelectedStep();

                                             // Unregister any existing scroll listener
                                             this.clearScrollListener();

                                             if (this.m_selectedStep != null)
                                             {
                                                 await this.m_selectedStep.component.onStepSelected();

                                                 // Immediately publish new view window and listen to scroll for changes
                                                 let viewContainer = this.tabContents.length > 0 ? this.tabContents.toArray()[this.m_selectedIndex]?.nativeElement?.parentElement : null;
                                                 if (viewContainer)
                                                 {
                                                     this.publishViewWindow(viewContainer);
                                                     this.m_scrollListener = this.renderer.listen(viewContainer, "scroll", (e) =>
                                                     {
                                                         this.publishViewWindow(e.target);
                                                     });
                                                 }
                                             }

                                             this.detectChanges();
                                         });

        this.m_firstStepLoaded = true;
    }

    cancel(): void
    {
        this.wizardCancelled.emit();
    }

    save(): void
    {
        if (this.m_selectedStep == null)
        {
            this.wizardCancelled.emit();
        }
        else
        {
            this.wizardCommitted.emit();
        }
    }

    getStep<T>(type: ComponentType<T>): T
    {
        if (this.m_selectedStep?.component instanceof type)
        {
            return this.m_selectedStep.component;
        }

        return null;
    }

    publishViewWindow(element: HTMLElement): void
    {
        this.viewWindowUpdated.emit(new VerticalViewWindow(element.scrollTop, element.clientHeight));
    }

    private clearScrollListener(): void
    {
        if (this.m_scrollListener)
        {
            this.m_scrollListener();
            this.m_scrollListener = null;
        }
    }

    mobileStepLabel(): string
    {
        return this.m_selectedStep?.component?.getLabel();
    }

    mobileStepNumber(): string
    {
        return `${this.selectedIndex + 1} / ${this.availableSteps?.length}`;
    }

    getNextStepLabel(): string
    {
        if (this.hasNext())
        {
            return this.availableSteps[this.selectedIndex + 1]?.component?.getLabel();
        }

        return null;
    }

    getPreviousStepLabel(): string
    {
        if (this.hasPrev())
        {
            return this.availableSteps[this.selectedIndex - 1]?.component?.getLabel();
        }

        return null;
    }

    getSaveTooltip(): string
    {
        return !this.isValid() ? this.getInvalidExplanation() : "Save changes";
    }
}

class InternalWizardStep<T>
{
    id: number;

    /**
     * An optional id used to reference this step.
     */
    name: string;

    /**
     * The UI element for the step.
     */
    component: WizardStep<T>;
}
