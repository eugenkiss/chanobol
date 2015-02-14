package anabolicandroids.chanobol.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import anabolicandroids.chanobol.annotations.ForApplication;
import butterknife.ButterKnife;

public abstract class BaseFragment extends Fragment {
    @Inject @ForApplication public Context appContext;
    @Inject public LayoutInflater inflater;
    @Inject public Resources resources;
    @Inject public AssetManager assets;

    protected abstract int getLayoutResource();

    @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutResource(), container, false);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((BaseActivity) getActivity()).inject(this);
    }
}
