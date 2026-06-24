package com.microsoft.hydralab.center.openai;

import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import org.junit.Assert;

public class ChatRequestTest {

    @Test
    public void testGetMaxTokens() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        Mockito.when(chatRequest.getMaxTokens()).thenReturn(800);
        int result = chatRequest.getMaxTokens();
        assertEquals(800, result);
    }

    @Test
    public void testSetMaxTokens() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        int maxTokens = 1000;
        chatRequest.setMaxTokens(maxTokens);
        Mockito.verify(chatRequest).setMaxTokens(maxTokens);
    }

    @Test
    public void testGetTemperature() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        Mockito.when(chatRequest.getTemperature()).thenReturn(0.75);
        double expectedTemperature = 0.75;
        double actualTemperature = chatRequest.getTemperature();
        assertEquals(expectedTemperature, actualTemperature, 0.0);
    }

    @Test
    public void testSetTemperature() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        double temperature = 0.5;
        chatRequest.setTemperature(temperature);
        Mockito.verify(chatRequest).setTemperature(temperature);
    }

    @Test
    public void testGetFrequencyPenalty() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        Mockito.when(chatRequest.getFrequencyPenalty()).thenReturn(0.0);
        double frequencyPenalty = chatRequest.getFrequencyPenalty();
        assertEquals(0.0, frequencyPenalty, 0.001);
    }

    @Test
    public void testSetFrequencyPenalty() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        double frequencyPenalty = 0.5;
        chatRequest.setFrequencyPenalty(frequencyPenalty);
        Mockito.verify(chatRequest).setFrequencyPenalty(frequencyPenalty);
    }

    @Test
    public void testGetPresencePenalty() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        Mockito.when(chatRequest.getPresencePenalty()).thenReturn(0.0);
        double result = chatRequest.getPresencePenalty();
        assertEquals(0.0, result, 0.0);
    }

    @Test
    public void testSetPresencePenalty() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        double presencePenalty = 0.5;
        chatRequest.setPresencePenalty(presencePenalty);
        Mockito.verify(chatRequest).setPresencePenalty(presencePenalty);
    }

    @Test
    public void testGetTopP() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        Mockito.when(chatRequest.getTopP()).thenReturn(0.95);
        double result = chatRequest.getTopP();
        assertEquals(0.95, result, 0.001);
    }

    @Test
    public void testSetTopP() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        double topP = 0.95;
        chatRequest.setTopP(topP);
        Mockito.verify(chatRequest).setTopP(topP);
    }

    @Test
    public void testGetStop() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        String expectedStop = "stop";
        Mockito.when(chatRequest.getStop()).thenReturn(expectedStop);
        String actualStop = chatRequest.getStop();
        Assert.assertEquals(expectedStop, actualStop);
    }

    @Test
    public void testSetStop() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        String stop = "stop";
        chatRequest.setStop(stop);
        Mockito.verify(chatRequest).setStop(stop);
    }

}
