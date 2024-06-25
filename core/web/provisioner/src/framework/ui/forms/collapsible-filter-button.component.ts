import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from "@angular/core";
import {Future} from "framework/utils/concurrency";

@Component({
               selector: "o3-collapsible-filter-button",
               template: `
                   <div class="o3-collapsible-filter-button--input" [class.show]="searchVisible">
                       <form (submit)="onSubmit()">
                           <mat-form-field>
                               <input matInput
                                      #searchInput
                                      name="searchText"
                                      [(ngModel)]="searchText"
                                      (ngModelChange)="onSearchTextChange($event)"
                                      [style.font-size]="fontPx + 'px'"
                                      autocomplete="off">
                           </mat-form-field>
                       </form>
                   </div>
                   <span class="o3-collapsible-filter-button--toggle">
                        <button mat-icon-button type="button" class="area-color" (click)="toggle()" [disableRipple]="disableRipple">
                            <i class="o3 o3-filter-outline area-color"></i>
                        </button>
                   </span>`
           })
export class CollapsibleFilterButtonComponent implements OnInit
{
    /**
     * The model value to bind to (optional).
     */
    @Input()
    model: string = null;

    /**
     * the size of the text and filter outline
     */
    @Input() fontPx: number = 14;

    /**
     * An event raised when the model value changes.
     */
    @Output()
    modelChange = new EventEmitter<any>();

    /**
     * The search input itself.
     */
    @ViewChild("searchInput", {static: true})
    searchInput: ElementRef;

    /**
     * Whether to show the search input.
     */
    searchVisible: boolean = false;

    /**
     * The current search text.
     */
    searchText: string = null;

    /**
     * The search text to emit. Used for debouncing.
     */
    searchTextNext: string = null;

    @Input()
    disableRipple: boolean = false;

    /**
     * Handle the init life cycle event.
     */
    ngOnInit()
    {
        // init the search text
        this.searchText    = this.model;
        this.searchVisible = !!this.searchText;
    }

    /**
     * Show / hide the search text input.
     */
    async toggle()
    {
        this.searchVisible = !this.searchVisible;

        if (this.searchVisible)
        {
            await Future.delayed(50);
            this.searchInput.nativeElement.focus();

            if (this.searchText)
            {
                this.onSearchTextChange(this.searchText);
            }
        }
        else
        {
            this.model = null;
            this.modelChange.emit(null);
        }
    }

    /**
     * Handle search input text changing.
     */
    async onSearchTextChange(searchText: string)
    {
        this.searchTextNext = searchText;
        await Future.delayed(250);

        if (this.searchTextNext == searchText)
        {
            this.model = searchText;
            this.modelChange.emit(searchText);
        }
    }

    /**
     * Handle search input submit.
     */
    onSubmit()
    {
        // do nothing
    }
}
