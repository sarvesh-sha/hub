import optio3_sdk
from optio3_sdk.rest import ApiException

def configure(host="http://localhost:8080", username="", password=""):
    configuration = optio3_sdk.Configuration()
    configuration.host = host + "/api/v1"

    user_api = optio3_sdk.UsersApi()

    # Log in to the api and set the client with the correct credentials
    try:
        (_, _, headers) = user_api.login_with_http_info(username=username, password=password)
        configuration.api_client = optio3_sdk.ApiClient(cookie=headers["Set-Cookie"])
    except ApiException as e:
        print("Exception when logging in: %s\n" % e)
        raise


def fetchSamples(point):
    timeseries_api = optio3_sdk.AssetTimeSeriesApi()
    data_connection_api = optio3_sdk.DataConnectionApi()

    spec = optio3_sdk.TimeSeriesPropertyRequest(sys_id=point, prop="present_value")
    req = optio3_sdk.TimeSeriesSinglePropertyRequest(spec=spec)
    
    return timeseries_api.get_values_single(body=req)