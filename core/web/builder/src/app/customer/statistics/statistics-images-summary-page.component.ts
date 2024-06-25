import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {UtilsService} from "framework/services/utils.service";
import {inParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-statistics-images-summary-page",
               templateUrl: "./statistics-images-summary-page.component.html"
           })
export class StatisticsImagesSummaryPageComponent extends SharedSvc.BaseApplicationComponent
{
    imageSummaries: ImageStatistics[];

    constructor(inj: Injector)
    {
        super(inj);
    }

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        this.fetchImageSummary();
    }

    async fetchImageSummary()
    {
        let lookup = new Map<String, ImageStatistics>();

        let images = await this.app.domain.registryImages.getExtendedAll();
        await inParallel(images,
                         async (image,
                                index) =>
                         {
                             let taggedImages = await image.getReferencingTags();
                             for (let taggedImage of taggedImages)
                             {
                                 let id = taggedImage.model.tag;

                                 let tagPos = id.lastIndexOf(":");
                                 let name   = id.substring(0, tagPos);
                                 let tag    = id.substring(tagPos + 1);

                                 let imageStats = lookup.get(name);
                                 if (!imageStats)
                                 {
                                     imageStats       = new ImageStatistics();
                                     imageStats.name  = name;
                                     imageStats.count = 0;
                                     lookup.set(name, imageStats);
                                 }

                                 imageStats.count++;
                             }
                         });

        let results: ImageStatistics[] = [];

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

        this.imageSummaries = results;
    }
}

class ImageStatistics
{
    name: string;

    count: number;
}
