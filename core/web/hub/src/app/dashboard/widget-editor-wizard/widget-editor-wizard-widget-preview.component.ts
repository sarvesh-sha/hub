import {BreakpointObserver} from "@angular/cdk/layout";
import {Component, ElementRef, Injector, Input, ViewChild} from "@angular/core";
import {UUID} from "angular2-uuid";

import {WidgetLayoutConfig, WidgetManipulator} from "app/dashboard/dashboard/widgets/widget-manipulator";
import * as Models from "app/services/proxy/model/models";

import {BaseComponent} from "framework/ui/components";
import {SyncDebouncer} from "framework/utils/debouncers";

import {Subscription} from "rxjs";

@Component({
               selector   : "o3-widget-editor-wizard-widget-preview[config]",
               templateUrl: "./widget-editor-wizard-widget-preview.component.html",
               styleUrls  : [
                   "./widget-editor-wizard-dialog.component.scss",
                   "./widget-editor-wizard-widget-preview.component.scss"
               ]
           })
export class WidgetEditorWizardWidgetPreviewComponent extends BaseComponent
{
    private static readonly ALWAYS_VERTICAL_THRESHOLD: number = 400;
    private static readonly NUM_ROWS: number                  = 1;

    private m_verticalViewThreshold = 800;
    @Input() set verticalViewThreshold(threshold: number)
    {
        if (threshold !== this.m_verticalViewThreshold)
        {
            this.m_verticalViewThreshold = threshold;
            this.rebuildObserver();
        }
    }

    private m_widgetContainer: Models.WidgetComposition[] = [];
    get widgetContainer(): Models.WidgetComposition[]
    {
        return this.m_widgetContainer;
    }

    private m_config: Models.WidgetConfiguration;
    @Input() set config(config: Models.WidgetConfiguration)
    {
        if (config && this.m_config !== config)
        {
            config.id     = UUID.UUID();
            this.m_config = config;
            this.rebuildWidgetContainer();
        }
    }

    private m_manualFontScalar: number = 1;
    @Input() set manualFontScalar(scalar: number)
    {
        if (scalar > 0 && this.m_manualFontScalar !== scalar)
        {
            this.m_manualFontScalar = scalar;
            this.rebuildWidgetManager();
        }
    }

    private m_height: number = 150;
    @Input() set height(height: number)
    {
        if (height > 0 && this.m_height !== height)
        {
            this.m_height = height;
            this.rebuildWidgetManager();
        }
    }

    private m_targetWidth: number;
    @Input() set targetWidth(width: number)
    {
        if (width > 0 && width !== this.m_targetWidth)
        {
            this.m_targetWidth = width;
            this.m_updateColsDebouncer.invoke();
        }
    }

    private m_colWidth: number;
    @Input() set colWidth(colWidth: number)
    {
        if (colWidth > 0 && this.m_colWidth !== colWidth)
        {
            this.m_colWidth = colWidth;
            this.m_updateColsDebouncer.invoke();
        }
    }

    private m_managerWidth: number;
    get managerWidth(): number
    {
        return this.m_managerWidth;
    }

    private m_numCols: number;
    private get numCols(): number
    {
        return this.m_numCols || 1;
    }

    private readonly m_updateColsDebouncer = new SyncDebouncer(0, () => this.updateNumCols());

    widgetManipulator: WidgetManipulator;

    private m_narrowSub: Subscription;
    private m_narrowView: boolean;
    get narrowView(): boolean
    {
        return this.m_narrowView;
    }

    constructor(inj: Injector,
                private observer: BreakpointObserver)
    {
        super(inj);

        this.rebuildWidgetManipulator();
        this.rebuildObserver();
    }

    private rebuildObserver()
    {
        if (this.m_narrowSub)
        {
            this.m_narrowSub.unsubscribe();
            this.m_narrowSub = null;
        }

        if (this.m_verticalViewThreshold > WidgetEditorWizardWidgetPreviewComponent.ALWAYS_VERTICAL_THRESHOLD)
        {
            const query       = `(max-width: ${this.m_verticalViewThreshold}px)`;
            this.m_narrowView = this.observer.isMatched(query);
            this.m_narrowSub  = this.subscribeToObservable(this.observer.observe(query), (breakpoint) => this.m_narrowView = breakpoint.matches);
        }
        else
        {
            this.m_narrowView = true;
        }
    }

    private rebuildWidgetContainer()
    {
        if (this.m_config)
        {
            this.m_widgetContainer = [
                Models.WidgetComposition.newInstance({
                                                         config : this.m_config,
                                                         outline: Models.WidgetOutline.newInstance({
                                                                                                       top   : 0,
                                                                                                       left  : 0,
                                                                                                       height: WidgetEditorWizardWidgetPreviewComponent.NUM_ROWS,
                                                                                                       width : this.numCols
                                                                                                   })
                                                     })
            ];
        }
    }

    private updateNumCols()
    {
        const prevNumCols = this.numCols;

        if (this.m_colWidth)
        {
            let targetWidth     = this.m_targetWidth > 0 ? this.m_targetWidth : 450;
            this.m_managerWidth = Math.floor(targetWidth / this.m_colWidth) * this.m_colWidth;
            this.m_numCols      = this.m_managerWidth / this.m_colWidth;
        }
        else
        {
            this.m_managerWidth = this.m_numCols = undefined;
        }

        if (prevNumCols !== this.numCols)
        {
            this.rebuildWidgetContainer();
            this.rebuildWidgetManager();
        }
    }

    private rebuildWidgetManipulator()
    {
        let layoutConfig       = new WidgetLayoutConfig(this.numCols, WidgetEditorWizardWidgetPreviewComponent.NUM_ROWS, this.m_height, this.m_manualFontScalar);
        this.widgetManipulator = new WidgetManipulator(this.injector, layoutConfig, null, null, null);
    }

    private rebuildWidgetManager()
    {
        if (this.widgetManipulator)
        {
            // trigger rebuild of WidgetManager: otherwise, the inner trackbyFn doesn't get updated and preview won't respect config changes
            this.widgetManipulator = null;
            this.detectChanges();
        }

        this.rebuildWidgetManipulator();
        this.detectChanges();
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();

        this.refresh();
    }

    public refresh(): Promise<boolean>
    {
        this.updateNumCols();
        return this.widgetManipulator.refresh();
    }
}
