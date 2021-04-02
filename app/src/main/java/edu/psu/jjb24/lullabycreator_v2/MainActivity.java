package edu.psu.jjb24.lullabycreator_v2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener {

    String[] sounds = {"Row, row, row your boat","Applause","Drumroll","Boo","Jokefill","Tick Tock",
            "Chopper","Explosion","Scanner","Scared","Siren","Thunder"};
    int[] resources = {R.raw.row_your_boat, R.raw.applause, R.raw.drumroll, R.raw.boo, R.raw.jokefill, R.raw.ticktock,
            R.raw.chopper, R.raw.explosion, R.raw.scanner, R.raw.scared, R.raw.siren, R.raw.thunder};

    BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int streams = intent.getIntExtra("status",0);
            if (streams == 0) {
                findViewById(R.id.mainActivityBtnStop).setVisibility(View.INVISIBLE);
                ((TextView) MainActivity.this.findViewById(R.id.mainActivityTxtStatus)).setText(R.string.default_status);
            }
            else {
                ((TextView) MainActivity.this.findViewById(R.id.mainActivityTxtStatus)).setText(streams + " streams are now playing.");
                findViewById(R.id.mainActivityBtnStop).setVisibility(View.VISIBLE);
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView list = findViewById(R.id.mainActivityLstStatus);
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, sounds));

        list.setOnItemClickListener(this);
        registerReceiver(statusReceiver, new IntentFilter("edu.psu.jjb24.multimediaexample.STATUS_UPDATE"));
        MusicPlayerService.status(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MusicPlayerService.play(this, resources[position]);
    }

    public void btnStopClick(View view) {
        MusicPlayerService.stop(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(statusReceiver);
    }

}
