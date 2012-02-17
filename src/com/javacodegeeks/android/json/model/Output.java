package com.javacodegeeks.android.json.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by IntelliJ IDEA.
 * User: roger
 * Date: 22.05.11
 * Time: 21:42
 * To change this template use File | Settings | File Templates.
 */
public class Output {
/*{"event":
        {"id":"5015",
                "customerId":"5003",
                "displayOrder":"0",
                "eventName":"marker",
                "componentId":12007,
                "actionMap":
                        {"clickevent":
                        {"clickhandler3":"something3",
                        "clickhandler":"something",
                        "clickhandler2":"something2"}}},    */
//        "displayOrder":0,
//        "downloadUrl":"http://ar.mobile-form.com:80/download?id=12007",
//        "componentType":"IMAGE"}


    @SerializedName("event")
    public RI_Event event;


    @SerializedName("displayOrder")
    public Integer displayOrder;


    @SerializedName("downloadUrl")
    public String downloadUrl;


    @SerializedName("componentType")
    public String componentType;

    @SerializedName("parent")
    public int parent;
}
