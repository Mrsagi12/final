package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

// Fragment that simply displays the instructions content (fragment_instructions.xml).
// Kept separate from the Activity to satisfy the project requirement of using
// the Fragment component, even though this app only needs one Fragment per screen.
public class InstructionsFragment extends Fragment {

    // Called when the Fragment needs to create its view - inflates the XML layout
    // and returns it so the hosting Activity can display it inside its container.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_instructions, container, false);
    }
}