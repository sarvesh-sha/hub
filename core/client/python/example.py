import optio3_sdk
from utils import configure, fetchSamples

# Configure the apis with the correct host and credentials
configure("http://localhost:8080", "test@email.com", "password")

# Get instance of the tableau API
data_connection_api = optio3_sdk.DataConnectionApi()

# Get aggregation of all discovered points, equipments, and buildings
metadata = data_connection_api.metadata_aggregation()

buildings = metadata.building_equipments.keys()
equipments = metadata.equipment_names.keys()
controllers = metadata.controller_names.keys()
all_points = []

print("%d buildings found." % len(buildings))
print("%d equipments found." % len(equipments))
print("%d controllers found." % len(controllers))

for building, equipments_in_building in metadata.building_equipments.items():
    print("Building: %s has %d equipments" % (building, len(equipments_in_building)))

for controller in controllers:
    controller_metadata = data_connection_api.controller_metadata_aggregation(controller)
    print("Controller: %s has %d points" % (controller_metadata.name, len(controller_metadata.points)))
    all_points = all_points + controller_metadata.points

timeseries_api = optio3_sdk.AssetTimeSeriesApi()


for point in all_points[:10]:
    resp = fetchSamples(point.point_id)
    print("Got %d samples for %s" % (len(resp.timestamps), point.point_id))
