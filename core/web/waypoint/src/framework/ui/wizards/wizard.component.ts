import {Component, ContentChildren, ElementRef, EventEmitter, HostListener, Injector, Input, Output, QueryList, ViewChild} from "@angular/core";
import {MatTabGroup} from "@angular/material/tabs";
import {BaseComponent} from "framework/ui/components";
import {WizardStep} from "framework/ui/wizards/wizard-step";
import {WizardStepGroupDirective} from "framework/ui/wizards/wizard-step-group.directive";

@Component({
               selector   : "o3-wizard",
               templateUrl: "./wizard.component.html"
           })
export class WizardComponent<T> extends BaseComponent
{
    @Input() label: string;
    @Input() modalMode: boolean       = false;
    @Input() finishButtonText: string = "FINISH";

    @Input() data: T;

    @Output() wizardCancel: EventEmitter<void> = new EventEmitter<void>();
    @Output() wizardCommit: EventEmitter<void> = new EventEmitter<void>();

    private m_selectedIndex: number               = 0;
    private m_selectedStep: InternalWizardStep<T> = null;

    public get selectedIndex(): number
    {
        return this.m_selectedIndex;
    }

    public set selectedIndex(value: number)
    {
        let available = this.availableSteps;
        if (value >= 0 && value < available.length)
        {
            this.m_selectedIndex = value;
        }
    }

    steps: InternalWizardStep<T>[] = [];

    /**
     * Step templates gathered from `ContentChildren` if described in your markup.
     */
    @ContentChildren(WizardStep)
    public set stepTemplates(stepTemplates: QueryList<WizardStep<T>>)
    {
        this.steps = [];
        this.translateTemplates(stepTemplates ? stepTemplates.toArray() : [], [], true);
    }

    @ViewChild(MatTabGroup, {static: true}) tabGroup: MatTabGroup;

    /**
     * Constructor
     * @param inj
     * @param elementRef
     */
    constructor(inj: Injector,
                private elementRef: ElementRef)
    {
        super(inj);
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();

        this.tabGroup.realignInkBar();
    }

    private switchSelectedStep()
    {
        if (this.m_selectedStep)
        {
            this.m_selectedStep.component.stepSelected = false;
        }

        let available = this.availableSteps;
        let index     = this.selectedIndex;

        this.m_selectedStep = (index >= 0 && index < available.length) ? available[index] : null;

        if (this.m_selectedStep)
        {
            this.m_selectedStep.component.stepSelected = true;
        }
    }

    @HostListener("window:keyup", ["$event"])
    public onKey(event: KeyboardEvent)
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
     * @param groups
     * @param switchSelected
     */
    private async translateTemplates(templates: WizardStep<T>[],
                                     groups: WizardStepGroupDirective[],
                                     switchSelected: boolean)
    {
        for (const template of templates)
        {
            // Bind data
            await template.setData(this.data);
            template.wizard = this;

            if (template instanceof WizardStep)
            {
                this.translateStepTemplate(template, groups);
            }

            if (template instanceof WizardStepGroupDirective)
            {
                if (template.steps && template.steps.length)
                {
                    // Filter current group to prevent infinite loop
                    let subSteps = template.steps.toArray()
                                           .filter(
                                               step => step !== template);
                    await this.translateTemplates(subSteps, groups.concat(template), false);
                }
            }
        }

        if (switchSelected)
        {
            this.switchSelectedStep();
        }
    }

    private translateStepTemplate(temp: WizardStep<T>,
                                  groups: WizardStepGroupDirective[])
    {
        let step       = new InternalWizardStep<T>();
        step.id        = this.steps.length;
        step.name      = temp.name;
        step.groups    = groups;
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

        return this.steps.filter((step) =>
                                 {
                                     let groupEnabled = step.groups.every((group) => group.isEnabled == null || group.isEnabled());
                                     let stepEnabled  = step.component.isEnabled();
                                     return groupEnabled && stepEnabled;
                                 });
    }

    /**
     * True if the user can jump to the specified step.
     * @param step
     * @param index
     */
    canJumpToStep(step: InternalWizardStep<T>,
                  index: number): boolean
    {
        if (this.selectedIndex == index)
        {
            return false;
        }

        if (this.selectedIndex > index)
        {
            return true;
        }

        return this.arePreviousStepsValid(index);
    }

    /**
     * Go to a named step (identified by its name property).
     * @param name
     */
    goToStep(name: string): boolean
    {
        for (let step of this.steps)
        {
            if (step.name && step.name == name)
            {
                let index = this.steps.indexOf(step);
                if (this.canJumpToStep(step, index))
                {
                    this.selectedIndex = index;
                    return true;
                }
            }
        }

        return false;
    }

    getStepId(index: number,
              item: InternalWizardStep<any>)
    {
        return item.id;
    }

    hasPrev(): boolean
    {
        return this.selectedIndex > 0;
    }

    prev()
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

    async next()
    {
        if (this.hasNext() && this.isValid())
        {
            let cancelled = await this.onNext();
            if (!cancelled)
            {
                this.selectedIndex = this.selectedIndex + 1;
                this.detectChanges();
            }
        }
    }

    /**
     * Returns true if next should be cancelled.
     */
    private async onNext(): Promise<boolean>
    {
        if (this.m_selectedStep != null)
        {
            return await this.m_selectedStep.component.onNext();
        }

        return false;
    }

    /**
     * Returns true if the current step is valid.
     */
    isValid(): boolean
    {
        return this.isStepValid(this.m_selectedStep);
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

    async onStepSelected()
    {
        this.switchSelectedStep();

        if (this.m_selectedStep != null)
        {
            await this.m_selectedStep.component.onStepSelected();
        }

        this.detectChanges();
    }

    cancel()
    {
        this.wizardCancel.emit();
    }

    save()
    {
        if (this.m_selectedStep == null)
        {
            this.wizardCancel.emit();
        }
        else
        {
            this.wizardCommit.emit();
        }
    }

    wizardClasses()
    {
        return {
            "modal-mode": this.modalMode
        };
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

    /**
     * Groups of wizard steps if this step has sub-steps.
     */
    groups: WizardStepGroupDirective[];
}
