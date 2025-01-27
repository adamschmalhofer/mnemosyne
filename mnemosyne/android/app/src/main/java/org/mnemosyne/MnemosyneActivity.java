
package org.mnemosyne;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MnemosyneActivity extends AppCompatActivity {

    public static final int SYNC_ACTIVITY_RESULT = 0;
    public static final int ACTIVATE_CARDS_ACTIVITY_RESULT = 1;
    public static final int EXPORT_DIRECTORY_RESULT = 2;
    public static final int IMPORT_DIRECTORY_RESULT = 3;

    private static final int REQUEST_WRITE_STORAGE = 112;

    String currentHtml;
    MediaPlayer mediaPlayer = null;
    ArrayList<Uri> soundFiles = new ArrayList<Uri>();
    ArrayList<Integer> starts = new ArrayList<Integer>();
    ArrayList<Integer> stops = new ArrayList<Integer>();
    int soundIndex = -1;

    Handler activityHandler = new Handler();
    MnemosyneThread mnemosyneThread;
    boolean paused = false;

    TextView questionLabel;
    TextView answerLabel;
    TextView statusbar;
    WebView question;
    WebView answer;
    Button showAnswerButton;
    Button button0;
    Button button1;
    Button button2;
    Button button3;
    Button button4;
    Button button5;

    GestureDetector gestureDetector;

    public int getStatusBarHeight() {
        int result = 50;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) myToolbar.getLayoutParams();
        params.setMargins(0, getStatusBarHeight(), 0, 0);
        myToolbar.setLayoutParams(params);

        questionLabel = (TextView) this.findViewById(R.id.questionLabel);
        question = (WebView) this.findViewById(R.id.question);
        answerLabel = (TextView) this.findViewById(R.id.answerLabel);
        answer = (WebView) this.findViewById(R.id.answer);
        showAnswerButton = (Button) this.findViewById(R.id.showAnswerButton);
        button0 = (Button) this.findViewById(R.id.button0);
        button1 = (Button) this.findViewById(R.id.button1);
        button2 = (Button) this.findViewById(R.id.button2);
        button3 = (Button) this.findViewById(R.id.button3);
        button4 = (Button) this.findViewById(R.id.button4);
        button5 = (Button) this.findViewById(R.id.button5);
        statusbar = (TextView) this.findViewById(R.id.statusbar);

        question.getSettings().setJavaScriptEnabled(true);
        answer.getSettings().setJavaScriptEnabled(true);
        question.getSettings().setAllowFileAccess(true);
        answer.getSettings().setAllowFileAccess(true);

        boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }
        else {
            MnemosyneInstaller installer = new MnemosyneInstaller(this, activityHandler);
            installer.execute();
            Log.i("Mnemosyne", "Started installer");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else
                {
                    Toast.makeText(this, "Mnemosyne was not allowed to write to your storage and will not be able to function properly.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void continueOnCreate() {
        mnemosyneThread = new MnemosyneThread(this, activityHandler, getPackageName());
        mnemosyneThread.start();

        showAnswerButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.review_controller_show_answer();
                    }
                });
            }
        });

        button0.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.review_controller_grade_answer(0);
                    }
                });
            }
        });

        button1.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.review_controller_grade_answer(1);
                    }
                });
            }
        });

        button2.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.review_controller_grade_answer(2);
                    }
                });
            }
        });

        button3.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.review_controller_grade_answer(3);
                    }
                });
            }
        });

        button4.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.review_controller_grade_answer(4);
                    }
                });
            }
        });

        button5.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.review_controller_grade_answer(5);
                    }
                });
            }
        });


        // Does not work yet as expected, since the webview has its own handling of
        // gestures to deal with scrolling
        //http://stackoverflow.com/questions/7774642/scroll-webview-horizontally-inside-a-viewpager
        //gestureDetector = new GestureDetector(this, new MyGestureListener());

        // First run wizard, warn about scoped storage.
        SharedPreferences settings = getSharedPreferences("UserInfo", 0);
        if (!settings.contains("shown_first_run_wizard")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(Html.fromHtml("This application is meant to be used in conjunction with the <a href='http://www.mnemosyne-proj.org'>Mnemosyne desktop app</a>. Input your cards there, start the desktop sync server in 'Configure Mnemosyne' and then you can sync and review the cards in this Android app.<br><br>IMPORTANT: note that only the database 'default.db' is synced. <br><br>IMPORTANT: If you used Mnemosyne before on this device, <a href='https://mnemosyne-proj.org/help/android-and-storage'>click here</a>."));
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mnemosyneThread.getHandler().post(new Runnable() {
                                public void run() {
                                    mnemosyneThread.bridge.controller_show_sync_dialog_pre();
                                }
                            });
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
            ((TextView) alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("shown_first_run_wizard", true);
        editor.commit();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setFullscreen();
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                    new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int visibility) {
                            //final String s = Integer.toString(visibility);
                            //Toast toast = Toast.makeText(getApplicationContext(),
                            //        s, Toast.LENGTH_SHORT);
                            //toast.show();
                            //Log.d("Mnemosyne", "visibility: " + Integer.toString(visibility));
                            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                                // Exiting full screen, so show the action bar and hide
                                // it 5 seconds later.
                                getSupportActionBar().show();
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        setFullscreen();
                                    }
                                }, 5000);
                            }
                            if (visibility == 0) {
                                getSupportActionBar().show();
                            }
                        }
                    });
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getSupportActionBar().hide();
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_sync:
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.controller_show_sync_dialog_pre();
                    }
                });
                return true;

            case R.id.menu_replay_media:
                handleSoundFiles(currentHtml);
                return true;

            case R.id.menu_star:
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.controller_star_current_card();
                    }
                });
                return true;

            case R.id.menu_activate:
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.controller_show_activate_cards_dialog_pre();
                    }
                });
                return true;

            case R.id.menu_study_mode:
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        final String[] items = {
                                "Scheduled -> forgotten -> new",
                                "Study new unlearned cards only",
                                "Cram all cards",
                                "Cram recently learned new cards"};
                        final String[] ids = {
                                "ScheduledForgottenNew",
                                "NewOnly",
                                "CramAll",
                                "CramRecent"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(MnemosyneActivity.this);
                        builder.setTitle("Select study mode");
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mnemosyneThread.setGradeNames(ids[which]);
                                mnemosyneThread.bridge.controller_set_study_mode_with_id(ids[which]);
                            }
                        });
                        builder.setCancelable(false);
                        builder.create();
                        builder.show();
                    }
                });
                return true;

            case R.id.menu_archive_logs:
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Mnemosyne:wakelocktag");
                        wl.acquire();
                        try {
                            mnemosyneThread.bridge.controller_do_db_maintenance();
                        } finally {
                            wl.release();
                        }
                    }
                });
                return true;

            case R.id.menu_export_data:
                export_data();
                return true;

            case R.id.menu_import_data:
                import_data();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void export_data() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, EXPORT_DIRECTORY_RESULT);
    }

    public void import_data() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, IMPORT_DIRECTORY_RESULT);
    }
    public void playNextSound() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        File f = new File(soundFiles.get(soundIndex).getPath());
        if (! f.exists()) {
            soundIndex++;
            if (soundIndex < soundFiles.size()) {
                playNextSound();
            } else {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            return;
        }

        try {
            mediaPlayer.setDataSource(getApplicationContext(), soundFiles.get(soundIndex));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                if (soundIndex >= starts.size()) {
                    return;
                }
                mp.seekTo(starts.get(soundIndex));
                mp.start();
                if (stops.get(soundIndex) != 0)
                {
                    final MediaPlayer _mp = mp;
                    int duration = stops.get(soundIndex) - starts.get(soundIndex);
                    new CountDownTimer(duration, 100) {
                        @Override
                        public void onFinish() {
                            _mp.release();
                            soundIndex++;
                            if (soundIndex < soundFiles.size()) {
                                playNextSound();
                            }
                        }

                        @Override
                        public void onTick(long millisUntilFinished) {}
                    }.start();
                }
            }
        });

        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                soundIndex++;
                if (soundIndex < soundFiles.size()) {
                    playNextSound();
                }
                else {
                    mp.release();
                    mp = null;
                }
            }
        });

        mediaPlayer.prepareAsync();
    }

    Pattern audioRE = Pattern.compile("<audio src=\"(.+?)\"(.*?)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    Pattern startRE = Pattern.compile("start=\"(.+?)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    Pattern stopRE = Pattern.compile("stop=\"(.+?)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public String handleSoundFiles(String html) {
        if (html == null) {
            return html;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        soundFiles.clear();
        starts.clear();
        stops.clear();
        Matcher matcher = audioRE.matcher(html);
        while (matcher.find()) {
            // Look for start and stop of sound segment in ms.
            int start = 0;
            int stop = 0;
            if (matcher.group(2) != null) {

                Matcher startMatcher = startRE.matcher(matcher.group(2));
                while (startMatcher.find()) {
                    start = (int) (Double.valueOf(startMatcher.group(1)).doubleValue() * 1000);
                    break;
                }

                Matcher stopMatcher = stopRE.matcher(matcher.group(2));
                while (stopMatcher.find()) {
                    stop = (int) (Double.valueOf(stopMatcher.group(1)).doubleValue() * 1000);
                    break;
                }
            }

            soundFiles.add(Uri.parse(matcher.group(1)));
            starts.add(start);
            stops.add(stop);
            soundIndex = 0;

            playNextSound();
        }

        return matcher.replaceAll("");
    }

    public void setQuestion(String html) {
        currentHtml = html;
        html = handleSoundFiles(html);
        question.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    public void setAnswer(String html, Boolean processAudio) {
        if (processAudio == true) {
            currentHtml = html;
            html = handleSoundFiles(html);
        }
        answer.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    // Get results back from sync activity.

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SYNC_ACTIVITY_RESULT) {
            if (resultCode == RESULT_OK) {
                final String server = data.getStringExtra("server");
                final String port_data = data.getStringExtra("port");
                final Integer port = port_data.isEmpty() ? new Integer(8512) : new Integer(port_data);
                final String username = data.getStringExtra("username");
                final String password = data.getStringExtra("password");
                final Boolean rememberPassword = data.getBooleanExtra("rememberPassword", true);

                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.config_set_string("server_for_sync_as_client", server);
                        mnemosyneThread.bridge.config_set_integer("port_for_sync_as_client", port);
                        mnemosyneThread.bridge.config_set_string("username_for_sync_as_client", username);
                        if (rememberPassword) {
                            mnemosyneThread.bridge.config_set_string("password_for_sync_as_client", password);
                            mnemosyneThread.bridge.config_set_boolean("remember_password_for_sync_as_client", true);
                        } else {
                            mnemosyneThread.bridge.config_set_string("password_for_sync_as_client", "");
                            mnemosyneThread.bridge.config_set_boolean("remember_password_for_sync_as_client", false);
                        }
                        mnemosyneThread.bridge.config_save();
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Mnemosyne:wakelocktag");
                        wl.acquire();
                        try {
                            mnemosyneThread.bridge.controller_sync(server, port, username, password);
                        } finally {
                            wl.release();
                        }
                        mnemosyneThread.bridge.controller_show_sync_dialog_post();
                    }
                });
            }
            else {
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.controller_show_sync_dialog_post();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mnemosyneThread.UIActivity.setFullscreen();
                            }
                        });
                    }
                });
            }
        }

        if (requestCode == ACTIVATE_CARDS_ACTIVITY_RESULT) {
            if (resultCode == RESULT_OK) {
                final String savedSet = data.getStringExtra("saved_set");
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.setProgressText("(De)activating cards...");
                        mnemosyneThread.bridge.database_set_criterion_with_name(savedSet);
                        mnemosyneThread.bridge.controller_show_activate_cards_dialog_post();
                        mnemosyneThread.closeProgress();
                    }
                });
            }
            else {
                mnemosyneThread.getHandler().post(new Runnable() {
                    public void run() {
                        mnemosyneThread.bridge.controller_show_activate_cards_dialog_post();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mnemosyneThread.UIActivity.setFullscreen();
                            }
                        });
                    }
                });
            }
        }

        if (requestCode == EXPORT_DIRECTORY_RESULT && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                final DocumentFile userDir = DocumentFile.fromTreeUri(this, uri);
                mnemosyneThread.getHandler().post(new Runnable() {
                    final DocumentFile userDir_ = userDir;
                    public void run() {
                        mnemosyneThread.doExport(userDir_);
                    }
                });
            }
        }

        if (requestCode == IMPORT_DIRECTORY_RESULT && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                final DocumentFile userDir = DocumentFile.fromTreeUri(this, uri);
                mnemosyneThread.getHandler().post(new Runnable() {
                    final DocumentFile userDir_ = userDir;
                    public void run() {
                        mnemosyneThread.doImport(userDir_);
                    }
                });
            }
        }
    }

    @Override
    public void onPause() {
        this.paused = true;
        super.onPause();
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch(Exception e){
        }

        if ((mnemosyneThread != null) && (mnemosyneThread.getHandler() != null))
        {
            mnemosyneThread.getHandler().post(new Runnable() {
            public void run() {
                mnemosyneThread.pauseMnemosyne();
                }
            });
        }
    }

    public void onResume() {
        super.onResume();
        if (this.paused == true) { // Make sure this does not get called on initial startup.
            setFullscreen();
        }
        this.paused = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
        }

        if (mnemosyneThread != null) {
            mnemosyneThread.getHandler().post(new Runnable() {
                public void run() {
                    mnemosyneThread.stopMnemosyne();
                }
            });
        }
    }

    //@Override
    //public boolean onTouchEvent(MotionEvent event){
    //    Log.d("Mnemosyne", "Touchevent" + event);
    //    this.gestureDetector.onTouchEvent(event);
    //    return super.onTouchEvent(event);
    //}

    //class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

    //    private static final int SWIPE_MIN_DISTANCE = 120;
    //    private static final int SWIPE_MAX_OFF_PATH = 200;
    //    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    //    @Override
    //    public boolean onDown(MotionEvent e) {
    //        return true;
    //    }

    //    @Override
    //    public boolean onFling(MotionEvent e1, MotionEvent e2,
    //            float velocityX, float velocityY) {
    //        Log.d("Mnemosyne", "onFling: " + e1.toString()+e2.toString());

    //       try {
    //            float diffAbs = Math.abs(e1.getY() - e2.getY());
    //            float diff = e1.getX() - e2.getX();

    //            if (diffAbs > SWIPE_MAX_OFF_PATH)
    //                return false;

    //            // Left swipe.
    //            if (diff > SWIPE_MIN_DISTANCE
    //                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
    //                MnemosyneActivity.this.onLeftSwipe();
    //                return true;

    //                // Right swipe.
    //           } else if (-diff > SWIPE_MIN_DISTANCE
    //                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
    //                MnemosyneActivity.this.onRightSwipe();
    //                return true;
    //            }
    //        } catch (Exception e) {
    //            Log.e("Mnemosyne", "Error on gestures");
    //        }
    //        return false;
    //    }
    //}

    //public void onLeftSwipe() {
    //    Log.d("Mnemosyne", "left swipe");
    //}

    //public void onRightSwipe() {
    //    Log.d("Mnemosyne", "right swipe");
    //}

}
