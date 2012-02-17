package com.javacodegeeks.android.json.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by IntelliJ IDEA.
 * User: roger
 * Date: 22.05.11
 * Time: 21:45
 * To change this template use File | Settings | File Templates.
 */
public class RI_Event {
/*  {"id":"5015",
   "customerId":"5003",
   "displayOrder":"0",
   "eventName":"marker",
   "componentId":12007,
   "actionMap":
            {"clickevent":
            {"clickhandler3":"something3","clickhandler":"something","clickhandler2":"something2"}}
  }
  */

    @SerializedName("id")
    public String id;

    @SerializedName("customerId")
    public String customerId;

    @SerializedName("displayOrder")
    public String displayOrder;


    @SerializedName("eventName")
    public String eventName;

    @SerializedName("componentId")
    public String componentId;

    @SerializedName("actionMap")
    public RI_ActionMap actionMap;

}
