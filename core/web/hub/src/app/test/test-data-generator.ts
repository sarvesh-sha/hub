export class TestDataGenerator
{
    public generateCanvasImage(fn: (context: CanvasRenderingContext2D) => void, width: number, height: number): string
    {
        const canvas: HTMLCanvasElement = document.createElement("canvas");
        canvas.width                    = width;
        canvas.height                   = height;

        fn(canvas.getContext("2d"));

        return canvas.toDataURL();
    }
}
