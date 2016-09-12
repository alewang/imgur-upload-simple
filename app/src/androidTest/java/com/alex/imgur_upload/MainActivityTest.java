package com.alex.imgur_upload;

/**
 * Created by alex on 8/8/16.
 */

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
	@Rule
	public ActivityTestRule<MainActivity> myActivity = new ActivityTestRule<>(MainActivity.class);

	@Test
	public void testNoExceptionOnResultWithNullData() {
		MainActivity act = myActivity.getActivity();
		act.onActivityResult(MainActivity.GET_IMAGE_REQUEST_CODE, MainActivity.RESULT_OK, null);
	}
}
