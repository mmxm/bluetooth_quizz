package com.example.ari.bt_ttt_app.controller;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import com.example.ari.bt_ttt_app.R;
import com.example.ari.bt_ttt_app.model.Question;
import com.example.ari.bt_ttt_app.model.QuestionBank;
import com.example.ari.bt_ttt_app.model.ScoreManager;

import org.json.JSONException;
import org.json.JSONObject;

public class BT_TTT extends AppCompatActivity implements View.OnClickListener{

    public static Activity act_2p;
    private TextView mQuestionTextView;
    private Button mAnswerButton1;
    private Button mAnswerButton2;
    private Button mAnswerButton3;
    private Button mAnswerButton4;

    private QuestionBank mQuestionBank;
    private Question mCurrentQuestion;

    private int myScore=0, opScore=0;
    private int mNumberOfQuestions;

    public static final String BUNDLE_EXTRA_SCORE = "BUNDLE_EXTRA_SCORE";
    public static final String BUNDLE_STATE_SCORE = "currentScore";
    public static final String BUNDLE_STATE_QUESTION = "currentQuestion";

    private boolean mEnableTouchEvents;

    boolean oncewin = false;
    boolean oncedrawen = false;
    boolean oppontentRematch = false;
    boolean playerRematch = false;
    protected boolean myAnswerGiven = false, opAnswerGiven = false;
    protected boolean waitResult = false, opReady = false, myReady = false;
    protected boolean isMyAnswerGood, isOpAnswerGood;
    boolean myRestart = false, myRestartGiven = false, opRestartGiven = false, opRestart = false;
    boolean restart = false;
    protected String lastResult;
    private ScoreManager scoreManager;


    int cnt = 0;
    int[] time = {1000};
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;
    private boolean isMaster;

    String TAG = "BT_CanvasView";
    String p1Name = "";
    String p2Name = "";
    public static int[][] a = new int[3][3];
    public static int turn = 0;

    public static ConnectedThread connectedThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        act_2p = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_ttt);mQuestionBank = this.generateQuestions();

        if (savedInstanceState != null) {
            //mScore = savedInstanceState.getInt(BUNDLE_STATE_SCORE);
            mNumberOfQuestions = savedInstanceState.getInt(BUNDLE_STATE_QUESTION);
        } else {
            //mScore = 0;
            mNumberOfQuestions = 3;
        }



        // Wire widgets
        mQuestionTextView = (TextView) findViewById(R.id.activity_game_question_text);
        mAnswerButton1 = (Button) findViewById(R.id.activity_game_answer1_btn);
        mAnswerButton2 = (Button) findViewById(R.id.activity_game_answer2_btn);
        mAnswerButton3 = (Button) findViewById(R.id.activity_game_answer3_btn);
        mAnswerButton4 = (Button) findViewById(R.id.activity_game_answer4_btn);

        // Use the tag property to 'name' the buttons
        mAnswerButton1.setTag(0);
        mAnswerButton2.setTag(1);
        mAnswerButton3.setTag(2);
        mAnswerButton4.setTag(3);

        mAnswerButton1.setOnClickListener(this);
        mAnswerButton2.setOnClickListener(this);
        mAnswerButton3.setOnClickListener(this);
        mAnswerButton4.setOnClickListener(this);



        bluetoothDevice = BT_TTT_names.mBluetoothDevice;
        bluetoothSocket = BT_TTT_names.mBluetoothSocket;

        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();



        Log.d(TAG, "isMaster" + String.valueOf(BT_TTT_names.isMaster));
        this.isMaster = BT_TTT_names.isMaster;

        if(this.isMaster){
            myReady = true;
            scoreManager = new ScoreManager();
            //mCurrentQuestion = mQuestionBank.getQuestion();
            //this.displayQuestion(mCurrentQuestion);
            //sendQuestion(mCurrentQuestion);
        }else {
            sendReady();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(BUNDLE_STATE_SCORE, myScore);
        outState.putInt(BUNDLE_STATE_QUESTION, mNumberOfQuestions);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        int responseIndex = (int) v.getTag();

        if(!myAnswerGiven) {
            this.myAnswerGiven = true;
            if (responseIndex == mCurrentQuestion.getAnswerIndex()) {
                // Good answer
                Toast.makeText(this, "Correct", Toast.LENGTH_SHORT).show();
                this.isMyAnswerGood = true;
                //mScore++;
            } else {
                // Wrong answer
                this.isMyAnswerGood = false;
                Toast.makeText(this, "Wrong answer!", Toast.LENGTH_SHORT).show();
            }

            mEnableTouchEvents = false;

            if(opAnswerGiven && isMaster) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        printScore();
                    }
                }, 2000); // LENGTH_SHORT is usually 2 second long
            }
            if (!isMaster){
                waitResult = true;
                sendAnswer(isMyAnswerGood);
            }
        }
    }

    //only called by Master
    public void endQuestion(){
        Log.i(TAG, "Ending question");

        // If this is the last question, ends the game.
        // Else, display the next question
        if (mNumberOfQuestions <= 0) { //TODO: Check that
            // End the game
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            AskNewGameDialogFragment f = AskNewGameDialogFragment.newInstance("TODO");
            f.show(ft, "dialog");
        }
        if (isMaster) {
            if(opReady && myReady){
                nextQuestion();
            }
        } else {
            sendReady();
        }

    }

    //Only called by master
    private void printScore(){
        int res = scoreManager.addScore(this.isMyAnswerGood, this.isOpAnswerGood);
        myScore = scoreManager.getMyScore();
        opScore = scoreManager.getOpScore();
        opReady = false;
        myReady = false;
        sendResult(res);
        lastResult = ScoreManager.printResult(this.isMyAnswerGood, res);
        createScoreDialog(lastResult, myScore, opScore);
    }

    private void createScoreDialog(String title, int myScore, int opScore){
        Log.i(TAG, "Printing result Toast");
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        //Check there is no open dialog
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        PrintScoreDialogFragment f = PrintScoreDialogFragment.newInstance(title, myScore, opScore);
        f.show(ft, "dialog");


        //Toast.makeText(BT_TTT.act_2p, lastResult, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Printed");
    }


    private void nextQuestion(){
        mCurrentQuestion = mQuestionBank.getQuestion();
        displayQuestion(mCurrentQuestion);
        sendQuestion(mCurrentQuestion);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mEnableTouchEvents && super.dispatchTouchEvent(ev);
    }



    protected void displayQuestion(final Question question) {
        mNumberOfQuestions--;
        mEnableTouchEvents = true;
        myAnswerGiven = false;
        opAnswerGiven = false;
        mQuestionTextView.setText(question.getQuestion());
        mAnswerButton1.setText(question.getChoiceList().get(0));
        mAnswerButton2.setText(question.getChoiceList().get(1));
        mAnswerButton3.setText(question.getChoiceList().get(2));
        mAnswerButton4.setText(question.getChoiceList().get(3));
    }


    private void sendQuestion(Question question){
        if (connectedThread != null) {
            threadWrite(question.toJson().toString());
        }
    }

    private void sendAnswer(boolean isAnswerGood){
        if (connectedThread != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("Answer", isAnswerGood);
            }catch(JSONException e) {
                e.printStackTrace();
            }
            threadWrite(obj.toString());
        }
    }
    private void sendResult(int result){
        if (connectedThread != null) {
            JSONObject obj = new JSONObject();
            try {
                    obj.put("Result", result);
                    obj.put("masterScore", scoreManager.getMyScore());
                    obj.put("opScore", scoreManager.getOpScore());

            }catch(JSONException e) {
                e.printStackTrace();
            }
            threadWrite(obj.toString());
        }
    }
    private void sendReady(){
        if (connectedThread != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("Ready", "true");

            }catch(JSONException e) {
                e.printStackTrace();
            }
            threadWrite(obj.toString());
        }
    }

    //Only called by slave
    private void sendRestartAnswer(boolean answer){
        if (connectedThread != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("Restart", answer);

            }catch(JSONException e) {
                e.printStackTrace();
            }
            threadWrite(obj.toString());
        }
    }

    //Only called by master
    private void sendRestartGame(boolean restart){
        if (connectedThread != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("Restart", restart);

            }catch(JSONException e) {
                e.printStackTrace();
            }
            threadWrite(obj.toString());
        }
    }

    private void threadWrite(String msg){
        byte[] ByteArray = msg.getBytes();
        connectedThread.write(ByteArray);
    }

    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private int cnt = 0;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "Create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Obtain BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "tmp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            if (cnt == 0) {
                // Say hello
                try {
                    byte[] ByteArray = MainActivity.MyName.getBytes();
                    connectedThread.write(ByteArray);
                    cnt++;
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }
            }
            byte[] buffer = new byte[1024];
            int bytes;
            // Listen InputStream while connected
            while (true) {
                try {
                    Log.i(TAG, "BEGIN Listening");

                    // Read from stream
                    String readMessage;
                    bytes = mmInStream.read(buffer);
                    readMessage = new String(buffer, 0, bytes);
                    Log.i(TAG, "Listening : " + readMessage);


                    if (isMaster){
                        // Message for Master
                        if (readMessage.contains("Ready") && !opReady){
                            opReady = true;
                            BT_TTT.act_2p.runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            endQuestion();
                                        }
                                    }
                            );
                        } else if( readMessage.contains("Answer") && !opAnswerGiven){
                            opAnswerGiven = true;
                            JSONObject jsonObj = new JSONObject(readMessage);
                            isOpAnswerGood = jsonObj.getBoolean("Answer");
                            if (opAnswerGiven && myAnswerGiven){
                                printScore();
                            }
                        } else if(readMessage.contains("Restart") && !opRestartGiven){
                            opRestartGiven = true;
                            JSONObject jsonObj = new JSONObject(readMessage);
                            opRestart = jsonObj.getBoolean("Restart");
                            chooseToEndGame();
                        }
                    }else {
                        // Message for slave
                        if (readMessage.contains("mQuestion")) {
                            // new question has been received by slave
                            JSONObject jsonObj = new JSONObject(readMessage);
                            Question q = new Question(jsonObj);
                            Log.d(TAG, "Parsed object : " + q.toString());
                            mCurrentQuestion = q;
                            BT_TTT.act_2p.runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            displayQuestion(mCurrentQuestion);
                                        }
                                    }
                            );
                        } else if (readMessage.contains("Result") && waitResult) {
                            waitResult = false;
                            JSONObject jsonObj = new JSONObject(readMessage);
                            lastResult = ScoreManager.printResult(isMyAnswerGood, jsonObj.getInt("Result"));
                            createScoreDialog(lastResult,jsonObj.getInt("opScore"),jsonObj.getInt("masterScore") );
                        } else if (readMessage.contains("Restart")) {
                            JSONObject jsonObj = new JSONObject(readMessage);
                            if(jsonObj.getBoolean("Restart")){
                                restartAct();
                            }else{
                                endAct();
                            }
                        }
                    }


                    // Message for both Master or slave
                    /*if (readMessage.equals("REMATCH")) {
                        Log.d(TAG, "rematch");
                        oppontentRematch = true;
                        //init();
                        //postInvalidate();
                        BT_TTT.act_2p.runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(BT_TTT.act_2p, MainActivity.MyName + " vs " + MainActivity.Opponent, Toast.LENGTH_SHORT).show();
                                    }
                                }
                        );
                    } else if (readMessage.equals("END")) {
                        break;
                    } else {
                        try {
                            Log.i(TAG, "Hello");
                            MainActivity.Opponent = readMessage;
                            Log.i(TAG, MainActivity.MyName + " vs " + MainActivity.Opponent);
                            BT_TTT.act_2p.runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(BT_TTT.act_2p, MainActivity.MyName + " vs " + MainActivity.Opponent, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                            );
                        } catch (Exception e) {
                            Log.d(TAG, e.getMessage());
                        }
                    } */


                } catch (Exception e) {
                    //Log.e(TAG, "disconnected", e);
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            if (!oncewin && !oncedrawen) {
                try {
                    Log.d(TAG, "Writing ");
                    mmOutStream.write(buffer);
                } catch (IOException e) {
                    Log.e(TAG, "Exception during write", e);
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private QuestionBank generateQuestions() {
        Question question1 = new Question("What is the name of the current french president?",
                Arrays.asList("François Hollande", "Emmanuel Macron", "Jacques Chirac", "François Mitterand"),
                1);

        Question question2 = new Question("How many countries are there in the European Union?",
                Arrays.asList("15", "24", "28", "32"),
                2);

        Question question3 = new Question("Who is the creator of the Android operating system?",
                Arrays.asList("Andy Rubin", "Steve Wozniak", "Jake Wharton", "Paul Smith"),
                0);

        Question question4 = new Question("When did the first man land on the moon?",
                Arrays.asList("1958", "1962", "1967", "1969"),
                3);

        Question question5 = new Question("What is the capital of Romania?",
                Arrays.asList("Bucarest", "Warsaw", "Budapest", "Berlin"),
                0);

        Question question6 = new Question("Who did the Mona Lisa paint?",
                Arrays.asList("Michelangelo", "Leonardo Da Vinci", "Raphael", "Carravagio"),
                1);

        Question question7 = new Question("In which city is the composer Frédéric Chopin buried?",
                Arrays.asList("Strasbourg", "Warsaw", "Paris", "Moscow"),
                2);

        Question question8 = new Question("What is the country top-level domain of Belgium?",
                Arrays.asList(".bg", ".bm", ".bl", ".be"),
                3);

        Question question9 = new Question("What is the house number of The Simpsons?",
                Arrays.asList("42", "101", "666", "742"),
                3);

        return new QuestionBank(Arrays.asList(question1,
                question2,
                question3,
                question4,
                question5,
                question6,
                question7,
                question8,
                question9));
    }



    @Override
    protected void onStart() {
        super.onStart();

        System.out.println("GameActivity::onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();

        System.out.println("GameActivity::onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();

        System.out.println("GameActivity::onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();

        System.out.println("GameActivity::onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        System.out.println("GameActivity::onDestroy()");
    }

    public static class PrintScoreDialogFragment extends DialogFragment {

        public static PrintScoreDialogFragment newInstance(String res, int myScore, int opScore) {
            PrintScoreDialogFragment f = new PrintScoreDialogFragment();

            Bundle args = new Bundle();
            args.putString("res", res);
            args.putInt("myScore", myScore);
            args.putInt("opScore", opScore);
            f.setArguments(args);
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            super.onCreate(savedInstanceState);
            String res = getArguments().getString("res");
            int myScore = getArguments().getInt("myScore");
            int opScore = getArguments().getInt("opScore");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(res)
                    .setMessage("Your score is " + myScore + "\n"+"Your opponent score is " + opScore);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((BT_TTT)getActivity()).myReady = true;
                    ((BT_TTT)getActivity()).endQuestion();
                }
            })
                    .setCancelable(false);
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    public static class AskNewGameDialogFragment extends DialogFragment {

        public static AskNewGameDialogFragment newInstance(String res) {
            AskNewGameDialogFragment f = new AskNewGameDialogFragment();

            Bundle args = new Bundle();
            args.putString("res", res);
            f.setArguments(args);
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            super.onCreate(savedInstanceState);
            String res = getArguments().getString("res");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(res)
                    .setMessage("Do you want to start a new game ?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((BT_TTT)getActivity()).endGame(true);
                }
            }).setCancelable(false);
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((BT_TTT)getActivity()).endGame(false);
                }
            })
                    .setCancelable(false);
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    private void endGame(boolean answer) {
        if (!isMaster){
            sendRestartAnswer(answer);
            if(!answer){
                endAct();
            }
        }else{
            this.myRestart = answer;
            this.myRestartGiven = true;
            chooseToEndGame();
        }

    }

    private void endAct(){
        Intent intent = new Intent();
        intent.putExtra(BUNDLE_EXTRA_SCORE, myScore);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void restartAct(){
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    //Only called by Master
    private void chooseToEndGame(){
        if (opRestartGiven && myRestartGiven) {
            if (opRestart && myRestart) {
                sendRestartGame(true);
                restartAct();
            } else if (!opRestart) {
                endAct();
            } else {
                sendRestartGame(false);
            }
        }
    }

}

