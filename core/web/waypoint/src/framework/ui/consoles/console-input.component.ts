import {Component, ElementRef, EventEmitter, Input, Output, Renderer2} from "@angular/core";

@Component({
               selector   : "o3-console-input",
               templateUrl: "./console-input.component.html"
           })
export class ConsoleInputComponent
{
    @Input() placeholder: string;

    inputText: string;

    inputHistory: string[] = [];

    inputHistoryOffset: number = 0;

    @Output() keyDown = new EventEmitter<ConsoleInputKeyEventArgs>();

    @Output() commandSubmitted = new EventEmitter<ConsoleInputCommandEventArgs>();

    constructor(private element: ElementRef,
                private renderer: Renderer2)
    {
    }

    submitInput()
    {
        if (this.inputText)
        {
            let command     = new ConsoleInputCommandEventArgs();
            command.command = this.inputText;
            this.commandSubmitted.emit(command);

            if (!command.cancel)
            {
                this.inputHistory.push(this.inputText);
                this.inputHistoryOffset = 0;
                this.inputText          = null;
            }
        }
    }

    onKeyDown(event: KeyboardEvent)
    {
        const ArrowUp: number   = 38;
        const ArrowDown: number = 40;

        let keycode: number = event.keyCode;
        let key             = event.key;

        let args     = new ConsoleInputKeyEventArgs();
        args.key     = key;
        args.keycode = keycode;
        this.keyDown.emit(args);

        if (!args.cancel)
        {
            if (this.inputHistory && this.inputHistory.length)
            {
                let index = this.inputHistory.length - this.inputHistoryOffset;
                if (index < 0) index = 0;
                if (index > this.inputHistory.length - 1) index = this.inputHistory.length - 1;

                if (keycode == ArrowUp)
                {
                    this.inputText          = this.inputHistory[index];
                    this.inputHistoryOffset = this.inputHistoryOffset + 1;
                    if (this.inputHistoryOffset >= this.inputHistory.length) this.inputHistoryOffset = this.inputHistory.length;
                    event.preventDefault();
                    event.stopPropagation();
                }
                else if (keycode == ArrowDown)
                {
                    this.inputText          = this.inputHistory[index];
                    this.inputHistoryOffset = this.inputHistoryOffset - 1;
                    if (this.inputHistoryOffset < 0) this.inputHistoryOffset = 0;
                    event.preventDefault();
                    event.stopPropagation();
                }
            }
        }
        else
        {
            event.preventDefault();
            event.stopPropagation();
        }
    }
}

export class ConsoleInputCommandEventArgs
{
    command: string;

    cancel: boolean;
}

export class ConsoleInputKeyEventArgs
{
    cancel: boolean;

    keycode: number;

    key: string;
}
