package anabolicandroids.chanobol;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import dagger.ObjectGraph;

public abstract class BaseActivity extends ActionBarActivity {
    private ObjectGraph activityGraph;

    protected abstract int getLayoutResource();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create the activity graph by .plus-ing our modules onto the application graph.
        App app = (App) getApplication();
        activityGraph = app.getAppGraph().plus(getModules().toArray());
        // Inject ourselves so subclasses will have dependencies fulfilled when this method returns.
        activityGraph.inject(this);

        setContentView(getLayoutResource());
        ButterKnife.inject(this);
    }

    @Override protected void onDestroy() {
        // Eagerly clear the reference to the activity graph to allow it to be garbage collected as
        // soon as possible.
        activityGraph = null;

        super.onDestroy();
    }

    /**
     * A list of modules to use for the individual activity graph. Subclasses can override this
     * method to provide additional modules provided they call and include the modules returned by
     * calling {@code super.getModules()}.
     */
    protected List<Object> getModules() {
        return Arrays.asList();
    }

    /** Inject the supplied {@code object} using the activity-specific graph. */
    public void inject(Object object) {
        activityGraph.inject(object);
    }
}
