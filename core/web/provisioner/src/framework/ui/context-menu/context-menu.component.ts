import {Component, ElementRef, EventEmitter, Input, Output, ViewChild, Directive} from "@angular/core";
import {MatMenu, MatMenuTrigger} from "@angular/material/menu";

@Component({
               selector   : "o3-context-menu",
               templateUrl: "./context-menu.component.html"
           })
export class ContextMenu
{
    @ViewChild("contextMenu", {static: true})
    contextMenu: MatMenu;

    @ViewChild("contextMenuTrigger", {static: true})
    contextMenuTrigger: MatMenuTrigger;

    items: ContextMenuItemComponent[] = [];

    get isShown(): boolean
    {
        return this.contextMenuTrigger.menuOpen;
    }

    constructor(private element: ElementRef)
    {

    }

    show(items?: ContextMenuItemComponent[])
    {
        this.contextMenu.hasBackdrop = true;
        this.contextMenu.xPosition   = "after";
        this.contextMenu.yPosition   = "below";

        if (items) this.items = items;
        this.contextMenuTrigger.openMenu();
    }

    reset()
    {
        // close the menu if its open
        if (this.contextMenuTrigger.menuOpen)
        {
            this.contextMenuTrigger.closeMenu();
        }
    }

    public onTableContextMenuItemClicked(event: any,
                                         item: ContextMenuItemComponent)
    {
        this.contextMenuTrigger.closeMenu();

        item.click.emit(event);
    }

    public cancelContextMenu(event: MouseEvent)
    {
        // cancel the original event so we don't show native context menu
        event.preventDefault();
        event.stopPropagation();
    }
}

@Directive()
export class ContextMenuItemComponent
{
    @Input()
    public label: string;

    @Input()
    public subMenu: any;

    @Input()
    public subMenuItems: ContextMenuItemComponent[] = [];

    @Output()
    public click: EventEmitter<Event> = new EventEmitter<Event>();

    constructor()
    {
    }

    addItem(label: string,
            callback?: () => void): ContextMenuItemComponent
    {
        let item = new ContextMenuItemComponent();

        item.label = label;
        if (callback)
        {
            item.click.subscribe((event: Event) => callback());
        }

        this.subMenuItems.push(item);

        return item;
    }
}
