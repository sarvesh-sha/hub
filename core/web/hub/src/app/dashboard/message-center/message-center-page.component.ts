import {Component} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";
import {DbChangeSubscription} from "app/services/domain/base.service";

import {UserMessageAlertExtended, UserMessageDeviceExtended, UserMessageExtended, UserMessageReportExtended, UserMessageRoleManagementExtended, UserMessageWorkflowExtended} from "app/services/domain/user-messages.service";
import {UserMessage} from "app/services/proxy/model/UserMessage";
import {UserMessageFilterRequest} from "app/services/proxy/model/UserMessageFilterRequest";
import {ModifiableTableRowRemove} from "framework/ui/shared/modifiable-table.component";

@Component({
               selector   : "o3-message-center-page",
               templateUrl: "./message-center-page.component.html",
               styleUrls  : ["./message-center-page.component.scss"]
           })
export class MessageCenterPageComponent extends SharedSvc.BaseApplicationComponent
{
    messages: UserMessageExtended[]          = [];
    displayedMessages: UserMessageExtended[] = [];
    messagesToDisplay: number                = 0;
    period: number                           = 30;
    limit: number                            = 10;

    private messageSubscription: DbChangeSubscription<UserMessage>;

    alwaysAllowDelete = () => true;

    async ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.messageSubscription = this.subscribeAny(this.app.domain.userMessages, () => this.updateMessages());

        await this.updateMessages();

        this.showMore();
    }

    get remainingCount(): number
    {
        return this.messages.length - this.messagesToDisplay;
    }

    get displayedLimit(): number
    {
        if (this.remainingCount < this.limit) return this.remainingCount;

        return this.limit;
    }

    async viewMessage(ext: UserMessageExtended)
    {
        ext.navigateTo(this.app.ui.navigation);

        if (ext.model.flagRead == false)
        {
            ext.model.flagRead = true;
            let extNew         = await ext.save();

            this.updateArray(this.messages, ext, extNew);
            this.updateArray(this.displayedMessages, ext, extNew);
        }
    }

    public async removeMessage(event: ModifiableTableRowRemove<UserMessageExtended>)
    {
        this.updateArray(this.messages, event.row, null);
        this.updateArray(this.displayedMessages, event.row, null);

        await event.row.remove();
    }

    showMore()
    {
        // if we're already displaying all possible results, skip ahead
        let numMessages = this.messages.length;
        if (this.messagesToDisplay < numMessages)
        {
            // calculate the current page by the number of displayed results
            let currentPage        = Math.floor(this.messagesToDisplay / this.limit);
            // how many rows should we have after we get more?
            this.messagesToDisplay = Math.min(numMessages, (currentPage + 1) * this.limit);

            this.updateDisplayedMessages();
        }
    }

    private async updateMessages(): Promise<void>
    {
        let filters        = new UserMessageFilterRequest();
        filters.flagActive = true;

        this.messages = await this.app.domain.userMessages.getTypedExtendedAll(filters);

        await this.updateDisplayedMessages();
    }

    private async updateDisplayedMessages()
    {
        this.displayedMessages = this.messages.slice(0, this.messagesToDisplay);

        for (let message of this.displayedMessages)
        {
            await message.prepareForNavigation();

            if (message.model.flagNew)
            {
                message.model.flagNew = false;
                let messageNew        = await message.save();

                this.updateArray(this.messages, message, messageNew);
                this.updateArray(this.displayedMessages, message, messageNew);
            }
        }
    }

    toggle(panel: any)
    {
        panel.toggle();
    }

    isAlert(ext: UserMessageExtended)
    {
        return ext instanceof UserMessageAlertExtended;
    }

    isDevice(ext: UserMessageExtended)
    {
        return ext instanceof UserMessageDeviceExtended;
    }

    isRoleManagement(ext: UserMessageExtended)
    {
        return ext instanceof UserMessageRoleManagementExtended;
    }

    isReport(ext: UserMessageExtended)
    {
        return ext instanceof UserMessageReportExtended;
    }

    isWorkflow(ext: UserMessageExtended)
    {
        return ext instanceof UserMessageWorkflowExtended;
    }

    private updateArray(array: UserMessageExtended[],
                        ext: UserMessageExtended,
                        extNew: UserMessageExtended)
    {
        let index = array.indexOf(ext);
        if (index > -1)
        {
            if (extNew)
            {
                array[index] = extNew;
            }
            else
            {
                array.splice(index, 1);
            }
        }
    }
}
