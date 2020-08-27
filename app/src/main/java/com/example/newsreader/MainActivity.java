package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity
{

    SQLiteDatabase ariticleDB;

    ListView listView ;
    final ArrayList<String> titles = new ArrayList<String>();
    final ArrayList<String> content = new ArrayList<String>();
    ArrayAdapter<String> arrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set table before Download task begins
        ariticleDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        ariticleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId ,INTEGER,title VARCHAR, content  VARCHAR)");


        /*   It will return the ids for the latest news   */
      //  String res="";
        DownloadTask task = new DownloadTask();
        try
        {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
//        Log.i("REsult",res);


        listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent intent = new Intent(getApplicationContext(), Main2Activity.class);
                intent.putExtra("content", content.get(position));
                startActivity(intent);


               // Toast.makeText(MainActivity.this, "Person Clicked : "+ titles.get(position), Toast.LENGTH_SHORT).show();
            }
        });
        updateListView();
    }

    public void updateListView()
    {
        Cursor c = ariticleDB.rawQuery("SELECT * FROM articles", null);
        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");
        if(c.moveToFirst())
        {
            titles.clear();
            content.clear();
            do
                {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            }
            while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }

    }
    public class DownloadTask extends AsyncTask<String, Void, String>
    {

        @Override
        protected String doInBackground(String... urls) {
            String result="";/*   It will contain the ids for the different latest news   */
            URL url;
            HttpsURLConnection urlConnection = null;

            try
            {
                url= new URL(urls[0]);
                urlConnection = (HttpsURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader= new InputStreamReader(in);
                int data = reader.read();

                while (data != -1)
                {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);// String Turned to JSON Array to loop through easily
                int numberOfItems = 20;
                if(jsonArray.length() < 20)
                {
                    numberOfItems = jsonArray.length();
                }

                //Before we hit information we need to clear the table
                ariticleDB.execSQL("DELETE FROM articles");

                for (int i = 0; i<numberOfItems ; i++)
                {
                    String articleId = jsonArray.getString(i); // Getting each id of news


                    /*   It will return the JSON type thing for each latest news   */

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    /*   Passing the url with each ids for the latest news   */
                    urlConnection = (HttpsURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader= new InputStreamReader(in);
                    data = reader.read();

                    String articleInfo = ""; /*   It will contain url,title,... for each latest news   */

                    while (data != -1)
                    {
                        char current = (char) data;
                        articleInfo += current;
                        data = reader.read();
                    }
//                                 Log.i("News Info", articleInfo);


                    /*  Getting single title and url  */

                    JSONObject jsonObject = new JSONObject(articleInfo);
                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url"))
                    {
                            String articleTitle = jsonObject.getString("title");
                            String articleUrl = jsonObject.getString("url");
                          Log.i("T and U ", articleTitle+articleUrl);


                        /*   It will return the description from url  for each latest news   */

                            url =new URL(articleUrl);
                            urlConnection = (HttpsURLConnection) url.openConnection();
                            in = urlConnection.getInputStream();
                            reader= new InputStreamReader(in);
                            data = reader.read();

                            String articleContent = ""; /*   It will contain content for each latest news   */

                            while (data != -1)
                            {
                                char current = (char) data;
                                articleContent += current;
                                data = reader.read();
                            }
 //                           Log.i("News ", articleContent);


                        //Passing data in SQL                                             // Add info later
                        String sql = "INSERT INTO articles (articleId, title, content) VALUES (?,?,?)";
                        //It protects the code, cause there may be things that break the code
                        SQLiteStatement statement = ariticleDB.compileStatement(sql);
                        // Start Passing Information
                        // 1 based indexing, not 0
                        statement.bindString(1, articleId);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleContent);

                        statement.execute();

                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
         }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }
}
