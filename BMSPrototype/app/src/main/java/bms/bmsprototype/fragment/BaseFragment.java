package bms.bmsprototype.fragment;

import android.app.Fragment;

/**
 * Abstract class used to allow cleaning before destruction.
 * Useful with the back stack.
 */
public abstract class BaseFragment extends Fragment {

    /**
     * Call this method to close opened connection.
     */
    public void clean(){}

    @Override
    public void onDestroy() {
        clean();
        super.onDestroy();
    }
}
