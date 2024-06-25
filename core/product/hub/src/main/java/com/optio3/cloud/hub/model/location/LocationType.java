/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.location;

import com.optio3.cloud.model.IEnumDescription;

public enum LocationType implements IEnumDescription
{
    // @formatter:off
    ADMITTING             ("Admitting"             , null, "dtmi:digitaltwins:rec_3_3:core:Room;15"                     ),
    APARTMENT             ("Apartment"             , null, "dtmi:digitaltwins:rec_3_3:core:Apartment;1"                 ),
    ATRIUM                ("Atrium"                , null, "dtmi:digitaltwins:rec_3_3:building:Atrium;1"                ),
    AUDITORIUM            ("Auditorium"            , null, "dtmi:digitaltwins:rec_3_3:building:Auditorium;1"            ),
    BACKOFFICE            ("BackOffice"            , null, "dtmi:digitaltwins:rec_3_3:building:BackOffice;1"            ),
    BALCONY               ("Balcony"               , null, "dtmi:digitaltwins:rec_3_3:building:Balcony;1"               ),
    BAR_ROOM              ("Bar Room"              , null, "dtmi:digitaltwins:rec_3_3:building:BarRoom;1"               ),
    BATHROOM              ("Bathroom"              , null, "dtmi:digitaltwins:rec_3_3:building:Bathroom;1"              ),
    BEDROOM               ("Bedroom"               , null, "dtmi:digitaltwins:rec_3_3:building:Bedroom;1"               ),
    BREAK_ROOM            ("Break Room"            , null, "dtmi:digitaltwins:rec_3_3:core:Room;25"                     ),
    BUILDING              ("Building"              , null, "dtmi:digitaltwins:rec_3_3:core:Building;1"                  ),
    CABLE_ROOM            ("Cable Room"            , null, "dtmi:digitaltwins:rec_3_3:building:CableRoom;1"             ),
    CAFETERIA_ROOM        ("Cafeteria Room"        , null, "dtmi:digitaltwins:rec_3_3:building:CafeteriaRoom;1"         ),
    CAMPUS                ("Campus"                , null, "dtmi:digitaltwins:rec_3_3:core:Campus;1"                    ),
    CINEMA                ("Cinema"                , null, "dtmi:digitaltwins:rec_3_3:building:Cinema;1"                ),
    CLASSROOM             ("Classroom"             , null, "dtmi:digitaltwins:rec_3_3:building:Classroom;1"             ),
    CLEANING_ROOM         ("Cleaning Room"         , null, "dtmi:digitaltwins:rec_3_3:building:CleaningRoom;1"          ),
    CLIMATE_CONTROL_ROOM  ("Climate Control Room"  , null, "dtmi:digitaltwins:rec_3_3:building:ClimateControlRoom;1"    ),
    CLOAK_ROOM            ("Cloak Room"            , null, "dtmi:digitaltwins:rec_3_3:building:CloakRoom;1"             ),
    CONFERENCE_ROOM       ("Conference Room"       , null, "dtmi:digitaltwins:rec_3_3:building:ConferenceRoom;1"        ),
    CONVERSATION_ROOM     ("Conversation Room"     , null, "dtmi:digitaltwins:rec_3_3:building:ConversationRoom;1"      ),
    COOKING_ROOM          ("Cooking Room"          , null, "dtmi:digitaltwins:rec_3_3:building:CookingRoom;1"           ),
    COPYING_ROOM          ("Copying Room"          , null, "dtmi:digitaltwins:rec_3_3:building:CopyingRoom;1"           ),
    COPY_ROOM             ("Copy Room"             , null, "dtmi:digitaltwins:rec_3_3:building:CopyingRoom;1"           ),
    DATAS_ERVER_ROOM      ("Data Server Room"      , null, "dtmi:digitaltwins:rec_3_3:building:DataServerRoom;1"        ),
    DELIVERY_ROOM         ("Delivery Room"         , null, "dtmi:digitaltwins:rec_3_3:core:Room;12"                     ),
    DINING_ROOM           ("Dining Room"           , null, "dtmi:digitaltwins:rec_3_3:building:DiningRoom;1"            ),
    DISTRIBUTION_CENTER   ("Distribution Center"   , null, "dtmi:digitaltwins:rec_3_3:core:Room;30"                     ),
    DRESSING_ROOM         ("Dressing Room"         , null, "dtmi:digitaltwins:rec_3_3:building:DressingRoom;1"          ),
    EDUCATIONAL_ROOM      ("Educational Room"      , null, "dtmi:digitaltwins:rec_3_3:building:EducationalRoom;1"       ),
    ELECTRICAL_ROOM       ("Electrical Room"       , null, "dtmi:digitaltwins:rec_3_3:building:ElectricityRoom;1"       ),
    ELEVATOR              ("Elevator"              , null, "dtmi:digitaltwins:rec_3_3:core:Room;21"                     ),
    ELEVATOR_ROOM         ("Elevator Room"         , null, "dtmi:digitaltwins:rec_3_3:building:ElevatorRoom;1"          ),
    ELEVATOR_SHAFT        ("Elevator Shaft"        , null, "dtmi:digitaltwins:rec_3_3:building:ElevatorShaft;1"         ),
    ENTRANCE              ("Entrance"              , null, "dtmi:digitaltwins:rec_3_3:building:Entrance;1"              ),
    EXERCISE_ROOM         ("Exercise_Room"         , null, "dtmi:digitaltwins:rec_3_3:building:ExerciseRoom;1"          ),
    EXHIBITION_ROOM       ("Exhibition_Room"       , null, "dtmi:digitaltwins:rec_3_3:building:ExhibitionRoom;1"        ),
    FACADE                ("Facade"                , null, "dtmi:digitaltwins:rec_3_3:building:Facade;1"                ),
    FACTORY               ("Factory"               , null, "dtmi:digitaltwins:rec_3_3:core:Room;28"                     ),
    FITTING_ROOM          ("Fitting Room"          , null, "dtmi:digitaltwins:rec_3_3:building:FittingRoom;1"           ),
    FLOOR                 ("Floor"                 , null, "dtmi:digitaltwins:rec_3_3:building:Floor;1"                 ),
    FOOD_HANDLING_ROOM    ("Food Handling Room"    , null, "dtmi:digitaltwins:rec_3_3:building:FoodHandlingRoom;1"      ),
    FRONT_DESK            ("Front Desk"            , null, "dtmi:digitaltwins:rec_3_3:core:Room;19"                     ),
    GARAGE                ("Garage"                , null, "dtmi:digitaltwins:rec_3_3:building:Garage;1"                ),
    GROUP_ROOM            ("Group Room"            , null, "dtmi:digitaltwins:rec_3_3:building:GroupRoom;1"             ),
    HALLWAY               ("Hallway"               , null, "dtmi:digitaltwins:rec_3_3:building:Hallway;1"               ),
    HOME                  ("Home"                  , null, null                                                         ),
    HOSPITAL              ("Hospital"              , null, "dtmi:digitaltwins:rec_3_3:building:Hospital;1"              ),
    ICU                   ("ICU"                   , null, "dtmi:digitaltwins:rec_3_3:core:Room;2"                      ),
    INPATIENT_SERVICES    ("Inpatient Services"    , null, "dtmi:digitaltwins:rec_3_3:core:Room;13"                     ),
    KITCHEN               ("Kitchen"               , null, "dtmi:digitaltwins:rec_3_3:core:Room;16"                     ),
    LABORATORY            ("Laboratory"            , null, "dtmi:digitaltwins:rec_3_3:building:Laboratory;1"            ),
    LAB_SERVICES          ("Lab Services"          , null, "dtmi:digitaltwins:rec_3_3:core:Room;9"                      ),
    LAND                  ("Land"                  , null, "dtmi:digitaltwins:rec_3_3:core:Land;1"                      ),
    LAUNDRY_ROOM          ("Laundry Room"          , null, "dtmi:digitaltwins:rec_3_3:building:LaundryRoom;1"           ),
    LEVEL                 ("Level"                 , null, "dtmi:digitaltwins:rec_3_3:core:Level;1"                     ),
    LIBRARY               ("Library"               , null, "dtmi:digitaltwins:rec_3_3:building:Library;1"               ),
    LIVING_ROOM           ("Living Room"           , null, "dtmi:digitaltwins:rec_3_3:building:LivingRoom;1"            ),
    LOADING_RECEIVING_ROOM("Loading Receiving Room", null, "dtmi:digitaltwins:rec_3_3:building:LoadingReceivingRoom;1"  ),
    LOBBY                 ("Lobby"                 , null, "dtmi:digitaltwins:rec_3_3:core:Room;20"                     ),
    LOCKER_ROOM           ("Locker Room"           , null, "dtmi:digitaltwins:rec_3_3:core:Room;23"                     ),
    LOUNGE                ("Lounge"                , null, "dtmi:digitaltwins:rec_3_3:core:Room;22"                     ),
    MEDITATION_ROOM       ("Meditation Room"       , null, "dtmi:digitaltwins:rec_3_3:building:MeditationRoom;1"        ),
    MORGUE                ("Morgue"                , null, "dtmi:digitaltwins:rec_3_3:core:Room;31"                     ),
    MOTHERS_ROOM          ("Mothers Room"          , null, "dtmi:digitaltwins:rec_3_3:building:MothersRoom;1"           ),
    MULTI_PURPOSE_ROOM    ("Multi Purpose Room"    , null, "dtmi:digitaltwins:rec_3_3:building:MultiPurposeRoom;1"      ),
    NURSERY               ("Nursery"               , null, "dtmi:digitaltwins:rec_3_3:core:Room;5"                      ),
    NURSING_FACILITY      ("Nursing Facility"      , null, "dtmi:digitaltwins:rec_3_3:core:Room;7"                      ),
    OFFICE                ("Office"                , null, "dtmi:digitaltwins:rec_3_3:building:Office;1"                ),
    OFFICE_ROOM           ("Office Room"           , null, "dtmi:digitaltwins:rec_3_3:building:Office;1"                ),
    OPERATING_ROOM        ("Operating_Room"        , null, "dtmi:digitaltwins:rec_3_3:core:Room;1"                      ),
    OUTPATIENT_SERVICES   ("Outpatient Services"   , null, "dtmi:digitaltwins:rec_3_3:core:Room;14"                     ),
    PANTRY                ("Pantry"                , null, "dtmi:digitaltwins:rec_3_3:building:Pentry;1"                ),
    PARKING               ("Parking Garage"        , null, "dtmi:digitaltwins:rec_3_3:building:Garage;1"                ),
    PERSONAL_HYGIENE      ("Personal Hygiene"      , null, "dtmi:digitaltwins:rec_3_3:building:PersonalHygiene;1"       ),
    PHARMACY              ("Pharmacy"              , null, "dtmi:digitaltwins:rec_3_3:core:Room;4"                      ),
    RADIOLOGY             ("Radiology"             , null, "dtmi:digitaltwins:rec_3_3:core:Room;6"                      ),
    RECEPTION             ("Reception"             , null, "dtmi:digitaltwins:rec_3_3:building:Reception;1"             ),
    RECORDING_ROOM        ("Recording Room"        , null, "dtmi:digitaltwins:rec_3_3:building:RecordingRoom;1"         ),
    RECOVERY_ROOM         ("Recovery Room"         , null, "dtmi:digitaltwins:rec_3_3:core:Room;8"                      ),
    RECREATIONAL_ROOM     ("Recreational Room"     , null, "dtmi:digitaltwins:rec_3_3:building:RecreationalRoom;1"      ),
    REGION                ("Region"                , null, "dtmi:digitaltwins:rec_3_3:core:Region;1"                    ),
    REGIONAL_CENTER       ("Regional Center"       , null, "dtmi:digitaltwins:rec_3_3:core:Room;29"                     ),
    RESTROOM              ("Restroom"              , null, "dtmi:digitaltwins:rec_3_3:building:Bathroom;1"              ),
    RESTING_ROOM          ("Resting Room"          , null, "dtmi:digitaltwins:rec_3_3:building:RestingRoom;1"           ),
    RETAIL_ROOM           ("Retail Room"           , null, "dtmi:digitaltwins:rec_3_3:building:RetailRoom;1"            ),
    ROOF_INNER            ("Roof Inner"            , null, "dtmi:digitaltwins:rec_3_3:building:RoofInner;1"             ),
    ROOF_OUTER            ("Roof Outer"            , null, "dtmi:digitaltwins:rec_3_3:building:RoofOuter;1"             ),
    ROOF_TOP              ("Roof Top"              , null, "dtmi:digitaltwins:rec_3_3:building:RoofOuter;1"             ),
    ROOM                  ("Room"                  , null, "dtmi:digitaltwins:rec_3_3:core:Room;1"                      ),
    SCHOOL                ("School"                , null, "dtmi:digitaltwins:rec_3_3:building:School;1"                ),
    SECTION               ("Section"               , null, "dtmi:digitaltwins:rec_3_3:core:Space;1"                     ),
    SECURITY_ROOM         ("Security Room"         , null, "dtmi:digitaltwins:rec_3_3:building:SecurityRoom;1"          ),
    SERVER_ROOM           ("Server Room"           , null, "dtmi:digitaltwins:rec_3_3:core:Room;24"                     ),
    SERVICE_SHAFT         ("Service Shaft"         , null, "dtmi:digitaltwins:rec_3_3:building:ServiceShaft;1"          ),
    SHELTER               ("Shelter"               , null, "dtmi:digitaltwins:rec_3_3:building:Shelter;1"               ),
    SHIP                  ("Ship"                  , null, "dtmi:digitaltwins:rec_3_3:core:Room;26"                     ),
    SHOPPING_MALL         ("Shopping Mall"         , null, "dtmi:digitaltwins:rec_3_3:building:ShoppingMall;1"          ),
    SLAB                  ("Slab"                  , null, "dtmi:digitaltwins:rec_3_3:building:Slab;1"                  ),
    SMALL_STUDY_ROOM      ("Small Study Room"      , null, "dtmi:digitaltwins:rec_3_3:building:SmallStudyRoom;1"        ),
    SPRINKLER_ROOM        ("Sprinkler Room"        , null, "dtmi:digitaltwins:rec_3_3:building:SprinklerRoom;1"         ),
    STADIUM               ("Stadium"               , null, "dtmi:digitaltwins:rec_3_3:building:Stadium;1"               ),
    STAFF_ROOM            ("Staff Room"            , null, "dtmi:digitaltwins:rec_3_3:building:StaffRoom;1"             ),
    STAIRWELL             ("Stairwell"             , null, "dtmi:digitaltwins:rec_3_3:building:Stairwell;1"             ),
    STAIRS                ("Stairs"                , null, null                                                         ),
    STORAGE               ("Storage"               , null, "dtmi:digitaltwins:rec_3_3:building:Storage;1"               ),
    STORAGE_ROOM          ("Storage Room"          , null, "dtmi:digitaltwins:rec_3_3:building:Storage;1"               ),
    SUB_BUILDING          ("SubBuilding"           , null, "dtmi:digitaltwins:rec_3_3:core:SubBuilding;1"               ),
    SUPPLY_ROOM           ("Supply Room"           , null, "dtmi:digitaltwins:rec_3_3:core:Room;11"                     ),
    TELECOMMUNICATION_ROOM("Telecommunication Room", null, "dtmi:digitaltwins:rec_3_3:building:TelecommunicationRoom;1" ),
    TENANT_UNIT           ("Tenant Unit"           , null, "dtmi:digitaltwins:rec_3_3:business:TenantUnit;1"            ),
    TERRACE               ("Terrace"               , null, "dtmi:digitaltwins:rec_3_3:building:Terrace;1"               ),
    THEATER               ("Theater"               , null, "dtmi:digitaltwins:rec_3_3:building:Theater;1"               ),
    THERAPY               ("Therapy"               , null, "dtmi:digitaltwins:rec_3_3:core:Room;10"                     ),
    TRAILER               ("Trailer"               , null, null                                                         ),
    TREATMENT_ROOM        ("Treatment Room"        , null, "dtmi:digitaltwins:rec_3_3:building:TreatmentRoom;1"         ),
    TREATMENT_WAITING_ROOM("Treatment Waiting Room", null, "dtmi:digitaltwins:rec_3_3:building:TreatmentWaitingRoom;1"  ),
    TRUCK                 ("Truck"                 , null, "dtmi:digitaltwins:rec_3_3:core:Room;27"                     ),
    UTILITIES_ROOM        ("Utilities Room"        , null, "dtmi:digitaltwins:rec_3_3:building:UtilitiesRoom;1"         ),
    WARD                  ("Ward"                  , null, "dtmi:digitaltwins:rec_3_3:core:Room;3"                      ),
    WASTE_MANAGEMENT_ROOM ("Waste Management Room" , null, "dtmi:digitaltwins:rec_3_3:building:WasteManagementRoom;1"   ),
    WORKSHOP              ("Workshop"              , null, "dtmi:digitaltwins:rec_3_3:building:Workshop;1"              ),
    ZONE                  ("Zone"                  , null, "dtmi:digitaltwins:rec_3_3:core:Zone;1"                      ),
    //--//
    OTHER("Other", null, null);
    // @formatter:on

    private final String m_displayName;
    private final String m_description;
    private final String m_azureDigitalTwin;

    LocationType(String displayName,
                 String description,
                 String azureDigitalTwin)
    {
        m_displayName      = displayName;
        m_description      = description;
        m_azureDigitalTwin = azureDigitalTwin;
    }

    @Override
    public String getDisplayName()
    {
        return m_displayName;
    }

    @Override
    public String getDescription()
    {
        return m_description;
    }

    public String getAzureDigitalTwin()
    {
        return m_azureDigitalTwin;
    }
}
