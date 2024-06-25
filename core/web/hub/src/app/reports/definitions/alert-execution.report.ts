import {DynamicReport} from "app/reports/dynamic/dynamic-report";
import {AppDomainContext} from "app/services/domain/domain.module";
import {ReportConfigurationExtended} from "app/services/domain/reporting.service";
import * as Models from "app/services/proxy/model/models";

export class AlertExecutionReport
{
    private m_alertDefinition: Models.RecordIdentity;
    private m_alertDefinitionVersion: Models.RecordIdentity;

    constructor(private m_domain: AppDomainContext)
    {
    }

    public setAlert(alertDefinition: Models.RecordIdentity,
                    alertDefinitionVersion?: Models.RecordIdentity)
    {
        this.m_alertDefinition        = alertDefinition;
        this.m_alertDefinitionVersion = alertDefinitionVersion;
    }

    public buildDynamicReport(): DynamicReport
    {
        let header         = ReportConfigurationExtended.getRichTextHeader("Alert Execution Report");
        let alertExecution = Models.ReportLayoutItem.newInstance({
                                                                     element: Models.CustomReportElementAlertExecution.newInstance({
                                                                                                                                       definition: this.m_alertDefinition,
                                                                                                                                       version   : this.m_alertDefinitionVersion
                                                                                                                                   })
                                                                 });
        let container      = Models.ReportLayoutColumn.newInstance({
                                                                       children: [
                                                                           header,
                                                                           alertExecution
                                                                       ]
                                                                   });
        let config         = Models.ReportConfiguration.newInstance({container: container});


        let report = new DynamicReport(null, null, this.m_domain);
        report.setData(new ReportConfigurationExtended(config));
        return report;
    }
}
