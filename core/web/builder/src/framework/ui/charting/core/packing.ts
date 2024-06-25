import {UtilsService} from "framework/services/utils.service";
import {Circle, Vector2} from "framework/ui/charting/charting-math";
import {ChartBox} from "framework/ui/charting/core/basics";

export class PackingAlgorithms
{
    public static packSiblings(siblings: Circle[])
    {
        let n = siblings ? siblings.length : 0;

        // Do nothing if no circles given
        if (n <= 0) return;

        // Sort the siblings by radius
        siblings.sort((a,
                       b) =>
                      { return b.radius - a.radius; });

        // Place first 3 circles to establish the front-chain
        let a: Circle;
        let b: Circle;
        let c: Circle;

        // Place first circle
        a   = siblings[0];
        a.x = 0;
        a.y = 0;

        // Continue if more than one circle
        if (n <= 1) return;

        // Place second circle
        b   = siblings[1];
        a.x = -b.radius;
        b.x = a.radius;
        b.y = 0;

        // Continue if there are more than two circles
        if (n <= 2) return;

        // Place the third circle
        c = siblings[2];
        this.place(b, a, c);

        // Initialize front-chain using the first 3 circles
        let aNode  = new Node(a);
        let bNode  = new Node(b);
        let cNode  = new Node(c);
        aNode.next = cNode.previous = bNode;
        bNode.next = aNode.previous = cNode;
        cNode.next = bNode.previous = aNode;

        // Attempt to place all additional siblings
        pack: for (let i = 3; i < n; i++)
        {
            this.place(aNode.circle, bNode.circle, siblings[i]);
            cNode = new Node(siblings[i]);

            // Find the closest intersecting circle on the front-chain
            let j  = bNode.next;
            let k  = aNode.previous;
            let sj = bNode.circle.radius;
            let sk = aNode.circle.radius;

            do
            {
                if (sj <= sk)
                {
                    if (j.circle.intersects(cNode.circle))
                    {
                        --i;
                        bNode          = j;
                        aNode.next     = bNode;
                        bNode.previous = aNode;
                        continue pack;
                    }

                    sj += j.circle.radius;
                    j = j.next;
                }
                else
                {
                    if (k.circle.intersects(cNode.circle))
                    {
                        --i;
                        aNode          = k;
                        aNode.next     = bNode;
                        bNode.previous = aNode;
                        continue pack;
                    }

                    sk += k.circle.radius;
                    k = k.previous;
                }
            }
            while (j !== k.next);

            // Insert the new circle between a and b
            cNode.previous = aNode;
            cNode.next     = bNode;
            aNode.next     = bNode.previous = bNode = cNode;

            // Compute the new closest pair to the centroid
            let aa = this.score(aNode);
            let ca;
            while ((cNode = cNode.next) !== bNode)
            {
                if ((ca = this.score(cNode)) < aa)
                {
                    aNode = cNode;
                    aa    = ca;
                }
            }

            bNode = aNode.next;
        }
    }

    public static histogramMaxAreaRectangle(histogram: number[],
                                            maxWidth: number,
                                            maxHeight: number): ChartBox
    {
        histogram = UtilsService.arrayCopy(histogram);
        maxWidth  = maxWidth || Number.MAX_SAFE_INTEGER;
        maxHeight = maxHeight || Number.MAX_SAFE_INTEGER;

        let maxAreaX      = 0;
        let maxAreaWidth  = 0;
        let maxAreaHeight = 0;
        let maxArea       = 0;
        let heightStack   = [];
        let idxStack      = [];
        let topOfStack    = -1;

        histogram.push(0);
        for (let i = 0; i < histogram.length; i++)
        {
            let prevIdx    = histogram.length;
            let currHeight = histogram[i];

            while (topOfStack >= 0 && heightStack[topOfStack] > currHeight)
            {
                let x      = prevIdx = idxStack.pop();
                let width  = Math.min(i - x, maxWidth);
                let height = Math.min(heightStack.pop(), maxHeight);
                topOfStack--;

                let area = width * height;
                if (area > maxArea)
                {
                    maxAreaX      = x;
                    maxAreaWidth  = width;
                    maxAreaHeight = height;
                    maxArea       = area;
                }
            }

            if (topOfStack === -1 || heightStack[topOfStack] < currHeight)
            {
                heightStack.push(currHeight);
                idxStack.push(Math.min(prevIdx, i));
                topOfStack++;
            }
        }

        return maxArea ? new ChartBox(maxAreaX, null, maxAreaWidth, maxAreaHeight) : null;
    }

    public static enclose(siblings: Circle[]): Circle
    {
        let center = this.centerOfMass(siblings);
        let max    = 0;

        // Find the largest distance from center of mass to the outer edge of any circle
        for (let circle of siblings)
        {
            let d = circle.distanceToVector(center);
            let r = circle.radius;
            if (d + r > max) max = d + r;
        }

        return new Circle(center.x, center.y, max);
    }

    private static place(a: Circle,
                         b: Circle,
                         c: Circle)
    {
        let delta = b.differenceVector(a);
        let d2    = b.distanceToVectorSquared(a);

        if (d2)
        {
            // Compute min distance between circles squared (if they were moved to be touching)
            let a2 = (a.radius + c.radius) ** 2;
            let b2 = (b.radius + c.radius) ** 2;

            if (a2 > b2)
            {
                let x = (d2 + b2 - a2) / (2 * d2);
                let y = Math.sqrt(Math.max(0, (b2 / d2) - (x * x)));

                c.x = b.x - x * delta.x - y * delta.y;
                c.y = b.y - x * delta.y + y * delta.x;
            }
            else
            {
                let x = (d2 + a2 - b2) / (2 * d2);
                let y = Math.sqrt(Math.max(0, (a2 / d2) - (x * x)));

                c.x = a.x + x * delta.x - y * delta.y;
                c.y = a.y + x * delta.y + y * delta.x;
            }
        }
        else
        {
            // If a and b are degenerate, place c besides a
            c.x = a.x + c.radius;
            c.y = a.y;
        }
    }

    private static score(node: Node): number
    {
        let a = node.circle;
        let b = node.next.circle;

        let ab = a.radius + b.radius;
        let cx = (a.x * b.radius + b.x * a.radius) / ab;
        let cy = (a.y * b.radius + b.y * a.radius) / ab;
        return cx * cx + cy * cy;
    }

    private static centerOfMass(circles: Circle[]): Vector2
    {
        // Return (0,0) if no circles
        if (!circles || circles.length === 0) return new Vector2(0, 0);

        let xWeight      = 0;
        let yWeight      = 0;
        let radiusWeight = 0;

        for (let circle of circles)
        {
            xWeight += (circle.x * circle.radius);
            yWeight += (circle.y * circle.radius);
            radiusWeight += circle.radius;
        }

        return new Vector2(xWeight / radiusWeight, yWeight / radiusWeight);
    }
}

class Node
{
    constructor(public circle: Circle,
                public next: Node     = null,
                public previous: Node = null)
    {}
}
