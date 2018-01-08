package org.hrana.hafez.view.activity;

import android.animation.ArgbEvaluator;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.hrana.hafez.R;
import org.hrana.hafez.view.CustomSwipeViewPager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static org.hrana.hafez.Constants.ACTION_CLICK;
import static org.hrana.hafez.Constants.CATEGORY_BUTTON_PRESS;

/**
 * Introduction and onboarding.
 */

public class IntroductionActivity extends BaseActivity {
    private static final int COUNT = 4;

    @BindView(R.id.intro_pager)
    CustomSwipeViewPager pager;
    @BindView(R.id.indicator)
    TabLayout tabs;
    @BindView(R.id.next)
    Button next;
    @BindView(R.id.skip)
    Button skip;
    private int[] backgroundColors;
    private boolean canSkip, isEuaAccepted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_intro);
        canSkip = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.can_skip_onboarding_key), false);
        isEuaAccepted = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.eua_accepted_key), false);

        ButterKnife.bind(this);

        // Get
        final String[] onboardTitles = getResources().getStringArray(R.array.onboardTitles);
        String[] onboardContents;
        if (isEuaAccepted) {
            onboardContents = getResources().getStringArray(R.array.onboardContentsEuaAccepted);
        } else {
            onboardContents = getResources().getStringArray(R.array.onboardContents);
        }
        backgroundColors = getResources().getIntArray(R.array.backgrounds);

        // Change colour on onboarding slides. Not working atm.
        final ArgbEvaluator eval = new ArgbEvaluator();

        pager.setAdapter(new CustomPagerAdapter(getSupportFragmentManager(),
                onboardTitles, onboardContents, backgroundColors));
        pager.setCurrentItem(COUNT - 1);
        pager.setSwipeEnabled(true); // Set to `canSkip` to disable skipping/swiping on first run of the app

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            float currentSum = 0f;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                int newColor;
                // if scrolling 'forward' (page numbers decreasing! RTL)
                if (position + positionOffset <= currentSum) {
                    newColor = (Integer) eval.evaluate(positionOffset, position, updateColor(position - 1));
                } else { // else if scrolling 'backward' (page numbers increasing! RTL)
                    newColor = (Integer) eval.evaluate(positionOffset, position, updateColor(position + 1));
                }
                pager.setBackgroundColor(newColor); // layout
                currentSum = position + positionOffset;
            }

            @Override
            public void onPageSelected(int position) {
                // Set background color, using first color in list as a fallback
                if (position == 0) {
                    skip.setVisibility(View.INVISIBLE);
                } else {
                    skip.setVisibility(canSkip ? View.VISIBLE : View.INVISIBLE);
                    next.setText(getString(R.string.next));
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        tabs.setupWithViewPager(pager);

        // Skipping or not
        if (canSkip) { // only after completing onboarding successfully already
            skip.setVisibility(View.VISIBLE);
        }

        // GA
        setAnalyticsScreenName(IntroductionActivity.class.getSimpleName());
    }

    /*
     * Update colors by incrementing through the color list.
     * Return the first color in the list if position is >= list size.
     */
    protected int updateColor(int newPosition) {
        if (newPosition < 0 || newPosition >= backgroundColors.length) {
            return backgroundColors[0];
        } else {
            return backgroundColors[newPosition];
        }
    }

    @OnClick(R.id.next)
    public void clickNext() {
        sendAnalyticsHitEvent(ACTION_CLICK, CATEGORY_BUTTON_PRESS, "Next: onboarding");
        int which = pager.getCurrentItem();
        if (which > 0) {
            pager.setCurrentItem(which - 1);
        } else {
            finishOnboarding();
        }
    }

    @OnClick(R.id.skip)
    public void clickSkip() {
        sendAnalyticsHitEvent(ACTION_CLICK, CATEGORY_BUTTON_PRESS, "Skipped Onboarding");
        finishOnboarding();

    }

    private void finishOnboarding() {
        Intent intent;
        if (isEuaAccepted) { // No need to review EUA
            intent = new Intent(this, MainActivity.class);
        }
        else {
            intent = new Intent(this, EuaActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PreferenceManager
                .getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(getString(R.string.can_skip_onboarding_key), true)
                .putBoolean(getString(R.string.onboarding_complete_key), true)
                .apply();

        startActivity(intent);
        this.finish();
    }

    @Override
    public void onBackPressed() {
        int which = pager.getCurrentItem();
        if (pager.getCurrentItem() < COUNT - 1) {
            pager.setCurrentItem(which + 1);
        } else {
            super.onBackPressed();
        }
    }

    public static class CustomPagerAdapter extends FragmentPagerAdapter {
        private String[] titles, contents;
        private int[] colors;

        public CustomPagerAdapter(FragmentManager fm, String[] titles, String[] contents, int[] colors) {
            super(fm);
            this.titles = titles;
            this.contents = contents;
            this.colors = colors;
        }

        @Override
        public Fragment getItem(int position) {
            return IntroFragment.getInstance(getTitle(position), getContent(position), position, colors);
        }

        @Override
        public int getCount() {
            return COUNT;
        }

        private String getTitle(int which) {
            return titles[which];
        }

        private String getContent(int which) {
            return contents[which];
        }

    }

    public static class IntroFragment extends Fragment {
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.content)
        TextView content;
        @BindView(R.id.linear_layout)
        LinearLayout layout;
        @BindView(R.id.onboarding_image)
        ImageView image;
        private Unbinder unbinder;

        private int position;
        private int[] backgrounds;
        final static String TITLE = "title", CONTENT = "content", POSITION = "position", BACKGROUNDS = "backgrounds";

        public static IntroFragment getInstance(String title, String content, int position, int[] backgrounds) {
            IntroFragment fragment = new IntroFragment();
            Bundle b = new Bundle();
            b.putString(TITLE, title);
            b.putString(CONTENT, content);
            b.putInt(POSITION, position);
            b.putIntArray(BACKGROUNDS, backgrounds);
            fragment.setArguments(b);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_appintro, container, false);
            unbinder = ButterKnife.bind(this, view);

            Bundle b = getArguments();
            if (b != null) {
                title.setText(b.getString(TITLE));
                content.setText(b.getString(CONTENT));
                position = b.getInt(POSITION);
                backgrounds = b.getIntArray(BACKGROUNDS);
                if (position < backgrounds.length) {
                    view.setBackgroundColor(backgrounds[position]);
                } else {
                    view.setBackgroundColor(backgrounds[0]);
                }
            }

            switch (position) {
                case 3:
                    image.setImageResource(R.drawable.ic_light_bulb_colour);
                    break;
                case 2:
                    image.setImageResource(R.drawable.ic_megaphone_colour);
                    break;
                case 1:
                    image.setImageResource(R.drawable.ic_text_news_colour);
                    break;
                case 0:
                    image.setImageResource(R.drawable.ic_green_check_done_colour);
                    break;
                default:
                    break;
            }

            return view;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putInt(POSITION, position);
            super.onSaveInstanceState(outState);
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            unbinder.unbind();
        }
    }
}
