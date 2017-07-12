package org.mkonchady.renuka;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    public TextView statusView;
    public TextView answerView1;
    public TextView answerView2;
    public TextView answerView3;
    public TextView answerView4;
    public PhotoView photoView;
    final String TAG = "Main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusView = (TextView) findViewById(R.id.status);
        answerView1 = (TextView) findViewById(R.id.answer1);
        answerView2 = (TextView) findViewById(R.id.answer2);
        answerView3 = (TextView) findViewById(R.id.answer3);
        answerView4 = (TextView) findViewById(R.id.answer4);
        photoView = (PhotoView) this.findViewById(R.id.photoView);
    }

}
