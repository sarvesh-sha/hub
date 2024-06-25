import {CdkPortal} from "@angular/cdk/portal";
import {Attribute, Component, ContentChild, ContentChildren, Directive, ElementRef, EventEmitter, Injector, Input, Output, QueryList, TemplateRef, ViewChild, ViewChildren} from "@angular/core";
import {MatTabGroup} from "@angular/material/tabs";

import {UtilsService} from "framework/services/utils.service";
import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {BaseComponent} from "framework/ui/components";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {TabActionDirective, TabActionPriority} from "framework/ui/shared/tab-action.directive";

import {Subject} from "rxjs";

@Directive({
               selector: "[o3TabContent]",
               exportAs: "o3TabContent"
           })
export class TabContentDirective
{
    constructor(public templateRef: TemplateRef<any>) { }
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

    public actions: TabActionDirective[] = [];

    @ContentChildren(TabActionDirective)
    public set tabGroupActionTemplates(tabGroupActionTemplates: QueryList<TabActionDirective>)
    {
        setTimeout(() => this.actions = tabGroupActionTemplates?.toArray() || []);
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

    @Output() public unselected = new EventEmitter<any>();
    @Output() public selected   = new EventEmitter<any>();

    primaryActions(sharedActions: TabActionDirective[]): TabActionDirective[]
    {
        return TabActionDirective.filterActions(TabActionPriority.primary, ...sharedActions, ...this.actions);
    }

    secondaryActions(sharedActions: TabActionDirective[]): TabActionDirective[]
    {
        return TabActionDirective.filterActions(TabActionPriority.secondary, ...sharedActions, ...this.actions);
    }

    tertiaryActions(sharedActions: TabActionDirective[]): TabActionDirective[]
    {
        return TabActionDirective.filterActions(TabActionPriority.tertiary, ...sharedActions, ...this.actions);
    }

    informative(sharedActions: TabActionDirective[]): TabActionDirective
    {
        let informative = TabActionDirective.filterActions(TabActionPriority.informative, ...sharedActions, ...this.actions);
        return informative.length === 1 ? informative[0] : null;
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
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_noUnderline: boolean | "";
    static ngAcceptInputType_fitContainer: boolean | "";

    private static readonly minNegativeTabsSpaceWidth = 200;
    private static readonly negativeTabsSpaceClass    = "negative-tabs-space-container";

    private m_tabs: TabComponent[] = [];

    @Output() public selectedIndexChange = new EventEmitter<number>();
    @Output() public viewWindowUpdated   = new EventEmitter<VerticalViewWindow>();

    tabsUpdated = new Subject<void>();

    menuOptions: TabActionDirective[]   = [];
    sharedActions: TabActionDirective[] = [];

    @ViewChild("projectorContainer", {
        read  : ElementRef,
        static: true
    })
    public set projectedElement(container: ElementRef)
    {
        this.projectedContainer = container.nativeElement;
    }

    projectedContainer: HTMLElement;
    isAbove = true;

    @Input() @CoerceBoolean() noUnderline  = false;
    @Input() @CoerceBoolean() fitContainer = false;
    @Input() cardClass: string;

    @ContentChild("negativeTabsSpace") negativeTabsSpace: TemplateRef<any>;

    @ViewChild("test_menuTrigger", {read: ElementRef}) test_menuTrigger: ElementRef;

    @ViewChild(MatTabGroup, {
        static: true,
        read  : ElementRef
    }) tabGroup: ElementRef;

    @ViewChildren("contentContainer", {read: ElementRef}) contentContainers: QueryList<ElementRef<HTMLElement>>;

    /**
     * tab templates gathered from `ContentChildren`.
     */
    @ContentChildren(TabComponent)
    public set tabTemplates(tabTemplates: QueryList<TabComponent>)
    {
        this.m_tabs = tabTemplates?.toArray() || [];

        this.tabsUpdated.next();

        this.updateNegativeTabSpaceContainer();
    }

    /**
     * tab group action templates gathered from `ContentChildren`.
     */
    @ContentChildren(TabActionDirective)
    public set tabGroupActionTemplates(menuOptions: QueryList<TabActionDirective>)
    {
        let options = menuOptions?.toArray() || [];

        this.menuOptions   = options.filter((opt) => !opt.priority);
        this.sharedActions = options.filter((opt) => opt.priority);

        this.updateNegativeTabSpaceContainer();
    }

    private m_selectedIndex: number;
    public get selectedIndex(): number
    {
        return this.m_selectedIndex;
    }

    public set selectedIndex(value: number)
    {
        if (!this.activateTab(value))
        {
            const fallbackFn = () => this.activateTab(this.m_tabs.indexOf(this.activeTabs[0]));
            UtilsService.executeWithRetries(async () => this.activateTab(value), 2, 100, fallbackFn, undefined, true);
        }
    }

    public get activeTabs(): TabComponent[]
    {
        return this.m_tabs?.filter((t) => !t.disabled) ?? [];
    }

    public get element(): ElementRef
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
        this.moveNegativeSpaceToTabHeader();
    }

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        this.emitUpdatedContainerInfo(8);
    }

    private emitUpdatedContainerInfo(numRetries: number)
    {
        UtilsService.executeWithRetries(async () => this.emitContainerInfoHelper(), numRetries, 333, undefined, 1, true);
    }

    private emitContainerInfoHelper(): boolean
    {
        if (this.contentContainers)
        {
            let elem = this.contentContainers.toArray()[this.selectedIndex || 0]?.nativeElement;
            if (elem?.clientHeight)
            {
                this.publishViewWindow(elem);
                return true;
            }
        }

        return false;
    }

    protected afterLayoutChange(): void
    {
        this.updateNegativeTabSpaceContainer();
        this.emitContainerInfoHelper();
    }

    publishViewWindow(element: HTMLElement)
    {
        this.viewWindowUpdated.emit(new VerticalViewWindow(element.scrollTop, element.clientHeight));
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
        if (this.isAbove)
        {
            let labelContainer = this.tabGroup?.nativeElement?.querySelector(".mat-tab-labels");
            if (this.projectedContainer && labelContainer)
            {
                this.projectedContainer.remove();
                this.projectedContainer.classList.add(TabGroupComponent.negativeTabsSpaceClass);

                labelContainer.appendChild(this.projectedContainer);
                this.isAbove = false;
            }
        }
    }

    private moveNegativeSpaceAbove()
    {
        if (!this.isAbove)
        {
            if (this.projectedContainer && this.element)
            {
                this.projectedContainer.remove();
                this.projectedContainer.classList.remove(TabGroupComponent.negativeTabsSpaceClass);
                let group: HTMLElement = this.element.nativeElement;
                group.insertBefore(this.projectedContainer, group.firstChild);
                this.isAbove = true;
            }
        }
    }

    private activateTab(index: number): boolean
    {
        let tab = this.activeTabs[index];
        if (tab && !tab.disabled)
        {
            this.m_selectedIndex = index;
            for (let curr of this.m_tabs)
            {
                let shouldBeActive = tab === curr;
                if (curr.active != shouldBeActive)
                {
                    curr.active = shouldBeActive;

                    if (shouldBeActive)
                    {
                        curr.selected.emit();
                    }
                    else
                    {
                        curr.unselected.emit();
                    }
                }
            }

            this.selectedIndexChange.emit(index);

            this.triggerLayoutChangeEvent();
            this.emitUpdatedContainerInfo(2);

            this.detectChanges();

            return true;
        }

        return false;
    }
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
