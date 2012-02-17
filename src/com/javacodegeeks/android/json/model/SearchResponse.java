package com.javacodegeeks.android.json.model;

import java.util.List;

public class SearchResponse {
/*
{"status":"OK","output":[{"event":
                                {"id":"5015","customerId":"5003","displayOrder":"0","eventName":"marker","componentId":12007,"actionMap":
                                    {"clickevent":
                                    {"clickhandler3":"something3","clickhandler":"something","clickhandler2":"something2"}}},
                                    "displayOrder":0,"downloadUrl":"http://ar.mobile-form.com:80/download?id=12007","componentType":"IMAGE"},
                        {"event":{"id":"9012","customerId":"5003","displayOrder":"0","eventName":"marker","componentId":9010,"actionMap":{"clickevent":{"clickhandler3":"something3","clickhandler":"something","clickhandler2":"something2"}}},"displayOrder":0,"downloadUrl":"http://ar.mobile-form.com:80/download?id=9010","componentType":"IMAGE"},
                        {"event":{"id":"10007","customerId":"5003","displayOrder":"0","eventName":"marker","componentId":12007,"actionMap":{"clickevent":{"clickhandler3":"something3","clickhandler":"something","clickhandler2":"something2"}}},"displayOrder":0,"downloadUrl":"http://ar.mobile-form.com:80/download?id=12007","componentType":"IMAGE"},
                        {"event":{"id":"13007","customerId":"5003","displayOrder":"0","eventName":"marker","componentId":9010,"actionMap":{"clickevent":{"clickhandler3":"something3","clickhandler":"something","clickhandler2":"something2"}}},"displayOrder":0,"downloadUrl":"http://ar.mobile-form.com:80/download?id=9010","componentType":"IMAGE"}]}
 */

/*
    {"status":"OK","output":
        [{"event":
                {"id":"6010","customerId":"5003","displayOrder":"0","eventName":"marker","componentId":5017,"actionMap":
                    {"clickevent":
                        {"clickhandler3":"something3","clickhandler":"something","clickhandler2":"something2"}}},
                        "displayOrder":0,"downloadUrl":"http://ar.mobile-form.com:80/download?id=5017","TTL":0,"componentType":"MODEL"},
         {"event":{"id":"13010","customerId":"5003","displayOrder":"0","eventName":"marker","componentId":9010,"actionMap":
                    {"clickevent":
                        {"clickhandler3":"something3","clickhandler":"something","clickhandler2":"something2"}}},
                        "displayOrder":0,"downloadUrl":"http://ar.mobile-form.com:80/download?id=9010","componentType":"IMAGE"}]}
        */


    public String status;
    public List<Output> output;
}
