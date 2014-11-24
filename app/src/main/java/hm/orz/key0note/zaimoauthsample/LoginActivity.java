package hm.orz.key0note.zaimoauthsample;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;


public class LoginActivity extends ActionBarActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();

    private static final String CONSUMER_KEY = "83cd8d1b6372be63b5b2c06731fae5a54aa55fdd";
    private static final String CONSUMER_SECRET = "471019143036c09b229eed30ce7b7d3e5aba6521";
    private static final String REQUEST_TOKEN_URL = "https://api.zaim.net/v2/auth/request";
    private static final String AUTHORIZE_URL = "https://auth.zaim.net/users/auth";
    private static final String ACCESS_TOKEN_URL = "https://api.zaim.net/v2/auth/access";

    private static final String CALLBACK = "http://zaim_oauth_sample/"; //コールバックは使わないので適当なURLを指定する
    private static final String LOGIN_COMPLETE_URL = AUTHORIZE_URL;

    private OAuthConsumer mConsumer;
    private OAuthProvider mProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");

        mConsumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
        mProvider = new CommonsHttpOAuthProvider(REQUEST_TOKEN_URL, ACCESS_TOKEN_URL, AUTHORIZE_URL);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final WebView webView = (WebView) findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.v(this.getClass().getName(), "onPageFinished url = " + url);

                if (url.equals(LOGIN_COMPLETE_URL)) {
                    webView.loadUrl("javascript:window.alert(document.getElementsByTagName(\'code\')[0].innerHTML);");
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.d(TAG, "url = " + url);
                Log.d(TAG, "message = " + message);

                // 上のonPageFinishedで埋め込んだjavascriptでOAuth Verifierのコードが取得できる。
                // このコードを利用してAccessTokenを取得する。
                String oauthVerifier = message;
                OAuthAccessAsyncTask asyncTask = new OAuthAccessAsyncTask(oauthVerifier, new AccessCallback() {
                    @Override
                    public void onComplete() {
                        // OAuthConsumerからTokenとTokenSecretが取得できる
                        Log.d(TAG, "ACCESS_TOKEN : " + mConsumer.getToken());
                        Log.d(TAG, "ACCESS_TOKEN_SECRET : " + mConsumer.getTokenSecret());
                    }
                });
                asyncTask.execute();

                return true;
            }
        });

        //
        OAuthRequestAsyncTask asyncTask = new OAuthRequestAsyncTask(CALLBACK, new RequestCallback() {
            @Override
            public void onComplete(String authUrl) {
                Log.d(TAG, "authUrl = " + authUrl);
                webView.loadUrl(authUrl);
            }
        });
        asyncTask.execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public interface RequestCallback {
        public void onComplete(String authUrl);
    }

    public interface AccessCallback {
        public void onComplete();
    }

    private class OAuthRequestAsyncTask extends AsyncTask<Void, Void, Void> {

        private RequestCallback mCallback;
        private String mCallbackUrl;
        private String mAuthUrl;

        OAuthRequestAsyncTask(String callbackUrl, RequestCallback callback) {
            mCallbackUrl = callbackUrl;
            mCallback = callback;
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            //OAuth認証
            try {
                mAuthUrl = mProvider.retrieveRequestToken(mConsumer, mCallbackUrl);
            } catch (OAuthMessageSignerException e) {
                e.printStackTrace();
            } catch (OAuthNotAuthorizedException e) {
                e.printStackTrace();
            } catch (OAuthExpectationFailedException e) {
                e.printStackTrace();
            } catch (OAuthCommunicationException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            mCallback.onComplete(mAuthUrl);
        }
    }

    private class OAuthAccessAsyncTask extends AsyncTask<Void, Void, Void> {

        private AccessCallback mCallback;
        private String mOAuthVerifier;

        OAuthAccessAsyncTask(String oauthVerifier, AccessCallback callback) {
            mOAuthVerifier = oauthVerifier;
            mCallback = callback;
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                // AccessToken取得
                mProvider.retrieveAccessToken(mConsumer, mOAuthVerifier);

            } catch (OAuthMessageSignerException e) {
                e.printStackTrace();
            } catch (OAuthNotAuthorizedException e) {
                e.printStackTrace();
            } catch (OAuthExpectationFailedException e) {
                e.printStackTrace();
            } catch (OAuthCommunicationException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            mCallback.onComplete();
        }
    }
}
