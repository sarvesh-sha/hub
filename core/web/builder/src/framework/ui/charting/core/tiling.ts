import {AxisAlignedBoundingBox} from "framework/ui/charting/charting-math";

export class TilingAlgorithms
{
    static slice(values: number[],
                 container: AxisAlignedBoundingBox): AxisAlignedBoundingBox[]
    {
        let sum   = 0;
        let nodes = [];

        for (let value of values)
        {
            sum += value;
            nodes.push(new Node(value));
        }

        let index  = -1;
        let count  = nodes.length;
        let factor = container.height / sum;
        let offset = 0;

        while (++index < count)
        {
            let node        = nodes[index];
            node.box.x      = container.x;
            node.box.width  = container.width;
            node.box.y      = container.y + (offset * factor);
            node.box.height = node.value * factor;
            offset += node.value;
        }

        return nodes.map((node) => { return node.box; });
    }

    static dice(values: number[],
                container: AxisAlignedBoundingBox): AxisAlignedBoundingBox[]
    {
        let sum   = 0;
        let nodes = [];

        for (let value of values)
        {
            sum += value;
            nodes.push(new Node(value));
        }

        let index  = -1;
        let count  = nodes.length;
        let factor = container.width / sum;
        let offset = 0;

        while (++index < count)
        {
            let node        = nodes[index];
            node.box.y      = container.y;
            node.box.height = container.height;
            node.box.x      = container.x + (offset * factor);
            node.box.width  = node.value * factor;
            offset += node.value;
        }

        return nodes.map((node) => { return node.box; });
    }


    static squarify(ratio: number,
                    values: number[],
                    container: AxisAlignedBoundingBox)
    {
        let sum   = 0;
        let nodes = [];

        for (let value of values)
        {
            sum += value;
            nodes.push(new Node(value));
        }

        let areas         = [];
        let containerArea = container.area();

        for (let value of values)
        {
            areas.push(value * (containerArea / sum));
        }

        let remaining = container.clone();
        let index     = 0;
        let count     = nodes.length;
        let direction = this.direction(container);
        let finished  = false;

        let boxes: AxisAlignedBoundingBox[] = [];

        // Fill in an alternating pattern
        while (!finished)
        {
            // Initialize to fill in next direction
            let start                                       = index;
            let end                                         = index;
            let side1                                       = direction ? remaining.height : remaining.width;
            let side2                                       = 0;
            let currentSum                                  = 0;
            let previousSum                                 = 0;
            let currentDivisions: AxisAlignedBoundingBox[]  = [];
            let previousDivisions: AxisAlignedBoundingBox[] = [];
            let currentRatio                                = Infinity;
            let previousRatio                               = Infinity;
            let subset                                      = [];
            let area;
            let last;

            // Fill to until the ideal ratio is reached
            do
            {
                // Commit current to previous
                previousSum       = currentSum;
                previousDivisions = currentDivisions;
                previousRatio     = currentRatio;

                // Update area sum
                currentSum += areas[end];

                // Calculate updated sides and ratios
                side2            = (currentSum / side1);
                area             = this.subArea(remaining, direction, side1, side2);
                subset           = values.slice(start, end + 1);
                currentDivisions = direction ? TilingAlgorithms.slice(subset, area) : TilingAlgorithms.dice(subset, area);
                last             = currentDivisions[currentDivisions.length - 1];
                currentRatio     = this.aspectRatio(last.width, last.height);

                // Increment and move to next value
                end++;

                // If the end of all values was reached, commit values ahead of time
                if (end >= count)
                {
                    previousSum       = currentSum;
                    previousDivisions = currentDivisions;
                    previousRatio     = currentRatio;
                    finished          = true;
                }
            }
            while (Math.abs(currentRatio - ratio) < Math.abs(previousRatio - ratio));

            // Add the boxes
            boxes = boxes.concat(previousDivisions);

            // Recompute side2
            side2 = (previousSum / side1);

            // Reduce remaining size
            if (direction)
            {
                remaining.x += side2;
                remaining.width -= side2;
            }
            else
            {
                remaining.height -= side2;
            }

            // Commit index change
            index = end - 1;

            // Pick next direction
            direction = this.direction(remaining);
        }

        // Return all boxes
        return boxes;
    }

    private static direction(box: AxisAlignedBoundingBox): boolean
    {
        return box.width / box.height >= box.height / box.width;
    }

    private static subArea(container: AxisAlignedBoundingBox,
                           direction: boolean,
                           side1: number,
                           side2: number): AxisAlignedBoundingBox
    {
        if (direction)
        {
            return new AxisAlignedBoundingBox(container.x, container.y, side2, side1);

        }
        else
        {
            return new AxisAlignedBoundingBox(container.x, container.y + (container.height - side2), side1, side2);
        }
    }

    private static aspectRatio(side1: number,
                               side2: number)
    {
        return Math.max(side1, side2) / Math.min(side1, side2);
    }
}

class Node
{
    constructor(public value: number,
                public box: AxisAlignedBoundingBox = new AxisAlignedBoundingBox(0, 0, 0, 0))
    {}
}
