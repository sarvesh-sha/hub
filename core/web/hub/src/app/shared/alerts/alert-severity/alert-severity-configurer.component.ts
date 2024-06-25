import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, Output, QueryList, ViewChild, ViewChildren} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {ColorPickerComponent} from "app/shared/colors/color-picker.component";

import {ControlOption} from "framework/ui/control-option";
import {CoerceBoolean} from "framework/ui/decorators/coerce-boolean.decorator";
import {SelectComponent} from "framework/ui/forms/select.component";

@Component({
               selector       : "o3-alert-severity-configurer",
               templateUrl    : "./alert-severity-configurer.component.html",
               styleUrls      : ["./alert-severity-configurer.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AlertSeverityConfigurerComponent extends BaseApplicationComponent
{
    static ngAcceptInputType_morePadding: boolean | "";

    @Input() @CoerceBoolean() morePadding: boolean;
    @Input() severityColors: Models.AlertMapSeverityColor[];
    @Input() selectedSeverities: Models.AlertSeverity[];

    @Output() selectedSeveritiesChange = new EventEmitter<Models.AlertSeverity[]>();
    @Output() severityColorsChange     = new EventEmitter<Models.AlertMapSeverityColor[]>();

    private m_allSeverities: boolean;
    get allSeverities(): boolean
    {
        if (this.m_allSeverities != null) return this.m_allSeverities;
        return !this.selectedSeverities?.length;
    }

    set allSeverities(all: boolean)
    {
        this.m_allSeverities = all;
    }

    alertSeverityOptions: ControlOption<string>[] = [];
    alertSeverityEnums: Models.EnumDescriptor[];

    @ViewChild("test_allSeverities", {read: ElementRef}) test_allSeverities: ElementRef;
    @ViewChild("test_severities") test_severities: SelectComponent<string>;
    @ViewChildren("test_color") test_colors: QueryList<ColorPickerComponent>;

    get severityMarginBottom(): number
    {
        if (!this.severityColors) return 0;
        return this.morePadding ? 15 : 5;
    }

    get valid(): boolean
    {
        if (!this.selectedSeverities) return true;
        if (this.allSeverities) return true;
        return !!this.selectedSeverities?.length;
    }

    public async ngOnInit()
    {
        super.ngOnInit();

        this.alertSeverityEnums   = await this.app.domain.alerts.describeSeverities();
        this.alertSeverityOptions = SharedSvc.BaseService.mapEnumOptions<string>(this.alertSeverityEnums);
        this.markForCheck();
    }

    severityName(severityColor: Models.AlertMapSeverityColor): string
    {
        return this.alertSeverityEnums?.find((severityEnum) => severityEnum.id === <string>severityColor.severity)?.displayName;
    }

    updateAlertSeveritySelection()
    {
        if (this.allSeverities)
        {
            this.selectedSeverities = [];
            this.selectedSeveritiesChange.emit(this.selectedSeverities);
        }
    }

    severityColorChanged()
    {
        this.severityColorsChange.emit(this.severityColors);
    }
}
