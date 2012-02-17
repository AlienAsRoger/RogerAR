package com.javacodegeeks.android.json.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by IntelliJ IDEA.
 * User: roger
 * Date: 22.05.11
 * Time: 21:56
 * To change this template use File | Settings | File Templates.
 */
public class RI_ActionMap {
    /*
      {"clickevent":
            {"clickhandler3":"something3","clickhandler":"something","clickhandler2":"something2"}}
     */

    @SerializedName("clickevent")
    public RI_ClickEvent clickevent;
}

