import {ChangeDetectionStrategy, Component, Input} from "@angular/core";
import {BaseApplicationComponent} from "app/services/domain/base.service";

@Component({
               selector       : "o3-tfidf-configuration",
               templateUrl    : "./term-frequency-inverse-document-frequency-configuration.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class TermFrequencyInverseDocumentFrequencyConfigurationComponent extends BaseApplicationComponent
{
    @Input() public model: TermFrequencyInverseDocumentFrequencyParameters;
}

export interface TermFrequencyInverseDocumentFrequencyParameters
{
    minNgram: number;
    maxNgram: number;
    minDocFrequency: number;
}
