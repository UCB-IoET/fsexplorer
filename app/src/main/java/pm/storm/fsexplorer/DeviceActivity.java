package pm.storm.fsexplorer;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.UUID;


public class DeviceActivity extends ActionBarActivity {

    private Firestorm tgt;
    private BluetoothGatt gatt;
    private FirestormListAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        Intent intent = getIntent();
        tgt = (Firestorm) intent.getParcelableExtra("firestorm");
        gatt = tgt.getDevice().connectGatt(this, false, gatt_cb);
        adapter = new FirestormListAdapter();
        ((ExpandableListView)findViewById(R.id.serviceList)).setAdapter(adapter);
        ((ExpandableListView)findViewById(R.id.serviceList)).setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                System.out.println("CLICKED");
                UUID svc = tgt.getServices().get(groupPosition);
                UUID attr = tgt.getAttributes(svc).get(childPosition);
                Intent intent;
                if (attr.toString().startsWith("00004c0f")) {
                    intent = new Intent(DeviceActivity.this, CoffeeActivity.class);
                } else {
                    intent = new Intent(DeviceActivity.this, AttributeActivity.class);
                }

                intent.putExtra("svc", new ParcelUuid(svc));
                intent.putExtra("attr", new ParcelUuid(attr));
                intent.putExtra("firestorm", tgt);
                startActivity(intent);
                return true;
            }
        });
    }

    public void onBackPressed()
    {
        gatt.disconnect();
        super.onBackPressed();
    }

    BluetoothGattCallback gatt_cb = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            System.out.println("Connection changed");
            gatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            System.out.println("Services discovered");
            for (BluetoothGattService gs : gatt.getServices())
            {
                System.out.println(gs.getUuid());
                if (gs.getUuid().toString().substring(4,8).equals("1800") ||
                    gs.getUuid().toString().substring(4,8).equals("1801")) {
                    continue;
                }
                for (BluetoothGattCharacteristic gc : gs.getCharacteristics()) {
                    if (tgt.addAttributeUuid(gs.getUuid(), gc.getUuid())) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
            gatt.disconnect();
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            System.out.println("Characteristic read");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            System.out.println("Characteristic changed");
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class FirestormListAdapter extends BaseExpandableListAdapter {

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            UUID svc = tgt.getServices().get(groupPosition);
            UUID attr = tgt.getAttributes(svc).get(childPosition);
            return attr.toString();
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            UUID svc = tgt.getServices().get(groupPosition);
            UUID attr = tgt.getAttributes(svc).get(childPosition);
            String sSVC =  svc.toString().substring(4,8);
            String sATTR = attr.toString().substring(4,8);
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) DeviceActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.device_list_item, null);
            }
            ManifestResolver mr = ManifestResolver.getInstance(DeviceActivity.this);
            String attrName = mr.getAttributeName(sSVC, sATTR);
            String attrFQN = mr.getAttributeFQN(sSVC, sATTR);
            ((TextView) convertView.findViewById(R.id.attrName)).setText(attrName);
            ((TextView) convertView.findViewById(R.id.attrFQN)).setText(attrFQN);
            ((TextView) convertView.findViewById(R.id.attrID)).setText("0x" + sATTR);
            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            UUID svc = tgt.getServices().get(groupPosition);
            return tgt.getAttributes(svc).size();
        }

        @Override
        public int getGroupCount() {
            return tgt.getServices().size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            UUID svc = tgt.getServices().get(groupPosition);
            return svc.toString();
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            UUID svc = tgt.getServices().get(groupPosition);
            String sSVC =  svc.toString().substring(4,8);
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) DeviceActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.device_list_header, null);
            }
            ManifestResolver mr = ManifestResolver.getInstance(DeviceActivity.this);
            String svcName = mr.getServiceName(sSVC);
            String svcFQN = mr.getServiceFQN(sSVC);
            ((TextView) convertView.findViewById(R.id.svcName)).setText(svcName);
            ((TextView) convertView.findViewById(R.id.svcFQN)).setText(svcFQN);
            ((TextView) convertView.findViewById(R.id.svcID)).setText("0x"+sSVC);
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

    }
}
