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

/**
 * Unique activity containing a loading view and a content view.
 */
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

    /**
     * Use the back stack to retreive preceding fragment and clean the current fragment
     * before destruction. if there is no more fragment in the back stack, app is killed.
     */
    @Override
    public void onBackPressed() {
        int fragments = getFragmentManager().getBackStackEntryCount();

        //Get the fragment we are exiting from.
        String tag = getFragmentManager().getBackStackEntryAt(fragments - 1).getName();
        BaseFragment frag = (BaseFragment) getFragmentManager().findFragmentByTag(tag);

        //Clean open connection on that fragment because it may not be destroyed immediately.
        frag.clean();

        //Finish if we just exited from the last fragment.
        if (fragments == 1) {
            finish();
            return;
        }

        super.onBackPressed();
    }

    /**
     * Add specified thread text with a UIThread to the text view located in the
     * content view.
     *
     * @param text
     */
    public void addToDebug(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _tvDebug.setText(text + "\n" + _tvDebug.getText());
            }
        });
    }

    /**
     * Create a {@link PairingFragment} and replace it with the current fragment.
     * This fragment should be create as the first fragment of the application.
     */
    public void moveToPairing()
    {
        beginLoading();
        PairingFragment f = PairingFragment.newInstance();
        replaceFragment(f);
    }

    /**
     * Create a {@link SelectionFragment} and replace it with the current fragment.
     * This should be called by a {@link PairingFragment}.
     *
     * @param info Wifi connection info used to create a socket connection
     * @param devicesName name of the device that we hace connection info
     */
    public void moveToSelection(WifiP2pInfo info, String devicesName)
    {
        beginLoading();
        SelectionFragment f = SelectionFragment.newInstance(info, devicesName);
        replaceFragment(f);
    }

    /**
     * Create a {@link StreamingFragment} and replace it with the current fragment.
     * This should be called by a {@link SelectionFragment}.
     *
     */
    public void moveToStreaming()
    {
        beginLoading();
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Create a {@link PlaybackFragment} and replace it with the current fragment.
     * This should be called by a {@link SelectionFragment}.
     */
    public void moveToPlayback()
    {
        beginLoading();
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Used to show an animated loading view and to hide current view.
     * Complete action with {@link MainActivity#endLoading()}.
     */
    public void beginLoading()
    {
        showContentOrLoadingIndicator(false);
    }

    /**
     * Used to hide the loading view and shoe the content view. This should be
     * use with {@link MainActivity#beginLoading()}
     */
    public void endLoading()
    {
        showContentOrLoadingIndicator(true);
    }

    /**
     * Replace the current fragment located in the R.id.fragment_container with the
     * newFragment in arguments. Old fragment is added to back stack
     *
     * @param newFragment The new fragment to place in the container.
     */
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
