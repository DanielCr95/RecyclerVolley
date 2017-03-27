package com.example.tiger.recyclervolley;

import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.tiger.recyclervolley.Realm.RealmHelper;
import com.example.tiger.recyclervolley.adapters.PostAdapter;
import com.example.tiger.recyclervolley.models.Post;
import com.example.tiger.recyclervolley.receiver.ConnectivityReceiver;
import com.example.tiger.recyclervolley.utils.AppController;
import com.example.tiger.recyclervolley.utils.Constants;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;

public class MainActivity extends AppCompatActivity implements ConnectivityReceiver.ConnectivityReceiverListener,
        SwipeRefreshLayout.OnRefreshListener{

    public CoordinatorLayout coordinatorLayout;
    public boolean isConnected;
    public static final String NA = "NA";
    public RecyclerView recycler_post;
    public PostAdapter adapter;
    NavigationView mNavigationView;
    ActionBar mActionBar;
    private Toolbar toolbar;
    private Toolbar searchToolbar;
    private boolean isSearch = false;
    ArrayList<Post> post_array = new ArrayList<>();
    public SwipeRefreshLayout swipeRefreshLayout;
    Realm realm;
    ArrayList<String> array;
    RealmChangeListener realmChangeListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar_viewpager);
        searchToolbar = (Toolbar) findViewById(R.id.toolbar_search);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        recycler_post = (RecyclerView) findViewById(R.id.recycler_post);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recycler_post.setLayoutManager(layoutManager);
        recycler_post.setItemAnimator(new DefaultItemAnimator());
        //    initToolbar();

        //
        //  initDrawer();

        //displayFragment(R.id.nav_home, getString(R.string.nav_home));

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorRed);

        prepareActionBar(toolbar);

        try {
            getData();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //setup realm
      //  RealmConfiguration config=new RealmConfiguration.Builder(this).schemaVersion(3).deleteRealmIfMigrationNeeded().build();
        //realm = Realm.getInstance(config);
        //retrieve data
        //final RealmHelper realmHelper = new RealmHelper(realm);
        //realmHelper.retrieveFromDB();

       // adapter = new PostAdapter(MainActivity.this, realmHelper.justRefresh());
       // recycler_post.setAdapter(adapter);
    //    realmChangeListener = new RealmChangeListener() {
        //    @Override
          //  public void onChange() {
      //          adapter = new PostAdapter(MainActivity.this, realmHelper.justRefresh());
        //        recycler_post.setAdapter(adapter);

//            }
        //};

      //  realm.addChangeListener(realmChangeListener);




    }

    public void getData() throws Exception {
        if (checkConnectivity()){
            try {
                swipeRefreshLayout.setRefreshing(true);
                getAllPosts();
            } catch (Exception e) {
                e.printStackTrace();

            }
        }else {

            getAllPosts();
            showSnack();

        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(getApplicationContext(),"u have resumed the app",Toast.LENGTH_SHORT).show();
        AppController.getInstance().setConnectivityReceiver(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(isSearch ? R.menu.menu_search_toolbar : R.menu.menu_main, menu);
        if (isSearch) {
            //Toast.makeText(getApplicationContext(), "Search " + isSearch, Toast.LENGTH_SHORT).show();
            final SearchView search = (SearchView) menu.findItem(R.id.action_search).getActionView();
            search.setIconified(false);
            search.setQueryHint("Search item...");
            search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    try {
                        adapter.getFilter().filter(s);
                    }
                    catch (NullPointerException e)
                    {
                        e.printStackTrace();
                    }
                    return true;
                }
            });
            search.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    closeSearch();
                    return true;
                }
            });
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search: {
                isSearch = true;
                searchToolbar.setVisibility(View.VISIBLE);
                prepareActionBar(searchToolbar);
                supportInvalidateOptionsMenu();
                return true;
            }
            case android.R.id.home:
                closeSearch();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void closeSearch() {
        if (isSearch) {
            isSearch = false;
            prepareActionBar(toolbar);
            searchToolbar.setVisibility(View.GONE);
            supportInvalidateOptionsMenu();
        }
    }

    private void prepareActionBar(Toolbar toolbar) {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    public boolean checkConnectivity() {
        return ConnectivityReceiver.isConnected();
    }

    public void showSnack() {

        Snackbar.make(coordinatorLayout, getString(R.string.no_internet_connected), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.settings), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    }
                }).setActionTextColor(Color.RED)
                .show();
    }


    @Override
    public void onNetworkChange(boolean inConnected) {
        this.isConnected = inConnected;
        Toast.makeText(getApplicationContext(),"the app network have been changed",Toast.LENGTH_SHORT).show();
    }

    public void getAllPosts() throws Exception{
        String TAG = "POSTS";
        String url = Constants.POSTS_URL;
        StringRequest jsonObjectRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("response", response);
                parseJson(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("error", error.getMessage());
            }
        });


        AppController.getInstance().addToRequestQueue(jsonObjectRequest, TAG);

    }

    public void parseJson(String response){

        try {
            RealmHelper realmHelper = new RealmHelper(realm);

            JSONObject jsonObject = new JSONObject(response);
            JSONArray array = jsonObject.getJSONArray("posts");
            post_array.clear();
            Post p;
            for(int i=0 ; i<array.length() ; i++)
            {
                JSONObject o = array.getJSONObject(i);
                String container= o.getString("excerpt");
                if(container.contains("uploads"))
                {
                    String[] splited = container.split(":");
                    String x = splited[1];
                    String[] y = x.split("jpg");
                    String z = y[0];
                    String output = "https:" + z + "jpg";

                    String id=o.getString("id");
                    String url=o.getString("url");

                  p = new Post();
                  p.setId(id);
                  p.setUrl(url);
                  p.setImage(output);
                    post_array.add(p);
                    //realmHelper.save(p);
                }
                 else
                {
                    String id=o.getString("id");
                    String url=o.getString("url");
                    p = new Post();
                    p.setId(id);
                    p.setUrl(url);
                    p.setImage("Empty");
                    post_array.add(p);
                   // realmHelper.save(p);

                }
            }

            adapter = new PostAdapter(MainActivity.this, post_array);
            recycler_post.setAdapter(adapter);
            swipeRefreshLayout.setRefreshing(false);

        } catch (JSONException e) {
            swipeRefreshLayout.setRefreshing(false);
            e.printStackTrace();
        }
    }

    public boolean contains(JSONObject jsonObject, String key){
        return jsonObject != null && jsonObject.has(key) && !jsonObject.isNull(key) ? true : false;
    }

    @Override
    public void onRefresh() {
        try {
            Toast.makeText(getApplicationContext(),"u have refreshed the app",Toast.LENGTH_SHORT).show();

            //when u swipe the app..the getdata method is invoked !
            getData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(getApplicationContext(),"u have paused the app",Toast.LENGTH_SHORT).show();
    }

    //  realm.removeChangeListener(realmChangeListener);
        //realm.close();

}
