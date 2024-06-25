import * as Base from "app/customer/engines/shared/base";
import * as Models from "app/services/proxy/model/models";
import {BlockDef} from "framework/ui/blockly/block";

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "travel_log_coordinates",
              model       : Models.AlertEngineOperatorUnaryControlPointCoordinates,
              outputType  : Models.AlertEngineValueControlPointCoordinates
          })
export class TravelLogCoordinatesBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryControlPointCoordinates>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint);

        this.appendConnectedBlock(this.block_a)
            .appendField("get travel log of");
    }
}

@BlockDef({
              blockContext     : "AlertRules",
              blockName        : "travel_log_get_new_entries",
              model            : Models.AlertEngineOperatorUnaryCoordinatesNewSamples,
              outputType       : Models.EngineValueList,
              outputElementType: Models.AlertEngineValueTravelEntry
          })
export class TravelLogGetNewEntriesBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryCoordinatesNewSamples>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPointCoordinates);

        this.appendConnectedBlock(this.block_a)
            .appendField("get new travel entries from");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_location_from_fence",
              model       : Models.AlertEngineOperatorUnaryTravelEntryInsideFence,
              outputType  : Models.AlertEngineValueLocation
          })
export class TravelEntryInsideFenceBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryTravelEntryInsideFence>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueTravelEntry);

        this.appendConnectedBlock(this.block_a)
            .appendField("get location from fence for");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_travel_entry_time",
              model       : Models.AlertEngineOperatorUnaryTravelEntryGetTime,
              outputType  : Models.EngineValueDateTime
          })
export class TravelEntryGetTimeBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryTravelEntryGetTime>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueTravelEntry);

        this.appendConnectedBlock(this.block_a)
            .appendField("get time of travel entry");
    }
}
