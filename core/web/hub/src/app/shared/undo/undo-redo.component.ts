import {ChangeDetectionStrategy, Component, Directive, DoCheck, ElementRef, HostListener, Input, Renderer2, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {StateHistory} from "app/shared/undo/undo-redo-state";

import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {getMatIconClass, MatIconSize} from "framework/ui/layout";

@Component({
               selector       : "o3-undo-redo[stateHistory]",
               templateUrl    : "./undo-redo.component.html",
               styleUrls      : ["./undo-redo.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class UndoRedoComponent<T> extends SharedSvc.BaseApplicationComponent
{
    static ngAcceptInputType_disableRipple: boolean | "";

    private m_stateHistory: StateHistory<T>;
    @Input() set stateHistory(history: StateHistory<T>)
    {
        if (history)
        {
            this.m_stateHistory = history;
            this.subscribeToObservable(history.changed, () => this.markForCheck());
        }
    }

    get stateHistory(): StateHistory<T>
    {
        return this.m_stateHistory;
    }

    get undoTooltip(): string
    {
        return this.m_stateHistory?.undoDescription() || "";
    }

    get redoTooltip(): string
    {
        return this.m_stateHistory?.redoDescription() || "";
    }

    sizeClass: string;

    @Input() set size(size: MatIconSize)
    {
        this.sizeClass = getMatIconClass(size);
    }

    @Input() @CoerceBoolean() disableRipple: boolean = false;

    @ViewChild("test_undo", {read: ElementRef}) test_undo: ElementRef;
    @ViewChild("test_redo", {read: ElementRef}) test_redo: ElementRef;
}

@Directive()
export abstract class UndoRedoButtonDirective implements DoCheck
{
    @Input() disabledClass: string = "mat-button-disabled";

    constructor(protected elRef: ElementRef<HTMLButtonElement>,
                protected renderer: Renderer2)
    {
    }

    protected abstract disabled(): boolean;

    ngDoCheck()
    {
        if (this.disabled())
        {
            this.renderer.setProperty(this.elRef.nativeElement, "disabled", true);
            this.renderer.addClass(this.elRef.nativeElement, this.disabledClass);
        }
        else
        {
            this.renderer.setProperty(this.elRef.nativeElement, "disabled", false);
            this.renderer.removeClass(this.elRef.nativeElement, this.disabledClass);
        }
    }
}

@Directive({
               selector: "[o3UndoButton]"
           })
export class UndoButtonDirective<T> extends UndoRedoButtonDirective
{
    @Input("o3UndoButton") stateHistory: StateHistory<T>;

    @HostListener("click") clicked()
    {
        this.stateHistory?.undo();
    }

    protected disabled(): boolean
    {
        return !this.stateHistory?.canUndo();
    }
}

@Directive({
               selector: "[o3RedoButton]"
           })
export class RedoButtonDirective<T> extends UndoRedoButtonDirective
{
    @Input("o3RedoButton") stateHistory: StateHistory<T>;

    @HostListener("click") clicked()
    {
        this.stateHistory?.redo();
    }

    protected disabled(): boolean
    {
        return !this.stateHistory?.canRedo();
    }
}
