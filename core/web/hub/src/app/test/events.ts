export const OPTIO3_ATTRIBUTE = "o3-test-id";

export interface TestEvent
{
    eventId?: string;
    type: keyof Events;
}

export interface ClickEvent extends TestEvent
{
    type: "click";
    numClicks: number;
    selector: string;
    elementName?: string;
}

export interface ClickPointEvent extends TestEvent
{
    type: "clickPoint";
    point: Point;
    pointDescription: string;
}

export interface MouseMoveEvent extends TestEvent
{
    type: "mouseMove";
    point: Point;
    pointDescription: string;
}

export interface JitterMouseInitializeEvent extends TestEvent
{
    type: "jitterMouseInitialize";
    x: number;
    y: number;
    interval: number;
}

export interface JitterMouseTerminateEvent extends TestEvent
{
    type: "jitterMouseTerminate";
}

export interface HoverEvent extends TestEvent
{
    type: "hover";
    selector: string;
    elementName?: string;
}

export interface DragEvent extends TestEvent
{
    type: "drag";
    selector?: string;
    source?: Point;
    destination: Point;
    release: boolean;
    elementName?: string;
}

export interface WheelEvent extends TestEvent
{
    type: "wheel";
    dx: number;
    dy: number;
}

export interface ScrollEvent extends TestEvent
{
    type: "scroll";
    dy: number;
    selector?: string;
}

export interface TypeEvent extends TestEvent
{
    type: "type";
    selector?: string;
    text?: string;
    overwrite?: boolean;
    keyEvents?: KeyEvent[];
    elementName?: string;
}

export interface KeyEvent
{
    type: "down" | "up" | "press";
    key: KeyInput;
}

export type KeyInput =
    "0"
    | "1"
    | "2"
    | "3"
    | "4"
    | "5"
    | "6"
    | "7"
    | "8"
    | "9"
    | "Power"
    | "Eject"
    | "Abort"
    | "Help"
    | "Backspace"
    | "Tab"
    | "Numpad5"
    | "NumpadEnter"
    | "Enter"
    | "\r"
    | "\n"
    | "ShiftLeft"
    | "ShiftRight"
    | "ControlLeft"
    | "ControlRight"
    | "AltLeft"
    | "AltRight"
    | "Pause"
    | "CapsLock"
    | "Escape"
    | "Convert"
    | "NonConvert"
    | "Space"
    | "Numpad9"
    | "PageUp"
    | "Numpad3"
    | "PageDown"
    | "End"
    | "Numpad1"
    | "Home"
    | "Numpad7"
    | "ArrowLeft"
    | "Numpad4"
    | "Numpad8"
    | "ArrowUp"
    | "ArrowRight"
    | "Numpad6"
    | "Numpad2"
    | "ArrowDown"
    | "Select"
    | "Open"
    | "PrintScreen"
    | "Insert"
    | "Numpad0"
    | "Delete"
    | "NumpadDecimal"
    | "Digit0"
    | "Digit1"
    | "Digit2"
    | "Digit3"
    | "Digit4"
    | "Digit5"
    | "Digit6"
    | "Digit7"
    | "Digit8"
    | "Digit9"
    | "KeyA"
    | "KeyB"
    | "KeyC"
    | "KeyD"
    | "KeyE"
    | "KeyF"
    | "KeyG"
    | "KeyH"
    | "KeyI"
    | "KeyJ"
    | "KeyK"
    | "KeyL"
    | "KeyM"
    | "KeyN"
    | "KeyO"
    | "KeyP"
    | "KeyQ"
    | "KeyR"
    | "KeyS"
    | "KeyT"
    | "KeyU"
    | "KeyV"
    | "KeyW"
    | "KeyX"
    | "KeyY"
    | "KeyZ"
    | "MetaLeft"
    | "MetaRight"
    | "ContextMenu"
    | "NumpadMultiply"
    | "NumpadAdd"
    | "NumpadSubtract"
    | "NumpadDivide"
    | "F1"
    | "F2"
    | "F3"
    | "F4"
    | "F5"
    | "F6"
    | "F7"
    | "F8"
    | "F9"
    | "F10"
    | "F11"
    | "F12"
    | "F13"
    | "F14"
    | "F15"
    | "F16"
    | "F17"
    | "F18"
    | "F19"
    | "F20"
    | "F21"
    | "F22"
    | "F23"
    | "F24"
    | "NumLock"
    | "ScrollLock"
    | "AudioVolumeMute"
    | "AudioVolumeDown"
    | "AudioVolumeUp"
    | "MediaTrackNext"
    | "MediaTrackPrevious"
    | "MediaStop"
    | "MediaPlayPause"
    | "Semicolon"
    | "Equal"
    | "NumpadEqual"
    | "Comma"
    | "Minus"
    | "Period"
    | "Slash"
    | "Backquote"
    | "BracketLeft"
    | "Backslash"
    | "BracketRight"
    | "Quote"
    | "AltGraph"
    | "Props"
    | "Cancel"
    | "Clear"
    | "Shift"
    | "Control"
    | "Alt"
    | "Accept"
    | "ModeChange"
    | " "
    | "Print"
    | "Execute"
    | "\u0000"
    | "a"
    | "b"
    | "c"
    | "d"
    | "e"
    | "f"
    | "g"
    | "h"
    | "i"
    | "j"
    | "k"
    | "l"
    | "m"
    | "n"
    | "o"
    | "p"
    | "q"
    | "r"
    | "s"
    | "t"
    | "u"
    | "v"
    | "w"
    | "x"
    | "y"
    | "z"
    | "Meta"
    | "*"
    | "+"
    | "-"
    | "/"
    | ";"
    | "="
    | ","
    | "."
    | "`"
    | "["
    | "\\"
    | "]"
    | "'"
    | "Attn"
    | "CrSel"
    | "ExSel"
    | "EraseEof"
    | "Play"
    | "ZoomOut"
    | ")"
    | "!"
    | "@"
    | "#"
    | "$"
    | "%"
    | "^"
    | "&"
    | "("
    | "A"
    | "B"
    | "C"
    | "D"
    | "E"
    | "F"
    | "G"
    | "H"
    | "I"
    | "J"
    | "K"
    | "L"
    | "M"
    | "N"
    | "O"
    | "P"
    | "Q"
    | "R"
    | "S"
    | "T"
    | "U"
    | "V"
    | "W"
    | "X"
    | "Y"
    | "Z"
    | ":"
    | "<"
    | "_"
    | ">"
    | "?"
    | "~"
    | "{"
    | "|"
    | "}"
    | "\""
    | "SoftLeft"
    | "SoftRight"
    | "Camera"
    | "Call"
    | "EndCall"
    | "VolumeDown"
    | "VolumeUp";

export function keyDown(key: KeyInput): KeyEvent
{
    return {
        type: "down",
        key : key
    };
}

export function keyUp(key: KeyInput): KeyEvent
{
    return {
        type: "up",
        key : key
    };
}

export function keyPress(key: KeyInput): KeyEvent
{
    return {
        type: "press",
        key : key
    };
}

export interface TestDefinition
{
    testId: string;
    testName: string;
    testTimeout?: number;
    categories: string[];
}

export interface TestDefinitionsEvent extends TestEvent
{
    type: "testDefinitions";
    tests: TestDefinition[];
}

export interface TestStartEvent extends TestEvent
{
    type: "testStart";
    testId: string;
    testName: string;
}

export interface TestResultEvent extends TestEvent
{
    type: "testResult";
    testId: string;
    testName: string;
    failure: string;
}

export interface TestEventAck extends TestEvent
{
    type: "ack";
    eventId: string;
    success: boolean;
}

export interface GenerateDataEvent extends TestEvent
{
    type: "generateData";
}

export interface DataGeneratedEvent extends TestEvent
{
    type: "dataGenerated";
    success: boolean;
}

export type Events = {
    "click": ClickEvent,
    "clickPoint": ClickPointEvent,
    "drag": DragEvent,
    "hover": HoverEvent,
    "jitterMouseInitialize": JitterMouseInitializeEvent,
    "jitterMouseTerminate": JitterMouseTerminateEvent,
    "mouseMove": MouseMoveEvent,
    "scroll": ScrollEvent,
    "type": TypeEvent,
    "wheel": WheelEvent,
    "testDefinitions": TestDefinitionsEvent,
    "testStart": TestStartEvent,
    "testResult": TestResultEvent,
    "ack": TestEventAck,
    "generateData": GenerateDataEvent,
    "dataGenerated": DataGeneratedEvent
};

export interface Point
{
    x: number;
    y: number;
}

export function isEvent<K extends keyof Events>(type: K,
                                                event: TestEvent): event is Events[K]
{
    return event.type === type;
}

export function asEvent<K extends keyof Events>(type: K,
                                                event: TestEvent): Events[K]
{
    if (isEvent(type, event))
    {
        return event;
    }

    throw Error("Event type mismatch");
}
