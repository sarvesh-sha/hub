import {Directive, Input, TemplateRef, ViewContainerRef} from "@angular/core";

import {SettingsService} from "app/services/domain/settings.service";

@Directive({
               selector: "[o3IfSystemPreference]"
           })
export class IfSystemPreferenceDirective
{
    private m_preference: string;

    constructor(private templateRef: TemplateRef<any>,
                private viewContainer: ViewContainerRef,
                private settings: SettingsService)
    {
    }

    @Input()
    public set o3IfSystemPreference(preference: string)
    {
        if (this.m_preference != preference)
        {
            this.m_preference = preference;
            this.fetchPreference();
        }
    }

    private async fetchPreference()
    {
        let hasPreference = !!await this.settings.getPreference(null, this.m_preference);
        if (hasPreference)
        {
            this.viewContainer.createEmbeddedView(this.templateRef);
        }
        else
        {
            this.viewContainer.clear();
        }
    }
}
