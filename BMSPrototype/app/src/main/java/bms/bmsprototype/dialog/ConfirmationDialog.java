/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bms.bmsprototype.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v13.app.FragmentCompat;

import bms.bmsprototype.R;

/**
 * Dialog used to request permissions for audio and video recording
 */
public class ConfirmationDialog extends DialogFragment {

    private static final String ARG_PERMS = "permissions";
    private static final String ARG_REQ = "request";

    public static ConfirmationDialog newInstance(String[] permissions, int requestPermissions) {
        ConfirmationDialog dialog = new ConfirmationDialog();
        Bundle args = new Bundle();
        args.putStringArray(ARG_PERMS, permissions);
        args.putInt(ARG_REQ, requestPermissions);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Fragment parent = getParentFragment();
        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.permission_request)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FragmentCompat.requestPermissions(parent, getArguments().getStringArray(ARG_PERMS), getArguments().getInt(ARG_REQ));
                    }
                })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                parent.getActivity().finish();
                            }
                        })
                .create();
    }
}