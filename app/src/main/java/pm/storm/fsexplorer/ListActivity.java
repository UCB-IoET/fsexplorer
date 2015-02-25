package pm.storm.fsexplorer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


public class ListActivity extends ActionBarActivity {


    Timer tmr;
    ArrayList<Firestorm> foundDevices;
    HashMap<String, Firestorm> addrToFirestorm;
    ArrayAdapter<Firestorm> fadapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        tmr = new Timer();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_UUID);
        registerReceiver(uuidReceiver, filter2);
        foundDevices = new ArrayList<>();
        fadapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, foundDevices);
        ((ListView) findViewById(R.id.scanlist)).setAdapter(fadapter);
        ((ListView)findViewById(R.id.scanlist)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(ListActivity.this, DeviceActivity.class);
                intent.putExtra("firestorm", foundDevices.get(position));
                startActivity(intent);

            }
        });
        addrToFirestorm = new HashMap<>();

    }

    //Adds a firestorm if it has not already been discovered
    private void addFirestorm(final Firestorm f)
    {
        if (!addrToFirestorm.containsKey(f.getAddr())) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addrToFirestorm.put(f.getAddr(), f);
                    fadapter.add(f);
                }
            });
        }
    }

    private void startScan()
    {
        fadapter.clear();
        addrToFirestorm.clear();
        final ProgressBar pb = (ProgressBar) findViewById(R.id.scanBar);
        pb.setVisibility(View.VISIBLE);
        pb.setIndeterminate(true);
        final BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        final BluetoothAdapter.LeScanCallback cb = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice bd, int rssi, byte[] scanrecord) {

                if (bd.getName() != null && bd.getName().equals("Firestorm")) {
                    System.out.println("Found firestorm\n");
                    final Firestorm f = new Firestorm(bd, rssi, scanrecord);
                    boolean ok = bd.fetchUuidsWithSdp();
                    if (!ok) {
                        System.out.println("Failed to fetch UUIDS");
                    }
                    System.out.println("ADV: " + f.getAdvHexString());
                    addFirestorm(f);
                } else {
                    if (bd.getName() != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i=0;i<scanrecord.length;i++) {
                            int v = scanrecord[i];
                            sb.append(String.format("%02x ", v&0xFF));
                        }
                        System.out.println("Found: "+sb.toString());
                    }
                }
            }
        };
        tmr.schedule(new TimerTask() {
            public void run() {

                System.out.println("Stopping scan");
                ba.stopLeScan(cb);
                ((Button)findViewById(R.id.scanbutton)).post(new Runnable()
                {
                    public void run()
                    {
                        pb.setVisibility(View.INVISIBLE);
                        ((Button)findViewById(R.id.scanbutton)).setText("SCAN");
                        ((Button)findViewById(R.id.scanbutton)).setEnabled(true);
                    }
                });
            }
        }, 5000);
        ba.startLeScan(cb);
    }

    private final BroadcastReceiver uuidReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_UUID)){
                BluetoothDevice src = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Parcelable []puid = (Parcelable[]) intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                if (puid == null) return;
                Firestorm f = addrToFirestorm.get(src.getAddress());
                System.out.println("puid is:"+puid);
                for (Parcelable pu : puid) {
                    ParcelUuid u = (ParcelUuid) pu;
                    System.out.println("Got parcel uuid" + u.toString() + " from " + src.getAddress());
                    if (f.addServiceUuid(u.getUuid()))
                        fadapter.notifyDataSetChanged(); //Services may have changed
                }

            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        startScan();
                        break;
                    default:
                        System.out.println("Other state");
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    public void onBackPressed()
    {
        finish();
    }
    public void onScanButtonClicked(View v)
    {
        Button b = (Button) v;
        b.setEnabled(false);
        b.setText("Scanning...");
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        if (ba == null)
        {
            System.out.println("BA was null");
        }
        if (ba.isEnabled())
        {
            startScan();
        } else {
            ba.enable();
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id) {
            //case R.id.action_settings:
            //    System.out.println("Settings pressed\n");
            //    return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
