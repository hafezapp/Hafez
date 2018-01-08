package org.hrana.hafez.view.activity;

import android.support.test.rule.ActivityTestRule;

import com.mauriciotogneri.greencoffee.GreenCoffeeConfig;
import com.mauriciotogneri.greencoffee.GreenCoffeeTest;
import com.mauriciotogneri.greencoffee.ScenarioConfig;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Locale;

/**
 * Simple test class for onboarding screens to experiment with GreenCoffee test library.
 */

@RunWith(Parameterized.class)
public class OnboardingTest extends GreenCoffeeTest {

    @Rule
    public ActivityTestRule<IntroductionActivity> activity =
            new ActivityTestRule<IntroductionActivity>(IntroductionActivity.class);

    public OnboardingTest(ScenarioConfig scenario) {
        super(scenario);
    }

    @Parameterized.Parameters
    public static Iterable<ScenarioConfig> scenarios() throws IOException
    {
        return new GreenCoffeeConfig("hranaTest") // screenshot if a test fails
                .withFeatureFromAssets("assets/onboarding.feature")
                .scenarios(
                        new Locale("en", "US"),
                        new Locale("fa", "IR")
                ); // the locales used to run the scenarios (optional)
    }

    @Test
    public void test() throws Exception {
        start(new OnboardSteps(activity.getActivity()));
    }
}
