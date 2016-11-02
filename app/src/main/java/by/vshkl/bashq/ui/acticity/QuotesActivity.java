package by.vshkl.bashq.ui.acticity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import by.vshkl.bashq.BashqApplication;
import by.vshkl.bashq.R;
import by.vshkl.bashq.common.Navigator;
import by.vshkl.bashq.injection.component.ApplicationComponent;
import by.vshkl.bashq.injection.component.DaggerQuotesComponent;
import by.vshkl.bashq.injection.component.QuotesComponent;
import by.vshkl.bashq.injection.module.ActivityModule;
import by.vshkl.bashq.injection.module.NavigationModule;
import by.vshkl.bashq.injection.module.QuotesModule;
import by.vshkl.bashq.ui.adapter.EndlessScrollListener;
import by.vshkl.bashq.ui.adapter.HidingScrollListener;
import by.vshkl.bashq.ui.adapter.QuotesAdapter;
import by.vshkl.mvp.model.Errors;
import by.vshkl.mvp.model.Quote;
import by.vshkl.mvp.presenter.QuotesPresenter;
import by.vshkl.mvp.presenter.common.Subsection;
import by.vshkl.mvp.view.QuotesView;

public class QuotesActivity extends AppCompatActivity implements QuotesView, DatePickerDialog.OnDateSetListener {

    @Inject
    QuotesPresenter quotesPresenter;
    @Inject
    Navigator navigator;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.fl_container)
    FrameLayout flContainer;
    @BindView(R.id.srl_update)
    SwipeRefreshLayout srlUpdate;
    @BindView(R.id.rv_quotes)
    RecyclerView rvQuotes;
    @BindView(R.id.pb_progress)
    ProgressBar pbProgress;
    @BindView(R.id.fab_calendar_multiple)
    FloatingActionMenu fabCalendarMenu;
    @BindView(R.id.fab_calendar_single)
    FloatingActionButton fabCalendar;

    private QuotesComponent quotesComponent;
    private QuotesAdapter quotesAdapter;
    private EndlessScrollListener scrollListener;
    private Subsection currentSubsection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quotes);
        ButterKnife.bind(QuotesActivity.this);

        setSupportActionBar(toolbar);

        initializeNavigationDrawer(QuotesActivity.this, toolbar, savedInstanceState);
        initializeDaggerComponent(((BashqApplication) getApplication()).getApplicationComponent());
        initializePresenter();
        initializeRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        quotesPresenter.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        quotesPresenter.onStop();
    }

    //==================================================================================================================

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        switch (currentSubsection) {
            case BEST_MONTH:
                quotesPresenter.setUrlPartBest("/bestmonth/" + year + "/" + monthOfYear);
                quotesAdapter.clearQuotes();
                quotesPresenter.getQuotes(false);
                break;
            case BEST_YEAR:
                quotesPresenter.setUrlPartBest("/bestyear/" + year);
                quotesAdapter.clearQuotes();
                quotesPresenter.getQuotes(false);
                break;
            case ABYSS_BEST:
                quotesPresenter.setUrlPartBest(
                        String.valueOf(year) + String.format("%02d", monthOfYear) + String.format("%02d", dayOfMonth));
                quotesAdapter.clearQuotes();
                quotesPresenter.getQuotes(false);
                break;
        }
    }

    //==================================================================================================================

    @OnClick(R.id.toolbar)
    void onToolbarClicked() {
        int position = ((LinearLayoutManager) rvQuotes.getLayoutManager()).findFirstVisibleItemPosition();
        if (position <= 10) {
            rvQuotes.smoothScrollToPosition(0);
        } else {
            rvQuotes.scrollToPosition(0);
        }
    }

    @OnClick(R.id.fab_calendar_multiple_month)
    void onFabCalendarMultipleMonthClicked() {
        currentSubsection = Subsection.BEST_MONTH;
        quotesPresenter.setSubsection(currentSubsection);
        showDatePickerDialog();
    }

    @OnClick(R.id.fab_calendar_multiple_year)
    void onFabCalendarMultipleYearClicked() {
        currentSubsection = Subsection.BEST_YEAR;
        quotesPresenter.setSubsection(currentSubsection);
        showDatePickerDialog();
    }

    @OnClick(R.id.fab_calendar_single)
    void onFabCalendarSingleClicked() {
        showDatePickerDialog();
    }

    //==================================================================================================================

    @Override
    public void showQuotes(final List<Quote> quotes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addQuotes(quotes);
            }
        });
    }

    @Override
    public void showEmpty() {

    }

    @Override
    public void showLoading() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                flContainer.setVisibility(View.GONE);
                pbProgress.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void hideLoading() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pbProgress.setVisibility(View.GONE);
                flContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void showError(Errors errorType) {

    }

    //==================================================================================================================

    private void initializeNavigationDrawer(AppCompatActivity activity, Toolbar toolbar, Bundle savedInstanceState) {
        new DrawerBuilder().withActivity(activity)
                .withToolbar(toolbar)
                .withSavedInstance(savedInstanceState)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.nd_new).withIcon(R.drawable.ic_new).withIdentifier(1),
                        new PrimaryDrawerItem().withName(R.string.nd_random).withIcon(R.drawable.ic_random).withIdentifier(2),
                        new PrimaryDrawerItem().withName(R.string.nd_best).withIcon(R.drawable.ic_best).withIdentifier(3),
                        new PrimaryDrawerItem().withName(R.string.nd_rating).withIcon(R.drawable.ic_rating).withIdentifier(4),
                        new PrimaryDrawerItem().withName(R.string.nd_abyss).withIcon(R.drawable.ic_abyss).withIdentifier(5),
                        new PrimaryDrawerItem().withName(R.string.nd_abyss_top).withIcon(R.drawable.ic_abyss_top).withIdentifier(6),
                        new PrimaryDrawerItem().withName(R.string.nd_abyss_best).withIcon(R.drawable.ic_abyss_best).withIdentifier(7),
                        new PrimaryDrawerItem().withName(R.string.nd_comics).withIcon(R.drawable.ic_comics).withIdentifier(8),
                        new SectionDrawerItem().withName(R.string.nd_fav),
                        new PrimaryDrawerItem().withName(R.string.nd_fav_quotes).withIcon(R.drawable.ic_favourite).withIdentifier(9),
                        new PrimaryDrawerItem().withName(R.string.nd_fav_comics).withIcon(R.drawable.ic_favourite).withIdentifier(10),
                        new SectionDrawerItem().withName(R.string.nd_other),
                        new PrimaryDrawerItem().withName(R.string.nd_settings).withIcon(R.drawable.ic_settings).withIdentifier(11)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        switch ((int) drawerItem.getIdentifier()) {
                            case 1:
                                currentSubsection = Subsection.INDEX;
                                toggleFloatingActionButton();
                                quotesPresenter.setSubsection(currentSubsection);
                                quotesAdapter.clearQuotes();
                                scrollListener.resetState();
                                quotesPresenter.getQuotes(false);
                                break;
                            case 2:
                                currentSubsection = Subsection.RANDOM;
                                toggleFloatingActionButton();
                                quotesPresenter.setSubsection(currentSubsection);
                                quotesAdapter.clearQuotes();
                                scrollListener.resetState();
                                quotesPresenter.getQuotes(false);
                                break;
                            case 3:
                                currentSubsection = Subsection.BEST;
                                toggleFloatingActionButton();
                                quotesPresenter.setSubsection(currentSubsection);
                                quotesAdapter.clearQuotes();
                                scrollListener.resetState();
                                quotesPresenter.getQuotes(false);
                                break;
                            case 4:
                                currentSubsection = Subsection.BY_RATING;
                                toggleFloatingActionButton();
                                quotesPresenter.setSubsection(currentSubsection);
                                quotesAdapter.clearQuotes();
                                scrollListener.resetState();
                                quotesPresenter.getQuotes(false);
                                break;
                            case 5:
                                currentSubsection = Subsection.ABYSS;
                                toggleFloatingActionButton();
                                quotesPresenter.setSubsection(currentSubsection);
                                quotesAdapter.clearQuotes();
                                scrollListener.resetState();
                                quotesPresenter.getQuotes(false);
                                break;
                            case 6:
                                currentSubsection = Subsection.ABYSS_TOP;
                                toggleFloatingActionButton();
                                quotesPresenter.setSubsection(currentSubsection);
                                quotesAdapter.clearQuotes();
                                scrollListener.resetState();
                                quotesPresenter.getQuotes(false);
                                break;
                            case 7:
                                currentSubsection = Subsection.ABYSS_BEST;
                                toggleFloatingActionButton();
                                quotesPresenter.setSubsection(currentSubsection);
                                quotesPresenter.setUrlPartBest(null);
                                quotesAdapter.clearQuotes();
                                scrollListener.resetState();
                                quotesPresenter.getQuotes(false);
                                break;
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                        }
                        return false;
                    }
                })
                .build();
    }

    private void initializeDaggerComponent(ApplicationComponent applicationComponent) {
        quotesComponent = DaggerQuotesComponent.builder()
                .quotesModule(new QuotesModule())
                .activityModule(new ActivityModule(QuotesActivity.this))
                .navigationModule(new NavigationModule())
                .applicationComponent(applicationComponent)
                .build();
        quotesComponent.inject(QuotesActivity.this);
    }

    private void initializePresenter() {
        quotesPresenter.attachView(QuotesActivity.this);
        currentSubsection = Subsection.INDEX;
        quotesPresenter.setSubsection(currentSubsection);
        toggleFloatingActionButton();
    }

    private void toggleFloatingActionButton() {
        if (currentSubsection == Subsection.BEST || currentSubsection == Subsection.BEST_YEAR
                || currentSubsection == Subsection.BEST_MONTH) {
            fabCalendar.setVisibility(View.GONE);
            fabCalendarMenu.setVisibility(View.VISIBLE);
        } else if (currentSubsection == Subsection.ABYSS_BEST) {
            fabCalendar.setVisibility(View.VISIBLE);
            fabCalendarMenu.setVisibility(View.GONE);
        } else {
            fabCalendar.setVisibility(View.GONE);
            fabCalendarMenu.setVisibility(View.GONE);
        }
    }

    private void initializeRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvQuotes.setLayoutManager(linearLayoutManager);

        quotesAdapter = new QuotesAdapter();
        rvQuotes.setAdapter(quotesAdapter);

        rvQuotes.addOnScrollListener(new HidingScrollListener() {
            @Override
            public void onHide() {
                if (currentSubsection == Subsection.BEST || currentSubsection == Subsection.BEST_YEAR
                        || currentSubsection == Subsection.BEST_MONTH) {
                    fabCalendarMenu.hideMenuButton(true);
                } else if (currentSubsection == Subsection.ABYSS_BEST) {
                    fabCalendar.hide(true);
                }
            }

            @Override
            public void onShow() {
                if (currentSubsection == Subsection.BEST || currentSubsection == Subsection.BEST_YEAR
                        || currentSubsection == Subsection.BEST_MONTH) {
                    fabCalendarMenu.showMenuButton(true);
                } else if (currentSubsection == Subsection.ABYSS_BEST) {
                    fabCalendar.show(true);
                }
            }
        });

        scrollListener = new EndlessScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (currentSubsection == Subsection.INDEX || currentSubsection == Subsection.RANDOM
                        || currentSubsection == Subsection.BY_RATING || currentSubsection == Subsection.ABYSS
                        || currentSubsection == Subsection.ABYSS_BEST) {
                    quotesPresenter.getQuotes(true);
                } else {
                    // TODO: show message that end of list reached and instruction about what to do with dat problem
                }
            }
        };

        rvQuotes.addOnScrollListener(scrollListener);

    }

    private void showDatePickerDialog() {
        Calendar calendarMaxDate = Calendar.getInstance();
        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
                QuotesActivity.this,
                calendarMaxDate.get(Calendar.YEAR),
                calendarMaxDate.get(Calendar.MONTH),
                calendarMaxDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.setMaxDate(calendarMaxDate);

        Calendar calendarMinDate = Calendar.getInstance();
        if (currentSubsection == Subsection.ABYSS_BEST) {
            calendarMinDate.set(Calendar.YEAR, calendarMaxDate.get(Calendar.YEAR) - 1);
        } else {
            calendarMinDate.set(Calendar.YEAR, 2004);
            calendarMinDate.set(Calendar.MONTH, 8);
            calendarMinDate.set(Calendar.DAY_OF_MONTH, 1);
        }
        datePickerDialog.setMinDate(calendarMinDate);

        datePickerDialog.showYearPickerFirst(true);

        datePickerDialog.show(getFragmentManager(), "Pick a date");
    }

    private void addQuotes(List<Quote> quotes) {
        quotesAdapter.addQuotes(quotes);
        quotesAdapter.notifyDataSetChanged();
    }
}
