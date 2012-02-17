package com.javacodegeeks.android.json.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by IntelliJ IDEA.
 * User: roger
 * Date: 22.05.11
 * Time: 22:00
 * To change this template use File | Settings | File Templates.
 */
public class RI_ClickEvent {
    /*
    {"clickhandler3":"something3","clickhandler":"something","clickhandler2":"something2"}
     */
    @SerializedName("clickhandler")
    public String clickhandler;

    @SerializedName("clickhandler2")
    public String clickhandler2;

    @SerializedName("clickhandler3")
    public String clickhandler3;


}
