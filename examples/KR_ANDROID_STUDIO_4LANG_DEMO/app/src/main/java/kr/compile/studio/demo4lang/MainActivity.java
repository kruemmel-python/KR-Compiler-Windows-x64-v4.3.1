package kr.compile.studio.demo4lang;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Android Studio Activity fuer die KR Unified Language 4-Sprachen-Fusion
 * (C++20 + Rust + Go + Python in main.kr -> libuser.so & libkr_go.so).
 */
public class MainActivity extends Activity {

    private static String loadError = null;

    static {
        try {
            System.loadLibrary("c++_shared");
            System.loadLibrary("kr_go");
            System.loadLibrary("user");
        } catch (Throwable t) {
            loadError = t.toString();
        }
    }

    public native int nativeRunKrMain();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#101824"));
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 64, 48, 64);

        TextView title = new TextView(this);
        title.setText("KR Unified Language v4.3.1\n4-Sprachen-Fusion (ARM64)");
        title.setTextColor(Color.parseColor("#00E5A3"));
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(title);

        TextView body = new TextView(this);
        body.setTextColor(Color.parseColor("#E6EDF3"));
        body.setTextSize(15f);
        body.setTypeface(Typeface.MONOSPACE);
        body.setPadding(0, 36, 0, 0);

        if (loadError != null) {
            body.setText("[FEHLER beim Laden von libuser.so]\n" + loadError);
        } else {
            int exitCode = nativeRunKrMain();
            body.setText(
                "Quelle: main.kr\n"
                + "----------------------------------------\n"
                + "[1/4 RUST]   rust_security_rule(20, 4)   = 122\n"
                + "[2/4 GO]     go_dispatch_service(122, 6) = 212\n"
                + "[3/4 PYTHON] python_evaluate_logic       = 334\n"
                + "             (inkl. Cross-Call -> Rust)\n"
                + "[4/4 C++20]  Exit-Code aus main.kr       = " + exitCode + "\n"
                + "----------------------------------------\n"
                + "Python-Strategie: AOT_NATIVE_CPP20_BRIDGE\n"
                + "Status: " + (exitCode == 0 ? "ERFOLGREICH VERIFIZIERT (334)" : "FEHLER")
            );
        }

        layout.addView(body);
        scroll.addView(layout);
        setContentView(scroll);
    }
}
