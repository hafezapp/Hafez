package org.hrana.hafez.view.activity;

import android.support.test.espresso.Espresso;

import com.mauriciotogneri.greencoffee.GreenCoffeeSteps;
import com.mauriciotogneri.greencoffee.annotations.Given;
import com.mauriciotogneri.greencoffee.annotations.Then;
import com.mauriciotogneri.greencoffee.annotations.When;

import org.hrana.hafez.R;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

/**
 * Espresso steps for test scenarios
 */

public class HomePageSteps extends GreenCoffeeSteps {
    public HomePageSteps() {
    }

    @Given("^I see the home screen$")
    public void iSeeHomepage() {
        onViewWithText(R.string.hint_report).isCompletelyDisplayed();
    }

    @Given("^I see the Newsfeed$")
    public void iSeeNewsfeed() {
        iSeeHomepage();
        iClickNews();
    }


    @Given("^I am offline$")
    public void iAmOffline() {

    }

    @Given("^I see the Contacts$")
    public void iSeeContacts() {

    }

    @When("^I click 'Settings'$")
    public void iClickSettings() {
        Espresso.openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onViewWithId(R.id.action_settings).click();
    }


    @When("^I click 'News'$")
    public void iClickNews() {
        onView(allOf(withClassName(endsWith("TabView")),
                withChild(withText(R.string.news)),
                withParent(withParent(withId(R.id.tabs)))))
                .perform(scrollTo()).perform(click());
    }

    @When("^I click 'Contacts'$")
    public void iClickContacts() {

    }

    @When("^I click 'Cases'$")
    public void iClickCases() {

    }

    @When("^I click 'Write Report'$")
    public void iWriteReport() {

    }

    @Then("^I see the news feed$")
    public void iSeeNewsFeed() {
        onViewWithId(R.id.recyclerview).isCompletelyDisplayed();
    }

    @Then("^I see the offline message$")
    public void iSeeOfflineMessage() {
        onViewWithId(R.id.offline_view).isCompletelyDisplayed();
    }


    public class NewsFeedSteps extends GreenCoffeeSteps {

    }

    public class ContactSteps extends GreenCoffeeSteps {
        @Given("^$")
        public void iSeeContactList() {

        }
        @When("^$")
        public void iSelectAContact() {

        }

        @Then("^$")
        public void iSeeContactDetails() {

        }
    }

    public class CaseSteps extends GreenCoffeeSteps {
        @Given("^$")
        public void iSeeChapters() {

        }
        @When("^$")
        public void iExpandChapter() {

        }
        @Then("^$")
        public void iSeeFullText() {

        }
    }
}
