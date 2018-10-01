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
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import com.example.ari.bt_ttt_app.model.Step;

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
    Question mCurrentQuestion;

    private int initialNumberQuestions = 3;
    private int mNumberOfQuestions;

    public static final String BUNDLE_EXTRA_SCORE = "BUNDLE_EXTRA_SCORE";
    public static final String BUNDLE_STATE_SCORE = "currentScore";
    public static final String BUNDLE_STATE_QUESTION = "currentQuestion";

    private boolean mEnableTouchEvents;


    private ScoreManager scoreManager;
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;
    boolean isMyAnswerGood;
    private boolean isMaster;
    int playerIndex;

    StepFactory stepFactory;
    Step initialStep, questionStep, scoreStep, restartStep;



    String TAG = "BT_CanvasView";
    public static ConnectedThread connectedThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        act_2p = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_ttt);
        mQuestionBank = this.generateQuestions();
        //mQuestionBank = new QuestionBank(this);

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



        //Log.d(TAG, "isMaster" + String.valueOf(BT_TTT_names.isMaster));
        this.isMaster = BT_TTT_names.isMaster;
        if(isMaster){
            playerIndex = 0;
        }else{
            playerIndex=1;
        }
        stepFactory = new StepFactory(playerIndex, 2);

        initialStep = stepFactory.createStep(TypeStep.INITALSTEP);
        questionStep = stepFactory.createStep(TypeStep.QUESTION);
        scoreStep = stepFactory.createStep(TypeStep.SCORE);
        restartStep = stepFactory.createStep(TypeStep.RESTART);

        initialStep.startStep(new Step.StepEvent());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(BUNDLE_STATE_QUESTION, mNumberOfQuestions);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        int responseIndex = (int) v.getTag();

        boolean isMyAnswerGood;
        if (responseIndex == mCurrentQuestion.getAnswerIndex()) {
            // Good answer
            Toast.makeText(this, "Correct", Toast.LENGTH_SHORT).show();
            isMyAnswerGood = true;
        } else {
            // Wrong answer
            isMyAnswerGood = false;
            Toast.makeText(this, "Wrong answer!", Toast.LENGTH_SHORT).show();
        }

        questionStep.newEvent(new Step.StepEvent(playerIndex, isMyAnswerGood));
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mEnableTouchEvents && super.dispatchTouchEvent(ev);
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

                    JSONObject jsonObj = new JSONObject(readMessage);
                    if (isMaster){
                        // Message for Master
                        int id = jsonObj.getInt("Id");
                        if (readMessage.contains("InitialReady")) {
                            initialStep.newEvent(new Step.StepEvent(id));
                        }else if (readMessage.contains("Ready") ){
                            scoreStep.newEvent(new Step.StepEvent(id));
                        } else if( readMessage.contains("Answer")){
                            boolean isOpAnswerGood = jsonObj.getBoolean("Answer");
                            questionStep.newEvent(new Step.StepEvent(id, isOpAnswerGood));
                        } else if(readMessage.contains("RestartAns")){
                            boolean ans = jsonObj.getBoolean("RestartAns");
                            restartStep.newEvent(new Step.StepEvent(id, ans));
                        }
                    }else {
                        // Message for slave
                        if (readMessage.contains("mQuestion")) {
                            Question q = new Question(jsonObj);
                            questionStep.startStep(new Step.StepEvent(q));
                        } else if (readMessage.contains("Result")) {
                            scoreStep.startStep(new Step.StepEvent(jsonObj));
                        } else if (readMessage.contains("RestartAsk")) {
                            restartStep.startStep(new Step.StepEvent());
                        }else if (readMessage.contains("Reset")) {
                            initialStep.startStep(new Step.StepEvent());
                        } else if (readMessage.contains("End")) {
                            endAct();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                Log.d(TAG, "Writing ");
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
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
                    int i = ((BT_TTT)getActivity()).playerIndex;
                    ((BT_TTT)getActivity()).scoreStep.newEvent(new Step.StepEvent(i));

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
                    int i = ((BT_TTT)getActivity()).playerIndex;
                    ((BT_TTT)getActivity()).restartStep.newEvent(new Step.StepEvent(i, true));
                }
            }).setCancelable(false);
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int i = ((BT_TTT)getActivity()).playerIndex;
                    ((BT_TTT)getActivity()).restartStep.newEvent(new Step.StepEvent(i, false));
                }
            })
                    .setCancelable(false);
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    private void displayQuestion(final Question question) {
        mNumberOfQuestions--;
        mEnableTouchEvents = true;
        mQuestionTextView.setText(question.getQuestion());
        mAnswerButton1.setText(question.getChoiceList().get(0));
        mAnswerButton2.setText(question.getChoiceList().get(1));
        mAnswerButton3.setText(question.getChoiceList().get(2));
        mAnswerButton4.setText(question.getChoiceList().get(3));
    }

    private void endAct(){
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }


    public JSONObject createGenericMessage(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("Id", this.playerIndex);

        }catch(JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
    public class InitialStep extends Step{

        //Wait for all players to be ready
        public InitialStep(int playerIndex, int numberPlayer){
            super(playerIndex, numberPlayer);
        };

        protected void onStartMaster(Step.StepEvent se){
            //say Master is ready
            scoreManager = new ScoreManager();
            scoreManager.reset();
            mNumberOfQuestions = initialNumberQuestions;

            newEvent(new StepEvent(0));
        }
        protected void onStartPlayer(Step.StepEvent se){
            //When the player initialize, send ready to master
            sendInitialReady();
        }

        @Override
        protected void onEventMaster(Step.StepEvent se) {
            //say master is ready or when a player says he is ready
        }

        protected void onEventPlayer(Step.StepEvent se){
            //nothing
        }

        @Override
        protected void onAllEventOccured() {
            //Create first question if everyone is ready
            questionStep.startStep(new StepEvent());
        }

        private void sendInitialReady(){
            if (connectedThread != null) {
                JSONObject obj = createGenericMessage();
                try {
                    obj.put("InitialReady", "true");

                }catch(JSONException e) {
                    e.printStackTrace();
                }
                threadWrite(obj.toString());
            }
        }
    }

    public class QuestionStep extends Step{

        boolean result[];
        //Wait for all players to be ready
        public QuestionStep(int playerIndex, int numberPlayer){
            super(playerIndex, numberPlayer);
            result = new boolean[numberPlayer];

        }

        protected void onStartMaster(Step.StepEvent se){
            //say Master is ready
            for(int i=0; i<numberPlayer; i++){
                result[i]=false;
            }
            nextQuestion();
        }

        protected void onStartPlayer(Step.StepEvent se){
            //When the question has been received by the player
            mCurrentQuestion = se.getQuestion();
            BT_TTT.act_2p.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            displayQuestion(mCurrentQuestion);
                        }
                    }
            );
        }

        protected void onEventMaster(Step.StepEvent se) {
            if (se.getPlayerIndex() ==0 )
                mEnableTouchEvents = false;
            handleAnswer(se);
            result[se.getPlayerIndex()] = se.getAnswer();

        }

        protected void onEventPlayer(Step.StepEvent se){
            //When the player gives an answer to the question
            mEnableTouchEvents = false;
            handleAnswer(se);
            sendAnswer(se.getAnswer());
        }

        protected void onAllEventOccured() {
            updateScore();
            scoreStep.startStep(new StepEvent());

        }

        private void sendAnswer(boolean isAnswerGood){
            if (connectedThread != null) {
                JSONObject obj = createGenericMessage();
                try {
                    obj.put("Answer", isAnswerGood);
                }catch(JSONException e) {
                    e.printStackTrace();
                }
                threadWrite(obj.toString());
            }
        }

        private void handleAnswer(StepEvent se){
            isMyAnswerGood = se.getAnswer();


        }

        private void nextQuestion(){
            mCurrentQuestion = mQuestionBank.getQuestion();
            BT_TTT.act_2p.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            displayQuestion(mCurrentQuestion);
                        }
                    }
            );
            sendQuestion(mCurrentQuestion);
        }

        private void sendQuestion(Question question){
            if (connectedThread != null) {
                threadWrite(question.toJson().toString());
            }
        }
        private void updateScore(){
            scoreManager.addScore(result[0], result[1]);
        }
    }

    public class ScoreStep extends Step{

        //Wait for all players to be ready
        public ScoreStep(int playerIndex, int numberPlayer){
            super(playerIndex, numberPlayer);
        };

        public void onStartMaster(Step.StepEvent se){
            int myScore = scoreManager.getMyScore();
            int opScore = scoreManager.getOpScore();
            int res = scoreManager.getLastResult();
            sendResult(res);
            createScoreDialog(scoreManager.getLastPrint(), myScore, opScore);
        }

        @Override
        protected void onStartPlayer(Step.StepEvent se){
            //display score dialog for player
            JSONObject jsonObj = se.getObj();
            try {
                String lastResult = ScoreManager.printResult(isMyAnswerGood, jsonObj.getInt("Result"));
                createScoreDialog(lastResult, jsonObj.getInt("opScore"), jsonObj.getInt("masterScore"));
            }catch (Exception e) { }
        }

        @Override
        protected void onEventMaster(Step.StepEvent se) {
            //Each time someone click OK and is now ready for next question
        }

        @Override
        protected void onEventPlayer(Step.StepEvent se) {
            //when the player click OK, send ready to master
            sendReady();
        }

        @Override
        protected void onAllEventOccured() {
            //Everyone is ready, choose between restart or next question
            if (mNumberOfQuestions <= 0) { //TODO: Check that
                // End the game
                restartStep.startStep(new StepEvent());
            }else{
                    questionStep.startStep(new StepEvent());
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
                JSONObject obj = createGenericMessage();
                try {
                    obj.put("Ready", "true");

                }catch(JSONException e) {
                    e.printStackTrace();
                }
                threadWrite(obj.toString());
            }
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
    }

    public class RestartStep extends Step{
        private boolean answer[];
        //Wait for all players to be ready
        public RestartStep(int playerIndex, int numberPlayer){
            super(playerIndex, numberPlayer);
            answer = new boolean[numberPlayer];
        };

        public void onStartMaster(Step.StepEvent se){
            sendAskRestart();
            showRestartFragment();
        }

        @Override
        protected void onStartPlayer(Step.StepEvent se){
            showRestartFragment();
        }

        @Override
        protected void onEventMaster(Step.StepEvent se) {
            //Each time someone click OK and is now ready for next question
            answer[se.getPlayerIndex()] = se.getAnswer();
            if (!se.getAnswer()){
                if(se.getPlayerIndex() == 0)
                    sendEnd();
                endAct();
            }
        }

        @Override
        protected void onEventPlayer(Step.StepEvent se) {
            //when the player click OK, send ready to master
            sendRestart(se.getAnswer());
            if (!se.getAnswer()){
                endAct();
            }
        }

        @Override
        protected void onAllEventOccured() {
            if (checkRestart()){
                initialStep.startStep(new StepEvent());
                sendReset();
            }else{
                sendEnd();
                endAct();
            }
        }

        private boolean checkRestart(){
            for(int i = 0; i<numberPlayer; i++) {
                if (!answer[i])
                    return false;
            }
            return true;

        }


        private void showRestartFragment(){
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            AskNewGameDialogFragment f = AskNewGameDialogFragment.newInstance("Restart");
            f.show(ft, "dialog");
        }
        private void sendAskRestart(){
            if (connectedThread != null) {
                JSONObject obj = createGenericMessage();
                try {
                    obj.put("RestartAsk", "true");

                }catch(JSONException e) {
                    e.printStackTrace();
                }
                threadWrite(obj.toString());
            }
        }
        private void sendRestart(boolean ans){
            if (connectedThread != null) {
                JSONObject obj = createGenericMessage();
                try {
                    obj.put("RestartAns", ans);

                }catch(JSONException e) {
                    e.printStackTrace();
                }
                threadWrite(obj.toString());
            }
        }
        private void sendReset(){
            if (connectedThread != null) {
                JSONObject obj = createGenericMessage();
                try {
                    obj.put("Reset", true);

                }catch(JSONException e) {
                    e.printStackTrace();
                }
                threadWrite(obj.toString());
            }
        }
        private void sendEnd(){
            if (connectedThread != null) {
                JSONObject obj = createGenericMessage();
                try {
                    obj.put("End", true);

                }catch(JSONException e) {
                    e.printStackTrace();
                }
                threadWrite(obj.toString());
            }
        }
    }


    public class StepFactory {

        private int playerIndex;
        private int playerNumber;

        public StepFactory(int playerIndex, int playerNumber){
            this.playerIndex = playerIndex;
            this.playerNumber = playerNumber;

        }

        public Step createStep(TypeStep stepType){
            switch (stepType){
                case INITALSTEP:
                    return new InitialStep(playerIndex, playerNumber);
                case QUESTION:
                    return new QuestionStep(playerIndex, playerNumber);
                case SCORE:
                    return new ScoreStep(playerIndex, playerNumber);
                case RESTART:
                    return new RestartStep(playerIndex, playerNumber);
            }
            return null;
        }

        public boolean getIsMaster() {
            return isMaster;
        }

        public void setIsMaster(boolean master) {
            isMaster = master;
        }

    }
    private enum TypeStep
    {
        INITALSTEP,
        QUESTION,
        SCORE,
        RESTART
    }

}

