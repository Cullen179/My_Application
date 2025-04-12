package com.example.myapplication.ui.subscription;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;

public class SubscriptionFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_subscription, container, false);
        
        // Initialize UI components
        TextView titleText = root.findViewById(R.id.text_subscription);
        titleText.setText("Subscription Plans");
        
        return root;
    }
} 