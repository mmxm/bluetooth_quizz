package com.example.ari.bt_ttt_app.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Florian PONROY - OpenClassrooms on 09/08/17.
 */

public class Question {

    private String mQuestion;
    private List<String> mChoiceList;
    private int mAnswerIndex;

    public Question(String question, List<String> choiceList, int answerIndex) {
        this.setQuestion(question);
        this.setChoiceList(choiceList);
        this.setAnswerIndex(answerIndex);
    }
    public Question(JSONObject question){
        try {
            this.setQuestion(question.getString("mQuestion"));
            JSONArray choiceList_json = question.getJSONArray("mChoiceList");
            List<String> choiceList  = new ArrayList<>();
            Log.d("Test", String.valueOf(choiceList_json.length()));
            if (choiceList_json != null) {
                for (int i=0;i<choiceList_json.length();i++){
                    choiceList.add(choiceList_json.getString(i));
                }
            }
            this.setChoiceList(choiceList);
            this.setAnswerIndex(question.getInt("mAnswerIndex"));
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getQuestion() {
        return mQuestion;
    }

    public void setQuestion(String question) {
        mQuestion = question;
    }

    public List<String> getChoiceList() {
        return mChoiceList;
    }

    public void setChoiceList(List<String> choiceList) {
        if (choiceList == null) {
            throw new IllegalArgumentException("Array cannot be null");
        }

        mChoiceList = choiceList;
    }

    public int getAnswerIndex() {
        return mAnswerIndex;
    }

    public void setAnswerIndex(int answerIndex) {
        if (answerIndex < 0 || answerIndex >= mChoiceList.size()) {
            throw new IllegalArgumentException("Answer index is out of bound");
        }

        mAnswerIndex = answerIndex;
    }

    @Override
    public String toString() {
        return "Question{" +
                "mQuestion='" + mQuestion + '\'' +
                ", mChoiceList=" + mChoiceList +
                ", mAnswerIndex=" + mAnswerIndex +
                '}';
    }

    public JSONObject toJson() {

        JSONObject obj = new JSONObject();
        try {

            obj.put("mQuestion", mQuestion);
            JSONArray choiceList = new JSONArray(mChoiceList);
            obj.put("mChoiceList", choiceList);
            obj.put("mAnswerIndex", mAnswerIndex);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //String jsonStr = obj.toString();
        return obj;
    }
}
