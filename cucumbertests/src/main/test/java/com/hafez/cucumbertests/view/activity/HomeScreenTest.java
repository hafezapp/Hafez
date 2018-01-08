package org.hrana.hafez.view.activity;

import android.support.test.rule.ActivityTestRule;

import com.mauriciotogneri.greencoffee.GreenCoffeeConfig;
import com.mauriciotogneri.greencoffee.GreenCoffeeTest;
import com.mauriciotogneri.greencoffee.ScenarioConfig;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Locale;

/**
 * Test basic homescreen features.
 */
@RunWith(Parameterized.class)
public class HomeScreenTest extends GreenCoffeeTest {

    @Rule
    public ActivityTestRule<MainActivity> activityActivityTestRule
            = new ActivityTestRule<>(MainActivity.class);

    public HomeScreenTest(ScenarioConfig scenario) {
        super(scenario);
    }

    @Parameterized.Parameters
    public static Iterable<ScenarioConfig> scenarios() throws IOException {
        return new GreenCoffeeConfig("hranaTest") // screenshot if a test fails
                .withFeatureFromAssets("assets/homepage.feature")
                .scenarios(
                        new Locale("en", "US"),
                        new Locale("fa", "IR")
                ); // the locales used to run the scenarios (optional)
    }

    @Test
    public void testBrowseHomepage() throws Exception {
        start(new HomePageSteps());
    }

    @Test
    public void testViewNewsFeed() throws Exception {
//        start(new NewsFeedSteps(activityActivityTestRule.getActivity()));
    }

    @Test
    public void testViewContacts() throws Exception {
//        start(new ContactSteps(activityActivityTestRule.getActivity()));
    }

}
