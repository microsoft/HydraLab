package com.microsoft.hydralab.android.client;


import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule =
            new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.WRITE_EXTERNAL_STORAGE");

    @Test
    public void mainActivityTest() {
        ViewInteraction imageView = onView(
                allOf(withId(R.id.debug_icon), withContentDescription("Debug"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.container),
                                        2),
                                0),
                        isDisplayed()));
        imageView.perform(click());

        ViewInteraction imageView2 = onView(
                allOf(withId(R.id.debug_icon), withContentDescription("Debug"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.container),
                                        2),
                                0),
                        isDisplayed()));
        imageView2.perform(click());

        ViewInteraction textView = onView(
                allOf(withText("HydraLab Client"),
                        withParent(allOf(IsInstanceOf.<View>instanceOf(android.view.ViewGroup.class),
                                withParent(IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class)))),
                        isDisplayed()));
        textView.check(matches(withText("HydraLab Client")));

        ViewInteraction imageView3 = onView(
                allOf(withId(R.id.place_holder), withContentDescription("Placeholder Image"),
                        withParent(allOf(withId(R.id.container),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        imageView3.check(matches(isDisplayed()));

        ViewInteraction imageView4 = onView(
                allOf(withId(R.id.debug_icon), withContentDescription("Debug"),
                        withParent(withParent(withId(R.id.container))),
                        isDisplayed()));
        imageView4.check(matches(isDisplayed()));

        ViewInteraction button = onView(
                allOf(withId(R.id.record_button), withText("START RECORDER"),
                        withParent(withParent(withId(R.id.container))),
                        isDisplayed()));
        button.check(matches(isDisplayed()));

        ViewInteraction imageView5 = onView(
                allOf(withId(R.id.debug_icon), withContentDescription("Debug"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.container),
                                        2),
                                0),
                        isDisplayed()));
        imageView5.perform(click());
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
