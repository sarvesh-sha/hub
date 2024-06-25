import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

export abstract class ExperimentsBasePageComponent extends SharedSvc.BaseApplicationComponent
{
    checklistSingle: string                = "red";
    gradient: Models.ColorSegment[]        = null;
    colorConfig: Models.ColorConfiguration = null;

    stringify(object: any): string
    {
        return JSON.stringify(object);
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
