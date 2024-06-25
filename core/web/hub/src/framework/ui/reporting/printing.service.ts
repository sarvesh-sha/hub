import {Injectable} from "@angular/core";

@Injectable()
export class PrintingService
{
    print()
    {
        setTimeout(() =>
                   {
                       console.debug("Triggering Print...");
                       window.print();
                   }, 100);
    }
}
