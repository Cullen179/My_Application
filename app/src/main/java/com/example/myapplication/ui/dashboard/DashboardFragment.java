package com.example.myapplication.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.myapplication.R;

public class DashboardFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Inflate the dashboard layout
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        
        // Replace with upload media fragment
        if (savedInstanceState == null) {
            UploadMediaFragment uploadMediaFragment = new UploadMediaFragment();
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(R.id.dashboard_container, uploadMediaFragment);
            transaction.commit();
        }
        
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}