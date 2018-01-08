package org.hrana.hafez.view.activity;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import static android.content.Intent.ACTION_GET_CONTENT;
import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;

import org.hrana.hafez.R;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AnyOf.anyOf;

/**
 * Test launching file import and submit actions from submission menu.
 */
public class CheckSubmissionTest {
    File f;

    @Rule
    public IntentsTestRule<MainActivity> intentsRule = new IntentsTestRule<MainActivity>(MainActivity.class);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        f = folder.newFile("picture.png"); // fake file

        Intent intent = new Intent();
        intent.setData(Uri.fromFile(f));
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, intent);

        // only newer sdks support open_document
        if (Build.VERSION.SDK_INT >= 19) {
            intending(anyOf(
                    hasAction(Intent.ACTION_GET_CONTENT),
                    hasAction(ACTION_OPEN_DOCUMENT))
            ).respondWith(result);
        } else {
            intending(hasAction(Intent.ACTION_GET_CONTENT)
            ).respondWith(result);
        }
    }

    @Test @Ignore // have to fix espresso-core error
    public void testSubmitFile() throws Exception {
        Espresso.openActionBarOverflowOrOptionsMenu(intentsRule.getActivity());
        onView(withText(R.string.menu_import_media)).check(matches(isDisplayed()));
        onView(withText(R.string.menu_import_media)).perform(click());
        if (Build.VERSION.SDK_INT >= 19) {
            intended(hasAction(ACTION_OPEN_DOCUMENT));
        } else {
            intended(hasAction(ACTION_GET_CONTENT));
        }
    }

}
