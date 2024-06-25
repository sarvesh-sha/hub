declare module "quill";
declare module "quill-image-resize";

declare interface Window
{
    OPTIO3_VERSION: string;
    OPTIO3_COMMIT: string;
    ResizeObserver: ResizeObserver_Static;
}

interface ResizeObserver
{
    observe(element: HTMLElement): void;
    unobserve(element: HTMLElement): void;
    disconnect(): void;
}

interface ResizeObserverEntry
{
    target: HTMLElement;
    contentRect: DOMRectReadOnly;
    contentBoxSize: ResizeObserverSize | ResizeObserverSize[];
    borderBoxSize: ResizeObserverSize | ResizeObserverSize[];
}

interface ResizeObserverSize
{
    inlineSize: number;
    blockSize: number;
}

interface ResizeObserver_Static {
    new(callback: (entries: ResizeObserverEntry[]) => void): ResizeObserver
}
