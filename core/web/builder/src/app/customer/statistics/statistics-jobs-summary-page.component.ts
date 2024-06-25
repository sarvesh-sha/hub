import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";

@Component({
               selector   : "o3-statistics-job-summary-page",
               templateUrl: "./statistics-jobs-summary-page.component.html"
           })
export class StatisticsJobsSummaryPageComponent extends SharedSvc.BaseApplicationComponent
{
    jobSummaries: JobStatistics[];

    constructor(inj: Injector)
    {
        super(inj);
    }

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        this.fetchJobSummary();
    }

    async fetchJobSummary()
    {
        let lookup = new Map<String, JobStatistics>();

        let jobs = await this.app.domain.jobs.getExtendedAll();
        for (let job of jobs)
        {
            let id = job.model.name + "/" + job.model.status;

            let jobStats = lookup.get(id);
            if (!jobStats)
            {
                jobStats        = new JobStatistics();
                jobStats.name   = job.model.name;
                jobStats.status = job.model.status;
                jobStats.count  = 0;
                lookup.set(id, jobStats);
            }

            jobStats.count++;
        }


        let results: JobStatistics[] = [];

        lookup.forEach(a => results.push(a));

        results.sort((a,
                      b) =>
                     {
                         let diff = UtilsService.compareStrings(a.name, b.name, true);
                         if (diff)
                         {
                             diff = b.count - a.count;
                         }
                         return diff;
                     });

        this.jobSummaries = results;
    }
}

class JobStatistics
{
    name: string;

    status: Models.JobStatus;

    count: number;
}

