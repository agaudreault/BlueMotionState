package bms.bmsprototype.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import bms.bmsprototype.R;
import bms.bmsprototype.fragment.BaseFragment;
import bms.bmsprototype.fragment.PairingFragment;
import bms.bmsprototype.fragment.PlaybackFragment;
import bms.bmsprototype.fragment.SelectionFragment;
import bms.bmsprototype.fragment.StreamingFragment;

public class MainActivity extends Activity {

    private View _contentView;
    private View _loadingView;
    private TextView _tvDebug;

    private int _shortAnimationDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        _contentView = findViewById(R.id.content);
        _loadingView = findViewById(R.id.loading_spinner);

        _tvDebug = (TextView)findViewById(R.id.tvDebug);

        // Initially hide the content view.
        _contentView.setVisibility(View.GONE);

        // Retrieve and cache the system's default "short" animation time.
        _shortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        moveToPairing();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        int fragments = getFragmentManager().getBackStackEntryCount();
        String tag = getFragmentManager().getBackStackEntryAt(fragments - 1).getName();
        BaseFragment frag = (BaseFragment) getFragmentManager().findFragmentByTag(tag);
        frag.clean();
        if (fragments == 1) {
            finish();
            return;
        }

        super.onBackPressed();
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
        beginLoading();
        PairingFragment f = PairingFragment.newInstance();
        replaceFragment(f);
    }

    public void moveToSelection(WifiP2pInfo info, String devicesName)
    {
        beginLoading();
        SelectionFragment f = SelectionFragment.newInstance(info, devicesName);
        replaceFragment(f);
    }

    public void moveToStreaming(WifiP2pInfo info)
    {
        beginLoading();
        StreamingFragment f = StreamingFragment.newInstance(info);
        replaceFragment(f);
    }

    public void moveToPlayback(WifiP2pInfo info)
    {
        beginLoading();
        PlaybackFragment f = PlaybackFragment.newInstance(info);
        replaceFragment(f);
    }

    public void beginLoading()
    {
        showContentOrLoadingIndicator(false);
    }

    public void endLoading()
    {
        showContentOrLoadingIndicator(true);
    }

    private void replaceFragment(final Fragment newFragment)
    {
        // Create new fragment and transaction
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        String tag = newFragment.getClass().getName();
        transaction.replace(R.id.fragment_container, newFragment, tag);
        transaction.addToBackStack(tag);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        // Commit the transaction
        transaction.commit();
    }

    /**
     * Cross-fades between {@link #_contentView} and {@link #_loadingView}.
     */
    private void showContentOrLoadingIndicator(boolean contentLoaded) {
        // Decide which view to hide and which to show.
        final View showView = contentLoaded ? _contentView : _loadingView;
        final View hideView = contentLoaded ? _loadingView : _contentView;

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
                .setDuration(_shortAnimationDuration)
                .setListener(null);

        // Animate the "hide" view to 0% opacity. After the animation ends, set its visibility
        // to GONE as an optimization step (it won't participate in layout passes, etc.)
        hideView.animate()
                .alpha(0f)
                .setDuration(_shortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hideView.setVisibility(View.GONE);
                    }
                });
    }

}
