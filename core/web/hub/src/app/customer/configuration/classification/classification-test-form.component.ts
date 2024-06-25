import {ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, Output} from "@angular/core";
import {ClassificationDetailPageComponent} from "app/customer/configuration/classification/classification-detail-page.component";
import {ControlPointsSelectionExtended} from "app/services/domain/report-definitions.service";
import * as Models from "app/services/proxy/model/models";
import {BaseComponent} from "framework/ui/components";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector       : "o3-classification-test-form",
               templateUrl    : "./classification-test-form.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ClassificationTestFormComponent extends BaseComponent
{
    @Input() sample: Models.DeviceElementNormalizationSample;

    @Output() sampleChange = new EventEmitter<Models.DeviceElementNormalizationSample>();

    objectTypes: ControlOption<string>[];

    sysId: string;

    selection: ControlPointsSelectionExtended;

    get backupStructure(): string
    {
        return this.sample.details.objectBackupStructure?.join(" | ");
    }

    set backupStructure(text: string)
    {
        this.sample.details.objectBackupStructure = text.split("|")
                                                        .map((s) => s.trim())
                                                        .filter((s) => !!s);
    }

    get ipAddress(): string
    {
        const address = this.ensureTransportAddress();
        return address.host;
    }

    set ipAddress(ip: string)
    {
        const address = this.ensureTransportAddress();
        address.host  = ip;
    }

    get port(): number
    {
        const address = this.ensureTransportAddress();
        return address.port;
    }

    set port(port: number)
    {
        const address = this.ensureTransportAddress();
        address.port  = port;
    }

    private ensureTransportAddress(): Models.UdpTransportAddress
    {
        if (!this.sample.details.controllerTransportAddress || !(this.sample.details.controllerTransportAddress instanceof Models.UdpTransportAddress))
        {
            this.sample.details.controllerTransportAddress = Models.UdpTransportAddress.newInstance({
                                                                                                        host: "0.0.0.0",
                                                                                                        port: 47808
                                                                                                    });
        }

        return <Models.UdpTransportAddress>this.sample.details.controllerTransportAddress;
    }

    constructor(private m_host: ClassificationDetailPageComponent,
                inj: Injector)
    {
        super(inj);
        this.selection   = new ControlPointsSelectionExtended(this.m_host.app.domain);
        this.objectTypes = this.m_host.app.bindings.getBacnetObjectTypes();
    }

    updateIdentifier()
    {
        let objectType = this.sample.details.objectType;
        if (this.sample.details.objectIdentifier)
        {
            let parts = this.sample.details.objectIdentifier.split("/");
            if (parts.length === 2)
            {
                this.sample.details.objectIdentifier = `${objectType}/${parts[1]}`;
            }
        }
        else
        {
            this.sample.details.objectIdentifier = objectType + "/";
        }
    }

    async loadSample()
    {
        if (!this.sysId)
        {
            this.sysId = this.selection.identities[0]?.sysId;
        }

        this.sample = await this.m_host.app.domain.normalization.loadSample(this.sysId);
        this.sampleChange.emit(this.sample);
        this.clearSelection();
        this.markForCheck();
    }

    clearSelection()
    {
        this.sysId = null;
        this.selection.setIdentities([]);
    }
}
