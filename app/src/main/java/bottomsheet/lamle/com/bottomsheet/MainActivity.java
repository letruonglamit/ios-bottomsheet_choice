package bottomsheet.lamle.com.bottomsheet;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void showBottomSheet(View view) {

        List<String> data = new ArrayList<>();
        data.add("Brazil");
        data.add("USA");
        data.add("China");
        data.add("Pakistan");
        data.add("Australia");
        data.add("India");
        data.add("Nepal");
        data.add("Sri Lanka");
        data.add("Spain");
        data.add("Italy");
        data.add("France");

        BottomSheetChoiceDialogFragment.newInstance()
                .setData(data)
                .setSelectedDoneListener(new BottomSheetChoiceDialogFragment.OnItemSelectedDoneListener() {
                    @Override
                    public void onItemSelected(int position, String name) {
                        Button button = findViewById(R.id.button);
                        button.setText(name);
                    }
                })
                .show(getSupportFragmentManager());
    }
}
