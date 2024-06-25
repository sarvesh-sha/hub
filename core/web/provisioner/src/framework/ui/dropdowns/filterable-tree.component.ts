import {ChangeDetectionStrategy, Component, ContentChild, ElementRef, EventEmitter, Injector, Input, NgZone, Output, TemplateRef, ViewChild} from "@angular/core";
import {MAT_CHECKBOX_DEFAULT_OPTIONS} from "@angular/material/checkbox";
import {SafeHtml} from "@angular/platform-browser";

import {IActionMapping, ITreeOptions, KEYS, TreeComponent, TreeNode} from "@circlon/angular-tree-component";
import {UUID} from "angular2-uuid";
import {BaseComponent} from "framework/ui/components";
import {Debouncer} from "framework/utils/debouncers";
import {asyncScheduler, BehaviorSubject, Observable} from "rxjs";
import {throttleTime} from "rxjs/operators";

@Component({
               selector       : "o3-filterable-tree",
               styleUrls      : ["./filterable-tree.component.scss"],
               templateUrl    : "./filterable-tree.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush,
               providers      : [
                   {
                       provide : MAT_CHECKBOX_DEFAULT_OPTIONS,
                       useValue: {
                           color      : "accent",
                           clickAction: "noop"
                       }
                   }
               ]
           })
export class FilterableTreeComponent<T> extends BaseComponent
{
    private m_nodes: ITreeNodeInternal<T>[] = [];
    private m_nodeIds                       = new Map<T, string>();

    private m_loadTriggered: boolean = false;

    private m_selectedNodes: T[] = [];
    public selectedNodesLookup   = new Map<T, boolean>();

    public isFlat: boolean = true;

    treeOptions: ITreeOptions;

    filter: string;
    private m_filterDebouncer: Debouncer;

    @ViewChild(TreeComponent, {static: false}) tree: TreeComponent;

    @Input() label: string;

    @Input() autoExpandSelected: boolean = true;
    @Input() autoExpandAll: boolean      = false;

    @Input() preventDeselection: boolean = false;

    @Input() hideFilter: boolean        = false;
    @Input() requireFilterText: boolean = false;

    @Input() filterFn: ITreeNodeFilter<T>;
    @Input() filterSubmitFn: ITreeNodeFilterSubmit<T>;

    @Input() useDisabledStyling: boolean = true;

    private m_filterPlaceholder: string;

    @Input() set filterPlaceholder(val: string)
    {
        this.m_filterPlaceholder = val;
    }

    get filterPlaceholder(): string
    {
        if (this.m_filterPlaceholder)
        {
            return this.m_filterPlaceholder;
        }

        return this.requireFilterText ? "Search..." : "Filter...";
    }

    @Input() hideLoadingMessage: boolean = false;

    //--//

    nodeTemplate: TemplateRef<any>;

    @Input() set templateOverride(nodeTemplate: TemplateRef<any>)
    {
        this.nodeTemplate = nodeTemplate;
    }

    @ContentChild("nodeTemplate", {static: true}) set templateOverrideContent(nodeTemplate: TemplateRef<any>)
    {
        this.nodeTemplate = nodeTemplate;
    }

    nodePostTemplate: TemplateRef<any>;

    @Input() set templatePostOverride(nodePostTemplate: TemplateRef<any>)
    {
        this.nodePostTemplate = nodePostTemplate;
    }

    @ContentChild("nodePostTemplate", {static: true}) set templatePostOverrideContent(nodePostTemplate: TemplateRef<any>)
    {
        this.nodePostTemplate = nodePostTemplate;
    }

    //--//

    private m_useCheckboxes: boolean = true;

    public get useCheckboxes(): boolean
    {
        return this.m_useCheckboxes;
    }

    @Input()
    public set useCheckboxes(value: boolean)
    {
        this.m_useCheckboxes = value;
        this.updateOptions();
    }

    //--//

    private m_useVirtualScroll: boolean = true;

    public get useVirtualScroll(): boolean
    {
        return this.m_useVirtualScroll;
    }

    @Input()
    public set useVirtualScroll(value: boolean)
    {
        this.m_useVirtualScroll = value;
        this.updateOptions();
    }

    //--//

    @Input() selectChildren: boolean = false;

    @Output() submit: EventEmitter<void> = new EventEmitter<void>();

    @Input() set nodes(nodes: ITreeNode<T>[])
    {
        if (!this.m_lazyLoader) this.updateNodes(nodes);
    }

    get nodes(): ITreeNode<T>[]
    {
        return this.m_nodes;
    }

    @Input() set lazyLoader(lazyLoader: ILazyLoader<T>)
    {
        this.m_lazyLoader = lazyLoader;
        this.updateOptions();
    }

    private m_lazyLoader: ILazyLoader<T>;

    @Input() set selectedNodes(nodes: T[])
    {
        if (nodes === this.m_selectedNodes) return;

        this.m_selectedNodes     = nodes || [];
        this.selectedNodesLookup = new Map<T, boolean>();
        for (let node of this.m_selectedNodes)
        {
            this.selectedNodesLookup.set(node, true);
        }

        this.bindNodes();
    }

    get selectedNodes(): T[]
    {
        if (!this.m_selectedNodes)
        {
            this.m_selectedNodes = [];

            for (let value of this.selectedNodesLookup.keys())
            {
                if (this.selectedNodesLookup.get(value))
                {
                    this.m_selectedNodes.push(value);
                }
            }
        }

        return this.m_selectedNodes;
    }

    @Output() selectedNodesChange: EventEmitter<T[]> = new EventEmitter();
    @Output() nodeExpandContract: EventEmitter<T>    = new EventEmitter();

    actionMapping: IActionMapping = {
        mouse: {
            click: (tree,
                    node,
                    $event) =>
            {
                let data = <ITreeNode<T>>node.data;

                // Handle non-nodes
                if (data.isDivider) return;
                // Handle toggling if cannot select
                if (data.disableSelection)
                {
                    if (data.children && data.children.length > 0) node.toggleExpanded();
                    return;
                }

                // Handle preventing deselection if no change and prevent deselect is enabled
                let isDeselection = this.selectedNodesLookup.get(data.id);
                if (this.preventDeselection && !this.m_useCheckboxes && isDeselection) return;

                // Toggle the node
                node.setIsActive(!isDeselection);
                this.toggleNode(node);
            },

            // Activate the node and submit upon double click if single select.
            dblClick: (tree,
                       node,
                       $event) =>
            {
                if (this.m_useCheckboxes) return;

                let data = <ITreeNode<T>>node.data;
                if (data.isDivider || data.disableSelection || data.children && data.children.length > 0) return;

                node.setIsActive(true);
                this.toggleNode(node, true);

                this.submit.next();
            }
        },
        keys : {
            [KEYS.CONTEXT_MENU]: (tree,
                                  node,
                                  $event) =>
            {},
            [KEYS.ENTER]       : (tree,
                                  node,
                                  $event) =>
            {
                // Toggle node expansion if there are children
                if (node.hasChildren)
                {
                    node.toggleExpanded();
                }
            },
            [KEYS.UP]          : (tree,
                                  node,
                                  $event) =>
            {},
            [KEYS.RIGHT]       : (tree,
                                  node,
                                  $event) =>
            {},
            [KEYS.DOWN]        : (tree,
                                  node,
                                  $event) =>
            {},
            [KEYS.LEFT]        : (tree,
                                  node,
                                  $event) =>
            {},
            [KEYS.SPACE]       : (tree,
                                  node,
                                  $event) =>
            {
                // Short circuit if deselection is prevented and already selected
                if (!this.m_useCheckboxes && this.preventDeselection && !!this.selectedNodesLookup.get(node.id)) return;

                // Activate and toggle the node
                this.toggleNode(node);
                node.setIsActive(!!this.selectedNodesLookup.get(node.id));
            }
        }
    };

    constructor(inj: Injector,
                public elementRef: ElementRef,
                private ngZone: NgZone)
    {
        super(inj);

        this.updateOptions();

        this.m_filterDebouncer = new Debouncer(500, async () =>
        {
            if (this.filterSubmitFn)
            {
                // Do nothing.
                this.tree.treeModel.filterNodes(null, true);
            }
            else if (this.filterFn)
            {
                let lowerWords = this.filter?.toLocaleLowerCase()
                                     .replace(/\s+/g, " ")
                                     .split(" ")
                                     .filter((word) => !!word);

                this.tree.treeModel.filterNodes((node: TreeNode) => lowerWords.every((lower) => this.filterFn(node.data.original, lower)), true);
            }
            else
            {
                this.tree.treeModel.filterNodes(this.filter, true);
            }

            this.sizeChanged(3, 100);
        });
    }

    private updateOptions()
    {
        this.treeOptions = {
            allowDrop       : false,
            allowDrag       : false,
            animateExpand   : false,
            displayField    : "label",
            nodeHeight      : 36,
            actionMapping   : this.actionMapping,
            useVirtualScroll: this.m_useVirtualScroll,
            idField         : "idString",
            scrollOnActivate: false,
            hasChildrenField: this.m_lazyLoader ? "hasChildren" : undefined,
            getChildren     : this.m_lazyLoader ? this.lazyLoadChildren.bind(this) : undefined
        };
    }

    private updateNodes(nodes: ITreeNode<T>[])
    {
        this.m_nodeIds = new Map<T, string>();

        let processedArray: ITreeNodeInternal<T>[] = [];
        if (nodes)
        {
            for (let node of nodes)
            {
                if (node)
                {   // TreeView filter doesn't like nulls.
                    processedArray.push(this.processNode(node));

                    if (node.children?.length > 0 || node.hasChildren) this.isFlat = false;
                }
            }
        }

        this.m_nodes = processedArray;

        this.bindNodes();
    }

    private bindNodes()
    {
        if (this.autoExpandSelected || this.autoExpandAll)
        {
            setTimeout(() =>
                       {
                           if (this.tree && this.tree.treeModel)
                           {
                               if (this.autoExpandAll) this.tree.treeModel.expandAll();

                               let firstSelected: TreeNode;
                               let firstSelectedIndex: number;
                               for (let item of this.selectedNodes)
                               {
                                   let selectedNode = this.getNode(item);
                                   if (selectedNode)
                                   {
                                       if (this.autoExpandSelected) selectedNode.ensureVisible();
                                       if (!firstSelected || selectedNode.index < firstSelectedIndex)
                                       {
                                           firstSelected      = selectedNode;
                                           firstSelectedIndex = selectedNode.index;
                                       }
                                   }
                               }

                               if (firstSelected)
                               {
                                   firstSelected.ensureVisible();
                                   setTimeout(() => firstSelected.scrollIntoView());
                               }
                           }
                       }, 50);
        }

        if (this.m_lazyLoader)
        {
            this.lazyLoad();
        }

        this.sizeChanged(3, 100);
    }

    focusIn(event: FocusEvent,
            node: TreeNode)
    {
        this.tree.treeModel.setFocus(true);
        this.tree.treeModel.setFocusedNode(node);
    }

    focusOut()
    {
        this.tree.treeModel.setFocus(false);
    }

    onFilterChanged()
    {
        // If there's a callback for whole text entry, don't filter.
        if (this.filterSubmitFn) return;

        this.m_filterDebouncer.invoke();
    }

    onFilterKeyUp(event: KeyboardEvent)
    {
        if (this.filterSubmitFn)
        {
            if (event.code == "Enter")
            {
                let value   = this.filter;
                this.filter = undefined;

                this.filterSubmitFn(value);
            }
        }
    }

    toggleNode(node: TreeNode,
               newValue?: boolean)
    {
        let data      = <ITreeNode<T>>node.data;
        let currValue = this.selectedNodesLookup.get(data.id);
        newValue      = newValue ?? !currValue;
        if (currValue === newValue) return;

        this.processCheck(node, newValue);
        this.ngZone.run(() => this.selectedNodesChange.emit(this.selectedNodes));
    }

    scrollIntoView(id: T)
    {
        let node = this.getNode(id);
        node.scrollIntoView();
    }

    expandNode(id: T)
    {
        let node = this.getNode(id);
        this.tree.treeModel.setExpandedNode(node, true);
    }

    async lazyLoad()
    {
        if (!this.m_lazyLoader || this.m_loadTriggered) return;
        let tree = await this.m_lazyLoader.getTree();

        // Do a chunked, throttled lazy load
        this.m_loadTriggered = true;
        this.chunkedLazyLoad(this.m_lazyLoader, tree)
            .subscribe((nodes: ITreeNode<T>[]) =>
                       {
                           this.updateNodes(nodes);
                           this.detectChanges();
                       });
    }

    private chunkedLazyLoad(loader: ILazyLoader<T>,
                            nodes: ILazyTreeNode<T>[],
                            chunkSize?: number,
                            throttle?: number): Observable<ITreeNode<T>[]>
    {
        // Ensure default chunk size and throttle time
        if (!chunkSize) chunkSize = 100;
        if (!throttle) throttle = 100;

        // Create behavior subject to track progress
        let subject = new BehaviorSubject<ITreeNode<T>[]>([]);

        // Split nodes into chunk tasks
        let chunks: ILazyTreeNode<T>[][] = [];
        for (let i = 0, j = nodes.length; i < j; i += chunkSize)
        {
            chunks.push(nodes.slice(i, i + chunkSize));
        }

        // Define chunked loading function
        let doLoad = async (chunks: ILazyTreeNode<T>[][]) =>
        {
            for (let chunk of chunks)
            {
                subject.next(subject.value.concat(await loader.loadNodes(chunk)));
            }

            subject.complete();
        };

        // Load each chunk
        doLoad(chunks);

        // Return observable to get async updates
        return subject.asObservable()
                      .pipe(throttleTime(throttle,
                                         asyncScheduler,
                                         {
                                             leading : true,
                                             trailing: true
                                         }));
    }

    private async lazyLoadChildren(node: TreeNode)
    {
        let lazyNode: ITreeNodeInternal<T> = node.data;

        // Load child nodes in very large chunks with no throttling since angular-tree-component
        // does not support chunked/progressive loading of child nodes
        return this.chunkedLazyLoad(this.m_lazyLoader, lazyNode.lazyChildren, 10000, 0)
                   .toPromise();
    }

    private processCheck(node: TreeNode,
                         checked: boolean)
    {
        let data = <ITreeNode<T>>node.data;

        // update checked node state
        if (data.id)
        {
            if (!this.m_useCheckboxes) this.selectedNodesLookup = new Map<T, boolean>();
            this.selectedNodesLookup.set(data.id, checked);
            this.m_selectedNodes = null;
        }

        // update child node state
        if (this.m_useCheckboxes && this.selectChildren)
        {
            if (node.children)
            {
                node.children.forEach((child) => this.processCheck(child, checked));
            }
            else if (data.lazyChildren)
            {
                data.lazyChildren.forEach((child) => this.processLazyCheck(child, checked));
            }
        }
    }

    private processLazyCheck(node: ILazyTreeNode<T>,
                             checked: boolean)
    {
        if (node.id)
        {
            if (!this.m_useCheckboxes) this.selectedNodesLookup = new Map<T, boolean>();
            this.selectedNodesLookup.set(node.id, checked);
            this.m_selectedNodes = null;
        }

        // update child node state
        if (this.m_useCheckboxes && this.selectChildren)
        {
            if (node.children)
            {
                node.children.forEach((child) => this.processLazyCheck(child, checked));
            }
        }
    }

    private processNode(node: ITreeNode<T>): ITreeNodeInternal<T>
    {
        let children: ITreeNodeInternal<T>[] = undefined;
        if (node.children)
        {
            children = [];
            for (let child of node.children)
            {
                let childProcessed = this.processNode(child);
                children.push(childProcessed);
            }
        }

        let idString = typeof node.id === "string" || typeof node.id === "number" ? "" + node.id : UUID.UUID();
        this.m_nodeIds.set(node.id, idString);

        return {
            id              : node.id,
            idString        : idString,
            children        : children,
            label           : node.label,
            safeLabel       : node.safeLabel,
            isDivider       : node.isDivider,
            disableSelection: node.disableSelection,
            original        : node,
            hasChildren     : node.hasChildren,
            lazyChildren    : node.lazyChildren
        };
    }

    private getNode(id: T): TreeNode
    {
        let idString = this.m_nodeIds.get(id);
        return idString && this.tree.treeModel.getNodeById(idString);
    }

    private sizeChanged(retries: number,
                        retryDelay: number)
    {
        if (this.tree)
        {
            this.tree.sizeChanged();
        }

        if (retries > 0)
        {
            setTimeout(() => this.sizeChanged(retries - 1, retryDelay), retryDelay);
        }
    }
}

export type ITreeNodeFilter<T> = (node: ITreeNode<T>,
                                  filterText: string) => boolean;

export type ITreeNodeFilterSubmit<T> = (filterText: string) => void;

export interface ITreeNode<T>
{
    id: T;
    label: string;
    children: ITreeNode<T>[];
    hasChildren: boolean;

    safeLabel?: SafeHtml;
    disableSelection?: boolean;
    isDivider?: boolean;
    lazyChildren?: ILazyTreeNode<T>[];
}

interface ITreeNodeInternal<T> extends ITreeNode<T>
{
    idString: string;
    original: ITreeNode<T>;
}

export interface ILazyTreeNode<T>
{
    id: T;
    children: ILazyTreeNode<T>[];
}

export interface ILazyLoader<T>
{
    getTree(): Promise<ILazyTreeNode<T>[]>;

    loadNodes(nodes: ILazyTreeNode<T>[]): Promise<ITreeNode<T>[]>

    getLabel(selected: T): Promise<string>;
}
