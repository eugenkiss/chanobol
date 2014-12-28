package anabolicandroids.chanobol;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;

import javax.inject.Inject;

import anabolicandroids.chanobol.annotations.ForActivity;
import anabolicandroids.chanobol.annotations.ForApplication;

public abstract class BaseFragment extends Fragment {
    @Inject @ForApplication public Context appContext;
    @Inject @ForActivity public Context context;
    @Inject public LayoutInflater inflater;
    @Inject public Resources resources;
    @Inject public AssetManager assets;

    protected abstract int getLayoutResource();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((BaseActivity) getActivity()).inject(this);
    }
}
