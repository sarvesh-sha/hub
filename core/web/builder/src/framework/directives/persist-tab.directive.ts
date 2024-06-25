import {Directive, Host, Input, OnDestroy, OnInit, Optional, Self} from "@angular/core";
import {MatTabGroup} from "@angular/material/tabs";
import {ActivatedRoute} from "@angular/router";
import {ViewStateService} from "framework/ui/navigation/view-state.service";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";
import {TabGroupComponent} from "framework/ui/tab-group/tab-group.component";
import {Subscription} from "rxjs";

@Directive({
               selector: "[persist-tab]"
           })
export class PersistTabDirective implements OnInit,
                                            OnDestroy
{
    /**
     * By default, tabs aren't persisted in bookmarks when they are saved. Allow this to be enabled.
     */
    @Input("saveInBookmark") saveInBookmark: boolean = false;

    private onChange: Subscription;

    constructor(@Host() @Self() @Optional() private tabs: MatTabGroup,
                @Optional() private overlay: OverlayComponent,
                @Optional() private o3Tabs: TabGroupComponent,
                private viewstate: ViewStateService,
                private route: ActivatedRoute)
    {
    }

    ngOnInit()
    {
        // If we are in an overlay, we don't want to try to restore the tab state
        if (this.tabs && !this.overlay)
        {
            let state = this.viewstate.restore();
            let index = state.getNumber("TAB_INDEX", 0);

            if (this.o3Tabs)
            {
                this.o3Tabs.selectedIndex = index;
                this.onChange             = this.o3Tabs.selectedIndexChange.subscribe((value: number) => this.persistChange(value));
            }
            else
            {
                this.tabs.selectedIndex = index;
                this.onChange           = this.tabs.selectedIndexChange.subscribe((value: number) => this.persistChange(value));
            }
        }
    }

    private persistChange(value: number)
    {
        let state = this.viewstate.restore();
        // only set to false if explictly false
        if (this.saveInBookmark === true || String(this.saveInBookmark) === "true")
        {
            state.set("TAB_INDEX", value, true);
        }
        else
        {
            state.set("TAB_INDEX", value, false);
        }
    }

    ngOnDestroy()
    {
        if (this.onChange)
        {
            this.onChange.unsubscribe();
            this.onChange = null;
        }
    }
}
