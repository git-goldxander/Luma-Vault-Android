package com.lumavault.app;

import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/** Keeps interactive content outside status bars, display cutouts and navigation bars. */
final class SystemBarInsets {
    private SystemBarInsets() { }

    static void apply(View view, int baseLeft, int baseTop, int baseRight, int baseBottom) {
        view.setPadding(baseLeft, baseTop, baseRight, baseBottom);
        if (Build.VERSION.SDK_INT < 35) return;
        view.setOnApplyWindowInsetsListener((target, windowInsets) -> {
            Insets safe = windowInsets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            target.setPadding(baseLeft + safe.left, baseTop + safe.top,
                    baseRight + safe.right, baseBottom + safe.bottom);
            return windowInsets;
        });
        view.requestApplyInsets();
    }
}
