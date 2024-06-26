package com.github.arobie1992.clarinet.sampleapp;

import com.github.arobie1992.clarinet.core.Nodes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.FirestoreOptions;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class App {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        var ignored = Nodes.newBuilder();
        FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId("yoonlab")
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .build();
        var db = firestoreOptions.getService();
        var ref = db.collection("test").document("firstTry");
        var result = ref.set(Map.of("test", "user"));
        System.out.println(result.get().getUpdateTime());
    }
}
