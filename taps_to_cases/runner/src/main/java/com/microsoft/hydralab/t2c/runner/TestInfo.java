// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner;

import java.util.ArrayList;

public class TestInfo {
    ArrayList<DriverInfo> drivers;
    ArrayList<ActionInfo> cases;

    public TestInfo(ArrayList<DriverInfo> drivers, ArrayList<ActionInfo> cases) {
        this.cases = cases;
        this.drivers = drivers;
    }

    public ArrayList<DriverInfo> getDrivers() {
        return drivers;
    }

    public ArrayList<ActionInfo> getCases() {
        return cases;
    }
}
