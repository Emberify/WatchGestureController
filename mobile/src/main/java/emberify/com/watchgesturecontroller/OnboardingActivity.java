package emberify.com.watchgesturecontroller;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.view.View;

import emberify.com.watchgesturecontroller.utils.SharedPreferencesUtils;

public class OnboardingActivity extends AppCompatActivity {

    AppCompatButton btnGetStarted;
    SharedPreferencesUtils spu = new SharedPreferencesUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        btnGetStarted = (AppCompatButton) findViewById(R.id.btn_get_started);

        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spu.saveSharedPreferencesBoolean(OnboardingActivity.this, SharedPreferencesUtils.PREF_OONBOARDING_COMPLETED, true);
                Intent intent = new Intent(OnboardingActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
