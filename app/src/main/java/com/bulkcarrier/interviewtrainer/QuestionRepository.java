package com.bulkcarrier.interviewtrainer;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class QuestionRepository {
    public static ArrayList<Question> load(Context context) {
        ArrayList<Question> list = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("questions.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Question(
                        o.getInt("id"),
                        o.getString("section"),
                        o.getString("q"),
                        o.getString("a"),
                        o.getString("ru")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
