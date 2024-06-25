import {ChartTextBox} from "framework/ui/charting/core/text";
import {numericSortBy, SortDirection} from "framework/utils/comparators";
import {SimplePubSub} from "framework/utils/pubsub";
import {MinPriorityQueue} from "framework/utils/queues";

// General use axis enum
export enum DataAxis
{
    X, Y
}

// Base class for a simple 2d vector
export class Vector2 implements ISpatiallyIndexable
{
    public static fromPolar(angle: number,
                            length: number)
    {
        return new Vector2(Math.cos(angle) * length, Math.sin(angle) * length);
    }

    public static pointDistance(x1: number,
                                y1: number,
                                x2: number,
                                y2: number): number
    {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    constructor(public x: number,
                public y: number)
    {}

    get left()
    {
        return this.x;
    }

    get right()
    {
        return this.x;
    }

    get top()
    {
        return this.y;
    }

    get bottom()
    {
        return this.y;
    }

    clone(): Vector2
    {
        return new Vector2(this.x, this.y);
    }

    toArray(): number[]
    {
        return [
            this.x,
            this.y
        ];
    }

    length(): number
    {
        return Vector2.pointDistance(this.x, this.y, 0, 0);
    }

    scale(scalar: number): void
    {
        this.x *= scalar;
        this.y *= scalar;
    }

    dot(vec2: Vector2): number
    {
        return vec2.x * this.x + vec2.y * this.y;
    }

    angle(vec2: Vector2): number
    {
        return Math.acos(this.dot(vec2) / (this.length() * vec2.length()));
    }

    angleOf(): number
    {
        let atan = Math.atan2(this.y, this.x) % Math.PI;
        return atan < 0 ? (Math.PI * 2) + atan : atan;
    }

    differenceVector(vector: Vector2): Vector2
    {
        return new Vector2(this.x - vector.x, this.y - vector.y);
    }

    add(vector: Vector2): Vector2
    {
        return new Vector2(this.x + vector.x, this.y + vector.y);
    }

    distanceToVector(vector: Vector2): number
    {
        return Vector2.pointDistance(this.x, this.y, vector.x, vector.y);
    }

    distanceToVectorSquared(vec2: Vector2): number
    {
        let dist = Vector2.pointDistance(this.x, this.y, vec2.x, vec2.y);
        return dist * dist;
    }

    distanceToBoxSquared(box: AxisAlignedBoundingBox): number
    {
        let dx = axisDistance(this.x, box.left, box.right);
        let dy = axisDistance(this.y, box.top, box.bottom);

        return dx * dx + dy * dy;

        function axisDistance(value: number,
                              min: number,
                              max: number): number
        {
            // Find the distance to the sides of a box for one axis
            // If the point is between the sides, return 0
            return value < min ? min - value : value <= max ? 0 : value - max;
        }
    }

    center()
    {
        return new Vector2((this.left + this.right) / 2, (this.top + this.bottom) / 2);
    }

    area(): number
    {
        return 0;
    }

    margin(): number
    {
        return 0;
    }

    intersectionArea(other: ISpatiallyIndexable): number
    {
        return 0;
    }

    min(): number
    {
        return Math.min(this.x, this.y);
    }

    max(): number
    {
        return Math.max(this.x, this.y);
    }

    lerp(target: Vector2,
         factor: number): Vector2
    {
        return new Vector2(this.x + ((target.x - this.x) * factor), this.y + ((target.y - this.y) * factor));
    }
}

export enum BoxAnchorPoint
{
    TOP, TOP_RIGHT, RIGHT, BOTTOM_RIGHT, BOTTOM, BOTTOM_LEFT, LEFT, TOP_LEFT, CENTER
}

export enum BoxSide
{
    TOP, RIGHT, BOTTOM, LEFT
}

// Base class for a simple axis aligned bounding box
export class AxisAlignedBoundingBox extends Vector2
{
    constructor(public x: number,
                public y: number,
                public width: number,
                public height: number)
    {
        super(x, y);
    }

    public static forPoints(points: Vector2[]): AxisAlignedBoundingBox
    {
        if (points.length === 0) return null;

        let minX = points[0].x;
        let maxX = points[0].x;
        let minY = points[0].y;
        let maxY = points[0].y;

        for (let i = 1; i < points.length; i++)
        {
            minX = points[i].x < minX ? points[i].x : minX;
            maxX = points[i].x > maxX ? points[i].x : maxX;
            minY = points[i].y < minY ? points[i].y : minY;
            maxY = points[i].y > maxY ? points[i].y : maxY;
        }

        return new AxisAlignedBoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    public static forSpatiallyIndexables(items: ISpatiallyIndexable[]): AxisAlignedBoundingBox
    {
        if (items.length === 0) return null;

        let minX = items[0].left;
        let maxX = items[0].right;
        let minY = items[0].top;
        let maxY = items[0].bottom;

        for (let i = 1; i < items.length; i++)
        {
            minX = items[i].left < minX ? items[i].left : minX;
            maxX = items[i].right > maxX ? items[i].right : maxX;
            minY = items[i].top < minY ? items[i].top : minY;
            maxY = items[i].bottom > maxY ? items[i].bottom : maxY;
        }

        return new AxisAlignedBoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    public static fromChartTextBox(box: ChartTextBox): AxisAlignedBoundingBox
    {
        return new AxisAlignedBoundingBox(box.x, box.y, box.width, box.height);
    }

    get left(): number
    {
        return this.width < 0 ? this.x + this.width : this.x;
    }

    get right(): number
    {
        return this.width < 0 ? this.x : this.x + this.width;
    }

    get top(): number
    {
        return this.height < 0 ? this.y + this.height : this.y;
    }

    get bottom(): number
    {
        return this.height < 0 ? this.y : this.y + this.height;
    }

    center(): Vector2
    {
        return new Vector2(this.x + (this.width / 2), this.y + (this.height / 2));
    }

    area(): number
    {
        return Math.abs(this.width * this.height);
    }

    margin(): number
    {
        return Math.abs(this.width) + Math.abs(this.height);
    }

    intersectionArea(other: ISpatiallyIndexable): number
    {
        let width  = Math.max(0, Math.min(this.right, other.right) - Math.max(this.left, other.left));
        let height = Math.max(0, Math.min(this.bottom, other.bottom) - Math.max(this.top, other.top));

        return width * height;
    }

    getAnchorPoint(anchor: BoxAnchorPoint): Vector2
    {
        switch (anchor)
        {
            case BoxAnchorPoint.TOP:
                return new Vector2((this.left + this.right) / 2, this.top);
            case BoxAnchorPoint.TOP_RIGHT:
                return new Vector2(this.right, this.top);
            case BoxAnchorPoint.RIGHT:
                return new Vector2(this.right, (this.top + this.bottom) / 2);
            case BoxAnchorPoint.BOTTOM_RIGHT:
                return new Vector2(this.right, this.bottom);
            case BoxAnchorPoint.BOTTOM:
                return new Vector2((this.left + this.right) / 2, this.bottom);
            case BoxAnchorPoint.BOTTOM_LEFT:
                return new Vector2(this.left, this.bottom);
            case BoxAnchorPoint.LEFT:
                return new Vector2(this.left, (this.top + this.bottom) / 2);
            case BoxAnchorPoint.TOP_LEFT:
                return new Vector2(this.left, this.top);
            default:
                return this.center();
        }
    }

    getSideSegment(side: BoxSide): Vector2[]
    {
        switch (side)
        {
            case BoxSide.TOP:
                return [
                    this.getAnchorPoint(BoxAnchorPoint.TOP_LEFT),
                    this.getAnchorPoint(BoxAnchorPoint.TOP_RIGHT)
                ];
            case BoxSide.RIGHT:
                return [
                    this.getAnchorPoint(BoxAnchorPoint.TOP_RIGHT),
                    this.getAnchorPoint(BoxAnchorPoint.BOTTOM_RIGHT)
                ];
            case BoxSide.BOTTOM:
                return [
                    this.getAnchorPoint(BoxAnchorPoint.BOTTOM_RIGHT),
                    this.getAnchorPoint(BoxAnchorPoint.BOTTOM_LEFT)
                ];
            case BoxSide.LEFT:
                return [
                    this.getAnchorPoint(BoxAnchorPoint.BOTTOM_LEFT),
                    this.getAnchorPoint(BoxAnchorPoint.TOP_LEFT)
                ];
        }
    }

    clone(): AxisAlignedBoundingBox
    {
        return new AxisAlignedBoundingBox(this.x, this.y, this.width, this.height);
    }

    hit(point: Vector2): boolean
    {
        return this.hitXY(point.x, point.y);
    }

    hitXY(x: number,
          y: number): boolean
    {
        if (x < this.left || x > this.right || y < this.top || y > this.bottom) return false;
        return true;
    }

    flipHorizontal(): void
    {
        this.x     = this.x + this.width;
        this.width = -this.width;
    }

    flipVertical(): void
    {
        this.y      = this.y + this.height;
        this.height = -this.height;
    }

    unflip(): void
    {
        if (this.height < 0) this.flipVertical();
        if (this.width < 0) this.flipHorizontal();
    }

    scale(scalar: number): void
    {
        this.scale2(scalar, scalar);
    }

    scale2(x: number,
           y: number): void
    {
        // TODO: Allow scaling about different points of the box instead of just center
        let dx = (this.width - (this.width * x)) / 2;
        let dy = (this.height - (this.height * y)) / 2;

        this.width *= x;
        this.height *= y;

        this.x += dx;
        this.y += dy;
    }

    updateToFit(item: ISpatiallyIndexable): AxisAlignedBoundingBox
    {
        // Find the extremes
        let left   = Math.min(item.left, this.left);
        let right  = Math.max(item.right, this.right);
        let top    = Math.min(item.top, this.top);
        let bottom = Math.max(item.bottom, this.bottom);

        // Update x/y and width/height
        this.x      = left;
        this.y      = top;
        this.width  = right - left;
        this.height = bottom - top;

        // Return current instance
        return this;
    }

    transformToFit(target: AxisAlignedBoundingBox,
                   maintainRatio: boolean = false): Transform
    {
        let fx   = target.width / this.width;
        let fy   = target.height / this.height;
        let fMin = Math.min(fx, fy);

        if (maintainRatio)
        {
            fx = fMin;
            fy = fMin;
        }

        let dx = (target.width - (this.width * fx)) / 2;
        let dy = (target.height - (this.height * fy)) / 2;

        let translation = new Vector2(target.x - this.x + dx, target.y - this.y + dy);
        let factor      = new Vector2(fx, fy);
        return new Transform().translate(translation)
                              .scale(factor);
    }
}

// Base class for a simple circle
export class Circle extends Vector2
{
    private m_radius: number;
    private m_radiusSquared: number;

    constructor(public x: number,
                public y: number,
                r: number)
    {
        super(x, y);

        this.radius = r;
    }

    public get radius(): number
    {
        return this.m_radius;
    }

    public set radius(r: number)
    {
        this.m_radius        = r;
        this.m_radiusSquared = r * r;
    }

    public get radiusSquared(): number
    {
        return this.m_radiusSquared;
    }

    hit(point: Vector2): boolean
    {
        return this.hitXY(point.x, point.y);
    }

    hitXY(x: number,
          y: number): boolean
    {
        if ((x - this.x) * (x - this.x) + (y - this.y) * (y - this.y) <= this.m_radiusSquared) return true;
        return false;
    }

    intersects(other: Circle): boolean
    {
        return this.distanceToVectorSquared(other) < (this.radius + other.radius) ** 2;
    }

    copy(other: Circle)
    {
        this.x      = other.x;
        this.y      = other.y;
        this.radius = other.radius;
    }

    getBoundingBox(): AxisAlignedBoundingBox
    {
        return new AxisAlignedBoundingBox(this.x - this.radius, this.y - this.radius, this.radius * 2, this.radius * 2);
    }
}

export class Transform extends SimplePubSub
{
    constructor(public a: number  = 1,
                public b: number  = 0,
                public c: number  = 0,
                public d: number  = 1,
                public tx: number = 0,
                public ty: number = 0)
    {
        super();
    }

    // Get isIdentity flag state
    isIdentity(): boolean
    {
        return this.a === 1 && this.b === 0 && this.c === 0 && this.d === 1 && this.tx === 0 && this.ty === 0;
    }

    // Manually sets the transformation matrix state
    set(a: number,
        b: number,
        c: number,
        d: number,
        tx: number,
        ty: number): Transform
    {
        this.a  = a;
        this.b  = b;
        this.c  = c;
        this.d  = d;
        this.tx = tx;
        this.ty = ty;

        this.changed();

        return this;
    }

    // Set the matrix state to match the given transform
    setToMatch(other: Transform): Transform
    {
        return this.set(other.a, other.b, other.c, other.d, other.tx, other.ty);
    }

    // Resets the transform to the default state (identity matrix)
    reset(): Transform
    {
        this.a  = 1;
        this.b  = 0;
        this.c  = 0;
        this.d  = 1;
        this.tx = 0;
        this.ty = 0;

        this.changed();

        return this;
    }

    // Creates a copy of the current transform
    clone(): Transform
    {
        return new Transform(this.a, this.b, this.c, this.d, this.tx, this.ty);
    }

    // Gets a string representation of the transformation matrix
    toString(): string
    {
        return `[ ${this.a}\t${this.c}\t${this.tx}\n  ${this.b}\t${this.d}\t${this.ty}\n  0\t0\t1 ]`;
    }

    // Applies a translation to the transform
    translate(delta: Vector2): Transform
    {
        this.tx += delta.x * this.a + delta.y * this.c;
        this.ty += delta.x * this.b + delta.y * this.d;

        this.changed();

        return this;
    }

    // Applies a scale to the transform
    scale(factor: Vector2,
          center?: Vector2): Transform
    {
        if (center) this.translate(center);

        this.a *= factor.x;
        this.b *= factor.x;
        this.c *= factor.y;
        this.d *= factor.y;

        if (center) this.translate(new Vector2(-center.x, -center.y));

        this.changed();

        return this;
    }

    // Applies a rotation to the transform
    rotate(degrees: number,
           center: Vector2 = new Vector2(0, 0)): Transform
    {
        let radians = degrees * (Math.PI / 180);

        let x   = center.x,
            y   = center.y,
            cos = Math.cos(radians),
            sin = Math.sin(radians),
            a   = this.a,
            b   = this.b,
            c   = this.c,
            d   = this.d,
            tx  = x - x * cos + y * sin,
            ty  = y - x * sin - y * cos;

        this.a = cos * a + sin * c;
        this.b = cos * b + sin * d;
        this.c = -sin * a + cos * c;
        this.d = -sin * b + cos * d;
        this.tx += tx * a + ty * c;
        this.ty += tx * b + ty * d;

        this.changed();

        return this;
    }

    // Inverts the coordinates of a given axis
    flip(axis: DataAxis): Transform
    {
        if (axis === DataAxis.X)
        {
            return this.scale(new Vector2(-1, 1));
        }
        else
        {
            return this.scale(new Vector2(1, -1));
        }
    }

    // Appends the given transform to the current transform
    append(transform: Transform): Transform
    {
        let a1  = this.a,
            b1  = this.b,
            c1  = this.c,
            d1  = this.d,
            a2  = transform.a,
            b2  = transform.b,
            c2  = transform.c,
            d2  = transform.d,
            tx2 = transform.tx,
            ty2 = transform.ty;

        this.a = a2 * a1 + c2 * c1;
        this.b = b2 * a1 + d2 * c1;
        this.c = a2 * b1 + c2 * d1;
        this.d = b2 * b1 + d2 * d1;
        this.tx += tx2 * a1 + ty2 * c1;
        this.ty += tx2 * b1 + ty2 * d1;

        this.changed();

        return this;
    }

    // Prepends the given transform to the current transform
    prepend(transform: Transform): Transform
    {
        let a1  = this.a,
            b1  = this.b,
            c1  = this.c,
            d1  = this.d,
            tx1 = this.tx,
            ty1 = this.ty,
            a2  = transform.a,
            b2  = transform.b,
            c2  = transform.c,
            d2  = transform.d,
            tx2 = transform.tx,
            ty2 = transform.ty;

        this.a  = a2 * a1 + b2 * b1;
        this.b  = a2 * c1 + b2 * d1;
        this.c  = c2 * a1 + d2 * b1;
        this.d  = c2 * c1 + d2 * d1;
        this.tx = a2 * tx1 + b2 * ty1 + tx2;
        this.ty = c2 * tx1 + d2 * ty1 + ty2;

        this.changed();

        return this;
    }

    // Inverts this transform's underlying matrix
    invert(): Transform
    {
        let a           = this.a,
            b           = this.b,
            c           = this.c,
            d           = this.d,
            tx          = this.tx,
            ty          = this.ty,
            determinant = a * d - b * c;

        if (determinant && !isNaN(determinant) && isFinite(tx) && isFinite(ty))
        {
            this.a  = d / determinant;
            this.b  = -b / determinant;
            this.c  = -c / determinant;
            this.d  = a / determinant;
            this.tx = (c * ty - d * tx) / determinant;
            this.ty = (b * tx - a * ty) / determinant;

            this.changed();

            return this;
        }

        return null;
    }

    extractTranslation(): Vector2
    {
        return new Vector2(this.tx, this.ty);
    }

    extractScale(): Vector2
    {
        return new Vector2((new Vector2(this.a, this.c)).length(), (new Vector2(this.b, this.d)).length());
    }

    // Applies the transform to a vector
    apply(vec2: Vector2): Vector2
    {
        return this.applyXY(vec2.x, vec2.y);
    }

    // Applies the transform to an arbitrary x/y pair
    applyXY(x: number,
            y: number): Vector2
    {
        return new Vector2(x * this.a + y * this.c + this.tx, x * this.b + y * this.d + this.ty);
    }

    applyToContext(ctx: CanvasRenderingContext2D): void
    {
        ctx.transform(this.a, this.b, this.c, this.d, this.tx, this.ty);
    }

    setContext(ctx: CanvasRenderingContext2D): void
    {
        ctx.setTransform(this.a, this.b, this.c, this.d, this.tx, this.ty);
    }

    private changed()
    {
        // Notify any listeners of a change event
        this.executeCallbacks("change");
    }
}

interface ISpatiallyIndexable
{
    intersectionArea: (other: ISpatiallyIndexable) => number,
    center: () => Vector2,
    area: () => number,
    margin: () => number,
    left: number,
    right: number,
    top: number,
    bottom: number,
}

class RStarTreeNode<T extends ISpatiallyIndexable> extends AxisAlignedBoundingBox
{
    public nodes: RStarTreeNode<T>[] = [];
    public data: T[]                 = [];

    constructor(public leaf: boolean = true)
    {
        super(0, 0, 0, 0);
    }

    get children(): ISpatiallyIndexable[]
    {
        return this.leaf ? this.data : this.nodes;
    }

    all(): T[]
    {
        if (this.leaf) return this.data;

        let all: T[] = [];
        for (let child of this.nodes) all = all.concat(child.all());

        return all;
    }

    refit(): void
    {
        this.fitTo(this.children);
    }

    sortAlongAxis(axis: DataAxis): void
    {
        if (axis === DataAxis.X)
        {
            this.children.sort(numericSortBy("left", SortDirection.ASCENDING));
            this.children.sort(numericSortBy("right", SortDirection.ASCENDING));
        }
        else if (axis === DataAxis.Y)
        {
            this.children.sort(numericSortBy("top", SortDirection.ASCENDING));
            this.children.sort(numericSortBy("bottom", SortDirection.ASCENDING));
        }
    }

    split(index: number): RStarTreeNode<T>[]
    {
        // Split the existing node splitting the data entries at the chosen index
        // We do not worry about sorting because it is still correctly sorted from chooseSplitIndex()
        // Create new entry groups
        let left  = this.children.slice(0, index);
        let right = this.children.slice(index);

        // Create new child nodes
        let leftNode  = new RStarTreeNode<T>();
        let rightNode = new RStarTreeNode<T>();

        // Adopt the same leaf-status as the original
        leftNode.leaf  = this.leaf;
        rightNode.leaf = this.leaf;

        // Add the split contents to the appropriate nodes
        leftNode.children.push(...left);
        rightNode.children.push(...right);

        // Refit the nodes
        leftNode.refit();
        rightNode.refit();

        // Return the resulting new nodes
        return [
            leftNode,
            rightNode
        ];
    }

    private fitTo(entries: ISpatiallyIndexable[]): void
    {
        let bounds  = AxisAlignedBoundingBox.forSpatiallyIndexables(entries);
        this.x      = bounds.x;
        this.y      = bounds.y;
        this.width  = bounds.width;
        this.height = bounds.height;
    }
}

export class RStarTree<T extends ISpatiallyIndexable>
{
    private m_root: RStarTreeNode<T> = null;
    private m_leafLevel: number      = 0;
    private m_maxEntries: number;
    private m_minEntries: number;
    private m_rebalances: boolean[];//Map<number, boolean>;

    private readonly REINSERT_PERCENT: number    = 0.3;
    private readonly MIN_ENTRIES_PERCENT: number = 0.4;

    constructor(maxEntries: number)
    {
        this.m_maxEntries = Math.round(maxEntries);
        this.m_minEntries = Math.ceil(maxEntries * this.MIN_ENTRIES_PERCENT);

        this.m_root = new RStarTreeNode<T>();
    }

    clear(): void
    {
        this.m_root = null;
    }

    all(): T[]
    {
        if (this.m_root) return this.m_root.all();
        return [];
    }

    insert(entry: T): void
    {
        // Reset rebalances state tracking
        this.m_rebalances = new Array(this.m_leafLevel);

        // Attempt to insert at the leaf level
        this.insertAtLevel(entry, this.m_leafLevel);
    }

    knn(point: Vector2,
        k: number,
        maxDistance: number = Infinity)
    {
        let nearest = [];
        let queue   = new MinPriorityQueue<ISpatiallyIndexable>();

        // Load the root into the queue
        queue.enqueue(point.distanceToBoxSquared(this.m_root), this.m_root);

        // Unpack until k nearest neighbors are found or options are exhausted
        while (nearest.length < k && !queue.isEmpty())
        {
            // If next node is a tree node, unpack it.
            // Otherwise keep popping until we reach a tree node or fill nearest array
            if (queue.peek() instanceof RStarTreeNode)
            {
                // Unpack this node
                let current = <RStarTreeNode<T>>queue.dequeue();
                for (let child of current.children)
                {
                    if (child instanceof RStarTreeNode)
                    {
                        queue.enqueue(point.distanceToBoxSquared(<RStarTreeNode<T>>child), child);
                    }
                    else
                    {
                        queue.enqueue(point.distanceToVectorSquared(<Vector2>child), child);
                    }
                }
            }
            else
            {
                // Keep popping while not tree nodes and not full
                while (!(queue.peek() instanceof RStarTreeNode) && nearest.length < k)
                {
                    // Only add if in range, otherwise, discard
                    if (queue.peekKey() <= maxDistance)
                    {
                        nearest.push(queue.dequeue());
                    }
                    else
                    {
                        queue.dequeue();
                    }
                }
            }
        }

        // Return the set of nearest points
        return nearest;
    }

    debugRender(ctx: CanvasRenderingContext2D,
                labelPoints: boolean = false): void
    {

        let levels: ISpatiallyIndexable[][] = [];
        let level: ISpatiallyIndexable[]    = [];
        let queue: ISpatiallyIndexable[]    = [this.m_root];

        while (queue.length > 0)
        {
            level = queue.slice(0);
            queue = [];

            for (let node of level)
            {
                if (node instanceof RStarTreeNode) queue.push(...(<RStarTreeNode<T>>node).children);
            }

            levels.push(level);
        }

        // Iterate bottom to top and render
        let baseHue = 0;
        let hueStep = 105;

        for (let i = 0; i < levels.length; i++)
        {
            let level = (levels.length - 1) - i;
            let color = `hsl(${(baseHue + hueStep * level) % 360} , 95%, 65%)`;

            for (let node of levels[level])
            {
                ctx.globalAlpha = 1;

                if (node instanceof RStarTreeNode)
                {
                    ctx.lineWidth   = 1;
                    ctx.strokeStyle = color;
                    ctx.strokeRect(node.left, node.top, node.width, node.height);
                }
                else
                {
                    let c         = node.center();
                    ctx.fillStyle = color;
                    ctx.fillRect(c.x - 0.5, c.y - 0.5, 1, 1);

                    if (labelPoints)
                    {
                        ctx.globalAlpha  = 0.5;
                        ctx.fillStyle    = "#333333";
                        ctx.font         = "4px sans-serif";
                        ctx.textAlign    = "left";
                        ctx.textBaseline = "middle";
                        ctx.fillText(`[${c.x.toFixed(1)},${c.y.toFixed(1)}]`, c.x + 2, c.y);
                    }
                }
            }
        }
    }

    private insertAtLevel(entry: ISpatiallyIndexable,
                          level: number)
    {
        // Find a good node (and its path) to place the entry in
        let insertPath = this.chooseSubtree(entry, level);
        let insertNode = insertPath[0];

        insertNode.children.push(entry);
        insertNode.updateToFit(entry);

        // If needed, handle re-balancing due to overflow
        if (insertNode.children.length > this.m_maxEntries) this.rebalance(insertPath, level);
    }

    private chooseSubtree(entry: ISpatiallyIndexable,
                          maxLevel: number): RStarTreeNode<T>[]
    {
        let path = [this.m_root];
        let node = this.m_root;

        // Continue until a leaf node is found or we reach the max level
        while (!node.leaf && path.length - 1 < maxLevel)
        {
            // TODO: Right now this is normal r-tree insertion, not the overlap minimizing version of r*-tree

            let enlargement: number;
            let area: number;

            let minNode: RStarTreeNode<T> = null;
            let minArea: number           = Infinity;
            let minEnlargement: number    = Infinity;

            // Check every child node and find the one with minimum enlargement (or minimum area is enlargement is tied)
            for (let child of node.nodes)
            {
                let copy    = child.clone();
                area        = child instanceof RStarTreeNode ? child.clone()
                                                                    .updateToFit(entry)
                                                                    .area() : 0;
                enlargement = area - child.area();

                if (enlargement < minEnlargement)
                {
                    minEnlargement = enlargement;
                    minArea        = area < minArea ? area : minArea;
                    minNode        = child;
                }
                else if (enlargement === minEnlargement)
                {
                    if (area < minArea)
                    {
                        minArea = area;
                        minNode = child;
                    }
                }
            }

            // Select node for this level
            node = minNode || node.nodes[0];

            // Add the node to the path
            path.unshift(node);
        }

        // Return the selected node path
        return path;
    }

    private split(path: RStarTreeNode<T>[]): boolean
    {
        // Get the node to split
        let node: RStarTreeNode<T> = path[0];

        // Check if we are splitting the root and/or a leaf
        let splittingRoot = path.length === 1;

        // Pick the axis to split the node on
        let axis = this.chooseSplitAxis(node);

        // Pick the optimal split index along the chosen axis
        let index = this.chooseSplitIndex(node, axis);

        // Split the existing node splitting the data entries at the chosen index
        // We do not worry about sorting because it is still correctly sorted from chooseSplitIndex()
        let replacements = node.split(index);

        // Get the parent node of the current node
        let parent = splittingRoot ? new RStarTreeNode<T>(false) : path[1];

        // Remove the current node from the parent
        parent.nodes.splice(parent.nodes.indexOf(node), 1);

        // Add the replacement nodes to the parent
        parent.children.push(...replacements);
        parent.refit();

        // If we split the root, update tree further
        if (splittingRoot)
        {
            this.m_root = parent;
            this.m_leafLevel++;
        }

        // Return if the root was split and leaf level increased
        return splittingRoot;
    }

    private chooseSplitAxis(node: RStarTreeNode<T>): DataAxis
    {
        // Calculate x/y split heuristic value
        let xHeuristic = this.calculateAllSplitMargins(node, DataAxis.X);
        let yHeuristic = this.calculateAllSplitMargins(node, DataAxis.Y);

        // Pick the axis to split on
        return xHeuristic < yHeuristic ? DataAxis.X : DataAxis.Y;
    }

    private calculateAllSplitMargins(node: RStarTreeNode<T>,
                                     axis: DataAxis): number
    {
        let minSplitIndex = this.m_minEntries;
        let maxSplitIndex = this.m_maxEntries - this.m_minEntries;

        // Sort along axis
        node.sortAlongAxis(axis);

        // Calculate the margin of the bounding box of each possible split
        let totalMargin = 0;
        let left        = AxisAlignedBoundingBox.forSpatiallyIndexables(node.children.slice(0, minSplitIndex));
        let right       = AxisAlignedBoundingBox.forSpatiallyIndexables(node.children.slice(maxSplitIndex + 1, node.children.length));

        // Add up all left splits
        for (let i = minSplitIndex; i < maxSplitIndex; i++)
        {
            totalMargin += left.updateToFit(node.children[i])
                               .margin();
        }

        // Add up all right splits
        for (let i = maxSplitIndex; i > minSplitIndex; i--)
        {
            totalMargin += right.updateToFit(node.children[i])
                                .margin();
        }

        // Return the sum of margins for all viable splits
        return totalMargin;
    }

    private chooseSplitIndex(node: RStarTreeNode<T>,
                             axis: DataAxis): number
    {
        let minSplitIndex = this.m_minEntries;
        let maxSplitIndex = this.m_maxEntries - this.m_minEntries;

        // Sort along axis
        node.sortAlongAxis(axis);

        // Track the current and optimal split indices
        let optimalIndex = minSplitIndex;
        let index        = minSplitIndex;

        // Track the optimal overlap area
        let optimalOverlap = Infinity;

        // Find the optimal split by overlap area
        for (index; index < maxSplitIndex; index++)
        {
            let left    = AxisAlignedBoundingBox.forSpatiallyIndexables(node.children.slice(0, index));
            let right   = AxisAlignedBoundingBox.forSpatiallyIndexables(node.children.slice(index));
            let overlap = left.intersectionArea(right);

            if (overlap < optimalOverlap)
            {
                optimalOverlap = overlap;
                optimalIndex   = index;
            }
        }

        // Return the optimal split index
        return optimalIndex;
    }

    private rebalance(path: RStarTreeNode<T>[],
                      level: number)
    {
        // Re-balance the overflowing node depending on level and re-balancing done so far
        if (level !== 0 && this.m_rebalances[level])
        {
            // Flag as rebalanced for this level
            this.m_rebalances[level] = true;

            // If we are not at the root, reinsert the entries of the node
            this.reinsert(path[0], level);
        }
        else
        {
            // If we are at the root or already rebalanced at this level, perform a split
            let increasedLevel = this.split(path);

            if (increasedLevel) this.m_rebalances.unshift(false);

            // Rebalance until we hit the root
            let nextPath = path.slice(1);
            if (nextPath.length > 0 && nextPath[0].children.length > this.m_maxEntries) this.rebalance(nextPath, increasedLevel ? level : level - 1);
        }
    }

    private reinsert(node: RStarTreeNode<T>,
                     level: number): void
    {
        // Calculate distances to center of node
        let distances = node.children.map((entry) =>
                                          {
                                              return {
                                                  entry   : entry,
                                                  distance: entry.center()
                                                                 .distanceToVectorSquared(node.center())
                                              };
                                          });

        // Sort the entries by distance, descending, we want the farthest items first
        distances.sort(numericSortBy("distance", SortDirection.DESCENDING));
        node.children.splice(0, node.children.length, ...distances.map((pair) =>
                                                                       {
                                                                           return pair.entry;
                                                                       }));

        // Remove some of the items from the array
        let amount  = Math.ceil(node.children.length * this.REINSERT_PERCENT);
        let entries = node.children.splice(0, amount);
        node.refit();

        // Invoke insert for entries
        for (let entry of entries) this.insertAtLevel(entry, level);
    }
}

export class ChartingMath
{
    static lerp(start: number,
                end: number,
                pct: number)
    {
        return start + ((end - start) * pct);
    }
}
