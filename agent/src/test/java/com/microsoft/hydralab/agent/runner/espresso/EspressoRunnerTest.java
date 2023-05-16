package com.microsoft.hydralab.agent.runner.espresso;

import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.util.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EspressoRunnerTest extends BaseTest {

    @Test
    void replaceIllegalStrInArg() {
        String argString1 = "com.microsoft.hydralab.agent.runner.espresso.EspressoRunnerTest#testCase";
        Assertions.assertTrue(argString1.equals(argString1.replaceAll(Const.RegexString.INSTRUMENT_ILLEGAL_STR, "")),
                "argString1 should not be modified");

        String argString2 = "com.microsoft.hydralab.agent.runner.espresso.EspressoRunnerTest# testCase";
        Assertions.assertTrue(argString1.equals(argString2.replaceAll(Const.RegexString.INSTRUMENT_ILLEGAL_STR, "")),
                "argString2 should be modified");

        String argString3 = "com.microsoft.hydralab.agent.runner.espresso.EspressoRunnerTest# |testCase ";
        Assertions.assertTrue(argString1.equals(argString3.replaceAll(Const.RegexString.INSTRUMENT_ILLEGAL_STR, "")),
                "argString3 should be modified");

        String argString4 = "com.microsoft.hydralab.agent.runner.espresso.EspressoRunnerTest#\"testCase ";
        Assertions.assertTrue(argString1.equals(argString4.replaceAll(Const.RegexString.INSTRUMENT_ILLEGAL_STR, "")),
                "argString4 should be modified");
    }

}