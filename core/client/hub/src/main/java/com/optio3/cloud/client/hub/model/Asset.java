/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Hub APIs
 * APIs and Definitions for the Optio3 Hub product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.hub.model;

import com.optio3.cloud.client.hub.model.BaseAssetDescriptor;
import com.optio3.cloud.client.hub.model.RecordIdentity;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes(
{
    @JsonSubTypes.Type(value = BACnetDevice.class),
    @JsonSubTypes.Type(value = Device.class),
    @JsonSubTypes.Type(value = DeviceElement.class),
    @JsonSubTypes.Type(value = GatewayAsset.class),
    @JsonSubTypes.Type(value = HostAsset.class),
    @JsonSubTypes.Type(value = IpnDevice.class),
    @JsonSubTypes.Type(value = Location.class),
    @JsonSubTypes.Type(value = LogicalAsset.class),
    @JsonSubTypes.Type(value = MetricsDeviceElement.class),
    @JsonSubTypes.Type(value = NetworkAsset.class)
})
public class Asset
{
  public static final String RECORD_IDENTITY = "Asset";



  public String sysId = null;
  public ZonedDateTime createdOn = null;
  public ZonedDateTime updatedOn = null;
  public String name = null;
  public String physicalName = null;
  public String logicalName = null;
  public String normalizedName = null;
  public String displayName = null;
  public AssetState state = null;
  public String assetId = null;
  public String serialNumber = null;
  public String customerNotes = null;
  public ZonedDateTime lastCheckedDate = null;
  public ZonedDateTime lastUpdatedDate = null;
  public Boolean hidden = null;
  public RecordIdentity location = null;
  public String pointClassId = null;
  public String equipmentClassId = null;
  public String azureDigitalTwinModel = null;
  public Boolean isEquipment = null;
  public List<String> classificationTags = new ArrayList<String>();
  public List<String> manualTags = new ArrayList<String>();
  public RecordIdentity parentAsset = null;
  public BaseAssetDescriptor identityDescriptor = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Asset {\n");

    sb.append("    sysId: ").append(toIndentedString(sysId)).append("\n");
    sb.append("    createdOn: ").append(toIndentedString(createdOn)).append("\n");
    sb.append("    updatedOn: ").append(toIndentedString(updatedOn)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    physicalName: ").append(toIndentedString(physicalName)).append("\n");
    sb.append("    logicalName: ").append(toIndentedString(logicalName)).append("\n");
    sb.append("    normalizedName: ").append(toIndentedString(normalizedName)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    assetId: ").append(toIndentedString(assetId)).append("\n");
    sb.append("    serialNumber: ").append(toIndentedString(serialNumber)).append("\n");
    sb.append("    customerNotes: ").append(toIndentedString(customerNotes)).append("\n");
    sb.append("    lastCheckedDate: ").append(toIndentedString(lastCheckedDate)).append("\n");
    sb.append("    lastUpdatedDate: ").append(toIndentedString(lastUpdatedDate)).append("\n");
    sb.append("    hidden: ").append(toIndentedString(hidden)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    pointClassId: ").append(toIndentedString(pointClassId)).append("\n");
    sb.append("    equipmentClassId: ").append(toIndentedString(equipmentClassId)).append("\n");
    sb.append("    azureDigitalTwinModel: ").append(toIndentedString(azureDigitalTwinModel)).append("\n");
    sb.append("    isEquipment: ").append(toIndentedString(isEquipment)).append("\n");
    sb.append("    classificationTags: ").append(toIndentedString(classificationTags)).append("\n");
    sb.append("    manualTags: ").append(toIndentedString(manualTags)).append("\n");
    sb.append("    parentAsset: ").append(toIndentedString(parentAsset)).append("\n");
    sb.append("    identityDescriptor: ").append(toIndentedString(identityDescriptor)).append("\n");
    sb.append("}");
    return sb.toString();
  }
  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private static String toIndentedString(java.lang.Object o)
  {
    if (o == null)
      return "null";
    return o.toString().replace("\n", "\n    ");
  }
}
