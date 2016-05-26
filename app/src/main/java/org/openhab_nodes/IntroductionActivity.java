package org.openhab_nodes;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import org.openhab_nodes.bootstrap.R;

/**
 * Shows an activity with a text, a help button and a next button.
 */
public class IntroductionActivity extends AppCompatActivity {
    TextView textView;
    Html.ImageGetter imageProvider;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.introduction_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        textView = (TextView) findViewById(R.id.text);
        assert textView != null;

        imageProvider = new BSImageGetter(this, textView);

        Button button = (Button) findViewById(R.id.btnNext);
        assert button != null;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(IntroductionActivity.this, OpenHABActivity.class);
                startActivity(intent);
            }
        });

        textView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                textView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                textView.setText(Html.fromHtml(getString(R.string.introduction_text), imageProvider, null));
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO repair help url
                Intent i = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://github.com/Openhab-Nodes/android/wiki/first_start"));
                startActivity(i);
            }
        });
    }
}
