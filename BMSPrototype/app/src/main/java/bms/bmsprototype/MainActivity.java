package bms.bmsprototype;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Collection;

import bms.bmsprototype.fragment.PairingFragment;
import bms.bmsprototype.fragment.SelectionFragment;

public class MainActivity extends Activity {
    /**
     * The view (or view group) containing the content. This is one of two overlapping views.
     */
    private View mContentView;

    /**
     * The view containing the loading indicator. This is the other of two overlapping views.
     */
    private View mLoadingView;

    private TextView _tvDebug;

    /**
     * The system "short" animation time duration, in milliseconds. This duration is ideal for
     * subtle animations or animations that occur very frequently.
     */
    private int mShortAnimationDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mContentView = findViewById(R.id.content);
        mLoadingView = findViewById(R.id.loading_spinner);
        _tvDebug = (TextView)findViewById(R.id.tvDebug);

        // Initially hide the content view.
        mContentView.setVisibility(View.GONE);

        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    protected void onStart() {
        super.onStart();
        moveToPairing();
    }

    public void addToDebug(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _tvDebug.setText(text + "\n" + _tvDebug.getText());
            }
        });
    }

    public void moveToPairing()
    {
        PairingFragment f = PairingFragment.newInstance();
        replaceFragment(f);
    }

    public void moveToSelection(WifiP2pInfo info, Collection<String> devicesName)
    {
        SelectionFragment f = SelectionFragment.newInstance(info, devicesName);
        replaceFragment(f);
    }

    public void moveToStreaming()
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void moveToPlayback()
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void viewCreated()
    {
        showContentOrLoadingIndicator(true);
    }

    private void replaceFragment(Fragment newFragment)
    {
        showContentOrLoadingIndicator(false);
        // Create new fragment and transaction
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    /**
     * Cross-fades between {@link #mContentView} and {@link #mLoadingView}.
     */
    private void showContentOrLoadingIndicator(boolean contentLoaded) {
        // Decide which view to hide and which to show.
        final View showView = contentLoaded ? mContentView : mLoadingView;
        final View hideView = contentLoaded ? mLoadingView : mContentView;

        // Set the "show" view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        showView.setAlpha(0f);
        showView.setVisibility(View.VISIBLE);

        // Animate the "show" view to 100% opacity, and clear any animation listener set on
        // the view. Remember that listeners are not limited to the specific animation
        // describes in the chained method calls. Listeners are set on the
        // ViewPropertyAnimator object for the view, which persists across several
        // animations.
        showView.animate()
                .alpha(1f)
                .setDuration(mShortAnimationDuration)
                .setListener(null);

        // Animate the "hide" view to 0% opacity. After the animation ends, set its visibility
        // to GONE as an optimization step (it won't participate in layout passes, etc.)
        hideView.animate()
                .alpha(0f)
                .setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hideView.setVisibility(View.GONE);
                    }
                });
    }
}
