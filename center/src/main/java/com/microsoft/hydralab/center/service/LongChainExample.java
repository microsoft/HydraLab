// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import dev.langchain4j.model.input.structured.StructuredPrompt;

/**
 * @author zhoule
 * @date 07/13/2023
 */

public class LongChainExample {

    @StructuredPrompt({
            "I want you to act as a software tester. I will provide a route map of a mobile application and it will be your job to write a test case. ",
            "The case should be in maestro script format. This is a maestro example",
            "{{maestroExample}}",
            "Firstly I will introduce the format of the route map.",
            "1. It is a unidirectional ordered graph in xml format, the nodes attribute are the pages of app and the id property of each node is the unique id of page. " +
                    "By the way the id of node equals -1 means the app has not been opened.",
            "2. The edges attributes means the only way of jumping from a page to another page. The source property is the unique id of original page and the target property " +
                    "is the unique id of the page after jumping. The attvalue of each edge means the operation type such launch app, click button, click testview etc..",
            "The commands that maestro supported is in the site https://maestro.mobile.dev/api-reference/commands.",
            "Requirements:",
                "1. the case should start from node which id is -1.",
                "2. the case must follow the direction of the edge.",
                "3. the case should jump as many pages as possible of the app.",
                "4. the page can be visited only once",
                "5. you can't use the back command",
                "6. add comment to case declare current page id",
            "The first route map is {{routeMap}}",
            "please generate a maestro script for this route map."
    })
    static class MaestroCaseGeneration {

        String maestroExample;
        String routeMap;
    }

}
