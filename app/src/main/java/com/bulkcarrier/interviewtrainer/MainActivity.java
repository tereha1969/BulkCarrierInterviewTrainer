package com.bulkcarrier.interviewtrainer;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private ArrayList<Question> allQuestions;
    private LinearLayout listLayout;
    private Spinner sectionSpinner;
    private EditText searchBox;
    private CheckBox readRuCheck;
    private String selectedSection = "All sections";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        allQuestions = QuestionRepository.load(this);
        requestNotificationPermission();
        buildUi();
        renderList();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(244,247,251));

        TextView title = new TextView(this);
        title.setText("Bulk Carrier Interview Trainer");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setPadding(24, 24, 24, 8);
        title.setBackgroundColor(Color.rgb(11,61,98));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Native APK: plays with screen off using foreground service");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(13);
        subtitle.setPadding(24, 0, 24, 20);
        subtitle.setBackgroundColor(Color.rgb(11,61,98));
        root.addView(subtitle);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(16, 16, 16, 8);
        controls.setBackgroundColor(Color.WHITE);

        searchBox = new EditText(this);
        searchBox.setHint("Search: SOLAS, liquefaction, PSC...");
        controls.addView(searchBox);

        sectionSpinner = new Spinner(this);
        ArrayList<String> sections = new ArrayList<>();
        sections.add("All sections");
        for (Question q : allQuestions) if (!sections.contains(q.section)) sections.add(q.section);
        sectionSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sections));
        controls.addView(sectionSpinner);

        readRuCheck = new CheckBox(this);
        readRuCheck.setText("Read Russian explanation");
        readRuCheck.setChecked(true);
        controls.addView(readRuCheck);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button playAll = new Button(this);
        playAll.setText("▶ Play all");
        playAll.setOnClickListener(v -> startAudio(-1));
        buttons.addView(playAll, new LinearLayout.LayoutParams(0, -2, 1));

        Button pause = new Button(this);
        pause.setText("⏸ Pause");
        pause.setOnClickListener(v -> sendAction(AudioPlayerService.ACTION_PAUSE));
        buttons.addView(pause, new LinearLayout.LayoutParams(0, -2, 1));

        Button resume = new Button(this);
        resume.setText("▶ Resume");
        resume.setOnClickListener(v -> sendAction(AudioPlayerService.ACTION_RESUME));
        buttons.addView(resume, new LinearLayout.LayoutParams(0, -2, 1));

        Button stop = new Button(this);
        stop.setText("⏹ Stop");
        stop.setOnClickListener(v -> sendAction(AudioPlayerService.ACTION_STOP));
        buttons.addView(stop, new LinearLayout.LayoutParams(0, -2, 1));

        controls.addView(buttons);
        root.addView(controls);

        sectionSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                selectedSection = parent.getItemAtPosition(pos).toString();
                renderList();
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { renderList(); }
            public void afterTextChanged(android.text.Editable e) {}
        });

        ScrollView scroll = new ScrollView(this);
        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setPadding(12, 12, 12, 24);
        scroll.addView(listLayout);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
    }

    private ArrayList<Question> filtered() {
        ArrayList<Question> result = new ArrayList<>();
        String term = searchBox == null ? "" : searchBox.getText().toString().toLowerCase().trim();
        for (Question q : allQuestions) {
            boolean okSection = selectedSection.equals("All sections") || q.section.equals(selectedSection);
            String hay = (q.section + " " + q.q + " " + q.a + " " + q.ru).toLowerCase();
            boolean okTerm = term.isEmpty() || hay.contains(term);
            if (okSection && okTerm) result.add(q);
        }
        return result;
    }

    private void renderList() {
        if (listLayout == null) return;
        listLayout.removeAllViews();
        ArrayList<Question> qs = filtered();

        TextView count = new TextView(this);
        count.setText(qs.size() + " / " + allQuestions.size() + " questions");
        count.setPadding(8, 8, 8, 8);
        listLayout.addView(count);

        for (Question q : qs) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(20, 20, 20, 20);
            card.setBackgroundColor(Color.WHITE);

            TextView badge = new TextView(this);
            badge.setText(q.id + ". " + q.section);
            badge.setTextColor(Color.rgb(11,61,98));
            card.addView(badge);

            TextView question = new TextView(this);
            question.setText("Question: " + q.q);
            question.setTextSize(18);
            question.setTextColor(Color.rgb(16,32,48));
            question.setPadding(0, 8, 0, 8);
            card.addView(question);

            TextView answer = new TextView(this);
            answer.setText("Answer: " + q.a);
            answer.setTextColor(Color.rgb(20,50,74));
            answer.setPadding(0, 6, 0, 6);
            card.addView(answer);

            TextView ru = new TextView(this);
            ru.setText("Объяснение: " + q.ru);
            ru.setTextColor(Color.rgb(51,51,51));
            ru.setPadding(0, 6, 0, 6);
            card.addView(ru);

            Button playFromHere = new Button(this);
            playFromHere.setText("▶ Play from here");
            playFromHere.setOnClickListener(v -> startAudio(q.id));
            card.addView(playFromHere);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, 16);
            listLayout.addView(card, lp);
        }
    }

    private void startAudio(int startId) {
        Intent i = new Intent(this, AudioPlayerService.class);
        i.setAction(startId > 0 ? AudioPlayerService.ACTION_PLAY_FROM : AudioPlayerService.ACTION_PLAY_ALL);
        i.putExtra(AudioPlayerService.EXTRA_START_ID, startId);
        i.putExtra(AudioPlayerService.EXTRA_SECTION, selectedSection);
        i.putExtra(AudioPlayerService.EXTRA_READ_RU, readRuCheck.isChecked());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
        else startService(i);
    }

    private void sendAction(String action) {
        Intent i = new Intent(this, AudioPlayerService.class);
        i.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
        else startService(i);
    }
}
