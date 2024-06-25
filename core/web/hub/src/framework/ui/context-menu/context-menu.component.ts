import {Component, Directive, EventEmitter, Input, Output, ViewChild} from "@angular/core";
import {MatMenu, MatMenuTrigger} from "@angular/material/menu";

@Component({
               selector   : "o3-context-menu",
               templateUrl: "./context-menu.component.html"
           })
export class ContextMenuComponent
{
    @ViewChild("contextMenu", {static: true}) contextMenu: MatMenu;
    @ViewChild("contextMenuTrigger", {static: true}) contextMenuTrigger: MatMenuTrigger;

    items: ContextMenuItemComponent[] = [];

    public static positionMenu(triggerWrapperElem: HTMLElement,
                               menuContainerElem: HTMLElement,
                               event: MouseEvent)
    {
        let bounds                    = menuContainerElem.getBoundingClientRect();
        triggerWrapperElem.style.left = `${event.clientX - bounds.left}px`;
        triggerWrapperElem.style.top  = `${event.clientY - bounds.top}px`;
    }

    public open(items?: ContextMenuItemComponent[])
    {
        this.contextMenu.hasBackdrop = true;
        this.contextMenu.xPosition   = "after";
        this.contextMenu.yPosition   = "below";

        if (items) this.items = items;
        this.contextMenuTrigger.openMenu();
    }

    public close(): boolean
    {
        // close the menu if its open
        if (this.contextMenuTrigger.menuOpen)
        {
            this.contextMenuTrigger.closeMenu();
            return true;
        }

        return false;
    }

    public onContextMenuItemClicked(event: any,
                                    item: ContextMenuItemComponent)
    {
        this.contextMenuTrigger.closeMenu();

        item.click.emit(event);
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

    @Input()
    public disabled: boolean;

    @Output()
    public click = new EventEmitter<Event>();

    addItem(label: string,
            callback?: () => void,
            disabled?: boolean): ContextMenuItemComponent
    {
        let item = new ContextMenuItemComponent();

        item.label    = label;
        item.disabled = disabled;
        if (callback)
        {
            item.click.subscribe((event: Event) => callback());
        }

        this.subMenuItems.push(item);

        return item;
    }
}
