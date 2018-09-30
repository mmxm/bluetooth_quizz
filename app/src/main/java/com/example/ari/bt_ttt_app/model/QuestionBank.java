package com.example.ari.bt_ttt_app.model;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Created by Florian PONROY - OpenClassrooms on 09/08/17.
 */

public class QuestionBank {
    private List<Question> mQuestionList;
    private int mNextQuestionIndex;

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

    /*public void generateQuestion(){
        try {
            byte[] encoded = Files.readAllBytes(Paths.get("questions.json"));
            String file = new String(encoded, StandardCharsets.UTF_8);
        }catch (Exception e){

        }
    }*/

}
