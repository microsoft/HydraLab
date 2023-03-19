package com.microsoft.hydralab.azure.tts;

import com.microsoft.cognitiveservices.speech.AudioDataStream;
import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

public class TestTTS {
    String speechSubscriptionKey = "d029eb2ca32145b5afbe434a33dfa28b";
    // Replace below with your own service region (e.g., "westus").
    String serviceRegion = "eastus";

    @Test
    public void testTts() throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream("script.txt")) {
            String text = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
            String who = "en-US-NancyNeural";
            String filePath = "previous.wav";
            speakAndSaveToFile(text, who, filePath);
        }
    }

    @Test
    public void testTtsSSML() throws IOException {
        int index = 1;
        processScripts(9);
    }

    private void processScripts(int... index) throws IOException {
        for (int i : index) {
            try (FileInputStream fileInputStream = new FileInputStream(String.format("scripts/script%d.xml", i))) {
                String text = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
                String filePath = String.format("scripts/output%d.wav", i);
                speakSSMLAndSaveToFile(text, filePath);
            }
        }
    }

    /**
     * @param text
     * @param who      Set the voice name, refer to https://aka.ms/speech/voices/neural for full list.
     * @param filePath
     */
    private void speakAndSaveToFile(String text, String who, String filePath) {
        try (SpeechConfig config = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion)) {

            config.setSpeechSynthesisVoiceName(who);
            try (SpeechSynthesizer synth = new SpeechSynthesizer(config)) {

                int exitCode = 1;

                // More about SSML: https://developers.google.com/assistant/df-asdk/ssml
                Future<SpeechSynthesisResult> task = synth.SpeakTextAsync(text);

                SpeechSynthesisResult result = task.get();
                AudioDataStream stream = AudioDataStream.fromResult(result);
                stream.saveToWavFile(filePath);

                if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                    System.out.println("Speech synthesized to speaker for text");
                    exitCode = 0;
                } else if (result.getReason() == ResultReason.Canceled) {
                    SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails
                            .fromResult(result);
                    System.out.println("CANCELED: Reason=" + cancellation.getReason());

                    if (cancellation.getReason() == CancellationReason.Error) {
                        System.out.println("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                        System.out.println("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
                        System.out.println("CANCELED: Did you update the subscription info?");
                    }
                }

                System.exit(exitCode);
            }
        } catch (Exception ex) {
            System.out.println("Unexpected exception: " + ex.getMessage());

            assert (false);
            System.exit(1);
        }
    }

    /**
     * @param ssml
     * @param filePath
     */
    private void speakSSMLAndSaveToFile(String ssml, String filePath) {
        try (SpeechConfig config = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion)) {

            // config.setSpeechSynthesisVoiceName(who);
            try (SpeechSynthesizer synth = new SpeechSynthesizer(config)) {

                // More about SSML: https://developers.google.com/assistant/df-asdk/ssml
                Future<SpeechSynthesisResult> task = synth.SpeakSsmlAsync(ssml);

                SpeechSynthesisResult result = task.get();
                AudioDataStream stream = AudioDataStream.fromResult(result);
                stream.saveToWavFile(filePath);

                if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                    System.out.println("Speech synthesized to speaker for text [" + ssml + "]");
                } else if (result.getReason() == ResultReason.Canceled) {
                    SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails
                            .fromResult(result);
                    System.out.println("CANCELED: Reason=" + cancellation.getReason());

                    if (cancellation.getReason() == CancellationReason.Error) {
                        System.out.println("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                        System.out.println("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
                        System.out.println("CANCELED: Did you update the subscription info?");
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Unexpected exception: " + ex.getMessage());
        }
    }
}
