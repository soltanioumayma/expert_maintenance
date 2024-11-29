package com.example.test;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class NetworkHelper {

    private static final String BASE_URL = "http://192.168.1.20/sync.php"; // Emulator uses this IP for localhost
    private RequestQueue queue;

    public NetworkHelper(Context context) {
        queue = Volley.newRequestQueue(context);
    }

    public void fetchUsersFromServer(VolleyCallback callback) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, BASE_URL,
                response -> callback.onSuccess(response),
                error -> callback.onError(error.toString()));
        queue.add(stringRequest);
    }

    public interface VolleyCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}
