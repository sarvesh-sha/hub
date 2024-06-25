import {CdkDragEnd, CdkDragMove, Point} from "@angular/cdk/drag-drop";

import {ReactiveManipulation} from "app/dashboard/dashboard/widgets/reactive-widget-manipulation";
import {WidgetContainerComponent} from "app/dashboard/dashboard/widgets/widget-container.component";
import {ResizePoint, WidgetManipulator, WidgetOutlineExtended} from "app/dashboard/dashboard/widgets/widget-manipulator";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {Direction} from "framework/ui/charting/app-charting-utilities";
import {Vector2} from "framework/ui/charting/charting-math";

export interface ManipulationNode
{
    widgetId: string;

    projectedOutline: Models.WidgetOutline;
}

export class WidgetManipulation
{
    public readonly forDrag: boolean = true;

    private m_reactives: ReactiveManipulation[];

    public get resizeInfo(): string
    {
        return this.m_resizePoint;
    }

    private m_participants = new Set<string>();
    private m_manipulator: WidgetManipulator;
    private m_placeholderManipulator: WidgetManipulator;

    get activeManipulator(): WidgetManipulator
    {
        return this.m_placeholderManipulator || this.m_manipulator;
    }

    private m_previewCenter: Point;
    get previewCenter(): Point
    {
        return this.m_previewCenter;
    }

    set previewCenter(previewCenter: Point)
    {
        this.m_previewCenter = previewCenter;
        this.m_manipulator.updateEnterable();
    }

    get baseAspectRatio(): number
    {
        return this.m_widgetPreviewDiagonal.y / this.m_widgetPreviewDiagonal.x;
    }

    get widgetId(): string
    {
        return this.m_widgetContainer.widget.id;
    }

    constructor(public readonly originManipulator: WidgetManipulator,
                private m_widgetContainer: WidgetContainerComponent,
                private m_resizePoint: ResizePoint,
                private readonly m_widgetCursorOffset: Vector2,
                private readonly m_widgetPreviewDiagonal: Vector2)
    {
        this.forDrag = !m_resizePoint;

        this.m_manipulator = this.originManipulator;
        this.m_participants.add(this.m_manipulator.id);

        this.m_manipulator.widgetOutlineExtLookup[this.widgetId].updateOverride();
    }

    public processState(manipulationEvent: CdkDragMove<string>): void
    {
        if (this.forDrag) this.updatePreviewCenter(manipulationEvent);

        this.m_manipulator.ensureBaseState(this.m_widgetContainer.outline, manipulationEvent);
        const manipulationVector = this.m_manipulator.getManipulationVector(manipulationEvent);

        let outlineExt       = this.m_manipulator.widgetOutlineExtLookup[this.widgetId];
        let targetOutline    = WidgetOutlineExtended.getManipulationOutline(this.m_manipulator.manipulationBaseState.baseOutline, manipulationVector, this.m_resizePoint);
        let manipulationFits = WidgetOutlineExtended.isValid(targetOutline,
                                                             this.m_manipulator.manipulationMinOutline,
                                                             this.m_manipulator.widgetGridConfig.numRows,
                                                             this.m_manipulator.widgetGridConfig.numCols);
        if (manipulationFits && !UtilsService.compareJson(targetOutline, outlineExt?.effectiveModel))
        {
            if (this.basicManipulationFits(this.widgetId, targetOutline))
            {
                this.revertManipulations();
                this.executeBasicManipulation(this.widgetId, this.m_manipulator, targetOutline, manipulationVector);
            }
            else if (!this.m_manipulator.columnar)
            {
                if (this.forDrag)
                {
                    // revert to fresh manipulation state to check reactiveDrag viability
                    const clearedReactives = this.revertManipulations();
                    manipulationFits       = this.reactiveDrag(targetOutline, manipulationVector);

                    if (!manipulationFits && clearedReactives)
                    {
                        // reapply previous manipulation state
                        this.m_reactives = clearedReactives;
                        for (let reactive of this.m_reactives) this.executeReactiveManipulation(reactive, this.activeManipulator);
                    }
                }
                else
                {
                    manipulationFits = this.reactiveResize(targetOutline);
                }
            }

            if (manipulationFits) this.m_placeholderManipulator = null;
        }
    }

    public transferManipulator(transferManipulator: WidgetManipulator)
    {
        if (!this.m_placeholderManipulator)
        {
            // a valid position for the drag was found in m_manipulator: set to m_placeholderManipulator
            this.m_placeholderManipulator = this.m_manipulator;
        }

        this.m_manipulator = transferManipulator;
        this.m_participants.add(transferManipulator.id);
    }

    public manipulatorParticipated(manipulator: WidgetManipulator): boolean
    {
        return this.m_participants.has(manipulator.id);
    }

    private updatePreviewCenter(manipulationEvent: CdkDragMove)
    {
        const cursor       = manipulationEvent.pointerPosition;
        this.previewCenter = {
            x: cursor.x - this.m_widgetCursorOffset.x + this.m_widgetPreviewDiagonal.x / 2,
            y: cursor.y - this.m_widgetCursorOffset.y + this.m_widgetPreviewDiagonal.y / 2
        };
    }

    private reactiveResize(targetOutline: Models.WidgetOutline): boolean
    {
        let manipulations = this.getReactiveResize(targetOutline);
        if (manipulations.length > 0)
        {
            for (let manipulation of manipulations) this.executeReactiveManipulation(manipulation, this.m_manipulator);
            this.updateReactiveResizes(manipulations);
            return true;
        }

        return false;
    }

    private reactiveDrag(targetOutline: Models.WidgetOutline,
                         manipulationVector: Vector2): boolean
    {
        let collisions = this.getCollisionsFromTopToBottom(targetOutline);
        if (collisions.length > 0)
        {
            if (this.m_manipulator.widgetOutlineExtLookup[collisions[0]].top < targetOutline.top) return false;

            const targetBottom = targetOutline.top + targetOutline.height;
            const widgetsMoved = new Set<string>();
            this.m_reactives   = [];
            for (let collisionId of collisions)
            {
                if (widgetsMoved.has(collisionId)) continue;

                const pressure             = targetBottom - this.m_manipulator.widgetOutlineExtLookup[collisionId].top;
                const reactiveManipulation = ReactiveManipulation.generate(this.m_manipulator, Direction.Down, pressure, false, this.m_reactives, collisionId);
                if (!reactiveManipulation.valid) return false;
                this.m_reactives.push(reactiveManipulation);

                for (let widgetId of reactiveManipulation.nodeLookup.keys()) widgetsMoved.add(widgetId);
            }

            for (let manipulation of this.m_reactives) this.executeReactiveManipulation(manipulation, this.m_manipulator);
        }

        this.executeBasicManipulation(this.widgetId, this.m_manipulator, targetOutline, manipulationVector);
        return true;
    }

    private getCollisionsFromTopToBottom(targetOutline: Models.WidgetOutline): string[]
    {
        let collisions = new Set<string>();
        for (let row = targetOutline.top; row < targetOutline.top + targetOutline.height; row++)
        {
            for (let col = targetOutline.left; col < targetOutline.left + targetOutline.width; col++)
            {
                let widgetId = this.m_manipulator.gridManipulator.getEntry(row, col);
                if (widgetId && widgetId !== this.widgetId) collisions.add(widgetId);
            }
        }

        return [...collisions].sort((idA,
                                     idB) => this.m_manipulator.widgetOutlineExtLookup[idA].top - this.m_manipulator.widgetOutlineExtLookup[idB].top);
    }

    private updateReactiveResizes(newManipulations: ReactiveManipulation[])
    {
        let prevReactives = this.m_reactives;
        this.m_reactives  = newManipulations;
        if (!prevReactives || prevReactives.length === 0) return;

        let widgetsToReset: Set<string> = new Set();
        for (let prevReactive of prevReactives)
        {
            for (let widgetId of prevReactive?.nodeLookup.keys() || [])
            {
                if (this.m_reactives.every((newReactive) => !newReactive?.nodeLookup.has(widgetId))) widgetsToReset.add(widgetId);
            }
        }

        for (let widgetId of widgetsToReset) this.executeBasicManipulation(widgetId, this.activeManipulator);
    }

    private getReactiveResize(targetOutline: Models.WidgetOutline): ReactiveManipulation[]
    {
        let baseOutline = this.m_manipulator.widgetOutlineExtLookup[this.widgetId].model;

        let horizontal: ReactiveManipulation = null;
        if (this.m_resizePoint.indexOf("left") >= 0)
        {
            let pressure = baseOutline.left - targetOutline.left;
            horizontal   = ReactiveManipulation.generate(this.m_manipulator, Direction.Left, pressure, true);
        }
        else if (this.m_resizePoint.indexOf("right") >= 0)
        {
            let pressure = targetOutline.left + targetOutline.width - (baseOutline.left + baseOutline.width);
            horizontal   = ReactiveManipulation.generate(this.m_manipulator, Direction.Right, pressure, true);
        }
        if (horizontal && !horizontal.valid) return [];

        let vertical: ReactiveManipulation = null;
        if (this.m_resizePoint.indexOf("top") >= 0)
        {
            let pressure = baseOutline.top - targetOutline.top;
            vertical     = ReactiveManipulation.generate(this.m_manipulator, Direction.Up, pressure, true, horizontal && [horizontal] || undefined);
        }
        else if (this.m_resizePoint.indexOf("bot") >= 0)
        {
            let pressure = targetOutline.top + targetOutline.height - (baseOutline.top + baseOutline.height);
            vertical     = ReactiveManipulation.generate(this.m_manipulator, Direction.Down, pressure, true, horizontal && [horizontal] || undefined);
        }
        if (vertical && !vertical.valid) return [];

        return [
            horizontal,
            vertical
        ];
    }

    private executeReactiveManipulation(manipulation: ReactiveManipulation,
                                        manipulator: WidgetManipulator)
    {
        if (!manipulation) return;

        let root          = manipulation.reactiveManipulationDAG;
        let outline       = manipulator.widgetOutlineExtLookup[root.widgetId];
        let targetOutline = outline.modelCopy(true);
        manipulation.executeManipulation(targetOutline, root);

        let shifts = new Vector2(0, 0);
        if (manipulation.forResize)
        {
            switch (manipulation.direction)
            {
                case Direction.Up:
                    shifts.y = targetOutline.top - outline.top;
                    break;

                case Direction.Down:
                    shifts.y = targetOutline.height - outline.height;
                    break;

                case Direction.Left:
                    shifts.x = targetOutline.left - outline.left;
                    break;

                case Direction.Right:
                    shifts.x = targetOutline.width - outline.width;
                    break;
            }
        }
        else
        {
            switch (manipulation.direction)
            {
                case Direction.Up:
                case Direction.Down:
                    shifts.y = targetOutline.top - outline.top;
                    break;

                case Direction.Left:
                case Direction.Right:
                    shifts.x = targetOutline.left - outline.left;
                    break;
            }
        }

        this.executeBasicManipulation(root.widgetId, manipulator, targetOutline, shifts);

        for (let widgetId of manipulation.nodeLookup.keys())
        {
            if (widgetId === root.widgetId) continue;

            let targetOutline = manipulator.widgetOutlineExtLookup[widgetId].modelCopy(true);
            let node          = manipulation.nodeLookup.get(widgetId);
            manipulation.executeManipulation(targetOutline, node);
            this.executeBasicManipulation(node.widgetId, manipulator, targetOutline);
        }
    }

    private revertManipulations(): ReactiveManipulation[]
    {
        let reactives    = this.m_reactives;
        this.m_reactives = null;

        if (reactives)
        {
            let widgetsToReset = new Set<string>();
            for (let reactive of reactives)
            {
                if (!reactive) continue;

                for (let widgetId of reactive.nodeLookup.keys()) widgetsToReset.add(widgetId);
            }

            for (let widgetId of widgetsToReset) this.executeBasicManipulation(widgetId, this.activeManipulator);
        }

        return reactives;
    }

    public completeManipulation(dragEnd: CdkDragEnd): boolean
    {
        const manipulator = this.activeManipulator;
        if (this.forDrag && dragEnd)
        {
            if (this.originManipulator === manipulator) dragEnd.source.reset();
            this.updateBasePreviewPosition(0, 0);
        }

        let existsModifications = false;
        if (this.m_reactives)
        {
            for (let reactive of this.m_reactives)
            {
                for (let widgetId of reactive?.nodeLookup.keys() || [])
                {
                    existsModifications = manipulator.updateGrid(widgetId) || existsModifications;
                }
            }

            if (this.forDrag) existsModifications = manipulator.updateGrid(this.widgetId) || existsModifications;
        }
        else if (manipulator.updateGrid(this.widgetId))
        {
            if (!this.forDrag)
            {
                const widgetContainer = manipulator.getWidgetContainer(this.widgetId);
                widgetContainer?.refreshWidget();
            }

            existsModifications = true;
        }

        return existsModifications;
    }

    private basicManipulationFits(manipulatedId: string,
                                  potentialOutline: Models.WidgetOutline): boolean
    {
        if (!potentialOutline) return false;

        if (!this.m_manipulator.columnar)
        {
            let lastRow = potentialOutline.top + potentialOutline.height;
            let lastCol = potentialOutline.left + potentialOutline.width;

            if (potentialOutline.top < 0 || potentialOutline.left < 0 || lastCol > this.m_manipulator.numCols) return false;

            let row = potentialOutline.top;
            while (row < lastRow && row < this.m_manipulator.numRows)
            {
                for (let col = potentialOutline.left; col < lastCol; col++)
                {
                    let widgetId = this.m_manipulator.gridManipulator.getEntry(row, col);
                    if (widgetId && widgetId !== manipulatedId) return false;
                }

                row++;
            }

            if (row < lastRow) this.m_manipulator.updateLayoutHeight(lastRow);
        }

        return true;
    }

    private executeBasicManipulation(widgetId: string,
                                     widgetManipulator: WidgetManipulator,
                                     targetOutline?: Models.WidgetOutline,
                                     manipulationVector?: Vector2): void
    {
        let widgetOutlineExt = widgetManipulator.widgetOutlineExtLookup[widgetId];
        if (!widgetOutlineExt?.isPresent) widgetOutlineExt = widgetManipulator.transferWidget();

        widgetOutlineExt.updateOverride(targetOutline);
        this.applyManipulationStyles(widgetId, widgetManipulator);

        if (manipulationVector && this.widgetId === widgetId && this.forDrag)
        {
            let previewAdjustmentVector = manipulationVector.clone();
            previewAdjustmentVector.scale(-1);

            this.updateBasePreviewPosition(previewAdjustmentVector.x * widgetManipulator.colWidth, previewAdjustmentVector.y * widgetManipulator.rowHeight);
        }
    }

    private applyManipulationStyles(widgetId: string,
                                    manipulator: WidgetManipulator): void
    {
        const widgetContainer = manipulator.getWidgetContainer(widgetId);
        if (widgetContainer)
        {
            let widgetWrapper    = widgetContainer.element.nativeElement.parentElement;
            let widgetOutlineExt = manipulator.widgetOutlineExtLookup[widgetId];

            if (!this.forDrag)
            {
                widgetWrapper.style.height = widgetOutlineExt.heightPx + "px";
                widgetWrapper.style.width  = widgetOutlineExt.widthCssPercent + "%";
            }

            if (!manipulator.columnar)
            {
                widgetWrapper.style.left = widgetOutlineExt.leftCssPercent + "%";
                widgetWrapper.style.top  = widgetOutlineExt.topCssPercent + "%";
            }
        }
    }

    private updateBasePreviewPosition(left: number,
                                      top: number): void
    {
        let style  = this.m_widgetContainer.element.nativeElement.style;
        style.left = (left || 0) + "px";
        style.top  = (top || 0) + "px";
    }
}
