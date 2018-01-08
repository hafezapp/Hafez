package org.hrana.hafez.view.activity;

import android.support.test.espresso.Espresso;

import com.mauriciotogneri.greencoffee.GreenCoffeeSteps;
import com.mauriciotogneri.greencoffee.annotations.Given;
import com.mauriciotogneri.greencoffee.annotations.Then;
import com.mauriciotogneri.greencoffee.annotations.When;

import junit.framework.Assert;

import org.hrana.hafez.R;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

public class SubmissionActivitySteps extends GreenCoffeeSteps {

    public SubmissionActivitySteps() {
    }

    @Given("^I see the submissions screen$")
    public void seeSubmissionPage() {
        onViewWithId(R.id.review_report_submission);
    }

    @When("^I click the overflow menu$")
    public void iClickSettings() {
        Espresso.openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
    }

    @When("^I choose Import File$")
    public void iChooseImportFile() {
        onViewWithText(R.string.menu_import_media).click();
    }

    @Then("^success$")
    public void success() {
        // pass
    }
}
