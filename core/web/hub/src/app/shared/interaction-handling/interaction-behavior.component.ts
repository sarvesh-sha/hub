import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output, SimpleChanges} from "@angular/core";

import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {ControlOption} from "framework/ui/control-option";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";

@Component({
               selector       : "o3-interaction-behavior",
               templateUrl    : "./interaction-behavior.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class InteractionBehaviorComponent extends BaseApplicationComponent
{
    static ngAcceptInputType_dataExplorerOption: boolean | "";
    static ngAcceptInputType_deviceElemOption: boolean | "";
    static ngAcceptInputType_paneOption: boolean | "";
    static ngAcceptInputType_noneOption: boolean | "";

    private m_behaviorHeader: string = "How will your widget respond to clicks?";
    @Input() set behaviorHeader(header: string)
    {
        this.m_behaviorHeader = header || this.m_behaviorHeader;
    }

    get behaviorHeader(): string
    {
        return this.m_behaviorHeader;
    }

    @Input() standardBehaviorLabel: string;
    @Input() @CoerceBoolean() dataExplorerOption: boolean;
    @Input() @CoerceBoolean() deviceElemOption: boolean;
    @Input() @CoerceBoolean() paneOption: boolean;
    @Input() @CoerceBoolean() noneOption: boolean;

    paneOptions: ControlOption<string>[] = [];

    @Input() selectedPane: string;
    @Output() selectedPaneChange = new EventEmitter<string>();

    behaviorOptions: ControlOption<Models.InteractionBehaviorType>[];

    private m_behavior: Models.InteractionBehaviorType;
    @Input() set behavior(behavior: Models.InteractionBehaviorType)
    {
        this.m_behavior         = behavior;
        this.behaviorSelections = new Set([behavior]);
    }

    behaviorSelections: Set<Models.InteractionBehaviorType>;
    @Output() behaviorChange = new EventEmitter<Models.InteractionBehaviorType>();

    get paneSelected(): boolean
    {
        return this.m_behavior === Models.InteractionBehaviorType.Pane;
    }

    public async ngOnInit()
    {
        super.ngOnInit();

        this.paneOptions = await this.app.bindings.getPaneConfigurations();
        this.rebuildOptions();
    }

    public ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);
        this.rebuildOptions();
    }

    private rebuildOptions()
    {
        this.behaviorOptions = [];
        if (this.noneOption) this.behaviorOptions.push(new ControlOption(Models.InteractionBehaviorType.None, "Do nothing"));
        if (this.standardBehaviorLabel) this.behaviorOptions.push(new ControlOption(Models.InteractionBehaviorType.Standard, this.standardBehaviorLabel));
        if (this.deviceElemOption) this.behaviorOptions.push(new ControlOption(Models.InteractionBehaviorType.NavigateDeviceElem, "Navigate to control point"));
        if (this.dataExplorerOption) this.behaviorOptions.push(new ControlOption(Models.InteractionBehaviorType.NavigateDataExplorer, "View in Data Explorer"));

        // pane should stay last to avoid pushing other options down because it may switch from unavailable -> available after pane options are grabbed
        if (this.paneOption && this.paneOptions.length) this.behaviorOptions.push(new ControlOption(Models.InteractionBehaviorType.Pane, "Explore in context pane"));
        this.markForCheck();
    }

    behaviorChanged()
    {
        let mode = this.behaviorSelections.size && this.behaviorSelections.values()
                                                       .next().value;
        if (!mode) return;

        this.behavior = mode;
        this.behaviorChange.emit(mode);
    }
}
