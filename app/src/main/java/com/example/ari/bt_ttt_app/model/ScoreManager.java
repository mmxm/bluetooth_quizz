package com.example.ari.bt_ttt_app.model;

public class ScoreManager {
    private int myScore;

    private int opScore;

    public void Game(){
        setMyScore(0);
        setOpScore(0);

    }

    public int addScore(boolean isMyAnswerGood, boolean isOpAnswerGood){
        if (isMyAnswerGood && isOpAnswerGood){
            this.setOpScore(this.getOpScore()+1);
            this.setMyScore(this.getMyScore()+1);
            return 1;
        }else if(isMyAnswerGood){
            this.setMyScore(this.getMyScore()+2);
            return 2;
        } else if(isOpAnswerGood){
            this.setOpScore(this.getOpScore()+2);
            return 2;
        }else{
            return 0;
        }
    }

    public static String printResult(boolean isMyAnswerGood, int result){
        if(result == 0){
            return "Everybody is wrong !";
        } else if(result == 1){
            return "Everybody is right !";
        } else if(isMyAnswerGood){
            return "You win !";
        }else{
            return "You lose !";
        }
    }

    public void updateScore(int myScore_, int opScore_){
        this.myScore = myScore_;
        this.opScore = opScore_;
    }

    public int getMyScore() {
        return myScore;
    }

    private void setMyScore(int myScore) {
        this.myScore = myScore;
    }


    public int getOpScore() {
        return opScore;
    }

    private void setOpScore(int opScore) {
        this.opScore = opScore;
    }

}
