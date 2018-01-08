package org.hrana.hafez.view.activity;

import android.app.Activity;

import com.mauriciotogneri.greencoffee.GreenCoffeeSteps;
import com.mauriciotogneri.greencoffee.annotations.Given;
import com.mauriciotogneri.greencoffee.annotations.Then;
import com.mauriciotogneri.greencoffee.annotations.When;

import org.hrana.hafez.R;
import org.junit.Assert;

/**
 * Steps for onboarding.
 */

public class OnboardSteps extends GreenCoffeeSteps {
    private Activity activity;

    public OnboardSteps(Activity activity) {
        this.activity = activity;
    }

    @Given("^I see the welcome screen")
    public void iSeeWelcomeScreen() {
        onViewWithText(string(R.string.onboarding_title_1));
    }

    @When("^I click next")
    public void iClickNext() {
        onViewWithId(R.id.next).click();
    }

    @When("^I click skip")
    public void iClickSkip() {
        onViewWithId(R.id.skip).click();
    }

    @When("^I click done")
    public void iClickDone() {
        onViewWithId(R.id.next).click();
    }

    @Then("^I see the second screen")
    public void iSeeSecondScreen() {
        onViewWithText(R.string.onboarding_text_2).isDisplayed();
    }

    @Then("^I see the third screen")
    public void iSeeThirdScreen() {
        onViewWithText(R.string.onboarding_text_3).isDisplayed();
    }

    @Then("^I see the fourth screen")
    public void iSeeFourthScreen() {
        onViewWithText(R.string.onboarding_text_4).isDisplayed();
    }


    @Then("^I finish the activity")
    public void iFinishActivity() {
        Assert.assertTrue(activity.isFinishing());
    }

}
