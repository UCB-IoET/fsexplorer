package pm.storm.fsexplorer;

import android.app.ActionBar;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.UUID;


public class AttributeActivity extends ActionBarActivity {
    private Firestorm tgt;
    private BluetoothGatt gatt;
    UUID svc;
    UUID attr;
    String sSvc;
    String sAttr;
    ManifestResolver.ManifestFormatEntry [] fields;
    View [] fieldUI;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attribute);
        Intent intent = getIntent();
        tgt = (Firestorm) intent.getParcelableExtra("firestorm");
        gatt = tgt.getDevice().connectGatt(this, false, gatt_cb);
        svc = ((ParcelUuid)intent.getParcelableExtra("svc")).getUuid();
        attr = ((ParcelUuid)intent.getParcelableExtra("attr")).getUuid();
        sSvc = svc.toString().substring(4,8);
        sAttr = attr.toString().substring(4,8);
        //Build the display
        genfields();
    }

    public void genfields() {
        fields = ManifestResolver.getInstance(this).getFields(sSvc, sAttr);
        fieldUI = new View[fields.length];
        System.out.println("MFARR "+fields+" len "+fields.length);
        for (int i = 0; i < fields.length; i++) {
            fieldUI[i] = addField(fields[i].name, fields[i].desc, fields[i].getTypeAsString(), fields[i].isNumeric());
        }
    }
    public void onBackPressed()
    {
        gatt.disconnect();
        super.onBackPressed();
    }

    private View addField(String name, String desc, String type, boolean isNumeric) {
        View fv;
        LayoutInflater inflater = (LayoutInflater) AttributeActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        fv = inflater.inflate(R.layout.attr_field, null);
        ((TextView) fv.findViewById(R.id.fieldName)).setText(name);
        ((TextView) fv.findViewById(R.id.fieldDesc)).setText(desc);
        ((TextView) fv.findViewById(R.id.type)).setText(type);
        if (isNumeric) {
            ((EditText) fv.findViewById(R.id.editValue)).setInputType(InputType.TYPE_CLASS_NUMBER);
        }
        LinearLayout layout = (LinearLayout) findViewById(R.id.fieldList);
        fv.setId(0);
        fv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(fv);
        return fv;
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
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_attribute, menu);
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

    public void wbOnClick(View v) {
        byte [] chardata = new byte[20];
        for (ManifestResolver.ManifestFormatEntry mfe : fields) {
            mfe.
        }
        //Get the values of all the fields
    }

    public void rbOnClick(View v) {

    }

    public void nbOnClick(View v) {

    }
}
