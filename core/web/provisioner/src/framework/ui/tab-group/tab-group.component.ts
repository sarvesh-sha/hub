import {CdkPortal} from "@angular/cdk/portal";
import {Attribute, Component, ContentChild, ContentChildren, Directive, ElementRef, EventEmitter, Injector, Input, Output, QueryList, TemplateRef, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";
import {MatTabChangeEvent, MatTabGroup} from "@angular/material/tabs";

import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {BaseComponent} from "framework/ui/components";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {Subject} from "rxjs";

@Directive({
               selector: "[o3TabContent]",
               exportAs: "o3TabContent"
           })
export class TabContentDirective
{
    constructor(public templateRef: TemplateRef<any>) { }
}

const TabActionPriorityPrivate = {
    primary    : "primary",
    secondary  : "secondary",
    tertiary   : "tertiary",
    informative: "informative"
};

export type TabActionPriority = keyof typeof TabActionPriorityPrivate;
export const TabActionPriority: { [P in TabActionPriority]: P } = <any>TabActionPriorityPrivate;

@Component({
               selector: "o3-tab-action[label], o3-tab-action[icon]",
               template: "<ng-template></ng-template>"
           })
export class TabActionComponent
{
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_disabled: boolean | "";

    @Input() public label: string;

    @Input() public labelFirstLevel: string;
    @Input() public labelSecondLevel: string;

    @Input() public icon: string;

    @Input() public priority: TabActionPriority = TabActionPriority.primary;

    @Input() public type: string;

    @Input() @CoerceBoolean() public disabled: boolean;

    @Input() public tooltip: string;

    @Input() public form: NgForm;

    @Output() click = new EventEmitter<any>();

    callback: () => void;

    constructor() { }

    onClick(event: Event)
    {
        if (this.type == "submit")
        {
            if (this.form)
            {
                this.form.onSubmit(event);
            }
        }
        else
        {
            this.click.emit(event);
        }
    }
}

@Component({
               selector: "o3-tab-meta",
               template: "<ng-template o3TabContent #template><ng-content></ng-content></ng-template>"
           })
export class TabMetaComponent
{
    @ViewChild("template", {
        read  : TabContentDirective,
        static: true
    }) public template: TabContentDirective;

    constructor() { }
}

@Component({
               selector: "o3-tab-section",
               template: `
                   <o3-tab-subsection *ngIf="label" [label]="label"></o3-tab-subsection>
                   <ng-content></ng-content>
                   <hr *ngIf="withSeparator"/>
               `
           })
export class TabSection
{
    @Input() label: string;

    @Input() withSeparator: boolean = true;
}

@Component({
               selector: "o3-tab",
               template: "<ng-template o3TabContent #template><ng-content></ng-content></ng-template>"
           })
export class TabComponent
{
    @Input() public label: string;
    @Input() public indicator: string;

    @Input() public disabled: boolean;

    @ViewChild("template", {
        read  : TabContentDirective,
        static: true
    }) public template: TabContentDirective;

    @ContentChild(CdkPortal) public portal: CdkPortal;

    public actions: TabActionComponent[] = [];

    @ContentChildren(TabActionComponent)
    public set tabGroupActionTemplates(tabGroupActionTemplates: QueryList<TabActionComponent>)
    {
        setTimeout(() =>
                   {
                       this.actions = [];
                       if (tabGroupActionTemplates) this.actions = tabGroupActionTemplates.toArray();
                   });
    }

    public meta: TabMetaComponent;

    public active: boolean;

    @ContentChildren(TabMetaComponent)
    public set tabMetaTemplate(tabMetaTemplate: QueryList<TabMetaComponent>)
    {
        this.meta = null;
        if (tabMetaTemplate && tabMetaTemplate.length) this.meta = tabMetaTemplate.first;
    }

    @Output() public submit = new EventEmitter<any>();

    @Output() public selected = new EventEmitter<any>();

    get primaryActions(): TabActionComponent[]
    {
        if (this.actions) return this.actions.filter((action) => action.priority == TabActionPriority.primary);
        return [];
    }

    get secondaryActions(): TabActionComponent[]
    {
        if (this.actions) return this.actions.filter((action) => action.priority == TabActionPriority.secondary);
        return [];
    }

    get tertiaryActions(): TabActionComponent[]
    {
        if (this.actions) return this.actions.filter((action) => action.priority == TabActionPriority.tertiary);
        return [];
    }

    get informative(): TabActionComponent
    {
        if (this.actions)
        {
            let informative = this.actions.filter((action) => action.priority == TabActionPriority.informative);
            if (informative.length === 1) return informative[0];
        }
        return null;
    }

    onSubmit(event: any)
    {
        this.submit.emit(event);
    }
}

@Component({
               selector   : "o3-tab-group",
               templateUrl: "./tab-group.component.html"
           })
export class TabGroupComponent extends BaseComponent
{
    private static readonly minNegativeTabsSpaceWidth: number = 200;
    private static readonly negativeTabsSpaceClass            = "negative-tabs-space-container";

    private m_tabs: TabComponent[] = [];

    @Output() public selectedIndexChange = new EventEmitter<number>();
    @Output() public scrolled            = new EventEmitter<VerticalViewWindow>();

    tabsUpdated: Subject<void> = new Subject<void>();

    firstLevelActions: TabHierarchicalActionComponent[] = [];

    @ViewChild("projectorContainer",
               {
                   read  : ElementRef,
                   static: true
               })
    public set projectedElement(container: ElementRef)
    {
        this.projectedContainer = container.nativeElement;
    }

    projectedContainer: HTMLElement;
    labelContainer: HTMLElement;
    isAbove = true;

    @Input() noUnderline: boolean  = false;
    @Input() fitContainer: boolean = false;
    @Input() cardClass: string;

    @ContentChild("negativeTabsSpace") negativeTabsSpace: TemplateRef<any>;

    @ViewChild(MatTabGroup, {static: true}) tabGroup: MatTabGroup;

    /**
     * tab templates gathered from `ContentChildren`.
     */
    @ContentChildren(TabComponent)
    public set tabTemplates(tabTemplates: QueryList<TabComponent>)
    {
        this.m_tabs = tabTemplates && tabTemplates.toArray() || [];

        this.tabsUpdated.next();

        this.updateNegativeTabSpaceContainer();
    }

    /**
     * tab group action templates gathered from `ContentChildren`.
     */
    @ContentChildren(TabActionComponent)
    public set tabGroupActionTemplates(tabGroupActionTemplates: QueryList<TabActionComponent>)
    {
        let actions = tabGroupActionTemplates?.toArray() || [];

        let firstLevelActions: TabHierarchicalActionComponent[] = [];
        for (let action of actions)
        {
            if (action.labelFirstLevel)
            {
                let firstLevel = this.ensureLevel(firstLevelActions, action.labelFirstLevel);
                if (action.labelSecondLevel)
                {
                    let secondLevel = this.ensureLevel(firstLevel.subActions, action.labelSecondLevel);
                    this.addToLevel(secondLevel.subActions, action);
                }
                else
                {
                    this.addToLevel(firstLevel.subActions, action);
                }
            }
            else
            {
                this.addToLevel(firstLevelActions, action);
            }
        }

        this.firstLevelActions = firstLevelActions;

        this.updateNegativeTabSpaceContainer();
    }

    publishScroll(event: Event)
    {
        let target: HTMLElement = <HTMLElement>event.target;
        this.scrolled.emit(new VerticalViewWindow(target.scrollTop, target.clientHeight));
    }

    public get activeTabs(): TabComponent[]
    {
        return this.m_tabs?.filter((t) => !t.disabled) ?? [];
    }

    private ensureLevel(actions: TabHierarchicalActionComponent[],
                        label: string): TabHierarchicalActionComponent
    {
        for (let existingAction of actions)
        {
            if (existingAction.label == label)
            {
                return existingAction;
            }
        }

        let newAction   = new TabHierarchicalActionComponent();
        newAction.label = label;
        actions.push(newAction);
        return newAction;
    }

    private addToLevel(actions: TabHierarchicalActionComponent[],
                       action: TabActionComponent)
    {
        let newAction    = new TabHierarchicalActionComponent();
        newAction.action = action;
        actions.push(newAction);
    }

    get element(): ElementRef
    {
        return this.m_element;
    }

    /**
     * Constructor
     * @param inj
     * @param m_element
     */
    constructor(inj: Injector,
                private m_element: ElementRef)
    {
        super(inj);
    }

    public ngOnInit()
    {
        super.ngOnInit();
        this.labelContainer = this.tabGroup._elementRef.nativeElement.querySelector(".mat-tab-labels");
        this.moveNegativeSpaceToTabHeader();
    }

    protected afterLayoutChange(): void
    {
        this.updateNegativeTabSpaceContainer();
    }

    updateNegativeTabSpaceContainer()
    {
        if (this.projectedContainer.clientWidth < TabGroupComponent.minNegativeTabsSpaceWidth)
        {
            this.moveNegativeSpaceAbove();
        }
        else
        {
            this.moveNegativeSpaceToTabHeader();
            if (this.projectedContainer.clientWidth < TabGroupComponent.minNegativeTabsSpaceWidth + 50)
            {
                this.moveNegativeSpaceAbove();
            }
        }
    }

    private moveNegativeSpaceToTabHeader()
    {
        if (!this.isAbove) return;
        this.projectedContainer.remove();
        this.projectedContainer.classList.add(TabGroupComponent.negativeTabsSpaceClass);

        this.labelContainer.appendChild(this.projectedContainer);
        this.isAbove = false;
    }

    private moveNegativeSpaceAbove()
    {
        if (this.isAbove) return;
        this.projectedContainer.remove();
        this.projectedContainer.classList.remove(TabGroupComponent.negativeTabsSpaceClass);
        let group: HTMLElement = this.element.nativeElement;
        group.insertBefore(this.projectedContainer, group.firstChild);
        this.isAbove = true;
    }

    get selectedIndex(): number
    {
        if (this.tabGroup) return this.tabGroup.selectedIndex;
        return -1;
    }

    set selectedIndex(value: number)
    {
        this.setSelectedIndex(value);
    }

    private setSelectedIndex(value: number,
                             retries = 3)
    {
        let tab = this.m_tabs[value];
        if (tab && !tab.disabled)
        {
            let tabIndex = this.activeTabs.indexOf(tab);
            if (this.tabGroup) this.tabGroup.selectedIndex = tabIndex;
            this.activateTab(value);
        }
        else if (tab && retries > 0)
        {
            setTimeout(() => this.setSelectedIndex(value, retries - 1), 100);
        }
        else
        {
            this.tabGroup.selectedIndex = 0;
            this.activateTab(this.m_tabs.indexOf(this.activeTabs[0]));
        }
    }

    onSelectedTabChange(event: MatTabChangeEvent)
    {
        this.activateTab(this.m_tabs.indexOf(this.activeTabs[event.index]));
    }

    private activateTab(index: number): void
    {
        for (let tab of this.m_tabs)
        {
            tab.active = false;
        }

        if (this.m_tabs?.length > index && index >= 0)
        {
            let tab    = this.m_tabs[index];
            tab.active = true;
            tab.selected.emit();
            this.selectedIndexChange.emit(index);
        }

        this.triggerLayoutChangeEvent();
    }
}

class TabHierarchicalActionComponent
{
    public label: string;

    public action: TabActionComponent;

    public subActions: TabHierarchicalActionComponent[] = [];
}

@Component({
               selector   : "o3-tab-subsection",
               templateUrl: "./tab-group-subsection.component.html",
               styleUrls  : ["./tab-group-subsection.component.scss"]
           })
export class TabSubsectionTitleComponent
{
    @Input() label: string;

    private m_withFiltering: boolean = false;
    @Input() set withFiltering(filtering: boolean)
    {
        this.m_withFiltering = filtering;
        if (this.m_withFiltering) this.elRef.nativeElement.style.justifyContent = "space-between";
    }

    get withFiltering(): boolean
    {
        return this.m_withFiltering;
    }

    @Input() filterText: string = "";

    @Output() filterTextChange: EventEmitter<string> = new EventEmitter();

    constructor(private elRef: ElementRef,
                @Attribute("centeredContent") centeredContent: string)
    {
        let style = this.elRef.nativeElement.style;
        if (centeredContent)
        {
            style.justifyContent = "center";
            style.marginBottom   = "-11px";
        }
    }

    filterChange(): void
    {
        this.filterTextChange.emit(this.filterText);
    }
}
