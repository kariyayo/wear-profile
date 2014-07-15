package info.bati11.wearprofile;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;


public class ProfileActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;

    private EditText editText;
    private Button button;
    private TextView nameTextView;
    private TextView descriptionTextView;

    private ImageView imageView;
    private Button syncButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        final LinearLayout layout = (LinearLayout)findViewById(R.id.layout);
        editText = (EditText)findViewById(R.id.editText);
        button = (Button)findViewById(R.id.button);
        nameTextView = (TextView)findViewById(R.id.nameTextView);
        descriptionTextView = (TextView)findViewById(R.id.descriptionTextView);

        final Context context = this;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProfileImageTask profileImageTask = new ProfileImageTask(context, layout);
                TwitterProfileTask task = new TwitterProfileTask(nameTextView, descriptionTextView, profileImageTask);
                task.execute(editText.getText().toString());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("TAG", "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("TAG", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("TAG", "onConnectionFailed: " + connectionResult);
    }

    private class TwitterProfileTask extends AsyncTask<String, Integer, User> {

        private TextView nameTextView;
        private TextView descriptionTextView;
        private ProfileImageTask profileImageTask;

        public TwitterProfileTask(TextView nameTextView, TextView descriptionTextView, ProfileImageTask profileImageTask1) {
            this.nameTextView = nameTextView;
            this.descriptionTextView = descriptionTextView;
            this.profileImageTask = profileImageTask1;
        }

        @Override
        protected User doInBackground(String... userNames) {
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey("1mPQ0eYZVu5tcd67jcUUp9qaw")
                    .setOAuthConsumerSecret("0E13NTbG2RpzgzcB86ThMmwhu0THTDoAphANd4jSWpKk5tXdeM")
                    .setOAuthAccessToken("258617593-dHbPMIL28SH2nl6OgVykRFCdvpQ53et47HDcu36g")
                    .setOAuthAccessTokenSecret("sQZCf5lhTsmBpkib0wLWk2L0EOKiaCjmPtPnqzTv8zNzt");
            TwitterFactory tf = new TwitterFactory(cb.build());
            Twitter twitter = tf.getInstance();
            User result = null;
            try {
                result = twitter.showUser(userNames[0]);
                Log.d("TAG", result.getDescription());
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(User user) {
            nameTextView.setText("@" + user.getScreenName());
            descriptionTextView.setText(user.getDescription());
            Log.d("TAG", user.getProfileImageURL());

            profileImageTask.execute(user.getProfileImageURL());
        }
    }

    private class ProfileImageTask extends AsyncTask<String, Integer, Bitmap> {

        private Context context;
        private ViewGroup layout;

        public ProfileImageTask(Context context, ViewGroup layout) {
            this.context = context;
            this.layout = layout;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            URL url = null;
            try {
                url = new URL(strings[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            Bitmap result = null;
            if (url != null) {
                try {
                    InputStream inputStream = url.openStream();
                    result = BitmapFactory.decodeStream(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            if (imageView == null) {
                imageView = new ImageView(context);
                imageView.setImageBitmap(bitmap);
                layout.addView(imageView);
            } else {
                imageView.setImageBitmap(bitmap);
            }

            if (syncButton == null) {
                syncButton = new Button(context);
                syncButton.setText("sync wearable");
                syncButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/profile/info");
                        DataMap dataMap = dataMapRequest.getDataMap();

                        dataMap.putString("name", nameTextView.getText().toString());
                        dataMap.putString("description", descriptionTextView.getText().toString());

                        PutDataRequest request = dataMapRequest.asPutDataRequest();
                        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                Log.d("TAG", "[info] onResult: " + dataItemResult.getStatus());

                                PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/profile/image");
                                DataMap dataMap = dataMapRequest.getDataMap();

                                Asset asset = createAssetFromBitmap(bitmap);
                                dataMap.putAsset("image", asset);

                                PutDataRequest request = dataMapRequest.asPutDataRequest();
                                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                                pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                                    @Override
                                    public void onResult(DataApi.DataItemResult dataItemResult) {
                                        Log.d("TAG", "[image] onResult: " + dataItemResult.getStatus());
                                    }
                                });
                            }
                        });
                    }
                });
                layout.addView(syncButton);
            }
        }
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }
}
