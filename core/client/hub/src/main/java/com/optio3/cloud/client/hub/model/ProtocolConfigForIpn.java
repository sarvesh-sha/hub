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

import com.optio3.cloud.client.hub.model.I2CSensor;
import com.optio3.cloud.client.hub.model.ProtocolConfig;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("ProtocolConfigForIpn")
public class ProtocolConfigForIpn extends ProtocolConfig
{

  public Float accelerometerFrequency = null;
  public Float accelerometerRange = null;
  public Float accelerometerThreshold = null;
  public List<I2CSensor> i2cSensors = new ArrayList<I2CSensor>();
  public String canPort = null;
  public Integer canFrequency = null;
  public Boolean canNoTermination = null;
  public Boolean canInvert = null;
  public String epsolarPort = null;
  public Boolean epsolarInvert = null;
  public String gpsPort = null;
  public String holykellPort = null;
  public Boolean holykellInvert = null;
  public String ipnPort = null;
  public Integer ipnBaudrate = null;
  public Boolean ipnInvert = null;
  public String obdiiPort = null;
  public Integer obdiiFrequency = null;
  public Boolean obdiiInvert = null;
  public String argohytosPort = null;
  public String stealthpowerPort = null;
  public String tristarPort = null;
  public String victronPort = null;
  public String montageBluetoothGatewayPort = null;
  public Boolean simulate = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProtocolConfigForIpn {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    accelerometerFrequency: ").append(toIndentedString(accelerometerFrequency)).append("\n");
    sb.append("    accelerometerRange: ").append(toIndentedString(accelerometerRange)).append("\n");
    sb.append("    accelerometerThreshold: ").append(toIndentedString(accelerometerThreshold)).append("\n");
    sb.append("    i2cSensors: ").append(toIndentedString(i2cSensors)).append("\n");
    sb.append("    canPort: ").append(toIndentedString(canPort)).append("\n");
    sb.append("    canFrequency: ").append(toIndentedString(canFrequency)).append("\n");
    sb.append("    canNoTermination: ").append(toIndentedString(canNoTermination)).append("\n");
    sb.append("    canInvert: ").append(toIndentedString(canInvert)).append("\n");
    sb.append("    epsolarPort: ").append(toIndentedString(epsolarPort)).append("\n");
    sb.append("    epsolarInvert: ").append(toIndentedString(epsolarInvert)).append("\n");
    sb.append("    gpsPort: ").append(toIndentedString(gpsPort)).append("\n");
    sb.append("    holykellPort: ").append(toIndentedString(holykellPort)).append("\n");
    sb.append("    holykellInvert: ").append(toIndentedString(holykellInvert)).append("\n");
    sb.append("    ipnPort: ").append(toIndentedString(ipnPort)).append("\n");
    sb.append("    ipnBaudrate: ").append(toIndentedString(ipnBaudrate)).append("\n");
    sb.append("    ipnInvert: ").append(toIndentedString(ipnInvert)).append("\n");
    sb.append("    obdiiPort: ").append(toIndentedString(obdiiPort)).append("\n");
    sb.append("    obdiiFrequency: ").append(toIndentedString(obdiiFrequency)).append("\n");
    sb.append("    obdiiInvert: ").append(toIndentedString(obdiiInvert)).append("\n");
    sb.append("    argohytosPort: ").append(toIndentedString(argohytosPort)).append("\n");
    sb.append("    stealthpowerPort: ").append(toIndentedString(stealthpowerPort)).append("\n");
    sb.append("    tristarPort: ").append(toIndentedString(tristarPort)).append("\n");
    sb.append("    victronPort: ").append(toIndentedString(victronPort)).append("\n");
    sb.append("    montageBluetoothGatewayPort: ").append(toIndentedString(montageBluetoothGatewayPort)).append("\n");
    sb.append("    simulate: ").append(toIndentedString(simulate)).append("\n");
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