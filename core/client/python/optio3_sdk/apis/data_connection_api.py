# coding: utf-8

"""
Copyright (C) 2017-2018, Optio3, Inc. All Rights Reserved.

Proprietary & Confidential Information.

Optio3 Hub APIs
APIs and Definitions for the Optio3 Hub product.

OpenAPI spec version: 1.0.0


NOTE: This class is auto generated by the swagger code generator program.
https://github.com/swagger-api/swagger-codegen.git
Do not edit the class manually.
"""


from __future__ import absolute_import

import sys
import os
import re

# python 2 and python 3 compatibility library
from six import iteritems

from ..configuration import Configuration
from ..api_client import ApiClient


class DataConnectionApi(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    Ref: https://github.com/swagger-api/swagger-codegen
    """

    def __init__(self, api_client=None):
        config = Configuration()
        if api_client:
            self.api_client = api_client
        else:
            if not config.api_client:
                config.api_client = ApiClient()
            self.api_client = config.api_client

    def controller_metadata_aggregation(self, controller_id, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.controller_metadata_aggregation(controller_id, callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param str controller_id: (required)
        :param bool unclassified:
        :param int below_threshold_id:
        :return: ControllerMetadataAggregation
                 If the method is called asynchronously,
                 returns the request thread.
        """
        kwargs['_return_http_data_only'] = True
        if kwargs.get('callback'):
            return self.controller_metadata_aggregation_with_http_info(controller_id, **kwargs)
        else:
            (data) = self.controller_metadata_aggregation_with_http_info(controller_id, **kwargs)
            return data

    def controller_metadata_aggregation_with_http_info(self, controller_id, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.controller_metadata_aggregation_with_http_info(controller_id, callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param str controller_id: (required)
        :param bool unclassified:
        :param int below_threshold_id:
        :return: ControllerMetadataAggregation
                 If the method is called asynchronously,
                 returns the request thread.
        """

        all_params = ['controller_id', 'unclassified', 'below_threshold_id']
        all_params.append('callback')
        all_params.append('_return_http_data_only')
        all_params.append('_preload_content')
        all_params.append('_request_timeout')

        params = locals()
        for key, val in iteritems(params['kwargs']):
            if key not in all_params:
                raise TypeError(
                    "Got an unexpected keyword argument '%s'"
                    " to method controller_metadata_aggregation" % key
                )
            params[key] = val
        del params['kwargs']
        # verify the required parameter 'controller_id' is set
        if ('controller_id' not in params) or (params['controller_id'] is None):
            raise ValueError("Missing the required parameter `controller_id` when calling `controller_metadata_aggregation`")


        collection_formats = {}

        resource_path = '/data-connection/metadata-aggregation/controller/{controllerId}'.replace('{format}', 'json')
        path_params = {}
        if 'controller_id' in params:
            path_params['controllerId'] = params['controller_id']

        query_params = {}
        if 'unclassified' in params:
            query_params['unclassified'] = params['unclassified']
        if 'below_threshold_id' in params:
            query_params['belowThresholdId'] = params['below_threshold_id']

        header_params = {}

        form_params = []
        local_var_files = {}

        body_params = None
        # HTTP header `Accept`
        header_params['Accept'] = self.api_client.\
            select_header_accept(['application/json'])

        # Authentication setting
        auth_settings = []

        return self.api_client.call_api(resource_path, 'GET',
                                        path_params,
                                        query_params,
                                        header_params,
                                        body=body_params,
                                        post_params=form_params,
                                        files=local_var_files,
                                        response_type='ControllerMetadataAggregation',
                                        auth_settings=auth_settings,
                                        callback=params.get('callback'),
                                        _return_http_data_only=params.get('_return_http_data_only'),
                                        _preload_content=params.get('_preload_content', True),
                                        _request_timeout=params.get('_request_timeout'),
                                        collection_formats=collection_formats)

    def equipment_aggregation(self, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.equipment_aggregation(callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :return: EquipmentAggregation
                 If the method is called asynchronously,
                 returns the request thread.
        """
        kwargs['_return_http_data_only'] = True
        if kwargs.get('callback'):
            return self.equipment_aggregation_with_http_info(**kwargs)
        else:
            (data) = self.equipment_aggregation_with_http_info(**kwargs)
            return data

    def equipment_aggregation_with_http_info(self, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.equipment_aggregation_with_http_info(callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :return: EquipmentAggregation
                 If the method is called asynchronously,
                 returns the request thread.
        """

        all_params = []
        all_params.append('callback')
        all_params.append('_return_http_data_only')
        all_params.append('_preload_content')
        all_params.append('_request_timeout')

        params = locals()
        for key, val in iteritems(params['kwargs']):
            if key not in all_params:
                raise TypeError(
                    "Got an unexpected keyword argument '%s'"
                    " to method equipment_aggregation" % key
                )
            params[key] = val
        del params['kwargs']

        collection_formats = {}

        resource_path = '/data-connection/equipment-aggregation'.replace('{format}', 'json')
        path_params = {}

        query_params = {}

        header_params = {}

        form_params = []
        local_var_files = {}

        body_params = None
        # HTTP header `Accept`
        header_params['Accept'] = self.api_client.\
            select_header_accept(['application/json'])

        # Authentication setting
        auth_settings = []

        return self.api_client.call_api(resource_path, 'GET',
                                        path_params,
                                        query_params,
                                        header_params,
                                        body=body_params,
                                        post_params=form_params,
                                        files=local_var_files,
                                        response_type='EquipmentAggregation',
                                        auth_settings=auth_settings,
                                        callback=params.get('callback'),
                                        _return_http_data_only=params.get('_return_http_data_only'),
                                        _preload_content=params.get('_preload_content', True),
                                        _request_timeout=params.get('_request_timeout'),
                                        collection_formats=collection_formats)

    def get_last_sample(self, connection_id, control_point_id, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.get_last_sample(connection_id, control_point_id, callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param str connection_id: (required)
        :param str control_point_id: (required)
        :return: datetime
                 If the method is called asynchronously,
                 returns the request thread.
        """
        kwargs['_return_http_data_only'] = True
        if kwargs.get('callback'):
            return self.get_last_sample_with_http_info(connection_id, control_point_id, **kwargs)
        else:
            (data) = self.get_last_sample_with_http_info(connection_id, control_point_id, **kwargs)
            return data

    def get_last_sample_with_http_info(self, connection_id, control_point_id, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.get_last_sample_with_http_info(connection_id, control_point_id, callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param str connection_id: (required)
        :param str control_point_id: (required)
        :return: datetime
                 If the method is called asynchronously,
                 returns the request thread.
        """

        all_params = ['connection_id', 'control_point_id']
        all_params.append('callback')
        all_params.append('_return_http_data_only')
        all_params.append('_preload_content')
        all_params.append('_request_timeout')

        params = locals()
        for key, val in iteritems(params['kwargs']):
            if key not in all_params:
                raise TypeError(
                    "Got an unexpected keyword argument '%s'"
                    " to method get_last_sample" % key
                )
            params[key] = val
        del params['kwargs']
        # verify the required parameter 'connection_id' is set
        if ('connection_id' not in params) or (params['connection_id'] is None):
            raise ValueError("Missing the required parameter `connection_id` when calling `get_last_sample`")
        # verify the required parameter 'control_point_id' is set
        if ('control_point_id' not in params) or (params['control_point_id'] is None):
            raise ValueError("Missing the required parameter `control_point_id` when calling `get_last_sample`")


        collection_formats = {}

        resource_path = '/data-connection/connection/{connectionId}/last-sample/{controlPointId}'.replace('{format}', 'json')
        path_params = {}
        if 'connection_id' in params:
            path_params['connectionId'] = params['connection_id']
        if 'control_point_id' in params:
            path_params['controlPointId'] = params['control_point_id']

        query_params = {}

        header_params = {}

        form_params = []
        local_var_files = {}

        body_params = None
        # HTTP header `Accept`
        header_params['Accept'] = self.api_client.\
            select_header_accept(['application/json'])

        # Authentication setting
        auth_settings = []

        return self.api_client.call_api(resource_path, 'GET',
                                        path_params,
                                        query_params,
                                        header_params,
                                        body=body_params,
                                        post_params=form_params,
                                        files=local_var_files,
                                        response_type='datetime',
                                        auth_settings=auth_settings,
                                        callback=params.get('callback'),
                                        _return_http_data_only=params.get('_return_http_data_only'),
                                        _preload_content=params.get('_preload_content', True),
                                        _request_timeout=params.get('_request_timeout'),
                                        collection_formats=collection_formats)

    def get_site(self, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.get_site(callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :return: DataConnectionSite
                 If the method is called asynchronously,
                 returns the request thread.
        """
        kwargs['_return_http_data_only'] = True
        if kwargs.get('callback'):
            return self.get_site_with_http_info(**kwargs)
        else:
            (data) = self.get_site_with_http_info(**kwargs)
            return data

    def get_site_with_http_info(self, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.get_site_with_http_info(callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :return: DataConnectionSite
                 If the method is called asynchronously,
                 returns the request thread.
        """

        all_params = []
        all_params.append('callback')
        all_params.append('_return_http_data_only')
        all_params.append('_preload_content')
        all_params.append('_request_timeout')

        params = locals()
        for key, val in iteritems(params['kwargs']):
            if key not in all_params:
                raise TypeError(
                    "Got an unexpected keyword argument '%s'"
                    " to method get_site" % key
                )
            params[key] = val
        del params['kwargs']

        collection_formats = {}

        resource_path = '/data-connection/site'.replace('{format}', 'json')
        path_params = {}

        query_params = {}

        header_params = {}

        form_params = []
        local_var_files = {}

        body_params = None
        # HTTP header `Accept`
        header_params['Accept'] = self.api_client.\
            select_header_accept(['application/json'])

        # Authentication setting
        auth_settings = []

        return self.api_client.call_api(resource_path, 'GET',
                                        path_params,
                                        query_params,
                                        header_params,
                                        body=body_params,
                                        post_params=form_params,
                                        files=local_var_files,
                                        response_type='DataConnectionSite',
                                        auth_settings=auth_settings,
                                        callback=params.get('callback'),
                                        _return_http_data_only=params.get('_return_http_data_only'),
                                        _preload_content=params.get('_preload_content', True),
                                        _request_timeout=params.get('_request_timeout'),
                                        collection_formats=collection_formats)

    def metadata_aggregation(self, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.metadata_aggregation(callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param bool unclassified:
        :return: MetadataAggregation
                 If the method is called asynchronously,
                 returns the request thread.
        """
        kwargs['_return_http_data_only'] = True
        if kwargs.get('callback'):
            return self.metadata_aggregation_with_http_info(**kwargs)
        else:
            (data) = self.metadata_aggregation_with_http_info(**kwargs)
            return data

    def metadata_aggregation_with_http_info(self, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.metadata_aggregation_with_http_info(callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param bool unclassified:
        :return: MetadataAggregation
                 If the method is called asynchronously,
                 returns the request thread.
        """

        all_params = ['unclassified']
        all_params.append('callback')
        all_params.append('_return_http_data_only')
        all_params.append('_preload_content')
        all_params.append('_request_timeout')

        params = locals()
        for key, val in iteritems(params['kwargs']):
            if key not in all_params:
                raise TypeError(
                    "Got an unexpected keyword argument '%s'"
                    " to method metadata_aggregation" % key
                )
            params[key] = val
        del params['kwargs']


        collection_formats = {}

        resource_path = '/data-connection/metadata-aggregation'.replace('{format}', 'json')
        path_params = {}

        query_params = {}
        if 'unclassified' in params:
            query_params['unclassified'] = params['unclassified']

        header_params = {}

        form_params = []
        local_var_files = {}

        body_params = None
        # HTTP header `Accept`
        header_params['Accept'] = self.api_client.\
            select_header_accept(['application/json'])

        # Authentication setting
        auth_settings = []

        return self.api_client.call_api(resource_path, 'GET',
                                        path_params,
                                        query_params,
                                        header_params,
                                        body=body_params,
                                        post_params=form_params,
                                        files=local_var_files,
                                        response_type='MetadataAggregation',
                                        auth_settings=auth_settings,
                                        callback=params.get('callback'),
                                        _return_http_data_only=params.get('_return_http_data_only'),
                                        _preload_content=params.get('_preload_content', True),
                                        _request_timeout=params.get('_request_timeout'),
                                        collection_formats=collection_formats)

    def receive_raw(self, endpoint_id, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.receive_raw(endpoint_id, callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param str endpoint_id: (required)
        :param str arg:
        :param object body:
        :return: object
                 If the method is called asynchronously,
                 returns the request thread.
        """
        kwargs['_return_http_data_only'] = True
        if kwargs.get('callback'):
            return self.receive_raw_with_http_info(endpoint_id, **kwargs)
        else:
            (data) = self.receive_raw_with_http_info(endpoint_id, **kwargs)
            return data

    def receive_raw_with_http_info(self, endpoint_id, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.receive_raw_with_http_info(endpoint_id, callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param str endpoint_id: (required)
        :param str arg:
        :param object body:
        :return: object
                 If the method is called asynchronously,
                 returns the request thread.
        """

        all_params = ['endpoint_id', 'arg', 'body']
        all_params.append('callback')
        all_params.append('_return_http_data_only')
        all_params.append('_preload_content')
        all_params.append('_request_timeout')

        params = locals()
        for key, val in iteritems(params['kwargs']):
            if key not in all_params:
                raise TypeError(
                    "Got an unexpected keyword argument '%s'"
                    " to method receive_raw" % key
                )
            params[key] = val
        del params['kwargs']
        # verify the required parameter 'endpoint_id' is set
        if ('endpoint_id' not in params) or (params['endpoint_id'] is None):
            raise ValueError("Missing the required parameter `endpoint_id` when calling `receive_raw`")


        collection_formats = {}

        resource_path = '/data-connection/endpoint/{endpointId}'.replace('{format}', 'json')
        path_params = {}
        if 'endpoint_id' in params:
            path_params['endpointId'] = params['endpoint_id']

        query_params = {}
        if 'arg' in params:
            query_params['arg'] = params['arg']

        header_params = {}

        form_params = []
        local_var_files = {}

        body_params = None
        if 'body' in params:
            body_params = params['body']
        # HTTP header `Accept`
        header_params['Accept'] = self.api_client.\
            select_header_accept(['application/json'])

        # HTTP header `Content-Type`
        header_params['Content-Type'] = self.api_client.\
            select_header_content_type(['application/json'])

        # Authentication setting
        auth_settings = []

        return self.api_client.call_api(resource_path, 'POST',
                                        path_params,
                                        query_params,
                                        header_params,
                                        body=body_params,
                                        post_params=form_params,
                                        files=local_var_files,
                                        response_type='object',
                                        auth_settings=auth_settings,
                                        callback=params.get('callback'),
                                        _return_http_data_only=params.get('_return_http_data_only'),
                                        _preload_content=params.get('_preload_content', True),
                                        _request_timeout=params.get('_request_timeout'),
                                        collection_formats=collection_formats)

    def receive_stream(self, endpoint_id, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.receive_stream(endpoint_id, callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param str endpoint_id: (required)
        :param str arg:
        :param InputStream body:
        :return: object
                 If the method is called asynchronously,
                 returns the request thread.
        """
        kwargs['_return_http_data_only'] = True
        if kwargs.get('callback'):
            return self.receive_stream_with_http_info(endpoint_id, **kwargs)
        else:
            (data) = self.receive_stream_with_http_info(endpoint_id, **kwargs)
            return data

    def receive_stream_with_http_info(self, endpoint_id, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.receive_stream_with_http_info(endpoint_id, callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param str endpoint_id: (required)
        :param str arg:
        :param InputStream body:
        :return: object
                 If the method is called asynchronously,
                 returns the request thread.
        """

        all_params = ['endpoint_id', 'arg', 'body']
        all_params.append('callback')
        all_params.append('_return_http_data_only')
        all_params.append('_preload_content')
        all_params.append('_request_timeout')

        params = locals()
        for key, val in iteritems(params['kwargs']):
            if key not in all_params:
                raise TypeError(
                    "Got an unexpected keyword argument '%s'"
                    " to method receive_stream" % key
                )
            params[key] = val
        del params['kwargs']
        # verify the required parameter 'endpoint_id' is set
        if ('endpoint_id' not in params) or (params['endpoint_id'] is None):
            raise ValueError("Missing the required parameter `endpoint_id` when calling `receive_stream`")


        collection_formats = {}

        resource_path = '/data-connection/endpoint/{endpointId}/stream'.replace('{format}', 'json')
        path_params = {}
        if 'endpoint_id' in params:
            path_params['endpointId'] = params['endpoint_id']

        query_params = {}
        if 'arg' in params:
            query_params['arg'] = params['arg']

        header_params = {}

        form_params = []
        local_var_files = {}

        body_params = None
        if 'body' in params:
            body_params = params['body']
        # HTTP header `Accept`
        header_params['Accept'] = self.api_client.\
            select_header_accept(['application/json'])

        # HTTP header `Content-Type`
        header_params['Content-Type'] = self.api_client.\
            select_header_content_type(['application/octet-stream'])

        # Authentication setting
        auth_settings = []

        return self.api_client.call_api(resource_path, 'POST',
                                        path_params,
                                        query_params,
                                        header_params,
                                        body=body_params,
                                        post_params=form_params,
                                        files=local_var_files,
                                        response_type='object',
                                        auth_settings=auth_settings,
                                        callback=params.get('callback'),
                                        _return_http_data_only=params.get('_return_http_data_only'),
                                        _preload_content=params.get('_preload_content', True),
                                        _request_timeout=params.get('_request_timeout'),
                                        collection_formats=collection_formats)

    def set_last_sample(self, connection_id, control_point_id, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.set_last_sample(connection_id, control_point_id, callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param str connection_id: (required)
        :param str control_point_id: (required)
        :param datetime body:
        :return: datetime
                 If the method is called asynchronously,
                 returns the request thread.
        """
        kwargs['_return_http_data_only'] = True
        if kwargs.get('callback'):
            return self.set_last_sample_with_http_info(connection_id, control_point_id, **kwargs)
        else:
            (data) = self.set_last_sample_with_http_info(connection_id, control_point_id, **kwargs)
            return data

    def set_last_sample_with_http_info(self, connection_id, control_point_id, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.set_last_sample_with_http_info(connection_id, control_point_id, callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param str connection_id: (required)
        :param str control_point_id: (required)
        :param datetime body:
        :return: datetime
                 If the method is called asynchronously,
                 returns the request thread.
        """

        all_params = ['connection_id', 'control_point_id', 'body']
        all_params.append('callback')
        all_params.append('_return_http_data_only')
        all_params.append('_preload_content')
        all_params.append('_request_timeout')

        params = locals()
        for key, val in iteritems(params['kwargs']):
            if key not in all_params:
                raise TypeError(
                    "Got an unexpected keyword argument '%s'"
                    " to method set_last_sample" % key
                )
            params[key] = val
        del params['kwargs']
        # verify the required parameter 'connection_id' is set
        if ('connection_id' not in params) or (params['connection_id'] is None):
            raise ValueError("Missing the required parameter `connection_id` when calling `set_last_sample`")
        # verify the required parameter 'control_point_id' is set
        if ('control_point_id' not in params) or (params['control_point_id'] is None):
            raise ValueError("Missing the required parameter `control_point_id` when calling `set_last_sample`")


        collection_formats = {}

        resource_path = '/data-connection/connection/{connectionId}/last-sample/{controlPointId}'.replace('{format}', 'json')
        path_params = {}
        if 'connection_id' in params:
            path_params['connectionId'] = params['connection_id']
        if 'control_point_id' in params:
            path_params['controlPointId'] = params['control_point_id']

        query_params = {}

        header_params = {}

        form_params = []
        local_var_files = {}

        body_params = None
        if 'body' in params:
            body_params = params['body']
        # HTTP header `Accept`
        header_params['Accept'] = self.api_client.\
            select_header_accept(['application/json'])

        # HTTP header `Content-Type`
        header_params['Content-Type'] = self.api_client.\
            select_header_content_type(['application/json'])

        # Authentication setting
        auth_settings = []

        return self.api_client.call_api(resource_path, 'POST',
                                        path_params,
                                        query_params,
                                        header_params,
                                        body=body_params,
                                        post_params=form_params,
                                        files=local_var_files,
                                        response_type='datetime',
                                        auth_settings=auth_settings,
                                        callback=params.get('callback'),
                                        _return_http_data_only=params.get('_return_http_data_only'),
                                        _preload_content=params.get('_preload_content', True),
                                        _request_timeout=params.get('_request_timeout'),
                                        collection_formats=collection_formats)

    def set_site(self, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.set_site(callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param DataConnectionSite body:
        :return: DataConnectionSite
                 If the method is called asynchronously,
                 returns the request thread.
        """
        kwargs['_return_http_data_only'] = True
        if kwargs.get('callback'):
            return self.set_site_with_http_info(**kwargs)
        else:
            (data) = self.set_site_with_http_info(**kwargs)
            return data

    def set_site_with_http_info(self, **kwargs):
        """
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please define a `callback` function
        to be invoked when receiving the response.
        >>> def callback_function(response):
        >>>     pprint(response)
        >>>
        >>> thread = api.set_site_with_http_info(callback=callback_function)

        :param callback function: The callback function
            for asynchronous request. (optional)
        :param DataConnectionSite body:
        :return: DataConnectionSite
                 If the method is called asynchronously,
                 returns the request thread.
        """

        all_params = ['body']
        all_params.append('callback')
        all_params.append('_return_http_data_only')
        all_params.append('_preload_content')
        all_params.append('_request_timeout')

        params = locals()
        for key, val in iteritems(params['kwargs']):
            if key not in all_params:
                raise TypeError(
                    "Got an unexpected keyword argument '%s'"
                    " to method set_site" % key
                )
            params[key] = val
        del params['kwargs']


        collection_formats = {}

        resource_path = '/data-connection/site'.replace('{format}', 'json')
        path_params = {}

        query_params = {}

        header_params = {}

        form_params = []
        local_var_files = {}

        body_params = None
        if 'body' in params:
            body_params = params['body']
        # HTTP header `Accept`
        header_params['Accept'] = self.api_client.\
            select_header_accept(['application/json'])

        # HTTP header `Content-Type`
        header_params['Content-Type'] = self.api_client.\
            select_header_content_type(['application/json'])

        # Authentication setting
        auth_settings = []

        return self.api_client.call_api(resource_path, 'POST',
                                        path_params,
                                        query_params,
                                        header_params,
                                        body=body_params,
                                        post_params=form_params,
                                        files=local_var_files,
                                        response_type='DataConnectionSite',
                                        auth_settings=auth_settings,
                                        callback=params.get('callback'),
                                        _return_http_data_only=params.get('_return_http_data_only'),
                                        _preload_content=params.get('_preload_content', True),
                                        _request_timeout=params.get('_request_timeout'),
                                        collection_formats=collection_formats)
