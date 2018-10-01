package com.example.ari.bt_ttt_app.model;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Florian PONROY - OpenClassrooms on 09/08/17.
 */

public class QuestionBank {
    private List<Question> mQuestionList;
    private int mNextQuestionIndex;
    private Context mContext;

    public QuestionBank(Context context){
        Log.d("TT", "Generating0 **********************************\n");
        mQuestionList = new ArrayList<>();
        this.generateQuestion();
        Collections.shuffle(mQuestionList);

        mNextQuestionIndex = 0;
        mContext = context;
    }

    public QuestionBank(List<Question> questionList) {
        mQuestionList = questionList;

        // Shuffle the question list
        Collections.shuffle(mQuestionList);

        mNextQuestionIndex = 0;
    }

    public Question getQuestion() {
        // Ensure we loop over the questions
        if (mNextQuestionIndex == mQuestionList.size()) {
            mNextQuestionIndex = 0;
        }

        // Please note the post-incrementation
        return mQuestionList.get(mNextQuestionIndex++);
    }

    public void generateQuestion(){
        try {

            Log.d("TT", "Generating **********************************\n");
            InputStream is = mContext.getAssets().open("questions.json");
            int size = is.available();

            byte[] buffer = new byte[size];
            is.read(buffer);

            is.close();
            String file = new String(buffer, StandardCharsets.UTF_8);
            Log.d("TT", "******************************\n" + file);
            JSONObject jsonObj = new JSONObject(file);
            JSONArray questions = jsonObj.getJSONArray("questions");

            JSONObject q;
            for (int i = 0; i < questions.length(); i++) {
                q= questions.getJSONObject(i);
                mQuestionList.add(new Question(q));
            }
        }catch (Exception e){

        }
    }

}
