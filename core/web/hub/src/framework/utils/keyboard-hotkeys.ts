export enum HotkeyAction
{
    Copy   = "Copy",
    Cut    = "Cut",
    Paste  = "Paste",
    Undo   = "Undo",
    Redo   = "Redo",
    Delete = "Delete"
}

export function getHotkeyAction(event: KeyboardEvent): HotkeyAction
{
    switch (event.key)
    {
        case "c":
            if (event.metaKey || event.ctrlKey)
            {
                return HotkeyAction.Copy;
            }
            break;

        case "x":
            if (event.metaKey || event.ctrlKey)
            {
                return HotkeyAction.Cut;
            }
            break;

        case "v":
            if (event.metaKey || event.ctrlKey)
            {
                return HotkeyAction.Paste;
            }
            break;

        case "z":
            if (event.ctrlKey)
            {
                return HotkeyAction.Undo;
            }
            else if (event.metaKey)
            {
                return event.shiftKey ? HotkeyAction.Redo : HotkeyAction.Undo;
            }
            break;

        case "y":
            if (event.ctrlKey)
            {
                return HotkeyAction.Redo;
            }
            break;

        case "Backspace":
        case "Delete":
            return HotkeyAction.Delete;
    }

    return null;
}
