package com.maya.assistant;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, messages;
    EditText input;
    ScrollView scroll;
    TextToSpeech tts;
    boolean ttsReady = false;
    boolean speakReplies = true;
    boolean waiting = false;
    JSONArray history = new JSONArray();
    final int VOICE_REQUEST = 101;
    final int MIC_PERMISSION = 102;
    final String PREFS = "maya_settings";
    final String KEY = "gemini_key";
    final String MODEL = "gemini-3.8-flash";

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true;
                int r = tts.setLanguage(new Locale("hi", "IN"));
                if (r == TextToSpeech.LANG_MISSING_DATA ||
                    r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }
            }
        });

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(14, 12, 14, 10);
        root.setBackgroundColor(Color.rgb(20, 16, 38));

        TextView title = new TextView(this);
        title.setText("🌸 MAYA AI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER);
        Button settings = new Button(this);
        settings.setText("API Key ⚙");
        top.addView(settings);
        Button voiceToggle = new Button(this);
        voiceToggle.setText("Voice: ON 🔊");
        top.addView(voiceToggle);
        root.addView(top);

        settings.setOnClickListener(v -> keyDialog());
        voiceToggle.setOnClickListener(v -> {
            speakReplies = !speakReplies;
            voiceToggle.setText(speakReplies ? "Voice: ON 🔊" : "Voice: OFF 🔇");
        });

        scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(3, 12, 3, 12);
        scroll.addView(messages);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        input = new EditText(this);
        input.setSingleLine(false);
        input.setMaxLines(3);
        input.setHint("Maya se baat karo...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.LTGRAY);
        bar.addView(input, new LinearLayout.LayoutParams(0, -2, 1));

        Button mic = new Button(this);
        mic.setText("🎙");
        bar.addView(mic);
        Button send = new Button(this);
        send.setText("Send");
        bar.addView(send);
        root.addView(bar);

        mic.setOnClickListener(v -> startVoice());
        send.setOnClickListener(v -> sendMessage());

        setContentView(root);
        addMessage("Maya", "Hey! 🌸 Main Maya hoon. API Key set karke mujhse Hinglish mein baat karo.");
    }

    void keyDialog() {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setHint("Gemini API key");
        field.setInputType(129);
        field.setText(getPreferences(0).getString(KEY, ""));

        new AlertDialog.Builder(this)
            .setTitle("Maya AI Setup")
            .setMessage("API key isi phone par save hogi. Use GitHub par share mat karna.")
            .setView(field)
            .setPositiveButton("Save", (d, w) -> {
                String k = field.getText().toString().trim();
                if (!k.isEmpty()) {
                    getPreferences(0).edit().putString(KEY, k).apply();
                    addMessage("Maya", "API key phone par save ho gayi. Ab chat try karo 🌸");
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    void startVoice() {
        if (checkSelfPermission("android.permission.RECORD_AUDIO")
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{"android.permission.RECORD_AUDIO"}, MIC_PERMISSION);
            return;
        }
        openVoiceRecognizer();
    }

    void openVoiceRecognizer() {
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Maya se baat karo");
            startActivityForResult(i, VOICE_REQUEST);
        } catch (Exception e) {
            addMessage("Maya", "Voice recognition is not available on this phone right now.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == MIC_PERMISSION && results.length > 0 &&
                results[0] == PackageManager.PERMISSION_GRANTED) {
            openVoiceRecognizer();
        } else if (requestCode == MIC_PERMISSION) {
            addMessage("Maya", "Voice use karne ke liye microphone permission allow karni hogi.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results =
                data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                input.setText(results.get(0));
                sendMessage();
            }
        }
    }

    void sendMessage() {
        if (waiting) return;
        String msg = input.getText().toString().trim();
        if (msg.isEmpty()) return;

        String apiKey = getPreferences(0).getString(KEY, "");
        if (apiKey.isEmpty()) {
            addMessage("Maya", "Pehle upar API Key button se apni Gemini key save karo.");
            keyDialog();
            return;
        }

        input.setText("");
        addMessage("Tum", msg);
        try {
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("parts", new JSONArray().put(new JSONObject().put("text", msg)));
            history.put(user);
        } catch (Exception ignored) {}

        waiting = true;
        addMessage("Maya", "Soch rahi hoon... 🌸");

        new Thread(() -> {
            String answer;
            try {
                URL url = new URL(
                    "https" + "://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(20000);
                c.setReadTimeout(45000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("x-goog-api-key", apiKey);

                JSONObject body = new JSONObject();
                JSONObject instruction = new JSONObject();
                instruction.put("parts", new JSONArray().put(new JSONObject().put(
                    "text", "You are Maya, a natural, friendly AI companion. " +
                    "Speak conversational Hinglish mixing Hindi and English naturally. " +
                    "Adapt your tone to the user's mood: be cheerful when appropriate, " +
                    "calm and caring when they are upset, playful when suitable, and " +
                    "serious when needed. Never claim to be a real human. Be concise, " +
                    "warm, respectful and helpful. Do not pretend to have memories " +
                    "or phone access you do not have."
                )));
                body.put("systemInstruction", instruction);
                body.put("contents", history);
                body.put("generationConfig", new JSONObject()
                    .put("temperature", 0.8)
                    .put("maxOutputTokens", 700));

                try (OutputStream out = c.getOutputStream()) {
                    out.write(body.toString().getBytes("UTF-8"));
                }

                int code = c.getResponseCode();
                InputStream stream = code >= 200 && code < 300
                    ? c.getInputStream() : c.getErrorStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int n;
                while ((n = stream.read(chunk)) != -1) buffer.write(chunk, 0, n);
                stream.close();

                JSONObject result = new JSONObject(buffer.toString("UTF-8"));
                if (code < 200 || code >= 300) {
                    answer = "Gemini API error (" + code + "). API key, model access, quota ya internet check karo.";
                } else {
                    answer = result.getJSONArray("candidates")
                        .getJSONObject(0).getJSONObject("content")
                        .getJSONArray("parts").getJSONObject(0).getString("text");

                    JSONObject modelReply = new JSONObject();
                    modelReply.put("role", "model");
                    modelReply.put("parts", new JSONArray().put(
                        new JSONObject().put("text", answer)));
                    history.put(modelReply);
                    while (history.length() > 12) {
                        JSONArray shorter = new JSONArray();
                        for (int j = 2; j < history.length(); j++) {
                            shorter.put(history.get(j));
                        }
                        history = shorter;
                    }
                }
                c.disconnect();
            } catch (Exception e) {
                answer = "Connection nahi ho paaya. Internet, API key aur model access check karo.";
            }

            final String reply = answer;
            runOnUiThread(() -> {
                waiting = false;
                int count = messages.getChildCount();
                if (count > 0) messages.removeViewAt(count - 1);
                addMessage("Maya", reply);
                if (speakReplies && ttsReady) {
                    tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "maya_reply");
                }
            });
        }).start();
    }

    void addMessage(String who, String text) {
        TextView bubble = new TextView(this);
        bubble.setText(who + "\n" + text);
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(16);
        bubble.setPadding(14, 12, 14, 12);
        bubble.setBackgroundColor(who.equals("Maya")
            ? Color.rgb(74, 48, 100) : Color.rgb(42, 55, 85));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 7, 0, 7);
        messages.addView(bubble, p);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
