package net.lonelytransistor.launcher.generics;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.lonelytransistor.launcher.repos.AllRepos;

public class GenericActivity extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AllRepos.init(this);
    }
}
