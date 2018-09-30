package com.example.ari.bt_ttt_app.model;

import org.json.JSONObject;

public abstract class Step {

    private boolean isStarted;
    protected int numberPlayer;
    protected int playerIndex;
    private boolean givenAnswer[];

    public boolean getIsStarted() {
        return isStarted;
    }

    public void setIsStarted(boolean started) {
        isStarted = started;
    }

    public Step(int playerIndex, int numberPlayer) {
        this.playerIndex = playerIndex;
        if(playerIndex == 0) {
            givenAnswer = new boolean[numberPlayer];
            this.numberPlayer = numberPlayer;
            resetAnswer();
        }
        isStarted = false;
        this.numberPlayer = numberPlayer;
    }


    private void resetAnswer(){
        for (int i = 0; i < numberPlayer; i++) {
            givenAnswer[i] = false;
        }
    }

    public void startStep(Step.StepEvent se){
        isStarted = true;
        if(playerIndex == 0) {
            resetAnswer();
            onStartMaster(se);
        }
        else
            onStartPlayer(se);
    }

    public void newEvent(Step.StepEvent se){
        if(playerIndex == 0) {
            givenAnswer[se.getPlayerIndex()] = true;
            onEventMaster(se);
            if (checkAllEventOccured()) {
                onAllEventOccured();
            }
        } else {
            onEventPlayer(se);
        }

    }
    private boolean checkAllEventOccured(){
        for(int i = 0; i<numberPlayer; i++){
            if(!givenAnswer[i])
                return false;
        }
        return true;
    }

    protected abstract void onEventMaster(Step.StepEvent se);
    protected abstract void onEventPlayer(Step.StepEvent se);
    protected abstract void onAllEventOccured();
    protected abstract void onStartMaster(Step.StepEvent se);
    protected abstract void onStartPlayer(Step.StepEvent se);



    public void finalize(){

    }

    public static class StepEvent{
        private JSONObject obj;
        int pIndex;
        Question question;
        boolean answer;

        public StepEvent(){
            obj = null;
            pIndex = -1;
        }
        public StepEvent(JSONObject obj){
            this.obj = obj;
        }
        public StepEvent(Question q){
            this.question = q;
        }

        public StepEvent(int playerIndex, boolean answer){
            this.answer = answer;
            this.pIndex = playerIndex;
        }
        public StepEvent(int playerIndex){
            this.pIndex = playerIndex;
        }

        public JSONObject getObj() {
            return obj;
        }

        public void setObj(JSONObject obj) {
            this.obj = obj;
        }

        public int getPlayerIndex() {
            return pIndex;
        }

        public void setPlayerIndex(int playerIndex) {
            this.pIndex = playerIndex;
        }

        public boolean getAnswer() {
            return answer;
        }

        public void setAnswer(boolean answer) {
            this.answer = answer;
        }


        public Question getQuestion() {
            return question;
        }

        public void setQuestion(Question question) {
            this.question = question;
        }



    }

}
