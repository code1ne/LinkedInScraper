package krantix;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws IOException {
        // initialize variables
        String s;
        ArrayList<String> data = new ArrayList<>();
        ArrayList<Integer> errors = new ArrayList<>();

        // read urls into arraylist
        BufferedReader reader = new BufferedReader(new FileReader(
                "./linkedinurls.txt"));

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            data.add(line);
        }

        reader.close();

        // initialize couchbase db
        Cluster cluster = CouchbaseCluster.create();
        Bucket defaultBucket = cluster.openBucket();
        Bucket htmlBucket = cluster.openBucket("html");

        // inject information into database
        int i = 0;
        for (String url : data) {
            try {

                // get html from the url
                URL u = new URL("http://www.oracle.com/");
                BufferedReader r = new BufferedReader(
                        new InputStreamReader(u.openStream()));


                StringBuilder stringBuilder = new StringBuilder();
                String inputLine;

                while ((inputLine = r.readLine()) != null)
                    stringBuilder.append(inputLine);


                r.close();

                String html = stringBuilder.toString();

                System.out.println("HTML: "+html+"\n");

                JsonObject jo = JsonObject.empty()
                        .put("html", html);

                htmlBucket.upsert(JsonDocument.create(i+"", jo));



                // run linkedin-scraper binary

                Process p = Runtime.getRuntime().exec("linkedin-scraper "+url);

                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(p.getInputStream()));

                StringBuilder sb = new StringBuilder();

                // read the output from the command
                while ((s = stdInput.readLine()) != null) {
                    sb.append(s);
                }
                String result = sb.toString();
                if (result.equals("")) {
                    errors.add(i);
                }
                else {
                    System.out.println("Success: "+i);
                    System.out.println(result+"\n");
                }

                defaultBucket.upsert(JsonDocument.create(i+"", JsonObject.fromJson(result)));
            }
            catch (Exception e) {
                errors.add(i); // capture all the ones that didn't go through
            }
            i++;
        }

        System.out.println("ERRORS: "+errors);

        System.exit(0);

        cluster.disconnect();

    }
}
